# ApacheJIT: A Large Dataset for Just-In-Time Defect Prediction

> [!NOTE]
> Only the relevant parts of the dataset are found here. The full dataset along with the sources to generate it can be retrieved at the following [doi](https://dl.acm.org/doi/10.1145/3524842.3527996).

## Inconsistencies

> [!CAUTION]
> To solve the inconsistencies some files in the dataset could have been modified, so read this section if you want to know the possible differences with the original apachejit dataset.

Inconsistencies found:

- `apachejit_total` records refer to projects using a string in the format `apache/long-project-name`, while in the `data` folder projects are referred using the format `NAME`;
- `apachejit_total` mentions a project named `apache-hadoop`, but project data is not present in the `data` folder;
- In the folder `data` there is a project file named `MESOS` which is not present in `apachejit_total` file.
- Issues in data folder refer to bugs and corresponding fixing commit. We do not have issue keys linked to bug inducing commits (they must be mined in commit comment)

Citing the apachejit paper:
> After collecting fixing commits as described above for all 15
> projects we noticed that the commit messages in Apache Mesos do
> not comply with the conventional format and even GitHub search
> was not able to find fixing commits. Therefore, we eliminated all
> Apache Mesos data and continued with the remaining 14 projects.

So, it is safe to exclude issues in MESOS since we don't know the corresponding fixing commit (we can store MESOS issue anyways, in case fixing commits will be available later).

Lines mentioning commits to `apache/hadoop` are loaded in db but ignored in the rest of the application since the project is not mentioned in the paper and there is no way to link the inducing issue to the commit using files in the dataset.