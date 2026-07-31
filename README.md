# GlowBook Backend

GlowBook backend Spring Boot ve MySQL tabanli REST API uygulamasidir. Flutter istemcileri JWT access token ve refresh token akisi ile bu API'ye baglanir.

## Gereksinimler

- Java 25
- Maven Wrapper
- MySQL

## Lokal Calistirma

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

Development profili:

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
$env:DB_URL='jdbc:mysql://localhost:3306/glowbook'
$env:DB_USERNAME='root'
$env:DB_PASSWORD=''
$env:JWT_SECRET='local-only-change-me'
.\mvnw.cmd spring-boot:run
```

## Production Environment

Gercek secret ve credential degerleri commit edilmez. `.env.example` sadece anahtar isimlerini ve placeholder degerleri gosterir.

Zorunlu environment variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `PORT`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`

Opsiyonel:

- `JWT_EXPIRATION_SECONDS`
- `JWT_REFRESH_EXPIRATION_DAYS`
- `REMINDER_CRON`
- SMS provider credential anahtarlari, gercek SMS sender eklendiginde platform secret olarak tutulmalidir.

## Deployment

Railway veya Render uzerinde start command:

```bash
java -Dserver.port=$PORT -jar target/glowbook-0.0.1-SNAPSHOT.jar
```

Build command:

```bash
./mvnw verify
```

Detaylar: `docs/DEPLOYMENT_BACKEND.md`

## Health

Public health endpointleri:

- `GET /actuator/health`
- `GET /health`

## Guvenlik

- Production CORS `CORS_ALLOWED_ORIGINS` ile allowlist olarak verilir.
- JWT secret environment variable ile gelir.
- Database password commit edilmez.
- Default dev secret production icin kullanilmaz.
- Token, password ve SMS credential loglanmamalidir.
