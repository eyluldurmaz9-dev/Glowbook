# GlowBook Backend Local Setup

## On Kosullar

- Java 25
- Maven Wrapper
- MySQL 8 veya uyumlu MySQL servisi

## Database

```sql
CREATE DATABASE glowbook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## Environment

PowerShell ornegi:

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
$env:DB_URL='jdbc:mysql://localhost:3306/glowbook'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='local-password'
$env:JWT_SECRET='local-development-placeholder'
```

## Calistirma

```powershell
cd glowbook
.\mvnw.cmd spring-boot:run
```

## Test ve Verify

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

## Flutter Baglantisi

Android emulator:

```powershell
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

Flutter Web:

```powershell
flutter run -d chrome --dart-define=API_BASE_URL=http://localhost:8080
```
