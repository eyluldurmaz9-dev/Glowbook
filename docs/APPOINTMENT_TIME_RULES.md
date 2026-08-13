# Randevu Zaman Kuralları

## İşletme saat dilimi

Backend'in yetkili saat dilimi `Europe/Istanbul` değeridir. Değer
`GLOWBOOK_BUSINESS_TIME_ZONE` ortam değişkeniyle değiştirilebilir. Kod içinde sabit
`+03:00` ofsetleri kullanılmaz. Spring tarafından sağlanan tek bir `Clock`, slot
listeleme ve randevu doğrulamasında ortak kullanılır.

## Geçmiş tarih ve aynı gün filtresi

- Geçmiş tarihler için uygunluk listesi boştur ve randevu oluşturma reddedilir.
- Bugün için başlangıç zamanı işletme saatine göre `şimdi` değerinden kesinlikle
  sonra olmayan slotlar listelenmez ve oluşturulamaz.
- Örnek: saat 13:25 ise 13:00 elenir, kurallar uygunsa ilk seçenek 14:00 olur.
- Gelecek günlerde sabah çalışma saatleri normal biçimde listelenir.

## Tam saat kuralı

Başlangıç dakikası ve saniyesi sıfır olmalıdır. `10:30` gibi değerler hem slot
çıktısından hem create/update doğrulamasından reddedilir.

## Personel uygunluğu

Slotlar yalnızca aktif, seçilen hizmet/seçenek için yetkin, izinli olmayan ve
çalışma saatleri içindeki personel için üretilir. Her personel kendi adı ve ayrı
slotuyla döner. PENDING ve APPROVED randevular aynı personel/tarih/saat slotunu
bloke eder.

## Misafir ve paket akışları

Misafir, kayıtlı müşteri ve `customerPackageId` kullanan randevular aynı
`AppointmentService.create` ve `AppointmentAlgorithmService.validateSlot`
zincirinden geçer. Paket oturumu zaman doğrulamasını atlayamaz.

## Eski slot yeniden kontrolü

Flutter son onaydan hemen önce `/api/appointments/available-slots` verisini
yeniden ister. Slot kaybolmuşsa hizmet, tarih ve personel korunur; saat temizlenip
kullanıcı saat seçimine döndürülür. Backend create işlemi de aynı slotu tekrar
doğruladığından yarış durumunda 409 döndürür.

## Test saati stratejisi

Backend bir `Clock` bağımlılığı kullanır. Birim testleri
`2026-08-13 13:25 Europe/Istanbul` anına sabitlenir; böylece dün, bugünün geçmiş
saati, sonraki tam saat ve yarının sabah slotları deterministik doğrulanır.
