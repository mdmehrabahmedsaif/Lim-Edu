$WshShell = New-Object -ComObject WScript.Shell
$DesktopPath = [System.Environment]::GetFolderPath('Desktop')
$ShortcutPath = Join-Path $DesktopPath "Lim Edu.lnk"

$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path
$TargetPath = Join-Path $ProjectRoot "Lim-Edu.vbs"
$IconPath = Join-Path $ProjectRoot "assets\icon.ico"

$Shortcut = $WshShell.CreateShortcut($ShortcutPath)
$Shortcut.TargetPath = $TargetPath
$Shortcut.WorkingDirectory = $ProjectRoot
$Shortcut.IconLocation = "$IconPath, 0"
$Shortcut.Description = "Lim Edu - Secure Restricted Educational Workspace"
$Shortcut.Save()

Write-Host "Desktop shortcut created successfully at: $ShortcutPath"
