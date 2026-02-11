@echo off
echo === Tic Tac Toe Java Build ===
echo.

echo [1/3] Compiling TicTacToe.java...
javac TicTacToe.java
if %ERRORLEVEL% NEQ 0 (
    echo Compilation Failed!
    pause
    exit /b
)
echo       Done!

echo [2/3] Packaging into TicTacToe.jar...
jar cfm TicTacToe.jar MANIFEST.MF *.class
if %ERRORLEVEL% NEQ 0 (
    echo Packaging Failed!
    pause
    exit /b
)
echo       Done!

echo [3/3] Launching...
echo.
java -jar TicTacToe.jar
