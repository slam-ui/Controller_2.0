# Интернет-магазин (Spring Boot)

## Описание проекта

Это учебный проект интернет-магазина, разработанный с использованием Spring Boot. Проект реализует полноценный REST API для управления товарами, категориями, заказами и пользователями с системой аутентификации и авторизации.

---

## Выполненные задания

### ✅ Задание 1: Подготовка репозитория

**Статус:** Выполнено

**Что реализовано:**
- Создан Spring Boot проект версии 4.0.0
- Проект опубликован на GitHub
- Реализованы контроллеры для работы с основными сущностями

**Контроллеры:**
1. `ProductController` - управление товарами
2. `CategoryController` - управление категориями
3. `OrderController` - управление заказами
4. `UserController` - управление пользователями
5. `OrderItemController` - управление позициями заказа
6. `AuthController` - аутентификация и регистрация
7. `BusinessController` - бизнес-операции

---

### ✅ Задание 2: Работа с REST

**Статус:** Выполнено

**Тема:** Интернет-магазин

**Сущности (5 штук):**

1. **User (Пользователь)**
    - `id` - уникальный идентификатор
    - `username` - логин (уникальный)
    - `password` - пароль (хэшированный)
    - `email` - электронная почта
    - `role` - роль (USER/ADMIN)

2. **Product (Товар)**
    - `id` - уникальный идентификатор
    - `name` - название товара
    - `price` - цена (BigDecimal)
    - `description` - описание
    - `category` - связь с категорией

3. **Category (Категория)**
    - `id` - уникальный идентификатор
    - `name` - название категории (уникальное)
    - `products` - список товаров в категории

4. **Order (Заказ)**
    - `id` - уникальный идентификатор
    - `createdDate` - дата создания
    - `status` - статус (NEW, PAID, SHIPPED)
    - `user` - связь с пользователем
    - `items` - список позиций заказа

5. **OrderItem (Позиция заказа)**
    - `id` - уникальный идентификатор
    - `order` - связь с заказом
    - `product` - связь с товаром
    - `quantity` - количество

**CRUD операции реализованы для всех сущностей:**
- `POST /api/{entity}` - создание
- `GET /api/{entity}` - получение всех
- `GET /api/{entity}/{id}` - получение по ID
- `PUT /api/{entity}/{id}` - обновление
- `DELETE /api/{entity}/{id}` - удаление

---

### ✅ Задание 3: Базы данных

**Статус:** Выполнено

**База данных:** PostgreSQL

**Конфигурация:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/shop_db
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

**Отношения между таблицами:**
- `User` ↔ `Order` (один-ко-многим)
- `Category` ↔ `Product` (один-ко-многим)
- `Order` ↔ `OrderItem` (один-ко-многим)
- `Product` ↔ `OrderItem` (один-ко-многим)

**Ограничения:**
- `User.username` - уникальный, обязательный
- `Category.name` - уникальный, обязательный
- `UserSession.refreshToken` - уникальный, обязательный

**5 бизнес-операций (не просто CRUD):**

1. **Оформление заказа** (`POST /api/business/order?userId={id}`)
    - Принимает Map товаров и их количество
    - Создает заказ и все его позиции в одной транзакции
    - Связывает заказ с пользователем

2. **Поиск товаров по категории и цене** (`GET /api/business/search?category={name}&min={price}&max={price}`)
    - Фильтрация по категории
    - Фильтрация по диапазону цен
    - Использует кастомный запрос в репозитории

3. **История заказов пользователя** (`getUserHistory`)
    - Получение всех заказов конкретного пользователя
    - Включает детали заказов и позиции

4. **Массовое изменение цен** (`applyDiscountToCategory`)
    - Применение скидки ко всем товарам категории
    - Пересчет цен в одной транзакции
    - Использует BigDecimal для точных расчетов

5. **Проверка статуса заказа** (`checkOrderStatus`)
    - Получение текущего статуса заказа
    - Форматированный вывод информации

---

### ✅ Задание 4: Базовая безопасность API

**Статус:** Выполнено

**Что реализовано:**
- ✅ Подключен Spring Security
- ✅ Настроена аутентификация (временно через JWT вместо Basic Auth)
- ✅ CSRF защита отключена (для REST API)
- ✅ Stateless сессии (JWT токены)
- ✅ Роли пользователей (USER, ADMIN)
- ✅ Авторизация по ролям для endpoints
- ✅ Регистрация пользователей (`POST /api/auth/register`)
- ✅ Хэширование паролей (BCrypt)
- ✅ Валидация пароля при регистрации (в сервисе)

**Настройки доступа:**
- `/api/auth/**` - открыто для всех (регистрация, вход)
- `GET /api/products/**` - открыто для всех (просмотр товаров)
- `GET /api/categories/**` - открыто для всех (просмотр категорий)
- Остальные endpoints - только для аутентифицированных пользователей

