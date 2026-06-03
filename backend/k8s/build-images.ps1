param(
  [string]$Registry = "",
  [string]$Tag = "0.1.0",
  [switch]$Push
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$Registry = $Registry.TrimEnd("/")

function Get-ImageName {
  param([string]$Service)

  if ([string]::IsNullOrWhiteSpace($Registry)) {
    return "microservice-industry-${Service}:${Tag}"
  }

  return "${Registry}/microservice-industry-${Service}:${Tag}"
}

$modules = @(
  "discovery-server",
  "api-gateway",
  "auth-service",
  "user-service",
  "notification-service",
  "payment-service",
  "file-service",
  "ai-service",
  "audit-service"
)

Push-Location $repoRoot
try {
  $configImage = Get-ImageName "config-server"
  docker build -f Dockerfile-config-server -t $configImage .
  if ($Push) {
    docker push $configImage
  }

  foreach ($module in $modules) {
    $image = Get-ImageName $module
    docker build --build-arg MODULE=$module -t $image .
    if ($Push) {
      docker push $image
    }
  }
}
finally {
  Pop-Location
}
