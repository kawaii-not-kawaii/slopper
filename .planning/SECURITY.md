# Security Audit — v1.1 AGP-9 Toolchain Modernization

**Date:** 2026-05-31
**Scope:** All source modules (app, core/*, feature/*)
**Verdict:** SECURED — no open threats

## Threat Register

| ID | Threat | Severity | Status | Mitigation |
|----|--------|----------|--------|------------|
| T-01 | Hardcoded secrets in source | HIGH | CLOSED | No hardcoded API keys, passwords, or tokens found. API keys stored in EncryptedSharedPreferences (AES256_GCM + AndroidKeyStore). |
| T-02 | SQL injection | HIGH | N/A | No raw SQL queries. Apollo GraphQL handles parameterization. |
| T-03 | XSS in WebView | MEDIUM | N/A | No WebView usage in the app. |
| T-04 | Insecure network traffic | MEDIUM | CLOSED | ConnectionStore validates URLs. HTTPS expected for server connections. |
| T-05 | Debug logging in production | LOW | CLOSED | No Log., println, or System.out statements in production code. |
| T-06 | Missing input validation | MEDIUM | CLOSED | Connection screen validates URL format. API inputs go through Apollo's typed queries. |
| T-07 | Insecure crypto | HIGH | CLOSED | EncryptedSharedPreferences uses AES256_GCM (NIST-approved). MasterKey backed by AndroidKeyStore. |
| T-08 | Dependency vulnerabilities | MEDIUM | ACCEPTED | OWASP dependency-check deferred (NVD_API_KEY required). All deps are stable releases. Dependabot configured. |
| T-09 | CI secrets exposure | HIGH | CLOSED | CI workflows use GitHub/Forgejo secrets for signing. No secrets in workflow files. |
| T-10 | Supply chain integrity | MEDIUM | CLOSED | Gradle wrapper validation enabled. Actions pinned by SHA. CodeQL SAST configured. |

## Security Controls

1. **EncryptedSharedPreferences** — API keys stored with AES256_GCM encryption backed by AndroidKeyStore
2. **No hardcoded secrets** — All credentials read from environment variables or encrypted storage
3. **Input validation** — URL format validation on connection screen
4. **Secure CI** — Actions pinned by SHA, wrapper validation, no secrets in workflow files
5. **Static analysis** — detekt + ktlint + CodeQL SAST in CI pipeline
6. **Supply chain** — Gradle wrapper validation, OpenSSF Scorecard

## Accepted Risks

1. **OWASP dependency-check** — Requires NVD_API_KEY for local runs. Mitigated by Dependabot + CodeQL SAST.
2. **Alpha detekt 2.0.0-alpha.3** — Build-time-only tool, never ships in APK. Stable detekt incompatible with Kotlin 2.3.x.
3. **Alpha baselineprofile 1.5.0-alpha06** — Build-time-only tool, never ships in APK. Stable version incompatible with AGP 9.
