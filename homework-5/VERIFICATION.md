# Отчёт о проверке выполнения TASKS.md

Дата проверки: 2026-07-09

Ниже — результат повторной проверки соответствия репозитория требованиям
`TASKS.md` (Homework 5: Configure MCP Servers).

## Задача 1: GitHub MCP ⭐

**Статус: выполнено ✅**

- Сервер `github` зарегистрирован в [`.mcp.json`](.mcp.json) (HTTP, через
  `api.githubcopilot.com/mcp`, токен передаётся через переменную окружения
  `GITHUB_PERSONAL_ACCESS_TOKEN`, реальный токен в репозиторий не сохранён).
- Скриншот реального взаимодействия: `docs/screenshots/github-mcp-result.png`
  — показывает подключение к GitHub через MCP и список репозиториев
  аккаунта `vedmid2905`.

Критерии успеха выполнены полностью.

## Задача 2: Filesystem MCP ⭐

**Статус: выполнено с оговоркой ⚠️**

- Сервер `filesystem` зарегистрирован в `.mcp.json` (stdio,
  `npx @modelcontextprotocol/server-filesystem`).
- В ходе этой сессии сервер был реально подключён и протестирован —
  вызовы `list_directory` и `read_text_file` через MCP filesystem
  выполнены успешно (видно в истории диалога).
- Скриншот `docs/screenshots/filesystem-mcp-result.png`, который остаётся
  в репозитории по решению автора, фактически показывает вызов встроенного
  инструмента `Read` в Claude Code, а не вызов MCP-сервера `filesystem`
  (инструменты `list_directory`/`read_text_file`). Формально сервер
  настроен и работает, но зафиксированный на скриншоте вызов не проходит
  через него.

Рекомендация: при желании — заменить скриншот на реальный вызов
`list_directory`/`read_text_file`, продемонстрированный в этой сессии.

## Задача 3: Jira или Notion MCP ⭐⭐

**Статус: выполнено с оговоркой ⚠️**

- Сервер `atlassian` зарегистрирован в `.mcp.json` (HTTP,
  `mcp.atlassian.com/v1/mcp/authv2`).
- В текущей сессии сервер **не авторизован** (OAuth-подключение требует
  интерактивного шага через `/mcp` в Claude Code и не может быть выполнено
  автоматически) — прямой вызов инструментов Jira через MCP в этой сессии
  не проверялся.
- Скриншот `docs/screenshots/jira-or-notion-mcp-result.png`, который
  остаётся в репозитории по решению автора, показывает список задач из
  другого инструмента ("Kiro") по запросу, отличному от требуемого
  ("покажи таски, які на мене" вместо "дай тикеты последних 5 багов").
  Формально скриншот демонстрирует работающую интеграцию с Jira, но не
  точно соответствует формулировке задания.

Рекомендация: при желании — авторизовать `atlassian` через `/mcp` и
повторить именно запрос "Дай тикеты последних 5 багов по проекту
[название]".

## Задача 4: Custom MCP Server (FastMCP) ⭐⭐⭐

**Статус: код выполнен полностью, скриншот отсутствует ⚠️**

Реализовано и проверено локально (через `fastmcp.Client`, in-memory):

- [`custom-mcp-server/server.py`](custom-mcp-server/server.py) —
  сервер на FastMCP:
  - **Resource** `lorem://lorem-ipsum/{word_count}` — читает
    `lorem-ipsum.md` и возвращает ровно `word_count` слов (по умолчанию 30).
  - **Tool** `read(word_count=30)` — вызывается Claude, возвращает тот же
    контент, что и ресурс.
- [`custom-mcp-server/lorem-ipsum.md`](custom-mcp-server/lorem-ipsum.md) —
  исходный текст (321 слово — достаточно для любых проверяемых
  `word_count`).
- [`custom-mcp-server/requirements.txt`](custom-mcp-server/requirements.txt)
  — явно указана зависимость `fastmcp>=2.0.0`.
- Сервер `lorem-ipsum` зарегистрирован в `.mcp.json` (stdio,
  `python custom-mcp-server/server.py`).

Результаты локального теста (`fastmcp.Client`, без внешнего MCP-клиента):

| Вызов | Результат | Количество слов |
|---|---|---|
| `read()` (по умолчанию) | `# Lorem Ipsum Lorem ipsum dolor sit amet...` | 30 ✅ |
| `read(word_count=10)` | `# Lorem Ipsum Lorem ipsum dolor sit amet, consectetur adipiscing` | 10 ✅ |
| `lorem://lorem-ipsum/15` (resource) | `# Lorem Ipsum Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor` | 15 ✅ |

**Не хватает:** `docs/screenshots/custom-mcp-read-tool-result.png` —
скриншот вызова инструмента `read` через реальный MCP-клиент (Claude Code
с подключённым `.mcp.json`) ещё не сделан. Это единственный отсутствующий
файл из ожидаемой структуры проекта.

## Проверка структуры проекта и зависимостей

| Файл / папка | Статус |
|---|---|
| `README.md` (с именем автора) | ✅ есть |
| `HOWTORUN.md` | ✅ есть |
| `.mcp.json` (все 4 сервера) | ✅ есть |
| `custom-mcp-server/server.py` | ✅ есть |
| `custom-mcp-server/lorem-ipsum.md` | ✅ есть |
| `custom-mcp-server/requirements.txt` (с `fastmcp`) | ✅ есть |
| `docs/screenshots/github-mcp-result.png` | ✅ есть |
| `docs/screenshots/filesystem-mcp-result.png` | ⚠️ есть, но не отражает вызов MCP-сервера (см. Задачу 2) |
| `docs/screenshots/jira-or-notion-mcp-result.png` | ⚠️ есть, но не соответствует точной формулировке запроса (см. Задачу 3) |
| `docs/screenshots/custom-mcp-read-tool-result.png` | ❌ отсутствует |

## Итог

- Задача 1 (GitHub MCP) — полностью выполнена.
- Задача 2 (Filesystem MCP) — сервер настроен и реально протестирован в
  этой сессии; скриншот в репозитории сохранён по решению автора, хотя
  формально не демонстрирует вызов именно MCP-сервера.
- Задача 3 (Jira/Notion MCP) — сервер настроен; авторизация и точный
  запрос из задания не подтверждены в этой сессии; скриншот сохранён по
  решению автора.
- Задача 4 (Custom MCP Server) — код полностью реализован и протестирован
  локально; единственный недостающий элемент — скриншот вызова `read`
  через реальный MCP-клиент.

Основной оставшийся шаг перед сдачей: сделать
`docs/screenshots/custom-mcp-read-tool-result.png`, подключив `.mcp.json`
в Claude Code и вызвав инструмент `read` сервера `lorem-ipsum`.
