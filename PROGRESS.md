# ZAY VIDEO EDITOR — Catatan Progres

> Baca file ini PERTAMA di setiap percakapan baru, lalu lanjutkan dari tahap terakhir. Jangan mulai ulang.

## Referensi desain resmi
`docs/konsep/zay-konsep-resmi.png` — jangan ubah identitas desain tanpa persetujuan pemilik.
Neon ungu/biru/pink + navy, kartu membulat, gradasi, bottom bar 4 tab (Beranda, Proyek, Template, Profil).

## Teknologi (versi dipilih untuk saling kompatibel)
- AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21 (plugin compose), JDK 17
- Compose BOM 2024.12.01 (Material3 1.3.x), Activity-Compose 1.9.3, Navigation-Compose 2.8.5
- compileSdk/targetSdk 35, minSdk 26
- Media3 1.5.1: exoplayer + ui (pemutar, Tahap 3), transformer + effect (ekspor MP4, Tahap 6)

## Status tahap
| Tahap | Isi | Status |
|---|---|---|
| 1 | Struktur proyek + desain semua halaman | SELESAI, build hijau, tampil di HP |
| 2 | Navigasi & interaksi UI | Sebagian sudah ada (lihat bawah) |
| 3 | Impor video, preview, timeline sungguhan | SELESAI, diuji di HP (impor, timeline, putar OK) |
| 4a | Belah/hapus/duplikat/geser klip + undo/redo | KODE SELESAI (v0.3.0), **BELUM DI-BUILD/DIUJI** |
| 4b | Simpan proyek (salin video ke penyimpanan app, daftar proyek nyata) | KODE SELESAI (v0.4.0), **BELUM DI-BUILD/DIUJI** (4a juga belum diuji) |
| 4c | Ganti nama proyek, hapus cache di Profil | KODE SELESAI (v0.4.1), **BELUM DI-BUILD/DIUJI**. Kode 4a-4c sudah ditinjau manual (tanpa compiler) dan tidak ditemukan error kompilasi yang jelas |
| 4d | Pilihan rasio sosial media (16:9, 9:16, 1:1, 4:5, 4:3, 21:9) + mode Pas/Isi | KODE SELESAI (v0.4.2), **BELUM DI-BUILD/DIUJI** |
| 5a | Audio: musik dari file, rekam suara, volume, geser, hapus | KODE SELESAI (v0.5.0), **BELUM DI-BUILD/DIUJI** |
| 5b | Teks dan stiker di atas video (pratinjau, timeline, simpan, ekspor) | KODE SELESAI (v0.7.0), **BELUM DI-BUILD/DIUJI** |
| 5c | Filter warna + transisi antar klip | KODE SELESAI (v0.8.0), **BELUM DI-BUILD/DIUJI**. Tab Overlay/Animasi/Blur belum dibuat |
| 6 | Mesin ekspor MP4 720p/1080p/4K + mode layar penuh | KODE SELESAI (v0.6.0), **BELUM DI-BUILD/DIUJI** |
| 7 | Pengujian, perbaikan, APK | Belum |

## Batasan lingkungan
Workspace Claude TIDAK bisa mengunduh Android SDK/Gradle plugin/Maven (diblokir), jadi build
dilakukan lewat GitHub Actions (`.github/workflows/build-apk.yml`) atau Android Studio.
Build #9 (GitHub Actions, assembleDebug) sukses sebelum Tahap 6 ditambahkan. Kode Tahap 6 (v0.6.0) ditulis TANPA compiler dan
TANPA akses ke jar Media3, jadi API Media3 diketik dari ingatan: jalankan build dulu dan perbaiki error-nya sebelum lanjut Tahap 5b/5c.

## Yang sudah benar-benar berfungsi (logika UI nyata, belum diuji di perangkat)
Splash→Onboarding (flag disimpan)→Beranda; tab bawah; filter & pencarian proyek; chip tab panel;
memilih klip & mengetuk timeline untuk memindah playhead; tombol lompat klip sebelum/berikut;
pemilih resolusi (dibagi ke layar Ekspor); estimasi ukuran file; pratinjau teks (font/warna/gaya/ukuran);
saklar pengaturan tersimpan di SharedPreferences ("zay_prefs").