**Компоненты безопасности:**
- `SecurityConfig` - конфигурация Spring Security
- `CustomUserDetailsService` - загрузка пользователей из БД
- `JwtAuthenticationFilter` - фильтр для проверки JWT токенов
- `PasswordEncoder` - BCrypt для хэширования паролей

---

### ✅ Задание 5: JWT Access/Refresh и управление сессиями

**Статус:** Выполнено

**Что реализовано:**

1. **Модель данных:**
    - ✅ Создана сущность `UserSession` с полями:
        - `id` - идентификатор сессии
        - `user` - связь с пользователем
        - `refreshToken` - refresh токен (уникальный)
        - `expiresAt` - время истечения

2. **JWT токены:**
    - ✅ Access Token - 15 минут жизни
    - ✅ Refresh Token - 30 дней жизни
    - ✅ В payload добавлены claims (username, role)
    - ✅ Валидация обоих типов токенов
    - ✅ Использование HS256 алгоритма

3. **Сервис работы с токенами:**
    - ✅ `AuthService` реализует все операции
    - ✅ Генерация пары токенов при входе
    - ✅ Сохранение сессий в БД
    - ✅ Refresh token rotation (старый токен удаляется)
    - ✅ Проверка срока действия токенов

4. **Endpoints аутентификации:**
    - ✅ `POST /api/auth/login` - вход, возвращает пару токенов
    - ✅ `POST /api/auth/refresh` - обновление токенов
    - ✅ `POST /api/auth/register` - регистрация нового пользователя

**Сценарий работы:**
1. Пользователь логинится → получает access и refresh токены
2. Access токен используется для доступа к защищенным endpoints
3. При истечении access токена → используется refresh для получения новой пары
4. Старый refresh токен инвалидируется (удаляется из БД)
5. Повторное использование старого refresh токена → ошибка 401

---

### ✅ Задание 6: HTTPS и сертификаты

**Статус:** Выполнено

**Что реализовано:**

1. **Сертификаты:**
    - ✅ Создан keystore.p12 (должен быть сгенерирован локально)
    - ✅ Конфигурация HTTPS в application.properties
    - ✅ Порт 8443 для HTTPS соединений

2. **Конфигурация HTTPS:**
```properties
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=password123
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=shop
```

3. **CI/CD:**
    - ✅ GitHub Actions workflow для сборки проекта
    - ✅ Автоматическая компиляция при push
    - ✅ Запуск тестов
    - ✅ PostgreSQL как service для тестов

**Для генерации сертификата используйте:**
```bash
keytool -genkeypair -alias shop -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 365 \
  -dname "CN=Student123456,OU=Shop,O=University,L=City,ST=State,C=RU"
```

## Технологии

- **Java 21**
- **Spring Boot 4.0.0**
- **Spring Security** - безопасность и аутентификация
- **Spring Data JPA** - работа с БД
- **PostgreSQL** - реляционная база данных
- **JWT (jjwt 0.11.5)** - токены доступа
- **Lombok** - уменьшение boilerplate кода
- **Maven** - сборка проекта

---

## Структура проекта

```
src/main/java/com/example/shop/
├── config/
│   └── SecurityConfig.java          # Конфигурация Spring Security
├── controller/
│   ├── AuthController.java          # Аутентификация
│   ├── BusinessController.java      # Бизнес-операции
│   ├── CategoryController.java      # CRUD категорий
│   ├── OrderController.java         # CRUD заказов
│   ├── OrderItemController.java     # CRUD позиций
│   ├── ProductController.java       # CRUD товаров
│   └── UserController.java          # CRUD пользователей
├── dto/
│   ├── LoginRequest.java            # DTO для входа
│   ├── RefreshRequest.java          # DTO для refresh
│   └── TokenResponse.java           # DTO с токенами
├── entity/
│   ├── Category.java                # Сущность категории
│   ├── Order.java                   # Сущность заказа
│   ├── OrderItem.java               # Сущность позиции
│   ├── Product.java                 # Сущность товара
│   ├── Role.java                    # Enum ролей
│   ├── User.java                    # Сущность пользователя
│   └── UserSession.java             # Сущность сессии
├── repository/
│   ├── CategoryRepository.java
│   ├── OrderItemRepository.java
│   ├── OrderRepository.java
│   ├── ProductRepository.java
│   ├── UserRepository.java
│   └── UserSessionRepository.java
├── security/
│   ├── CustomUserDetailsService.java  # Загрузка пользователей
│   ├── JwtAuthenticationFilter.java   # JWT фильтр
│   └── JwtTokenProvider.java          # Генерация/валидация JWT
├── service/
│   ├── AuthService.java             # Логика аутентификации
│   ├── CategoryService.java
│   ├── OrderItemService.java
│   ├── OrderService.java
│   ├── ProductService.java
│   ├── ShopBusinessService.java     # Бизнес-логика
│   └── UserService.java
└── ShopApplication.java             # Главный класс
```

---

## Установка и запуск

### Предварительные требования

- JDK 21 или выше
- PostgreSQL 15
- Maven 3.9+

### Шаг 1: Настройка базы данных

