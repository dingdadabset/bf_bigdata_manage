#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="$ROOT_DIR/.run-local"
BACKEND_DIR="$ROOT_DIR/dga-backend"
FRONTEND_DIR="$ROOT_DIR/dga-frontend"
BACKEND_PID_FILE="$RUNTIME_DIR/backend.pid"
FRONTEND_PID_FILE="$RUNTIME_DIR/frontend.pid"
BACKEND_LOG="$RUNTIME_DIR/backend.log"
FRONTEND_LOG="$RUNTIME_DIR/frontend.log"
BACKEND_JAR="$BACKEND_DIR/target/dga-backend-0.0.1-SNAPSHOT.jar"
BACKEND_PORT=8081
FRONTEND_PORT=3000

mkdir -p "$RUNTIME_DIR"

listening_pid() {
  local port="$1"
  lsof -nP -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -n 1 || true
}

pid_from_file() {
  local pid_file="$1"
  [[ -f "$pid_file" ]] && cat "$pid_file" || true
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
    Backend)
      [[ "$command" == *"$BACKEND_JAR"* ]] || [[ "$command" == *"dga-backend-0.0.1-SNAPSHOT.jar"* ]] || [[ "$cwd" == "$BACKEND_DIR" ]]
      ;;
    Frontend)
      { [[ "$cwd" == "$FRONTEND_DIR" ]] && [[ "$command" == *"npm"* || "$command" == *"node"* || "$command" == *"vite"* ]]; }
      ;;
    *)
      return 1
      ;;
  esac
}

is_running() {
  local pid_file="$1"
  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file")"
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      return 0
    fi
    rm -f "$pid_file"
  fi
  return 1
}

ensure_port_available() {
  local name="$1"
  local port="$2"
  local pid_file="$3"
  local owner_pid
  local expected_pid

  owner_pid="$(listening_pid "$port")"
  [[ -z "$owner_pid" ]] && return 0

  expected_pid="$(pid_from_file "$pid_file")"
  if [[ -n "$expected_pid" ]] && [[ "$owner_pid" == "$expected_pid" ]] && kill -0 "$expected_pid" 2>/dev/null; then
    return 0
  fi

  if service_owns_pid "$name" "$owner_pid"; then
    mkdir -p "$(dirname "$pid_file")"
    echo "$owner_pid" >"$pid_file"
    echo "==> Found existing $name on port $port (PID $owner_pid), adopted pid file"
    return 0
  fi

  echo "==> Cannot start $name: port $port is already used by PID $owner_pid"
  if [[ -n "$expected_pid" ]]; then
    echo "==> $name pid file points to PID $expected_pid, which does not own port $port"
  else
    echo "==> $name pid file is missing"
  fi
  echo "==> Run ./stop-local.sh if it owns the process, or stop PID $owner_pid manually, then retry."
  return 1
}

wait_for_service() {
  local name="$1"
  local url="$2"
  local pid_file="$3"
  local log_file="$4"
  local retries=30

  for ((i = 1; i <= retries; i++)); do
    if [[ -f "$pid_file" ]]; then
      local pid
      pid="$(cat "$pid_file")"
      if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then
        echo "==> $name exited unexpectedly, latest log:"
        tail -n 40 "$log_file" 2>/dev/null || true
        return 1
      fi
    fi

    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "==> $name is ready: $url"
      return 0
    fi

    sleep 1
  done

  echo "==> $name did not become ready in time, latest log:"
  tail -n 40 "$log_file" 2>/dev/null || true
  return 1
}

echo "==> Preparing backend"
(cd "$BACKEND_DIR" && mvn -q -DskipTests package)

echo "==> Preparing frontend"
if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
  (cd "$FRONTEND_DIR" && npm install)
fi

if is_running "$BACKEND_PID_FILE"; then
  ensure_port_available "Backend" "$BACKEND_PORT" "$BACKEND_PID_FILE"
  echo "==> Backend already running (PID $(cat "$BACKEND_PID_FILE"))"
else
  ensure_port_available "Backend" "$BACKEND_PORT" "$BACKEND_PID_FILE"
  echo "==> Starting backend on http://localhost:$BACKEND_PORT"
  : >"$BACKEND_LOG"
  (
    cd "$BACKEND_DIR"
    nohup java -jar "$BACKEND_JAR" --spring.profiles.active=test >"$BACKEND_LOG" 2>&1 &
    echo $! >"$BACKEND_PID_FILE"
  )
fi

wait_for_service "Backend" "http://localhost:$BACKEND_PORT/api/health" "$BACKEND_PID_FILE" "$BACKEND_LOG"

if is_running "$FRONTEND_PID_FILE"; then
  ensure_port_available "Frontend" "$FRONTEND_PORT" "$FRONTEND_PID_FILE"
  echo "==> Frontend already running (PID $(cat "$FRONTEND_PID_FILE"))"
else
  ensure_port_available "Frontend" "$FRONTEND_PORT" "$FRONTEND_PID_FILE"
  echo "==> Starting frontend on http://localhost:$FRONTEND_PORT"
  : >"$FRONTEND_LOG"
  (
    cd "$FRONTEND_DIR"
    nohup npm run dev -- --host 0.0.0.0 >"$FRONTEND_LOG" 2>&1 &
    echo $! >"$FRONTEND_PID_FILE"
  )
fi

wait_for_service "Frontend" "http://localhost:$FRONTEND_PORT" "$FRONTEND_PID_FILE" "$FRONTEND_LOG"

echo
echo "DGA local services are running:"
echo "  Frontend: http://localhost:3000"
echo "  Backend:  http://localhost:8081"
echo "  Health:   http://localhost:8081/api/health"
echo
echo "Logs:"
echo "  $BACKEND_LOG"
echo "  $FRONTEND_LOG"
echo
echo "Stop both services with: ./stop-local.sh"
