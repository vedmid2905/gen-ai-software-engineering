"""TaskFlow: a minimal in-memory task manager used as the pipeline's sample application."""

from .store import Task, TaskStore

__all__ = ["Task", "TaskStore"]
