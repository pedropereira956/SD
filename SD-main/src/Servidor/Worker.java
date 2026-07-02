package Servidor;

import Main.Connector;
import Main.Event;
import Main.Frame;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Worker implements Runnable {

    private final Connector connector;
    private final Gestor gestor;

    private boolean autenticado = false;
    private String username = null;


    public Worker(Connector connector, Gestor gestor) {
        this.connector = connector;
        this.gestor = gestor;
    }

    public void run() {

        try (connector) {

            while (true) {
                Frame request = connector.receive();

                Frame response = processarRequest(request);

                connector.send(response.tag, response.data);
            }

        } catch (EOFException e) {
            System.out.println("Cliente " + (username != null ? username : "") + " desconectou-se.");
        } catch (IOException e) {
            System.out.println("Erro de comunicação com o cliente: " + e.getMessage());
        }
    }

    private Frame processarRequest(Frame request) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(request.data);
            DataInputStream in = new DataInputStream(bais);

            int opCode = in.readInt();

            if (!autenticado && opCode > 2) {
                return criarRespostaErro(request.tag, "Cliente não autenticado.");
            }

            switch (opCode) {
                case 1:
                    return handleLogin(in, request.tag);
                case 2:
                    return handleRegisto(in, request.tag);
                case 3:
                    return handleRegistarEvento(in, request.tag);
                case 4:
                    return handleNovoDia(request.tag);
                case 5:
                    return handleVendasSimultaneas(in, request.tag);
                case 6:
                    return handleVendasConsecutivas(in, request.tag);
                case 7:
                    return handleAgregacao(in, request.tag);
                case 8:
                    return handleFiltragem(in, request.tag);

                default:
                    return criarRespostaErro(request.tag, "Operação desconhecida.");
            }

        } catch (IOException e) {
            return criarRespostaErro(request.tag, "Erro a ler o pedido: " + e.getMessage());
        }
    }

    private Frame handleLogin(DataInputStream in, int tag) throws IOException {
        String user = in.readUTF();
        String pass = in.readUTF();

        if (gestor.autenticar(user, pass)) {
            this.autenticado = true;
            this.username = user;
            return criarRespostaSucesso(tag, "Login OK");
        } else {
            return criarRespostaErro(tag, "Credenciais inválidas");
        }
    }

    private Frame handleRegisto(DataInputStream in, int tag) throws IOException {
        String user = in.readUTF();
        String pass = in.readUTF();

        if (gestor.registar(user, pass)) {
            return criarRespostaSucesso(tag, "Registo OK");
        } else {
            return criarRespostaErro(tag, "Utilizador já existe");
        }
    }

    private Frame handleRegistarEvento(DataInputStream in, int tag) throws IOException {
        Event e = Event.deserialize(in);

        gestor.registarEvento(e);

        return criarRespostaSucesso(tag, "Evento registado");
    }

    private Frame handleNovoDia(int tag) {
        gestor.novoDia();
        return criarRespostaSucesso(tag, "Novo dia iniciado com sucesso.");
    }

    private Frame handleVendasSimultaneas(DataInputStream in, int tag) throws IOException {
        String p1 = in.readUTF();
        String p2 = in.readUTF();

        try {
            boolean result = gestor.subscreverVendasSimultaneas(p1, p2);
            if (result) {
                return criarRespostaSucesso(tag, "ALERTA: Vendas simultâneas detetadas para " + p1 + " e " + p2);
            } else {
                return criarRespostaSucesso(tag, "O dia terminou sem as vendas simultâneas.");
            }
        } catch (InterruptedException e) {
            return criarRespostaErro(tag, "Operação interrompida no servidor.");
        }
    }

    private Frame handleVendasConsecutivas(DataInputStream in, int tag) throws IOException {
        int n = in.readInt();

        try {
            String produto = gestor.subscreverVendasConsecutivas(n);

            if (produto != null) {
                return criarRespostaSucesso(tag, "ALERTA: " + n + " vendas consecutivas de: " + produto);
            } else {
                return criarRespostaSucesso(tag, "O dia terminou sem vendas consecutivas.");
            }
        } catch (InterruptedException e) {
            return criarRespostaErro(tag, "Operação interrompida no servidor.");
        }
    }

    private Frame handleAgregacao(DataInputStream in, int tag) throws IOException {
        int tipo = in.readInt();
        String produto = in.readUTF();
        int dias = in.readInt();

        double resultado = gestor.agregar(tipo, produto, dias);

        return criarRespostaSucesso(tag, String.valueOf(resultado));
    }

    private Frame handleFiltragem(DataInputStream in, int tag) throws IOException {
        int dias = in.readInt();
        int numProdutos = in.readInt();
        Set<String> produtos = new HashSet<>();

        for (int i = 0; i < numProdutos; i++) {
            produtos.add(in.readUTF());
        }

        List<Event> eventos = gestor.getEventosFiltrados(dias, produtos);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            out.writeInt(1);
            out.writeInt(eventos.size());

            for (Event e : eventos) {
                e.serialize(out);
            }

            out.flush();
            return new Frame(tag, baos.toByteArray());

        } catch (IOException e) {
            return criarRespostaErro(tag, "Erro na serialização da lista.");
        }
    }

    private Frame criarRespostaErro(int tag, String mensagem) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            out.writeInt(0);
            out.writeUTF(mensagem);
            out.flush();

            return new Frame(tag, baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    private Frame criarRespostaSucesso(int tag, String mensagem) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            out.writeInt(1);
            out.writeUTF(mensagem);
            out.flush();

            return new Frame(tag, baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }
}
