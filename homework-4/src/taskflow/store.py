"""In-memory task storage and query logic for TaskFlow."""

from dataclasses import dataclass
from itertools import count
from typing import Optional


@dataclass
class Task:
    id: int
    title: str
    priority: int
    completed: bool = False


class TaskStore:
    """Thread-unsafe, process-local task store (sufficient for a CLI demo app)."""

    def __init__(self):
        self._tasks: dict[int, Task] = {}
        self._id_counter = count(1)

    def add(self, title: str, priority: int) -> Task:
        task = Task(id=next(self._id_counter), title=title, priority=priority)
        self._tasks[task.id] = task
        return task

    def get(self, task_id: int) -> Optional[Task]:
        return self._tasks.get(task_id)

    def all(self) -> list[Task]:
        return list(self._tasks.values())

    def complete(self, task_id: int) -> bool:
        task = self._tasks.get(task_id)
        if task is None:
            return False
        task.completed = True
        return True

    def search(self, term: str) -> list[Task]:
        """Return tasks whose title contains the given search term (case-insensitive)."""
        term_lower = term.lower()
        return [
            task for task in self._tasks.values()
            if term_lower in task.title.lower()
        ]

    def sorted_by_priority(self) -> list[Task]:
        """Return tasks ordered from highest to lowest priority."""
        return sorted(self._tasks.values(), key=lambda t: t.priority, reverse=True)

    def stats(self) -> dict:
        tasks = self.all()
        total = len(tasks)
        completed = sum(1 for t in tasks if t.completed)
        rate = (completed / total * 100) if total else 0.0
        return {"total": total, "completed": completed, "completion_rate": round(rate, 2)}
