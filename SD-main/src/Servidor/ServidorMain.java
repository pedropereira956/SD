package Servidor;

import Main.Connector;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorMain {
    public static void main(String[] args) {

        int port = 12345;
        limparFicheirosAntigos();
        final int D_HISTORICO = 7;
        final int S_LIMITE_MEMORIA = 3;

        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Servidor criado com sucesso!");

            Gestor gestor = new Gestor(D_HISTORICO, S_LIMITE_MEMORIA);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente encontrado!");

                Connector connector = new Connector(clientSocket);

                Worker worker = new Worker(connector, gestor);

                Thread threadClient = new Thread(worker);
                threadClient.start();
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void limparFicheirosAntigos() {
        File pasta = new File(".");
        File[] ficheiros = pasta.listFiles((dir, name) -> name.startsWith("dia_") && name.endsWith(".dat"));

        if (ficheiros != null) {
            for (File f : ficheiros) {
                f.delete();
            }
        }
        System.out.println(">>> Ambiente limpo. Ficheiros antigos apagados.");
    }
}