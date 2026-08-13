# Randevu uygunluğu

Backend yalnızca dakika ve saniyesi `00` olan başlangıçları üretir ve kabul eder.
Çalışma başlangıcı tam saat değilse ilk sonraki tam saate yuvarlanır. GlowBook'ta
dinamik hizmet süresi bulunmadığından rezervasyon aralığı 60 dakikadır.

Uygunluk; aktif çalışan, hizmet yetkinliği, çalışma günü/saatleri, tatil, izin ve
aynı personelin çakışan PENDING/APPROVED randevuları birlikte değerlendirilerek
hesaplanır. Manuel yarım-saat isteği HTTP 400 ve Türkçe mesajla reddedilir.