```sql
CREATE DATABASE shop_db;
```

### Шаг 2: Переменные окружения

Создайте переменную окружения:
```bash
export DB_PASSWORD=your_password
```

Или установите в IDE (Run Configuration → Environment Variables):
```
DB_PASSWORD=your_password
```

### Шаг 3: Генерация сертификата (для HTTPS)

```bash
keytool -genkeypair -alias shop -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore src/main/resources/keystore.p12 \
  -validity 365 -storepass password123 \
  -dname "CN=YourStudentID,OU=Shop,O=University,L=Moscow,ST=Moscow,C=RU"
```

### Шаг 4: Сборка проекта

```bash
./mvnw clean package
```

### Шаг 5: Запуск приложения

```bash
./mvnw spring-boot:run
```

Приложение запустится на `https://localhost:8443`

---

## API Endpoints

### Аутентификация

| Метод | URL | Описание | Доступ |
|-------|-----|----------|--------|
| POST | `/api/auth/register` | Регистрация | Все |
| POST | `/api/auth/login` | Вход (получение токенов) | Все |
| POST | `/api/auth/refresh` | Обновление токенов | Все |

### Товары

| Метод | URL | Описание | Доступ |
|-------|-----|----------|--------|
| GET | `/api/products` | Список всех товаров | Все |
| GET | `/api/products/{id}` | Товар по ID | Все |
| POST | `/api/products` | Создать товар | Auth |
| PUT | `/api/products/{id}` | Обновить товар | Auth |
| DELETE | `/api/products/{id}` | Удалить товар | Auth |

### Категории

| Метод | URL | Описание | Доступ |
|-------|-----|----------|--------|
| GET | `/api/categories` | Список всех категорий | Все |
| GET | `/api/categories/{id}` | Категория по ID | Все |
| POST | `/api/categories` | Создать категорию | Auth |
| PUT | `/api/categories/{id}` | Обновить категорию | Auth |
| DELETE | `/api/categories/{id}` | Удалить категорию | Auth |

### Заказы

| Метод | URL | Описание | Доступ |
|-------|-----|----------|--------|
| GET | `/api/orders` | Список всех заказов | Auth |
| GET | `/api/orders/{id}` | Заказ по ID | Auth |
| POST | `/api/orders` | Создать заказ | Auth |
| PUT | `/api/orders/{id}` | Обновить заказ | Auth |
| DELETE | `/api/orders/{id}` | Удалить заказ | Auth |

### Бизнес-операции

| Метод | URL | Описание | Доступ |
|-------|-----|----------|--------|
| POST | `/api/business/order?userId={id}` | Оформить заказ | Auth |
| GET | `/api/business/search?category={name}&min={price}&max={price}` | Поиск товаров | Auth |

---

## Примеры запросов

### Регистрация пользователя

```bash
curl -X POST https://localhost:8443/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "SecurePass123!",
    "email": "john@example.com"
  }' \
  --insecure
```

### Вход (получение токенов)

```bash
curl -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "SecurePass123!"
  }' \
  --insecure
```

Ответ:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Создание товара (с токеном)

```bash
curl -X POST https://localhost:8443/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "name": "Ноутбук",
    "price": 50000.00,
    "description": "Игровой ноутбук",
    "category": {"id": 1}
  }' \
  --insecure
```

### Оформление заказа

```bash
curl -X POST 'https://localhost:8443/api/business/order?userId=1' \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "1": 2,
    "3": 1
  }' \
  --insecure
```

### Поиск товаров

```bash
curl -X GET 'https://localhost:8443/api/business/search?category=Electronics&min=100&max=2000' \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  --insecure
```

### Обновление токенов

```bash
curl -X POST https://localhost:8443/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }' \
  --insecure
```

---

## Тестирование

### Запуск тестов

```bash
./mvnw test
```

### CI/CD

Проект настроен на автоматическую сборку при push в main ветку через GitHub Actions.

Workflow включает:
- Компиляцию проекта
- Запуск тестов
- Поднятие PostgreSQL для тестов

---

## Основные функции сервиса

1. **Управление товарами** - добавление, редактирование, удаление товаров
2. **Категоризация** - организация товаров по категориям
3. **Оформление заказов** - создание заказов с несколькими товарами
4. **Аутентификация и авторизация** - JWT токены, роли пользователей
5. **Безопасные сессии** - refresh token rotation для защиты
6. **Поиск и фильтрация** - поиск товаров по категории и цене
7. **HTTPS** - шифрование трафика
8. **История заказов** - просмотр заказов пользователя
9. **Массовые операции** - изменение цен по категориям

---

## Безопасность

### Реализованные меры:

1. **Хэширование паролей** - BCrypt
2. **JWT токены** - статeless аутентификация
3. **Refresh token rotation** - старые токены инвалидируются
4. **HTTPS** - шифрование трафика
5. **Авторизация по ролям** - USER/ADMIN
6. **Переменные окружения** - чувствительные данные не в коде
7. **CSRF защита** - отключена для REST API (stateless)


---
