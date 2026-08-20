### 1. 4chan API
- **Official read-only JSON API** exists (launched 2012).
- **Documentation**: https://github.com/4chan/4chan-API
- **Base domain**: `https://a.4cdn.org`
- **Main endpoints**:
  - `/boards.json` – list of all boards
  - `/{board}/catalog.json` – full catalog
  - `/{board}/{page}.json` – index page
  - `/{board}/threads.json` – thread list summary
  - `/{board}/thread/{op}.json` – full thread
  - `/{board}/archive.json` – archived threads
- **Rules**: ≤ 1 request/sec, thread polling ≥ 10s, prefer `If-Modified-Since`.
- **Data format**: Pure JSON. Comments are HTML-escaped.

### 2. Getting Images from 4chan Posts
From a post object, construct the image URL using:
- `tim` (timestamp-based filename)
- `ext` (file extension)

**Full image**:  
`https://i.4cdn.org/{board}/{tim}{ext}`

**Thumbnail**:  
`https://i.4cdn.org/{board}/{tim}s.jpg`

Example from provided post (`tim: 1786665896682816`, `ext: .jpg`):  
`https://i.4cdn.org/{board}/1786665896682816.jpg`

### 3. Endchan API
- Endchan runs on a **LynxChan** fork (InfinityNow).
- **Has JSON endpoints** (static files + action API).
- **Key read endpoints**:
  - `/index.json` – site overview
  - `/boards.js?json=1`
  - `/{board}/catalog.json`
  - `/{board}/{page}.json`
  - `/{board}/res/{threadId}.json`
- **Documentation sources**:
  - LynxChan engine docs: https://gitgud.io/LynxChan/LynxChan (`doc/` folder)
  - Community summary: https://gitlab.com/catamphetamine/imageboard/-/blob/master/docs/engines/lynxchan.md
- Action/write API uses JSON RPC-style requests under `/.api/`.
