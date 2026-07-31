# Backend Release Readiness

## Result

READY WITH LIMITATIONS

## Evidence

- `.\mvnw.cmd test`: passed.
- `.\mvnw.cmd verify`: passed.
- Production profile smoke test with safe H2/MySQL-mode override: passed.
- Repository status was clean before final docs were added.
- No real secret file was detected.

## Limitations

- Real production MySQL schema was not validated in this environment.
- Real SMS provider is not configured.
- Migration execution strategy is not fully automated by Flyway/Liquibase.
- Test coverage is useful but still narrow for release-grade backend confidence.

## Required Before Production

1. Provision production MySQL.
2. Apply schema/migrations.
3. Start with `SPRING_PROFILES_ACTIVE=prod`.
4. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` and `PORT`.
5. Configure SMS provider credentials in the deployment platform secret manager.
6. Run API smoke tests against the deployed backend.
