# Budgeting App — Brainstorm Spec

A YNAB-style budgeting app. Free for users. Flutter (iOS + Android), with a web version when a server is involved. AI-assisted. Aimed at YNAB feature-parity without using YNAB's name, logo, or signature phrases.

> **Research-informed adjustments (May 2026):** This spec has been updated based on three rounds of KEEN user research saved in the project:
> - `KEEN-U_1.HTM` — general budgeting sentiment (r/ynab, r/actualbudgeting, r/personalfinance)
> - `KEEN-W_1.HTM` — web UI sentiment on YNAB and Actual Budget
> - `KEEN-M_1.HTM` — multi-currency sentiment from expats, digital nomads, and cross-border workers
>
> Headline findings: free pricing, mobile-native, and zero-based methodology are all validated; YNAB's mobile redesign created the biggest competitive opening; AI trust is fragile; web UI should be re-positioned as a deep-dive surface (mobile primary, web companion — the inverse of YNAB/Actual); multi-currency is a strong wedge feature, not a niche, and Wise is the hub almost every cross-border user already uses.

---

## 1. Methodology (locked)

The engine the whole app runs on.

**Core idea: zero-based budgeting.** Every dollar that hits an account gets assigned a job (rent, groceries, savings, fun) until nothing is left unassigned. Only budget money the user *actually has* — never income expected later.

**Supporting habits:**
- Save a small amount monthly for irregular expenses (car repairs, holidays, annual subscriptions).
- When a category is overspent, move money from another category to cover it. Adjust, don't ignore.
- Aim to live on *last* month's income, not this one (builds a one-month buffer).
- Treat the budget as a living plan — review and shift as life happens.

---

## 2. Core Tier Features (locked)

### 2.1 Accounts
- **Budget accounts** — actively assigned to. Checking, savings, cash, credit cards.
- **Tracking accounts** — watched but not budgeted. Investments, retirement, house value, car value.
- User enters real-world balance at setup; transactions keep it in sync thereafter.
- Credit cards get special handling (see Section 6).

### 2.2 Categories
- Organized into **groups** (e.g., "Monthly Bills").
- Each category shows three numbers: **Assigned**, **Activity**, **Available**.
- "Available" is the everyday "can I afford this?" number.
- Unspent money **rolls over** month-to-month. It stays in the category until the user moves it.

### 2.3 Transactions
**Fields:** date, payee, category, amount, account, optional memo.

**Behaviors:**
- **Inflows** land in "Ready to Assign" by default.
- **Outflows** pull from the Available in their category.
- **Splits** — one transaction across multiple categories.
- **Transfers** — moving money between user's own accounts; don't affect the budget.
- **Cleared status** — flag for "bank has processed this."

**Lifecycle:** pending → cleared → reconciled. Reconciled transactions are locked from accidental edits.

**Quality-of-life:**
- Six colored flags with user-defined meanings.
- Search & filter by payee, category, account, date, amount, flag, cleared status.
- Refunds are positive transactions tagged to the original category — restores Available.
- Edit-anywhere math: edits to old transactions ripple through history.
- Auto-import deduplication when bank import matches a manual entry.
- Tracking-account transactions don't move budget money.

**Payee categorization — YNAB-plus (locked):** per-payee default category, plus smart memory of the last few categories used for that payee.

### 2.4 Ready to Assign
- A single number: (cash in budget accounts) − (assigned to categories).
- Inflows add to it. Assigning drains it. Target = $0.
- Goes red when negative; nags user to fix by un-assigning or pulling from a category.
- The core daily ritual: income arrives → assign → repeat.

---

## 3. Next Layer Features (locked)

### 3.1 Targets
- **Cadences:** refill weekly, refill monthly, refill yearly, save by a custom date, custom cadence.
- Progress bars with color signals (green/yellow/red).
- Tell the user how much to assign this month to stay on track.
- Show an **"underfunded"** total across all categories.
- Can be **snoozed** for the month when life happens.
- Targets are *guidance*, not automation — they suggest, the user assigns.

### 3.2 Scheduled Transactions
- Frequencies: every X days, weekly, biweekly, monthly, every X months, yearly, custom.
- Appear as **upcoming** in the register before their date.
- Auto-enter on the scheduled date as an uncleared transaction.
- A single occurrence can be skipped without canceling the schedule.
- Edits apply to future occurrences only; past ones stay put.
- Effectively give the budget a 30-day forecast.

