# Fix Summary: Bug 001 — TaskFlow

Source: `context/bugs/001/implementation-plan.md`. All 3 steps applied in order; tests
run after each step.

## Changes Made

### 1. Search off-by-one — `src/taskflow/store.py:41-47`
- **Before**:
  ```python
  if task.title.lower().find(term_lower) > 0
  ```
- **After**:
  ```python
  if term_lower in task.title.lower()
  ```
- **Test result**: `python -m unittest discover -s tests -v` → **11/11 passed**

### 2. Lexicographic priority sort — `src/taskflow/store.py:49-51`
- **Before**:
  ```python
  return sorted(self._tasks.values(), key=lambda t: str(t.priority), reverse=True)
  ```
- **After**:
  ```python
  return sorted(self._tasks.values(), key=lambda t: t.priority, reverse=True)
  ```
- **Test result**: `python -m unittest discover -s tests -v` → **11/11 passed**

### 3. Command injection in `export_tasks` — `src/taskflow/cli.py` (was lines 1-25)
- **Before**:
  ```python
  import argparse
  import subprocess
  import sys

  from .store import TaskStore

  _store = TaskStore()


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
  import argparse
  import sys

  from .store import TaskStore

  _store = TaskStore()


  def export_tasks(filename: str, grep_term: str = "") -> str:
      """Write task titles to a file, optionally filtered to titles containing grep_term."""
      titles = [t.title for t in _store.all()]
      if grep_term:
          titles = [title for title in titles if grep_term in title]
      with open(filename, "w", encoding="utf-8") as f:
          f.write("\n".join(titles))
      return filename
  ```
- **Test result**: `python -m unittest discover -s tests -v` → **11/11 passed**

## Overall Status

**Success** — all 3 plan steps applied; full suite passes after every step (11/11).

## Manual Verification

Reproduced the fixed behavior directly against the sample app:

```python
# Search now matches at index 0
s = TaskStore(); s.add("Milk run", 1)
s.search("Milk")            # -> [Task(id=1, title='Milk run', priority=1, ...)]  (was [])

# Priority sort is now numeric
s2 = TaskStore(); s2.add("Low prio", 2); s2.add("High prio", 10)
s2.sorted_by_priority()     # -> [priority 10, priority 2]  (was [priority 2, priority 10])

# Injection payload no longer executes — it is now a literal (non-matching) substring
export_tasks("taskflow_out.txt", "milk & echo pwned> PWNED_by_injection.txt & rem")
# PWNED_by_injection.txt is NOT created (previously it was)
```

To reproduce via the CLI:
```
python run.py add "Milk run" --priority 1
python run.py list --search Milk
python run.py add "Low prio" --priority 2
python run.py add "High prio" --priority 10
python run.py list --sort priority
```

## References

- Plan: `context/bugs/001/implementation-plan.md`
- Verified research: `context/bugs/001/research/verified-research.md`
- Changed files: `src/taskflow/store.py`, `src/taskflow/cli.py`
