# Дизайн системы 4-agent bug-fix pipeline

## 1. Цель
Реализовать pipeline из 4 обязательных агентов, который:
- проверяет качество и достоверность bug research;
- применяет запланированные фиксы к seeded sample-приложению и прогоняет тесты;
- проверяет изменённый код на security-проблемы (без правок кода);
- генерирует unit-тесты для изменённого кода по принципам FIRST;
- запускается целиком одной командой, без ручного вызова каждого агента.

## 2. Архитектурный подход
Система строится как набор независимых агентов, обменивающихся markdown-артефактами через файловую систему (а не через прямые вызовы друг друга).

### Основные принципы
- Каждый агент — отдельный `*.agent.md` файл со своим system prompt и явно указанной моделью во frontmatter.
- Skills (`research-quality-measurement`, `unit-tests-FIRST`) — переиспользуемые стандарты качества, которые агенты явно подключают и обязаны использовать при формировании результата.
- Обмен между этапами идёт через markdown-файлы в `context/bugs/XXX/` — легко читается человеком и легко проверяется автоматически.
- Security Verifier и Unit Test Generator работают только с diff’ом (файлами, изменёнными Bug Fixer по `fix-summary.md`), а не со всей кодовой базой — это ограничивает scope и ускоряет прогон.
- Security Verifier — read-only агент: он никогда не редактирует код, только пишет отчёт.
- Оркестрация — единый скрипт (`run-pipeline.sh` / `npm run pipeline`), который вызывает агентов по порядку и передаёт между ними пути к артефактам и список изменённых файлов.

## 3. Компоненты

### 3.1 Orchestrator (`run-pipeline.sh` / `npm run pipeline`)
- запускает агентов строго в порядке: Bug Researcher → Bug Research Verifier → Bug Planner → Bug Fixer → Security Verifier → Unit Test Generator;
- прокидывает список изменённых файлов из `fix-summary.md` в Security Verifier и Unit Test Generator;
- останавливается и явно сообщает, на каком этапе произошёл сбой (fail-fast), не переходя к следующему агенту.

### 3.2 Bug Researcher / Bug Planner (вспомогательные роли)
Не входят в 4 обязательных агента задания, но нужны, чтобы pipeline мог запускаться end-to-end одной командой без ручной подготовки входных файлов:
- **Bug Researcher** читает `context/bugs/XXX/bug-context.md` и код в `src/`, производит `research/codebase-research.md` с находками и ссылками file:line.
- **Bug Planner** читает `research/verified-research.md` (прошедшее верификацию) и производит `implementation-plan.md` — пошаговый план фиксов с указанием файлов, before/after кода и команды прогона тестов.

### 3.3 Bug Research Verifier *(обязательный, Task 1)*
- читает `research/codebase-research.md`;
- сверяет каждую ссылку file:line и каждый сниппет с реальным исходным кодом;
- оценивает качество research по skill `research-quality-measurement`;
- пишет `research/verified-research.md` с обязательными секциями: Verification Summary, Verified Claims, Discrepancies Found, Research Quality Assessment, References.

### 3.4 Bug Fixer *(обязательный, Task 2)*
- читает `implementation-plan.md` целиком;
- применяет изменения файл за файлом строго по плану;
- запускает тесты после каждого изменения; при провале — останавливается и документирует причину;
- пишет `fix-summary.md`.

### 3.5 Security Vulnerabilities Verifier *(обязательный, Task 3)*
- читает `fix-summary.md` и только изменённые файлы;
- ищет инъекции, hardcoded secrets, небезопасные сравнения, отсутствие валидации, небезопасные зависимости, XSS/CSRF (где применимо);
- каждая находка получает severity (CRITICAL/HIGH/MEDIUM/LOW/INFO), file:line и рекомендацию по устранению;
- пишет только `security-report.md`, код не трогает.

### 3.6 Unit Test Generator *(обязательный, Task 4)*
- читает `fix-summary.md` и только изменённые файлы;
- генерирует тесты только для нового/изменённого кода, следуя skill `unit-tests-FIRST`;
- запускает тесты;
- пишет `test-report.md`.

