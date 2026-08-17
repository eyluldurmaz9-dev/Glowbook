# WhatsApp Randevu Bildirimleri

## Ozet

Randevu **basariyla olusturuldugunda** ("Randevunuz olusturuldu") musteriye/misafire
gonderilen bildirim artik SMS yerine resmi **Meta WhatsApp Business Platform / Cloud
API** uzerinden gonderiliyor. Bu, kayitli musteri, misafir, paket satin alma + ilk
randevu ve paketten sonraki randevu rezervasyonlarinin **hepsi icin** gecerlidir; hepsi
ayni `AppointmentService.create()` metodundan gectigi icin tek bir entegrasyon noktasi
yeterli oldu.

Randevu onaylama (`approve`), iptal (`cancel`), saat degisikligi (`updateTime`) ve
ertesi gun hatirlatma cron'u (`ReminderScheduler`) **degistirilmedi** ve SMS uzerinden
gonderilmeye devam ediyor — gorev kapsami sadece "randevu olusturuldu" onay mesaji
icindi ve bu dorduncusu icin onaylanmis bir WhatsApp sablonu/icerigi verilmedi. Ayni
anda hem SMS hem WhatsApp gonderilen tek bir olay yok: her `NotificationType` tam olarak
bir kanaldan gecer.

## Mevcut durum: WHATSAPP_ENABLED=false

GlowBook henuz gercek bir kuafor/güzellik salonu tarafindan canli kullanilmiyor. Bu
yuzden varsayilan (ve su anki) production konfigurasyonu:

```
WHATSAPP_ENABLED=false
```

Bu durumda:
- Randevu olusturma **tamamen normal calisir** (hicbir sey bozulmaz).
- `WhatsAppNotificationService.sendAppointmentConfirmation` cagirilir ama ilk satirda
  `whatsAppEnabled == false` oldugu icin **hicbir sey yapmadan** (WhatsApp saglayicisina
  hic dokunmadan, Notification satiri bile olusturmadan) geri doner.
- Gercek Meta credential'lari, onayli sablon vb. **gerekmez**.

Gercek bir salon GlowBook'u kullanmaya basladiginda tek yapilmasi gereken, asagidaki
adimlari tamamlayip Railway'de `WHATSAPP_ENABLED=true` ve `WHATSAPP_PROVIDER=meta`
ayarlamak (ve servisi yeniden deploy etmek).

## Mimari

```
Randevu basariyla commit edildi
  -> AppointmentSmsEvent (APPOINTMENT_CREATED) [@TransactionalEventListener AFTER_COMMIT]
  -> AppointmentSmsEventListener
       - type == APPOINTMENT_CREATED  -> WhatsAppNotificationService
       - digerleri (APPROVED/CANCELLED/UPDATED/REMINDER) -> NotificationService (SMS, degismedi)
  -> WhatsAppNotificationService.sendAppointmentConfirmation(event)
       - whatsAppEnabled=false ise: no-op
       - aksi halde: telefonu normalize et, Notification satiri olustur (channel=WHATSAPP,
         status=PENDING), WhatsAppSender.sendTemplate(...) cagir, sonuca gore
         SENT/FAILED olarak isaretleyip kaydet
  -> WhatsAppSender (arayuz)
       - LoggingWhatsAppSender (varsayilan, provider=log): hicbir dis istek atmaz,
         sadece loglar, sahte "accepted" sonucu doner
       - MetaWhatsAppSender (provider=meta): gercek Graph API cagrisi
```

`AppointmentService` hicbir zaman ham HTTP/Meta kodu icermiyor — sadece
`ApplicationEventPublisher` uzerinden event yayinliyor, tipki SMS'te oldugu gibi.
`WhatsAppNotificationService`/`WhatsAppSender` ayrimi, `NotificationService`/`SmsSender`
ayrimiyla ayni desendir (bkz. `SmsSender.java`, `TwilioSmsSender.java`).

### Neden `NotificationService`'e eklemek yerine ayri bir sinif?

