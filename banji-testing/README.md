# Banji Sule — Scanner Test Suite

## Overview
This folder contains a complete, independently built Maven test suite
for the GDMC terrain scanner module. It was developed by Banji Sule
as part of the COMP6030 Final Group Project at the University of Kent.

## Purpose
The Maven build in this folder was created to verify that the test
results achieved via the team's Gradle build (GDMCTesterBanji.java)
were accurate, reproducible, and genuinely connected to a live
Minecraft 1.21.11 world. Both builds produced identical outcomes,
confirming the correctness and robustness of the scanner logic.

## Build Tool
- **This folder:** Maven (pom.xml)
- **Team project:** Gradle (build.gradle)
- **Reason for Maven:** Used to cross-verify results and as a
  personal learning exercise in build tool configuration.

## Test Results
Both the Maven and Gradle builds were executed against a live
Minecraft 1.21.11 world via the GDMC HTTP Interface 1.8.4:

| Build Tool | Tests Run | Tests Passed | Tests Failed | Result |
|------------|-----------|--------------|--------------|--------|
| Gradle     | 58        | 58           | 0            | PASS   |
| Maven      | 17        | 17           | 0            | PASS   |

## Live World Evidence
The scanner successfully connected to Minecraft via localhost:9000,
read 4,096 real block columns from the champion plot, and confirmed:
- altMap values: Y=67 to Y=74 (real terrain heights)
- slopeMap[32][32] = 1 (genuine terrain variation detected)
- seaMap: water correctly identified at Y=62
- Gold block border placed in live Minecraft world at champion plot

## How to Run
1. Open Minecraft 1.21.11 with GDMC HTTP Interface 1.8.4 mod
2. Load a world and run: /buildarea set 0 0 0 256 255 256
3. Open this folder in IntelliJ IDEA
4. Run Main.java to see live scanner output
5. Run GdmcScannerTest.java to execute all 17 tests

## File Structure
src/main/java/com/gdmc/scanner/  — GdmcHttpClient, QuickScanner,
                                   DeepScanner, Main
src/main/java/com/gdmc/model/   — PlotCandidate, QuickScanResult,
                                   DeepScanResult
src/test/java/com/gdmc/         — GdmcScannerTest (17 tests)

## Author
Banji Sule | University of Kent | COMP6030
