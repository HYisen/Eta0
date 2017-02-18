package model;

import java.io.Serializable;

/**
 * Created by Alex on 2017/2/1.
 * Hyperlink is the avatar of a hyperlink in HTML.
 */
class Hyperlink{
    private final String text;
    private final String href;

    Hyperlink(String text, String href) {
        this.text = text;
        this.href = href;
    }

    String getText() {
        return text;
    }

    String getHref() {
        return href;
    }

}
