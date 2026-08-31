# Отчёт о проверке выполнения TASKS.md

Дата проверки: 2026-07-09

## Итог

Все 4 обязательных файла присутствуют в `homework-3/`:

| Требование | Файл | Статус |
|---|---|---|
| Спецификация | [specification.md](specification.md) | ✅ |
| Правила для агента | [agents.md](agents.md) | ✅ |
| Editor/AI rules | [.github/copilot-instructions.md](.github/copilot-instructions.md) | ✅ |
| README | [README.md](README.md) | ✅ |

## `specification.md` — все слои на месте

- **High-level objective** — есть, одна чёткая фраза с границами scope.
- **Mid-level objectives (M1–M5)** — наблюдаемые, проверяемые.
- **Non-functional & policy** — security, audit retention (7 лет), p95-латентность (300/600/700 мс), availability (99.9%/99.5%), rate limits.
- **Implementation notes** — деньги в minor units, opaque ID, явный state machine, идемпотентность, optimistic concurrency.
- **Context (beginning/ending)** — конкретно описаны.
- **Low-level tasks** — 11 задач, каждая привязана к M1–M5, с acceptance criteria.

Сквозные требования тоже закрыты:

- **Edge cases** — таблица из 8 сценариев (пустые состояния, гонки, дубликаты, фрод-паттерны и т.д.) с ожидаемым поведением и compliance-последствиями.
- **Verification** — unit/integration/e2e/reconciliation/manual compliance review.
- **Performance** — измеримые, помечены как "assumed targets" с обоснованием.

## `agents.md` и `.github/copilot-instructions.md`

Оба файла покрывают домен-правила (never log PAN/CVV), тестирование, security/compliance, обработку edge cases — как и требуется в TASKS.md.

## `README.md`

Все три обязательные секции есть: студент/summary, rationale (включая обоснование выбора performance targets), industry best practices со ссылками на конкретные разделы спецификации.

## Замечание (не влияет на выполнение задания)

В папке остались файлы, не входящие в список deliverables из TASKS.md:

- `dhat.log`
- `otchet.txt`

Это сохранённые логи/транскрипты прошлых диалогов с ассистентом. Они не мешают проверке, но при сдаче репозитория "как есть" их стоит удалить или вынести из `homework-3/`, чтобы не путать проверяющего лишними файлами.

## Вывод

Задание homework-3 выполнено полностью в соответствии с TASKS.md.