## Tahap 3 (v0.2.0)
Tombol + Media membuka pemilih video galeri (maks 10 sekali pilih, tanpa izin storage). Video masuk ke `EditorSession.clips`,
tampil sebagai klip di lapisan Video (berurutan), diputar ExoPlayer di pratinjau (putar/jeda, lompat klip, ketuk timeline untuk seek).
Sebelum ada video yang diimpor, editor masih menampilkan timeline contoh. Setelah impor, lapisan Teks/Musik/Efek kosong (Tahap 5).
Klip impor belum bisa dihapus (Tahap 4) dan belum tersimpan antar sesi aplikasi.

## Tahap 4a (v0.3.0)
Panel Potong: Belah (di posisi playhead pada klip terpilih), Hapus, Duplikat, Geser kiri/kanan. Undo/redo (maks 50 langkah)
menyimpan snapshot daftar klip di `EditorSession`. Klip menyimpan `trimStartSec..trimEndSec` dari file asli; pemutar memakai
`MediaItem.ClippingConfiguration`. Impor juga bisa di-undo.

## Tahap 4b (v0.4.0)
`data/ProjectStore.kt`: proyek disimpan sebagai JSON di `filesDir/projects/<id>.json`, video disalin ke `filesDir/media/` saat impor
(jadi proyek tetap utuh walau file asli dihapus). Simpan otomatis tiap timeline/resolusi berubah, saat keluar editor, dan saat aplikasi ke latar.
Beranda dan halaman Proyek menampilkan proyek nyata (bukan data contoh). Titik tiga di kartu proyek: favorit / hapus.
Proyek baru dibuat dari tombol Proyek Baru; file proyek baru muncul setelah video pertama diimpor. Timeline proyek kosong tidak lagi berisi contoh.
Tahap 4c (v0.4.1): ganti nama proyek (titik tiga di kartu > Ganti nama) dan Hapus cache di Profil sudah berfungsi.
Belum ada: thumbnail asli (masih gradasi). Saklar "Simpan otomatis" di Profil belum dipakai (simpan otomatis selalu aktif).

## Tahap 4d (v0.4.2) - Rasio
`EditorSession.aspects/aspectIndex/fillFrame/outputSize()`. Chip "Rasio" di baris Media membuka panel rasio. Pratinjau mengikuti rasio
(tinggi maks 250dp); mode Pas = resize_mode fit, Isi = zoom. Tersimpan di JSON proyek (`aspect`, `fill`). Layar Ekspor menampilkan rasio dan
ukuran piksel. Mesin ekspor (Tahap 6) WAJIB memakai `EditorSession.outputSize()` dan `fillFrame` (Presentation crop/letterbox).

## Tahap 5a (v0.5.0) - Audio
`data/DemoData.kt`: `AudioClip`, `EditorSession.audioClips` (+ add/delete/setAudioVolume/moveAudio). Disimpan di JSON proyek (`audios`).
`ui/screens/editor/AudioHelper.kt`: salin audio, `VoiceRecorder` (MediaRecorder, m4a, izin RECORD_AUDIO), `AudioMixer` (satu ExoPlayer per klip,
disinkronkan dengan playhead tiap 100 ms). Panel Musik = tambah musik, rekam suara, pilih klip, volume (Bisu/25/50/75/100), pindah ke playhead, hapus.
Audio tampil di lapisan Musik pada timeline. Batasan: audio belum bisa dibelah/dipotong, belum masuk undo/redo, hanya terdengar saat video
diputar (butuh minimal satu video), volume maksimal 100%. Audio ikut hasil ekspor sejak Tahap 6.

## Tahap 6 (v0.6.0) - Ekspor MP4 dan layar penuh
`export/ExportManager.kt` (objek global, status `ExportManager.state` = Idle/Running/Done/Failed). Memakai Media3 Transformer:
- Video: semua klip timeline berurutan (trim dari `ImportedClip.toMediaItem()`), tiap klip diberi `Presentation.createForWidthAndHeight(w, h, layout)`
  dengan w,h dari `EditorSession.outputSize()`; layout `SCALE_TO_FIT` (mode Pas) atau `SCALE_TO_FIT_WITH_CROP` (mode Isi). H.264 + AAC.
- Audio: suara asli klip ikut bila SEMUA klip punya trek suara (jika ada klip bisu, suara asli dibuang semua). Tiap `AudioClip` (musik/suara)
  = satu sequence sendiri; Transformer mencampur semua sequence. Posisi mulai dibuat dengan file WAV hening di depan audio
  (cache, dihapus setelah ekspor). Volume lewat `GainAudioProcessor`. Audio dipotong bila melewati akhir video; audio yang mulai
  setelah video selesai dilewati.
