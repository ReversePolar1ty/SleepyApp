# Sleepy 💤

**Sleepy** is a clean, intuitive Flutter application designed to help you manage your screen time and improve your sleep hygiene. Set a timer, and once it's up, Sleepy automatically locks your device, ensuring you put your phone away.

---

## 🌍 Language / Язык
- [English](#english)
- [Русский](#русский)

---

<a name="english"></a>
## 🚀 Features

- **Intuitive Timer Dial:** A beautiful, interactive circular dial to set your time with a single touch.
- **Background Persistence:** Powered by an Android Foreground Service, the timer keeps running even if you close the app or lock your screen.
- **Smart Notifications:** Manage your timer directly from the notification tray:
    - View remaining time (updated in minutes).
    - Quick-add **+10 minutes** if you need just a bit more time.
    - **Cancel** the timer instantly.
- **Device Locking:** Uses Android's Device Administration API to securely lock your screen when the time expires.
- **Theme Support:** Fully supports Light (Red/White) and Dark (Orange/Dark) modes, with automatic preference saving.
- **Custom Branding:** Features a custom "Sleepy Moon" icon for a cozy, nighttime feel.

## 🛠 Technical Stack

- **Framework:** [Flutter](https://flutter.dev)
- **Language:** Dart & Kotlin
- **Android Features:** 
    - `DevicePolicyManager` for screen locking.
    - `ForegroundService` for reliable background execution.
    - `NotificationChannel` & `BroadcastReceiver` for interactive notifications.
    - `SharedPreferences` for user preference persistence.

## 📦 Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/ReversePolar1ty/SleepyApp.git
   ```
2. **Install dependencies:**
   ```bash
   flutter pub get
   ```
3. **Android Setup:**
    - Open the project in Android Studio.
    - Sync with Gradle Files.
    - Run the app on an Android device (API 21+).
4. **Permissions:**
    - Upon first run, the app will ask for **Notification Access** (Android 13+).
    - To enable the locking feature, tap **"Request Admin"** and activate the Device Admin permission.

---

<a name="русский"></a>
## 🚀 Особенности

- **Интуитивный циферблат:** Красивый интерактивный круг для установки времени одним касанием.
- **Работа в фоне:** Благодаря Foreground Service таймер не останавливается, даже если закрыть приложение или заблокировать экран.
- **Умные уведомления:** Управляйте таймером прямо из шторки:
    - Просмотр оставшегося времени (в минутах).
    - Быстрое добавление **+10 минут**.
    - Мгновенная **Отмена** таймера.
- **Блокировка устройства:** Использует Android Device Administration API для надежной блокировки экрана по истечении времени.
- **Поддержка тем:** Полноценные Светлая (Красно-белая) и Тёмная (Оранжево-тёмная) темы с сохранением выбора.
- **Кастомный стиль:** Уникальная иконка «Луна» для создания уютной ночной атмосферы.

## 🛠 Технологии

- **Фреймворк:** [Flutter](https://flutter.dev)
- **Языки:** Dart и Kotlin
- **Системные функции Android:** 
    - `DevicePolicyManager` для блокировки экрана.
    - `ForegroundService` для стабильной работы в фоне.
    - `NotificationChannel` и `BroadcastReceiver` для интерактивных пушей.
    - `SharedPreferences` для сохранения настроек пользователя.

## 📦 Установка и настройка

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/ReversePolar1ty/SleepyApp.git
   ```
2. **Установите зависимости:**
   ```bash
   flutter pub get
   ```
3. **Настройка Android:**
    - Откройте проект в Android Studio.
    - Синхронизируйте файлы Gradle.
    - Запустите на Android-устройстве (API 21+).
4. **Разрешения:**
    - При первом запуске приложение попросит доступ к **Уведомлениям** (Android 13+).
    - Для работы блокировки нажмите **«Разрешить блокировку»** и активируйте права администратора устройства.

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
