package Client;

import Main.Connector;
import Main.Frame;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Demultiplexer implements AutoCloseable {
    private final Connector connector;
    private final Map<Integer, byte[]> buffer = new HashMap<>();
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private final Thread receiverThread;

    private IOException exception = null;
    private volatile boolean running = true;

    public Demultiplexer(Socket socket) throws IOException {
        this.connector = new Connector(socket);

        this.receiverThread = new Thread(this::receiveLoop);
        this.receiverThread.setDaemon(true);
    }

    public void start() {
        this.receiverThread.start();
    }

    public void send(int tag, byte[] data) throws IOException {
        if (exception != null) throw exception;
        if (!running) throw new IOException("Demultiplexer fechado");
        connector.send(tag, data);
    }

    public byte[] receive(int tag) throws InterruptedException, IOException {
        lock.lock();
        try {
            while (!buffer.containsKey(tag) && exception == null && running) {
                condition.await();
            }

            if (exception != null) {
                throw new IOException("Thread de leitura falhou", exception);
            }

            if (!running && !buffer.containsKey(tag)) {
                throw new IOException("Demultiplexer fechado antes de receber resposta");
            }

            return buffer.remove(tag);
        } finally {
            lock.unlock();
        }
    }

    private void receiveLoop() {
        try {
            while (running) {
                Frame frame = connector.receive();

                lock.lock();
                try {
                    buffer.put(frame.tag, frame.data);
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        } catch (IOException e) {
            lock.lock();
            try {
                if (running) {
                    exception = e;
                    condition.signalAll();

                    System.err.println("Conexao perdida com servidor: " + e.getMessage());
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void close() throws IOException {
        lock.lock();
        try {
            running = false;
            condition.signalAll();
        } finally {
            lock.unlock();
        }

        connector.close();

        try {
            receiverThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}