- Bitrate = dasar resolusi x faktor kualitas x faktor jumlah piksel rasio (dihitung di ExportScreen). Encoder fallback aktif (4K diturunkan otomatis bila HP tidak mampu).
- Hasil: ditulis ke `filesDir/exports/ZAY_<waktu>.mp4`; bila saklar "Simpan ke Galeri" aktif dan Android 10+, disalin ke Movies/ZAY Video Editor
  (MediaStore) lalu file internal dihapus. Android 9 ke bawah atau gagal: tetap di penyimpanan aplikasi (dibuka lewat FileProvider).
- ExportScreen: tombol Export Video memulai ekspor; kartu progres (Batal), kartu selesai (Putar, Bagikan, Selesai), kartu gagal (pesan error + kode).
  Layar tetap menyala selama ekspor; tombol kembali dikunci saat ekspor berjalan.
Layar penuh: `ui/screens/editor/FullscreenPlayer.kt`, dibuka dari ikon Fullscreen di pratinjau editor. Menyembunyikan bilah sistem, memutar ke
lanskap untuk rasio >= 1, memakai ExoPlayer yang sama, ketuk layar = tampil/sembunyi kontrol (putar/jeda, slider, waktu), tombol kembali menutup.
Manifest: `configChanges` pada MainActivity (rotasi tidak membuat ulang activity), FileProvider + `res/xml/file_paths.xml`.
Batasan Tahap 6: frame rate hasil mengikuti video sumber (chip fps belum berefek); teks/stiker/efek/transisi belum ada jadi belum ikut;
ekspor berjalan di dalam aplikasi (jika aplikasi dimatikan sistem saat di latar, ekspor berhenti; belum ada foreground service);
tidak ada pemeriksaan ruang penyimpanan sebelum ekspor.
Jika build gagal, tersangka pertama: nama API di `ExportManager.kt` (konstruktor `EditedMediaItemSequence`, `Composition.Builder`, `ExportException.errorCode`,
`BaseAudioProcessor`) karena belum pernah dikompilasi.

Perbaikan v0.6.1: mode bingkai bawaan = Isi (video di-zoom/dipotong memenuhi rasio); memilih rasio otomatis mengaktifkan Isi. Pas tetap bisa dipilih manual.

## Tahap 5b (v0.7.0) - Teks dan stiker
- `data/Overlay.kt`: `Overlay` (jenis Text/Sticker, isi, waktu mulai+durasi, x/y pecahan 0..1, `size` dalam piksel acuan sisi pendek 1080,
  rotasi, warna ARGB, font, tebal/miring, animasi) + `OverlayStyle` (daftar font/animasi). `EditorSession.overlays` + add/update/delete/duplicate.
  Disimpan di JSON proyek (`overlays`, ProjectStore). Belum masuk undo/redo (seperti audio).
- `overlay/OverlayRenderer.kt`: SATU renderer (android.graphics.Canvas) dipakai pratinjau DAN ekspor. Animasi: Tanpa, Fade, Naik, Zoom, Ketik, Pantul.
  Semua ukuran relatif ke sisi pendek bingkai, jadi sama di 720p/1080p/4K. Stiker = emoji (digambar sebagai teks).
- Pratinjau: `OverlayLayer` (OverlayControls.kt) di atas PlayerView, juga di layar penuh. Overlay terpilih berbingkai putus-putus dan bisa
  digeser dengan menyeret di pratinjau. Waktu dibaca tiap frame (withFrameNanos) supaya animasi mulus.
- Panel Teks/Stiker: tanpa overlay terpilih = mode tambah (tombol Tambahkan); dengan overlay terpilih jenis sama = mode edit langsung
  (font, warna, animasi, ukuran, durasi, putar, posisi cepat Atas/Tengah/Bawah, mulai di garis putih, duplikat, hapus).
- Timeline: teks dan stiker ada di lapisan Teks; item yang bertumpuk dipindah ke baris bawahnya (maks 3 baris per lapisan; berlaku juga untuk Musik).
- Ekspor: `export/OverlayFrames.kt` (subclass `BitmapOverlay`, bitmap bingkai penuh, dua bitmap bergantian) dibungkus `OverlayEffect`
  dan dipasang di TINGKAT COMPOSITION (`Composition.Builder.setEffects`), supaya waktunya = waktu timeline gabungan.
