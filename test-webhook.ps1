# Simulates a Meta Instagram comment webhook POST to test the local pipeline.
# Run this AFTER the app is running (mvnw.cmd spring-boot:run) in a separate PowerShell window.

$body = @{
    object = "instagram"
    entry = @(@{
        id = "12345"
        time = 1234567890
        changes = @(@{
            field = "comments"
            value = @{
                id = "17895695668004550"
                text = "LINK please"
                from = @{ id = "999"; username = "testuser" }
                media = @{ id = "888" }
            }
        })
    })
} | ConvertTo-Json -Depth 10

Write-Host "Sending test webhook payload to http://localhost:8080/webhook ..."
Invoke-RestMethod -Uri "http://localhost:8080/webhook" -Method Post -Body $body -ContentType "application/json"
Write-Host "Done. Check the app console for 'Trigger keyword matched' and the Meta Graph API response/error log."
