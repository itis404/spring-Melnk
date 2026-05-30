[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/Knv-_QWg)
# StudyMarket

StudyMarket — MPA-маркетплейс студенческих товаров на Spring Boot MVC. Студенты продают учебники, конспекты, технику, билеты и вещи для общаги; покупатель оформляет заказ, а продавцу уходит Telegram-уведомление через прямой HTTP-запрос к Bot API.

## Быстрый запуск

```bash
cp .env
docker compose up --build
```

Приложение: http://localhost:8080  
OpenAPI UI: http://localhost:8080/swagger-ui  
Демо-пользователи: `seller / password`, `buyer / password`

## Соответствие требованиям

| Требование | Где реализовано |
| --- | --- |
| Spring Boot + Spring MVC, MPA | `StudyMarketApplication`, MVC-контроллеры, Thymeleaf-страницы |
| HTML5, CSS3, удобный интерфейс | `templates/*`, `static/css/app.css`, адаптивная сетка, анимации |
| Шаблонизатор и развитые шаблоны | Thymeleaf, фрагменты `head`, `header`, `footer`, `product-card`, `flash` |
| Формы, валидация, защита от повторной отправки | Form-классы с Bean Validation, CSRF, PRG, `data-submit-once` |
| Аутентификация, авторизация, регистрация | Spring Security, `AuthController`, закрытые `/orders`, `/chats`, `/products/new`, `/ajax/**` |
| PostgreSQL | `docker-compose.yml`, `application.yml` |
| Spring Data JPA + JPA/CriteriaBuilder | Spring Data repositories + `ProductCriteriaRepository` |
| Минимум 6 JPA-сущностей, M2M/O2M | 10 сущностей: users, roles, products, categories, orders, items, favorites, reviews, messages; M2M product-category/user-role, O2M order-items/product-reviews |
| CRUD хотя бы одной сущности | Product: MVC create/read/update/delete и REST create/read/update/delete |
| Архитектура Controller-Service-Repository | Пакеты `controller`, `service`, `repository`, `domain`, `dto`, `form` |
| JavaScript и AJAX | `static/js/app.js`: избранное и чат |
| Корректная обработка ошибок | `GlobalExceptionHandler`, шаблон `error/custom.html`, JSON для `/api` и `/ajax` |
| Стороннее API без SDK | `TelegramNotificationService` использует `RestClient` и HTTP Bot API |
| REST API + OpenAPI | `/api/products`, `springdoc-openapi`, Postman collection |
| Конвертеры | `ProductToDtoConverter` |
| Логирование исключений | `GlobalExceptionHandler`, `TelegramNotificationService` |
| 2 нестандартных запроса | `ProductRepository.searchByTextAndCategory`, `OrderRepository.findVisibleForUser`, другие `@Query` |
| Subselect | `ProductRepository.findHighlyRatedWithSubselect` |
| Redis-кэш | `@EnableCaching`, `@Cacheable`, Redis в `docker-compose.yml` |
| Docker: приложение и СУБД отдельно | `app`, `postgres`, `redis` в разных контейнерах |

## Telegram

Токен бота не хранится в коде. Перед запуском укажите его в `.env`:

```env
TELEGRAM_BOT_TOKEN=...
TELEGRAM_API_BASE_URL=https://api.telegram.org
TELEGRAM_WEBHOOK_SECRET_TOKEN=...
TELEGRAM_COMMANDS_ENABLED=true
TELEGRAM_POLLING_ENABLED=true
TELEGRAM_PROXY_HOST=
TELEGRAM_PROXY_PORT=
TELEGRAM_PROXY_TYPE=HTTP
```

Чтобы продавец получал уведомления, у пользователя должен быть заполнен `telegramChatId` при регистрации. Для отправки сообщений приложение использует `chat_id`, а не `user_id`. В личном чате эти значения обычно совпадают, но в группах и каналах отличаются.

Бот поддерживает команды `/start` и `/id`. При обычном запуске приложения включен long polling: пользователь пишет боту `/id`, а бот отвечает chat id и user id. Для production можно отключить polling через `TELEGRAM_POLLING_ENABLED=false` и использовать webhook приложения:

```text
POST /telegram/webhook
```

Если задан `TELEGRAM_WEBHOOK_SECRET_TOKEN`, укажите этот же секрет при настройке webhook в Telegram.

Отправить личное сообщение по `@username` через Bot API нельзя: пользователь должен сначала написать боту, чтобы бот получил chat id. `@username` можно использовать как `chat_id` только для публичных каналов или групп, где бот имеет доступ.

Если приложение пишет, что не может подключиться к Telegram Bot API, это означает, что именно среда запуска приложения не достает до `api.telegram.org`. Для Docker/закрытых сетей укажите `TELEGRAM_PROXY_HOST`, `TELEGRAM_PROXY_PORT`, `TELEGRAM_PROXY_TYPE=HTTP` или `SOCKS`; либо поднимите локальный Telegram Bot API server и задайте его через `TELEGRAM_API_BASE_URL`.

Для ручной проверки бота без запуска Spring можно использовать Python-утилиту:

```bash
python3 scripts/telegram_bot.py get-me
python3 scripts/telegram_bot.py set-commands
python3 scripts/telegram_bot.py get-commands
python3 scripts/telegram_bot.py delete-webhook
python3 scripts/telegram_bot.py poll-once
python3 scripts/telegram_bot.py poll
```

Если Telegram работает с хоста, но не из Docker-контейнера, запустите host bridge:

```bash
python3 scripts/telegram_api_bridge.py --port 9091
```

И укажите в `.env`:

```env
TELEGRAM_API_BASE_URL=http://host.docker.internal:9091
```

## REST API

Коллекция Postman находится в `postman/StudyMarket.postman_collection.json`. Для изменения товаров используется Basic Auth демо-пользователя `seller / password`.
