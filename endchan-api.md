### Documentation Sources
- LynxChan engine docs (core reference):  
  [https://gitgud.io/LynxChan/LynxChan](https://gitgud.io/LynxChan/LynxChan) (especially the `doc/` folder, including `Json.txt` and older `Api.txt` files)
- Practical community summary of the LynxChan API:  
  [https://gitlab.com/catamphetamine/imageboard/-/blob/master/docs/engines/lynxchan.md](https://gitlab.com/catamphetamine/imageboard/-/blob/master/docs/engines/lynxchan.md)
- Libraries that support Endchan (e.g. the `imageboard` npm package lists it as a supported engine)

### Main Read-Only Endpoints (Static JSON)
These work similarly to other imageboard APIs and return plain JSON:

| Endpoint | Description | Example |
|----------|-------------|---------|
| `/index.json` | Site overview (top boards, latest posts/images, stats) | `https://endchan.net/index.json` |
| `/{board}/catalog.json` | Catalog of threads on a board | `https://endchan.net/{board}/catalog.json` |
| `/{board}/{page}.json` | Threads on a specific board page | `https://endchan.net/{board}/1.json` |
| `/{board}/res/{threadId}.json` | Full thread (OP + replies) | `https://endchan.net/{board}/res/{threadId}.json` |
| `/boards.js?json=1` (or similar variants) | Board listing | Various forms exist |

**Media** is served from paths like `/.media/...` (thumbnails often prefixed with `t_`).

### Action / Write API
LynxChan exposes a JSON RPC-style API under paths such as `/.api/...` (or form endpoints with `?json=1`).

Typical request structure:
```json
{
  "auth": { "user": "...", "hash": "..." },
  "parameters": { ... },
  "captchaId": "..."
}
```

Common operations include creating threads/replies, solving captchas, account actions, moderation, etc. Responses follow a pattern like:
```json
{
  "status": "ok" | "error" | ...,
  "data": ...
}
```

### Notes
- Endchan supports multiple mirrors/domains (endchan.net, endchan.org, Tor/Lokinet variants, etc.). Use the one that is currently reachable.
- There is no single polished “official Endchan API docs” page — you rely on the LynxChan engine documentation + testing the live endpoints.
- Libraries such as the `imageboard` package already implement support for Endchan and can serve as practical reference implementations.

For the most accurate current field lists, fetch live examples (e.g. `/index.json` or a catalog) and cross-reference with the LynxChan `doc/Json.txt` documentation.
