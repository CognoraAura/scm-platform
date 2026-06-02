$files = Get-ChildItem -Recurse -Filter "*.java" | Where-Object { !$_.FullName.Contains("\node_modules\") -and !$_.FullName.Contains("\.git\") }
$totalFixed = 0
$fixedFiles = 0
$replacementChar = @(0xEF, 0xBF, 0xBD)  # U+FFFD replacement character
foreach ($file in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    $changed = $false
    $i = 0
    while ($i -lt $bytes.Count - 2) {
        if ($bytes[$i] -ge 0xE0 -and $bytes[$i] -le 0xEF -and $bytes[$i+1] -ge 0x80 -and $bytes[$i+1] -le 0xBF -and $bytes[$i+2] -eq 0x3F) {
            $bytes[$i] = $replacementChar[0]
            $bytes[$i+1] = $replacementChar[1]
            $bytes[$i+2] = $replacementChar[2]
            $changed = $true
            $totalFixed++
            $i += 3
        } else {
            $i++
        }
    }
    if ($changed) {
        [System.IO.File]::WriteAllBytes($file.FullName, $bytes)
        $fixedFiles++
    }
}
Write-Host "Files fixed: $fixedFiles"
Write-Host "Corrupted sequences replaced: $totalFixed"
