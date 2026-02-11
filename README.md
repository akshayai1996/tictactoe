# 🎮 Tic Tac Toe — Multi-Language Collection

A feature-rich Tic Tac Toe game implemented in **8 different programming languages**, all sharing the same glassmorphism dark theme, minimax AI, and gameplay experience.

## 🚀 Downloads & Implementations

**[All Releases](https://github.com/akshayai1996/tictactoe/releases)** | **[Latest v1.0](https://github.com/akshayai1996/tictactoe/releases/tag/v1.0)**

| # | Language | Framework | Directory | Executable / Download | Requirements |
|---|----------|-----------|-----------|-----------------------|--------------|
| 1 | **HTML/JavaScript** | Vanilla JS | [`tictactoe_javascript/`](tictactoe_javascript/) | [`tictactoe.html`](https://github.com/akshayai1996/tictactoe/releases/download/v1.0/tictactoe.html) | Web Browser (Chrome, Edge, Firefox) |
| 2 | **Python** | PySide6 (Qt) | [`tictactoe_python/`](tictactoe_python/) | [`tictactoe.pyw`](https://github.com/akshayai1996/tictactoe/releases/download/v1.0/tictactoe.pyw) | Python 3.6+ with `PySide6` installed |
| 3 | **Java** | Swing | [`tictactoe_java/`](tictactoe_java/) | [`tictactoe.jar`](https://github.com/akshayai1996/tictactoe/releases/download/v1.0/tictactoe.jar) | Java Runtime Environment (JRE) 8+ |
| 4 | **C#** | WPF | [`tictactoe_csharp/`](tictactoe_csharp/) | [`tictactoe_csharp.exe`](https://github.com/akshayai1996/tictactoe/releases/download/v1.0/tictactoe_csharp.exe) | Windows with .NET Framework 4.7.2+ |
| 5 | **Go** | Fyne | [`tictactoe_go/`](tictactoe_go/) | [`tictactoe_go.exe`](https://github.com/akshayai1996/tictactoe/releases/download/v1.0/tictactoe_go.exe) | None (Standalone) |
| 6 | **C++** | Qt6 | [`tictactoe_cpp/`](tictactoe_cpp/) | [`tictactoe_cpp.exe`](https://github.com/akshayai1996/tictactoe/releases/download/v1.0/tictactoe_cpp.exe) | Windows with Qt6 DLLs available |
| 7 | **Android** | Kotlin/Jetpack Compose | [`tictactoe_android/`](tictactoe_android/) | [`tictactoe.apk`](https://github.com/akshayai1996/tictactoe/releases/download/v1.0/tictactoe.apk) | Android 8.0+ (Oreo) |
| 8 | **Rust** | eframe/egui | [`tictactoe_rust/`](tictactoe_rust/) | [`tictactoe_rust.exe`](https://github.com/akshayai1996/tictactoe/releases/download/v1.0/tictactoe_rust.exe) | None (Standalone) |

## ✨ Features (All Versions)

- 🎨 **Glassmorphism Dark Theme** — Consistent premium UI across all platforms
- 🤖 **Minimax AI** — Unbeatable computer opponent
- 📊 **3 Difficulty Levels** — Easy, Medium, Hard
- 🔊 **Sound Effects** — Audio feedback for moves, wins, and losses
- ⚙️ **Customizable** — Choose your marker (X/O) and who starts
- 🏆 **Win/Lose/Draw Detection** — With colored cell highlighting

## 🎨 Theme

All versions share the same dark glassmorphism color palette:

| Element | Color |
|---------|-------|
| Background | `#020617` |
| Card | `#0f172a` |
| Cell | `#1e293b` |
| Accent (Cyan) | `#22d3ee` |
| Win (Green) | `#10b981` |
| Lose (Red) | `#f43f5e` |

## 🏗️ Building Each Version

### HTML/JavaScript
Just open `tictactoe.html` in any browser. No build required!

### Python
```bash
pip install PySide6
python tictactoe.pyw
```

### Java
```bash
cd tictactoe_java
javac TicTacToe.java
java TicTacToe
```

### C# (WPF)
```bash
cd tictactoe_csharp
dotnet run
```

### Go
```bash
cd tictactoe_go
go run .
```

### C++ (Qt6)
```bash
cd tictactoe_cpp
mkdir build && cd build
cmake .. -G Ninja
ninja
```

### Android
Open `tictactoe_android/` in Android Studio and build.

### Rust
```bash
cd tictactoe_rust
cargo build --release
# Executable: target/release/tictactoe_rust.exe
```

## 📝 License

MIT License
