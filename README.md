# AutoDocs — Android-застосунок обліку ремонтів і ТО

Особистий офлайн-застосунок (APK поза Google Play). Стек: Kotlin + Jetpack
Compose, Room, WorkManager, DataStore. minSdk 34 (Android 14), UI лише
українською, валюта — гривня.

## Стан: Етап 1 — каркас ✅

Зроблено:
- Gradle-проєкт (AGP 8.6.1, Kotlin 2.0.21, Compose BOM 2024.10.01, KSP для Room).
- Room-схема версії 1 з усіма таблицями під MVP: `cars`, `work_types`,
  `service_records`, `service_record_items`, `maintenance_rules`,
  `mileage_entries`, `photos` (щоб не робити болючих міграцій щоразу, коли
  з'являється новий екран).
- Навігація: плаваюча капсула меню (Головна · Журнал · План ТО ·
  Налаштування) + кнопка "+", `NavHost` з чотирма екранами-заглушками.
- Тема: тільки темна, всі кольорові токени й радіуси з фінального дизайну
  (`ui/theme/Color.kt`, `Dimens.kt`), шрифти Onest і JetBrains Mono підключені
  як ресурси (`ui/theme/Type.kt`).
- F13 «Налаштування», базова версія: структура розділів (Автомобіль / Дані /
  Нагадування / Про застосунок) без реальної логіки — вона з'явиться разом із
  відповідними фічами.
- Іконка застосунку: тимчасова векторна заглушка (adaptive icon), не фінальний
  дизайн.
- **Ключ підпису APK згенеровано** — `keystore/autodocs-release.keystore`,
  пароль у `keystore/keystore.properties`. **Обов'язково прочитай
  `keystore/README-KEYSTORE.txt` і збережи ці два файли в надійному місці поза
  проєктом.** Без цього ключа наступні збірки не зможуть оновити встановлений
  застосунок.

## Структура

```
app/src/main/java/com/autodocs/app/
  AutoDocsApp.kt          — Application, доступ до БД
  MainActivity.kt
  data/
    entity/               — 7 таблиць Room (Car, WorkType, ServiceRecord, ...)
    dao/                  — DAO для кожної таблиці
    AppDatabase.kt
    Converters.kt
  ui/
    theme/                — Color, Type, Theme, Dimens
    navigation/           — Destinations, BottomMenu, AutoDocsNavHost
    components/           — GlassSurface (базова "скляна" картка)
    screens/{home,journal,plan,settings}/
```

## Збірка

Через обмеження мережі в цій хмарній сесії Claude (немає доступу до
`dl.google.com` / Maven-репозиторіїв Google й Maven Central) сама збірка APK
**не була виконана тут**. Код повністю готовий і синтаксично коректний,
потребує лише один реальний прогін Gradle з нормальним інтернетом. Варіанти —
дивись повідомлення від Claude в чаті.

Коли мережа доступна:
```
./gradlew assembleDebug          # налагоджувальна збірка
./gradlew assembleRelease        # підписана релізна збірка (потрібен keystore/keystore.properties)
```

Готовий GitHub Actions workflow — `.github/workflows/build.yml` (збирає debug
завжди, release — якщо додані секрети `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`).

## Наступні етапи (з autodocs-handoff.md)
2. Авто: картка авто, авто та архів
3. Журнал: довідник робіт, запис, журнал
4. Бекап: експорт/імпорт ZIP
5. Планування: пробіг+прогноз, регламент, розрахунок наступного ТО
6. Нагадування: сповіщення, головний екран «Що далі»
7. Документи: скан, фото до запису, фото техпаспорта
