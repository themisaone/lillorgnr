# Company Fetcher & Aqua Fetcher

Two small portable Java utilities that share the same `OrgNrs.txt` input, `config.properties`, and `config.accountinginfo`, but produce separate CSV files and merge into different Excel columns.

| JAR | Purpose | Default command | Excel merge target |
|-----|---------|-----------------|-------------------|
| **CompanyFetcher.jar** | Proff financials (Revenue, Salary, EBIT) | `java -jar CompanyFetcher.jar` | Columns E, G, I |
| **AquaFetcher.jar** | Akvakulturregisteret Kapasitet sum | `java -jar AquaFetcher.jar` | Column K |

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

This mirrors the manual flow on the first tab: search by organisasjonsnummer → open the legal entity → sum Kapasitet on matching tillatelser. Filters are configured in `config.accountinginfo` (default: Prod.stadium `matfisk` and Formal `kommersiell`). Non-matching licenses are skipped and leave an empty capacity in CSV/Excel.

### Excel merge (both JARs)

Both tools support `--merge-excel` with the same highlight options:

**CompanyFetcher** — match org.nr in column **B**, write:
- Revenue → **E**
- SalaryCost → **G**
- EBIT → **I**

**AquaFetcher** — match org.nr in column **B**, write:
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

Produces two runnable fat JARs:

```
target/CompanyFetcher.jar
target/AquaFetcher.jar
```

Run tests:

```bash
mvn test
```

## Project layout (runtime)

Place these files together in a working folder:

```
CompanyFetcher.jar
AquaFetcher.jar
OrgNrs.txt
config.properties
config.accountinginfo
RealPage.xlsx          # needed for Excel merge
```

## Configuration

Settings are loaded from `src/main/resources/application.properties` (defaults), overridden by **`config.properties`** in the working directory, plus **`config.accountinginfo`** (path set by `accounting.info.file`).

### config.properties

| Property | Description | Example |
|----------|-------------|---------|
| `input.file` | Input org.nr list | `OrgNrs.txt` |
| `output.file` | Proff CSV output | `CompanyFinancials.csv` |
| `aqua.output.file` | Aquaculture CSV output | `AquacultureCapacity.csv` |
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

## How to run

### CompanyFetcher — fetch Proff data

```bash
java -jar target/CompanyFetcher.jar
```

Output: `CompanyFinancials.csv`

### CompanyFetcher — merge into Excel

**Close the Excel file first.**

```bash
java -jar target/CompanyFetcher.jar --merge-excel --highlight-color=LIGHT_YELLOW
```

### AquaFetcher — fetch aquaculture capacity

```bash
java -jar target/AquaFetcher.jar
```

Output: `AquacultureCapacity.csv`

### AquaFetcher — merge into Excel (column K)

```bash
java -jar target/AquaFetcher.jar --merge-excel --highlight-color=LIGHT_BLUE
```

Use a **different highlight color on each run** so you can tell which cells were updated in that pass. Cells not updated keep their previous color.

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

Only tillatelser matching **both** `mtb.prod.stadium.allowed` and `mtb.formal.allowed` are summed. Others are skipped; `TotalCapacity` and `Unit` are left empty. On Excel merge, empty rows still get the highlight color (if requested) with the cell cleared.

## Architecture

```
OrgNrs.txt → CompanyFetcher.jar → CompanyFinancials.csv → Excel (E, G, I)
OrgNrs.txt → AquaFetcher.jar    → AquacultureCapacity.csv → Excel (K)
```

Both JARs share the same codebase (`no.companyfetcher` package) but have separate entry points (`Main` vs `AquaMain`).

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
