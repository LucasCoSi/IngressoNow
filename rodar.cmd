@echo off
REM Atalho para quem usa cmd.exe: o cmd nao executa .ps1 diretamente.
REM Uso: rodar.cmd            (API + desktop)
REM       rodar.cmd --headless  (so API)
REM Repassa os argumentos para o rodar.ps1 e ignora a ExecutionPolicy.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0rodar.ps1" %*
