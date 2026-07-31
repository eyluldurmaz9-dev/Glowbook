# Backend Deployment

## Hedef Platform

GlowBook backend Railway veya Render uzerinde calisabilecek sekilde hazirlandi.

## Java ve Build

- Java version: 25
- Build command:

```bash
./mvnw verify
```

- Start command:

```bash
java -Dserver.port=$PORT -jar target/glowbook-0.0.1-SNAPSHOT.jar
```

Windows lokal build:

```powershell
.\mvnw.cmd verify
```

## Environment Variables

| Key | Zorunlu | Not |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Evet | Production icin `prod`. |
| `PORT` | Evet | Railway/Render tarafindan verilir. |
| `DB_URL` | Evet | MySQL JDBC URL. |
| `DB_USERNAME` | Evet | MySQL kullanicisi. |
| `DB_PASSWORD` | Evet | MySQL sifresi, secret olarak tutulur. |
| `JWT_SECRET` | Evet | Uzun, rastgele secret. |
| `CORS_ALLOWED_ORIGINS` | Evet | Virgulle ayrilmis Flutter Web domain allowlist. |
| `JWT_EXPIRATION_SECONDS` | Hayir | Varsayilan prod degeri 900. |
| `JWT_REFRESH_EXPIRATION_DAYS` | Hayir | Varsayilan 30. |
| `REMINDER_CRON` | Hayir | Scheduler cron degeri. |
| `SMS_PROVIDER` | Hayir | Gercek sender eklenirse kullanilir. |
| `SMS_API_KEY` | Hayir | Platform secret olmali. |
| `SMS_API_SECRET` | Hayir | Platform secret olmali. |

## Railway

1. Repository'yi Railway projesine baglayin.
2. Java 25 destekli build image kullandiginizi dogrulayin.
3. Environment variables listesini Railway Variables alanina ekleyin.
4. Build command olarak `./mvnw verify` kullanin.
5. Start command olarak `java -Dserver.port=$PORT -jar target/glowbook-0.0.1-SNAPSHOT.jar` kullanin.

## Render

1. New Web Service olusturun.
2. Build command: `./mvnw verify`
3. Start command: `java -Dserver.port=$PORT -jar target/glowbook-0.0.1-SNAPSHOT.jar`
4. Environment variables listesini Render dashboard uzerinden girin.

## CORS

Production'da wildcard origin kullanilmaz. Ornek:

```text
CORS_ALLOWED_ORIGINS=https://glowbook.example.com,https://glowbook.vercel.app
```

## Health Check

Railway/Render health check path olarak su degerlerden biri kullanilabilir:

```text
/actuator/health
```

veya

```text
/health
```

## Database Migration

`src/main/resources/db/migration` altinda SQL migration dosyasi bulunur; ancak mevcut `pom.xml` icinde Flyway veya Liquibase runtime dependency yoktur. Production icin iki guvenli secenek vardir:

- Flyway/Liquibase dependency ekleyip migrationlari uygulama startup zincirine baglamak.
- Veritabani migrationlarini CI/CD veya database release adiminda manuel ve versioned olarak uygulamak.

Prod profil `spring.jpa.hibernate.ddl-auto=validate` kullandigi icin schema uygulama baslamadan once hazir olmalidir.

## Logging ve SMS

Mevcut SMS sender log tabanli fallback davranisi kullanir. Gercek SMS provider credential bilgileri repository'ye yazilmamali, sadece deployment platform secret olarak tutulmalidir.
