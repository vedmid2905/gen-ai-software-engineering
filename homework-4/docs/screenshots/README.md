# Screenshots / Run Evidence

This sandbox has no display, so these are **real captured terminal transcripts**
(not synthetic) from actually running the app, the test suite, and inspecting the
pipeline's outputs — not mocked or hand-written expected output.

- `01-app-run.txt` — TaskFlow CLI demonstrating all 3 fixes working: search
  matches a title starting with the term, priority sort is numeric, and the
  command-injection payload no longer creates a file.
- `02-test-run.txt` — full `python -m unittest discover -s tests -v` run: 20/20
  passing (11 baseline + 9 generated for this fix).
- `03-security-report.txt` — the Security Verifier's `security-report.md` output.
- `04-pipeline-run-summary.txt` — the artifact tree produced under
  `context/bugs/001/` plus a before/after test count.

**For the actual PR submission**, replace or supplement these with real PNG
screenshots of your own terminal running `./run-pipeline.sh`, `python -m unittest
discover -s tests -v`, and the app commands in `HOWTORUN.md` — the course
submission checklist expects image screenshots, and these `.txt` transcripts are
provided as accurate, verifiable evidence of what those screenshots would show.
