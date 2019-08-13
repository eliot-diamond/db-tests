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
    public boolean equals(Object obj) {
        if (obj instanceof SearchResultHeading) {
            return (title.equals(((SearchResultHeading) obj).title));
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }
}
