GlowBook Flutter scaffold

Bu klasör Flutter istemcisi için başlangıç iskeletini içerir.

Hızlı başlatma:

- Flutter SDK kurulu olduğundan emin olun
- Proje kökünden:

```bash
cd flutter_app
flutter pub get
flutter run -d chrome   # web için
```

Notlar:
- `lib/src/services/api_client.dart` içinde Dio ve interceptor entegrasyonları eklenecek.
- Firebase FCM için native konfigürasyonlar (Android/iOS) manuel eklenmelidir.
