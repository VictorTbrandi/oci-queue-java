# OCI Queue Java - execução no PowerShell do Windows.
# Execute primeiro config.exemplo.ps1 com os valores da SUA fila, ou defina as variáveis
# OCI_QUEUE_ID e OCI_QUEUE_MESSAGES_ENDPOINT diretamente neste terminal.
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:OCI_QUEUE_ID)) {
    throw "Defina OCI_QUEUE_ID com o OCID da fila principal (nao use o OCID da DLQ)."
}
if ([string]::IsNullOrWhiteSpace($env:OCI_QUEUE_MESSAGES_ENDPOINT)) {
    throw "Defina OCI_QUEUE_MESSAGES_ENDPOINT com o ponto final de mensagens da OCI."
}
if ([string]::IsNullOrWhiteSpace($env:MAIL_MODE)) { $env:MAIL_MODE = "LOG" }

# PowerShell antigo/Windows pode interpretar UTF-8 usando a pagina de codigo local.
# Estes ajustes afetam apenas a sessao atual; nao alteram configuracoes globais.
chcp 65001 > $null
$utf8 = New-Object System.Text.UTF8Encoding($false)
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8

Write-Host "Iniciando o produtor/consumidor OCI Queue Java (modo $env:MAIL_MODE)..."
& mvn compile exec:java
if ($LASTEXITCODE -ne 0) { throw "Falha na execucao Maven (codigo $LASTEXITCODE)." }
