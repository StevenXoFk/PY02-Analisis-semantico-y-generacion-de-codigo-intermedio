@echo off

cd /d "%~dp0"
if "%~1"=="" (
    echo Uso: run.bat ^<ruta_archivo_fuente^>
    exit /b 1
)
chcp 65001 > nul
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 ^
     -cp "bin;lib\java-cup-11b-runtime.jar" compilador.Main "%~1"