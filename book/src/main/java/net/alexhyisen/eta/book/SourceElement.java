package net.alexhyisen.eta.book;

/**
 * This is the avatar of book element in source xml.
 * If it's added at first time, I would convert the xml with this class, rather than annotation.
 */
public class SourceElement {
    private String name;
    private String path;
    private String link;

    public SourceElement() {
    }

    public SourceElement(Book orig) {
        this.name = orig.getName();
        this.path = orig.getPath();
        this.link = orig.getSource();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
