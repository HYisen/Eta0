package net.alexhyisen;

import org.apache.commons.codec.digest.Crypt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Keeper {
    private static final Path DB_PATH = Path.of(".", "shadow");
    private static final Path DB_PATH_SWP = Path.of(".", "shadow.swp");
    private static Map<String, String> db;//username->salted hashed password

    public static boolean isAuthorized(String username, String password) {
        String encrypted = db.get(username);

        if (encrypted == null) {
            //username not found
            return false;
        }

        return Crypt.crypt(password, encrypted.substring(0, 19)).equals(encrypted);
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

    public void save() throws IOException {
        List<String> lines = db.entrySet().stream()
                .map(v -> String.format("%s:%s", v.getKey(), v.getValue()))
                .collect(Collectors.toList());
        Files.delete(DB_PATH_SWP);
        Files.write(DB_PATH_SWP, lines, StandardOpenOption.CREATE_NEW);
        Files.move(DB_PATH_SWP, DB_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
