resource "aws_dlm_lifecycle_policy" "postgres_weekly_snapshots" {

  description        = "Weekly PostgreSQL snapshots"

  execution_role_arn = module.dlm_role.role_arn

  state = "ENABLED"

  policy_details {

    resource_types = ["VOLUME"]

    target_tags = {
      Backup = "weekly"
    }

    schedule {

      name = "weekly-postgres-snapshots"

      create_rule {
        cron_expression = "cron(0 2 ? * SUN *)"
      }

      retain_rule {
        count = 4
      }

      copy_tags = true

      tags_to_add = {
        SnapshotCreator = "DLM"
        Frequency       = "Weekly"
        Application     = "ApnaFund"
      }
    }
  }

  tags = {
    Name = "apnafund-postgres-weekly-snapshots"
    Env  = "dev"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "postgres_backup_retention" {

  bucket = aws_s3_bucket.app.id

  rule {

    id     = "postgres-backup-retention"

    status = "Enabled"

    filter {
      prefix = "backups/dev/postgres/"
    }

    expiration {
      days = 30
    }

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}