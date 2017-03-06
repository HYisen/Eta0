package net.alexhyisen.eta.model.mailer;

import java.io.IOException;

/**
 * Created by Alex on 2017/3/5.
 * Client is an abstraction of a Socket-like object.
 */
interface Client {
    void link(String host, int port) throws IOException;
    void send(String content);
    String receive() throws IOException;
    default String receive(int times) throws IOException {
        StringBuilder sb=new StringBuilder();
        for(int k=0;k!=times;k++){
            sb.append(receive()).append('\n');
        }
        return sb.toString();
    }
}
