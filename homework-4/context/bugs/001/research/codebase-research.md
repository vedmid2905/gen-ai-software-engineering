# Codebase Research: Bug 001 — TaskFlow

## Issue 1: Search excludes matches at the start of the title

- **File**: `src/taskflow/store.py`
- **Location**: lines 41-47, specifically line 46
- **Quoted Snippet**:
  ```python
  def search(self, term: str) -> list[Task]:
      """Return tasks whose title contains the given search term (case-insensitive)."""
      term_lower = term.lower()
      return [
          task for task in self._tasks.values()
          if task.title.lower().find(term_lower) > 0
      ]
  ```
- **Explanation**: `str.find` returns the index of the first match, or `-1` if not
  found. When the search term occurs at the very start of the title, `find` returns
  `0`. The condition `task.title.lower().find(term_lower) > 0` evaluates `0 > 0` as
  `False`, so a task whose title *starts with* the search term is excluded from the
  results. Only titles where the match starts at index 1 or later are returned. This
  matches the symptom in `bug-context.md` (`search("Milk")` against title `"Milk
  run"` returns `[]`).

## Issue 2: Priority sort compares priorities as strings

- **File**: `src/taskflow/store.py`
- **Location**: lines 49-51, specifically line 51
- **Quoted Snippet**:
  ```python
  def sorted_by_priority(self) -> list[Task]:
      """Return tasks ordered from highest to lowest priority."""
      return sorted(self._tasks.values(), key=lambda t: str(t.priority), reverse=True)
  ```
- **Explanation**: The sort key is `str(t.priority)` — the priority integer is
  converted to a string before comparison, so ordering is lexicographic rather than
  numeric. Lexicographically, `"2" > "10"` (because `"2"` > `"1"` at the first
  character), so with `reverse=True` a task with priority `2` sorts ahead of a task
  with priority `10`, even though `10` is the numerically higher priority. This
  matches the symptom in `bug-context.md`.

## Issue 3 (Security): Command injection in `export_tasks`

- **File**: `src/taskflow/cli.py`
- **Location**: lines 12-25, specifically lines 18-20
- **Quoted Snippet**:
  ```python
  if grep_term:
      command = f"grep '{grep_term}' _export_source.txt > {filename}"
      subprocess.run(command, shell=True, check=False)
  ```
- **Explanation**: Both `grep_term` and `filename` are user-supplied CLI arguments
  (`--grep` and the positional `filename` on the `export` subcommand, see
  `build_parser`, lines 50-52) that are interpolated directly into a string passed to
  `subprocess.run(..., shell=True)`. Neither value is escaped or validated, so any
  shell metacharacter (`;`, `&`, backticks, `$()`, etc., depending on the underlying
  shell) in either argument is interpreted by the shell instead of being treated as
  literal data. This matches the symptom in `bug-context.md`, which records a
  manually verified exploit that plants an arbitrary file via the `--grep` argument.

## References

- `src/taskflow/store.py` (read in full)
- `src/taskflow/cli.py` (read in full)
- `context/bugs/001/bug-context.md` (source of the reported symptoms)

No discrepancies between the reported symptoms and the current source were found
during this investigation.
