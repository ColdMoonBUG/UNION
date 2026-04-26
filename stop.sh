#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="jusic-serve"
LOG_DIR="$ROOT_DIR/logs"
PID_FILE="$LOG_DIR/${APP_NAME}.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "未找到 PID 文件: $PID_FILE"
  echo "如果服务已停止，可以忽略。"
  exit 0
fi

PID="$(cat "$PID_FILE")"
if [[ -z "$PID" ]]; then
  rm -f "$PID_FILE"
  echo "PID 文件为空，已清理。"
  exit 0
fi

if ! kill -0 "$PID" 2>/dev/null; then
  rm -f "$PID_FILE"
  echo "$APP_NAME 未运行，已清理旧 PID 文件。"
  exit 0
fi

kill "$PID"
rm -f "$PID_FILE"

echo "$APP_NAME 已停止，PID=$PID"
