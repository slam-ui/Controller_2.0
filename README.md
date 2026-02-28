# Интернет-магазин (Spring Boot)

## Описание проекта

Учебный проект интернет-магазина на Spring Boot с REST API, JWT-аутентификацией, HTTPS и управлением сессиями.

---

## Исправленные проблемы

### 🔧 Критические исправления

| # | Проблема | Решение |
|---|----------|---------|
| 1 | JWT-ключ пересоздавался при каждом перезапуске | Ключ вынесен в `jwt.secret` (application.properties / env) |
| 2 | Тесты падали из-за SSL без keystore | Добавлен `application-test.properties` с `server.ssl.enabled=false` |
| 3 | `UserSession` не соответствовал заданию | Переписан по спецификации: UUID id, deviceId, SessionStatus (ACTIVE/USED/REVOKED) |
| 4 | Refresh rotation без reuse detection | Добавлено: повторное использование → отзыв всех сессий пользователя |
| 5 | `getUserHistory()` — `findAll()` + фильтрация в памяти | Заменено на `findByUserId(userId)` в репозитории |
| 6 | `applyDiscountToCategory()` — `findAll()` + фильтрация в памяти | Заменено на пакетный `UPDATE` через JPQL |
| 7 | `UserController.create()` не хешировал пароль | `UserService` добавляет хеширование перед сохранением |
| 8 | Нет `/api/auth/logout` | Добавлен endpoint, отзывающий сессию по refresh-токену |
| 9 | Пароль SSL хранился открыто в `application.properties` | Вынесен в переменную окружения `${SSL_KEYSTORE_PASSWORD}` |
| 10 | `server.jks` отсутствовал | Инструкция ниже — генерация полной цепочки сертификатов |

---

## Технологии

- Java 21 / Spring Boot 4.0.0
- Spring Security + JWT (jjwt 0.11.5)
- Spring Data JPA / PostgreSQL
- Lombok / Maven

---

## Установка и запуск

### 1. База данных
```sql
CREATE DATABASE shop_db;
```

### 2. Переменные окружения
```bash
export DB_PASSWORD=your_postgres_password
export JWT_SECRET=dGhpcy1pcy1hLXZlcnktc2VjcmV0LWtleS0xMjM0NTY3ODk=
export SSL_KEYSTORE_PASSWORD=serverPass
```

> **Генерация JWT_SECRET:**
> ```bash
> openssl rand -base64 32
> ```

### 3. Генерация цепочки сертификатов (Root CA → Intermediate CA → Server)

#### Root CA
```bash
keytool -genkeypair -alias rootCA -keyalg RSA -keysize 4096 -validity 3650 \
  -keystore rootCA.jks -storepass rootCAPass \
  -dname "CN=MyRootCA, OU=Dev, O=MyOrg, L=Moscow, ST=Moscow, C=RU" \
  -ext bc=ca:true -ext KeyUsage=digitalSignature,keyCertSign

keytool -export -alias rootCA -keystore rootCA.jks -storepass rootCAPass -file rootCA.crt
```

#### Intermediate CA
```bash
keytool -genkeypair -alias intermediateCA -keyalg RSA -keysize 2048 -validity 1825 \
  -keystore intermediateCA.jks -storepass intCAPass \
  -dname "CN=MyIntermediateCA, OU=Dev, O=MyOrg, L=Moscow, ST=Moscow, C=RU"

keytool -certreq -alias intermediateCA -keystore intermediateCA.jks -storepass intCAPass \
  -file intermediateCA.csr

keytool -gencert -alias rootCA -keystore rootCA.jks -storepass rootCAPass \
  -infile intermediateCA.csr -outfile intermediateCA.crt -validity 1825 \
  -ext "BasicConstraints:critical:true,CA:true,pathLen:0" \
  -ext "KeyUsage=digitalSignature,keyCertSign"

keytool -import -alias rootCA -keystore intermediateCA.jks -storepass intCAPass \
  -file rootCA.crt -noprompt

keytool -import -alias intermediateCA -keystore intermediateCA.jks -storepass intCAPass \
  -file intermediateCA.crt -noprompt
```

#### Серверный сертификат
```bash
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -validity 365 \
  -keystore server.jks -storepass serverPass \
  -dname "CN=localhost, OU=Dev, O=MyOrg, L=Moscow, ST=Moscow, C=RU"

keytool -certreq -alias server -keystore server.jks -storepass serverPass -file server.csr

keytool -gencert -alias intermediateCA -keystore intermediateCA.jks -storepass intCAPass \
  -infile server.csr -outfile server.crt -validity 365 \
  -ext KeyUsage=digitalSignature,keyEncipherment \
  -ext EKU=serverAuth,clientAuth \
  -ext san=dns:localhost

keytool -import -alias rootCA       -keystore server.jks -storepass serverPass -file rootCA.crt       -noprompt
keytool -import -alias intermediateCA -keystore server.jks -storepass serverPass -file intermediateCA.crt -noprompt
keytool -import -alias server       -keystore server.jks -storepass serverPass -file server.crt       -noprompt

# Переместить server.jks в src/main/resources/
mv server.jks src/main/resources/server.jks
```

### 4. Сборка и запуск
```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

Приложение: `https://localhost:8443`

---

## API Endpoints

### Аутентификация

| Метод | URL | Описание | Доступ |
|-------|-----|----------|--------|
| POST | `/api/auth/register` | Регистрация | Все |
| POST | `/api/auth/login` | Вход, получение токенов | Все |
| POST | `/api/auth/refresh` | Обновление токенов | Все |
| POST | `/api/auth/logout` | Выход, отзыв refresh-токена | Все |

### Товары / Категории / Заказы

| Метод | URL | Доступ |
|-------|-----|--------|
| GET | `/api/products` | Все |
| GET | `/api/categories` | Все |
| POST | `/api/products`, `/api/categories`, `/api/orders` | Auth |
| PUT/DELETE | любые | Auth |

### Бизнес-операции

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/api/business/order?userId={id}` | Оформить заказ |
| GET | `/api/business/search?category=X&min=Y&max=Z` | Поиск товаров |

---

## Примеры запросов

### Регистрация
```bash
curl -k -X POST https://localhost:8443/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"SecurePass123!","email":"john@example.com"}'
```

### Вход
```bash
curl -k -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"SecurePass123!"}'
```

### Обновление токенов
```bash
curl -k -X POST https://localhost:8443/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

### Выход
```bash
curl -k -X POST https://localhost:8443/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

### Создание товара
```bash
curl -k -X POST https://localhost:8443/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{"name":"Ноутбук","price":50000.00,"description":"Игровой","category":{"id":1}}'
```

---

## Архитектура сессий (Задание 5)

```
UserSession {
  id:                UUID        (PK)
  userEmail:         String      (username пользователя)
  deviceId:          String      (опционально)
  accessToken:       String(512)
  refreshToken:      String(512)
  accessTokenExpiry: Instant
  refreshTokenExpiry:Instant
  status:            SessionStatus (ACTIVE / USED / REVOKED)
}
```

**Сценарий refresh rotation:**
1. Клиент → `POST /refresh` с refresh-токеном
2. Сервер находит сессию в БД
3. Если `status = USED/REVOKED` → **все сессии пользователя отзываются** (reuse detection)
4. Если истёк → ошибка, требуется повторный вход
5. Старая сессия → `status = USED`
6. Новая сессия создаётся → `status = ACTIVE`
7. Клиент получает новую пару токенов
