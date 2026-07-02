package Client;

import Main.Event;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class ClienteMain {

    private static BibliotecaCliente cliente = null;
    private static final Scanner scanner = new Scanner(System.in);
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        System.out.println("--- Cliente de Casas Barbosa ---");

        try {
            cliente = new BibliotecaCliente(SERVER_HOST, SERVER_PORT);

            cliente.start();

            System.out.println("Conexão estabelecida com sucesso!");

            menuPrincipal();

        } catch (IOException e) {
            System.err.println("ERRO: Não foi possível conectar ao servidor em " + SERVER_HOST + ":" + SERVER_PORT);
            System.err.println("Certifique-se de que o ServidorMain está em execução.");
        } finally {
            if (cliente != null) {
                try {
                    cliente.close();
                } catch (IOException e) {
                }
            }
            scanner.close();
        }
    }

    private static void menuPrincipal() {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Login");
            System.out.println("2. Registar Novo Utilizador");
            System.out.println("3. Registar Venda");
            System.out.println("4. Começar Novo Dia");
            System.out.println("5. Subscrever Vendas Simultâneas");
            System.out.println("6. Subscrever Vendas Consecutivas");
            System.out.println("7. Consultar Informação de Produtos");
            System.out.println("8. Filtrar Produtos");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opção: ");

            try {
                if (scanner.hasNextInt()) {
                    opcao = scanner.nextInt();
                    scanner.nextLine();

                    switch (opcao) {
                        case 1:
                            handleLogin();
                            break;
                        case 2:
                            handleRegistar();
                            break;
                        case 3:
                            handleRegistarVenda();
                            break;
                        case 4:
                            handleNovoDia();
                            break;
                        case 5:
                            handleSimultaneas();
                            break;
                        case 6:
                            handleConsecutivas();
                            break;
                        case 7:
                            handleConsultar();
                            break;
                        case 8:
                            handleFiltragem();
                            break;
                        case 0:
                            System.out.println("A encerrar conexão...");
                            break;
                        default:
                            System.out.println("Opção inválida.");
                    }
                } else {
                    System.out.println("Entrada inválida. Por favor, digite um número.");
                    scanner.nextLine();
                }
            } catch (Exception e) {
                System.err.println("ERRO DE EXECUÇÃO: " + e.getMessage());
            }
        }
    }

    private static void handleLogin() throws Exception {
        System.out.print("Username: ");
        String user = scanner.nextLine();
        System.out.print("Password: ");
        String pass = scanner.nextLine();

        String resultado = cliente.login(user, pass);
        System.out.println(resultado);
    }

    private static void handleRegistar() throws Exception {
        System.out.print("Novo Username: ");
        String user = scanner.nextLine();
        System.out.print("Nova Password: ");
        String pass = scanner.nextLine();

        String resultado = cliente.registar(user, pass);
        System.out.println(resultado);
    }

    private static void handleRegistarVenda() throws Exception {
        System.out.print("Nome do Produto: ");
        String produto = scanner.nextLine();
        System.out.print("Quantidade: ");
        int quantidade = scanner.nextInt();
        System.out.print("Preço Unitário: ");
        double preco = scanner.nextDouble();
        scanner.nextLine();

        String resultado = cliente.registarVenda(produto, quantidade, preco);
        System.out.println(resultado);
    }

    private static void handleNovoDia() throws Exception {
        String resultado = cliente.novoDia();
        System.out.println(resultado);
    }

    private static void handleSimultaneas() throws Exception {
        System.out.print("Produto 1: ");
        String p1 = scanner.nextLine();
        System.out.print("Produto 2: ");
        String p2 = scanner.nextLine();

        System.out.println(">>> À espera que as vendas ocorram... (O programa vai bloquear)");
        String res = cliente.subscreverSimultaneas(p1, p2);
        System.out.println(">>> NOTIFICAÇÃO RECEBIDA: " + res);
    }

    private static void handleConsecutivas() throws Exception {
        System.out.print("Numero de vendas consecutivas (N): ");
        int n = scanner.nextInt();
        scanner.nextLine();

        System.out.println(">>> À espera de " + n + " vendas consecutivas... (O programa vai bloquear)");
        String res = cliente.subscreverConsecutivas(n);
        System.out.println(">>> NOTIFICAÇÃO RECEBIDA: " + res);
    }

    private static void handleConsultar() throws Exception {
        System.out.println("1. Quantidade Total");
        System.out.println("2. Volume de Vendas");
        System.out.println("3. Preço Máximo");
        System.out.println("4. Preço Médio");

        System.out.print("Tipo: ");
        int tipo = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Produto: ");
        String prod = scanner.nextLine();
        System.out.print("Quantos dias para trás? ");
        int dias = scanner.nextInt();
        scanner.nextLine();

        String res = cliente.consultarAgregacao(tipo, prod, dias);
        System.out.println(">>> RESULTADO: " + res);
    }

    private static void handleFiltragem() throws Exception {
        System.out.print("Quantos dias para trás? ");
        int dias = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Insira os produtos a filtrar (separados por espaço):");
        String linha = scanner.nextLine();
        String[] nomes = linha.split(" ");
        Set<String> produtos = new HashSet<>();
        for(String s : nomes) produtos.add(s);

        List<Event> resultados = cliente.filtrarEventos(dias, produtos);

        System.out.println(">>> RESULTADOS (" + resultados.size() + " eventos):");
        for (Event e : resultados) {
            System.out.println("   - " + e.toString());
        }
    }
}