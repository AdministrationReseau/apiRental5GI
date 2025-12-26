# ============================================
# Génération du contexte projet Spring Boot
# Fleet Management - WebFlux
# ============================================

$OUTPUT_FILE = "project_context.txt"
$PROJECT_ROOT = "."

Write-Host "Génération du contexte du projet..."

# Nettoyer l'ancien fichier
if (Test-Path $OUTPUT_FILE) {
    Remove-Item $OUTPUT_FILE
}

# =========================
# En-tête
# =========================
@"
# Contexte du Projet : Fleet & Geofence API
Généré le : $(Get-Date)
Framework : Spring Boot WebFlux (Reactive)

"@ | Out-File $OUTPUT_FILE -Encoding UTF8

# =========================
# 1. Arborescence
# =========================
@"
## 1. Arborescence du Projet

"@ | Out-File $OUTPUT_FILE -Append -Encoding UTF8

$excludePattern = 'target|build|.git|.idea|.vscode|.mvn|gradle|bin'

tree /F |
ForEach-Object {
if ($_ -notmatch $excludePattern) {
$_ | Out-File $OUTPUT_FILE -Append -Encoding UTF8
}
}

@"


"@ | Out-File $OUTPUT_FILE -Append -Encoding UTF8

"## 2. Contenu des Fichiers`n" | Out-File $OUTPUT_FILE -Append -Encoding UTF8

Get-ChildItem -Recurse -File |
Where-Object {
    $_.Extension -in ".java",".yml",".properties",".sql",".md" -or $_.Name -in "pom.xml","build.gradle"
} |
Where-Object {
    $_.FullName -notmatch 'target|build|\.git|\.idea|\.vscode'
} |
ForEach-Object {

    $file = $_.FullName
    $ext = $_.Extension.TrimStart('.')
    if ($ext -eq "yml") { $ext = "yaml" }

@"
---
### Fichier : $file
```$ext
"@ | Out-File $OUTPUT_FILE -Append -Encoding UTF8

    Get-Content $file | Out-File $OUTPUT_FILE -Append -Encoding UTF8

@"
"@ | Out-File $OUTPUT_FILE -Append -Encoding UTF8
}

"✅ Contexte généré : $OUTPUT_FILE" | Write-Host