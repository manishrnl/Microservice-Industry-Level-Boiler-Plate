param(
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"
$target = "http://127.0.0.1:$Port"

Write-Host "Starting an HTTPS tunnel to $target"
Write-Host "After it prints a public https:// URL, open:"
Write-Host "https://manishrnl-microservice-template.netlify.app/login?api=<that-https-url>"
Write-Host ""

if (Get-Command cloudflared -ErrorAction SilentlyContinue) {
    cloudflared tunnel --url $target
    exit $LASTEXITCODE
}

Write-Host "cloudflared was not found. Falling back to localtunnel via npx."
Write-Host "If localtunnel asks for a password, install cloudflared and rerun this script."
npx --yes localtunnel --port $Port
