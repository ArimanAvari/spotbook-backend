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

## Карточки мест

Для всех запросов карточек нужен заголовок:

```text
Authorization: Bearer <token>
```

Endpoint-ы:

- `GET /api/places` — список карточек текущего пользователя;
- `POST /api/places` — создание карточки;
- `GET /api/places/{id}` — детали карточки;
- `PUT /api/places/{id}` — обновление карточки;
- `DELETE /api/places/{id}` — удаление карточки;
- `PATCH /api/places/{id}/status` — изменение статуса;
- `POST /api/places/{id}/photo` — загрузка одной фотографии через `multipart/form-data`.

Фотографии сохраняются в папку:

```text
uploads/place_photos
```

## Группы

Для всех запросов групп тоже нужен `Authorization: Bearer <token>`.

Endpoint-ы:

- `GET /api/groups` — список групп текущего пользователя;
- `POST /api/groups` — создание группы;
- `DELETE /api/groups/{id}` — удаление группы;
- `GET /api/groups/{id}/places` — список карточек внутри группы;
- `POST /api/groups/{groupId}/places/{placeId}` — добавить карточку в группу;
- `DELETE /api/groups/{groupId}/places/{placeId}` — убрать карточку из группы без удаления карточки.

## Синхронизация

Endpoint-ы:

- `POST /api/sync/export` — принять локальные изменения клиента;
- `GET /api/sync/import` — вернуть все группы и карточки текущего пользователя.

Экспорт принимает группы и карточки со статусами `NOT_SYNCED` или `DELETED`. Для новых записей сервер создаёт `serverId`, для существующих обновляет данные, для `DELETED` удаляет запись пользователя. Импорт возвращает серверные данные текущего пользователя для сохранения в Room.

## Что будет реализовано дальше

- Android-клиент.

Разработка ведётся в отдельной ветке `backend-dev`.
