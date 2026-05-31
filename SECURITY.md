# Security Policy

> ⚠️ Slopper is **alpha software** under active development. It is not
> production-ready and ships pre-release build tooling. Treat any deployment as
> experimental.

## Supported versions

Only the latest commit on the `master` branch is supported. There are no
tagged releases yet; fixes land on `master`.

| Version | Supported |
|---------|-----------|
| `master` (latest) | ✅ |
| anything older | ❌ |

## Reporting a vulnerability

**Please do not open a public issue for security vulnerabilities.**

Report privately via GitHub's **Private Vulnerability Reporting**:

1. Go to the [**Security** tab](https://github.com/kawaii-not-kawaii/slopper/security) of this repository.
2. Click **Report a vulnerability** and fill in the advisory form.

This opens a private channel between you and the maintainer. Please include:

- affected component / file(s) and the version (commit SHA) you tested,
- a description of the issue and its impact,
- reproduction steps or a proof of concept, if available.

### What to expect

- Acknowledgement on a best-effort basis (this is a solo-maintained hobby project).
- A fix on `master` once validated; credit in the advisory if you'd like it.

## Scope notes

- **Your Stash server credentials / API key** are stored on-device in
  encrypted SharedPreferences and never committed to this repository — please
  do not include real credentials in any report.
- Build-tooling pre-releases (AGP 9, Kotlin 2.3.20/KSP2, detekt 2.0-alpha,
  baseline-profile 1.5.0-alpha) are **build-time only** and never ship in the
  APK; report runtime/app-level issues here, and upstream-tool issues upstream.
