# Copie e execute estas linhas no PowerShell do VS Code, usando a sua fila.
# Identificadores de fila nao sao credenciais. NUNCA salve a chave PEM neste projeto.
$env:OCI_QUEUE_ID = "COLE_AQUI_O_OCID_DA_FILA_PRINCIPAL"
$env:OCI_QUEUE_MESSAGES_ENDPOINT = "https://cell-1.queue.messaging.us-phoenix-1.oci.oraclecloud.com"
$env:MAIL_MODE = "LOG"

# Opcional: envio real de e-mail, somente com conta SMTP de testes autorizada.
# $env:MAIL_MODE = "SMTP"
# $env:SMTP_HOST = "smtp.example.com"
# $env:SMTP_PORT = "587"
# $env:SMTP_USERNAME = "seu-usuario"
# $env:SMTP_PASSWORD = "SENHA_DE_APLICATIVO"
# $env:SMTP_FROM = "seu-email@example.com"

# Depois execute: .\iniciar.ps1
