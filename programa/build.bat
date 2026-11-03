@echo off

cd /d "%~dp0"
set GEN=src\compilador\generated
if not exist %GEN% mkdir %GEN%
if not exist bin mkdir bin

echo [1/3] Generando parser con CUP...
java -cp "lib\java-cup-11b.jar;lib\java-cup-11b-runtime.jar" java_cup.Main ^
     -destdir %GEN% -package compilador.generated ^
     -parser Parser -symbols sym spec\Parser.cup
if errorlevel 1 goto error

echo [2/3] Generando scanner con JFlex...
java -jar lib\jflex-full-1.9.1.jar -d %GEN% --nobak spec\Lexer.flex
if errorlevel 1 goto error

echo [3/3] Compilando...
javac -encoding UTF-8 -cp lib\java-cup-11b-runtime.jar -d bin src\compilador\*.java %GEN%\*.java
if errorlevel 1 goto error

echo.
echo Compilacion exitosa
exit /b 0

:error
echo.
echo *** Fallo la compilacion ***
exit /b 1