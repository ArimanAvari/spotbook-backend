# SpotBook Backend

Backend-часть курсового проекта «Личный путеводитель».

Сервер создаётся на Kotlin и Ktor. Сейчас добавлен базовый запуск и health endpoint.

## Запуск

```powershell
.\gradlew.bat run
```

После запуска можно проверить:

```powershell
curl http://localhost:8080/health
```

## Авторизация

Авторизация реализована на backend без Firebase. Пользователи хранятся в SQLite, пароль сохраняется только как `password_hash`.

Endpoint-ы:

- `POST /api/auth/register` — регистрация по email и password;
- `POST /api/auth/login` — вход по email и password;
- `GET /api/auth/me` — защищённая проверка текущего пользователя по JWT.

Защищённые запросы должны отправляться с заголовком:

```text
Authorization: Bearer <token>
```

## База данных

При запуске сервер создаёт SQLite-базу:

```text
data/spotbook.db
```

Создаются таблицы:

- `users`;
- `groups`;
- `place_cards`.

## Что будет реализовано дальше

- REST API;
- хранение фотографий на сервере.

Разработка ведётся в отдельной ветке `backend-dev`.