### 3.3 Moving Money Between Categories
- Mechanic: source → destination → amount. The Available shifts; no transaction is created.
- Used when overspent, priorities shift, a category is over-funded, or to un-assign.
- When a category goes red, the app prompts: *"Cover this from where?"*

### 3.4 Reconciliation
- Workflow: app asks *"Is the balance $X right now?"*
- If yes → cleared transactions get locked (reconciled).
- If no → user enters the real bank balance, a **Reconciliation Balance Adjustment** transaction is created.
- Keeps the in-app balance and the real bank balance from drifting apart.

---

## 4. Advanced Tier Features (locked)

### 4.1 Reports & Insights
A dedicated tab with five core views (name to be decided — not "Reflect"):
- **Net Worth** — assets minus liabilities, over time.
- **Spending Trends** — bar chart by category, month over month.
- **Spending Breakdown** — percentage view of where money went.
- **Income vs. Expense** — total in vs. out for any date range.
- **Age of Money** — average days money sits before being spent (kept, but de-emphasized).
- **Strong AI integration target.**

### 4.2 Bank Sync
**Default for all users (free, every country):**
- Manual transaction entry
- CSV / OFX file import from the user's bank

**Optional auto-sync (server required):**
- **SimpleFIN Bridge** — user pays $15/yr to SimpleFIN directly; US/Canada banks
- **Plaid (BYOK)** — user creates their own Plaid developer account, plugs in their keys
- **Stripe Financial Connections (BYOK)** — same pattern; lower priority

**Future:** GoCardless Bank Account Data for EU/UK (free for low volume).

**Three user tiers:**
- **Casual** — no server, manual + CSV, free forever
- **Sync user** — self-hosts the backend (or pays a third party like Pikapods/Coolify to manage it) + SimpleFIN
- **Power user** — self-hosts with deeper customization + BYOK any aggregator

### 4.3 Multi-Device Sync
Three modes mapping to the three user tiers:
- **Casual (no server):** backup/restore via the user's own iCloud or Google Drive. Eventual, not real-time.
- **Sync user (self-hosted server):** the user's server is source of truth. Real-time sync across devices. They can run it themselves or pay Pikapods/Coolify to manage it for them.
- **Power user (self-hosted, customized):** same as sync user, with deeper tweaks.

**Encryption (locked):**
- **Default = Option B** — at-rest encryption with your key. Server features (including AI) work normally. Comparable security to YNAB.
- **Opt-in = Option C** — end-to-end encryption with the user's key. Only the user's devices can decrypt. AI limited to on-device. Marketing differentiator vs. YNAB.

### 4.4 Partner / Family Sharing
- Invite via email or share link.
- Invited members log in with their own account but see the same budget.
- Real-time sync between members.
- **Permissions:** full access for all members in v1 (matching YNAB).
- **Audit trail:** who entered/edited each transaction, when.
- **Change notes (locked):** owner can toggle mandatory mode.
  - Required for: add/edit transactions, move money, edit assignments, reconciliation balance adjustments.
  - Not required for: creating/deleting categories, approving auto-imported transactions.
  - Solo users: notes are optional.
  - UX safeguard: 5-character minimum to discourage "asdf" gaming.
  - Notes are visible inline on the transaction and in the audit log.
- **Cap:** 6 members per budget (self-hosted: unlimited).
- **E2E sharing:** encryption key exchanged via QR code or one-time invite link.

### 4.5 Multi-Currency Support (locked, ships v2.0)

Validated by `KEEN-M_1.HTM` research. Strong demand across r/expats (394-upvote top post is a multi-currency banking rant), r/digitalnomad, and r/personalfinance. YNAB has no native support — users rely on a fragile third-party plugin (rmillan.com). Multi-currency is a **wedge feature, not a niche** — audience spans expats, digital nomads, freelancers, remote workers, international students, and cross-border families.

**Marketing positioning:** *"The budgeting app for people who don't live in one currency."*

