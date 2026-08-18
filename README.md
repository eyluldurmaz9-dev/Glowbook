# GlowBook Backend

GlowBook Backend, GlowBook randevu ve salon yonetim sisteminin Spring Boot tabanli REST API uygulamasidir. Flutter Android, iOS ve Web istemcileri JWT access token ve refresh token akisi ile bu API'ye baglanir.

## Proje Tanimi

Backend asagidaki is alanlarini yonetir:

- Authentication, JWT ve refresh token
- Role based authorization: customer, employee, admin
- Customer, employee ve admin operasyonlari
- Service, service option, service category ve package kataloglari
- Appointment creation, availability, cancel/update ve status akislari
- Waiting list
- Notifications
- Working hours, holidays, scheduler ve reminder altyapisi
- Production uyumlu environment based configuration

## Gereksinimler

- Java 25
- Maven Wrapper
- MySQL 8 veya uyumlu MySQL servisi
- Railway veya Render deployment icin Java destekli runtime

## MySQL Kurulumu

Lokal MySQL icin ornek database:

```sql
CREATE DATABASE glowbook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Kullanici ve yetkileri ortam politikalariniza gore olusturun. Database sifresi repository'ye commit edilmez.

## Environment Variables

Gercek secret ve credential degerleri commit edilmez. `.env.example` sadece gerekli anahtar isimlerini ve guvenli placeholder degerleri gosterir.

Zorunlu production degerleri:

- `SPRING_PROFILES_ACTIVE=prod`
- `PORT`
- Railway MySQL variables: `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`

Opsiyonel degerler:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` override degerleri
- `HIBERNATE_DDL_AUTO` (`update` varsayilan Railway production degeridir)
- `JWT_EXPIRATION_SECONDS`
- `JWT_REFRESH_EXPIRATION_DAYS`
- `REMINDER_CRON`
- SMS provider credential anahtarlari

Lokal calistirma ornegi:

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
$env:DB_URL='jdbc:mysql://localhost:3306/glowbook'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='local-password'
$env:JWT_SECRET='local-development-placeholder'
.\mvnw.cmd spring-boot:run
```

## Migration

Projede mevcut configuration JPA tabanli schema yonetimini kullanir. Production ortaminda otomatik schema degisiklikleri yerine kontrollu migration stratejisi onerilir. Flyway/Liquibase eklenmesi gerekiyorsa ayri bir teknik karar ve migration planiyla yapilmalidir.

## Test ve Build Komutlari

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

Cross-platform:

```bash
./mvnw test
./mvnw verify
```

Production profile smoke testi guvenli test database konfigurasyonu ile calistirilmalidir; gercek production database ile test calistirmayin.

## API Dokumantasyonu

Bu teslim paketinde Swagger/OpenAPI dependency'si projeye eklenmemistir; dependency churn ve release riski yaratmamak icin endpoint referansi controller sozlesmelerinden dokumante edilmistir.

Endpoint listesi: `docs/API_REFERENCE.md`

Public health endpointleri:

- `GET /actuator/health`
- `GET /health`

## Railway/Render Deployment

Production backend URL: `https://glowbook-production-7b59.up.railway.app`

Build command:

```bash
./mvnw verify
```

Start command:

```bash
java -Dserver.port=$PORT -jar target/glowbook-0.0.1-SNAPSHOT.jar
```

Deployment platformunda environment variables secret olarak tanimlanmalidir. Detayli adimlar `docs/DEPLOYMENT_BACKEND.md` dosyasindadir.

Railway MySQL attached ise uygulama `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER` ve `MYSQLPASSWORD` degiskenlerinden JDBC URL olusturur. `DB_URL`, `DB_USERNAME` veya `DB_PASSWORD` verilirse bu degerler Railway varsayilanlarini override eder. Frontend erisimi icin `CORS_ALLOWED_ORIGINS` degeri Vercel production domainini icermelidir.

Railway backend servisinde elle eklenmis `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` veya `SPRING_DATASOURCE_URL` varsa silin. GlowBook Railway'in MySQL plugin degiskenlerini kullanir; yanlis yazilmis bir datasource username degeri `Access denied for user '}root'` hatasiyla backend'i dusurur.

## Guvenlik Notlari

- Production CORS `CORS_ALLOWED_ORIGINS` ile allowlist olarak verilir.
- JWT secret environment variable ile gelir.
- Database password, SMS credential ve token degerleri commit edilmez.
- Default dev placeholder degerleri production icin kullanilmaz.
- Token, password ve kisisel veri loglanmamalidir.
- Admin, employee ve customer endpointlerinde rol kontrolleri backend tarafinda uygulanmalidir; UI gizleme tek basina guvenlik kabul edilmez.
