@echo off
set CSC=C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe

echo Uses compiler: %CSC%
echo Compiling...

"%CSC%" /target:winexe /out:TicTacToe.exe TicTacToe.cs /r:System.Windows.Forms.dll /r:System.Drawing.dll

if %ERRORLEVEL% EQU 0 (
    echo Compilation Successful! Launching...
    start TicTacToe.exe
) else (
    echo Compilation Failed.
    pause
)
