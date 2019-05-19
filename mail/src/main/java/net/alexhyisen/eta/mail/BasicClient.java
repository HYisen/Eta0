package net.alexhyisen.eta.mail;

import net.alexhyisen.Utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Alex on 2017/3/5.
 * BasicClient is a Client that use Java net infrastructure.
 */
class BasicClient implements Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    @SuppressWarnings("unused")
    public BasicClient(String host, int port) throws IOException {
        link(host, port);
    }

    @SuppressWarnings("WeakerAccess")
    public BasicClient() {
    }

    @Override
    public void link(String host, int port) throws IOException {
        //System.out.println("linking "+host+" at "+port);
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void send(String content) {
        Utility.log(Utility.LogCls.MAIL, "client: " + content);
        out.println(content);
    }

    @Override
    public String receive() throws IOException {
        String line = in.readLine();
        Utility.log(Utility.LogCls.MAIL, "server: " + line);
        return line;
    }

    @Override
    public void close() throws IOException {
        //out.close();
        //in.close();
        //Stream from socket would automatically closed, no need to revoke implicitly.
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        Client.smokeTest(new BasicClient());
    }
}
