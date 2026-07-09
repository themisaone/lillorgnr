# Company Fetcher

A small portable Java utility that retrieves financial information for Norwegian companies based on organization numbers, writes the results to CSV, and optionally merges them into an Excel workbook.

Designed to be run a few times per year. The architecture supports swapping the data source from web scraping (Proff.no) to the official Proff API without changing the rest of the application.

## What it does

### Phase 1 — Fetch financial data

Reads a list of organization numbers, fetches company financials for a configured accounting year, and writes a CSV file.

For each company it retrieves:

| Field | Description |
|-------|-------------|
| Revenue | Sum driftsinntekter |
| SalaryCost | Lønnskostnader |
| EBIT | Driftsresultat (EBIT) |

All amounts are stored in **NOK** (not thousands). Example: Proff shows `248 204` → stored as `248204000`.

Failed companies are written to the CSV with a status like `FAILED: NOT_FOUND`; the batch continues without stopping.

### Phase 2 — Merge CSV into Excel

Reads the generated CSV and updates an existing Excel file:

- Match org.nr from CSV against **column B** in the workbook
- Write **Revenue** → column **E**
- Write **SalaryCost** → column **G**
- Write **EBIT** → column **I**

Updated cells can be highlighted with a **run-specific color** so you can see which values were refreshed in the latest pass.

## Current state

| Area | Status |
|------|--------|
| Maven project, Java 21 | Done |
| Text input (`OrgNrs.txt`) | Done |
| CSV output | Done |
| Proff web provider (`PROFF_WEB`) | Done |
| Proff API provider stub (`PROFF_API`) | Done (requires API key) |
| Dummy provider for local testing | Done |
| Retry, logging, configuration | Done |
| Excel merge with cell highlighting | Done |
| Excel input (`OrgNrs.xlsx`) | Not implemented |
| Windows `.exe` packaging (`jpackage`) | Not implemented |

## Requirements

- **Java 21** (LTS)
- **Maven 3.x** (for building)
- Network access (when using `PROFF_WEB` or `PROFF_API`)

## Build

```bash
mvn clean package
```

This produces a runnable fat JAR:

```
target/CompanyFetcher.jar
```

Run tests:

```bash
mvn test
```

## Project layout (runtime)

Place these files together in a working folder:

```
CompanyFetcher.jar
OrgNrs.txt
config.properties
RealPage.xlsx          # only needed for Excel merge
```

Example `OrgNrs.txt`:

```
994613405
895366722
975862801
```

Lines starting with `#` and blank lines are ignored.

## Configuration

Settings are loaded from `src/main/resources/application.properties` (defaults) and overridden by **`config.properties`** in the working directory.

| Property | Description | Example |
|----------|-------------|---------|
| `accounting.year` | Regnskap year to fetch | `2024` |
| `input.file` | Input org.nr list | `OrgNrs.txt` |
| `output.file` | CSV output path | `CompanyFinancials.csv` |
| `excel.file` | Excel workbook for merge | `RealPage.xlsx` |
| `provider` | Data source | `PROFF_WEB`, `PROFF_API`, `DUMMY` |
| `request.delay.ms` | Delay between company lookups | `1500` |
| `retry.count` | Retries on network errors | `3` |

For the Proff API (future):

```bash
export PROFF_API_KEY=your-token-here
```

Set `provider=PROFF_API` in `config.properties`.

## How to run

### Step 1 — Fetch data to CSV

```bash
java -jar target/CompanyFetcher.jar
```

Example output:

```
Reading 3 companies
██████████ 100%

Completed:
3 OK
0 FAILED

Output:
CompanyFinancials.csv
```

Logs are written to `logs/company-fetcher.log`.

### Step 2 — Merge CSV into Excel

**Close the Excel file first** (LibreOffice/Excel lock prevents writes).

```bash
java -jar target/CompanyFetcher.jar --merge-excel --highlight-color=LIGHT_YELLOW
```

On the next run, use a **different color** so you can see which cells were updated in that pass:

```bash
java -jar target/CompanyFetcher.jar --merge-excel --highlight-color=LIGHT_GREEN
```

Cells updated in the latest run get the new color. Cells not updated (e.g. org.nr missing from CSV) keep their previous color — making stale data visible.

#### Highlight colors

Named colors:

- `LIGHT_YELLOW`
- `LIGHT_GREEN`
- `LIGHT_BLUE`
- `LIGHT_ORANGE`
- `CORAL`

Or hex: `--highlight-color=#FFF2CC`

`--highlight-color` is **required** for `--merge-excel` unless you opt out with `--no-highlight`.

Update values without changing cell colors:

```bash
java -jar target/CompanyFetcher.jar --merge-excel --no-highlight
```

## CSV output format

```csv
OrgNr,OrgName,AccountingYear,Revenue,SalaryCost,EBIT,Status
994613405,Arnøy Laks AS,2024,248204000,25194000,15303000,OK
```

Status values:

| Status | Meaning |
|--------|---------|
| `OK` | All three financial fields retrieved |
| `PARTIAL` | Some fields missing (empty in CSV) |
| `FAILED: …` | Lookup failed (e.g. `NOT_FOUND`, `NETWORK`) |

## Architecture

The application never knows whether data comes from scraping or the API. Everything goes through a single provider interface:

```
OrgNrs.txt → CompanyService → CompanyProvider → CompanyData → CsvExporter → CSV
                                    ↓
                          ProffWebProvider (today)
                          ProffApiProvider (future)
```

Excel merge is a separate step:

```
CompanyFinancials.csv → ExcelMergeService → RealPage.xlsx
```

## Things to be aware of

### Proff web scraping

- `PROFF_WEB` resolves companies via Proff search and the Brønnøysund register (for URL fallback).
- Proff may block automated requests in some environments. If scraping fails, consider `PROFF_API` once you have a key.
- A delay between requests (`request.delay.ms`) reduces the risk of rate limiting.

### Excel merge

- Only **individual cells** written by the tool are highlighted — not whole columns.
- **Close** `RealPage.xlsx` before running `--merge-excel`.
- Use a **new highlight color on each run** to distinguish fresh updates from stale cells.
- Rows with an org.nr in column B that are **not** in the CSV are left unchanged (including their cell colors).

### Legal / terms of use

Proff.no restricts systematic automated data collection. This utility is intended for occasional internal use. When available, the official **Proff API** is the preferred long-term data source.

### Amounts

Proff displays amounts in **thousands of NOK**. The utility converts them to full NOK in CSV and Excel.

## Development

Main package: `no.companyfetcher`

```
src/main/java/no/companyfetcher/
├── Main.java
├── model/CompanyData.java
├── input/          TextFileReader, CsvCompanyReader
├── output/         CsvExporter, ExcelUpdater
├── provider/       ProffWebProvider, ProffApiProvider, DummyProvider
├── parser/         ProffParser
├── service/        CompanyService, ExcelMergeService
├── config/         Configuration
└── cli/            CommandLineArgs
```

Use `provider=DUMMY` in `config.properties` for offline end-to-end testing without network access.

## Planned extensions

- Excel input (`OrgNrs.xlsx`)
- Standalone Windows executable via `jpackage`
- Full Proff API integration when `PROFF_API_KEY` is available
