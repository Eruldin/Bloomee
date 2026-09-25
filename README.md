# Bloomee

Regl, su ve genel kadın sağlığı takibi için Android uygulaması (Kotlin + Jetpack Compose, Material 3).

## Öne çıkanlar

- **Döngü takibi** — kanama günleri, akış yoğunluğu, ruh hali, semptomlar, ağrı, uyku, kilo ve notlar.
- **Tahmin** — luteal faz sabitli tahmin motoru; düzensiz döngüleri ve gecikmeyi ayrıca işaretler, yeterli veri yoksa bunu açıkça söyler.
- **Takvim** — kaydedilen regl günleri, tahmini günler ve doğurgan pencere aynı görünümde.
- **Su takibi** — kiloya, hareket düzeyine ve döngü fazına göre kişisel hedef, hızlı ekleme ve 7 günlük grafik.
- **İçgörüler** — ortalama döngü uzunluğu, değişkenlik, döngü grafiği, en sık semptomlar.
- **Hatırlatıcılar** — WorkManager ile su, regl ve ilaç bildirimleri.
- **Partner modu** — isteğe bağlı, sadece kullanıcı açtığında görünen özet.
- **Yedekleme** — JSON dışa/içe aktarma; paylaşım Android paylaş menüsüyle.
- **Asistan** — isteğe bağlı Gemini sohbeti; anahtar APK'ya gömülmez, kullanıcı Ayarlar'dan girer.

Uygulama offline-first çalışır: tüm veri Room ve DataStore ile cihazda tutulur, bulut senkronu isteğe bağlıdır.

## Kurulum

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

`local.properties` içinde Android SDK yolunun (`sdk.dir`) tanımlı olması gerekir. Gerekenler: JDK 17, Android SDK platform 35.

## Firebase (isteğe bağlı bulut senkronu)

`app/google-services.json` yoksa Google Services eklentisi uygulanmaz ve bulut senkronu `UNCONFIGURED` durumunda kalır; uygulama tümüyle çevrimdışı çalışmaya devam eder. Dosya eklenince senkron Ayarlar'dan açılabilir: anonim Firebase kimliğiyle `users/{uid}/dailyLogs` ve `users/{uid}/hydration` altında `updatedAt` karşılaştırmalı son-yazan-kazanır birleştirme yapılır.

## Sağlık uyarısı

Uygulamadaki tahminler ve öneriler genel bilgilendirme amaçlıdır, tıbbi tavsiye veya teşhis yerine geçmez.
