# ReplayLead — Devpost Draft

Status: working submission copy. Public links are verified; store availability and real revenue remain conditional until direct evidence exists.

## Tagline

Practice the hard conversation, rewind the moment, and lead it better.

## One-line description

ReplayLead is a mobile rehearsal coach that lets new managers practice difficult conversations, retry a pivotal response, and leave with specific language they can use in real life.

## Inspiration

New managers are promoted for doing strong individual work and then immediately asked to give difficult feedback, set boundaries, and say no fairly. Reading advice is not the same as saying the words under pressure. ReplayLead creates a private place to practice before the real conversation becomes the first rehearsal.

## What it does

The user chooses a realistic management scenario and rates how prepared they feel. ReplayLead plays the counterpart and responds to what the manager actually says. At any manager message, Rewind removes the later conversation and starts a genuine alternate branch. After practice, ReplayLead scores clarity, empathy, assertiveness, and actionability, identifies one strong moment, gives one focused improvement, and suggests a stronger response. The final confidence check makes progress visible.

ReplayLead Pro is designed as a recurring practice habit: unlimited rehearsals, deeper branch comparison, history, and confidence trends. RevenueCat supplies the live Offering, localized price, purchase flow, entitlement state, and user-triggered restore. The app never invents a price when RevenueCat is not configured.

## How we built it

- Kotlin, Jetpack Compose, and a small state-driven Android architecture.
- RevenueCat Android SDK 10.19.1 for Offering lookup, purchase, entitlement verification, and restore.
- Cloudflare Worker with Workers AI for in-character replies and schema-constrained coaching reports.
- A deterministic on-device fallback so a temporary network failure never destroys a rehearsal.
- Strict server-side scenario allowlisting, transcript limits, structured output validation, and per-IP rate limiting.
- Automated Android unit tests, lint, APK assembly, backend tests, TypeScript typecheck, and GitHub Actions gates.

## RevenueCat implementation

The paywall fetches the current RevenueCat Offering only when opened, displays the StoreProduct's localized price, purchases the selected annual package, and unlocks only when the configured `replaylead_pro` entitlement is active. Restore is always an explicit user action. Release builds fail if the public SDK key is missing or has the Test Store `test_` prefix.

Evidence to add:

- Demo purchase and restore segment: https://www.youtube.com/watch?v=XlhX__dl-Qw
- Device evidence: `artifacts/submission/revenuecat-pro-restored-1080x2400.png`
- [BLOCKED: live store product and public platform key evidence, if Gate B passes]

## Challenges

The biggest product challenge was preventing the demo from becoming a scripted chat mockup. Rewind therefore changes the underlying conversation state, not just the animation. The second challenge was honest monetization: early builds used a preview price, but the final flow only enables purchase after RevenueCat returns a real package and localized price. A third challenge was resilience, solved by labeling the active engine and falling back locally without losing the conversation.

## Accomplishments

- A real alternate-branch interaction that is immediately understandable in a short demo.
- Coaching that maps directly to the Career Coaching criteria: realistic practice, useful feedback, and confidence building.
- A purchase integration that fails safely and blocks Test Store keys from release builds.
- A working offline path plus a production-shaped AI service instead of a demo that depends on perfect connectivity.

## What we learned

Practice feels useful when the feedback is narrow enough to act on immediately. A single concrete improvement and a sentence the manager can actually say are more valuable than a long generic analysis. We also learned that monetization trust starts before checkout: live pricing, explicit restore, and clear fallback states matter as much as the purchase button.

## What's next

Branch comparison, spaced practice plans, scenario authoring for team leads, and private progress history. Live-store and growth work will only be described with real evidence collected during the Shipaton eligibility period.

## Built with

Android, Kotlin, Jetpack Compose, RevenueCat, Cloudflare Workers, Workers AI, TypeScript, Vitest, Gradle, GitHub Actions.

## Category targets

- Career Coaching
- RevenueCat Design Award
- RevenueCat Peace Prize
- Next Gen only if active-student status and a qualifying education email are confirmed
- Grand Prize only if the live-store and real-revenue gates are actually satisfied

## Required links

- Public repository: https://github.com/augustm210/replaylead
- Demo video: https://www.youtube.com/watch?v=XlhX__dl-Qw
- Live API: https://replaylead-api.augustm210.workers.dev
- App/store URL: [BLOCKED or not required for Next Gen]
- Privacy policy: https://github.com/augustm210/replaylead/blob/main/submission/privacy-policy.md
