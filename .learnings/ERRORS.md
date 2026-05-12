# Errors

Command failures and integration errors.

---

## [ERR-20260508-001] missing_status_script

**Logged**: 2026-05-08T15:03:00+08:00
**Priority**: low
**Status**: pending
**Area**: infra

### Summary
Assumed `./status.sh` existed in this project, but only `run-local.sh` and `stop-local.sh` are present.

### Error
```text
zsh:1: no such file or directory: ./status.sh
```

### Context
- Command attempted from repository root: `./status.sh`
- The project root contains `run-local.sh` and `stop-local.sh`, but no status script.

### Suggested Fix
Check available local scripts with `ls` before invoking status helpers in this repo.

### Metadata
- Reproducible: yes
- Related Files: run-local.sh

---
