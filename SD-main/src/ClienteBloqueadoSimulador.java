
import java.io.*;
import java.net.Socket;

/**
 * Simula um cliente problemático que envia pedidos mas não consome respostas.
 * Usado para testar a robustez do sistema.
 */
public class ClienteBloqueadoSimulador {

    private Socket socket;
    private DataOutputStream out;

    public ClienteBloqueadoSimulador(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    /**
     * Envia múltiplos pedidos sem nunca ler as respostas.
     * Isto pode causar:
     * - Buffers de saída do servidor ficarem cheios
     * - Bloqueio da thread Worker do servidor
     * - Impacto em outros clientes se não houver proteções adequadas
     */
    public void enviarSemLer(int numPedidos) throws IOException {
        System.out.println("Cliente bloqueado: Enviando " + numPedidos +
                " pedidos SEM ler respostas...");

        for (int i = 0; i < numPedidos; i++) {
            // Criar pedido de registo de venda
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(baos);

            // OpCode 3 = Registar Evento
            dataOut.writeInt(3);

            // Dados do evento (produto, quantidade, preço)
            dataOut.writeUTF("ProdutoBloqueado" + i);
            dataOut.writeInt(1);
            dataOut.writeDouble(10.0);

            dataOut.flush();
            byte[] data = baos.toByteArray();

            // Enviar frame
            int tag = i + 1000; // Tags únicas
            out.writeInt(4 + data.length); // size
            out.writeInt(tag);              // tag
            out.write(data);                // data
            out.flush();

            if (i % 10 == 0) {
                System.out.println("  Enviados " + (i+1) + " pedidos (sem ler respostas)");
            }

            try {
                Thread.sleep(50); // Pequeno delay entre envios
            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("Cliente bloqueado: Todos os pedidos enviados. " +
                "Agora vou esperar indefinidamente SEM ler o socket...");

        // NÃO fecha o socket, NÃO lê respostas - fica "pendurado"
        // Isto simula um cliente com problemas de rede ou que crashou após enviar

        try {
            Thread.sleep(30000); // Espera 30 segundos
        } catch (InterruptedException e) {
            // ignora
        }

        System.out.println("Cliente bloqueado: A terminar...");
        socket.close();
    }
}