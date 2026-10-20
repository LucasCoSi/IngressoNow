# Uso:  .\rodar.cmd            (API + desktop)  <- no cmd.exe
#       .\rodar.ps1            (API + desktop)  <- no PowerShell
#       .\rodar.cmd --headless  (só API)
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

# 2) Portas: o jar usa 8080; o Postgres usa 5432. Se outro projeto estiver
#    rodando nelas, avisa e para (antes ficava 90s esperando o banco em vao).
function DonoPorta([int]$porta) {
  try {
    $nomes = @(docker ps --filter "publish=$porta" --format '{{.Names}}' 2>$null)
    if ($nomes.Count -gt 0) { return ($nomes -join ',') }
  } catch { }
  $conn = Get-NetTCPConnection -LocalPort $porta -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1
  if ($conn) {
    $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
    if ($proc) { return "$($proc.ProcessName) (pid $($proc.Id))" }
    return 'processo local'
  }
  return $null
}

# o container app usa a mesma porta 8080 do jar local
Compose @('stop', 'app')

$bloqueios = @()
$donoBanco = DonoPorta 5432
if ($donoBanco -and ($donoBanco -notmatch 'poo2-postgres')) {
  $bloqueios += "5432 em uso por $donoBanco"
}
$donoApi = DonoPorta 8080
if ($donoApi) {
  if ($donoApi -match '^java \(pid (\d+)\)') {
    $bloqueios += "8080 em uso pelo proprio app (java pid $($Matches[1])): feche a instancia atual ou abra http://localhost:8080"
  } else {
    $bloqueios += "8080 em uso por $donoApi"
  }
}
if ($bloqueios.Count -gt 0) {
  Write-Host ''
  Write-Host 'Nao foi possivel iniciar: portas ocupadas.' -ForegroundColor Yellow
  $bloqueios | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
  Write-Host '  - instancia do app: Stop-Process -Id <pid>' -ForegroundColor Yellow
  Write-Host '  - outro stack: docker compose -p <projeto> down' -ForegroundColor Yellow
  exit 1
}

# 3) Postgres (so o banco; o jar roda local)
Compose @('up', '-d', 'postgres')
$t = 0
while ((Calmo { docker exec poo2-postgres pg_isready -U postgres -d poo2 } -ne 0) -and ($t -lt 90)) {
  Start-Sleep -Seconds 2; $t += 2
}
if (Calmo { docker exec poo2-postgres pg_isready -U postgres -d poo2 } -ne 0) {
  throw 'Postgres nao ficou pronto em 90s. Veja: docker logs poo2-postgres'
}
# O pg_isready acima testa o socket interno; se o container estiver sem o
# mapeamento de porta (acontece apos conflito de 5432), o jar nao conecta.
if (-not (Get-NetTCPConnection -LocalPort 5432 -State Listen -ErrorAction SilentlyContinue)) {
  throw 'Postgres responde no container, mas a porta 5432 nao esta publicada no host. Rode: docker compose up -d --force-recreate postgres'
}
Write-Host 'Banco OK. Iniciando app...'

# 4) App (repassa flags, ex: --headless)
& java --enable-native-access=ALL-UNNAMED -jar target\plataforma-eventos-0.0.1-SNAPSHOT.jar @AppArgs
