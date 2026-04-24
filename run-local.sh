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

mkdir -p "$RUNTIME_DIR"

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

wait_for_service() {
  local name="$1"
  local url="$2"
  local pid_file="$3"
  local log_file="$4"
  local retries=30

  for ((i = 1; i <= retries; i++)); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "==> $name is ready: $url"
      return 0
    fi

    if [[ -f "$pid_file" ]]; then
      local pid
      pid="$(cat "$pid_file")"
      if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then
        echo "==> $name exited unexpectedly, latest log:"
        tail -n 40 "$log_file" 2>/dev/null || true
        return 1
      fi
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
  echo "==> Backend already running (PID $(cat "$BACKEND_PID_FILE"))"
else
  echo "==> Starting backend on http://localhost:8081"
  (
    cd "$BACKEND_DIR"
    nohup java -jar "$BACKEND_JAR" --spring.profiles.active=test >"$BACKEND_LOG" 2>&1 &
    echo $! >"$BACKEND_PID_FILE"
  )
fi

if is_running "$FRONTEND_PID_FILE"; then
  echo "==> Frontend already running (PID $(cat "$FRONTEND_PID_FILE"))"
else
  echo "==> Starting frontend on http://localhost:3000"
  (
    cd "$FRONTEND_DIR"
    nohup npm run dev -- --host 0.0.0.0 >"$FRONTEND_LOG" 2>&1 &
    echo $! >"$FRONTEND_PID_FILE"
  )
fi

wait_for_service "Backend" "http://localhost:8081/api/health" "$BACKEND_PID_FILE" "$BACKEND_LOG"
wait_for_service "Frontend" "http://localhost:3000" "$FRONTEND_PID_FILE" "$FRONTEND_LOG"

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
