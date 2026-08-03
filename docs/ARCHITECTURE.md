# GlowBook Backend Architecture

## Genel Yapi

Backend Spring Boot REST API olarak organize edilmistir. Ana katmanlar entity, repository, service, DTO, controller, security ve exception handling yapilaridir.

## Katmanlar

- Entity: JPA domain modelleri.
- Repository: Spring Data JPA veri erisimi.
- Service: Is kurallari, appointment availability ve state transition mantigi.
- DTO: API request/response sinirlari.
- Controller: REST endpointleri.
- Security: JWT, role authorization ve CORS configuration.
- Scheduler: reminder ve periyodik is akislari.

## Veritabani

Production hedefi MySQL'dir. Lokal veya test ortaminda farkli guvenli test konfigurasyonlari kullanilabilir. Production schema yonetimi icin kontrollu migration stratejisi onerilir.

## Istemci Sozlesmesi

Flutter istemcisi appointment availability, waiting list, appointment status ve rol yetkilerini backend'den gelen gercek cevaplara gore isler. UI tarafinda backend yerine is kurali tahmini yapilmamalidir.

## Guvenlik

- JWT secret environment variable'dan okunur.
- Role kontrolleri backend endpointlerinde uygulanir.
- CORS production'da allowlist ile sinirlanir.
- Token, password ve credential degerleri loglanmaz.
