import io
import os
import tempfile
import unittest
from contextlib import redirect_stdout

import context  # noqa: F401  (adds src/ to sys.path)

from taskflow import cli


class CliBaselineTests(unittest.TestCase):
    """Baseline CLI coverage; does not exercise the export --grep path (see bug-context.md)."""

    def setUp(self):
        cli._store = cli.TaskStore()

    def test_add_prints_task(self):
        out = io.StringIO()
        with redirect_stdout(out):
            cli.main(["add", "Buy milk", "--priority", "2"])
        self.assertIn("Buy milk", out.getvalue())

    def test_complete_unknown_task_returns_error_code(self):
        exit_code = cli.main(["complete", "999"])
        self.assertEqual(exit_code, 1)

    def test_export_without_grep_writes_file(self):
        cli.main(["add", "Buy milk"])
        with tempfile.TemporaryDirectory() as tmp:
            out_path = os.path.join(tmp, "out.txt")
            cli.main(["export", out_path])
            with open(out_path, encoding="utf-8") as f:
                self.assertIn("Buy milk", f.read())


if __name__ == "__main__":
    unittest.main()
