# Personel pasifleştirme

`DELETE /api/admin/employees/{employeeId}` fiziksel silme yapmaz. Çalışanı
`active=false` durumuna getirir.

- geçmiş ve mevcut randevu satırları korunur;
- yetkinlik atamaları korunur;
- çalışan katalog ve randevu slotlarından çıkarılır;
- yeni randevu ve randevu yeniden zamanlama işlemleri reddedilir;
- yönetici listesinde `Pasif` olarak görünmeye devam eder;
- yönetici düzenleme ekranından yeniden aktifleştirildiğinde eski yetkinlikler
  tekrar kullanılabilir veya aynı ekranda değiştirilebilir.

Flutter, `Personeli Sil` işleminden önce geçmiş kayıtların korunup hesabın
pasifleştirileceğini açıklayan onay diyaloğu gösterir. Bu aşamada hard delete
bilerek sunulmamıştır; tarihsel bütünlük varsayılan davranıştır.
