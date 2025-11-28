#!/bin/bash
set -euo pipefail

LOG_PREFIX="[user-data]"

# ---------- Config ----------
APNA_ENV="dev"
S3_CONFIG_BUCKET="${S3_CONFIG_BUCKET:-apnafund-config-861082243595-ap-south-1}"
S3_STARTUP_KEY="${S3_STARTUP_KEY:-apnafund/${APNA_ENV}/startup.sh}"
AWS_REGION="${AWS_REGION:-ap-south-1}"

# ---------- Defensive cleanup for stale Docker CE repo ----------
if [ -f /etc/yum.repos.d/docker-ce.repo ]; then
  echo "${LOG_PREFIX} Removing stale /etc/yum.repos.d/docker-ce.repo to avoid 404 errors"
  rm -f /etc/yum.repos.d/docker-ce.repo || true
fi

# ---------- Refresh repositories ----------
echo "${LOG_PREFIX} Cleaning and refreshing dnf metadata"
dnf -y clean all || true
dnf -y makecache || true

# ---------- Ensure correct curl is installed (allow erasing curl-minimal if present) ----------
echo "${LOG_PREFIX} Ensuring curl is installed"
if ! command -v curl >/dev/null 2>&1; then
  # try install, if conflict with curl-minimal, remove and re-install
  if ! dnf -y install curl --allowerasing >/dev/null 2>&1; then
    dnf -y remove curl-minimal >/dev/null 2>&1 || true
    dnf -y install curl >/dev/null 2>&1 || true
  fi
else
  echo "${LOG_PREFIX} curl already present"
fi

# ---------- Install base packages (idempotent) ----------
echo "${LOG_PREFIX} Installing base packages (git, unzip, jq, tar, wget, bind-utils, which, docker, awscli)"
# include docker package (distro docker). If you prefer docker-ce, see commented section below.
dnf -y install git unzip jq tar wget bind-utils which docker awscli || true

# ---------- Add Docker official repo for  docker-ce instead of distro docker ----------
# Uncomment the two lines below if you want docker-ce from Docker's repo, then uncomment the docker-ce install later.
# echo "${LOG_PREFIX} Adding Docker official repo (commented by default)"
# dnf -y config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo || true

# ---------- Install docker-ce (uncomment if prefer docker-ce) ----------
# echo "${LOG_PREFIX} Installing docker-ce (commented by default)"
# dnf -y install docker-ce docker-ce-cli containerd.io --allowerasing || true

# ---------- Enable and start Docker service ----------
echo "${LOG_PREFIX} Enabling and starting Docker service"
systemctl enable --now docker || true

# ---------- Ensure docker group exists and add ec2-user ----------
if ! getent group docker >/dev/null 2>&1; then
  echo "${LOG_PREFIX} Creating docker group"
  groupadd -f docker || true
fi

if id -u ec2-user >/dev/null 2>&1; then
  if id -nG ec2-user 2>/dev/null | grep -qw docker; then
    echo "${LOG_PREFIX} ec2-user already in docker group"
  else
    echo "${LOG_PREFIX} Adding ec2-user to docker group"
    usermod -aG docker ec2-user || true
  fi
else
  echo "${LOG_PREFIX} ec2-user not present; skipping usermod"
fi

# ---------- Install Docker Compose CLI plugin (idempotent) ----------
COMPOSE_PATH="/usr/libexec/docker/cli-plugins/docker-compose"
COMPOSE_BIN_URL="https://github.com/docker/compose/releases/download/v2.20.2/docker-compose-linux-x86_64"

if [ ! -f "$COMPOSE_PATH" ]; then
  echo "${LOG_PREFIX} Installing Docker Compose CLI plugin to ${COMPOSE_PATH}"
  mkdir -p "$(dirname "$COMPOSE_PATH")"
  if curl -fsSL "${COMPOSE_BIN_URL}" -o "${COMPOSE_PATH}"; then
    chmod +x "${COMPOSE_PATH}" || true
    echo "${LOG_PREFIX} Docker Compose CLI plugin installed"
  else
    echo "${LOG_PREFIX} Warning: failed to download docker-compose from ${COMPOSE_BIN_URL}"
  fi
else
  echo "${LOG_PREFIX} Docker Compose CLI plugin already present at ${COMPOSE_PATH}"
fi

# Some distros also package docker-compose-plugin; try to install if not present (non-fatal)
if ! command -v docker-compose >/dev/null 2>&1 && ! docker compose version >/dev/null 2>&1; then
  echo "${LOG_PREFIX} Attempting to install docker-compose-plugin package (non-fatal)"
  dnf -y install docker-compose-plugin || true
fi

# ---------- Small swap ----------
if ! swapon --show | grep -q "swapfile"; then
  fallocate -l 1G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=1024
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo "/swapfile none swap sw 0 0" >> /etc/fstab
fi

# ---------- Folders ----------
install -d -m 750 /opt/apnafund
install -d -m 750 /opt/apnafund/postgres
install -d -m 750 /opt/apnafund/liquibase
install -d -m 750 /opt/apnafund/bin
install -d -m 750 /var/log/apnafund
chown -R ec2-user:ec2-user /opt/apnafund