- Batasan: overlay yang mulai setelah video selesai tidak diekspor; teks satu baris di panel (otomatis membungkus bila lebar); belum ada font unggahan;
  4K memakai dua bitmap bingkai penuh (sekitar 66 MB untuk 9:16) sehingga berisiko kehabisan memori di HP kecil.
- Tersangka pertama bila build/ekspor bermasalah (API Media3 diketik dari ingatan, belum dikompilasi): `BitmapOverlay`/`OverlayEffect` (androidx.media3.effect),
  `ImmutableList` (Guava; bila "Unresolved reference", tambah dependency `com.google.guava:guava:31.1-android`),
  `Composition.Builder.setEffects`. Bila teks tampil kebesaran/kekecilan di hasil ekspor, periksa skala default `OverlaySettings`
  (bitmap bingkai penuh diharapkan memenuhi frame persis).

## Tahap 5c (v0.8.0) - Filter dan transisi
`fx/ClipFx.kt`: 13 filter warna (matriks warna 4x5, kekuatan 0..1) dan 6 transisi (Tanpa, Fade, Cahaya, Geser, Zoom, Putar).
`ImportedClip` punya `filter`, `filterStrength`, `transition`, `transitionDur` (disimpan di JSON proyek, ikut undo/redo kecuali geseran slider).
- Filter: pratinjau = ColorMatrixColorFilter di TextureView pemutar (`PreviewFx.kt`, juga di layar penuh); ekspor = `RgbMatrix` Media3 per klip.
- Transisi: dipasang di sambungan klip terpilih dan klip berikutnya, separuh durasi di tiap sisi (total durasi video tidak berubah).
  Ekspor: `MatrixTransformation` (geser/zoom/putar) + `RgbMatrix` (pudar gelap/kilat putih) per klip. Pratinjau hanya versi sederhana (lapisan gelap/terang `TransitionDip`).
- Timeline: lapisan Efek menampilkan batang filter dan transisi (id = id klip + 1_000_000 / 2_000_000, diarahkan kembali ke klip saat diketuk).
- Tersangka pertama bila build gagal: `RgbMatrix`/`MatrixTransformation` di `ClipFx.buildEffects` (API Media3 diketik tanpa compiler).
  Bila transisi tidak terlihat di hasil ekspor, periksa apakah waktu (`presentationTimeUs`) per klip dibaca benar.
- Belum ada: Overlay, Animasi, Blur (tab di panel Efek hanya menampilkan catatan).

## v0.8.1 - Edit langsung di timeline
Ketuk item di timeline = terpilih: muncul tombol X (hapus) dan pegangan tepi putih. Teks/stiker: geser badan = pindah waktu, tarik tepi = perpanjang/perpendek (maks sampai akhir video).
Musik: geser badan + X (belum bisa diperpendek). Video: tarik tepi = potong/perpanjang (dalam batas file asli) + X. Callback di `TimelineView` (`onDragStart/onDragBody/onDragEdge/onDeleteItem`), logikanya di `EditorScreen`.
Pemutar dimuat ulang dengan jeda 120 ms setelah timeline berubah. **BELUM DI-BUILD/DIUJI.**
Keluhan pemilik: tampilan belum persis seperti mockup dan Tahap 5 (filter/transisi) belum terlihat jalan, perlu screenshot/penjelasan.

## v0.9.0 - Efek diperbanyak (5c lanjutan)
Panel Efek punya 3 tab (tab Blur dihapus, belum ada efeknya):
- Filter: 25 filter warna (ClipFx.filters). Panel memakai klip terpilih, atau klip di garis putih bila yang terpilih teks/musik.
- Overlay: 11 efek lapisan (`overlay/FxLayers.kt`: Salju, Hujan, Bintang, Debu Film, Vinyet, Sinema, Hati, Konfeti, Cahaya Bocor, Kilat, Gelembung).
  Disimpan sebagai `Overlay` berjenis `OverlayKind.Effect` (content = kunci efek, size = kekuatan 10..100), digambar OverlayRenderer sehingga pratinjau = ekspor.
  Tampil di lapisan Efek timeline, bisa digeser/diperpanjang/dihapus seperti teks. `frameKey` efek berubah tiap 1/30 detik.
- Animasi: 7 gerakan klip (ClipFx.motionAt: Getar, Denyut, Zoom Masuk/Keluar, Goyang, Melayang, Cermin). Pratinjau: `graphicsLayer` pada PlayerView; ekspor: `MatrixTransformation` per klip.
Versi di Profil kini 0.9.0. **BELUM DI-BUILD/DIUJI.** Tersangka build: `FxLayers.kt`, `ClipFx.buildEffects`, `graphicsLayer` di EditorScreen.

