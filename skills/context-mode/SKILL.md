name: context-mode
description: Context window optimization and mandatory routing rules based on mksglu/context-mode. Use whenever analyzing, counting, filtering, comparing, searching, parsing, or transforming large data, fetching URLs, or running data-heavy commands so raw bytes stay out of the context window.

Context Mode

Context window optimization skill based on mksglu/context-mode. It enforces the "Think in Code" paradigm and sandbox tool routing to prevent raw data dumps from flooding the conversation context window.

When to Use

Analyzing, counting, filtering, comparing, searching, parsing, or transforming datasets or multi-file repositories

Fetching external URLs or documentation pages without dumping raw HTML into the context window

Running shell commands or inspections that generate large outputs (logs, test outputs, snapshots, issue lists)

Operating in an environment equipped with context-mode MCP tools (ctx_execute, ctx_batch_execute, ctx_execute_file, ctx_index, ctx_search, ctx_fetch_and_index) or a code execution sandbox

Steps

1. Think in Code (Mandatory Paradigm)

Stop treating the LLM as a raw data processor; treat it as a code generator:

Program the analysis, do not compute it by reading: Instead of reading 50 files into context to count functions or inspect patterns, write a script that performs the analysis and prints (console.log() or print()) only the final summary or answer.

Isolate stdout: Execute analysis scripts inside the sandbox (ctx_execute(language, code) or code interpreter) so only compact standard output enters the conversation context.

2. Enforce Tool Selection and Routing

When context-mode MCP tools are available, route operations strictly by task type:

GATHER (ctx_batch_execute): Use ctx_batch_execute(commands, queries) to run multiple commands, automatically index the outputs into the FTS5 knowledge base, and return targeted search snippets in a single call.

PROCESS (ctx_execute / ctx_execute_file): Use ctx_execute(language, code) for general script execution or ctx_execute_file(path, language, code) to inspect and process large local files without loading raw file bytes into context.

WEB (ctx_fetch_and_index + ctx_search): Never use raw curl or wget that dumps raw HTTP responses into context. Fetch and convert pages into indexed chunks via ctx_fetch_and_index(url, source) and query them with ctx_search(queries).

SEARCH (ctx_search): Batch all related lookup questions into a single ctx_search(queries: [...]) call against indexed content.

3. Avoid Blocked Context-Flooding Patterns

Do not run raw curl or wget directly into standard context output.

Do not run un-sandboxed inline HTTP dumps (node -e "fetch(...)", python -c "requests.get(...)") unless wrapped inside a sandbox script that filters and prints only the extracted result.

Do not read large log files, build artifacts, or snapshots line-by-line into the prompt window.

Write large generated artifacts directly to files and return the file path plus a one-line description rather than dumping large blocks inline.

Gotchas

Progressive throttling awareness: Repeated sequential search calls can trigger throttling in context-mode. Combine multiple questions into a single ctx_search(queries: [...]) or ctx_batch_execute(commands, queries) call instead of chaining single queries.

Intent-driven filtering: When command output exceeds 5 KB, provide clear search queries or intents so the FTS5 / BM25 index returns relevant snippets rather than truncated headers.

No forced prose degradation: Keep raw tool data out of the context window, but never sacrifice the clarity, completeness, or quality of the final explanation presented to the user.