`NotificationService.createAndSendAfterCommit` SMS icin degismeden kaldi; approve,
cancel, update ve reminder akislari bu metodu hâlâ kullaniyor. WhatsApp mantigi
`WhatsAppNotificationService` icinde ayri tutuldugu icin SMS tarafinda **hicbir satir**
degismedi — regresyon riski sifira indirildi.

### Islemsel guvenlik (transaction safety)

`WhatsAppNotificationService.sendAppointmentConfirmation` ve
`recordDeliveryStatusUpdate`, randevu islemi zaten commit olduktan SONRA,
`Propagation.REQUIRES_NEW` ile **ayri** bir transaction'da calisir (SMS ile birebir
ayni desen). Bu sayede:
- WhatsApp gonderimi asla randevu olusturmadan once tetiklenmez (once commit, sonra
  bildirim).
- WhatsApp saglayicisi hata verirse/timeout olursa, bu sadece o Notification satirini
  `FAILED` isaretler — randevu **asla geri alinmaz**.

### Ayni bildirim iki kez gonderilir mi?

Hayir. `NotificationRepository.existsByAppointmentAppointmentIdAndType(appointmentId,
APPOINTMENT_CREATED)` kontrolu (SMS'te zaten var olan ayni mekanizma) her gonderimden
once calisir: bu randevu/tip icin zaten bir bildirim satiri varsa ikinci kez gonderilmez.
Ayrica bir slot icin ayni anda iki basarili randevu olusturulamayacagi icin (ikinci
istek 409 Conflict alir) zaten en fazla bir commit, dolayisiyla en fazla bir event, en
fazla bir gonderim olur.

## Telefon normalizasyonu

Yeni bir bilesen eklenmedi — mevcut `TurkishPhoneNumberService.normalize(...)` yeniden
kullanildi. `05xxxxxxxxx`, `5xxxxxxxxx`, `+905xxxxxxxxx`, bosluklu/tireli varyasyonlari
`+905xxxxxxxxx` haline getirir; gecersiz numaralari musteri dostu bir hata ile
reddeder. `Appointment.phone` zaten `AppointmentService.create()` icinde bu servisle
normalize edilip kaydediliyor; WhatsApp gonderimi de ayni degeri (idempotent sekilde)
tekrar normalize ederek kullanir.

## Sablon (template) stratejisi

Sablon adi/dili kod icinde **hicbir yerde hardcode edilmedi** — konfigurasyondan okunur:

```
whatsapp.template-name=${WHATSAPP_TEMPLATE_NAME:appointment_confirmation_tr}
whatsapp.template-language=${WHATSAPP_TEMPLATE_LANGUAGE:tr}
```

Onerilen sablon govdesi (Meta'da onaylatilacak):

```
GlowBook randevunuz olusturuldu.
Tarih: {{1}}
Saat: {{2}}
Hizmet: {{3}}
Uzman: {{4}}
Gorusmek uzere.
```

Gonderilen parametreler sirasiyla: **tarih** ("17 Ağustos 2026" formatinda, Turkce ay
adiyla), **saat** ("14:00"), **gercek hizmet adi**, **gercek atanan personelin tam adi**
(asla "2 uzman" gibi anonim bir deger degil — `Appointment.employee` alanindan direkt
okunur). Tum tarih/saat degerleri GlowBook'un yetkili is saati dilimini
(`glowbook.business-time-zone`, varsayilan `Europe/Istanbul`) kullanan
`Appointment.appointmentDate`/`appointmentTime` alanlarindan gelir — UTC veya teknik
zaman damgasi musteriye hicbir zaman gosterilmez.

## Musteriye gorunen metin

Backend hic bir zaman "Cloud API", "Graph API", "HTTP", "Meta token", "webhook",
"provider" gibi teknik terimler icermez. Basari/hata durumlarinda musteriye uygun
Turkce ifadeler:

- Basarili: "Randevu bilgilerin WhatsApp uzerinden gonderildi."
- Basarisiz (randevu yine de olusturulur): "Randevun olusturuldu ancak WhatsApp
  bilgilendirmesi su anda gonderilemedi."

Bu metinler su an backend tarafinda ayri bir alan olarak API yanitina eklenmiyor —
randevu yaniti degismedi (geriye donuk uyumluluk); bildirim durumu
`GET /api/notifications/customer/{id}` uzerinden okunabilir. Flutter tarafinda SMS'e
referans veren herhangi bir metin bulunmadigi icin (kontrol edildi, sifir sonuc)
degistirilecek bir ekran metni yoktu.

## Bildirim kaydi (Notification entity)

`notifications` tablosuna eklenen yeni kolonlar (mevcut `sms_sent` kolonuna dokunulmadi):

| Kolon | Aciklama |
| --- | --- |
| `channel` | `SMS` (varsayilan, eski satirlarla uyumlu) veya `WHATSAPP` |
| `template` | Kullanilan Meta sablon adi (yalniz WhatsApp) |
| `provider_message_id` | Meta'nin dondurdugu mesaj id'si — webhook korelasyonu icin |
| `delivery_status` | `PENDING` / `SENT` / `DELIVERED` / `READ` / `FAILED` |
| `sent_at`, `delivered_at`, `read_at`, `failed_at` | Ilgili durum gecisinin zamani |

Sema `spring.jpa.hibernate.ddl-auto=update` ile otomatik olusturulur (bu repoda
Flyway/Liquibase kullanilmiyor, `src/main/resources/db/migration` klasoru pom.xml'de
bagli degil); ayrica bir migration adimi gerekmez.

**Onemli:** hicbir zaman access token/app secret bu tabloya veya baska bir DB
alanina yazilmaz. Loglarda telefon numaralari maskelenir (`****1234` formati,
`LoggingWhatsAppSender`/`TwilioSmsSender` ile ayni yontem).

## Teslimat durumu: "gonderildi" != "ulasti"

Cloud API'ye POST atip 2xx yaniti almak sadece Meta'nin mesaji **kabul ettigini**
kanitlar (`delivery_status=SENT`). Gercek `DELIVERED`/`READ` durumu sadece Meta'nin
webhook'u araciligiyla gelir. Kod hicbir zaman "gonderildi" ile "teslim edildi"ni
karistirmaz.

## Meta kurulumu (adim adim)

1. **Meta Developer hesabi ve App**: https://developers.facebook.com uzerinden bir
   Meta hesabi olusturun, "Business" turunde yeni bir App yaratin.
2. **WhatsApp urunu ekleyin**: App dashboard'unda "Add Product" -> "WhatsApp" ile
   WhatsApp Business Platform / Cloud API'yi App'e baglayin.
3. **WhatsApp Business Account (WABA)**: App'e bagli bir WhatsApp Business Account
   olusturun/secin. `WHATSAPP_BUSINESS_ACCOUNT_ID` bu hesabin id'sidir (mesaj
   gonderirken zorunlu degildir, sablon yonetimi/Meta destegi icin faydalidir).
4. **Telefon numarasi**: WABA'ya bir telefon numarasi ekleyin (test asamasinda Meta'nin
   verdigi ucretsiz test numarasi kullanilabilir; canliya gecerken gercek isletme
   numarasi eklenip dogrulanmalidir). Dashboard'da gorunen **Phone Number ID** degeri
   `WHATSAPP_PHONE_NUMBER_ID`'dir (telefon numarasinin kendisi degil, Meta'nin verdigi
   ID).
