# Implementation Plan: Bug 001 — TaskFlow

Source: `context/bugs/001/research/verified-research.md` (Research Quality: Level 4 — VERIFIED).
All three steps below may be applied in any order — they touch disjoint code paths.

## Step 1: Fix search off-by-one

- **File**: `src/taskflow/store.py`
- **Before**:
  ```python
  def search(self, term: str) -> list[Task]:
      """Return tasks whose title contains the given search term (case-insensitive)."""
      term_lower = term.lower()
      return [
          task for task in self._tasks.values()
          if task.title.lower().find(term_lower) > 0
      ]
  ```
- **After**:
  ```python
  def search(self, term: str) -> list[Task]:
      """Return tasks whose title contains the given search term (case-insensitive)."""
      term_lower = term.lower()
      return [
          task for task in self._tasks.values()
          if term_lower in task.title.lower()
      ]
  ```
- **Reasoning**: Replaces the `find(...) > 0` off-by-one check with the `in`
  operator, which is `True` for a match at any position (including index 0) and
  reads more clearly than comparing against `-1`/`0`.
- **Test command**: `python -m unittest discover -s tests -v`

## Step 2: Fix lexicographic priority sort

- **File**: `src/taskflow/store.py`
- **Before**:
  ```python
  def sorted_by_priority(self) -> list[Task]:
      """Return tasks ordered from highest to lowest priority."""
      return sorted(self._tasks.values(), key=lambda t: str(t.priority), reverse=True)
  ```
- **After**:
  ```python
  def sorted_by_priority(self) -> list[Task]:
      """Return tasks ordered from highest to lowest priority."""
      return sorted(self._tasks.values(), key=lambda t: t.priority, reverse=True)
  ```
- **Reasoning**: Sorts by the numeric `priority` value directly instead of its
  string representation, so priority `10` correctly outranks priority `2`.
- **Test command**: `python -m unittest discover -s tests -v`

## Step 3: Eliminate command injection in `export_tasks`

- **File**: `src/taskflow/cli.py`
- **Before**:
  ```python
  def export_tasks(filename: str, grep_term: str = "") -> str:
      """Write all task titles to a file, optionally filtered through a shell grep pattern."""
      lines = "\n".join(t.title for t in _store.all())
      with open("_export_source.txt", "w", encoding="utf-8") as f:
          f.write(lines)

      if grep_term:
          command = f"grep '{grep_term}' _export_source.txt > {filename}"
          subprocess.run(command, shell=True, check=False)
      else:
          with open(filename, "w", encoding="utf-8") as f:
              f.write(lines)

      return filename
  ```
- **After**:
  ```python
  def export_tasks(filename: str, grep_term: str = "") -> str:
      """Write task titles to a file, optionally filtered to titles containing grep_term."""
      titles = [t.title for t in _store.all()]
      if grep_term:
          titles = [title for title in titles if grep_term in title]
      with open(filename, "w", encoding="utf-8") as f:
          f.write("\n".join(titles))
      return filename
  ```
  Also remove the now-unused `import subprocess` at the top of the file.
- **Reasoning**: Removes the shell entirely instead of trying to escape it —
  filtering is done with a plain Python substring check, and the file is written
  directly with the built-in `open()`. Neither `grep_term` nor `filename` is ever
  passed to a shell, so shell metacharacters in either argument can no longer be
  interpreted as commands. This matches the "expected behavior" already stated in
  `context/bugs/001/bug-context.md`.
- **Test command**: `python -m unittest discover -s tests -v`

## Verification

After all three steps are applied, run the full suite once more:
`python -m unittest discover -s tests -v`
