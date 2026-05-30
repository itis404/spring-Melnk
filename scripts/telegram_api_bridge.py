#!/usr/bin/env python3
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def load_dotenv(path: Path = ROOT / ".env") -> None:
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def upstream_url(path: str) -> str:
    base_url = os.environ.get("TELEGRAM_UPSTREAM_API_BASE_URL", "https://api.telegram.org").rstrip("/")
    return base_url + path


def read_json(handler: BaseHTTPRequestHandler) -> bytes:
    if handler.headers.get("Transfer-Encoding", "").lower() == "chunked":
        return read_chunked_body(handler)

    content_length = int(handler.headers.get("Content-Length", "0"))
    if content_length <= 0:
        return b"{}"
    return handler.rfile.read(content_length)


def read_chunked_body(handler: BaseHTTPRequestHandler) -> bytes:
    chunks: list[bytes] = []
    while True:
        size_line = handler.rfile.readline().strip()
        if not size_line:
            continue
        chunk_size = int(size_line.split(b";", 1)[0], 16)
        if chunk_size == 0:
            handler.rfile.readline()
            break
        chunks.append(handler.rfile.read(chunk_size))
        handler.rfile.readline()
    return b"".join(chunks) or b"{}"


def forward(path: str, body: bytes) -> tuple[int, bytes]:
    request = urllib.request.Request(
        upstream_url(path),
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    timeout = int(os.environ.get("TELEGRAM_READ_TIMEOUT_SECONDS", "40"))
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        error_body = error.read()
        try:
            description = json.loads(error_body.decode("utf-8")).get("description", "unknown error")
        except Exception:
            description = error_body.decode("utf-8", errors="replace")
        print(f"Telegram upstream HTTP {error.code}: {description}", file=sys.stderr, flush=True)
        return error.code, error_body
    except urllib.error.URLError as error:
        print(f"Telegram upstream connection failed: {error.reason}", file=sys.stderr, flush=True)
        raise


class TelegramBridgeHandler(BaseHTTPRequestHandler):
    server_version = "StudyMarketTelegramBridge/1.0"

    def do_POST(self) -> None:
        token = os.environ.get("TELEGRAM_BOT_TOKEN", "").strip()
        expected_prefix = f"/bot{token}/"
        if not token or not self.path.startswith(expected_prefix):
            self.send_json(404, {"ok": False, "description": "unknown bot endpoint"})
            return

        body = read_json(self)
        try:
            status, response_body = forward(self.path, body)
        except urllib.error.URLError as error:
            self.send_json(502, {"ok": False, "description": f"upstream connection failed: {error.reason}"})
            return

        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(response_body)))
        self.end_headers()
        self.wfile.write(response_body)

    def do_GET(self) -> None:
        if self.path == "/health":
            self.send_json(200, {"ok": True, "version": "1.1-chunked"})
            return
        self.send_json(404, {"ok": False, "description": "not found"})

    def log_message(self, format: str, *args: object) -> None:
        safe_path = self.path
        token = os.environ.get("TELEGRAM_BOT_TOKEN", "").strip()
        if token:
            safe_path = safe_path.replace(token, "***")
        message = (format % args).replace(self.path, safe_path)
        sys.stderr.write("%s - %s\n" % (self.log_date_time_string(), message))

    def send_json(self, status: int, payload: dict) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main() -> int:
    load_dotenv()
    parser = argparse.ArgumentParser(description="Host bridge for Telegram Bot API")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=9091)
    args = parser.parse_args()

    if not os.environ.get("TELEGRAM_BOT_TOKEN", "").strip():
        print("TELEGRAM_BOT_TOKEN не найден в окружении или .env", file=sys.stderr)
        return 1

    server = ThreadingHTTPServer((args.host, args.port), TelegramBridgeHandler)
    print(f"Telegram API bridge слушает http://{args.host}:{args.port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nBridge остановлен.")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
