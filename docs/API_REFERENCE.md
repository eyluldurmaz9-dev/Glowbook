# GlowBook API Reference

Bu dokuman mevcut Spring Boot controller mapping'lerinden uretilmistir. Projede bu teslimde Swagger/OpenAPI dependency'si bulunmadigi icin yeni dependency eklenmemis, release riski azaltmak amaciyla sozlesme manuel olarak dokumante edilmistir.

Response alanlari icin DTO siniflari ve controller/service testleri kaynak kabul edilmelidir. Endpoint veya response uydurulmamali, degisiklik gerekiyorsa once backend sozlesmesi guncellenmelidir.

## Authentication

Base path: `/api/auth`

- `POST /register`: yeni kullanici kaydi.
- `POST /login`: access token ve refresh token uretir.
- `POST /refresh`: refresh token ile yeni access token akisi.

## Catalog and Admin Catalog

Public/catalog base path: `/api/catalog`

- `GET /services`: hizmet listesi.
- `GET /services/{serviceId}/options`: hizmet secenekleri.
- `GET /services/{serviceId}/packages`: hizmete bagli paketler.
- `GET /working-hours`: calisma saatleri.
- `GET /holidays`: tatil gunleri.

Admin base pathleri:

- `POST /api/admin/services`: hizmet olusturma.
- `PUT /api/admin/services/{serviceId}`: hizmet guncelleme.
- `DELETE /api/admin/services/{serviceId}`: hizmet silme.
- `POST /api/admin/services/{serviceId}/options`: hizmet secenegi olusturma.
- `PUT /api/admin/options/{optionId}`: hizmet secenegi guncelleme.
- `POST /api/admin/services/{serviceId}/packages`: paket olusturma.
- `PUT /api/admin/packages/{packageId}`: paket guncelleme.
- `POST /api/admin/working-hours`: calisma saati olusturma.
- `PUT /api/admin/working-hours/{workingHourId}`: calisma saati guncelleme.
- `POST /api/admin/holidays`: tatil gunu olusturma.

## Customers

Base path: `/api`

- `GET /admin/customers`: admin musteri listesi.
- `GET /customers/{customerId}`: musteri profili.
- `PUT /customers/{customerId}`: musteri profili guncelleme.
- `POST /customers/{customerId}/packages/{packageId}`: musteri paket satin alma/atama akisi.
- `GET /customers/{customerId}/packages`: musterinin paketleri.

## Employees

Base path: `/api/admin/employees`

- `GET /`: personel listesi.
- `POST /`: personel olusturma.
- `PUT /{employeeId}`: personel guncelleme.
- `DELETE /{employeeId}`: personel silme.
- `POST /services`: personel-hizmet eslestirme.
- `GET /services/{serviceId}`: hizmete atanmis personeller.
- `POST /leaves`: personel izin gunu olusturma.
- `GET /{employeeId}/leaves`: personel izin gunleri.

## Appointments

Base path: `/api/appointments`

- `GET /available-slots`: backend kaynakli uygun randevu slotlari.
- `POST /`: randevu olusturma.
- `GET /{appointmentId}`: randevu detayi.
- `GET /customer/{customerId}/upcoming`: yaklasan randevular.
- `GET /customer/{customerId}/past`: gecmis randevular.
- `GET /employee/{employeeId}`: personele ait randevular.
- `PATCH /{appointmentId}/approve`: randevu onaylama.
- `PATCH /{appointmentId}/complete`: randevu tamamlama.
- `PATCH /{appointmentId}/cancel`: randevu iptali.
- `PATCH /{appointmentId}/time`: randevu saat guncelleme.

## Waiting List

Base path: `/api/waiting-list`

- `POST /`: bekleme listesine ekleme.
- `GET /`: bekleme listesi.
- `GET /customer/{customerId}`: musterinin bekleme listesi kayitlari.
- `PATCH /{waitingListId}/cancel`: bekleme listesi kaydi iptali.
- `PATCH /{waitingListId}/converted`: bekleme listesi kaydini randevuya donusmus isaretleme.

## Notifications

Base path: `/api/notifications`

- `GET /customer/{customerId}`: musteri bildirimleri.
- `GET /customer/{customerId}/unread`: okunmamis musteri bildirimleri.
- `PATCH /{notificationId}/read`: bildirimi okundu yapma.

## Health

- `GET /actuator/health`
- `GET /health`

## Notlar

- Pagination, sorting ve filtering yalnizca ilgili endpoint tarafindan parametre olarak destekleniyorsa kullanilmalidir.
- Appointment availability ve waiting list kurallarinda backend tek kaynak kabul edilir.
- 401/403 cevaplari istemcide guvenli Turkce hata mesajlarina cevrilmelidir.
