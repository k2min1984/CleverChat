# M6 Log Collection Manifest

This manifest defines the CleverChat log files that an external collector may read.
It does not introduce an ELK/OpenSearch/Loki server dependency inside the app.

## Baseline

- Runtime profiles: `stage`, `prod`
- Default log directory: `/var/log/cleverchat`
- Filebeat sample: `3.개발/cleverchat/deploy/filebeat-cleverchat.yml`
- Application logback config: `3.개발/cleverchat/src/main/resources/logback-spring.xml`
- Collector ownership: operations team or deployment automation

## Channels

| File | Format | Retention | Purpose | Main Fields |
| --- | --- | --- | --- | --- |
| `app.log` | Text | 90 days | General application logs | timestamp, thread, level, logger, message |
| `api.log` | JSON line | 90 days | `/admin/api/**` and `/chat/api/**` access logs | timestamp, event, requestId, method, path, status, durationMs, actor, remoteAddrHash, userAgentHash |
| `security.log` | JSON line | 365 days | Authentication, authorization, CSRF, login lock events | timestamp, event, requestId, type, username, path, remoteAddrHash, userAgentHash |
| `error.log` | Text | 180 days | ERROR level application logs | timestamp, thread, level, logger, message |
| `slow-query.log` | JSON line | 90 days | MyBatis statements over the configured threshold | timestamp, event, requestId, statementId, operation, durationMs, success |

## Sensitive Data Policy

The collector must not add request bodies, response bodies, query strings, raw IP
addresses, raw User-Agent values, PII, SMTP secrets, webhook URLs, API keys, or
field-encryption key material to collected events.

Application JSON channels already use hashed network identifiers where needed.
External collectors should preserve that policy and should not enrich logs with
raw client network data unless a separate security review approves it.

## Correlation Procedure

1. Start with `api.log` and find the failing `requestId`, `path`, `status`, and `durationMs`.
2. Check `error.log` for the same timestamp window and application exception context.
3. Check `slow-query.log` for the same `requestId` when the API duration is high.
4. Check `security.log` for the same `requestId` when the failure is authentication,
   authorization, CSRF, login lock, or session related.

## Follow-Up Scope

- ELK/OpenSearch/Loki server provisioning
- Dashboards and alert rules
- Long-term archive storage
- Fluent Bit or Vector deployment samples
- Provider-specific collector hardening
