package net.alexhyisen.eta.model.mailer;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alex on 2017/3/5.
 * Client is an abstraction of a Socket-like object.
 */
interface Client extends Closeable {
    void link(String host, int port) throws IOException;

    void send(String content);

    String receive() throws IOException;

    @SuppressWarnings("UnusedReturnValue")
    default List<String> receive(int times) throws IOException {
        List<String> rtn = new LinkedList<>();
        for (int k = 0; k != times; k++) {
            rtn.add(receive());
        }
        return rtn;
    }

    static void smokeTest(Client client) throws IOException {
        client.link("localhost", 4444);
        client.send("Hello");
        System.out.println("get " + client.receive());
        System.out.println("get " + client.receive());
        client.send("Bye.");
        System.out.println("get " + client.receive());
        client.close();
    }
}