# ---------- Secrets -> .env ----------
SECRET_JSON="$(aws secretsmanager get-secret-value \
  --secret-id apnafund/${APNA_ENV}/postgres \
  --query SecretString --output text --region "${AWS_REGION}" 2>/dev/null || true)"

if [ -n "$SECRET_JSON" ]; then
  umask 077
  echo "$SECRET_JSON" | jq -r '
    "POSTGRES_DB="+.db,
    "POSTGRES_HOST="+.host,
    "POSTGRES_PORT="+(.port|tostring),
    "POSTGRES_ADMIN_USER="+.admin_user,
    "POSTGRES_ADMIN_PASS="+.admin_pass,
    "POSTGRES_DEPLOY_USER="+.deploy_user,
    "POSTGRES_DEPLOY_PASS="+.deploy_pass,
    "POSTGRES_APP_USER="+.app_user,
    "POSTGRES_APP_PASS="+.app_pass,
    "POSTGRES_SCHEMA="+.target_schema,
    "POSTGRES_APP_ROLE="+.app_role
  ' > /opt/apnafund/.env || true
fi

# -----------------------------
# Dedicated EBS volume for Postgres data (if attached)
# /dev/xvdf shows up as /dev/nvme1n1 on AL2023
# -----------------------------
DATA_DEV="/dev/nvme1n1"
DATA_DIR="/opt/apnafund/postgres"

if lsblk | grep -q "nvme1n1"; then
  echo "Detected extra EBS volume ${DATA_DEV}"

  # format only if not already formatted
  if ! blkid ${DATA_DEV} >/dev/null 2>&1; then
    echo "Formatting ${DATA_DEV} as XFS ..."
    mkfs.xfs -f ${DATA_DEV}
  else
    echo "${DATA_DEV} already formatted."
  fi

  mkdir -p "${DATA_DIR}"

  # ensure one fstab entry (idempotent)
  if ! grep -q "^${DATA_DEV} " /etc/fstab; then
    echo "${DATA_DEV} ${DATA_DIR} xfs defaults,nofail 0 2" >> /etc/fstab
  fi

  # mount and fix ownership
  mount -a || true
  chown -R ec2-user:ec2-user /opt/apnafund
  echo "${DATA_DEV} mounted on ${DATA_DIR}."
else
  echo "No secondary EBS volume detected."
fi

# ---------- Download managed control files from S3 (first-boot best-effort) ----------
S3_PREFIX="$(dirname "${S3_STARTUP_KEY}")"   # e.g. apnafund/dev
REGION="${AWS_REGION:-ap-south-1}"

DST_UPD="/opt/apnafund/bin/update_startup_from_s3.sh"
DST_DISK="/usr/local/bin/apnafund-disk-setup.sh"
DST_STARTUP="/opt/apnafund/bin/startup.sh"
DST_SERVICE="/etc/systemd/system/apnafund-startup.service"

# create dirs
mkdir -p "$(dirname "$DST_UPD")"
mkdir -p "$(dirname "$DST_DISK")"
mkdir -p "$(dirname "$DST_STARTUP")"

# download if available (do not fail boot if not present)
aws s3 cp "s3://${S3_CONFIG_BUCKET}/${S3_PREFIX}/update_startup_from_s3.sh" "$DST_UPD" --region "$REGION" || true
aws s3 cp "s3://${S3_CONFIG_BUCKET}/${S3_PREFIX}/apnafund-disk-setup.sh" "$DST_DISK" --region "$REGION" || true
aws s3 cp "s3://${S3_CONFIG_BUCKET}/${S3_PREFIX}/startup.sh" "$DST_STARTUP" --region "$REGION" || true
aws s3 cp "s3://${S3_CONFIG_BUCKET}/${S3_PREFIX}/apnafund-startup.service" "$DST_SERVICE" --region "$REGION" || true

[ -f "$DST_UPD" ] && { chmod +x "$DST_UPD"; chown root:root "$DST_UPD"; }
[ -f "$DST_DISK" ] && { chmod +x "$DST_DISK"; chown root:root "$DST_DISK"; }
[ -f "$DST_STARTUP" ] && { chmod +x "$DST_STARTUP"; chown root:root "$DST_STARTUP"; }
if [ -f "$DST_SERVICE" ]; then
  chmod 644 "$DST_SERVICE"
  chown root:root "$DST_SERVICE"
  systemctl daemon-reload || true
fi

# Run updater once (best-effort) to ensure service + scripts are in place
if [ -x "$DST_UPD" ]; then
  sudo "$DST_UPD" || true
fi

# Enable the startup service (it may have been installed by the updater)
systemctl enable apnafund-startup.service || true

# ---------- Optional nginx ----------
dnf -y install nginx
systemctl enable --now nginx
echo "<h1>ApnaFund ${APNA_ENV^} $(date)</h1>" > /usr/share/nginx/html/index.html
