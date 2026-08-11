# PowerShell Script to build and run all loan microservices locally

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "Building Loan Microservices Architecture..." -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Check for Java 21
$javaVersion = java -version 2>&1
if ($null -eq $javaVersion) {
    Write-Error "Java is not installed or not in PATH."
    exit 1
}

# Run maven package
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed."
    exit 1
}

Write-Host "`n=============================================" -ForegroundColor Green
Write-Host "Build Successful! Starting Microservices..." -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

$services = @(
    @{ Name = "credit-service"; Port = 8082; Jar = "credit-service/target/credit-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "valuation-service"; Port = 8083; Jar = "valuation-service/target/valuation-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "underwriting-service"; Port = 8084; Jar = "underwriting-service/target/underwriting-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "disbursement-service"; Port = 8085; Jar = "disbursement-service/target/disbursement-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "notification-service"; Port = 8086; Jar = "notification-service/target/notification-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "loan-service"; Port = 8081; Jar = "loan-service/target/loan-service-0.0.1-SNAPSHOT.jar" }
)

$processes = @()

foreach ($srv in $services) {
    Write-Host "Starting $($srv.Name) on port $($srv.Port)..." -ForegroundColor Yellow
    # Start the process in a new minimized console window so it runs concurrently
    $p = Start-Process java -ArgumentList "-jar $($srv.Jar)" -WindowStyle Minimized -PassThru
    $processes += @{ Process = $p; Name = $srv.Name }
}

Write-Host "`nAll microservices are starting up in the background!" -ForegroundColor Green
Write-Host "Dashboard will be available at: http://localhost:8081/index.html" -ForegroundColor Cyan
Write-Host "`nPress Ctrl+C in this terminal window to stop all services..." -ForegroundColor Red

try {
    # Keep the script running to monitor Ctrl+C
    while ($true) {
        Start-Sleep -Seconds 1
    }
}
finally {
    Write-Host "`nStopping all microservice processes..." -ForegroundColor Red
    foreach ($proc in $processes) {
        if ($null -ne $proc.Process -and !$proc.Process.HasExited) {
            Write-Host "Stopping $($proc.Name)..."
            Stop-Process -Id $proc.Process.Id -Force
        }
    }
    Write-Host "All processes stopped. Exit." -ForegroundColor Green
}
