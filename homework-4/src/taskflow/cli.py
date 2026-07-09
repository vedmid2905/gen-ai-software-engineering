"""Command-line interface for TaskFlow."""

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


def _print_task(task) -> None:
    status = "x" if task.completed else " "
    print(f"[{status}] #{task.id} (p{task.priority}) {task.title}")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="taskflow", description="Minimal in-memory task manager")
    sub = parser.add_subparsers(dest="command", required=True)

    add_p = sub.add_parser("add", help="Add a new task")
    add_p.add_argument("title")
    add_p.add_argument("--priority", type=int, default=1)

    list_p = sub.add_parser("list", help="List tasks")
    list_p.add_argument("--search", default=None, help="Only show tasks whose title contains this term")
    list_p.add_argument("--sort", choices=["priority"], default=None)

    complete_p = sub.add_parser("complete", help="Mark a task complete")
    complete_p.add_argument("task_id", type=int)

    sub.add_parser("stats", help="Show completion statistics")

    export_p = sub.add_parser("export", help="Export task titles to a file")
    export_p.add_argument("filename")
    export_p.add_argument("--grep", default="", help="Only export titles matching this shell grep pattern")

    return parser


def main(argv=None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    if args.command == "add":
        task = _store.add(args.title, args.priority)
        _print_task(task)
    elif args.command == "list":
        tasks = _store.search(args.search) if args.search else _store.all()
        if args.sort == "priority":
            tasks = sorted(tasks, key=lambda t: t.priority, reverse=True) if not args.search else _store.sorted_by_priority()
        for task in tasks:
            _print_task(task)
    elif args.command == "complete":
        if not _store.complete(args.task_id):
            print(f"No such task: {args.task_id}", file=sys.stderr)
            return 1
    elif args.command == "stats":
        print(_store.stats())
    elif args.command == "export":
        path = export_tasks(args.filename, args.grep)
        print(f"Exported to {path}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
