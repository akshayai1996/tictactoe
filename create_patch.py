import json

data = {
    "body": "🎮 Tic Tac Toe - 8 Language Implementations\n\nIncludes: HTML/JS, Python, Java, C#, Go, C++, Android, Rust"
}

with open("release_patch.json", "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False)
