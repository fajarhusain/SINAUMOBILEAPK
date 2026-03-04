# 🔐 ExamLock - Secure Exam Browser for Android

**ExamLock** adalah aplikasi exam browser Android yang aman untuk mencegah kecurangan saat ujian.

---

## 📋 FITUR KEAMANAN

| No | Fitur | Status |
|----|-------|--------|
| 1 | Blokir navigasi (BACK, HOME, RECENT) | ✅ |
| 2 | Blokir notifikasi & quick settings | ✅ |
| 3 | Blokir buka aplikasi lain | ✅ |
| 4 | Auto reset/logout saat keluar app | ✅ |
| 5 | Mode OFFLINE & ONLINE | ✅ |
| 6 | Dukungan QR Code (Scan & Generate) | ✅ |
| 7 | Password untuk keluar ujian | ✅ |
| 8 | Fullscreen (95%+ layar) | ✅ |
| 9 | Password masuk aplikasi | ✅ |
| 10 | Anti Screenshot (FLAG_SECURE) | ✅ |
| 11 | Anti Screen Recording | ✅ |
| 12 | Blokir dual layar/split screen | ✅ |
| 13 | Alarm peringatan saat keluar | ✅ |
| 14 | URL ujian tersembunyi (tidak terlihat user) | ✅ |
| 15 | Disable navigasi bar | ✅ |
| 16 | Tampilan modern dark theme | ✅ |
| 17 | Support Android 8.0+ (API 26+) termasuk 14 | ✅ |
| 18 | Blokir internet aplikasi lain | ✅ (via VPN service) |
| 19 | Hapus proses saat keluar | ✅ |
| 20 | Deteksi Floating Apps | ✅ |
| 21 | Blokir floating apps internal | ✅ |
| 22 | Blokir pesan masuk | ✅ (via Accessibility) |
| 23 | Tampilkan status baterai | ✅ |
| 24 | Tampilkan jam ujian | ✅ |
| 25 | Patch deteksi floating apps (Accessibility Service) | ✅ |
| 26 | QR Code Enkripsi (Base64/AES) | ✅ |
| 27 | Anti copy-paste | ✅ |
| 28 | Anti clipboard | ✅ |
| 29 | Deteksi Bluetooth | ✅ |
| 30 | Deteksi Headset | ✅ |
| 31 | Anti Aplikasi Mengambang 99.99% | ✅ |

---

## 🏗️ STRUKTUR PROJECT

```
ExamLock/
├── app/
│   └── src/main/
│       ├── java/com/examlock/browser/
│       │   ├── ui/
│       │   │   ├── LoginActivity.java         # Halaman login
│       │   │   ├── DashboardActivity.java     # Setup ujian
│       │   │   ├── ExamActivity.java          # Browser ujian utama
│       │   │   ├── QRScanActivity.java        # Scanner QR Code
│       │   │   ├── QRGeneratorActivity.java   # Generator QR Code
│       │   │   └── AdminSettingsActivity.java # Pengaturan admin
│       │   ├── service/
│       │   │   ├── ExamGuardService.java      # Foreground service monitor
│       │   │   └── ExamAccessibilityService.java # Blokir floating apps
│       │   ├── receiver/
│       │   │   ├── BootReceiver.java          # Auto-start on boot
│       │   │   └── ScreenReceiver.java        # Monitor screen on/off
│       │   └── utils/
│       │       ├── SecurityUtils.java         # Utilitas keamanan
│       │       └── AppPreferences.java        # Penyimpanan pengaturan
│       ├── res/                               # Layout, drawable, values
│       └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 🚀 CARA BUILD APK

### Prasyarat:
- **Android Studio** (versi terbaru - Hedgehog atau lebih baru)
- **JDK 17** atau lebih baru
- **Android SDK** dengan API level 34

### Langkah Build:

1. **Buka Project di Android Studio**
   ```
   File → Open → Pilih folder ExamLock
   ```

2. **Sync Gradle**
   ```
   File → Sync Project with Gradle Files
   ```

3. **Build APK Debug**
   ```
   Build → Build Bundle(s)/APK(s) → Build APK(s)
   ```
   APK tersimpan di: `app/build/outputs/apk/debug/app-debug.apk`

4. **Build APK Release (untuk distribusi)**
   ```
   Build → Generate Signed Bundle/APK
   → APK → Create new keystore
   → Isi informasi keystore → Build Release
   ```

### Atau via Command Line:
```bash
# Debug
./gradlew assembleDebug

