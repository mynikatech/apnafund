data "aws_iam_policy_document" "postgres_backup_access" {

  statement {

    sid    = "AllowListBackupPrefix"

    effect = "Allow"

    actions = [
      "s3:ListBucket"
    ]

    resources = [
      aws_s3_bucket.app.arn
    ]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"

      values = [
        "backups/dev/postgres/*"
      ]
    }
  }

  statement {

    sid    = "AllowBackupObjectAccess"

    effect = "Allow"

    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject"
    ]

    resources = [
      "${aws_s3_bucket.app.arn}/backups/dev/postgres/*"
    ]
  }
}

resource "aws_iam_policy" "postgres_backup_access" {

  name   = "ApnaFundPostgresBackupAccess"

  policy = data.aws_iam_policy_document.postgres_backup_access.json
}

resource "aws_iam_role_policy_attachment" "attach_postgres_backup_access" {

  role       = module.iam_deploy_role.role_name

  policy_arn = aws_iam_policy.postgres_backup_access.arn
}