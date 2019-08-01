package uk.ac.diamond.daq.persistence.service;

public class SearchResultHeading {
    private String title;
    private int priority;

    public static int compare (SearchResultHeading lhs, SearchResultHeading rhs) {
        return Integer.compare(lhs.priority, rhs.priority);
    }

    public SearchResultHeading(String title, int priority) {
        this.title = title;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }
}
