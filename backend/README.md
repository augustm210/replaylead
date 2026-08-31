# ReplayLead API

This Cloudflare Worker provides two narrow endpoints for the Android app:

- `POST /v1/reply` returns the in-character counterpart's next response.
- `POST /v1/coach` returns a structured coaching report.
- `GET /health` is a no-AI health check.

The Worker uses a Workers AI binding, validates scenario IDs and transcript size, requests schema-constrained JSON, and applies a per-IP rate limit before invoking the model. The mobile app must retain its local fallback so a temporary service failure never blocks a rehearsal.

## Local verification

```powershell
npm install
npm test
npm run typecheck
```

## Deployment

Authenticate Wrangler, review the rate-limit namespace for the target Cloudflare account, then run `npm run deploy`. Put the resulting HTTPS origin in `conversation.apiUrl` in the Android project's `local.properties`.
