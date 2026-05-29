package kr.co.cleverchat.common.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BackupReadinessTest {

    private static final Path BACKUP_DIR = Path.of("deploy/backup");

    @Test
    void backupScriptsUsePgDumpCustomFormatAndEnvironmentOnlyConnection() throws IOException {
        String powershell = Files.readString(BACKUP_DIR.resolve("cleverchat-pg-backup.ps1"));
        String shell = Files.readString(BACKUP_DIR.resolve("cleverchat-pg-backup.sh"));

        assertThat(powershell).contains("pg_dump -Fc --no-owner --no-acl");
        assertThat(shell).contains("pg_dump -Fc --no-owner --no-acl");

        for (String requiredEnv :
                new String[] {"PGHOST", "PGPORT", "PGDATABASE", "PGUSER", "PGPASSWORD"}) {
            assertThat(powershell).contains(requiredEnv);
            assertThat(shell).contains(requiredEnv);
        }

        assertThat(powershell)
                .contains("Get-FileHash -Algorithm SHA256", "CLEVERCHAT_BACKUP_RETENTION_DAYS");
        assertThat(shell)
                .contains("sha256sum", "CLEVERCHAT_BACKUP_RETENTION_DAYS", "find \"$BACKUP_DIR\"");
    }

    @Test
    void backupSamplesDoNotHardCodeSecretsOrRealHosts() throws IOException {
        String combined =
                Files.readString(BACKUP_DIR.resolve("cleverchat-pg-backup.ps1"))
                        + "\n"
                        + Files.readString(BACKUP_DIR.resolve("cleverchat-pg-backup.sh"))
                        + "\n"
                        + Files.readString(BACKUP_DIR.resolve("README.md"));
        String normalized = combined.toLowerCase();

        assertThat(normalized)
                .doesNotContain(
                        "postgres://",
                        "jdbc:postgresql://",
                        "password=",
                        "apikey",
                        "api_key:",
                        "smtp_password",
                        "webhook_url",
                        "cleverchat_field_encryption_key_base64=");
    }

    @Test
    void runbookDocumentsRestoreAndEncryptionKeyPairing() throws IOException {
        String readme = Files.readString(BACKUP_DIR.resolve("README.md"));
        String opsMemo = Files.readString(Path.of("../../2.설계/06.운영메모/M6_운영관측_운영메모.md"));
        String nfr = Files.readString(Path.of("../../2.설계/04.아키텍처/NFR_비기능요구사항.md"));

        assertThat(readme)
                .contains(
                        "pg_restore --list",
                        "Run a fresh backup before production Flyway migrations",
                        "CLEVERCHAT_FIELD_ENCRYPTION_KEY_BASE64",
                        "Restore Rehearsal");
        assertThat(opsMemo)
                .contains(
                        "Backup Automation Agent Baseline",
                        "pg_dump -Fc --no-owner --no-acl",
                        "pg_restore --list",
                        "CLEVERCHAT_FIELD_ENCRYPTION_KEY_BASE64");
        assertThat(nfr).contains("RPO target is 24 hours", "OS scheduler", "application process");
    }
}
