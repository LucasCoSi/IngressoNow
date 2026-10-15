# Uso:  .\rodar.ps1            (API + desktop)
#       .\rodar.ps1 --headless  (só API)
# Sobe o Postgres (docker-compose), espera aceitar conexão e roda o jar.
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$AppArgs)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $MyInvocation.MyCommand.Path)

# Nativas (docker/compose) cospem progresso no stderr; sob 'Stop' isso aborta
# o script mesmo com 2>$null. Roda acalmado e devolve o exit code via $LASTEXITCODE.
function Calmo([scriptblock]$s) {
  $prev = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try { & $s 2>$null | Out-Null } finally { $ErrorActionPreference = $prev }
  return $LASTEXITCODE
}

function Compose($c) {
  if (Get-Command docker-compose -ErrorAction SilentlyContinue) { Calmo { docker-compose @c } | Out-Null }
  else { Calmo { docker compose @c } | Out-Null }
}

# 1) Docker daemon no ar (tenta abrir o Docker Desktop se estiver parado)
if (Calmo { docker info } -ne 0) {
  $dd = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
  if (Test-Path -LiteralPath $dd) { Write-Host 'Abrindo Docker Desktop...'; Start-Process $dd }
  $t = 0
  while ((Calmo { docker info } -ne 0) -and ($t -lt 180)) { Start-Sleep -Seconds 5; $t += 5 }
  if (Calmo { docker info } -ne 0) { throw 'Docker indisponivel. Abra o Docker Desktop e rode de novo.' }
}

# 2) Postgres (só o banco; o jar roda local sem conflitar a porta 8080)
Compose @('up', '-d', 'postgres')
Compose @('stop', 'app')  # o container app usa a mesma porta 8080 do jar local
$t = 0
while ((Calmo { docker exec poo2-postgres pg_isready -U postgres -d poo2 } -ne 0) -and ($t -lt 90)) {
  Start-Sleep -Seconds 2; $t += 2
}
if (Calmo { docker exec poo2-postgres pg_isready -U postgres -d poo2 } -ne 0) {
  throw 'Postgres nao ficou pronto em 90s. Veja: docker logs poo2-postgres'
}
Write-Host 'Banco OK. Iniciando app...'

# 3) App (repassa flags, ex: --headless)
& java --enable-native-access=ALL-UNNAMED -jar target\plataforma-eventos-0.0.1-SNAPSHOT.jar @AppArgs
