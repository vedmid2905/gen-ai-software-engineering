#!/usr/bin/env python3
"""FastMCP server exposing the banking pipeline's shared/results/ over MCP.

Tools:
  - get_transaction_status(transaction_id) -> status of one transaction
  - list_pipeline_results()                -> summary of every processed transaction

Resource:
  - pipeline://summary -> the latest pipeline run summary as text
"""
import json
import os
from pathlib import Path

from fastmcp import FastMCP

mcp = FastMCP("pipeline-status")

SUMMARY_FILE_NAME = "pipeline-summary.json"


def _results_dir() -> Path:
    """Locate shared/results/, defaulting to the pipeline's own default location
    (src/shared/results, relative to this repository) or PIPELINE_RESULTS_DIR
    if set."""
    override = os.environ.get("PIPELINE_RESULTS_DIR")
    if override:
        return Path(override)
    return Path(__file__).resolve().parent.parent / "src" / "shared" / "results"


def _read_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


@mcp.tool()
def get_transaction_status(transaction_id: str) -> dict:
    """Return the current pipeline status (validated/rejected/approved/flagged,
    risk score, reason, audit) for a single transaction ID, read from
    shared/results/."""
    file_path = _results_dir() / f"{transaction_id}.json"
    if not file_path.is_file():
        return {
            "error": f"No result found for transaction '{transaction_id}'. "
                     f"Has the pipeline been run yet?"
        }
    return _read_json(file_path)


@mcp.tool()
def list_pipeline_results() -> dict:
    """Return every per-transaction result from the last pipeline run, plus a
    count, read from shared/results/ (the aggregate pipeline-summary.json file
    is excluded from the list)."""
    results_dir = _results_dir()
    if not results_dir.is_dir():
        return {
            "error": f"Results directory not found: {results_dir}. "
                     f"Run the pipeline first (e.g. the /run-pipeline skill)."
        }
    results = []
    for file_path in sorted(results_dir.glob("*.json")):
        if file_path.name == SUMMARY_FILE_NAME:
            continue
        results.append(_read_json(file_path))
    return {"count": len(results), "results": results}


@mcp.resource("pipeline://summary")
def pipeline_summary() -> str:
    """Return the latest pipeline run summary (totals + generated-at timestamp)
    as plain text."""
    summary_path = _results_dir() / SUMMARY_FILE_NAME
    if not summary_path.is_file():
        return ("No pipeline run found yet. Run the pipeline "
                 "(POST /api/pipeline/run, the CLI Integrator, or the "
                 "/run-pipeline skill) first.")
    data = _read_json(summary_path)
    lines = [
        f"Pipeline run at {data.get('generated_at')}",
        f"Total processed: {data.get('total_processed')}",
        f"Approved: {data.get('approved')}",
        f"Flagged: {data.get('flagged')}",
        f"Rejected: {data.get('rejected')}",
    ]
    return "\n".join(lines)


if __name__ == "__main__":
    mcp.run()
