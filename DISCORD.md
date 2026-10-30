# VOXCORE ve Discord (Faz 8)

Durum: **gerçek Discord oturumunda test edilmedi.** Aşağıdakiler Android'in belgelenmiş davranışına ve genel ses yönlendirme bilgisine dayanır; hangi yolun cihazınızda işlediğini denemeden bilemeyiz.

## Neden doğrudan olmaz

- Android, root olmadan bir uygulamanın başka bir uygulamanın mikrofon girişini değiştirmesine izin vermez. VOXCORE kendi mikrofonunu işler ve kendi çıkışına çalar; Discord yine gerçek mikrofonu okur.
- Android 10 ve sonrasında iki sıradan uygulama mikrofonu aynı anda alamaz: biri ses alır, diğeri sessizlik alır. Ön plandaki arayüzü olan uygulama öncelik alır; sesli arama (ses modu "iletişim") her zaman öncelik alır; `VOICE_COMMUNICATION` ve `CAMCORDER` gibi gizlilik açısından hassas kaynaklar önceliklidir. Yani Discord aramasındayken VOXCORE'un mikrofonu büyük olasılıkla sessiz kalır. (Kaynak: developer.android.com/media/platform/sharing-audio-input)
- Ayarlar > "Ses iletişimi" kaynağı bu nedenle diğer uygulamaları susturabilir.

## Denenecek yollar (sıralı)

1. **İki cihaz, kablo.** VOXCORE telefonunun kulaklık çıkışı (3.5 mm ya da USB ses adaptörü) → bilgisayarın hat/mikrofon girişi. Discord'da o girişi seçin. En güvenilir yol. Test: Discord'da mikrofon testi, VOXCORE'da gecikme testi (kablo ile giriş-çıkış arasında).
2. **Bluetooth.** VOXCORE çıkışını bir Bluetooth ses cihazına vermek ve onu bilgisayarın girişi yapmak cihaza özgüdür; çoğu zaman Bluetooth müzik profili mikrofon girişi olarak kullanılamaz. Garanti yok.
3. **Akustik.** Hoparlör sesini diğer cihazın mikrofonuna yaklaştırmak. Kalite düşük, geri besleme riski yüksek. Yalnızca deneme için.
4. **Root / sanal ses cihazı.** Uygulama kapsamı dışında.

## Test kontrol listesi

- [ ] VOXCORE kulaklıkla çalışıyor, ıslık yok
- [ ] Gecikme testi sonucu: ____ ms
- [ ] Seçilen yolla Discord mikrofon testinde efektli ses duyuluyor
- [ ] Discord'da 10 dakikalık gerçek oturum, kopma/kesinti yok

Sonuçları Claude'a bildirin; çalışan yol belgelenir.
