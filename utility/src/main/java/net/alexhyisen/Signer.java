package net.alexhyisen;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;

/**
 * Created by new on 2017/6/27.
 * A stand alone procedure used to generate digital signature.
 */
public class Signer {
    public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, IOException {
        KeyPair pair = generateKey();
        save(pair.getPrivate(), Paths.get("priKey"));
        save(pair.getPublic(), Paths.get("pubKey"));
    }

    private static void save(Serializable obj, Path path) throws IOException {
        File file = Files.createFile(path).toFile();
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
        os.writeObject(obj);
        os.close();
    }

    public static <T extends Serializable> T load(Path path) throws IOException, ClassNotFoundException {
        ObjectInputStream is = new ObjectInputStream(new FileInputStream(path.toFile()));
        Object rtn = is.readObject();
        //noinspection unchecked
        return (T) rtn;
    }

    private static KeyPair generateKey() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        SecureRandom random = SecureRandom.getInstanceStrong();
        keyGen.initialize(1024, random);
        return keyGen.generateKeyPair();
    }
}
