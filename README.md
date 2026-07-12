# Company Fetcher, Aqua Fetcher, MTB Calc & GUI

One fat JAR for everything: **GUI** (no arguments) or **command line** via `--command=PROF|AQUA|MTBCALC`.

| Mode | Command |
|------|---------|
| **GUI** (Windows-friendly) | `java -jar OrgNrGui.jar` or double-click `StartGui.bat` |
| **Proff fetch** | `java -jar OrgNrGui.jar --command=PROF` |
| **Proff Excel merge** | `java -jar OrgNrGui.jar --command=PROF --merge-excel --highlight-color=LIGHT_YELLOW` |
| **Aqua fetch** | `java -jar OrgNrGui.jar --command=AQUA` |
| **Aqua Excel merge** | `java -jar OrgNrGui.jar --command=AQUA --merge-excel --highlight-color=LIGHT_BLUE` |
| **MTB calc** | `java -jar OrgNrGui.jar --command=MTBCALC` |

Designed to be run a few times per year.

## What they do

### CompanyFetcher — Proff financial data

Reads organization numbers, fetches company financials for a configured accounting year, and writes `CompanyFinancials.csv`.

| Field | Description |
|-------|-------------|
| Revenue | Sum driftsinntekter |
| SalaryCost | Lønnskostnader |
| EBIT | Driftsresultat (EBIT) |

All amounts are stored in **NOK** (not thousands). Example: Proff shows `248 204` → stored as `248204000`.

### AquaFetcher — Aquaculture capacity

