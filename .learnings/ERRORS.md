# Errors

Command failures and integration errors.

---

## [ERR-20260512-002] zsh_path_variable_shadow

**Logged**: 2026-05-12T07:25:00Z
**Priority**: low
**Status**: pending
**Area**: infra

### Summary
Using `path` as a loop variable in zsh altered command lookup and caused standard commands to appear missing.

### Error
```text
zsh: command not found: curl
zsh: command not found: tr
zsh: command not found: cut
```

### Context
- Attempted a localhost API endpoint loop in zsh.
- The loop variable was named `path`, which conflicts with zsh's special path/PATH handling.

### Suggested Fix
Avoid `path` as a variable name in zsh scripts; use `endpoint` or `api_path` instead.

### Metadata
- Reproducible: yes
- Related Files: none

---

## [ERR-20260512-001] node_fetch_localhost_eprem

**Logged**: 2026-05-12T07:22:50Z
**Priority**: low
**Status**: pending
**Area**: infra

### Summary
Node `fetch` from the sandbox failed against localhost while `curl` to the same backend worked.

### Error
```text
TypeError: fetch failed
cause: connect EPERM ::1:8081 / 127.0.0.1:8081
```

### Context
- Attempted to batch-check local API endpoints with `node -e` and `fetch`.
- The same localhost backend was reachable with `curl`.
- This appears to be a sandbox/tool networking limitation, not an application failure.

### Suggested Fix
Use `curl` or browser session requests for localhost API checks in this workspace instead of Node `fetch`.

### Metadata
- Reproducible: unknown
- Related Files: dga-backend/src/main/java/com/dga/access/security/JwtService.java

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
