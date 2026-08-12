# Role testing

Demo accounts are created only when `ENABLE_DEMO_USERS=true`. Passwords are mandatory environment secrets:

- `admin@glowbook.test` / `DEMO_ADMIN_PASSWORD`
- `employee@glowbook.test` / `DEMO_EMPLOYEE_PASSWORD`
- `customer@glowbook.test` / `DEMO_CUSTOMER_PASSWORD`

No plaintext password is present in source. The employee demo account is a real active employee assigned to every active service. Disable the flag after testing; disabling does not automatically delete existing demo rows, so remove/deactivate them through an authorized operational process if they were enabled in production.

Login uses `POST /api/auth/login` with `username`, `password` and requested `role`. The JWT role comes from the stored account type/employee role, never from registration or an unchecked client request. Normal registration always returns CUSTOMER.

## Yetki matrisi

`DemoUserIntegrationTest` aşağıdaki davranışları otomatik doğrular:

- anonim kullanıcı → korumalı endpoint: `401`
- müşteri → yönetici endpointi: `403`
- müşteri → personel endpointi: `403`
- personel → yalnızca yönetici endpointi: `403`
- personel → personel program endpointi: başarılı
- yönetici → yönetim endpointi: başarılı
- müşteri → müşteri rolüyle giriş: başarılı
- yanlış parola: güvenli hata
- yanlış giriş türü: anlaşılır Türkçe rol hatası
- manipüle edilmiş kayıt isteğindeki `ADMIN` rolü: yok sayılır, sonuç `CUSTOMER`
- demo personel: gerçek `DEMOEMP` çalışan kaydına ve aktif hizmetlere bağlı

Ayrı `DemoUserConfigConditionTest`, özellik false olduğunda veya hiç
tanımlanmadığında demo kullanıcı yapılandırmasının yüklenmediğini doğrular.

Railway kurulum ve manuel giriş adımları için `docs/DEMO_ACCOUNTS.md` dosyasına
bakın.
