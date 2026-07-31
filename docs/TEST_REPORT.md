# GlowBook Backend Test Report

Tarih: 2026-07-31

## Calistirilan Komutlar

| Komut | Sonuc | Not |
| --- | --- | --- |
| `.\mvnw.cmd test` | Basarili | 4 test calisti, 0 hata, 0 failure. |
| `.\mvnw.cmd verify` | Basarili | Testler ve Spring Boot jar repackage basarili. |
| `.\mvnw.cmd test -Dspring.profiles.active=prod -Dspring.jpa.hibernate.ddl-auto=create-drop` | Basarili | Prod profil H2 MySQL mode ve surece ozel env secret ile smoke test edildi. Gercek MySQL prod sema validasyonu degildir. |

## Test Kapsami

- Spring context startup testi.
- Auth service refresh/login davranisi icin unit test.
- Authorization helper/role support testleri.
- Prod profil smoke testi environment degiskenlerinin baglanabildigini ve context'in acildigini dogruladi.

## Notlar

- Mockito inline mock maker JDK gelecegi icin dynamic agent uyarisi verdi. Testler basarili; ileride Mockito Java agent konfigurasyonu eklenmeli.
- Test ortaminda H2 kullaniliyor. Gercek MySQL entegrasyon testi CI icin ayri profile veya Testcontainers ile guclendirilmeli.
