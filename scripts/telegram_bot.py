#!/usr/bin/env python3
import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
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
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


def api_url(method: str) -> str:
    base_url = os.environ.get("TELEGRAM_API_BASE_URL", "https://api.telegram.org").rstrip("/")
    if "host.docker.internal" in base_url and not Path("/.dockerenv").exists():
        base_url = base_url.replace("host.docker.internal", "127.0.0.1")
    token = os.environ.get("TELEGRAM_BOT_TOKEN", "").strip()
    if not token:
        raise RuntimeError("TELEGRAM_BOT_TOKEN не найден в окружении или .env")
    return f"{base_url}/bot{token}/{method}"


def opener() -> urllib.request.OpenerDirector:
    proxy_host = os.environ.get("TELEGRAM_PROXY_HOST", "").strip()
    proxy_port = os.environ.get("TELEGRAM_PROXY_PORT", "").strip()
    proxy_type = os.environ.get("TELEGRAM_PROXY_TYPE", "HTTP").strip().lower()
    if proxy_host and proxy_port and proxy_port != "0":
        proxy_url = f"{proxy_type}://{proxy_host}:{proxy_port}"
        return urllib.request.build_opener(urllib.request.ProxyHandler({
            "http": proxy_url,
            "https": proxy_url,
        }))
    return urllib.request.build_opener()


def call(method: str, payload: dict | None = None) -> dict:
    body = json.dumps(payload or {}).encode("utf-8")
    request = urllib.request.Request(
        api_url(method),
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    timeout = int(os.environ.get("TELEGRAM_READ_TIMEOUT_SECONDS", "40"))
    try:
        with opener().open(request, timeout=timeout) as response:
            data = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        details = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Telegram API HTTP {error.code}: {details}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"Не удалось подключиться к Telegram API: {error.reason}") from error

    if not data.get("ok"):
        raise RuntimeError(f"Telegram API error: {data.get('description', 'unknown error')}")
    return data


def set_commands() -> None:
    call("setMyCommands", {
        "commands": [
            {"command": "start", "description": "Как подключить StudyMarket"},
            {"command": "id", "description": "Узнать chat id для уведомлений"},
        ]
    })
    print("Команды /start и /id отправлены в Telegram.")


def get_commands() -> None:
    commands = call("getMyCommands").get("result", [])
    if not commands:
        print("Команды у бота не заданы.")
        return
    for command in commands:
        print(f"/{command.get('command')} - {command.get('description', '')}")


def delete_webhook() -> None:
    call("deleteWebhook", {"drop_pending_updates": False})
    print("Webhook отключен, можно использовать polling.")


def get_me() -> None:
    result = call("getMe")["result"]
    username = result.get("username", "unknown")
    print(f"Бот доступен: @{username}")


def send_message(chat_id: str, text: str) -> None:
    call("sendMessage", {"chat_id": chat_id, "text": text})


def command_name(text: str) -> str:
    return text.strip().split(maxsplit=1)[0].split("@", 1)[0].lower()


def handle_update(update: dict) -> None:
    message = update.get("message") or update.get("edited_message") or {}
    text = message.get("text", "")
    chat = message.get("chat") or {}
    sender = message.get("from") or {}
    chat_id = chat.get("id")
    if not text or not chat_id:
        return

    command = command_name(text)
    if command in {"/start", "/ыефке"}:
        send_message(str(chat_id), (
            "Привет! Я бот StudyMarket.\n\n"
            "Напишите /id, и я покажу chat id для поля Telegram chat id в профиле."
        ))
    elif command in {"/id", "/шв"}:
        user_id = sender.get("id", "не найден")
        username = sender.get("username")
        username_line = f"\nUsername: @{username}" if username else ""
        send_message(str(chat_id), (
            "StudyMarket Telegram ID\n\n"
            f"Chat id: {chat_id}\n"
            f"User id: {user_id}{username_line}\n\n"
            "Для уведомлений StudyMarket используйте Chat id."
        ))


def poll() -> None:
    delete_webhook()
    set_commands()
    offset = 0
    print("Polling запущен. Нажмите Ctrl+C для остановки.")
    while True:
        data = call("getUpdates", {
            "offset": offset,
            "timeout": 25,
            "allowed_updates": ["message", "edited_message"],
        })
        for update in data.get("result", []):
            offset = max(offset, int(update["update_id"]) + 1)
            handle_update(update)
        time.sleep(0.2)


def poll_once() -> None:
    delete_webhook()
    set_commands()
    data = call("getUpdates", {
        "offset": 0,
        "timeout": 0,
        "limit": 20,
        "allowed_updates": ["message", "edited_message"],
    })
    updates = data.get("result", [])
    for update in updates:
        handle_update(update)
    print(f"Обработано update: {len(updates)}")


def main() -> int:
    load_dotenv()
    parser = argparse.ArgumentParser(description="StudyMarket Telegram bot helper")
    parser.add_argument("command", choices=["get-me", "set-commands", "get-commands", "delete-webhook", "poll", "poll-once"])
    args = parser.parse_args()
    try:
        if args.command == "get-me":
            get_me()
        elif args.command == "set-commands":
            set_commands()
        elif args.command == "get-commands":
            get_commands()
        elif args.command == "delete-webhook":
            delete_webhook()
        elif args.command == "poll":
            poll()
        elif args.command == "poll-once":
            poll_once()
    except KeyboardInterrupt:
        print("\nPolling остановлен.")
    except Exception as error:
        print(str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
