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
  Kişi adı, soyadı, yaş ve sabıka kaydı bilgileriyle birlikte biyometrik tanımlama yapılır.
</p>

---

## ✨ Özellikler

| Özellik | Açıklama |
|---|---|
| 📷 **Gerçek Zamanlı Tanıma** | Kamera akışından anlık yüz tanıma ve eşleştirme |
| 👤 **Kişi Kaydı** | Ad, Soyad, Yaş ve Sabıka bilgisi ile yeni yüz ekleme |
| 🟢 **Renk Kodlaması** | Tanınan kişiler Yeşil, Bilinmeyenler Kırmızı çerçeve ile gösterilir |
| 🔄 **Kamera Yönetimi** | Ön ve arka kamera arasında hızlı geçiş desteği |
| 🖼️ **Galeri Desteği** | Cihaz galerisinden fotoğraf yükleyerek kişi kaydetme |
| 💾 **Kalıcı Veritabanı** | Uygulama kapansa dahi kayıtlı yüz verileri korunur |
| 🎛️ **Hassasiyet Ayarı** | Tanıma eşiği (Threshold) ayarı ile doğruluk kontrolü |

---

## 🧠 Çalışma Mantığı

Uygulama modern yapay zeka tekniklerini kullanarak şu adımları izler:
1. **Yüz Tespiti:** Google ML Kit ile görüntüdeki insan yüzü koordinatları belirlenir.
2. **Vektör Dönüşümü:** TFLite MobileFaceNet modeli yüzü 192 boyutlu matematiksel bir vektöre çevirir.
3. **Analiz:** Bu vektör, kayıtlı yüzlerle **Öklid Mesafesi** yöntemiyle karşılaştırılır.
4. **Tanımlama:** Mesafe eşik değerinden küçükse kişi kimliği ekrana basılır.

---

## 📂 Proje Yapısı

* **MainActivity.java:** Kamera yönetimi ve ana uygulama mantığı.
* **SimilarityClassifier.java:** Yüz benzerliklerini hesaplayan matematiksel sınıf.
* **mobile_face_net.tflite:** Uygulamanın beyni olan TensorFlow Lite AI modeli.

---

## ⚙️ Teknik Detaylar

* **Yazılım Dili:** Java
* **AI Framework:** TensorFlow Lite & Google ML Kit
* **Görüntü İşleme:** Android CameraX API
* **UI Tasarımı:** Material Design & Lottie Animations

---

## ⚠️ Notlar

- Uygulama tamamen yerel (offline) çalışır; internet bağlantısı veya bulut sunucusu gerekmez.
- Sabıka ve kimlik bilgileri kullanıcı tarafından manuel olarak kaydedilen verilerdir.

---

<p align="center">⭐ Bu projeyi beğendiysen GitHub üzerinden yıldız vermeyi unutma!</p>
