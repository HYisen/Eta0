package net.alexhyisen;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KeeperTest {
    @Test
    public void simple() {
        sceneForKeeper(() -> {
            var keeper = new Keeper();
            try {
                keeper.load();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String username = "username";
            String password = "password";
            try {
                keeper.put(username, password);
            } catch (IOException e) {
                //How could such thing happened? I've protect the scene. Privilege or ConcurrentModificationException?
                throw new RuntimeException(e);
            }
            Assert.assertTrue("happy path", keeper.isAuthorized(username, password));
            Assert.assertFalse("bad password", keeper.isAuthorized(username, password + "1"));
            Assert.assertFalse("bad username", keeper.isAuthorized(username + "1", password));
        });
    }

    private void sceneForKeeper(Runnable procedure) {
        scene(() -> {
            try {
                Files.createFile(Keeper.DB_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            procedure.run();
            try {
                Files.delete(Keeper.DB_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, Keeper.DB_PATH, Keeper.DB_PATH_SWP);
    }

    //I don't use @Before & @After because of bakPath transmission issue.
    private void scene(Runnable procedure, Path... saved) {
        Map<Path, Path> paths = Arrays
                .stream(saved)
                .filter(Files::exists)
                .collect(Collectors.toUnmodifiableMap(Function.identity(), this::genBakPath));//origPath -> bakPath

        paths.forEach((old, neo) -> {
            try {
                Files.move(old, neo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        procedure.run();
        paths.forEach((old, neo) -> {
            try {
                Files.move(neo, old);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Path genBakPath(Path origPath) {
        String idlePath = origPath.toString() + ".bak";
        if (!Files.exists(Path.of(idlePath))) {
            return Path.of(idlePath);
        } else {
            int cnt = 0;
            while (true) {
                Path nextPath = Path.of(idlePath + cnt);
                if (!Files.exists(nextPath)) {
                    return nextPath;
                }
                cnt++;
            }
            //If all the billions of names are occupied, dead loop, I gave up under such circumstance.
        }
    }
}