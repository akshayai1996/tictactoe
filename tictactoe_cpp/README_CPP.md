# How to Build C++ Tic-Tac-Toe

This folder contains the complete C++ source code for the game, using the **Qt6** framework.

## Prerequisites
1. **Qt 6 Framework**: Install from [qt.io](https://www.qt.io/download-qt-installer) or via your package manager.
2. **CMake**: A standard build tool.
3. **C++ Compiler**: `g++` (MinGW) or `MSVC` (Visual Studio).

## Building with Qt Creator (Easiest)
1. Open **Qt Creator**.
2. Select **File > Open File or Project...**
3. Open `CMakeLists.txt` from this folder.
4. Configure the project with your installed Qt kit.
5. Click the green **Run** button (Ctrl+R).

## Building from Command Line
If you have Qt and CMake in your PATH:

```bash
mkdir build
cd build
cmake ..
cmake --build .
./TicTacToe_QT.exe
```

## Features Ported
- **Circle Radio Buttons**: Custom `paintEvent` override to draw perfect circles.
- **Minimax AI**: Full recursive algorithm in native C++.
- **Sound**: Uses Windows `Beep` API on separate threads for non-blocking audio.
- **Glassmorphism UI**: Identical stylesheets and shadows as the Python version.