5. **Erisim token'i (access token)**: Gelistirme icin App dashboard'undaki gecici
   token kullanilabilir; production icin **System User** olusturup kalici bir token
   uretin (App ayarlari -> Business Settings -> System Users). Bu deger
   `WHATSAPP_ACCESS_TOKEN`'dir — asla repoya yazilmaz, sadece Railway secret olarak
   girilir.
6. **Sablon olusturma**: WhatsApp Manager'da (business.facebook.com/wa/manage/message-templates)
   yeni bir "Marketing" degil, **"Utility"** kategorisinde sablon olusturun (randevu
   onayi islemsel bir bildirimdir, pazarlama degildir). Onerilen ad: `appointment_confirmation_tr`,
   dil: Turkish (tr). Govde metni yukaridaki "Sablon stratejisi" bolumundeki `{{1}}..{{4}}`
   yer tutuculariyla birebir ayni sirada olmalidir.
7. **Sablon onayi**: Meta sablonlari genellikle birkac dakika ile birkac saat icinde
   inceleyip onaylar/reddeder. **Onaylanmadan** o sablonla mesaj gonderilemez —
   `MetaWhatsAppSender` bu durumda Graph API'den hata alir, bu da otomatik olarak
   ilgili Notification satirini `FAILED` yapar (randevu yine de olusturulmus olur).
