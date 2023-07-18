# ExtractMethodsFirehouse
This tool enables the analysis of open-source repositories and extraction of long methods committed within the past n hours.

### Input
* The file [selected-repos.csv](https://github.com/maldil/ExtractMethodsFirehouse/blob/main/ExtractMethodsFirehouse/selected-repos.csv) requires updating by including all the projects that need to be analyzed. Please ensure that each project entry follows the format `author/project_name` within the file.

### Configurations
The file [Configurations.java](https://github.com/maldil/ExtractMethodsFirehouse/blob/main/ExtractMethodsFirehouse/src/main/java/config/Configurations.java) encompasses all the configurations that require updating:

- PROJECT_REPOSITORY: This represents the local path where the projects need to be downloaded.
- LONG_METHODS: This signifies the local path where the analysis output should be stored.
- METHOD_LENGTH: This denotes the size of the methods (in LOC) that should be considered as long methods.
- DURATION: This determines the time duration within which commits should be considered, starting from the current time. For example, if this is set to 24, it will consider commits made within the past 24 hours.

Please update above filed and then invoke the [main function](https://github.com/maldil/ExtractMethodsFirehouse/blob/12c6356168301831095b951817328cda3838daed/ExtractMethodsFirehouse/src/main/java/org/com/Main.java#L40). 

### Output
A CSV file containing the exact lines for the long methods will be saved in the `resource` folder, located within the `src/main` directory.
