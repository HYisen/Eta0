package net.alexhyisen.eta.model.server;

import com.google.gson.Gson;
import net.alexhyisen.eta.model.Book;
import net.alexhyisen.eta.model.Chapter;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Alex on 2017/6/8.
 * something warp various messages to be send in JSON
 */
public class Envelope {
    public enum EnvelopeType {
        CHAPTER,
        BOOK,
        SHELF,
    }

    private String type;
    private String name;
    private String[] content;

    public Envelope() {
    }

    public Envelope(EnvelopeType type, String name, String[] content) {
        this.type = type.toString();
        this.name = name;
        this.content = content;
    }

    Envelope(Chapter orig) {
        this.type = EnvelopeType.CHAPTER.toString();
        this.name = orig.getName();
        this.content = orig.getData();
    }

    Envelope(Book orig) {
        this.type = EnvelopeType.BOOK.toString();
        this.name = orig.getName();
        if (!orig.isOpened()) {
            orig.open();
        }
        this.content = orig.getChapters().stream()
                .map(Chapter::getName)
                .collect(Collectors.toList()).toArray(new String[orig.getChapters().size()]);
    }

    Envelope(List<Book> orig) {
        this.type = EnvelopeType.SHELF.toString();
        this.name = null;//The appearance of NPE implies a misread of EnvelopeType, let it die.
        this.content = orig.stream()
                .map(Book::getName)
                .peek(System.out::println)
                //It seems as if a generic toArray must be used. An swallowed Exception occurs under such circumstance.
                //java.lang.ClassCastException: [Ljava.lang.Object; cannot be cast to [Ljava.lang.String
                .collect(Collectors.toList()).toArray(new String[orig.size()]);
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String[] getContent() {
        return content;
    }

    String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this, this.getClass());
    }
}
