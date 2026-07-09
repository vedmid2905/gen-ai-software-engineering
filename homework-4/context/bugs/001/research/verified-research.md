# Verified Research: Bug 001 — TaskFlow

## Verification Summary

- **Result**: PASS
- **Research Quality (per `skills/research-quality-measurement.md`)**: **Level 4 — VERIFIED**

All three file:line claims in `research/codebase-research.md` were opened and checked
against the current source in this session. All three quoted snippets match the
source byte-for-byte at the cited lines, and all three seeded issues listed in
`context/bugs/001/bug-context.md` are covered by the research.

## Verified Claims

1. **Search off-by-one** — `src/taskflow/store.py:46`. Quoted snippet
   `if task.title.lower().find(term_lower) > 0` confirmed verbatim at that line.
   Confirmed `str.find` returns `0` for a match at index 0, and `0 > 0` is `False`,
   so a title starting with the search term is excluded. Matches
   `bug-context.md` Issue 1 exactly.

2. **Lexicographic priority sort** — `src/taskflow/store.py:51`. Quoted snippet
   `return sorted(self._tasks.values(), key=lambda t: str(t.priority), reverse=True)`
   confirmed verbatim at that line. Confirmed `str(2) > str(10)` under Python's
   default string comparison (`"2" > "10"` is `True`), so with `reverse=True` a
   priority-2 task is ordered ahead of a priority-10 task. Matches `bug-context.md`
   Issue 2 exactly.

3. **Command injection in `export_tasks`** — `src/taskflow/cli.py:18-20`. Quoted
   snippet
   ```python
   if grep_term:
       command = f"grep '{grep_term}' _export_source.txt > {filename}"
       subprocess.run(command, shell=True, check=False)
   ```
   confirmed verbatim at those lines. Confirmed both `grep_term` and `filename`
   originate from CLI-supplied arguments (`cli.py:50-52`) and are interpolated into
   a `shell=True` command with no escaping or validation. Matches `bug-context.md`
   Issue 3 exactly, including the recorded exploit.

## Discrepancies Found

None found. All quoted snippets matched the current source exactly, and no
fabricated or drifted file:line references were present.

## Research Quality Assessment

- **Reference accuracy**: 3/3 file:line claims matched source exactly = **100%**
- **Completeness**: 3/3 seeded issues from `bug-context.md` covered = **all items covered**
- **Level**: **4 — VERIFIED**
- **Reasoning**: Every quoted snippet was independently re-read from the current
  source and matched character-for-character, and every issue documented in
  `bug-context.md` (search off-by-one, lexicographic sort, command injection) has a
  corresponding, accurate entry in the research. No claim required correction, so
  this research can be used directly as input to the implementation plan without
  re-verification of individual claims.

## References

- `src/taskflow/store.py` (re-read in full to check claims 1 and 2)
- `src/taskflow/cli.py` (re-read in full to check claim 3)
- `context/bugs/001/bug-context.md` (source of the seeded issues)
- `context/bugs/001/research/codebase-research.md` (document under verification)
