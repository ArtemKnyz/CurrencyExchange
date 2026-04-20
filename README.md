# 💱 Currency Exchange API

REST API для управления валютами и расчёта обменных курсов.

## 🚀 Возможности

- Получение списка валют
- Получение валюты по коду
- Добавление новой валюты
- Получение курсов валют
- Конвертация между валютами (включая через USD)

---

## 🛠️ Технологии

- Java 17
- Jakarta Servlet
- JDBC
- SQLite
- Jackson (JSON)
- SLF4J + Logback (логирование)
- Maven

---

## 🧠 Архитектура

- **Servlet** — обработка HTTP-запросов
- **DAO** — работа с базой данных
- **Model (record)** — DTO
- **Util** — подключение к БД

---

## 📁 Структура проекта

src/
├── main/
│   ├── java/
│   │   ├── servlet/     # Контроллеры
│   │   ├── dao/         # Доступ к БД
│   │   ├── model/       # DTO (records)
│   │   └── util/        # Утилиты (DBConnection)
│   └── resources/
│       └── logback.xml  # Конфигурация логирования
└── test/                # Юнит-тесты (в разработке)

## 📡 API

### Получить все валюты
GET /currencies
<img width="390" height="367" alt="image" src="https://github.com/user-attachments/assets/66b6d73e-23fc-4b8f-ae9f-c9d4b5a4eb74" />


### Получить валюту по коду
GET /currency/USD
<img width="401" height="173" alt="image" src="https://github.com/user-attachments/assets/eadb042b-55c3-4345-a2b3-4c59b04b3d7a" />


### Добавить валюту
Параметры:
- name
- code
- sign

---

### Конвертация валют
GET /exchange?from=USD&to=EUR&amount=100

Пример:
"http://localhost:8081/exchange?from=USD&to=EUR&amount=100"
Ответ:
{
"baseCurrency": "USD",
"targetCurrency": "EUR",
"rate": 0.92,
"amount": 100,
"convertedAmount": 92
}


### ▶ Запуск проекта
mvn clean package
mvn tomcat7:run

## 📊 Логирование
Используется SLF4J + Logback

- логирование запросов
- логирование ошибок
- логирование бизнес-операций


