# Personel hizmet yetkinlikleri

## Veri modeli

GlowBook mevcut `employee_services` ara tablosunu kullanır; rakip bir ilişki
modeli oluşturulmamıştır. Tablo `employee_id`, `service_id` ve artık isteğe bağlı
`option_id` içerir. Yeni yönetim işlemleri alt hizmet (`ServiceOption`) düzeyinde
kayıt oluşturur. Eski `option_id = NULL` satırları üretim verisini bozmamak için
ana hizmetin tüm alt hizmetlerini kapsayan geriye uyumlu atama olarak yorumlanır.

Prod profilinde mevcut `spring.jpa.hibernate.ddl-auto=update` ayarı nullable
`option_id` kolonunu ve foreign key'i veri silmeden ekler. Tablo veya geçmiş
kayıtlar yeniden oluşturulmaz. Deploy öncesinde MySQL yedeği alınması ve deploy
sonrasında `employee_services.option_id` kolonunun doğrulanması önerilir.

## API

- `GET /api/admin/employees/{employeeId}/services`: mevcut yetkinlikler
- `PUT /api/admin/employees/{employeeId}/services`: `optionIds` kümesini atomik değiştirir
- `GET /api/catalog/services/{serviceId}/options/{optionId}/employees`: aktif ve yetkin personel
- `GET /api/appointments/available-slots`: `serviceId`, `optionId`, `date` ister

Personel oluşturma/güncelleme isteği de `optionIds` alanını kabul eder. Yinelenen
ID'ler küme olarak tekilleştirilir; bulunmayan/pasif hizmet seçenekleri reddedilir.

## Randevu doğrulaması

Flutter seçilen alt hizmeti slot isteğine gönderir. Backend yalnızca aktif ve o
alt hizmete atanmış çalışanları döndürür. Manuel olarak uyumsuz çalışan gönderimi
`400` ve `Seçilen personel bu hizmeti vermiyor.` mesajıyla reddedilir. Flutter bu
onaylı Türkçe mesajı kullanıcıya gösterir ve hata halinde sahte personel/slot
fallback verisi kullanmaz.

Demo modunda `DEMOEMP`, katalogdaki ilk üç aktif ana hizmetin alfabetik ilk aktif
alt hizmetine atanır. Bütün hizmetler otomatik verilmez.
