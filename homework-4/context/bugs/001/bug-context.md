# Bug Context: 001 — TaskFlow search/sort bugs and export command injection

## Application
`TaskFlow` — a minimal in-memory task manager (Python stdlib only), located at `src/taskflow/`.
Run it via `python run.py <command> [args...]`; run its test suite via
`python -m unittest discover -s tests -v`.

## Seeded Issue 1 (Bug): Search excludes matches at the start of the title
- **File**: `src/taskflow/store.py`
- **Function**: `TaskStore.search`
- **Symptom**: A task whose title *starts with* the search term is not returned, because the
  code checks `title.lower().find(term_lower) > 0` instead of `>= 0`. `str.find` returns `0`
  when the match is at the very beginning of the string, and `0 > 0` is `False`.
- **Reproduction**:
  ```python
  store = TaskStore()
  store.add("Milk run", priority=1)
  store.search("Milk")   # returns [] — expected the task above
  ```
- **Expected behavior**: `search` should return any task whose title contains the term,
  regardless of the match position, including position 0.

## Seeded Issue 2 (Bug): Priority sort compares priorities as strings
- **File**: `src/taskflow/store.py`
- **Function**: `TaskStore.sorted_by_priority`
- **Symptom**: `sorted_by_priority` sorts using `key=lambda t: str(t.priority)`, i.e.
  lexicographic (string) order instead of numeric order. This makes double-digit priorities
  sort incorrectly relative to single-digit ones (e.g. priority `2` outranks priority `10`).
- **Reproduction**:
  ```python
  store = TaskStore()
  store.add("Low prio", priority=2)
  store.add("High prio", priority=10)
  [t.priority for t in store.sorted_by_priority()]   # [2, 10] — expected [10, 2]
  ```
- **Expected behavior**: Tasks should be ordered by numeric priority, highest first.

## Seeded Issue 3 (Security — CWE-78 OS Command Injection): `export --grep`
- **File**: `src/taskflow/cli.py`
- **Function**: `export_tasks`
- **Symptom**: When a `grep_term` is supplied, it is interpolated directly into a shell
  command string and executed with `subprocess.run(command, shell=True)`. The `filename`
  argument is interpolated the same way. Neither value is sanitized or escaped, so shell
  metacharacters in either argument let an attacker run arbitrary commands.
- **Reproduction** (Windows `cmd.exe`; use `;` instead of `&` on a POSIX shell):
  ```python
  from taskflow.cli import export_tasks, _store
  _store.add("Buy milk", 1)
  export_tasks("out.txt", "milk & echo pwned> PWNED_by_injection.txt & rem")
  # creates PWNED_by_injection.txt containing "pwned" — arbitrary command execution
  ```
  This was manually verified: the command above creates a file outside the intended
  export target, proving arbitrary command execution via the `--grep` CLI flag.
- **Expected behavior**: `export` must never pass user-supplied input to a shell. Filtering
  and file writes should be done in-process (or via `subprocess.run([...], shell=False)`
  with an argument list), with no shell metacharacter interpretation.

## Existing Test Coverage
`tests/test_store.py` and `tests/test_cli.py` contain baseline tests that intentionally do
**not** exercise the three cases above (e.g. the search test uses a term that isn't at
index 0, the sort test only uses single-digit priorities, and the export test never
passes `--grep`). `python -m unittest discover -s tests -v` passes today even though all
three issues are present — the pipeline is expected to add tests that do expose them.
