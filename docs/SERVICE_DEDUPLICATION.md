# Hizmet tekilleştirme

Tekrarların kök nedeni seed eşleşmesinin yalnızca büyük/küçük harf duyarsız
olması; Türkçe karakterli adlarla ASCII karşılıklarını farklı saymasıydı. Seed
artık aksan, Türkçe `ı/i`, noktalama ve boşlukları normalize eden kanonik anahtar
kullanır ve yeni mantıksal tekrar üretmez.

Flutter katalog modeli de eski üretim kayıtlarının geçiş sürecinde aynı kanonik
anahtarla yalnızca bir mantıksal hizmet göstermesini sağlar. Gerçekten farklı
adlara ve kimliklere sahip hizmetler birleştirilmez. Mevcut ilişkili üretim
kayıtları veri kaybı riski nedeniyle otomatik silinmez.
