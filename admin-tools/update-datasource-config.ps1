[CmdletBinding()]
param(
    [string]$BootstrapFile,
    [string]$JdbcUrl,
    [string]$Username,
    [switch]$SkipUrl,
    [switch]$SkipUsername,
    [switch]$AllowEmptyPassword
)

$ErrorActionPreference = 'Stop'

function Resolve-BootstrapFile {
    param([string]$ExplicitPath)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        return [System.IO.Path]::GetFullPath($ExplicitPath)
    }

    $locator = Join-Path $HOME '.m2m-xml-editor/bootstrap-location.properties'
    if (Test-Path -LiteralPath $locator -PathType Leaf) {
        foreach ($line in [System.IO.File]::ReadAllLines($locator, [System.Text.Encoding]::UTF8)) {
            if ($line -match '^\s*bootstrap\.file\s*=\s*(.+?)\s*$') {
                $candidate = $Matches[1].Trim()
                if (-not [string]::IsNullOrWhiteSpace($candidate)) {
                    return [System.IO.Path]::GetFullPath($candidate)
                }
            }
        }
    }

    $programData = [Environment]::GetFolderPath('CommonApplicationData')
    if (-not [string]::IsNullOrWhiteSpace($programData)) {
        return Join-Path $programData 'M2M-XML-EDITOR\config\application-bootstrap.properties'
    }

    throw 'A bootstrap konfiguráció helye nem állapítható meg. Add meg a -BootstrapFile paramétert.'
}

function ConvertFrom-SecureStringToPlainText {
    param([Security.SecureString]$SecureValue)
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Escape-JavaPropertyValue {
    param([string]$Value)
    if ($null -eq $Value) { return '' }
    return $Value.Replace('\', '\\').Replace("`t", '\t').Replace("`r", '\r').Replace("`n", '\n')
}

function Set-PropertyValue {
    param(
        [System.Collections.Generic.List[string]]$Lines,
        [string]$Key,
        [string]$Value
    )

    $escaped = Escape-JavaPropertyValue $Value
    $replacement = "$Key=$escaped"
    $pattern = '^\s*' + [regex]::Escape($Key) + '\s*[:=]'
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i] -match $pattern) {
            $Lines[$i] = $replacement
            return
        }
    }
    $Lines.Add($replacement)
}

$target = Resolve-BootstrapFile $BootstrapFile
if (-not (Test-Path -LiteralPath $target -PathType Leaf)) {
    throw "A bootstrap konfiguráció nem található: $target"
}

Write-Host "Bootstrap konfiguráció: $target"
Write-Host 'Ez az eszköz csak a kapcsolódási konfigurációt módosítja; az adatbázis-felhasználó jelszavát NEM változtatja meg.'

if (-not $SkipUrl -and [string]::IsNullOrWhiteSpace($JdbcUrl)) {
    $JdbcUrl = Read-Host 'Új JDBC URL (Enter = maradjon változatlan)'
}
if (-not $SkipUsername -and [string]::IsNullOrWhiteSpace($Username)) {
    $Username = Read-Host 'Új adatbázis-felhasználó (Enter = maradjon változatlan)'
}
$passwordSecure = Read-Host 'Új adatbázis-jelszó' -AsSecureString
$password = ConvertFrom-SecureStringToPlainText $passwordSecure
try {
    if ([string]::IsNullOrEmpty($password) -and -not $AllowEmptyPassword) {
        $confirmation = Read-Host 'Üres adatbázis-jelszót adtál meg. Biztosan ezt szeretnéd? Írd be: IGEN'
        if ($confirmation -cne 'IGEN') {
            throw 'A datasource konfiguráció módosítása megszakítva.'
        }
    }

    $lines = [System.Collections.Generic.List[string]]::new()
    foreach ($line in [System.IO.File]::ReadAllLines($target, [System.Text.Encoding]::UTF8)) {
        $lines.Add($line)
    }

    if (-not $SkipUrl -and -not [string]::IsNullOrWhiteSpace($JdbcUrl)) {
        Set-PropertyValue $lines 'spring.datasource.url' $JdbcUrl.Trim()
    }
    if (-not $SkipUsername -and -not [string]::IsNullOrWhiteSpace($Username)) {
        Set-PropertyValue $lines 'spring.datasource.username' $Username.Trim()
    }
    Set-PropertyValue $lines 'spring.datasource.password' $password

    $backup = "$target.bak"
    Copy-Item -LiteralPath $target -Destination $backup -Force

    $temp = "$target.tmp"
    [System.IO.File]::WriteAllLines($temp, $lines, (New-Object System.Text.UTF8Encoding($false)))
    Move-Item -LiteralPath $temp -Destination $target -Force

    Write-Host 'A datasource bootstrap konfiguráció frissítve.'
    Write-Host "Biztonsági mentés: $backup"
    Write-Host 'Indítsd újra az alkalmazást. Ha a kapcsolat továbbra sem működik, az adatbázis oldali hitelesítést az üzemeltetővel kell ellenőrizni.'
}
finally {
    $password = $null
}
