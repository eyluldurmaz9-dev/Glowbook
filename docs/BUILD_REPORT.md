# GlowBook Backend Build Report

Tarih: 2026-07-31

## Build Sonuclari

- `.\mvnw.cmd test`: Basarili.
- `.\mvnw.cmd verify`: Basarili.
- Jar repackaging basarili: `target/glowbook-0.0.1-SNAPSHOT.jar`.

## Production Profile

- `application-prod.properties` environment degiskenleri bekliyor:
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `JWT_SECRET`
  - Opsiyonel `JWT_EXPIRATION_SECONDS`, `JWT_REFRESH_EXPIRATION_DAYS`, `REMINDER_CRON`
- Guvenli smoke test H2 MySQL mode ile calistirildi ve basarili oldu.
- Gercek production baslangici icin MySQL instance, gecerli schema, secret yonetimi ve migration/validate zinciri gereklidir.

## Release Notlari

- Prod profil `spring.jpa.hibernate.ddl-auto=validate` kullaniyor; bu dogru release davranisidir ancak schema migration sureci CI/CD tarafinda garanti altina alinmalidir.
- Varsayilan local JWT secret sadece dev/test icindir; production'da mutlaka environment secret verilmelidir.
