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

## Что будет реализовано дальше

- REST API;
- SQLite;
- собственная авторизация;
- JWT-токены;
- хранение фотографий на сервере.

Разработка ведётся в отдельной ветке `backend-dev`.
