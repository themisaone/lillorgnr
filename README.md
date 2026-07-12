# Org Nr Report Tool

One fat JAR for everything: **GUI** (no arguments) or **command line** via `--command=FETCH|PROF|AQUA`.

For each org.nr in `OrgNrs.txt`, the tool fetches Proff financials, aquaculture capacity, and calculates MTB **Fee** from TotalCapacity. Everything is stored in a single CSV.

| Mode | Command |
|------|---------|
| **GUI** (Windows-friendly) | `java -jar OrgNrGui.jar` or double-click `StartGui.bat` |
| **Fetch all data** | `java -jar OrgNrGui.jar --command=FETCH` |
| **Excel merge (all)** | GUI: one button — columns E, G, I, K, M |
| **Proff Excel merge** | `java -jar OrgNrGui.jar --command=PROF --merge-excel --highlight-color=LIGHT_YELLOW` |
| **Aqua Excel merge** | `java -jar OrgNrGui.jar --command=AQUA --merge-excel --highlight-color=LIGHT_BLUE` |
| **Fee Excel merge** | `java -jar OrgNrGui.jar --command=MTB --merge-excel --highlight-color=LIGHT_GREEN` |

Designed to be run a few times per year.

## What it does

For every line in `OrgNrs.txt`:

1. **Proff** — fetch Revenue, SalaryCost, EBIT for the configured accounting year
2. **Akvakultur** — fetch TotalCapacity from [Akvakulturregisteret](https://sikker.fiskeridir.no/akvakulturregisteret/web/legalEntities) (filtered by `config.accountinginfo`)
3. **MTB Fee** — calculate tiered fee from TotalCapacity using `config.mtbstages`

All results are written to one file: `OrgNrReport.csv`.

### Excel merge (PROF & AQUA commands)

Both merge commands read from the same `OrgNrReport.csv`:

**PROF** — match org.nr in column **B**, write:
- Revenue → **E**
- SalaryCost → **G**
- EBIT → **I**

**AQUA** — match org.nr in column **B**, write:
- TotalCapacity → **K**

**Fee** — match org.nr in column **B**, write:
- Fee → **M**

**GUI merge (all at once)** — writes E, G, I, K, and M in one pass.

Updated cells can be highlighted with a **run-specific color** so you can see which values were refreshed in the latest pass.

## Requirements

- **Java 21** (LTS)
- **Maven 3.x** (for building)
- Network access

## Build

```bash
mvn clean package
```

Produces one runnable fat JAR:

```
target/OrgNrGui.jar
```

Run tests:

```bash
mvn test
```

## Project layout (runtime)

Place these files together in a working folder:

```
OrgNrGui.jar
StartGui.bat           # Windows: double-click to open GUI
OrgNrs.txt
config.properties
config.accountinginfo
config.mtbstages
RealPage.xlsx          # needed for Excel merge
```

## Configuration

Settings are loaded from **`config.properties`** in the working directory, plus **`config.accountinginfo`** (path set by `accounting.info.file`).

### config.properties

| Property | Description | Example |
|----------|-------------|---------|
| `proffaqua.input.file` | Org.nr input list | `OrgNrs.txt` |
| `output.file` | Combined CSV output | `OrgNrReport.csv` |
| `excel.file` | Excel workbook for merge | `RealPage.xlsx` |
| `accounting.info.file` | Accounting/MTB filter file | `config.accountinginfo` |
| `provider` | Proff data source | `PROFF_WEB`, `PROFF_API`, `DUMMY` |
| `request.delay.ms` | Delay between org numbers | `1500` |
| `retry.count` | Retries on network errors | `3` |

### config.accountinginfo

| Property | Description | Example |
|----------|-------------|---------|
| `accounting.year` | Regnskap year (Proff) | `2024` |
| `mtb.prod.stadium.allowed` | Allowed Prod.stadium values (comma-separated) | `matfisk` |
| `mtb.formal.allowed` | Allowed Formal values (comma-separated) | `kommersiell` |
| `mtb.stages.file` | Tiered fee staircase CSV | `config.mtbstages` |

## GUI for Windows (non-technical users)

Copy the working folder to Windows (must include `config.properties`, `config.accountinginfo`, `config.mtbstages`, `OrgNrs.txt`, and `OrgNrGui.jar`).

**Option 1:** Double-click `StartGui.bat`  
**Option 2:** `java -jar OrgNrGui.jar` (requires Java 21 installed)

The GUI provides:

- **Mode selector:** **Bare Proff** (Proff only) or **Proff og MTB** (Proff + Aqua + Fee)
- **Rediger OrgNrs.txt** / **Vis resultat-CSV**
- **Hent data** — fetches according to selected mode
- **Slå sammen til Excel** — merges E,G,I only (Bare Proff) or E,G,I,K,M (Proff og MTB)

Highlight colors: `LIGHT_GREEN`, `LIGHT_YELLOW`, `LIGHT_BLUE`, `LIGHT_ORANGE`

Close Excel before clicking any merge button (the GUI will remind you).

## How to run (command line)

### Fetch all data

```bash
java -jar target/OrgNrGui.jar --command=FETCH
```

Output: `OrgNrReport.csv`

### Proff — merge into Excel

**Close the Excel file first.**

```bash
java -jar target/OrgNrGui.jar --command=PROF --merge-excel --highlight-color=LIGHT_YELLOW
```

### Fee — merge into Excel (column M)

```bash
java -jar target/OrgNrGui.jar --command=MTB --merge-excel --highlight-color=LIGHT_GREEN
```

### Aqua — merge into Excel (column K)

```bash
java -jar target/OrgNrGui.jar --command=AQUA --merge-excel --highlight-color=LIGHT_BLUE
```

Use a **different highlight color on each Excel merge run** so you can tell which cells were updated in that pass. Cells not updated keep their previous color.

#### Highlight colors

- `LIGHT_YELLOW`, `LIGHT_GREEN`, `LIGHT_BLUE`, `LIGHT_ORANGE`, `CORAL`
- Or hex: `--highlight-color=#FFF2CC`
- `--highlight-color` is **required** unless you use `--no-highlight`

## CSV format

**OrgNrReport.csv**

```csv
OrgNr,OrgName,AccountingYear,Revenue,SalaryCost,EBIT,ProffStatus,TotalCapacity,Unit,EntryCount,AquaStatus,Fee
994613405,Arnøy Laks AS,2024,248204000,25194000,15303000,OK,4250,TN,4,OK,72520
975862801,Elvevoll Settefisk AS,,,,,FAILED: NOT_FOUND,,,0,OK: NO_MATCHING_MTB,0
```

- Proff amounts are in **NOK** (not thousands). Example: Proff shows `248 204` → stored as `248204000`.
- `Fee` is calculated from `TotalCapacity` using tiered prices in `config.mtbstages`.

## Architecture

```
OrgNrs.txt → FETCH → OrgNrReport.csv
                      ├─ GUI merge → Excel (E, G, I, K, M)
                      ├─ PROF merge → Excel (E, G, I)
                      ├─ AQUA merge → Excel (K)
                      └─ MTB merge → Excel (M)
```

## Things to be aware of

- **Close** the Excel file before any `--merge-excel` run.
- Only **individual cells** written by the tool are highlighted.
- Proff web scraping may be blocked in some environments; use `PROFF_API` when you have a key.
- Aqua MTB filters are configured in `config.accountinginfo` (`mtb.prod.stadium.allowed`, `mtb.formal.allowed`).

## Development

Use `provider=DUMMY` in `config.properties` for offline Proff testing.

## Planned extensions

- Excel input (`OrgNrs.xlsx`)
- Windows `.exe` packaging via `jpackage`
- Full Proff API integration
