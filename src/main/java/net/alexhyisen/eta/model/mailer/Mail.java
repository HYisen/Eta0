package net.alexhyisen.eta.model.mailer;

import javax.annotation.Nullable;
import java.util.Optional;

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
    private final String content;

    public Mail(@Nullable String senderName, String senderAddr,
                @Nullable String recipientName, String recipientAddr,
                String subject, String content) {
        this.senderName = senderName;
        this.senderAddr = senderAddr;
        this.recipientName = recipientName;
        this.recipientAddr = recipientAddr;
        this.subject = subject;
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public @Nullable String getSenderName() {
        return senderName;
    }

    public String getSenderAddr() {
        return senderAddr;
    }

    public @Nullable String getRecipientName() {
        return recipientName;
    }

    public String getRecipientAddr() {
        return recipientAddr;
    }
}
