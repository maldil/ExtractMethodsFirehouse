package config;

public class Configurations {
    public enum TIME {
        AUTHOR,
        COMMITTER,
    }
    public static String PROJECT_REPOSITORY = "";
    public static String LONG_METHODS= "";
    public static Integer METHOD_LENGTH = 30;
    public static Integer DURATION = 72;
    public static TIME TIME_CONFIG = TIME.AUTHOR;
}
