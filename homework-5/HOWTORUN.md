# How to Run

This document explains how to install, run, connect, and test everything in
this homework, with a focus on the custom `lorem-ipsum` MCP server
(`custom-mcp-server/`).

## 1. Install dependencies

The custom server only needs `fastmcp`:

```bash
cd custom-mcp-server
pip install -r requirements.txt
```

The `filesystem` MCP server is run via `npx` and needs Node.js installed
(no local install required — `npx` fetches
`@modelcontextprotocol/server-filesystem` on first run).

The `github` and `atlassian` (Jira/Notion) servers are hosted, remote HTTP
MCP servers — no local install is required for them either, only
authentication (see step 3).

## 2. Run the custom server standalone

To sanity-check the server on its own (stdio transport):

```bash
cd custom-mcp-server
python server.py
```

The process will sit and wait for MCP messages on stdin/stdout — that's
expected; it's meant to be launched by an MCP client (Claude Code, Claude
Desktop, etc.), not run interactively. Press `Ctrl+C` to stop it.

You can also run it with the FastMCP CLI inspector for interactive testing:

```bash
fastmcp dev custom-mcp-server/server.py
```

## 3. Connect the MCP configuration

All four servers are registered in [`.mcp.json`](.mcp.json) at the repo
root:

| Server | Type | Transport |
|---|---|---|
| `github` | GitHub MCP | HTTP (Copilot-hosted) |
| `filesystem` | Filesystem MCP | stdio (`npx @modelcontextprotocol/server-filesystem`) |
| `atlassian` | Jira/Confluence MCP | HTTP |
| `lorem-ipsum` | Custom FastMCP server | stdio (`python custom-mcp-server/server.py`) |

Claude Code automatically picks up `.mcp.json` in the project root. When you
open this project in Claude Code:

1. It detects the four servers in `.mcp.json`.
2. `github` requires a `GITHUB_PERSONAL_ACCESS_TOKEN` environment variable
   (set it in your shell, or replace the placeholder in `.mcp.json` locally —
   never commit a real token).
3. `atlassian` requires an interactive OAuth login the first time — run
   `/mcp` inside Claude Code and follow the authorization link.
4. `filesystem` and `lorem-ipsum` need no credentials and connect
   immediately.

Run `/mcp` inside Claude Code at any time to see connection status for all
four servers.

## 4. Use and test the custom `read` tool

Once connected, ask Claude Code (or any MCP client) to call the tool, e.g.:

> Use the lorem-ipsum MCP server's `read` tool to get 15 words from
> lorem-ipsum.md.

Expected behavior:
- `read()` with no arguments returns exactly the first **30** words of
  `custom-mcp-server/lorem-ipsum.md`.
- `read(word_count=N)` returns exactly the first **N** words.
- The `lorem://lorem-ipsum/{word_count}` **resource** returns the same
  content and can be read directly (without an explicit tool call) by
  clients that support resource reads.

### Local test without a full MCP client

You can verify both the resource and the tool in-process with the FastMCP
`Client`, without spinning up Claude Code:

```python
import asyncio
from fastmcp import Client
from server import mcp  # from within custom-mcp-server/

async def main():
    async with Client(mcp) as client:
        result = await client.call_tool("read", {"word_count": 10})
        print(result.data)

        resource = await client.read_resource("lorem://lorem-ipsum/10")
        print(resource[0].text)

asyncio.run(main())
```

Both calls should print exactly 10 words from `lorem-ipsum.md`.

## Resources vs. Tools (why the server has both)

- **Resources** are URIs Claude can read from directly (files, APIs, etc.) —
  here, `lorem://lorem-ipsum/{word_count}` reads `lorem-ipsum.md` and
  returns the requested number of words as passive, addressable content.
- **Tools** are actions Claude actively calls to perform an operation and
  get a result back — here, the `read` tool wraps the same logic as the
  resource so Claude can invoke it explicitly (e.g. "call `read` with
  `word_count=50`") rather than just reading a fixed URI.