8. **Sablon adi/dili GlowBook'a bildirilir**: Onaylanan ad/dil `WHATSAPP_TEMPLATE_NAME`
   ve `WHATSAPP_TEMPLATE_LANGUAGE` olarak Railway'e girilir (varsayilanlar zaten
   `appointment_confirmation_tr`/`tr`).

## Railway Environment Variables

Backend Railway'de calisiyor. WhatsApp'i gercekten aktif etmek icin **Variables**
sekmesine asagidakiler eklenmelidir (var olan `JWT_*`/`DB_*`/datasource degiskenlerine
dokunulmaz):

| Degisken | Zorunlu | Aciklama |
| --- | --- | --- |
| `WHATSAPP_ENABLED` | Evet (canliya gecerken) | `true` yapilmadikca hicbir gonderim denenmez. Su an bilincli olarak ayarlanmiyor/`false`. |
| `WHATSAPP_PROVIDER` | Evet (canliya gecerken) | `meta` yapilmadikca `log` (no-op) provider aktif kalir. |
| `WHATSAPP_ACCESS_TOKEN` | Evet | Meta System User kalici token'i. Secret. |
| `WHATSAPP_PHONE_NUMBER_ID` | Evet | Meta dashboard'daki Phone Number ID. |
| `WHATSAPP_BUSINESS_ACCOUNT_ID` | Hayir | Sablon yonetimi/destek icin faydali; mesaj gonderiminde kullanilmiyor. |
| `WHATSAPP_TEMPLATE_NAME` | Hayir | Varsayilan `appointment_confirmation_tr`. |
| `WHATSAPP_TEMPLATE_LANGUAGE` | Hayir | Varsayilan `tr`. |
| `WHATSAPP_GRAPH_API_VERSION` | Hayir | Varsayilan `v21.0`. |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | Onerilir | Meta App dashboard'unda webhook abonelik dogrulamasi icin girilecek deger ile ayni olmali. |
| `WHATSAPP_APP_SECRET` | Onerilir | Meta App Secret — webhook imza dogrulamasi (HMAC) icin. Bos birakilirsa webhook imzasi dogrulanmaz (loglara uyari yazilir) ama endpoint yine calisir. |

Degiskenler eklendikten/degistirildikten sonra **Railway servisini yeniden deploy
etmek gerekir** (Railway genelde Variables degisiminde otomatik redeploy tetikler;
tetiklemezse "Deploy" ile elle baslatin).

## Webhook kurulumu

Backend'de public (JWT gerektirmeyen) bir webhook endpoint'i var:

```
GET  /api/whatsapp/webhook   (Meta abonelik dogrulamasi)
POST /api/whatsapp/webhook   (teslimat/okundu/basarisiz durum bildirimleri)
```

`SecurityConfig` bu path'i acikca `permitAll()` yapar; guvenligi Spring Security degil,
controller'in kendisi saglar (GET icin `hub.verify_token` eslesmesi, POST icin
`X-Hub-Signature-256` HMAC dogrulamasi).

