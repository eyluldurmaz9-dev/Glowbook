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
| `PORT` | Evet | Railway tarafindan verilir; uygulama `server.port=${PORT:8080}` ile dinler. |
| `MYSQLHOST` | Railway icin evet | Railway MySQL host degeri. |
| `MYSQLPORT` | Railway icin evet | Railway MySQL port degeri. |
| `MYSQLDATABASE` | Railway icin evet | Railway MySQL database adi. |
| `MYSQLUSER` | Railway icin evet | Railway MySQL kullanicisi. |
| `MYSQLPASSWORD` | Railway icin evet | Railway MySQL sifresi, secret olarak tutulur. |
| `DB_URL` | Hayir | Verilirse `MYSQL*` degerlerinden uretilen JDBC URL yerine kullanilir. |
| `DB_USERNAME` | Hayir | Verilirse `MYSQLUSER` yerine kullanilir. |
| `DB_PASSWORD` | Hayir | Verilirse `MYSQLPASSWORD` yerine kullanilir. |
| `JWT_SECRET` | Evet | Uzun, rastgele secret. |
| `CORS_ALLOWED_ORIGINS` | Evet | Virgulle ayrilmis Flutter Web domain allowlist. |
| `HIBERNATE_DDL_AUTO` | Hayir | Varsayilan `update`; schema hazirsa `validate` yapilabilir. |
| `JWT_EXPIRATION_SECONDS` | Hayir | Varsayilan prod degeri 900. |
| `JWT_REFRESH_EXPIRATION_DAYS` | Hayir | Varsayilan 30. |
| `REMINDER_CRON` | Hayir | Scheduler cron degeri. |
| `SMS_PROVIDER` | Hayir | Gercek sender eklenirse kullanilir. |
| `SMS_API_KEY` | Hayir | Platform secret olmali. |
| `SMS_API_SECRET` | Hayir | Platform secret olmali. |

## Railway

1. Repository'yi Railway projesine baglayin.
2. Java 25 destekli build image kullandiginizi dogrulayin.
3. Railway MySQL servisini backend servisine attach edin; Railway `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD` ve `MYSQL_URL` degiskenlerini saglar.
4. Backend servisine `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET` ve `CORS_ALLOWED_ORIGINS` ekleyin.
5. Build command olarak `./mvnw verify` kullanin.
6. Start command olarak `java -Dserver.port=$PORT -jar target/glowbook-0.0.1-SNAPSHOT.jar` kullanin.

Railway attached MySQL ile ek `DB_URL` yazmak zorunlu degildir. Uygulama su formda JDBC URL olusturur:

```text
jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

Manuel override gerekiyorsa `DB_URL`, `DB_USERNAME` ve `DB_PASSWORD` backend service variables olarak eklenebilir.

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

Prod profil `spring.jpa.hibernate.ddl-auto=${HIBERNATE_DDL_AUTO:update}` kullanir. Railway MySQL yeni ve bos geldiginde uygulamanin ayakta kalmasi icin varsayilan `update` degeridir. Schema tamamen yonetilen hale geldiginde `HIBERNATE_DDL_AUTO=validate` yapilip migration adimi CI/CD surecine alinabilir.

## Logging ve SMS

Mevcut SMS sender log tabanli fallback davranisi kullanir. Gercek SMS provider credential bilgileri repository'ye yazilmamali, sadece deployment platform secret olarak tutulmalidir.
