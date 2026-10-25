# Changelog

All notable changes to Slopper are listed here. Versions follow [Semantic Versioning](https://semver.org);
while the major version is 0, expect rough edges.

## [0.3.0] - 2026-10-25

### Added
- Date and time pickers for date fields in filters, so dates no longer have to be typed.
- The player's codec badge now shows the decoder actually in use (hardware or software).

### Changed
- Videos decode on the phone's hardware decoder instead of the CPU, which cuts battery drain and heat.
- Playback pauses when you leave the app. Picture-in-picture keeps playing.
- The progress bar stops updating while paused, saving battery.
- Repeat-one loops in place instead of reloading the scene.
- Updated libraries, including Compose, OkHttp and Paging.

### Fixed
- **Security:** your Stash API key is no longer sent to other servers when a request is redirected.
- Watch time sent to Stash was counted about 5.5× too high.
- The resume position could be lost when closing the player. Finished scenes now start over from the beginning.
- The app no longer crashes on every launch if its saved login can't be read. It asks you to reconnect instead.

## [0.2.0-alpha] and earlier

Not tracked in this file. See the git history.

[0.3.0]: https://github.com/kawaii-not-kawaii/slopper/releases/tag/v0.3.0
