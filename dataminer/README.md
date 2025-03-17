The program can create a TLP dataset starting from any number of JIT datasets. It offers the following services:
- Measure TLP features
- Create a summary table of Ticket data
- Create JSON file to feed external programs in order to mine NLP4RE and T2T features.

To run the program you must have docker installed, the type in a console:

```sh
docker-compose up -d --build
```

In the `backend` directory sits the java code implementing the main logic of the services described above.