## v0.11.3 - Alat Beranda tanpa kotak (desain saja)
- Enam alat di Beranda kini ikon outline putih + label di bawah, tanpa kotak berwarna (permintaan pengguna, mengikuti gaya grid ikon bersih). Klik tetap membuka proyek baru.
**BELUM DI-BUILD/DIUJI.** Tersangka build: impor alias `Icons.Outlined.*` di `HomeScreen.kt`.

## v0.11.2 - Logo asli + kotak alat kompak (desain saja)
- `ZayLogo` kini memakai gambar logo asli dari konsep (`res/drawable-nodpi/zay_logo.png`, latar transparan, resolusi rendah; ganti dengan file asli bila ada). Dipakai di Beranda (64dp) dan Splash.
- Enam kotak alat Beranda: tinggi 60dp, ikon 32dp di kiri + label di kanan (pilihan A). Fungsi klik tidak berubah.
**BELUM DI-BUILD/DIUJI.**

## v0.11.1 - Foto hero menyatu (desain saja)
- Tepi kiri foto hero dipudarkan lewat alpha gambar (`graphicsLayer Offscreen` + `drawWithContent` BlendMode.DstIn) sehingga tidak ada tepi kotak. Tidak ada perubahan fungsi.
**BELUM DI-BUILD/DIUJI.** Tersangka build: `HomeScreen.kt` (drawWithContent/CompositingStrategy).

## v0.11.0 - Potong musik + geser garis putih (hanya MENAMBAH)
- Musik: tombol **Belah** di panel Musik (membelah di garis putih), dan tepi kiri/kanan musik terpilih bisa diseret untuk memperpendek/memperpanjang.
  Model: `AudioClip.trimStartSec` + `sourceDurationSec` (default aman); `EditorSession.splitAudio/trimAudio`; disimpan di ProjectStore ("trim", "source");
  mixer pratinjau (`AudioHelper`) dan ekspor (`ExportManager`, ClippingConfiguration start+end) memakai trimStart.
- Garis putih (playhead): seret di penggaris waktu (scrub) atau tahan lalu geser garis putih itu sendiri (strip 28dp). Ketuk tetap berfungsi.
**BELUM DI-BUILD/DIUJI.** Tersangka build: `TimelineView.kt` (seekCb/playheadNow), `EditorSession.trimAudio` (coerceIn).

## v0.10.0 - Tambahan kecil (hanya MENAMBAH, tidak mengganti kode lama)
- Thumbnail proyek memakai frame video asli (Beranda + halaman Proyek): `ProjectItem.thumbPath/thumbSec` (opsional, default null = gradasi lama),
  diisi `ProjectStore.readItem` dari klip pertama; `ProjectThumb` memuat frame lewat MediaMetadataRetriever di IO + LruCache 24 item.
- Ekspor: cek ruang penyimpanan sebelum mulai (`ExportManager.start`), pesan gagal berisi perkiraan MB dibutuhkan vs tersedia.
v0.10.1: foto hero Beranda dipasang (Image + pudar tepi kiri di `HomeScreen.kt`).
v0.10.2: Beranda disamakan dengan konsep: ikon Transisi=Layers, Stiker=Image, mahkota di header + tab Template.
**BELUM DI-BUILD/DIUJI.** Tersangka build: `CrownIcon.kt` (ImageVector.Builder.path), `produceState` di `ProjectCard.kt`. Kode v0.4.x-v0.9.0 juga belum pernah dikompilasi sejak build #9.

## Yang MASIH PLACEHOLDER (menampilkan toast "Fitur ini aktif di Tahap X")
Bahasa/kualitas bawaan di Profil, efek Overlay/Animasi/Blur.
Data proyek, klip timeline, daftar musik, dan thumbnail adalah DATA CONTOH (`data/DemoData.kt`).

