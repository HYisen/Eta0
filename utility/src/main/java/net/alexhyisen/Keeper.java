package net.alexhyisen;

import org.apache.commons.codec.digest.Crypt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A global singleton offers security.
 */
public class Keeper {
    static final Path DB_PATH = Path.of(".", "shadow");
    static final Path DB_PATH_SWP = Path.of(".", "shadow.swp");

    private static Map<String, String> db;//username->salted hashed password

    private static ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private static Set<String> memory = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static Random rand = new Random();

    private static char genRandomChar() {
        int code = rand.nextInt(62);
        if (code < 10) {
            return (char)('0' + code);
        } else if (code < 36) {
            return (char) ('a' + code - 10);
        } else {
            return (char) ('a' + code - 36);
        }
    }

    public static String genRandomString() {
        var sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(genRandomChar());
        }
        return sb.toString();
    }

    public boolean isAuthorized(String token) {
        return memory.contains(token);
    }

    public String register() {
        String token = genRandomString();
        while (memory.contains(token)) {
            token = genRandomString();
        }
        String finalToken = token;
        ses.schedule(() -> memory.remove(finalToken), 3600, TimeUnit.SECONDS);
        return token;
    }

    public boolean isAuthorized(String username, String password) {
        String encrypted = db.get(username);

        if (encrypted == null) {
            //username not found
            return false;
        }

        return encrypted.equals(Crypt.crypt(password, encrypted.substring(0, 19)));
    }

    /**
     * Idempotent, without security check method that insert/update user credentials.
     * Should seldom be used, so synchronization performance cost does not matter.
     * Moreover, the clone & swap cost is much more greater than the synchronize, but nobody cares.
     *
     * @throws IOException if fails to persistent the update
     */
    public void put(String username, String password) throws IOException {
        synchronized (Keeper.class) {
            Map<String, String> tmp = new LinkedHashMap<>(db);//I don't want to change the order.
            tmp.put(username, Crypt.crypt(password));
            db = tmp;
            save();
        }
    }

    public void load() throws IOException {
        if (Files.exists(DB_PATH_SWP)) {
            throw new RuntimeException(DB_PATH_SWP + "(swap file) exists");
        }
        db = Files
                .lines(DB_PATH)
                .map(v -> v.split(":"))
                .collect(Collectors.toUnmodifiableMap(v -> v[0], v -> v[1]));
    }

    private void save() throws IOException {
        List<String> lines = db.entrySet().stream()
                .map(v -> String.format("%s:%s", v.getKey(), v.getValue()))
                .collect(Collectors.toList());
        Files.write(DB_PATH_SWP, lines, StandardOpenOption.CREATE_NEW);
        Files.move(DB_PATH_SWP, DB_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
