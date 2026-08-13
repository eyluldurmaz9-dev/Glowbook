# Personel ve saat seçimi

`GET /api/appointments/available-slots` yanıtı personel ID'si, adı, tarih ve tam
saat listesini içerir. Flutter aynı saatte uygun olan her personeli ayrı seçenek
olarak gösterir. Seçim `employeeId`, personel adı, tarih ve saati birlikte tutar;
özet ve randevu isteği anonim kapasite kullanmaz.

Backend gönderilen personelin aktifliğini, hizmet yetkinliğini ve slot
uygunluğunu kaydetmeden hemen önce yeniden doğrular.
