"""Runnable entry point for TaskFlow. Usage: python run.py <command> [args...]"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / "src"))

from taskflow.cli import main  # noqa: E402

if __name__ == "__main__":
    raise SystemExit(main())
