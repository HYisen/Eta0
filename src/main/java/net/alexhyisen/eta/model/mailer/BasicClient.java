package net.alexhyisen.eta.model.mailer;

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

    public BasicClient(String host,int port) throws IOException {
        link(host, port);
    }

    public BasicClient() {
    }

    @Override
    public void link(String host, int port) throws IOException {
        //System.out.println("linking "+host+" at "+port);
        socket = new Socket(host, port);
        out=new PrintWriter(socket.getOutputStream(),true);
        in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void send(String content) {
        System.out.println("client: "+content);
        out.println(content);
    }

    @Override
    public String receive() throws IOException {
        String line=in.readLine();
        System.out.println("server: "+line);
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
        Client client=new BasicClient();
        client.link("localhost",4444);
        client.send("Hello");
        System.out.println("get "+client.receive());
        System.out.println("get "+client.receive());
        client.send("Bye.");
        System.out.println("get "+client.receive());
    }
}
