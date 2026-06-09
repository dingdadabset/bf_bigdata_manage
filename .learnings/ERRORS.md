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

## [ERR-20260602-001] run_local_stale_backend_jar

**Logged**: 2026-06-02T06:02:06Z
**Priority**: medium
**Status**: pending
**Area**: backend

### Summary
Restarting local services after backend dependency changes can fail if an old backend process still owns port 8081, and running only `mvn compile` is not enough for the jar used by `run-local.sh`.

### Error
```text
Web server failed to start. Port 8081 was already in use.
```

### Context
- Added a backend dependency and first verified with `mvn -q -DskipTests compile`.
- `run-local.sh` runs `dga-backend/target/dga-backend-0.0.1-SNAPSHOT.jar`, so backend runtime changes require a packaged jar.
- A previous backend PID was still alive during restart and the new process failed to bind port 8081.

### Suggested Fix
For backend dependency/runtime changes, use `run-local.sh` or `mvn -q -DskipTests package` before restarting; if port 8081 is stale, stop the old process and then rerun `run-local.sh`.

### Metadata
- Reproducible: yes
- Related Files: run-local.sh, dga-backend/pom.xml

---

## [ERR-20260602-002] cross_exec_localhost_unreachable

**Logged**: 2026-06-02T06:06:02Z
**Priority**: low
**Status**: pending
**Area**: infra

### Summary
A foreground Spring Boot process can remain running in one exec session while `curl` from another exec session cannot connect to its localhost port.

### Error
```text
curl: (7) Failed to connect to 127.0.0.1 port 8081 after 0 ms: Could not connect to server
```

### Context
- Started the backend in a long-running exec session and saw Spring Boot reach ACCEPTING_TRAFFIC.
- Separate exec calls to `curl 127.0.0.1:8081` failed immediately.
- The Java session stayed alive until explicitly stopped by PID, so this appears to be tool/session networking isolation rather than a backend startup failure.

### Suggested Fix
For localhost verification in this environment, prefer checks performed inside the same startup script/session, browser tooling, or normal user terminal rather than cross-exec curl against a foreground process.

### Metadata
- Reproducible: unknown
- Related Files: run-local.sh

---