## 4. Поток данных

```text
context/bugs/001/bug-context.md
        |
        v
Bug Researcher --------------------> research/codebase-research.md
        |
        v
Bug Research Verifier (skill: research-quality-measurement)
        |                            -> research/verified-research.md
        v
Bug Planner ------------------------> implementation-plan.md
        |
        v
Bug Fixer ---------------------------> [изменения в src/] + fix-summary.md
        |
        +------------------------------------+
        v                                     v
Security Verifier                    Unit Test Generator (skill: unit-tests-FIRST)
        |                                     |
        v                                     v
security-report.md                   tests/* + test-report.md
```

## 5. Структура репозитория

```
homework-4/
├── agents/
│   ├── bug-researcher.agent.md        (вспомогательный)
│   ├── bug-planner.agent.md           (вспомогательный)
│   ├── research-verifier.agent.md     (обязательный)
│   ├── bug-fixer.agent.md             (обязательный)
│   ├── security-verifier.agent.md     (обязательный)
│   └── unit-test-generator.agent.md   (обязательный)
├── skills/
│   ├── research-quality-measurement.md
│   └── unit-tests-FIRST.md
├── context/bugs/001/
│   ├── bug-context.md
│   ├── research/codebase-research.md
│   ├── research/verified-research.md
│   ├── implementation-plan.md
│   ├── fix-summary.md
│   ├── security-report.md
│   └── test-report.md
├── src/            # sample-приложение
├── tests/          # baseline + сгенерированные тесты
├── docs/screenshots/
├── run-pipeline.sh
├── README.md
└── HOWTORUN.md
```

## 6. Формат артефактов

- **bug-context.md** — описание seeded багов и security issue: что сломано, ожидаемое поведение, как воспроизвести.
- **codebase-research.md** — находки Bug Researcher с точными file:line и цитатами кода.
- **verified-research.md** — Verification Summary (pass/fail + quality level), Verified Claims, Discrepancies Found, Research Quality Assessment (level + reasoning), References.
- **implementation-plan.md** — список шагов: файл, локация, before/after код, команда для прогона тестов.
- **fix-summary.md** — Changes Made (файл, локация, before/after, результат теста), Overall Status, Manual Verification, References.
- **security-report.md** — таблица находок: severity, file:line, описание, remediation.
- **test-report.md** — перечень сгенерированных тестов, к какому изменению относятся, результат прогона.

## 7. Технические решения
- Агенты и skills — markdown-файлы с YAML frontmatter (модель, роль, инструменты), без выделенного рантайма — исполняются агентской CLI (Claude Code / аналог).
- Явный выбор модели на агента: **сильная reasoning-модель** — Bug Research Verifier и Security Verifier (нужна глубокая проверка фактов и поиск уязвимостей); **быстрая/дешёвая модель** — Bug Fixer и Unit Test Generator (рутинные, хорошо специфицированные операции по готовому плану).
- Файловый протокол обмена через `context/bugs/XXX/` — каждый артефакт можно просмотреть и проверить независимо от остальных этапов.
- Security Verifier и Unit Test Generator ограничены diff’ом, а не всей кодовой базой — обязательное сужение scope для скорости и точности.
- Sample-приложение — простой единый стек, минимум зависимостей, один runnable entry point и одна тестовая команда.

## 8. Критерии приёмки
- Весь pipeline запускается одной командой без ручных промежуточных вызовов.
- Каждый обязательный агент использует соответствующий skill там, где это требуется заданием.
- Все ссылки file:line в `verified-research.md` подтверждены реальным кодом; расхождения задокументированы.
- `fix-summary.md` покрывает все шаги плана, тесты прогнаны после каждого изменения.
- `security-report.md` содержит severity + file:line + remediation для каждой находки и не содержит правок кода.
- `test-report.md` подтверждает, что тесты сгенерированы только для изменённого кода, удовлетворяют FIRST и прогнаны успешно.
- Sample-приложение после прогона pipeline проходит тесты и демонстрирует исправленные баги и закрытую уязвимость.
