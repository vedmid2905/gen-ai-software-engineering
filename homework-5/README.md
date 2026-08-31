# Homework 5: MCP Servers Configuration

This project demonstrates the configuration and integration of four MCP (Model Context Protocol) servers:

1. **GitHub MCP** - Connect Claude/Copilot to GitHub repositories
2. **Filesystem MCP** - Connect Claude/Copilot to local filesystem
3. **Jira/Notion MCP** - Connect Claude/Copilot to Jira or Notion
4. **Custom MCP Server** - A custom FastMCP server for reading content with word limit control

## Author

Kostiantyn Vedmid

## Project Structure

- `README.md` - This file
- `HOWTORUN.md` - Installation and usage instructions
- `VERIFICATION.md` - Verification report checking this repo against `TASKS.md` (in Russian)
- `.mcp.json` - MCP servers configuration (all four servers)
- `custom-mcp-server/` - Custom FastMCP server implementation
  - `server.py` - FastMCP server with a `lorem-ipsum` resource and a `read` tool
  - `lorem-ipsum.md` - Source text served by the resource/tool
  - `requirements.txt` - Python dependencies (`fastmcp`)
- `docs/screenshots/` - Screenshots of MCP interactions

## Overview

Each MCP server is configured to allow Claude to interact with different data sources and services through standardized tools and resources.

- **Resources** are URIs that Claude can read from (e.g. files, APIs) - they expose passive, addressable content.
- **Tools** are actions Claude can call to perform an operation (e.g. reading a file, running a command) and get a result back.

The custom server (`custom-mcp-server/server.py`) demonstrates both: a `lorem://lorem-ipsum/{word_count}` resource and a `read` tool, both returning exactly `word_count` words (default 30) from `lorem-ipsum.md`.

See `HOWTORUN.md` for detailed setup, connection, and testing instructions.