## Peta kode
- `ZayApp.kt` navigasi (splash, onboarding, main, editor, export)
- `ui/theme/` warna & tema; `ui/components/Neon.kt` komponen bersama (`showSoon()` = penanda fitur belum dibangun)
- `ui/screens/` Splash, Onboarding, Main(+Home, Projects, Template, Profile), Export
- `ui/screens/editor/` Editor, TimelineView, panel Musik/Teks/Efek/Transisi/Stiker
- `data/DemoData.kt` data contoh + `EditorSession` + `ImportedClip` (sementara, diganti ViewModel di Tahap 4)
- `export/ExportManager.kt` mesin ekspor MP4 (Media3 Transformer) + `GainAudioProcessor`
- `ui/screens/editor/FullscreenPlayer.kt` pemutar layar penuh
- `data/Overlay.kt` + `overlay/OverlayRenderer.kt` + `export/OverlayFrames.kt` teks/stiker (Tahap 5b); `ui/screens/editor/OverlayControls.kt` lapisan pratinjau + kontrol edit
- `ui/screens/editor/PlayerHelper.kt` seek/posisi gabungan ExoPlayer + pembaca durasi video; `res/layout/zay_player_view.xml` PlayerView

## Deviasi dari konsep (perlu persetujuan jika ingin diubah)
- (SELESAI v0.10.1) Foto perempuan berheadphone di hero Beranda kini dipakai lagi, dipotong dari konsep resmi (`res/drawable-nodpi/zay_hero_girl.webp`, resolusi rendah ~156x210 px diperbesar 4x). Ganti dengan file aslinya (tanpa teks) untuk hasil tajam.
- (v0.10.2) Mahkota di header Beranda dan tab Template kini dipakai (`ui/components/CrownIcon.kt`); mahkota header hanya hiasan (belum ada fungsi). Badge mahkota 4K belum dipakai (4K diberi label "Ultra").
- Thumbnail foto proyek diganti gradasi sampai Tahap 3.

## Riwayat build
- Build #2 (GitHub Actions): gagal di compileDebugKotlin, 1 error: TextPanel.kt:138 `padding` belum di-import. SUDAH DIPERBAIKI (import ditambahkan). Menunggu build #3.
- Build #9: sukses (assembleDebug, 1m 51s) sebelum Tahap 6. Peringatan saja: Node.js 20 deprecated, setup-java v4 deprecated, ubuntu-latest pindah ke Ubuntu 26 (bisa di-pin ke `ubuntu-24.04`).
- Menunggu build #10 untuk Tahap 6 (v0.6.0) + 5b (v0.7.0). Satu build akan menguji semuanya; perbaiki error kompilasi dulu sebelum lanjut 5c.
- Cara build dari HP: unggah ZayVideoEditor.zip ke repo GitHub, jalankan workflow `Build APK` (`.github/workflows/build-apk.yml`) (mengekstrak zip lalu gradle assembleDebug).

## Redesain Beranda + ikon (v0.8.2) — tema Sapphire Platinum
Beranda: hero foto biru (zay_hero_blue.webp) memudar tanpa bingkai, tombol Video Baru (platinum) + Template (garis platinum), kartu "Lanjutkan Mengedit"
(proyek terakhir), baris Proyek Saya, 6 alat bundar. Tanpa logo mahkota. Bar bawah: pil melayang, 4 tab (Beranda, Proyek, Template, Profil), ikon Template = Dashboard.
Tema global: BrandGradient jadi biru safir, glow latar biru, kartu berbingkai platinum, Navy lebih gelap. Judul Proyek Saya pakai Serif.
Logo/ikon aplikasi = PNG (ic_launcher_foreground.png + ic_launcher_background.png). **BELUM DI-BUILD/DIUJI** (kode ditulis tanpa compiler).

## v0.8.3 — halaman Template ikut tema Sapphire Platinum
TemplateScreen: judul serif, banner "Pilihan Unggulan" (foto biru), kartu 2 kolom tinggi berselang-seling berlabel kategori. Isi template masih placeholder (Tahap 5).
Editor belum diubah (baru mockup tampilan-4-editor-sapphire.png). **BELUM DI-BUILD/DIUJI.**

## v0.8.4 — Ekspor 5 pilihan resolusi
Ditambah 480p (Hemat) dan 2K/1440p (Tajam) di samping 720p, 1080p, 4K. Indeks lama (0=720, 1=1080, 2=4K) dipertahankan agar proyek tersimpan tetap benar; baru: 3=2K, 4=480p.
Urutan tampil lewat EditorSession.resolutionOrder (480p, 720p, 1080p, 2K, 4K), dipakai di layar Ekspor (grid 3 kolom) dan menu resolusi Editor.
8K sengaja tidak ditambah (HP umumnya tidak mendukung). **BELUM DI-BUILD/DIUJI.**