Reads the same `OrgNrs.txt` and writes `AquacultureCapacity.csv` with the **sum of Kapasitet** per company from [Akvakulturregisteret](https://sikker.fiskeridir.no/akvakulturregisteret/web/legalEntities).

This mirrors the manual flow on the first tab: search by organisasjonsnummer → open the legal entity → sum Kapasitet on matching tillatelser. Filters are configured in `config.accountinginfo` (default: Prod.stadium `matfisk` and Formal `kommersiell`). Output: `AquacultureCapacity.csv` (unchanged format — no fee fields).

### MtbCalc — MTB fee calculation

Reads `MtbInput.txt` (konsern name + MTB number per line) and writes `MtbCalc.csv` with tiered **Fee** plus **MedlCont** and **ServAvgift** (half of values from `config.accountinginfo`).

### Excel merge (PROF & AQUA commands)

Both commands support `--merge-excel` with the same highlight options:

**PROF** — match org.nr in column **B**, write:
- Revenue → **E**
- SalaryCost → **G**
- EBIT → **I**

**AQUA** — match org.nr in column **B**, write:
- TotalCapacity → **K**

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
MtbInput.txt
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
| `proffaqua.input.file` | Shared org.nr input (Proff + Aqua) | `OrgNrs.txt` |
| `mtb.input.file` | MTB calc input (name,mtb per line) | `MtbInput.txt` |
| `proff.output.file` | Proff CSV output | `CompanyFinancials.csv` |
| `aqua.output.file` | Aquaculture CSV output | `AquacultureCapacity.csv` |
| `mtb.output.file` | MTB calc CSV output | `MtbCalc.csv` |
| `excel.file` | Excel workbook for merge | `RealPage.xlsx` |
| `accounting.info.file` | Accounting/MTB filter file | `config.accountinginfo` |
| `provider` | Proff data source | `PROFF_WEB`, `PROFF_API`, `DUMMY` |
| `request.delay.ms` | Delay between lookups | `1500` |
| `retry.count` | Retries on network errors | `3` |

### config.accountinginfo

| Property | Description | Example |
|----------|-------------|---------|
| `accounting.year` | Regnskap year (CompanyFetcher) | `2024` |
| `mtb.prod.stadium.allowed` | Allowed Prod.stadium values (comma-separated) | `matfisk` |
| `mtb.formal.allowed` | Allowed Formal values (comma-separated) | `kommersiell` |
| `mtb.stages.file` | Tiered fee staircase CSV | `config.mtbstages` |
| `medl.kontigent` | Full membership fee (CSV gets half as `MedlCont`) | `14200` |
| `serv.avgift` | Full service fee (CSV gets half as `ServAvgift`) | `12200` |

## GUI for Windows (non-technical users)

Copy the working folder to Windows (must include `config.properties`, `config.accountinginfo`, `config.mtbstages`, input files, and `OrgNrGui.jar`).

**Option 1:** Double-click `StartGui.bat`  
**Option 2:** `java -jar OrgNrGui.jar` (requires Java 21 installed)

The GUI provides:

- **View input file** / **View result CSV** buttons for each section (opens in Excel/Notepad on Windows)
- **Proff:** Fetch button + Merge to Excel with color dropdown
- **Aqua:** Fetch button + Merge to Excel (column K) with color dropdown
- **MTB:** Calculate fees button

Highlight colors: `LIGHT_GREEN`, `LIGHT_YELLOW`, `LIGHT_BLUE`, `LIGHT_ORANGE`

Close Excel before clicking any merge button (the GUI will remind you).

## How to run (command line)

### Proff — fetch data

```bash
java -jar target/OrgNrGui.jar --command=PROF
```

Output: `CompanyFinancials.csv`

### Proff — merge into Excel

**Close the Excel file first.**

```bash
java -jar target/OrgNrGui.jar --command=PROF --merge-excel --highlight-color=LIGHT_YELLOW
```

### Aqua — fetch aquaculture capacity

```bash
java -jar target/OrgNrGui.jar --command=AQUA
```

Output: `AquacultureCapacity.csv`

### Aqua — merge into Excel (column K)

```bash
java -jar target/OrgNrGui.jar --command=AQUA --merge-excel --highlight-color=LIGHT_BLUE
```

### MTB — calculate fees

```bash
java -jar target/OrgNrGui.jar --command=MTBCALC
```

Input `MtbInput.txt` (one konsern per line):

```
ARNØY LAKS,4250
ELVEVOLL SETTEFISK,
```

Output: `MtbCalc.csv`

Use a **different highlight color on each Excel merge run** so you can tell which cells were updated in that pass. Cells not updated keep their previous color.

#### Highlight colors

- `LIGHT_YELLOW`, `LIGHT_GREEN`, `LIGHT_BLUE`, `LIGHT_ORANGE`, `CORAL`
- Or hex: `--highlight-color=#FFF2CC`
- `--highlight-color` is **required** unless you use `--no-highlight`

## CSV formats

**CompanyFinancials.csv**

```csv
OrgNr,OrgName,AccountingYear,Revenue,SalaryCost,EBIT,Status
994613405,Arnøy Laks AS,2024,248204000,25194000,15303000,OK
```

**AquacultureCapacity.csv**

```csv
OrgNr,OrgName,TotalCapacity,Unit,EntryCount,Status
994613405,ARNØY LAKS AS,4250,TN,4,OK
975862801,ELVEVOLL SETTEFISK AS,,,0,OK: NO_MATCHING_MTB
```

**MtbCalc.csv**

```csv
Name,MTB,Fee,MedlCont,ServAvgift
ARNØY LAKS,4250,72520,7100,6100
ELVEVOLL SETTEFISK,,,7100,6100
```

`Fee` uses the tiered prices in `config.mtbstages`. `MedlCont` and `ServAvgift` are half of `medl.kontigent` and `serv.avgift`.

## Architecture

```
OrgNrs.txt   → --command=PROF → CompanyFinancials.csv → Excel (E, G, I)
OrgNrs.txt   → --command=AQUA → AquacultureCapacity.csv → Excel (K)
MtbInput.txt → --command=MTBCALC → MtbCalc.csv
```

All three JARs share the same codebase (`no.companyfetcher` package) with separate entry points.

## Things to be aware of

- **Close** `RealPage.xlsx` before any `--merge-excel` run.
- Only **individual cells** written by the tool are highlighted.
- Proff web scraping may be blocked in some environments; use `PROFF_API` when you have a key.
- AquaFetcher MTB filters are configured in `config.accountinginfo` (`mtb.prod.stadium.allowed`, `mtb.formal.allowed`).

## Development

Use `provider=DUMMY` in `config.properties` for offline CompanyFetcher testing.

## Planned extensions

- Excel input (`OrgNrs.xlsx`)
- Windows `.exe` packaging via `jpackage`
- Full Proff API integration
