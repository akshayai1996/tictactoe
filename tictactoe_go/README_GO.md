# 🐹 Tic Tac Toe in Go (Golang)

A native cross-platform desktop application built with **Go** and **Fyne**.

## 🛠️ Prerequisites (One-time Setup)

Since Go compiles to machine code, it needs a compiler and C bindings for graphics.

### 1. Install Go
- Download the **Go MSI Installer** from: https://go.dev/dl/
- Run the installer and click Next > Next > Finish.

### 2. Install GCC (Required for GUI)
Fyne (the GUI toolkit) requires a C compiler to talk to your creative card.
- Download **TDM-GCC** from: https://jmeubank.github.io/tdm-gcc/
- Run the installer, select **Create**, and keep clicking Next.
- **IMPORTANT:** Restart your computer or terminal after installing both to update your PATH.

## 🚀 How to Run

1. Open your terminal in this folder (`tictactoe_go`).
2. Initialize the project (first time only):
   ```bash
   go mod init tictactoe
   go mod tidy
   ```
3. Run the game:
   ```bash
   go run .
   ```

## 📦 How to Build (.exe)

To create a standalone `tic-tac-toe.exe` (optimized & hidden console):

```bash
go build -ldflags="-s -w -H=windowsgui" -o tic-tac-toe.exe
```
*(The `-H=windowsgui` flag hides the console window)*

## 🧩 Structure
- `main.go`: Contains all the game logic, AI, and UI code in one file.
