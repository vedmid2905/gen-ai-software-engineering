# Test Report: Bug 001 — TaskFlow

**Scope**: Tests generated only for the files listed as changed in `fix-summary.md`:
`src/taskflow/store.py` (`search`, `sorted_by_priority`) and `src/taskflow/cli.py`
(`export_tasks`). New file: `tests/test_bug_001_fixes.py`. Skill used:
`skills/unit-tests-FIRST.md`.

## Generated Tests

| Test | Covers | FIRST property emphasized | Result |
|------|--------|---------------------------|--------|
| `test_search_matches_title_starting_with_term` | `store.py::TaskStore.search` (Change 1) | **Self-validating** — asserts the exact match returned, not just non-empty | PASS |
| `test_search_is_case_insensitive_at_start` | `store.py::TaskStore.search` (Change 1) | **Independent** — fresh `TaskStore` in `setUp` | PASS |
| `test_search_still_matches_mid_string_term` | `store.py::TaskStore.search` (Change 1, regression guard) | **Repeatable** — no reliance on ordering/time | PASS |
| `test_search_no_match_returns_empty_list` | `store.py::TaskStore.search` (Change 1) | **Fast** — pure in-memory call, no I/O | PASS |
| `test_double_digit_priority_outranks_single_digit` | `store.py::TaskStore.sorted_by_priority` (Change 2) | **Self-validating** — asserts exact expected order `[10, 2]` | PASS |
| `test_sorted_by_priority_is_fully_descending` | `store.py::TaskStore.sorted_by_priority` (Change 2) | **Repeatable** — deterministic input set, no randomness | PASS |
| `test_export_grep_filters_by_plain_substring` | `cli.py::export_tasks` (Change 3) | **Fast/Independent** — uses `tempfile.TemporaryDirectory`, no shared filesystem state | PASS |
| `test_export_grep_term_is_not_shell_interpreted` | `cli.py::export_tasks` (Change 3, security regression) | **Timely** — written immediately alongside the injection fix it verifies | PASS |
| `test_export_without_grep_writes_all_titles` | `cli.py::export_tasks` (Change 3) | **Self-validating** — asserts on file content, not just exit code | PASS |

## Test Run

Command: `python -m unittest discover -s tests -v`

```
Ran 20 tests in 0.023s

OK
```

20/20 tests passed — 11 pre-existing baseline tests (unchanged, still passing) plus
9 new tests added above, all passing against the fixed code in
`src/taskflow/store.py` and `src/taskflow/cli.py`.

## FIRST Compliance Notes

- **Fast**: No test spawns a subprocess or sleeps; `export_tasks` tests use
  `tempfile.TemporaryDirectory`, which is local disk I/O only.
- **Independent**: Every test class resets its own store in `setUp` (`TaskStore()`
  for `store.py` tests, `cli._store = TaskStore()` for `cli.py` tests); no test
  depends on another test's leftover state.
- **Repeatable**: No test depends on wall-clock time or iteration order; temp
  directories are cleaned up automatically by the context manager.
- **Self-validating**: Every test asserts a specific expected value (exact list
  contents, exact file content, exact absence of a file) rather than printing for
  manual inspection.
- **Timely**: All 9 tests were written in this same pipeline run, directly
  alongside the fixes in `fix-summary.md` — no fix in this change is left
  uncovered by a corresponding test.

## References

- `context/bugs/001/fix-summary.md`
- `skills/unit-tests-FIRST.md`
- `tests/test_bug_001_fixes.py`
