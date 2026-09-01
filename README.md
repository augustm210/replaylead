# ReplayLead

ReplayLead is a mobile rehearsal coach for new managers. It lets people practice difficult workplace conversations, rewind a key moment, try a different response, and receive actionable feedback before the real conversation happens.

## Why it exists

New managers are often asked to give feedback, set boundaries, say no, and resolve conflict before they have had a safe place to practice. ReplayLead turns those moments into short, repeatable rehearsals.

## Current working slice

- Three realistic management scenarios
- Practice-before confidence check
- Interactive counterpart with deterministic local fallback behavior
- Rewind from any user message into a new branch
- Coaching on clarity, empathy, assertiveness, and actionability
- Practice-after confidence check
- RevenueCat SDK initialization and an honest paywall preview
- RevenueCat live Offering, purchase, entitlement, and user-triggered restore handling
- Cloudflare Workers AI conversation service with a transparent on-device fallback

The app labels which engine produced each rehearsal. A deterministic local engine keeps the demo functional offline; when `conversation.apiUrl` points to the deployed Worker, AI replies and reports are used first and failures fall back locally without losing the session.

## Build

Requirements are installed portably under `.tooling` by the project setup process and are intentionally ignored by Git.

1. Create `local.properties`:

   ```properties
   sdk.dir=E:\\work\\replaylead\\.tooling\\android-sdk
   revenuecat.apiKey=
   revenuecat.entitlementId=replaylead_pro
   conversation.apiUrl=
   ```

2. Build and test:

   ```powershell
   .\scripts\build_local.ps1
   ```

3. Verify the backend:

   ```powershell
   cd backend
   npm ci
   npm test
   npm run typecheck
   ```

Never put secret RevenueCat keys or model-provider API keys in this Android project. Only a platform-specific public RevenueCat SDK key belongs in the app. Model credentials must stay on the server. Release builds fail fast when the RevenueCat key is missing or uses the Test Store `test_` prefix.

## Architecture

- Kotlin and Jetpack Compose
- Small state-driven UI with a testable domain engine
- RevenueCat Android SDK (public client key only)
- Cloudflare Worker with Workers AI, schema-constrained responses, validation, and per-IP rate limiting

## Competition status

ReplayLead is being built for RevenueCat Shipaton 2026. The current strategy and evidence log live in `ops/`.

- Demo: https://www.youtube.com/watch?v=XlhX__dl-Qw
- Live API health check: https://replaylead-api.augustm210.workers.dev/health
- Privacy policy: https://github.com/augustm210/replaylead/blob/main/submission/privacy-policy.md

The RevenueCat transaction shown in the demo uses RevenueCat Test Store and does not represent real revenue. Google Play publication remains conditional on Google completing the developer-account identity review.

## License

MIT
