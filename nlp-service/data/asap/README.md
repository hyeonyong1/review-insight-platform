# ASAP data directory

Run:

```bat
scripts\phase2_setup.bat
```

The script downloads the official Meituan-Dianping ASAP files here:

- train.csv
- dev.csv
- test.csv

The full dataset is intentionally ignored by Git and should not be committed to this repository.

ASAP contains 46,730 genuine Dianping restaurant reviews with manually annotated sentiment polarities for 18 aspect categories. Labels are 1 (positive), 0 (neutral), -1 (negative), -2 (not mentioned).

Official source: https://github.com/Meituan-Dianping/asap
