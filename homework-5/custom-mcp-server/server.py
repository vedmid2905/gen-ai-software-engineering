"""Custom MCP server built with FastMCP.

Exposes the contents of lorem-ipsum.md two ways:
  - as a Resource (a URI Claude can read directly), and
  - as a Tool named `read` (an action Claude can call), which
    returns the same content as the resource.

Resources are URIs that Claude can read from (e.g. files, APIs) -
they are pulled into context like a file, without Claude having to
invoke anything. Tools are actions Claude can call to perform an
operation (e.g. reading a file, running a command, hitting an API)
and get a result back.
"""

from pathlib import Path

from fastmcp import FastMCP

LOREM_IPSUM_PATH = Path(__file__).parent / "lorem-ipsum.md"
DEFAULT_WORD_COUNT = 30

mcp = FastMCP("lorem-ipsum-server")


def _read_words(word_count: int = DEFAULT_WORD_COUNT) -> str:
    text = LOREM_IPSUM_PATH.read_text(encoding="utf-8")
    words = text.split()
    return " ".join(words[:word_count])


@mcp.resource("lorem://lorem-ipsum/{word_count}")
def lorem_ipsum_resource(word_count: int = DEFAULT_WORD_COUNT) -> str:
    """Return the first `word_count` words from lorem-ipsum.md."""
    return _read_words(word_count)


@mcp.tool
def read(word_count: int = DEFAULT_WORD_COUNT) -> str:
    """Read lorem-ipsum.md and return exactly `word_count` words (default 30)."""
    return _read_words(word_count)


if __name__ == "__main__":
    mcp.run()
