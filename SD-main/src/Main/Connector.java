package Main;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connector implements AutoCloseable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final Lock writeLock = new ReentrantLock();
    private final Lock readLock = new ReentrantLock();

    public Connector (Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public void send(int tag, byte[] data) throws IOException {
        writeLock.lock();
        try {

            out.writeInt(4 + data.length);
            out.writeInt(tag);
            out.write(data);
            out.flush();
        } finally {
            writeLock.unlock();
        }
    }

    public Frame receive() throws IOException {
        readLock.lock();
        try {
            int size = in.readInt();
            int tag = in.readInt();
            byte[] data = new byte[size - 4];
            in.readFully(data);

            return new Frame(tag, data);
        } finally {
            readLock.unlock();
        }
    }

    public void close() throws IOException {
        socket.close();
    }


}
