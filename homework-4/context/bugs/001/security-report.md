# Security Report: Bug 001 — TaskFlow

**Scope**: Only files listed as changed in `fix-summary.md`: `src/taskflow/store.py`,
`src/taskflow/cli.py`. No code was modified as part of this review.

## Findings

### 1. [RESOLVED] OS Command Injection in `export_tasks` — CWE-78
- **Severity**: N/A (confirmed fixed — recorded for traceability)
- **Location**: `src/taskflow/cli.py:11-18` (previously lines 12-25)
- **Description**: The previous implementation built a shell command string from
  unsanitized `grep_term`/`filename` CLI arguments and executed it via
  `subprocess.run(command, shell=True)`. The current implementation (per
  `fix-summary.md`, Change 3) removes the shell call entirely and filters titles
  with a plain Python substring check (`if grep_term in title`). Re-tested the
  previously working exploit payload (`--grep "milk & echo pwned> PWNED.txt & rem"`)
  against the current code: no external process is spawned and no file is created
  outside the intended `filename` target. **Confirmed fixed.**
- **Remediation**: None required — already remediated by avoiding the shell.

### 2. Unvalidated export filename allows writing to an arbitrary path — CWE-22 / CWE-73
- **Severity**: **HIGH**
- **Location**: `src/taskflow/cli.py:16` (`open(filename, "w", ...)`), reachable from
  `cli.py:44` (`export_p.add_argument("filename")`)
- **Description**: The `export` subcommand's `filename` argument is passed directly
  to `open(filename, "w")` with no validation of its value. A caller can supply a
  path outside the working directory (e.g. an absolute path or a `../`-relative
  path) and `export_tasks` will silently truncate and overwrite whatever file
  exists there, subject only to OS file permissions. This was not part of the
  seeded command-injection bug fixed in this change, but it is present in the same
  function this diff touched and is independently exploitable — a caller who
  controls the `filename` argument gets an arbitrary-file-write primitive.
- **Remediation**: Resolve `filename` against a fixed export directory and reject
  the request if the resolved path escapes it, e.g.:
  ```python
  export_dir = Path("exports").resolve()
  target = (export_dir / filename).resolve()
  if not target.is_relative_to(export_dir):
      raise ValueError("filename must not escape the export directory")
  ```
  Create `export_dir` if missing, and use `target` in place of the raw `filename`
  when opening the file.

### 3. Stale help text still advertises shell-pattern semantics — INFO
- **Severity**: **INFO**
- **Location**: `src/taskflow/cli.py:45`
  (`export_p.add_argument("--grep", ..., help="Only export titles matching this shell grep pattern")`)
- **Description**: The `--grep` flag no longer runs a shell `grep` command — it now
  performs a plain Python substring match (per `fix-summary.md`, Change 3). The help
  text still says "shell grep pattern," which could mislead a future maintainer into
  reintroducing a shell call (e.g. to support real regex/grep semantics) without
  realizing the injection risk that was just removed.
- **Remediation**: Update the help string to describe the current behavior, e.g.
  `help="Only export titles containing this substring"`, and consider a code
  comment noting why this must not be implemented by shelling out.

## Summary

| # | Finding | Severity |
|---|---------|----------|
| 1 | OS command injection in `export_tasks` | Resolved (confirmed fixed) |
| 2 | Unvalidated export filename (path traversal / arbitrary write) | HIGH |
| 3 | Stale `--grep` help text implies shell semantics | INFO |

No hardcoded secrets, insecure comparisons, or unsafe third-party dependencies were
found in the reviewed files. `src/taskflow/store.py`'s changes (search and sort
logic) introduce no security-relevant behavior.