**Schema work (starts v0.5, so we don't paint ourselves into a single-currency corner):**
- `currency` field on every **account** — required, defaults to budget's home currency
- `native_amount` + `native_currency` fields on every **transaction** — nullable, populated when the user pre-enters in another currency or when the bank feed carries the original currency in metadata
- FX-rate snapshot stored per transaction (historical, never retroactively changed)

**P0 — table stakes (v2.0):**
- Pick a **home currency** for the budget. Categories, Ready to Assign, and reports all display in home currency.
- Log expenses in **any currency** — auto-convert at the rate on the transaction's date.
- **Lock historical FX rates** on transactions. Today's rate moving doesn't rewrite history.
- **FX-aware account types** — separate accounts per currency, transfers between them handled as currency exchanges with the actual rate used (no double-counting in net worth).
- **Bank-truth principle (locked from prior brainstorm):** when a bank-synced transaction shows the home-currency amount, that is the source of truth for the budget math. Native amount is metadata. If the user didn't pre-enter the native-currency version, the transaction just records in the bank's currency — no inference.
- **Frankfurter API** (European Central Bank rates, free, no key, historical data back to 1999) as the default rate provider. Daily refresh cached locally.

**P1 — what makes KEEN the clear winner (v2.0–v2.5):**
- **Wise integration** — sync balances, conversions, and transactions from the multi-currency hub almost every cross-border user already uses
- **"Trip" / "location" tags** — view expenses for "Bali Sep 2025" or "moved to Portugal" alongside the normal monthly view
- **Receipt scanning** that handles foreign currency symbols (extends Section 7.5 receipt scanning to detect ¥/€/£/etc.)
- **Income smoothing** — visualize variable freelance income across currencies as a smoothed home-currency monthly equivalent
- **Offline-first mobile** — works on a plane or in a country with bad data (this is already a Keen design principle; flagging it here for the nomad audience)

**P2 — opportunistic differentiators (post-v2.5):**
- Map-centric expense view (where am I spending most this trip?)
- Tax-residency-aware reports (Jan–Dec, Apr–Mar, fiscal-year toggle)
- "Sending money home" category type with Wise transfer reconciliation
- Multi-tax reporting view (same expenses categorized differently for two countries' tax rules)

**Encryption considerations:** multi-currency works with both Option B (default at-rest) and Option C (E2E). Frankfurter rate fetches happen on-device for Option C users.

---

## 5. Credit Cards (locked)

### 5.1 YNAB-Style Payment-Category Mechanic
- Each card gets a special **Payment category** auto-created with it.
- On spend: $50 outflow on the card categorized as "Groceries" → app auto-moves $50 from Groceries → Card Payment. Cash is reserved.
- On payment: transfer from checking → card. Payment category Available drops by the payment amount.
- **Card overspending:** prompts *"Cover this from where?"* Payment goes red until resolved.
- **Interest charges:** categorized to "Interest & Fees"; auto-moves into Payment.
- **Refunds:** reverse flow — Payment shrinks, original spending category's Available is restored.
- **Pre-existing debt:** at card setup, app asks *"How much of this are you ready to budget for paying off now?"* That amount goes to Payment; the rest is debt to plan for later.

### 5.2 Beyond YNAB
- 🟢 **APR field** — powers interest projections, debt payoff timelines, "$X paid in interest this year" awareness nudges.
- 🟢 **Late fee field** — used in payment reminders.
- 🟢 **Credit limit + utilization tracking** — alerts at 30% / 50% / 80%. Real differentiator (affects credit scores).
- 🟡 **Rewards tracking & "best card for X" (low priority, v1.5+):**
  - Manual entry as the base.
  - AI extracts category-percentage data from photographed or pasted cardholder terms.
  - Quarterly refresh + activation reminders for rotating-category cards (Chase Freedom Flex, Discover IT).
  - Community-curated list for top ~50 popular US cards.
  - Manual override always available.

---

## 6. Future Considerations (locked, but later)

### 6.1 Crowdsourced Payee → Category Database
Public, open dataset of payee-name → category mappings, contributed by opted-in users. Becomes more accurate with scale. A real technical moat — no equivalent exists in the open today.

**Contribution flow:**
- Opt-in per user. App asks once: *"Mind sharing this payee → category with the public database? We strip everything but the payee name and category."*
- What's contributed: normalized payee name + chosen category. Nothing else.
- What's stripped: amount, date, transaction ID, location, account, user ID, memo, currency, anything bank-provided.

**Privacy pipeline (defense in depth):**
1. **On-device structural strip** — every user, every tier. Known fields removed by code before anything leaves the device.
2. **On-device PII model pass** — `openai/privacy-filter` via Transformers.js. Catches PII that slipped past step 1 (e.g., a person name accidentally typed into a payee field). Apache 2.0, ~1.5B params, runs in browser.
3. **Server-side double-check** — full-size privacy filter runs again before the cleaned payload is added to the public DB.
4. **Aggregation gate** — payee only appears publicly after 5+ distinct users contribute it.

**Integrity safeguards:**
- Anonymous contributions (ephemeral tokens, no user identity attached).
- Voting weight — veteran users count more than new accounts. Resists spam.
- AI moderation classifier filters adversarial garbage (e.g., "Walmart" tagged as "Vacation").
- Suggestion UI: *"Community suggests: Groceries (87% of 412 votes)."* User chooses or overrides.

**Storage:** public-domain dataset hosted as an open repo so anyone can fork or audit. App syncs updates daily or weekly.

**Priority:** v1.5 / v2 — after the app has real users to contribute. Pairs with the AI layer.

### 6.2 In-App Community (revisit later)
Most likely an external Discord or forum link in v1 — validate that users actually want to socialize about budgeting before building it in-app.

Eventual options to revisit:
- **Built-in forum with pseudonymous handles** — full control, easier moderation.
- **Matrix** — federated, end-to-end encrypted, has real moderation tools.
- **Nostr** — strongest privacy story, but onboarding friction and structurally weak moderation make it a tough fit for a financial-app community in 2026. Possibly a future opt-in layer for the privacy-purist segment.

**Priority:** future / low. Validate demand first via external link.

---

## 7. AI Integration (locked)

### 7.1 Architecture: Hybrid + BYOK
- **On-device** handles simple, private, free-to-the-user tasks.
- **Cloud (BYOK)** handles complex tasks — user provides their own OpenAI-compatible API key and (optionally) a custom URL. App stays free; user pays for their own AI calls.

**Supported cloud endpoints via OpenAI-compatible + custom URL:**
- Cloud providers: OpenAI, Anthropic (via LiteLLM), Gemini, OpenRouter (~100 models behind one key), DeepInfra, Together, Fireworks
- Self-hosted: Ollama, LM Studio, vLLM, llama.cpp
- Anything else that speaks OpenAI-compatible API

### 7.2 On-Device Models
Multi-model strategy — load the right one for the task; memory cost is only the active model.

- **LFM2.5-350M** (Liquid AI) — categorization, structured outputs, tool use. <500MB quantized. ~188 tok/s on Snapdragon Gen4.
- **Gemma 4 E2B** (Google) — multimodal (text + image + native audio). Used for receipt scanning and voice input. ~2GB memory.
- **Qwen 3.5 0.8B** (Alibaba) — general-purpose fallback, strong multilingual coverage.

### 7.3 E2E Compatibility
- Option B users (default at-rest encryption): full cloud + on-device AI works.
- Option C users (E2E with user key): cloud AI requires explicit per-request consent. On-device AI works freely.

### 7.4 AI Categorization Rules (locked)

**Guiding principle — "AI suggests, the human commits."** Per the KEEN user research, trust in AI budgeting tools is fragile. Rocket Money's autonomous bill-negotiation has drawn 865-upvote scam complaints; Honey's collapse is still recent memory. Keen's AI never *acts* — it only *proposes*. Every AI suggestion is reviewable, reversible, and tied to an explicit user tap. The following rules are not negotiable:

- **AI suggests, never auto-assigns.** Every new payee → category mapping requires explicit user confirmation. No silent automation for the first occurrence.
- **After 3 confirmations** of the same payee → category, switch to silent auto-fill with a quick undo. (Mirrors YNAB-plus payee memory.)
- **Per-user rejection memory.** If a user rejects an AI suggestion, it's not suggested again for that user. Separate from the community DB, though enough community rejections downvote the public mapping.
- **Top 3 suggestions, not top 1.** User picks from a short list rather than rejecting + searching.
- **"No fit" → offer to create a new category** from the transaction. Closes the loop.
- **Bulk import behavior:** suggestions per transaction, with accept-all / accept-some / one-by-one review options.
- **No autonomous actions, ever.** Keen will never cancel a subscription, negotiate a bill, change an account setting, or move money without an explicit user tap. (This is the Rocket Money anti-pattern.)

### 7.5 AI Feature Tiers

**Core (essential for v1):**
- Smart category suggestions (community DB + user history + payee context)
- Anomaly & duplicate detection (flag unusual charges, double-imported transactions)
- Smart scheduled-transaction detection (*"You've paid Netflix on the 14th for 3 months — make this a scheduled transaction?"*)

**Value-add (v1 or v1.5):**
- Natural-language queries (*"How much did I spend on coffee last quarter?"*)
- Insights & narrative summaries (AI explains *what changed* in plain English — the killer feature)
- Spending forecasts (*"At current pace, dining will overspend by $80 this month"*)
- **Non-essential spending audit** (*"What discretionary items am I spending the most on?"*) — AI ranks discretionary spending; discretionary flag user-set or AI-inferred per category
- **Subscription redundancy detection** (*"What overlapping subscriptions could I cancel?"*) — groups recurring charges by service type (streaming, fitness, cloud storage), flags overlaps

**Advanced (v1.5+):**
- Receipt scanning — OCR + AI extracts items, prices, splits across categories
- Voice input — *"I spent $40 at Trader Joe's on groceries today"* → transaction created
- Reward category extraction — AI parses cardholder agreements (also handles rotating-category cards; see Section 5.2)
- New-user budget construction — AI suggests categories from imported transaction history
- Goal coaching — *"You're $200 behind on vacation. Here are 3 categories to pull from."*

**Power user (post-v1):**
- **MCP server** — app exports an MCP endpoint so users can connect Claude Desktop, Cursor, or any MCP-capable AI client to their budget. Scripted analysis and custom workflows.

### 7.6 Tuning Considerations
- **Categorization needs an eval set** — test pairs of "this transaction should map to this category." Without it, regressions are invisible.
- **Anomaly detection precision/recall is tunable** — too sensitive = annoying false alarms; too loose = misses real concerns. Sensitivity should be user-adjustable.
- **Subscription detection corner cases** — annual subs, varying amounts (Netflix tier upgrades), bundles (Apple One). Need explicit handling.

---

## 8. Tech Setup (locked)

### 8.1 Frontend
- **Mobile:** Flutter (iOS + Android, single codebase)
- **Web:** SvelteKit, separate from backend (frontend-only — consumes the same API the mobile app uses)

### 8.2 Backend
- **Language:** Go (compiles to a single static binary; self-hoster-friendly; easy for OSS contributors to read)
  - *Alternative on the table for future reconsideration:* Bun + TypeScript if shared types with SvelteKit feel valuable
- **Database:** SQLite, one DB per user/budget (Actual Budget's pattern; trivial backups; isolated per user)
- **Sync protocol:** CRDT-based message log (each change is an immutable message; merges are automatic and conflict-free; handles offline edits gracefully)
- **Real-time:** WebSocket for live push; polling fallback for self-hosters with simpler setups
- **Auth:** email/password (argon2 hashing) + OAuth (Google, Apple — Apple required by App Store if any third-party auth is offered on iOS). Passkeys as a future enhancement.
- **Push notifications:** FCM (Android) + APNs (iOS) for bill reminders, large transaction alerts, family activity, sync completion

### 8.3 Hosting Model
**Developer operates one public service only: the payee database.**

- **Where:** Cloudflare Workers + D1 (globally distributed, SQLite-on-Cloudflare, generous free tier, easy to deploy)
- **Estimated cost:** $0 in early days; $5–10/month at any reasonable scale
- **Everything else** (sync server, app data) is either app-bundled or self-hosted by the user.

### 8.4 User Tier Model (corrected)
- **Casual user** — installs the app from App Store / Play Store. Manual entry + CSV import. No server. Free forever.
- **Sync user** — self-hosts the backend on their own VPS/homelab, **or pays a third party** like Pikapods or Coolify to manage it. Connects SimpleFIN or BYOK Plaid.
- **Power user** — self-hosts with deeper customizations.

Implication: non-technical users who want auto-sync have to either learn self-hosting or pay a managed-hosting service. The developer never operates a multi-tenant sync service.

### 8.5 Self-Hosting Story
- **Primary packaging: Docker Compose** — single `docker-compose.yml` for Go backend + SQLite. Optional Caddy for HTTPS, SimpleFIN worker, push relay. One-command setup. Works on any VPS, NAS (Synology, Unraid, TrueNAS), or homelab.
- **Secondary packaging: single Go binary** — static compile for Linux/macOS/Windows/ARM. SvelteKit static build embedded inside via Go's `embed` package. Download, run, done.
- **Third-party managed self-hosting:**
  - **Pikapods** (~$2–5/mo per instance) — fully managed; non-technical-user friendly
  - **Coolify** — free, self-hosted PaaS for users with their own VPS
- **Backups:** Litestream config for streaming SQLite to S3/R2/B2 (disaster recovery)
- **Mobile → self-hosted server:** user enters server URL during sign-in; app stores URL + auth token; easy server URL switching for users with multiple instances

### 8.6 License & Funding
- **License: AGPL v3** — permits self-hosting and personal use freely; prevents companies from hosting it commercially without contributing back; same license as Actual Budget.
- **Funding: GitHub Sponsors as the primary channel.** Always optional, never gated. Buy Me a Coffee for one-time donors. Open Collective if it scales. No Patreon.
- **Transparency:** publish the hosting bill so donors see where money goes.
- **Cost levers if it ever gets tight:** raise the payee DB contribution threshold; move to Hetzner VPS; worst case pause the public payee DB temporarily.

### 8.7 Production Essentials
- **Litestream** — streaming SQLite backups
- **Cloudflare R2** — receipt images, backups (cheapest object storage)
- **Sentry** — crash reporting (free tier)
- **Cloudflare CDN** — SvelteKit static asset delivery (free tier)

### 8.8 Web UI Principles (locked, informed by `KEEN-W_1.HTM`)

The web UI research surfaced a clear strategic move: both YNAB and Actual Budget treat web as the *real* budgeting surface and mobile as a convenience layer. Keen **inverts this**. Mobile is the primary surface; web is the deep-dive for what big screens actually do better.

**What the web app is for:**
- Multi-widget customizable dashboards
- Multi-month planning and target adjustment
- Bulk transaction edits and categorization
- Year-over-year and seasonal analysis
- Report-building with chart drill-through
- Power-user workflows: keyboard shortcuts, dense-mode view, saved filter presets

**What the web app is NOT for:**
- Being a precondition for using Keen at all (mobile users must be fully served without ever opening web)
- Hosting features mobile can't do (mobile-first design is enforced for every new feature)

**Performance budget (existential — YNAB is failing this).** The web research showed YNAB web app load times of 20+ seconds, with extreme cases at 40 minutes, for long-time users. Some are wiping their entire history just to make the app usable. Keen's web app must:
- Load to interactive in under 1 second, even with 10+ years of history
- Use server-side pagination and virtual scrolling on long registers
- Index all common queries (date ranges, category filters, payee searches) at the database level
- Run performance benchmarks against synthetic 10-year datasets in CI from day one

**Anti-patterns from the research (do not ship):**
- Renaming core concepts ("Budget" → "Plan") that breaks muscle memory
- UI churn that destroys learned workflows between versions
- Customization that requires editing raw JSON (Actual Budget's biggest weakness — power features locked behind code)
- Charts that can't be clicked through to the underlying transactions
- Single-level categories with no nesting

**Feature shortlist (folded into the v1.5 / v2.0 / v2.5 roadmap entries):**

*P0 — table stakes:* sub-second load with 10+ years history · "find the money first" interaction · running balance on every register · multi-month forward view with target tracking · saved filter presets across registers and reports · click-through drill-down from any chart into transactions.

*P1 — what the community is begging for:* customizable dashboard widget grid with **WYSIWYG editor** (no JSON) · nested categories on web · year-over-year report as a built-in widget type · progress-bar widget for categories · keyboard shortcuts and dense-mode view (Toolkit-style) · receipt storage for HSA / tax write-offs.

*P2 — opportunistic differentiators:* Sankey cash-flow chart (top YNAB feature request) · dashboard sharing as **named templates** (not raw JSON) · sub-month interval views (weekly/biweekly target planning) · side-by-side this-month-vs-same-month-last-year comparison.

A per-version `keen-web-ui-spec.md` (mirror of the mobile spec) will be created when SvelteKit work begins in v1.5.

---

## 9. Name & Branding (locked)

### 9.1 Name
- **Brand name:** **Keen**
- **Industry-context name:** **Keen Budgeting** — used on the App Store listing, GitHub README, marketing pages where clarity about category matters
- **Tagline / acronym expansion:** *Keep Every Earned Nickel* — the YNAB-style "meaning behind the name" that doubles as a memorable description of zero-based budgeting

### 9.2 Why It Works
- Real word, easy to pronounce, easy to type
- "Keen" means sharp, alert, focused — exact mindset budgeting requires
- *Keep Every Earned Nickel* is a fresh, accurate description of zero-based budgeting that avoids any YNAB trademark ("Give Every Dollar a Job," "Age Your Money")
- Carries the YNAB-style "acronym as word" charm without copying
- Adding "Budgeting" as a descriptor narrows trademark scope to the right industry

### 9.3 Known Adjacent Brands (verified non-blocking)
- **KEEN Footwear** — major brand, but USPTO Class 25 (clothing/footwear). Different from the needed Class 9 (software) + Class 36 (financial services).
- **Keen.com** — psychic/advice service. Different niche.
- **Keen.io** — defunct/acquired analytics platform.
- **keenpocket.com** — a budget-app *review blog*, not a competing app.
- **No competing budgeting app named "Keen" or "Keen Budgeting"** in App Store, Google Play, or the open-source ecosystem at time of research.

### 9.4 Verification Still To Run Before Public Launch
- USPTO TESS search for "KEEN" in Classes 9, 36, and 42 (software / financial services / SaaS)
- Domain availability: `keen.budget`, `getkeen.app`, `keenbudgeting.com`, `keen-budgeting.com`
- App Store / Play Store search for "Keen" + "budget"
- GitHub org availability: `github.com/keenbudgeting` or similar
- Social handles: `@keenbudgeting` on X, Bluesky, Mastodon, Instagram, Reddit

---

## 10. Roadmap (locked)

Two guiding principles:
1. Each milestone must be usable on its own — no "wait for v3 to do basic budgeting."
2. Complete the **Casual user tier** (no server) before adding the sync tier — protects the "free, no friction" promise.

### v0.1 — Internal alpha (foundations)
- Go backend skeleton, SQLite schema, auth flows
- Flutter mobile skeleton + sign-in
- Accounts, categories, transactions, Ready to Assign math
- Goal: smoke-test the architecture; not user-ready

### v0.5 — Public beta (Casual user tier MVP)
- Everything from v0.1, polished
- Splits, transfers, cleared status, reconciliation
- CSV / OFX import
- **YNAB import** *(new — YNAB exodus is happening now per research; don't make switchers lose data)*
- Targets (monthly cadence first)
- Moving money between categories
- Basic search & filter
- **Category search bar on mobile pickers** *(new — long-standing YNAB mobile gap)*
- **Inline calculator in every amount field, including split rows** *(new — fixes "bonkers" YNAB mobile gap)*
- **Bulk categorize swipe queue** *(new — Monarch-style flow; the #1 reason switchers consider competitors)*
- **Customizable launch screen** *(new — directly addresses the YNAB "Home tab" backlash)*
- **Multi-currency schema groundwork** *(new — adds `currency` field to accounts and `native_amount`/`native_currency` fields to transactions; UI remains single-currency until v2.0, but no painful migration later)*
- Reports: Spending Breakdown + Income vs Expense
- Mobile UX polished
- Web: Flutter Web temporarily (defer SvelteKit polish)

### v1.0 — Minimum Public Release 🚀 *(methodology demo)*
- Everything from v0.5, polished
- Scheduled transactions
- Full target cadences (weekly, yearly, save-by-date, custom)
- **Sub-categories / nested targets** *(new — 199-upvote YNAB feature request)*
- **"Month ahead" funding indicator** *(new — research calls this the killer retention feature for switchers)*
- All reports (Net Worth, Spending Trends, Age of Money, etc.)
- Credit cards: basic balance tracking (full payment-category mechanic deferred to v1.5)
- Payee categorization v1: per-payee defaults (no AI, no community DB yet)
- Backup/restore via iCloud / Google Drive
- **Public launch: App Store + Play Store**
- *Strategic positioning: ship the methodology fast, get real users, learn what they want before scaling.*

### v1.5 — Sync tier + on-device AI
- Self-hosted Go backend (Docker Compose + single binary)
- CRDT sync over WebSocket
- SimpleFIN integration, BYOK Plaid
- SvelteKit web app (replaces Flutter Web) — see Section 8.8 for principles
  - **P0 web features:** sub-second load with 10+ years of history (performance budget enforced) · running balance on every register · multi-month forward view · saved filter presets · click-through drill-down from any chart
  - **Web-specific:** keyboard shortcuts and dense-mode view (Toolkit-style)
- Litestream backups
- On-device AI categorization (LFM2.5-350M) + 3-confirms rule + top-3 suggestions
- Full credit-card mechanic (auto-move spending → Payment category)
- Credit card fields: APR, late fee, credit limit, utilization alerts
- Encryption Option B (default at-rest with developer's key)

### v2.0 — Family, cloud AI, multi-currency, customizable dashboards
- Partner/family sharing, audit trail, mandatory-change-notes mode
- BYOK cloud AI (OpenAI-compatible + custom URL)
- AI insights, narrative summaries, forecasts, anomaly detection
- Natural-language queries
- Subscription redundancy detection, non-essential spending audit
- Encryption Option C (E2E) opt-in
- **Multi-currency launch** *(new — Section 4.5 P0 features)*: home-currency budgets · expenses in any currency with Frankfurter rate auto-conversion · locked historical FX · FX-aware account types · proper foreign-currency transfer handling
- **Customizable dashboard with WYSIWYG widget editor** *(new — fixes Actual's biggest weakness)*: widget grid, multiple chart types, drag-and-drop, **no JSON editing required**
- **Web: P1 features land**: nested categories on web · year-over-year report widget · progress-bar widget · receipt storage for HSA / tax write-offs
- **Marketing pivot:** add the "*budgeting app for people who don't live in one currency*" angle to outreach (r/expats, r/digitalnomad, r/JapanFinance, r/EuropeFIRE)

### v2.5 — Public payee DB + multi-currency P1 + web polish
- Cloudflare Workers + D1 backend
- Privacy filter pipeline (on-device + server-side, OpenAI privacy-filter)
- 5+ user aggregation gate, anonymous contribution flow
- Suggestion UI in app: *"Community suggests: Groceries (87% of 412 votes)"*
- **Multi-currency P1** *(new — Section 4.5)*: Wise integration · "Trip / Location" tags for nomad-friendly expense grouping · income smoothing for variable freelance income · foreign-currency receipt symbols in OCR
- **Web P2 features land**: Sankey cash-flow chart (top YNAB feature request) · dashboard sharing as named templates (not raw JSON) · sub-month interval views (weekly/biweekly) · this-month-vs-same-month-last-year comparison report

### v3.0 — Multimodal AI
- Receipt scanning (Gemma 4 E2B)
- Voice input
- AI-assisted reward category extraction from cardholder agreements
- New-user budget construction assistant
- Goal coaching

### v3.5+ — Future
- Rewards tracking + quarterly activation reminders
- Community-curated top-50 card list
- MCP server export
- In-app community (after Discord/forum link validates demand)
- Privacy-purist option: explore Nostr or Matrix

---

## 11. Brainstorm Complete

All decisions locked. The spec covers methodology, three feature tiers, credit card handling, future considerations, AI integration, tech setup, name & branding, and the release roadmap.

**Recommended immediate next steps:**
1. Run the trademark + domain + handle verifications in Section 9.4
2. Stand up the GitHub repo: `keenbudgeting/keen` (or similar), with AGPL v3 LICENSE and README
3. Start v0.1 scaffolding: Go backend with auth + SQLite schema, Flutter mobile sign-in flow
4. Set up GitHub Sponsors profile alongside the repo (even if early — establishes the funding channel)
