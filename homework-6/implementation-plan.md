# План реализации

## Фаза 1 — Подготовка инфраструктуры
1. Создать директории для пайплайна:
   - shared/input
   - shared/processing
   - shared/output
   - shared/results
2. Добавить базовые модели:
   - ProcessingMessage
   - ProcessingResult
   - FraudAssessment
   - PipelineSummary
3. Подготовить утилиты для работы с файлами JSON и директорией shared/.

## Фаза 2 — Реализация валидатора
1. Создать TransactionValidatorAgent.
2. Реализовать правила проверки:
   - required fields;
   - amount > 0;
   - currency supported;
   - account format.
3. Сформировать результат в виде JSON и сохранить в shared/output/.

## Фаза 3 — Реализация fraud detection
1. Создать FraudDetectorAgent.
2. Реализовать правила риска:
   - amount thresholds;
   - unusual timing;
   - cross-border indicators.
3. Вычислять risk score и label (low/medium/high).

## Фаза 4 — Реализация compliance и settlement
1. Создать ComplianceSettlementAgent.
2. На основе результата валидатора и fraud-оценки принимать решение:
   - approved;
   - flagged;
   - rejected.
3. Добавить запись аудита с timestamp и agent name.

## Фаза 5 — Реализация reporting
1. Создать ReportingAgent.
2. Собрать итоговую статистику по всем транзакциям.
3. Сохранить summary JSON в shared/results/.

## Фаза 6 — Orchestrator
1. Создать IntegratorService / PipelineOrchestrator.
2. Реализовать последовательность:
   - load input;
   - write messages to shared/input;
   - run agents in chain;
   - collect final results.
3. Обеспечить повторяемый запуск без остаточных данных от предыдущего прогона.

## Фаза 7 — Тесты
1. Unit tests для validator.
2. Unit tests для fraud detector.
3. Unit tests для compliance agent.
4. Integration test для полного пайплайна.

## Фаза 8 — Документация и демонстрация
1. Описать запуск pipeline.
2. Добавить screenshots/notes для PR.
3. Подготовить README/HOWTORUN.

## Приоритеты
- P0: валидатор + orchestrator + базовый запуск;
- P1: fraud detection + compliance settlement;
- P2: reporting + тесты + docs.
