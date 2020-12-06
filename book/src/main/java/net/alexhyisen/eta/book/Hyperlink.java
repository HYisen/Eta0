package net.alexhyisen.eta.book;

/**
 * Created by Alex on 2017/2/1.
 * Hyperlink is the avatar of a hyperlink in HTML.
 */
public class Hyperlink {
    private final String text;
    private final String href;

    Hyperlink(String text, String href) {
        this.text = text;
        this.href = href;
    }

    String getText() {
        return text;
    }

    @Override
    public String toString() {
        return String.format("Hyperlink[%s@%s]", text, href);
    }

    String getHref() {
        return href;
    }

}
