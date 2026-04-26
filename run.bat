@echo off
setlocal

if not exist out mkdir out
javac -encoding UTF-8 -d out src\main\java\com\example\snake\*.java
if errorlevel 1 (
    echo Compile failed.
    pause
    exit /b 1
)

java -cp out com.example.snake.SnakeGame
endlocal
