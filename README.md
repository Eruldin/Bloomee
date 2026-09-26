# Bloomee

Regl, su ve genel kadın sağlığı takibi için Android uygulaması (Kotlin + Jetpack Compose, Material 3).

## Öne çıkanlar

- **Döngü takibi** — kanama günleri, akış yoğunluğu, ruh hali, semptomlar, ağrı, uyku, kilo ve notlar.
- **Tahmin** — luteal faz sabitli tahmin motoru; düzensiz döngüleri ve gecikmeyi ayrıca işaretler, yeterli veri yoksa bunu açıkça söyler.
- **Takvim** — kaydedilen regl günleri, tahmini günler ve doğurgan pencere aynı görünümde.
- **Su takibi** — kiloya, hareket düzeyine ve döngü fazına göre kişisel hedef, hızlı ekleme ve 7 günlük grafik.
- **Kalori takibi** — Mifflin-St Jeor denklemiyle (kilo, boy, yaş, hareket) tahmin edilen günlük hedef, öğün bazlı kayıt ve 7 günlük grafik.
- **İçgörüler** — ortalama döngü uzunluğu, değişkenlik, döngü grafiği, en sık semptomlar.
- **Hatırlatıcılar** — WorkManager ile su, regl ve ilaç bildirimleri.
- **Partner modu** — isteğe bağlı, sadece kullanıcı açtığında görünen özet.
- **Yedekleme** — JSON dışa/içe aktarma; paylaşım Android paylaş menüsüyle.
- **Asistan** — isteğe bağlı Gemini sohbeti; anahtar APK'ya gömülmez, kullanıcı Ayarlar'dan girer. Döngü, su ve kalori bağlamını görür.
- **Randevu** — İçgörüler ekranındaki tuş MHRS'yi tarayıcıda açar.
- **Tema** — Ayarlar'dan 5 renk paleti (Gül, Lavanta, Okyanus, Orman, Gün batımı); açık/koyu mod sistemi izler.

Uygulama offline-first çalışır: tüm veri Room ve DataStore ile cihazda tutulur, bulut senkronu isteğe bağlıdır.

## Kurulum

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

`local.properties` içinde Android SDK yolunun (`sdk.dir`) tanımlı olması gerekir. Gerekenler: JDK 17, Android SDK platform 35.

## Firebase (isteğe bağlı bulut senkronu)

`app/google-services.json` yoksa Google Services eklentisi uygulanmaz ve bulut senkronu `UNCONFIGURED` durumunda kalır; uygulama tümüyle çevrimdışı çalışmaya devam eder. Dosya eklenince senkron Ayarlar'dan açılabilir: anonim Firebase kimliğiyle `users/{uid}/dailyLogs`, `users/{uid}/hydration` ve `users/{uid}/nutrition` altında `updatedAt` karşılaştırmalı son-yazan-kazanır birleştirme yapılır.

## Platformlar

| Modül | Hedef | Not |
|-------|-------|-----|
| `:app` | Android telefon/tablet (API 26+) | Ana uygulama |
| `:wear` | Wear OS saatler (API 30+) | Özet arayüz + hızlı su/kalori ekleme |
| `:desktop` | PC (Compose Desktop, JVM) | Ayrı masaüstü arayüz; `./gradlew :desktop:run` ile başlat |
| `:shared` | Ortak Kotlin/JVM kütüphane | Domain katmanı + tema renk değerleri |

Masaüstü uygulaması verilerini `~/.bloomee/bloomee-store.json` dosyasında tutar; şema telefonun `bloomee-yedek.json` yedeğiyle aynıdır, iki yönde içe/dışa aktarım "Profil ve veri" ekranından yapılır. Saat uygulaması şimdilik kendi yerel kaydını tutar (telefon senkronu sonraki aşama).

## CI

`.github/workflows/android-ci.yml` her PR'da ve `main`'e push'ta GitHub üzerinde lint (telefon + saat), `testDebugUnitTest`, telefon + saat debug APK'ları, `:shared` + masaüstü derlemesi ve `bundleRelease` koşturur.

`v*` etiketi push'lanınca `.github/workflows/release.yml` testleri koşturur ve dört imzalı artefaktı GitHub Release'e ekler: telefon APK + AAB, saat APK'sı (`bloomee-wear-*.apk`) ve PC `.deb` kurulum paketi (`bloomee-desktop-*.deb`). Release, `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` repo secret'larını **zorunlu** tutar — eksikse iş hata verir (imzasız paket yayınlanmaz, imzasız sürüm mevcut kullanıcıların güncellemesini bozardı). Etiket sürümün tek kaynağıdır: `v1.2.3` → `versionName=1.2.3`, `versionCode=10203` (M×10000+m×100+p) — telefon, saat ve PC paketleri aynı sürümü alır.

## Yayınlama (Play Store)

1. Bir seferlik yayın anahtarı oluştur (yedekle ve güvende tut — tüm gelecek güncellemeler aynı anahtarı ister):
   ```bash
   keytool -genkeypair -v -keystore bloomee-release.jks -alias bloomee -keyalg RSA -keysize 2048 -validity 10000
   ```
2. `keystore.properties.example` dosyasını `keystore.properties` olarak kopyalayıp şifreleri doldur (dosya ve `.jks` gitignore'ludur, repoya girmez).
3. İmzalı paket üret:
   ```bash
   ./gradlew bundleRelease   # app/build/outputs/bundle/release/app-release.aab
   ```
4. `.aab` dosyasını Play Console'a yükle (`store-assets/bloomee-icon-512.png` mağaza görseli olarak hazır). Yerel derlemede sürüm elle verilebilir: `./gradlew bundleRelease -PversionName=1.1.0 -PversionCode=10100`; CI'da etiketten otomatik hesaplanır.

`keystore.properties` yoksa release derleme imzasız üretilir; yalnızca doğrulama amaçlıdır, Play'e yüklenemez.

Yeni sürüm yayınlamak için: `git tag v1.1.0 && git push origin v1.1.0` — release workflow'u tüm platform artefaktlarını otomatik oluşturup GitHub Release'e ekler. Etiket her zaman `v<major>.<minor>.<patch>` biçiminde olmalı.

## Sağlık uyarısı

Uygulamadaki tahminler ve öneriler genel bilgilendirme amaçlıdır, tıbbi tavsiye veya teşhis yerine geçmez.
