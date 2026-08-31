# Дизайн системы multi-agent banking pipeline

## 1. Цель
Реализовать пайплайн обработки банковских транзакций, который:
- читает входные данные из sample-transactions.json;
- передаёт каждую транзакцию по цепочке агентов;
- валидирует данные, оценивает риск, принимает решение по settlement/compliance и формирует результат;
- сохраняет итоговые результаты в shared/results/ в структурированном JSON.

## 2. Архитектурный подход
Система строится как набор кооперативных агентов с файловым протоколом обмена сообщениями.

### Основные принципы
- Каждый агент работает как отдельный шаг обработки.
- Обмен между агентами идёт через JSON-файлы в директориях shared/.
- Логика хранится в сервисах и агентах, а не в контроллерах.
- Для денежных значений используется BigDecimal.
- Для аудита сохраняются только безопасные метаданные без plaintext PII.

## 3. Компоненты

### 3.1 Orchestrator / Integrator
Отвечает за:
- загрузку sample-transactions.json;
- создание shared/ директорий;
- запуск цепочки агентов;
- мониторинг статусов и сбор итогов.

### 3.2 Transaction Validator Agent
Проверяет:
- наличие обязательных полей;
- корректность amount;
- поддерживаемый currency;
- корректность account-идентификаторов;
- базовые бизнес-правила (например, отрицательные суммы отклоняются).

### 3.3 Fraud Detector Agent
Оценивает риск транзакции:
- высокая сумма → повышенный риск;
- необычное время → повышенный риск;
- кросс-бордера / международные признаки → повышенный риск;
- результат: score + decision.

### 3.4 Compliance & Settlement Agent
Принимает final decision:
- approved, flagged, rejected;
- применяет правила для разрешения или блокировки операции.

### 3.5 Reporting Agent
Собирает результаты:
- пишет итоговый report в shared/results/;
- формирует summary по всем транзакциям.

## 4. Поток данных
```text
sample-transactions.json
        |
        v
Integrator / Orchestrator
        |
        v
shared/input/        -> входные JSON-сообщения
        |
        v
Transaction Validator
        |
        v
shared/processing/  -> промежуточные результаты
        |
        v
Fraud Detector
        |
        v
Compliance & Settlement
        |
        v
Reporting Agent
        |
        v
shared/results/     -> окончательные результаты + summary
```

## 5. Структура пакетов
- banking.domain — модели транзакции и результатов;
- banking.dto — DTO для запросов/ответов и сообщений;
- banking.service — бизнес-логика и orchestrator;
- banking.agent — агенты обработки;
- banking.store — временное хранение результатов;
- banking.validation — правила проверки;
- banking.exception — исключения.

## 6. Модель данных
### Основные сущности
- Transaction — исходная транзакция;
- ProcessingMessage — сообщение между агентами;
- ProcessingResult — результат обработки агента;
- FraudAssessment — оценка риска;
- PipelineSummary — общий итог по пайплайну.

### Формат результата
```json
{
  "transaction_id": "TXN001",
  "status": "approved",
  "decision": "valid",
  "risk_score": 15,
  "reason": null,
  "audit": {
    "timestamp": "2026-07-09T10:00:00Z",
    "agent": "compliance_settlement"
  }
}
```

## 7. Технические решения
- Java 21 + Spring Boot 3.x;
- BigDecimal для всех денежных значений;
- Jackson / Spring Boot JSON для сериализации;
- File-based message protocol через shared/ directories;
- In-memory store для промежуточных результатов;
- Unit tests с JUnit 5 + Mockito/AssertJ.

## 8. Критерии приемки
- Все транзакции из sample-transactions.json проходят через пайплайн;
- результаты сохраняются в shared/results/;
- каждое решение имеет понятный статус и причину;
- тесты покрывают validator, fraud, compliance, reporting и интеграцию.
