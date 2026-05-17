# 🎯 Real-Time Face Recognition Android

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/ML-TensorFlow%20Lite-FF6F00?style=for-the-badge&logo=tensorflow" />
  <img src="https://img.shields.io/badge/API-21%2B-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-MIT-purple?style=for-the-badge" />
</p>

<p align="center">
  Gerçek zamanlı yüz tanıma uygulaması. TensorFlow Lite ve Google ML Kit kullanılarak geliştirilmiştir.
  Kişi adı, soyadı, yaş ve sabıka kaydı bilgileriyle birlikte yüz tanıma yapılır.
</p>

---

## ✨ Özellikler

| Özellik | Açıklama |
|---|---|
| 📷 Gerçek Zamanlı Tanıma | Kamera akışından anlık yüz tanıma |
| 👤 Kişi Kaydı | Ad, Soyad, Yaş, Sabıka bilgisi ile kayıt |
| 🟢 Renk Kodu | Tanınan kişi Yeşil — Bilinmeyen Kırmızı |
| 🔄 Kamera Değiştirme | Ön / arka kamera geçişi |
| 🖼️ Fotoğraftan Yükleme | Galeriden fotoğraf ile kişi ekleme |
| 💾 Kalıcı Kayıt | Uygulama kapatılsa bile veriler korunur |
| 🎛️ Hassasiyet Ayarı | Tanıma eşiğini özelleştirebilme |
| 👨‍💻 Geliştirici Modu | Öklid mesafe değerleri görünür |

---

## 🧠 Nasıl Çalışır?

```
Kamera → ML Kit (Yüz Tespiti) → TFLite MobileFaceNet (192 boyutlu vektör)
       → Öklid Mesafesi Karşılaştırması → Kişi Tanımlama
```

1. **Yüz Tespiti:** Google ML Kit ile kamera görüntüsünden yüz bulunur
2. **Embedding:** MobileFaceNet modeli yüzü 192 sayılık vektöre dönüştürür
3. **Karşılaştırma:** Yeni yüz, kayıtlı yüzlerle Öklid mesafesi ile karşılaştırılır
4. **Sonuç:** Mesafe < eşik ise kişi tanınmış, değilse "Bilinmiyor" çıkar

---

## 🚀 Kurulum

### Gereksinimler
- Android Studio Flamingo veya üzeri
- Android SDK 21+
- Java 8+

### Adımlar

```bash
# 1. Repoyu klonla
git clone https://github.com/KULLANICI_ADIN/Real-Time-Face-Recognition-Android.git

# 2. Android Studio → File → Open → Klasörü seç

# 3. Gradle sync bitmesini bekle

# 4. Run ▶ tuşuna bas
```

---

## 📂 Proje Yapısı

```
app/src/main/
├── java/com/atharvakale/facerecognition/
│   ├── MainActivity.java          ← Ana ekran, kamera, tanıma mantığı
│   ├── SimilarityClassifier.java  ← Yüz benzerlik sınıflandırıcı
│   └── splash_screen.java         ← Açılış ekranı (Lottie animasyon)
├── assets/
│   └── mobile_face_net.tflite     ← Yüz tanıma AI modeli (5MB)
└── res/
    ├── layout/                    ← Ekran tasarımları
    └── values/                    ← Renkler, stringler, temalar
```

---

## 🔑 Kritik Kod Bölümleri

### Kişi Kaydetme — satır ~212
```java
final EditText nameIn    = new EditText(context); nameIn.setHint("İsim");
final EditText surIn     = new EditText(context); surIn.setHint("Soyisim");
final EditText ageIn     = new EditText(context); ageIn.setHint("Yaş");
final EditText criminalIn= new EditText(context); criminalIn.setHint("Sabıka Kaydı");

// Pipe ayracıyla tek string olarak kaydedilir
String combined = isim + "|" + soyisim + "|" + yaş + "|" + sabıka;
registered.put(combined, faceEmbedding);
```

### Tanıma Sonucu Gösterme — satır ~261
```java
String[] p = raw.split("\\|");
// p[0]=Ad  p[1]=Soyad  p[2]=Yaş  p[3]=Sabıka
reco_name.setText(
    "AD: "     + p[0] + "\n" +
    "SOYAD: "  + p[1] + "\n" +
    "YAŞ: "    + p[2] + "\n" +
    "SABIKA: " + p[3]
);
reco_name.setTextColor(Color.GREEN);   // Tanındı → Yeşil
// ...
reco_name.setTextColor(Color.RED);     // Bilinmiyor → Kırmızı
```

### Öklid Mesafesi ile Karşılaştırma — findNearest()
```java
float distance = 0;
for (int i = 0; i < emb.length; i++) {
    float diff = emb[i] - knownEmb[i];
    distance += diff * diff;
}
distance = (float) Math.sqrt(distance);
// < 1.0  →  Tanındı ✅
// >= 1.0 →  Bilinmiyor ❌
```

---

## ⚙️ Kullanılan Teknolojiler

| Kütüphane | Sürüm | Amaç |
|---|---|---|
| TensorFlow Lite | 0.3.0 | Yüz embedding çıkarımı |
| Google ML Kit Face | 16.1.5 | Yüz tespiti |
| MobileFaceNet | — | 192 boyutlu yüz vektörü modeli |
| CameraX | 1.2.0-alpha | Kamera yönetimi |
| Lottie | 4.2.2 | Açılış animasyonu |
| Gson | 2.8.9 | JSON kayıt/yükleme |
| Material Design | 1.6.1 | UI bileşenleri |

---

## ⚠️ Önemli Notlar

> **Sabıka ve yaş bilgileri kullanıcı tarafından manuel girilir.** Uygulama bu verileri otomatik tespit etmez.

- Herhangi bir sunucu veya harici veritabanına **bağlantı yoktur**
- Tüm veriler telefon içindeki **SharedPreferences** ile saklanır
- Yalnızca **önceden kaydedilen** yüzler tanınabilir
- İnternet bağlantısı **gerekmez**

---

## 📄 Lisans

MIT License — Özgürce kullanabilir, değiştirebilir ve dağıtabilirsiniz.

---

## 🤝 Katkıda Bulunma

1. Fork yap
2. Branch aç: `git checkout -b ozellik/harika-ozellik`
3. Commit: `git commit -m 'Harika özellik eklendi'`
4. Push: `git push origin ozellik/harika-ozellik`
5. Pull Request aç 🎉

---

<p align="center">⭐ Beğendiysen yıldız vermeyi unutma!</p>
