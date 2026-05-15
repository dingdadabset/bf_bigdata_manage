#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="$ROOT_DIR/.run-local"
BACKEND_DIR="$ROOT_DIR/dga-backend"
FRONTEND_DIR="$ROOT_DIR/dga-frontend"
BACKEND_PID_FILE="$RUNTIME_DIR/backend.pid"
FRONTEND_PID_FILE="$RUNTIME_DIR/frontend.pid"
BACKEND_JAR="$BACKEND_DIR/target/dga-backend-0.0.1-SNAPSHOT.jar"
BACKEND_PORT=8081
FRONTEND_PORT=3000

listening_pid() {
  local port="$1"
  lsof -nP -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -n 1 || true
}

process_command() {
  local pid="$1"
  ps -p "$pid" -o command= 2>/dev/null || true
}

process_cwd() {
  local pid="$1"
  local line
  lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | while IFS= read -r line; do
    case "$line" in
      n*) printf '%s\n' "${line#n}"; break ;;
    esac
  done
}

service_owns_pid() {
  local name="$1"
  local pid="$2"
  local command
  local cwd

  command="$(process_command "$pid")"
  cwd="$(process_cwd "$pid")"

  case "$name" in
    backend)
      [[ "$command" == *"$BACKEND_JAR"* ]] || [[ "$command" == *"dga-backend-0.0.1-SNAPSHOT.jar"* ]] || [[ "$cwd" == "$BACKEND_DIR" ]]
      ;;
    frontend)
      { [[ "$cwd" == "$FRONTEND_DIR" ]] && [[ "$command" == *"npm"* || "$command" == *"node"* || "$command" == *"vite"* ]]; }
      ;;
    *)
      return 1
      ;;
  esac
}

stop_pid() {
  local name="$1"
  local pid="$2"

  if [[ -z "$pid" ]] || ! kill -0 "$pid" 2>/dev/null; then
    return 1
  fi

  kill "$pid"
  echo "==> Stopped $name (PID $pid)"
  return 0
}

stop_service() {
  local name="$1"
  local pid_file="$2"
  local port="$3"
  local stopped=0

  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file")"
    if stop_pid "$name" "$pid"; then
      stopped=1
    else
      echo "==> $name pid file was stale"
    fi
    rm -f "$pid_file"
  fi

  local owner_pid
  owner_pid="$(listening_pid "$port")"
  if [[ -n "$owner_pid" ]]; then
    if service_owns_pid "$name" "$owner_pid"; then
      stop_pid "$name" "$owner_pid" || true
      stopped=1
    else
      echo "==> $name port $port is still used by external PID $owner_pid; leaving it running"
    fi
  fi

  if [[ "$stopped" -eq 0 && -z "${owner_pid:-}" ]]; then
    echo "==> $name is not running"
  fi
}

stop_service "backend" "$BACKEND_PID_FILE" "$BACKEND_PORT"
stop_service "frontend" "$FRONTEND_PID_FILE" "$FRONTEND_PORT"
