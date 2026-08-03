# GlowBook Project Overview

GlowBook backend, Flutter istemcilerine randevu, katalog, kullanici, rol ve bildirim servisleri sunan Spring Boot REST API uygulamasidir.

## Repository Ayrimi

- `glowbook-backend`: Spring Boot backend ve is kurallari.
- `glowbook-flutter`: Android, iOS ve Flutter Web istemcisi.
- `glowbook-visualAssentManager-ui`: yalnizca UI/UX tasarim referansi.

Backend deposu tasarim veya Flutter uygulama kodunu icermez. UI referansi yalnizca sozlesme ve tasarim dogrulama surecinde baglamsal kaynak olarak kullanilir.

## Teslim Kapsami

- Authentication, JWT ve refresh token.
- Role based authorization.
- Catalog, customer, employee, admin, appointment, waiting list ve notification API'leri.
- Health endpointleri ve production environment dokumantasyonu.
- Test, verify, deployment ve final delivery belgeleri.

## Sinirlar

- SMS icin gercek credential gerektiren akislarda mock basari uretilmez.
- Production database, real SMS provider ve cloud deployment manuel dogrulama ister.
- Swagger/OpenAPI dependency'si bu teslimde eklenmemistir; API referansi `docs/API_REFERENCE.md` icindedir.
