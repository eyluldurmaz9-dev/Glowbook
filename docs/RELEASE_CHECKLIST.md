# GlowBook Backend Release Checklist

## Repository

- [x] `git status` checked.
- [x] Branch checked: `appmod/java-upgrade-20260721081103`.
- [x] Remote checked: `origin https://github.com/eyluldurmaz9-dev/Glowbook.git`.
- [x] Recent commits checked.
- [x] Untracked files checked before release commit.
- [x] Merge conflict entries checked.
- [x] Build/cache output tracking checked.
- [x] Push state checked.

## Security

- [x] `.env` and credential files are ignored.
- [x] `.env.example` contains placeholders only.
- [x] Production database, JWT and CORS configuration use environment variables.
- [x] Production CORS uses allowlist configuration, not wildcard configuration.
- [x] SMS fallback log masks sensitive data.
- [x] Secret pattern scan performed without printing secret values.

## Deployment

- [x] Production profile exists.
- [x] `PORT` environment variable supported.
- [x] Railway/Render build and start commands documented.
- [x] Health endpoint available for platform checks.
- [x] Java version documented as 25.
- [ ] Real MySQL production schema validated.
- [ ] Production migration runner decision finalized.
- [ ] Real SMS provider configured with platform secrets.

## Validation

- [x] `.\mvnw.cmd test` passed.
- [x] `.\mvnw.cmd verify` passed.
- [x] Prod profile smoke test passed with safe H2/MySQL-mode override.
