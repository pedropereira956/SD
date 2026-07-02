package Client;

import Main.Event;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BibliotecaCliente implements AutoCloseable {
    private final Demultiplexer demultiplexer;
    private int nextTag = 1;
    private final Lock tagLock = new ReentrantLock();

    public BibliotecaCliente(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);

        this.demultiplexer = new Demultiplexer(socket);

    }

    public void start() {
        this.demultiplexer.start();
    }

    private int getNextTag() {
        tagLock.lock();
        try {
            return nextTag++;
        } finally {
            tagLock.unlock();
        }
    }

    public void close() throws IOException {
        demultiplexer.close();
    }

    public String login(String user, String pass) throws Exception {
        int tag = getNextTag();
        byte[] responseData;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(1);
            out.writeUTF(user);
            out.writeUTF(pass);
            out.flush();

            demultiplexer.send(tag, baos.toByteArray());
            responseData = demultiplexer.receive(tag);

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(responseData));
            int statusCode = in.readInt();
            String mensagem = in.readUTF();

            if (statusCode == 1) {
                return "SUCESSO: " + mensagem;
            } else {
                throw new Exception("FALHA no Login: " + mensagem);
            }
        } catch (InterruptedException | IOException e) {
            throw new Exception("Erro de comunicação ao fazer login.", e);
        }
    }

    public String registar(String user, String pass) throws Exception {
        int tag = getNextTag();
        byte[] responseData;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(2);
            out.writeUTF(user);
            out.writeUTF(pass);
            out.flush();

            demultiplexer.send(tag, baos.toByteArray());
            responseData = demultiplexer.receive(tag);

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(responseData));
            int statusCode = in.readInt();
            String mensagem = in.readUTF();

            if (statusCode == 1) {
                return "SUCESSO: " + mensagem;
            } else {
                throw new Exception("FALHA no Registo: " + mensagem);
            }
        } catch (InterruptedException | IOException e) {
            throw new Exception("Erro de comunicação ao registar.", e);
        }
    }

    public String registarVenda(String produto, int quantidade, double preco) throws Exception {
        int tag = getNextTag();
        byte[] responseData;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(3);

            Event e = new Event(produto, quantidade, preco);
            e.serialize(out);
            out.flush();

            demultiplexer.send(tag, baos.toByteArray());
            responseData = demultiplexer.receive(tag);

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(responseData));
            int statusCode = in.readInt();
            String mensagem = in.readUTF();

            if (statusCode == 1) {
                return "SUCESSO: " + mensagem;
            } else {
                throw new Exception("FALHA ao registar venda: " + mensagem);
            }
        } catch (InterruptedException | IOException e) {
            throw new Exception("Erro de comunicação ao registar venda.", e);
        }
    }

    public String novoDia() throws Exception {
        int tag = getNextTag();
        byte[] responseData;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(4);
            out.flush();

            demultiplexer.send(tag, baos.toByteArray());
            responseData = demultiplexer.receive(tag);

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(responseData));
            int statusCode = in.readInt();
            String mensagem = in.readUTF();

            if (statusCode == 1) {
                return "SUCESSO: " + mensagem;
            } else {
                throw new Exception("FALHA ao iniciar novo dia: " + mensagem);
            }
        } catch (InterruptedException | IOException e) {
            throw new Exception("Erro de comunicação ao iniciar novo dia.", e);
        }
    }

    public String subscreverSimultaneas(String p1, String p2) throws Exception {
        int tag = getNextTag();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(5);
            out.writeUTF(p1);
            out.writeUTF(p2);
            out.flush();

            demultiplexer.send(tag, baos.toByteArray());
            byte[] responseData = demultiplexer.receive(tag);

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(responseData));
            int statusCode = in.readInt();
            String mensagem = in.readUTF();

            if (statusCode == 1) return mensagem;
            else throw new Exception(mensagem);
        } catch (Exception e) {
            throw new Exception("Erro na subscrição simultânea.", e);
        }
    }

    public String subscreverConsecutivas(int n) throws Exception {
        int tag = getNextTag();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(6);
            out.writeInt(n);
            out.flush();

            demultiplexer.send(tag, baos.toByteArray());
            byte[] responseData = demultiplexer.receive(tag);

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(responseData));
            int statusCode = in.readInt();
            String mensagem = in.readUTF();

            if (statusCode == 1) return mensagem;
            else throw new Exception(mensagem);
        } catch (Exception e) {
            throw new Exception("Erro na subscrição consecutiva.", e);
        }
    }

    public String consultarAgregacao(int tipo, String produto, int dias) throws Exception {
        int tag = getNextTag();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(7);
            out.writeInt(tipo);
            out.writeUTF(produto);
            out.writeInt(dias);
            out.flush();

            demultiplexer.send(tag, baos.toByteArray());
            byte[] responseData = demultiplexer.receive(tag);

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(responseData));
            int statusCode = in.readInt();
            String mensagem = in.readUTF();

            if (statusCode == 1) return mensagem;
            else throw new Exception("Erro na consulta: " + mensagem);
        } catch (Exception e) {
            throw new Exception("Falha na comunicação.", e);
        }
    }

    public List<Event> filtrarEventos(int dias, Set<String> produtos) throws Exception {
        int tag = getNextTag();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(8);
            out.writeInt(dias);
            out.writeInt(produtos.size());
            for (String s : produtos) {
                out.writeUTF(s);
            }
            out.flush();

            demultiplexer.send(tag, baos.toByteArray());

            byte[] responseData = demultiplexer.receive(tag);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(responseData));

            int statusCode = in.readInt();
            if (statusCode == 0) {
                throw new Exception(in.readUTF());
            }

            int numEventos = in.readInt();
            List<Event> lista = new ArrayList<>();
            for (int i = 0; i < numEventos; i++) {
                lista.add(Event.deserialize(in));
            }

            return lista;

        } catch (Exception e) {
            throw new Exception("Erro ao filtrar eventos.", e);
        }
    }

}
