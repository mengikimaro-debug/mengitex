# 🛡️ MengiTex — Ultra-Fast AES-256 Encrypted SMS & Security Vault

[![Node.js Package](https://github.com/mengikimaro-debug/mengitex/actions/workflows/npm-publish-github-packages.yml/badge.svg?branch=main&event=release)](https://github.com/mengikimaro-debug/mengitex/actions/workflows/npm-publish-github-packages.yml)
![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-1E8E6A?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin)
![Security](https://img.shields.io/badge/Encryption-Hardware%20AES--256--GCM-0F172A?style=for-the-badge&logo=google-cloud)
![Capacity](https://img.shields.io/badge/Turbo%20Engine-600K%2B%20SMS-D97706?style=for-the-badge)

**MengiTex** ni programu ya kisasa ya Android ya kutuma, kupokea, na kuhifadhi ujumbe mfupi (SMS/MMS) kwa usalama wa hali ya juu (**Hardware-Backed AES-256-GCM Encryption**) na kasi ya papo hapo (**6G-Velocity Frame-0 Architecture**).

---

## ✨ Vipengele Vikuu (Key Features)

### ⚡ 1. Kasi ya 6G & Kutuma SMS Papo Hapo (Zero-Latency Messaging)
- **Frame-0 Instant Launch:** App inafunguka na kuonyesha ujumbe wote ndani ya `<3ms` bila kusubiri kupakia upya.
- **Instant Chat Dispatch:** Kutuma na kupokea SMS kunatokea papo hapo bila kuchelewa wala kujirudia (*Zero-Duplicate Local Echo Guard*).
- **Dual-SIM Smart Routing:** Utambuzi wa laini zote mbili (SIM 1 & SIM 2) pamoja na ripoti za moja kwa moja za *Sent* na *Delivered*.
- **Scheduled SMS:** Panga muda na tarehe maalum ya kutuma ujumbe kiotomatiki.

### 🛡️ 2. Kizuia Matangazo Mahiri (Smart East-Africa Ad Blocker)
- **Utambuzi wa Namba za Watu:** Namba zote za kawaida zinazoanza na **`07...`**, **`06...`**, au **`+255...`** zinatambuliwa kama namba salama za watu na zinaingia moja kwa moja kwenye **Kikasha (Inbox)**.
- **Chujio la Matangazo (Auto Ad-Filter):** Ujumbe wa matangazo unaotoka kwenye namba fupi zinazoanza na **`15`**, **`16`**, **`17`**, **`18`**, **`19`**, **`09`**, **`55`** au majina yasiyo na namba (*Alphanumeric Sender IDs*) unatengwa kimya kimya kwenye kichupo cha **Ads** bila kukusumbua.

### 🚀 3. Hyper-Turbo 600K+ Backup & Universal Restore
- **Uwezo wa SMS 600,000+:** Injini ya *Streaming Multi-Core* yenye uwezo wa kuchambua, kusimba, na kuingiza hadi **SMS 600,000+** kwa sekunde chache tu bila kujaa kumbukumbu (*O(1) RAM Streaming*).
- **Aina Zote za Mafaili:** Inakubali kuingiza na kuhamisha mafaili ya **`.xml`**, **`.xlm`**, **`.mengitex`**, **`.json`**, **`.csv`**, na **`.txt`** pamoja na kusoma SMS zote za simu moja kwa moja.
- **3-Tier Zero-Data-Loss Auto-Recovery:** Mfumo wa kujirejesha kiotomatiki (*Triple-Redundant Keystore + Dual-File Instant Cache + Auto-Guardian Shield*) unaohakikisha data zako hazipotei kamwe.

### 🔐 4. Ngome ya Siri & Ulinzi (Fortress Security Vault)
- **Hardware-Backed AES-256-GCM + AAD-v3:** Kila ujumbe, templeti, na faili la siri linasimbwa kwa funguo inayolindwa na *Android Keystore*.
- **Passcode & Auto-Lock:** Funga app kwa nenosiri (*PBKDF2-HMAC-SHA256*) pamoja na kujifunga kiotomatiki skrini ikizimika.
- **App Disguise (Ficha Icon):** Badilisha muonekano wa icon ya app kuwa **Kikokotoo (Calculator)** au ifiche kwenye skrini kuu.
- **Burn-After-Read & Taka (Trash):** Chaguo la kufuta ujumbe wa siri baada ya kusomwa au kurejesha ujumbe uliofutwa kwa bahati mbaya kutoka kwenye *Taka*.

### 🎛️ 5. Muonekano Tulivu & Mfumo wa Kisasa (Modern UI & UX)
- **Conversation Pop-View:** Shikilia (*Long-Press*) mazungumzo yoyote kwenye orodha kufungua *Pop-View* yenye chaguo za **Chagua Zote (Mark All)**, **Weka Zimesomwa**, **Nyamazisha (Mute)**, **Zuia**, na **Futa**.
- **Matte Off-White & Carbon Dark Mode:** Muonekano tulivu usiachosha macho wenye chaguo za *By System*, *Off-White*, *Dark*, pamoja na *Dim Light Mode*.
- **GitHub Auto-Updater (`mengikimaro-debug/mengitex`):** Mfumo wa ndani kwenye *Mipangilio (Settings)* unaokagua, kupakua (`0%–100%`), na kusakinisha toleo jipya la APK moja kwa moja kutoka **GitHub Releases**.
- **Lugha Mbili:** Inafanya kazi kwa **Kiswahili** na **Kiingereza (English)**.

---

## 🛠️ Teknolojia Zilizotumika (Tech Stack)

| Sehemu | Teknolojia |
| :--- | :--- |
| **Lugha (Language)** | Kotlin 100% (Coroutines & StateFlow) |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Database** | Room SQLite (WAL Mode + CursorWindow-Safe Paged Engine) |
| **Usimbaji (Cryptography)** | AndroidKeyStore AES-256-GCM + AAD-v3 & PBKDF2-HMAC-SHA256 |
| **Backup & Cloud** | Streaming XML/XLM PullParser, Encrypted `.mengitex`, Firebase Firestore Cloud Backup |
| **Auto-Update** | GitHub Releases REST API v3 + Android FileProvider APK Installer |

---

## 📦 Jinsi ya Kuweka Toleo Jipya Kwenye GitHub Releases (OTA Updates)

MengiTex ina mfumo wa kusasisha toleo jipya moja kwa moja kutoka GitHub (`https://github.com/mengikimaro-debug/mengitex`):
1. Tengeneza au hifadhi APK yako (unaweza kubonyeza **Hifadhi APK** au **Tuma APK** ndani ya *Mipangilio -> Mfumo wa Update Mpya*).
2. Nenda kwenye ukurasa wa GitHub wa mradi wako (`https://github.com/mengikimaro-debug/mengitex/releases`) -> **Draft a new release**.
3. Weka tag ya toleo jipya (mfano: `v2.5.0`) na pakia faili la `.apk` kwenye sehemu ya **Assets**.
4. Watumiaji wa MengiTex wakienda kwenye **Mipangilio (Settings) -> Mfumo wa Update Mpya (GitHub)** na kubonyeza **Kagua Sasa**, wataweza kupakua na kusakinisha toleo jipya moja kwa moja ndani ya app!

---

## 🚀 Jinsi ya Kujenga Mradi (Build Instructions)

1. **Clone repository hii:**
   ```bash
   git clone https://github.com/mengikimaro-debug/mengitex.git
   cd mengitex
   ```
2. **Fungua kwenye Android Studio** (Ladybug au mpya zaidi).
3. **Jenga APK ya Debug au Release:**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 👤 Mtengenezaji (Author)

Imetengenezwa kwa ubora na usalama wa hali ya juu kwa ajili ya mawasiliano ya kisasa ya **MengiTex** (`@mengikimaro-debug`).