1. Meta App dashboard'unda WhatsApp -> Configuration -> Webhook bolumune gidin.
2. **Callback URL**: `https://<railway-backend-domain>/api/whatsapp/webhook`
3. **Verify Token**: Railway'deki `WHATSAPP_WEBHOOK_VERIFY_TOKEN` ile birebir ayni
   degeri girin.
4. "Verify and Save" tiklandiginda Meta GET istegi atar; backend token eslesirse
   `hub.challenge` degerini oldugu gibi geri doner ve Meta abonelik onaylanir.
5. **Webhook fields**: `messages` altinda mesaj durumu (`message_template_status_update`
   degil, gonderdiginiz mesajlarin `statuses` alani) abone olun.
6. Meta'dan sonraki her POST, `WhatsAppNotificationService.recordDeliveryStatusUpdate`
   uzerinden ilgili `Notification` satirini `provider_message_id` ile eslestirip
   `delivery_status`/`delivered_at`/`read_at`/`failed_at` gunceller. Sira disi/gec gelen
   webhook'lar zaten daha ileri bir durumu geriye almaz (orn. `read`den sonra gelen
   gecikmis bir `sent` yok sayilir).

## Canli test prosedürü (Meta credential'lari hazir oldugunda)

1. Railway'de `WHATSAPP_ENABLED=true`, `WHATSAPP_PROVIDER=meta` ve yukaridaki diger
   degiskenleri girin, redeploy edin.
2. Kendi telefon numaranizi (WhatsApp'i acik) test alici olarak, Meta test
   numarasindan gelen ilk mesaji WhatsApp'ta onaylayin (Meta test numaralari yalnizca
   "test alici" olarak eklenmis numaralara mesaj gonderebilir — bu adim App
   dashboard'undaki "To" listesine numaranizi eklemeyi de icerir).
3. Gercek/test ortaminda bu numarayla **tek** bir kontrollu randevu olusturun (misafir
   veya kayitli fark etmez).
4. WhatsApp'ta "GlowBook randevunuz olusturuldu..." mesajinin geldigini dogrulayin.
5. `GET /api/notifications/customer/{id}` (veya DB'den `notifications` tablosu)
   uzerinden ilgili satirin `channel=WHATSAPP`, `delivery_status=SENT` (birkac saniye
   sonra webhook ile `DELIVERED`) oldugunu dogrulayin.

Bu adim Meta credential'lari/onayli sablon olmadan **yapilamaz** — mock testler ile
gercek teslimat birbirinin yerine gecmez.

## Test stratejisi

Tum otomatik testler `FakeWhatsAppSender` (bkz. `src/test/java/glowbook/support/`)
kullanir; gercek Meta API'sine **hicbir zaman** cagri yapilmaz. Kapsam:

- `WhatsAppAppointmentNotificationIntegrationTest`: kayitli musteri, misafir, paket ilk
  randevu, paketten sonraki randevu, telefon normalizasyonu, reddedilen/rollback olan
  randevunun bildirim gondermedigini, saglayici hatasinin randevuyu etkilemedigini,
  duplicate slot denemesinin en fazla bir mesaj gonderdigini dogrular.
- `WhatsAppDisabledIntegrationTest`: `WHATSAPP_ENABLED=false` iken hicbir dis istege
  cikilmadigini ve bildirim satiri olusmadigini dogrular (su anki production
  konfigurasyonu).
- `WhatsAppWebhookControllerIntegrationTest`: verify handshake (dogru/yanlis token) ve
  durum guncellemelerinin (`sent -> delivered -> read`, sira disi/duplicate webhook)
  doğru islendigini dogrular.
- `LoggingWhatsAppSenderTest`: varsayilan saglayicinin gercekten hicbir HTTP istemcisi
  olmadigini (mimari olarak dis istek atamayacagini) dogrular.
- `WhatsAppSecretLoggingTest`: token/secret alanlarina dokunan dosyalardaki hicbir log
  cagrisinin bu alanlari referans almadigini statik olarak dogrular.
