[CmdletBinding()]
param(
    [ValidateSet('Debug','Release','Both')][string]$Variant='Release',
    [Parameter(Mandatory=$true)][string]$DriveAppDirectory,
    [switch]$CreateConfirmedDrivePath
)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if(-not [IO.Path]::IsPathRooted($DriveAppDirectory)) {throw 'Drive 보관 경로는 로컬 절대 경로여야 합니다.'}
$drivePath=[IO.Path]::GetFullPath($DriveAppDirectory)
if(-not (Test-Path -LiteralPath $drivePath -PathType Container)) {
    if(-not $CreateConfirmedDrivePath) {throw "확인된 Drive 앱 폴더가 없습니다: $drivePath"}
    $driveRoot=[IO.Path]::GetPathRoot($drivePath)
    if(-not (Test-Path -LiteralPath $driveRoot -PathType Container)) {throw "Drive가 마운트되어 있지 않습니다: $driveRoot"}
    New-Item -ItemType Directory -Path $drivePath -Force | Out-Null
}
$resolvedDrive=(Get-Item -LiteralPath $drivePath).FullName
$variants=if($Variant -eq 'Both') {@('Debug','Release')} else {@($Variant)}
$receipts=[Collections.Generic.List[object]]::new()
foreach($buildVariant in $variants) {
    $outputs=Join-Path $projectRoot 'app\build\outputs\apk'
    $before=@{}
    if(Test-Path -LiteralPath $outputs) {
        foreach($apk in Get-ChildItem -LiteralPath $outputs -Recurse -Filter '*.apk') {
            $before[$apk.FullName]=@{Hash=(Get-FileHash -LiteralPath $apk.FullName -Algorithm SHA256).Hash;WriteTime=$apk.LastWriteTimeUtc.Ticks}
        }
    }
    $buildTime=[TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTime]::UtcNow,'Korea Standard Time').ToString('yyyyMMdd-HHmmss')
    Push-Location -LiteralPath $projectRoot
    try {
        & (Join-Path $projectRoot 'gradlew.bat') ":app:assemble$buildVariant" --offline --console=plain
        if($LASTEXITCODE -ne 0) {throw "$buildVariant APK 빌드 실패 ($LASTEXITCODE)"}
    } finally {Pop-Location}
    $fresh=@(Get-ChildItem -LiteralPath $outputs -Recurse -Filter '*.apk' | Where-Object {
        $old=$before[$_.FullName]
        (-not $old) -or $old.WriteTime -ne $_.LastWriteTimeUtc.Ticks -or $old.Hash -ne (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    })
    foreach($apk in $fresh) {
        $metadataPath=Join-Path $apk.DirectoryName 'output-metadata.json'
        if(-not (Test-Path -LiteralPath $metadataPath)) {throw "APK 메타데이터가 없습니다: $($apk.FullName)"}
        $metadata=Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
        $element=@($metadata.elements | Where-Object { $_.outputFile -eq $apk.Name })
        if($element.Count -ne 1) {throw "APK 버전 메타데이터가 명확하지 않습니다: $($apk.FullName)"}
        $variantName=$metadata.variantName -replace '[^A-Za-z0-9._-]','_'
        $versionName=[string]$element[0].versionName
        $versionCode=[string]$element[0].versionCode
        if(-not $versionName -or -not $versionCode) {throw "APK 버전 정보가 없습니다: $($apk.FullName)"}
        $filterLabel=(@($element[0].filters | ForEach-Object { "$($_.filterType)-$($_.value)" }) -join '-') -replace '[^A-Za-z0-9._-]','_'
        $suffix=if($filterLabel) {"-$filterLabel"} else {''}
        $safeVersion=$versionName -replace '[^A-Za-z0-9._-]','_'
        $fileName="OurList-$variantName-v$safeVersion-$versionCode-$buildTime$suffix.apk"
        $destination=Join-Path $resolvedDrive $fileName
        if(Test-Path -LiteralPath $destination) {throw "기존 APK를 덮어쓰지 않습니다: $destination"}
        Copy-Item -LiteralPath $apk.FullName -Destination $destination
        $sourceHash=(Get-FileHash -LiteralPath $apk.FullName -Algorithm SHA256).Hash
        $copyHash=(Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
        $copy=Get-Item -LiteralPath $destination
        if($apk.Length -ne $copy.Length -or $sourceHash -ne $copyHash) {throw "APK 복사 검증 실패: $destination"}
        $receipt=[pscustomobject]@{app='OurList';variant=$variantName;versionName=$versionName;versionCode=$versionCode;buildTimeKst=$buildTime;source=$apk.FullName;driveCopy=$destination;bytes=$apk.Length;sha256=$sourceHash;cloudSync='not_verified'}
        $receipts.Add($receipt)
        $receipt | ConvertTo-Json -Compress | Write-Output
    }
    if($fresh.Count -eq 0) {Write-Output "$buildVariant 새 APK 없음 (기존 산출물은 다시 복사하지 않음)"}
}
$receiptDirectory=Join-Path $projectRoot 'reports\releases'
New-Item -ItemType Directory -Path $receiptDirectory -Force | Out-Null
$receiptName='archive-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fff')+'.json'
ConvertTo-Json -InputObject @($receipts) -Depth 5 | Set-Content -LiteralPath (Join-Path $receiptDirectory $receiptName) -Encoding utf8
Write-Output '로컬 Drive 복사·크기·SHA-256 검증 완료. 실제 클라우드 동기화는 별도 확인이 필요합니다.'
