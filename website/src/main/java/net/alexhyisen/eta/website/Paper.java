package net.alexhyisen.eta.website;

public class Paper {
    private String name;
    private String href;
    private String date;

    public Paper(String name, String href, String date) {
        this.name = name;
        this.href = href;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public String getHref() {
        return href;
    }

    public String getDate() {
        return date;
    }
}
