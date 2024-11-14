import java.util.Date;

public class Requirement {
    private final String projectName;
    private final String dataset;
    private final String id;
    private final String title;
    private final String text;
    private final boolean buggy;
    private final Date date;

    Requirement(String id, String dataset, String projectName, String title, String text, boolean buggy, Date date) {
        this.projectName = projectName;
        this.dataset = dataset;
        this.id = id;
        this.title = title;
        this.text = text;
        this.buggy = buggy;
        this.date = date;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getDataset() {
        return dataset;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public boolean isBuggy() {
        return buggy;
    }

    public Date getDate() {
        return date;
    }
}
