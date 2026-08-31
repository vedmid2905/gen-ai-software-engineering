import unittest

import context  # noqa: F401  (adds src/ to sys.path)

from taskflow.store import TaskStore


class TaskStoreBaselineTests(unittest.TestCase):
    """Baseline coverage that existed before the pipeline's seeded-bug investigation.

    These tests intentionally avoid the edge cases described in
    context/bugs/001/bug-context.md, so they pass both before and after the fix.
    """

    def setUp(self):
        self.store = TaskStore()

    def test_add_and_get(self):
        task = self.store.add("Buy milk", priority=2)
        self.assertEqual(self.store.get(task.id), task)

    def test_get_missing_returns_none(self):
        self.assertIsNone(self.store.get(999))

    def test_complete_marks_task(self):
        task = self.store.add("Write report", priority=1)
        self.assertTrue(self.store.complete(task.id))
        self.assertTrue(self.store.get(task.id).completed)

    def test_complete_missing_returns_false(self):
        self.assertFalse(self.store.complete(999))

    def test_search_matches_substring_not_at_start(self):
        self.store.add("Buy milk", priority=1)
        results = self.store.search("milk")
        self.assertEqual(len(results), 1)

    def test_sorted_by_priority_single_digit(self):
        self.store.add("Low", priority=1)
        self.store.add("High", priority=3)
        ordered = self.store.sorted_by_priority()
        self.assertEqual([t.priority for t in ordered], [3, 1])

    def test_stats_empty_store(self):
        self.assertEqual(
            self.store.stats(),
            {"total": 0, "completed": 0, "completion_rate": 0.0},
        )

    def test_stats_with_completed_tasks(self):
        t1 = self.store.add("A", priority=1)
        self.store.add("B", priority=1)
        self.store.complete(t1.id)
        stats = self.store.stats()
        self.assertEqual(stats["total"], 2)
        self.assertEqual(stats["completed"], 1)
        self.assertEqual(stats["completion_rate"], 50.0)


if __name__ == "__main__":
    unittest.main()