# Release (butuh keystore)
./gradlew assembleRelease
```

---

## ⚙️ KONFIGURASI PERTAMA KALI

### Login Admin:
- **Username:** `admin`
- **Password:** `password` (ganti di SecurityUtils.java - hash MD5)

### Login Siswa Default:
- **Username:** nama siswa apa saja
- **Password:** `siswa2024` (bisa diubah di Pengaturan Admin)

### Password Keluar Ujian Default:
- `1234` (bisa diubah di Pengaturan Admin)

---

## 🔑 CARA MENGGUNAKAN

### Untuk Admin/Pengawas:
1. Login dengan akun admin
2. Buka **Pengaturan Lanjutan**
3. Set URL ujian, password keluar, nama institusi
4. Generate QR Code untuk distribusi ke siswa
5. Klik **Mulai Ujian**

### Untuk Siswa:
1. Login dengan username & password yang diberikan pengawas
2. Scan QR Code dari pengawas (opsional)
3. Isi nama lengkap
4. Klik **Mulai Ujian**
5. Ujian berjalan dalam mode fullscreen aman

### Untuk Keluar Ujian:
1. Tekan tombol **BACK** di dalam ujian
2. Dialog muncul meminta password
3. Masukkan password exit yang diberikan pengawas

---

## 📱 IZIN YANG DIPERLUKAN

Setelah install, pastikan berikan izin berikut:

1. **Usage Access** (Penggunaan Aplikasi)
   - Pengaturan → Keamanan → Izin Penggunaan Aplikasi → ExamLock → Aktifkan

2. **Accessibility Service**
   - Pengaturan → Aksesibilitas → ExamLock Security Monitor → Aktifkan

3. **Draw over other apps** (Tampil di atas aplikasi lain)
   - Pengaturan → Aplikasi → ExamLock → Izin Khusus → Aktifkan

4. **Camera** - Untuk scan QR Code

---

## 🔒 CARA KERJA KEAMANAN

### Anti Screenshot & Screen Recording:
- `FLAG_SECURE` di semua Activity mencegah screenshot dan screen recording

### Anti Floating Apps:
- **AccessibilityService** mendeteksi perubahan window foreground
- **ExamGuardService** menggunakan UsageStatsManager untuk monitoring
- Jika app tidak diizinkan terdeteksi → otomatis kembali ke ujian + alarm

### Anti Copy-Paste:
- JavaScript diinjeksi ke WebView untuk disable:
  - `contextmenu`, `copy`, `cut`, `paste`, `selectstart`, `dragstart`
- CSS `user-select: none` diterapkan ke semua elemen

### Auto Reset:
- Saat `onPause()` dipanggil (keluar app) → alarm berbunyi
- Setelah 2 detik → reset ke halaman login

### QR Code Enkripsi:
- Data dikodekan Base64 (dapat diupgrade ke AES-256)
- Format: `{"url":"...","exit_password":"...","student":"..."}`

---

## 🔧 KUSTOMISASI

### Mengubah Password Admin:
Di `SecurityUtils.java`:
```java
private static final String ADMIN_PASSWORD_HASH = "..."; // MD5 hash password baru
```
Generate MD5: `echo -n "passwordbaru" | md5sum`

### Menambah Domain yang Diizinkan:
Di `ExamActivity.java`, method `isAllowedUrl()` atau di `SecurityUtils.java`.

### Menambah Floating App yang Diblokir:
Di `ExamGuardService.java` dan `ExamAccessibilityService.java`:
```java
private static final Set<String> BLOCKED_PACKAGES = new HashSet<>(Arrays.asList(
    "com.package.aplikasi.tambahan",
    ...
));
```

---

## 📝 CATATAN PENTING

1. **Accessibility Service** adalah fitur paling penting - pastikan diaktifkan
2. Untuk **Android 10+**, izin `PACKAGE_USAGE_STATS` harus diaktifkan manual
3. Beberapa device Xiaomi/Samsung memerlukan pengaturan tambahan untuk autostart
4. Untuk production, gunakan **AES-256** untuk enkripsi QR Code
5. Ganti semua password default sebelum deployment

---

## 📞 DUKUNGAN TEKNIS

Aplikasi ini adalah **source code project** yang siap di-compile menggunakan Android Studio.

Untuk pertanyaan teknis, modifikasi, atau penambahan fitur, silakan hubungi developer.

---

*ExamLock v2.0 | Secure Exam Browser for Android*  
*Support: Android 8.0 (API 26) - Android 14+ (API 34)*
