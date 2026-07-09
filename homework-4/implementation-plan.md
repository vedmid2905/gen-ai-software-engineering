# План реализации

## Фаза 1 — Подготовка инфраструктуры
1. Создать директории: `agents/`, `skills/`, `context/bugs/001/`, `docs/screenshots/`.
2. Подготовить шаблоны артефактов (research, verified-research, implementation-plan, fix-summary, security-report, test-report) — секции и формат, как в `design.md`.
3. Определить общий формат frontmatter для `*.agent.md` (модель, роль, инструменты, подключаемые skills).

## Фаза 2 — Sample Mini Application
1. Выбрать простой стек (один язык, минимум зависимостей, один runnable entry point).
2. Реализовать приложение с минимум 2 намеренными багами и минимум 1 security issue.
3. Настроить `npm test` (или аналог) и команду запуска приложения.
4. Задокументировать баги и уязвимость в `context/bugs/001/bug-context.md` с точными file:line.

## Фаза 3 — Bug Researcher и Bug Planner (вспомогательные агенты)
1. Создать `agents/bug-researcher.agent.md` — читает `bug-context.md` и код, производит `research/codebase-research.md`.
2. Создать `agents/bug-planner.agent.md` — читает `research/verified-research.md`, производит `implementation-plan.md`.
3. Прогнать вручную на seeded баге, чтобы получить реалистичные входные артефакты для Фазы 4–5.

## Фаза 4 — Research Quality skill + Research Verifier *(Task 1, обязательный)*
1. Создать `skills/research-quality-measurement.md` с уровнями/метками качества research.
2. Создать `agents/research-verifier.agent.md` (сильная reasoning-модель), подключить skill.
3. Проверить, что `verified-research.md` содержит все обязательные секции и корректно оценивает качество по skill.

## Фаза 5 — Bug Fixer *(Task 2, обязательный)*
1. Создать `agents/bug-fixer.agent.md` (быстрая/дешёвая модель).
2. Реализовать процесс: чтение плана целиком → применение изменений по файлам → прогон тестов после каждого шага → `fix-summary.md`.
3. Проверить корректную остановку и документирование при падении теста.

## Фаза 6 — Security Vulnerabilities Verifier *(Task 3, обязательный)*
1. Создать `agents/security-verifier.agent.md` (сильная reasoning-модель).
2. Реализовать сканирование только изменённых файлов (список — из `fix-summary.md`).
3. Проверить, что `security-report.md` содержит severity, file:line и remediation для каждой находки, а код не изменяется.

## Фаза 7 — Unit Test Generator + FIRST skill *(Task 4, обязательный)*
1. Создать `skills/unit-tests-FIRST.md` (Fast, Independent, Repeatable, Self-validating, Timely).
2. Создать `agents/unit-test-generator.agent.md` (быстрая/дешёвая модель), подключить skill.
3. Реализовать генерацию тестов только для изменённого кода, прогон тестов, запись `test-report.md`.

## Фаза 8 — Оркестрация одной командой
1. Создать `run-pipeline.sh` (или `npm run pipeline`), вызывающий агентов по порядку.
2. Реализовать передачу списка изменённых файлов из Bug Fixer в Security Verifier и Unit Test Generator.
3. Реализовать fail-fast: остановка и понятное сообщение об этапе при сбое любого агента.
4. Прогнать полный цикл на seeded баге из Фазы 2 и убедиться, что все артефакты создаются корректно.

## Фаза 9 — Документация и скриншоты
1. Написать `README.md`: обзор, запуск pipeline и приложения, обоснование выбора модели на агента, author/student info.
2. Написать `HOWTORUN.md`.
3. Сделать скриншоты в `docs/screenshots/`: запуск pipeline, применённые фиксы, security-скан, прогон unit-тестов.

## Приоритеты
- **P0**: sample-приложение с seeded багами + скелет оркестратора + 4 обязательных агента (минимально рабочие версии).
- **P1**: skills (`research-quality-measurement`, `unit-tests-FIRST`) полностью интегрированы и используются агентами.
- **P2**: вспомогательные Bug Researcher/Bug Planner доведены до стабильной работы в общем прогоне.
- **P3**: документация, скриншоты, финальная полировка README/HOWTORUN.
