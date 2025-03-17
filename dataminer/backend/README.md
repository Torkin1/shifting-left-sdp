In the `src` directory sits the java code implementing the mining logic. The relevant packages and classes are:
- `it.torkin.dataminer.control.features.miners`: implementation of TLP features miners;
- `it.torkin.dataminer.control.dataset.processed.filters.impl`: Implementations of filters used to discard anomalous tickets;
- `it.torkin.dataminer.control.measurementdate.impl`: Implementation of measurement date values calculation;
- `it.torkin.dataminer.control.issue.IssueController`: Class implementing methods designed to access Issues data according to a measurement date;
- `it.torkin.dataminer.control.dataset`: Classes that extracts the corresponding tickets details and changelogs from one or more JIT datasets;
- `it.torkin.dataminer.config`: You can check what runtime settings are available and an explanation of them in this package;
- `it.torkin.dataminer.bootstrap.ApplicationStartup`: Implements the main script resembling an high-level view of the TLP dataset creation. 

Besides, you could want to peek at the following resources:

- `pom.xml`: dependencies and version numbers;
- `src/main/resources/Dockerfile.backend`: Additional dependencies and build process details;
- `*.properties`: runtime settings values, which can differ according to the target (default one, `test` or `container`);
- `src/main/resources/buggy-similarity`: T2T features values;
- `src/main/resources/nlp4re`: NLP4RE features values;
- `scripts`: python scripts used to further processing the TLP dataset produced by the program.

The program creates a `data` dir where it stores the files it needs. If this program runs on a container, it is recommended to mount this dir as a volume in order to be accessible from outside.

Filtered tickets keys are cached on files in the `data/processed-issues`. If you want to re-trigger filtering of tickets, you must delete the directory first.

The TLP dataset is produced in the `data/output/measurements` directory.