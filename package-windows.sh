#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
DEPLOY_DIR="$ROOT/windows-package"

echo "Building JARs..."
(cd "$ROOT" && mvn -q package)

echo "Creating $DEPLOY_DIR ..."
rm -rf "$DEPLOY_DIR"
mkdir -p "$DEPLOY_DIR"

cp "$ROOT/target/CompanyFetcher.jar" "$DEPLOY_DIR/"
cp "$ROOT/target/AquaFetcher.jar" "$DEPLOY_DIR/"
cp "$ROOT/target/MtbCalc.jar" "$DEPLOY_DIR/"
cp "$ROOT/target/OrgNrGui.jar" "$DEPLOY_DIR/"

cp "$ROOT/config.properties" "$DEPLOY_DIR/"
cp "$ROOT/config.accountinginfo" "$DEPLOY_DIR/"
cp "$ROOT/config.mtbstages" "$DEPLOY_DIR/"

cat > "$DEPLOY_DIR/OrgNrs.txt" <<'EOF'
# Én org.nr per linje (tomme linjer og linjer som starter med # ignoreres)
# Eksempel:
# 994613405
EOF

cat > "$DEPLOY_DIR/MtbInput.txt" <<'EOF'
# Navn,MTB (tomme linjer og linjer som starter med # ignoreres)
# Eksempel:
# ARNØY LAKS,4250
EOF

cat > "$DEPLOY_DIR/StartGui.bat" <<'EOF'
@echo off
cd /d "%~dp0"
echo Starter Org.nr verktoy...
java -jar OrgNrGui.jar
if errorlevel 1 (
    echo.
    echo Feil ved oppstart. Er Java 21 installert?
    echo Last ned fra: https://adoptium.net/
)
pause
EOF

cat > "$DEPLOY_DIR/LESMEG.txt" <<'EOF'
Org.nr verktoy - Windows
========================

Forutsetning:
  Java 21 må være installert (https://adoptium.net/)

Start:
  Dobbeltklikk StartGui.bat

Filer du redigerer:
  OrgNrs.txt      - org.nr for Proff og Akvakultur (Rediger inndatafil i GUI)
  MtbInput.txt    - navn,MTB for gebyrberegning

Excel:
  Legg din egen Excel-fil i denne mappen.
  Sett filnavnet i config.properties under excel.file=
  Org.nr må stå i kolonne B.

Viktig:
  Lukk Excel-filen før du slår sammen data fra GUI.

Output (opprettes automatisk):
  CompanyFinancials.csv
  AquacultureCapacity.csv
  MtbCalc.csv
EOF

echo ""
echo "Done. Transfer this folder to Windows:"
echo "  $DEPLOY_DIR"
echo ""
ls -la "$DEPLOY_DIR"
