#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="jusic-serve"
HTTP_PORT="8080"
JAR_PATH="$ROOT_DIR/target/${APP_NAME}.jar"
LOG_DIR="$ROOT_DIR/logs"
PID_FILE="$LOG_DIR/${APP_NAME}.pid"
LOG_FILE="$LOG_DIR/nohup.log"

mkdir -p "$LOG_DIR"

if [[ -f "$PID_FILE" ]]; then
  OLD_PID="$(cat "$PID_FILE")"
  if [[ -n "$OLD_PID" ]] && kill -0 "$OLD_PID" 2>/dev/null; then
    echo "$APP_NAME 已在运行中，PID=$OLD_PID"
    echo "业务日志: $LOG_DIR/common-all.log"
    echo "错误日志: $LOG_DIR/common-error.log"
    echo "启动兜底日志: $LOG_FILE"
    if command -v xdg-open >/dev/null 2>&1; then
      xdg-open "http://localhost:${HTTP_PORT}/" >/dev/null 2>&1 || true
    fi
    exit 0
  fi
  rm -f "$PID_FILE"
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "未找到 $JAR_PATH，开始打包..."
  bash "$ROOT_DIR/mvnw" -q -DskipTests package
fi

: > "$LOG_FILE"
nohup java -jar "$JAR_PATH" >> "$LOG_FILE" 2>&1 &
PID=$!
echo "$PID" > "$PID_FILE"

for _ in $(seq 1 30); do
  if command -v curl >/dev/null 2>&1 && curl -fsS "http://localhost:${HTTP_PORT}/" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if command -v xdg-open >/dev/null 2>&1; then
  xdg-open "http://localhost:${HTTP_PORT}/" >/dev/null 2>&1 || true
fi

echo "$APP_NAME 启动成功，PID=$PID"
echo "业务日志: $LOG_DIR/common-all.log"
echo "错误日志: $LOG_DIR/common-error.log"
echo "启动兜底日志: $LOG_FILE"
echo "停止进程: kill $PID"
