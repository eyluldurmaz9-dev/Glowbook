# GlowBook User Roles

## Customer

Customer kullanicisi kendi profiline, randevularina, paketlerine ve bildirimlerine erisir. Baska kullanicilarin verilerine erisim backend tarafinda engellenmelidir.

## Employee

Employee kullanicisi yetkili oldugu randevu ve takvim verilerini gorur, backend'in izin verdigi randevu durum guncellemelerini yapar.

## Admin

Admin kullanicisi katalog, paket, personel, calisma saatleri, izin/tatil gunleri ve randevu operasyonlarini yonetir. Admin endpointleri admin rol kontrolu altinda tutulmalidir.

## Yetkilendirme Prensibi

UI route guard destekleyici bir katmandir. Nihai guvenlik Spring Security, JWT dogrulama ve role authorization kontrolleriyle backend tarafinda uygulanir.
