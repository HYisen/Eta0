package net.alexhyisen.eta.mail;

import net.alexhyisen.Config;
import net.alexhyisen.eta.book.Chapter;

import javax.annotation.Nullable;

/**
 * Created by Alex on 2017/3/5.
 * Mail is all the data that need in a mail.
 * It is just a data structure.
 */
public class Mail {
    private final String senderName;
    private final String senderAddr;
    private final String recipientName;
    private final String recipientAddr;
    private final String subject;
    private final String[] content;

    public Mail(@Nullable String senderName, String senderAddr,
                @Nullable String recipientName, String recipientAddr,
                String subject, String... content) {
        this.senderName = senderName;
        this.senderAddr = senderAddr;
        this.recipientName = recipientName;
        this.recipientAddr = recipientAddr;
        this.subject = subject;
        this.content = content;
    }

    public Mail(Config config, String bookName, Chapter chapter) {
        this(
                config.get("senderName"),
                config.get("senderAddr"),
                config.get("recipientName"),
                config.get("recipientAddr"),
                String.format("《%s》 %s", bookName, chapter.getName()),
                chapter.getData());
    }

    public Mail(Config config, String subject, String... content) {
        this(
                config.get("senderName"),
                config.get("senderAddr"),
                config.get("recipientName"),
                config.get("recipientAddr"),
                subject,
                content);
    }

    public String getSubject() {
        return subject;
    }

    public String[] getContent() {
        return content;
    }

    @SuppressWarnings("WeakerAccess")
    public @Nullable
    String getSenderName() {
        return senderName;
    }

    @SuppressWarnings("WeakerAccess")
    public String getSenderAddr() {
        return senderAddr;
    }

    @SuppressWarnings("WeakerAccess")
    public @Nullable
    String getRecipientName() {
        return recipientName;
    }

    @SuppressWarnings("WeakerAccess")
    public String getRecipientAddr() {
        return recipientAddr;
    }
}
