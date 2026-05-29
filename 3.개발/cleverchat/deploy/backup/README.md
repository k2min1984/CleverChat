# CleverChat PostgreSQL Backup Sample

These scripts are operations samples for running PostgreSQL logical backups
outside the CleverChat application process. They do not add an application API,
admin screen, or scheduler.

## Required Environment

Set database connection values through the OS environment or a secret manager:

- `PGHOST`
- `PGPORT`
- `PGDATABASE`
- `PGUSER`
- `PGPASSWORD`

Optional backup settings:

- `CLEVERCHAT_BACKUP_DIR`: output directory, defaults to `./backups`
- `CLEVERCHAT_BACKUP_RETENTION_DAYS`: retention days, defaults to `14`, minimum `7`

Do not hard-code DB passwords, webhook URLs, SMTP secrets, API keys, or field
encryption key material in these scripts.

## Run

Windows PowerShell:

```powershell
.\cleverchat-pg-backup.ps1
```

Linux or systemd/cron wrapper:

```sh
./cleverchat-pg-backup.sh
```

The scripts create a timestamped PostgreSQL custom-format dump with:

```sh
pg_dump -Fc --no-owner --no-acl
```

Each dump is paired with a `.sha256` checksum file.

## Restore Rehearsal

Run restore rehearsal in a non-production PostgreSQL instance at least weekly.

1. Verify the dump file before restore.
   ```sh
   pg_restore --list cleverchat-example.dump
   ```
2. Restore into an empty rehearsal database.
   ```sh
   pg_restore --clean --if-exists --no-owner --no-acl --dbname "$PGDATABASE" cleverchat-example.dump
   ```
3. Start the application against the rehearsal database.
4. Verify Flyway schema history, table counts, admin login, scenario list, chat history, feedback, and crawl/search operation screens.
5. For encrypted chat fields, restore with the matching `CLEVERCHAT_FIELD_ENCRYPTION_KEY_BASE64`, `CLEVERCHAT_FIELD_ENCRYPTION_KEY_ID`, and any required previous keys.

## Deployment Use

- Run a fresh backup before production Flyway migrations.
- Run a fresh backup before data-destructive maintenance or encryption backfill/rotation maintenance.
- If Flyway fails, keep the failed database unchanged for diagnosis and restore only after the rollback decision is approved.
- Application rollback means redeploying the previous JAR/config pair. Database rollback is backup restore based; down migrations are not assumed.

## Follow-Up Scope

- External storage upload such as S3, NAS, or object storage
- Backup encryption at rest
- Success/failure notification integration
- systemd timer or Windows Task Scheduler registration files
