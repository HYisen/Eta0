package net.alexhyisen.eta.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Created by Alex on 2017/3/9.
 * An imitation of boost::timer in cpp.
 * Manual operation is needed,
 * the absence of destructor in Java makes the initialization in constructor meaningless.
 */
public class Timer {
    private Instant record;
    private String project;

    public void resume(String project) {
        this.project = project;
        record = Instant.now();
    }

    public void report(String project) {
        long interval = Duration.between(record, Instant.now()).toMillis();
        if (Objects.equals(project, this.project)) {
            System.out.printf("%s costs %4dms\n", project, interval);
        } else {
            System.out.printf("from %s to %s passed %4dms\n", this.project, project, interval);
        }
    }

    public void report() {
        report(project);
    }
}
