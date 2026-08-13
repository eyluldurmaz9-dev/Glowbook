# Yönetici personel yönetimi

Yönetici panelindeki personel formunda `Hizmet Yetkinlikleri` bölümü bulunur.
Aktif ana hizmetler gerçek katalog verisine göre gruplandırılır ve her grubun
aktif ana hizmetleri checkbox olarak gösterilir. Aynı ada sahip katalog kayıtları
tek satırda birleştirilir.

Yönetici:

- sıfır veya daha fazla ana hizmet seçebilir;
- bir çalışana farklı kategorilerden birden çok yetkinlik verebilir;
- düzenlemede mevcut seçimleri işaretli görür;
- seçim ekleyebilir veya kaldırabilir;
- çalışanı pasifleştirip daha sonra yeniden aktifleştirebilir.

Personel listesi ad/ID, iletişim, aktif-pasif durum ve kısa yetkinlik özetini
gösterir. Yetkinlik değiştirme endpointleri yalnızca `ADMIN` rolüne açıktır.

Test kapsamı; oluşturma, düzenleme, atama kaldırma, public filtreleme, uyumsuz
randevu reddi, pasif çalışanın gizlenmesi, geçmiş randevu korunması, admin/non-admin
yetkilendirmesi, demo atamaları ve Flutter seçim/onay davranışlarını kapsar.
