# Whiteboard - Android Diagram App

Mind map ve diyagram oluşturmak için yerel Android uygulaması. Miro/draw.io benzeri bir deneyim sunar.

## Özellikler

### Şekiller
- **Dikdörtgen** - Standart kutu
- **Yuvarlak Köşeli Dikdörtgen** - Mind map node'ları için ideal
- **Daire** - Yuvarlak şekiller
- **Elmas (Diamond)** - Karar noktaları için
- **Parallelogram** - Input/Output için

### Bağlantı Çizgileri (Connector)
- **Düz çizgi** - Doğrudan bağlantı
- **Ortogonal** - 90 derece kırılmalı, flowchart tarzı
- **Bezier** - Yumuşak eğrili bağlantı
- **Ok başları** - Tek yön, çift yön, veya yok

### Düzenleme
- Şekilleri sürükleyip taşıma
- 8 köşeden resize
- Metin ekleme (çift tıklama)
- Renk değiştirme (fill + stroke)
- Zoom in/out (pinch gesture)
- Pan (sürükleyerek gezinme)
- Grid gösterimi
- Snap to grid

### Diğer
- Undo/Redo desteği
- Birden fazla diyagram dosyası
- Otomatik kaydetme
- Room Database ile lokal depolama

## Nasıl Kullanılır

1. **Yeni Diyagram**: Ana ekranda "New Diagram" butonuna tıkla
2. **Şekil Ekle**: Sol toolbar'dan şekil seç, canvas'a tıkla
3. **Bağlantı Oluştur**: "Connect" aracını seç, ilk şekle tıkla, ikinci şekle tıkla
4. **Metin Ekle**: Şekle çift tıkla
5. **Renk Değiştir**: Şekli seç, alt toolbar'dan palet ikonuna tıkla
6. **Zoom**: İki parmakla sıkıştır/genişlet
7. **Pan**: "Pan" aracını seç veya iki parmakla sürükle

## Proje Yapısı

```
app/src/main/java/com/whiteboard/app/
├── data/
│   ├── local/           # Room database
│   ├── model/           # Data classes
│   └── repository/      # Data operations
├── ui/
│   ├── editor/          # Diyagram editörü
│   │   └── components/  # Canvas, toolbar, dialogs
│   ├── home/            # Ana ekran
│   ├── navigation/      # Navigation setup
│   └── theme/           # Material 3 theme
├── MainActivity.kt
└── WhiteboardApplication.kt
```

## Teknolojiler

- Kotlin
- Jetpack Compose
- Material 3
- Room Database
- Coroutines & Flow
- Navigation Compose

## Kurulum

1. Zip'i aç
2. Android Studio'da "Open" ile klasörü seç
3. Gradle sync'i bekle
4. Run butonuna bas

## Min SDK: 26 (Android 8.0)
## Target SDK: 34 (Android 14)
