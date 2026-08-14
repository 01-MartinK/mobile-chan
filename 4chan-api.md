### Official Documentation
The complete and authoritative documentation is maintained here:

**[https://github.com/4chan/4chan-API](https://github.com/4chan/4chan-API)**

It includes detailed pages for every endpoint, field descriptions, examples, rules, and terms of service.

### Base URL
All API data is served from:
- **`https://a.4cdn.org`** (JSON endpoints)
- Images/media: `https://i.4cdn.org`
- Static assets: `https://s.4cdn.org`

### Main Endpoints

| Endpoint | Description | Example |
|----------|-------------|---------|
| `/boards.json` | List of all boards + settings | `https://a.4cdn.org/boards.json` |
| `/{board}/catalog.json` | Full catalog (all threads + previews) | `https://a.4cdn.org/g/catalog.json` |
| `/{board}/{page}.json` | Threads on a specific index page | `https://a.4cdn.org/g/1.json` |
| `/{board}/threads.json` | Summarized list of all live threads | `https://a.4cdn.org/g/threads.json` |
| `/{board}/thread/{op}.json` | Full thread (OP + all replies) | `https://a.4cdn.org/g/thread/12345678.json` |
| `/{board}/archive.json` | List of archived thread IDs | `https://a.4cdn.org/g/archive.json` |

### Key Rules
- Maximum **1 request per second**
- Thread polling: at least **10 seconds** between updates
- Prefer the `If-Modified-Since` header
- Read-only only (no posting)

### Data Format
Everything is plain **JSON**.  
Comments are HTML-escaped.  
Image URLs are built from the `tim` + `ext` fields in the post objects.

For full field lists and examples, see the individual markdown files in the GitHub repo (especially `pages/Threads.md`, `pages/Catalog.md`, and `pages/Boards.md`).
