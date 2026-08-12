[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080/api/v1',
    [string]$Username = 'investment_analyst',
    [string]$Password = 'Password123!',
    [string]$ProjectCode = 'UAT-DEMO-001'
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$headers = @{}

function Invoke-IidsApi {
    param(
        [ValidateSet('GET', 'POST', 'PUT', 'DELETE')]
        [string]$Method,
        [string]$Path,
        [object]$Body
    )

    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        ContentType = 'application/json; charset=utf-8'
    }
    if ($null -ne $Body) {
        $parameters.Body = $Body | ConvertTo-Json -Depth 10
    }
    $response = Invoke-RestMethod @parameters
    if ($response.code -ne 'SUCCESS') {
        throw "API failed: $Method $Path - $($response.code) $($response.message)"
    }
    return $response.data
}

Write-Host "Connecting to $BaseUrl ..." -ForegroundColor Cyan
$login = Invoke-IidsApi POST '/auth/login' @{
    username = $Username
    password = $Password
}
$headers.Authorization = "Bearer $($login.token)"

$existingProject = @(Invoke-IidsApi GET '/projects' $null) | Where-Object { $_.code -eq $ProjectCode } | Select-Object -First 1
if ($existingProject) {
    Write-Host "Demo project already exists: $ProjectCode (id=$($existingProject.id)). No duplicate data was created." -ForegroundColor Yellow
    exit 0
}

$project = Invoke-IidsApi POST '/projects' @{
    code = $ProjectCode
    name = 'User Acceptance Demo Project'
    projectType = 'INDUSTRIAL'
    department = 'Investment Department'
    tags = 'UAT,demo'
    description = 'Created by scripts/initialize_demo_data.ps1'
}

function New-DemoScenario {
    param(
        [string]$Name,
        [decimal]$Price,
        [decimal]$UnitCost
    )

    $scenario = Invoke-IidsApi POST "/projects/$($project.id)/scenarios" @{
        name = $Name
        horizonYears = 5
        constructionYears = 1
        remarks = 'Reusable UAT demo scenario'
    }

    Invoke-IidsApi PUT "/scenarios/$($scenario.id)/parameters" @{
        wacc = 0.10
        waccSource = 'UAT demo assumption'
        taxRate = 0.25
        depreciationYears = 5
        residualRate = 0
        loanRatioLimit = 0.70
        pricePerUnit = $Price
        unitCost = $UnitCost
        annualOutput = 1000
        fixedOperatingCost = 10000
        formulaVersion = 'fin-std-2.0.0'
        depreciationPolicy = 'STRAIGHT_LINE'
        amortizationYears = 0
        amortizableAmount = 0
        repaymentMethod = 'EQUAL_PRINCIPAL'
    } | Out-Null

    Invoke-IidsApi POST "/scenarios/$($scenario.id)/investment-items" @{
        category = 'CONSTRUCTION'
        name = 'Construction Investment'
        amount = 200000
        yearNo = 0
        itemCode = 'CONST'
        sortOrder = 1
    } | Out-Null
    Invoke-IidsApi POST "/scenarios/$($scenario.id)/investment-items" @{
        category = 'WORKING_CAPITAL'
        name = 'Working Capital'
        amount = 20000
        yearNo = 1
        itemCode = 'WC'
        sortOrder = 2
    } | Out-Null
    Invoke-IidsApi POST "/scenarios/$($scenario.id)/financing-plans" @{
        sourceType = 'EQUITY'
        ratio = 1
        amount = 220000
        interestRate = 0
        termYears = 0
        repaymentMethod = 'EQUAL_PRINCIPAL'
        graceYears = 0
    } | Out-Null

    $run = Invoke-IidsApi POST "/scenarios/$($scenario.id)/calculation-tasks" @{
        taskType = 'FULL'
        requestKey = "demo-$($scenario.id)"
    }
    $taskId = $run.task.id
    $deadline = (Get-Date).AddSeconds(90)
    do {
        Start-Sleep -Milliseconds 500
        $task = Invoke-IidsApi GET "/calculation-tasks/$taskId" $null
        if ($task.status -eq 'FAILED') {
            throw "Calculation failed for scenario $($scenario.id): $($task.errorMessage)"
        }
    } while ($task.status -ne 'SUCCESS' -and (Get-Date) -lt $deadline)
    if ($task.status -ne 'SUCCESS') {
        throw "Calculation timed out for scenario $($scenario.id)"
    }

    return [pscustomobject]@{
        scenario = $scenario
        taskId = $taskId
    }
}

$baseline = New-DemoScenario 'Baseline Scenario' 140 40
$conservative = New-DemoScenario 'Conservative Scenario' 100 45
$report = Invoke-IidsApi POST "/calculation-tasks/$($baseline.taskId)/reports?format=EXCEL" $null

$summary = [pscustomobject]@{
    projectCode = $project.code
    projectId = $project.id
    baselineScenarioId = $baseline.scenario.id
    baselineTaskId = $baseline.taskId
    conservativeScenarioId = $conservative.scenario.id
    conservativeTaskId = $conservative.taskId
    reportId = $report.id
}

Write-Host 'Demo data initialized successfully:' -ForegroundColor Green
$summary | Format-List
