# ZAY VIDEO EDITOR

Aplikasi editor video Android (Kotlin + Jetpack Compose). Status: **Tahap 6 — ekspor MP4 dan layar penuh** (v0.6.0).
Lihat `PROGRESS.md` untuk status lengkap.

## Cara mendapatkan APK (pilih salah satu)

### A. GitHub Actions (tanpa instal apa pun, bisa dari HP/laptop)
1. Buat akun di github.com, lalu buat repository baru (boleh Private).
2. Unggah **seluruh isi folder proyek ini** (termasuk folder tersembunyi `.github`).
3. Buka tab **Actions** → **Build APK** → **Run workflow**. Tunggu sekitar 5–10 menit.
4. Buka hasil run yang hijau → bagian **Artifacts** → unduh `ZAY-Video-Editor-debug-apk`.
5. Ekstrak zip, pasang `app-debug.apk` di HP (izinkan "Instal dari sumber tidak dikenal").
6. Jika run berwarna merah, salin pesan error-nya ke saya; saya perbaiki.

### B. Android Studio di laptop
1. Pasang Android Studio versi terbaru.
2. **File → Open** → pilih folder proyek ini → tunggu *Gradle Sync* selesai.
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. APK ada di `app/build/outputs/apk/debug/app-debug.apk`.

> APK yang dihasilkan adalah versi *debug* (bisa langsung dipasang). Versi rilis bertanda tangan dibuat di Tahap 7.
