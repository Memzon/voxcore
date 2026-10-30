# VOXCORE (Faz 1–9)

Android için profesyonel ses stüdyosu. Kotlin + Jetpack Compose, Türkçe arayüz, koyu/mor tema.

## Durum

Hiçbir faz derlenmedi ve cihazda denenmedi. Kod elle yazıldı ve statik olarak gözden geçirildi; ilk derlemede hata çıkması olasıdır.

| Faz | Modül | Durum |
|---|---|---|
| 1 | Proje kurulumu, tema, gezinme, tüm ekranlar | Yazıldı |
| 2 | Canlı mikrofon motoru, efektler, 10 bant EQ, ön plan servisi | Yazıldı |
| 3 | Gerçek zamanlı eko (çok vuruşlu), Türkçe akıllı kelime eko (SpeechRecognizer + TextToSpeech) | Yazıldı |
| 4 | Ses çalar: MP3/WAV/OGG, ileri-geri sarma, ses, kalıcı kitaplık | Yazıldı |
| 5 | Mikser: gürültü kapısı, kompresör (motora bağlı) | Yazıldı |
| 6 | Ön ayarlar: Room, JSON dışa/içe aktarma ve doğrulama | Yazıldı |
| 7 | Kayıt (16 bit mono WAV), kayıt geçmişi, çalma sırası (tekrar, karışık, sıralama) | Yazıldı |
| 8 | Ses yolu seçimi, gecikme ölçümü, Discord yöntemleri | Yazıldı; Discord aktarımı TEST EDİLMEDİ |
| 9 | Son gözden geçirme, derleme akışı | Statik gözden geçirme yapıldı; APK bu ortamda üretilemedi |

## Bilinen sınırlamalar

- Kulaklık kullanın. Hoparlörde mikrofon sesi geri yakalar ve ıslık olur.
- Gösterilen "tahmini gecikme" arabellek boyutundan hesaplanır. Ayarlar > Gecikme testi gidiş-dönüş ölçer (hoparlör→mikrofon ya da kablo gerekir).
- Perde kaydırıcı basit gecikme hattı yöntemi kullanır; seste hafif titreşim olabilir.
- Kelime eko: tanıma kalitesi ve çevrimdışı çalışma cihaza bağlıdır; ses motoruyla aynı anda mikrofon paylaşımı bazı cihazlarda çalışmaz; tanıyıcı bip sesi çıkarabilir; cihazda Türkçe metin okuma sesi kurulu olmalıdır.
- Ses çalar çıkışı telefonun medya çıkışıdır; mikrofona ya da Discord'a otomatik karışmaz.
- Kayıt, ses motorunun işlenmiş çıkışını kaydeder (motor kapalıysa kaydı başlatmak motoru da açar).

## Discord

Android, bir uygulamanın başka bir uygulamanın mikrofon girişini değiştirmesine izin vermez (root olmadan). Android 10 ve sonrasında iki sıradan uygulama mikrofonu aynı anda alamaz; biri sessizlik alır, sesli arama her zaman öncelik alır. Ayrıntı ve denenecek yollar: `DISCORD.md`. Gerçek Discord oturumunda test edilmeden "çalışıyor" denmez.

## Derleme

1. GitHub Actions: depoya push edin, Actions sekmesinde "APK derle" çalışmasından `voxcore-debug-apk` indirin. Başarısız olursa `voxcore-build-raporlari` ve adım log'unu paylaşın.
2. Android Studio: klasörü açın (Gradle wrapper'ı Studio oluşturur), Build > Build APK(s). Çıktı: `app/build/outputs/apk/debug/app-debug.apk`.

## Paket yapısı

`audio` (motor, ayarlar, ses giriş/çıkışı), `effects` (DSP), `speech` (kelime eko), `player` (çalar, sıra), `recorder` (WAV kayıt), `presets` (JSON kodlayıcı), `database` (Room), `diagnostics` (gecikme testi), `services` (ön plan servisi), `ui`.
