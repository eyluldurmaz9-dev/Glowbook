# GlowBook demo hesapları

Demo hesapları varsayılan olarak kapalıdır. Uygulama bu hesapları yalnızca
`ENABLE_DEMO_USERS=true` olduğunda oluşturur. Parolalar kaynak kodda bulunmaz ve
yalnızca ortam değişkenlerinden okunur.

## Railway değişkenleri

Railway proje ekranında backend servisini açın ve **Variables** bölümüne şu dört
değişkeni ekleyin:

```text
ENABLE_DEMO_USERS=true
DEMO_ADMIN_PASSWORD=<SİZİN_BELİRLEYECEĞİNİZ_PAROLA>
DEMO_EMPLOYEE_PASSWORD=<SİZİN_BELİRLEYECEĞİNİZ_PAROLA>
DEMO_CUSTOMER_PASSWORD=<SİZİN_BELİRLEYECEĞİNİZ_PAROLA>
```

Gerçek parolaları Git'e, dokümana veya Flutter yapılandırmasına eklemeyin.
Değişkenleri kaydettikten sonra backend servisini yeniden deploy edin. Başlangıç
başarılıysa aşağıdaki hesaplar mevcut parola kodlayıcıyla kaydedilir:

- Yönetici: `admin@glowbook.test`
- Personel: `employee@glowbook.test` (`employeeId=DEMOEMP`)
- Müşteri: `customer@glowbook.test`

Personel hesabı aktif bir çalışan kaydıdır ve başlangıç anındaki tüm aktif
hizmetlere atanır. Böylece personel panelindeki program ve hizmet verilerini
gerçek endpointler üzerinden kullanabilir.

## Oluşturmayı doğrulama

1. Railway deploy durumunun başarılı olduğunu ve `/actuator/health` yanıtının
   `UP` olduğunu kontrol edin.
2. GlowBook giriş ekranında doğru giriş türünü seçin.
3. İlgili e-posta ile yalnızca Railway'de tanımladığınız parolayı kullanın.
4. Yönetici hesabının Yönetici Paneli'ne, personel hesabının Personel Paneli'ne,
   müşteri hesabının müşteri alanına yönlendirildiğini doğrulayın.

Test tamamlanınca `ENABLE_DEMO_USERS=false` yapıp yeniden deploy edin. Bayrağı
kapatmak yeni otomatik oluşturmayı durdurur; daha önce oluşturulmuş satırları
otomatik silmez. Üretimde artık gerek duyulmuyorsa hesapları yetkili operasyon
süreciyle pasifleştirin veya kaldırın.
