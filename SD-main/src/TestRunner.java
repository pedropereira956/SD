import Client.BibliotecaCliente;
import Main.Event;

import java.util.*;

public class TestRunner {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    private static int sucessos = 0;
    private static int falhas = 0;
    private static long tempoTotal = 0;
    private static final Object lock = new Object();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== TESTES DE DESEMPENHO ===\n");
        System.out.println("1. Teste de Carga Basica");
        System.out.println("2. Teste de Escalabilidade");
        System.out.println("3. Teste de Robustez");
        System.out.println("4. Teste de Cache");
        System.out.println("5. Teste de Subscricoes");
        System.out.println("0. Sair");
        System.out.print("\nOpcao: ");

        int opcao = scanner.nextInt();

        try {
            switch (opcao) {
                case 1: testeCargaBasica(); break;
                case 2: testeEscalabilidade(); break;
                case 3: testeRobustez(); break;
                case 4: testeCache(); break;
                case 5: testeSubscricoes(); break;
                case 0: System.out.println("Saindo..."); break;
                default: System.out.println("Opcao invalida");
            }
        } catch (Exception e) {
            System.err.println("ERRO: " + e.getMessage());
        }

        scanner.close();
    }

    // TESTE 1: CARGA BASICA
    private static void testeCargaBasica() throws Exception {
        System.out.println("\n=== TESTE 1: CARGA BASICA ===\n");

        int numClientes = 5;
        int opsPorCliente = 20;

        resetMetricas();
        Thread[] threads = new Thread[numClientes];
        long inicio = System.currentTimeMillis();

        for (int i = 0; i < numClientes; i++) {
            final int id = i;
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        BibliotecaCliente cliente = new BibliotecaCliente(HOST, PORT);
                        cliente.start();
                        cliente.login("user", "user");

                        Random rand = new Random();

                        for (int op = 0; op < opsPorCliente; op++) {
                            long opInicio = System.nanoTime();

                            try {
                                int tipo = rand.nextInt(3);

                                if (tipo == 0) {
                                    String produto = "Produto" + rand.nextInt(5);
                                    cliente.registarVenda(produto, rand.nextInt(10) + 1,
                                            rand.nextDouble() * 100);
                                    Thread.sleep(10); // Pequeno delay para evitar sobrecarga
                                } else if (tipo == 1) {
                                    cliente.consultarAgregacao(rand.nextInt(4) + 1,
                                            "Produto" + rand.nextInt(5),
                                            rand.nextInt(3) + 1);
                                } else {
                                    Set<String> produtos = new HashSet<>();
                                    produtos.add("Produto" + rand.nextInt(5));
                                    cliente.filtrarEventos(1, produtos);
                                }

                                long opFim = System.nanoTime();

                                synchronized(lock) {
                                    tempoTotal += (opFim - opInicio) / 1_000_000;
                                    sucessos++;
                                }

                            } catch (Exception e) {
                                synchronized(lock) {
                                    falhas++;
                                }
                            }
                        }

                        cliente.close();

                    } catch (Exception e) {
                        System.err.println("Cliente " + id + " erro: " + e.getClass().getSimpleName());
                    }
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long fim = System.currentTimeMillis();
        imprimirResultados("Carga Basica", numClientes, opsPorCliente, inicio, fim);
    }

    // TESTE 2: ESCALABILIDADE
    private static void testeEscalabilidade() throws Exception {
        System.out.println("\n=== TESTE 2: ESCALABILIDADE ===\n");

        int[] numClientes = {5, 10, 20, 30, 40};
        int opsPorCliente = 10;

        System.out.println("Clientes | Throughput | Latencia | Sucesso");
        System.out.println("---------|------------|----------|--------");

        for (int n : numClientes) {
            resetMetricas();
            Thread[] threads = new Thread[n];

            long inicio = System.currentTimeMillis();

            for (int i = 0; i < n; i++) {
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        try {
                            BibliotecaCliente cliente = new BibliotecaCliente(HOST, PORT);
                            cliente.start();
                            cliente.login("user", "user");

                            for (int op = 0; op < opsPorCliente; op++) {
                                long opInicio = System.nanoTime();
                                try {
                                    cliente.registarVenda("ProdutoTeste", 1, 10.0);
                                    Thread.sleep(5); // Pequeno delay
                                    long opFim = System.nanoTime();
                                    synchronized(lock) {
                                        tempoTotal += (opFim - opInicio) / 1_000_000;
                                        sucessos++;
                                    }
                                } catch (Exception e) {
                                    synchronized(lock) {
                                        falhas++;
                                    }
                                }
                            }

                            cliente.close();
                        } catch (Exception e) {
                            // ignora
                        }
                    }
                });
                threads[i].start();
            }

            for (Thread t : threads) {
                t.join();
            }

            long fim = System.currentTimeMillis();
            imprimirLinhaEscalabilidade(n, inicio, fim);

            Thread.sleep(2000);
        }
    }

    // TESTE 3: ROBUSTEZ
    private static void testeRobustez() throws Exception {
        System.out.println("\n=== TESTE 3: ROBUSTEZ ===\n");

        System.out.println("Criando cliente bloqueado...");

        Thread clienteBloqueado = new Thread(new Runnable() {
            public void run() {
                try {
                    ClienteBloqueadoSimulador sim = new ClienteBloqueadoSimulador(HOST, PORT);
                    sim.enviarSemLer(50);
                } catch (Exception e) {
                    // ignora
                }
            }
        });

        clienteBloqueado.start();
        Thread.sleep(2000);

        System.out.println("Testando clientes normais...\n");

        int numClientesNormais = 5;
        resetMetricas();
        Thread[] threads = new Thread[numClientesNormais];

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < numClientesNormais; i++) {
            final int id = i;
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        BibliotecaCliente cliente = new BibliotecaCliente(HOST, PORT);
                        cliente.start();
                        cliente.login("user", "user");

                        for (int op = 0; op < 10; op++) {
                            long opInicio = System.nanoTime();
                            try {
                                cliente.registarVenda("ProdutoNormal", 1, 5.0);
                                Thread.sleep(10); // Delay
                                long opFim = System.nanoTime();
                                synchronized(lock) {
                                    tempoTotal += (opFim - opInicio) / 1_000_000;
                                    sucessos++;
                                }
                            } catch (Exception e) {
                                synchronized(lock) {
                                    falhas++;
                                }
                            }
                        }

                        cliente.close();
                        System.out.println("Cliente " + id + " completou");

                    } catch (Exception e) {
                        System.err.println("Cliente " + id + " falhou");
                    }
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join(30000);
        }

        long fim = System.currentTimeMillis();

        System.out.println("\n--- RESULTADOS ---");
        imprimirResultados("Robustez", numClientesNormais, 10, inicio, fim);

        if (sucessos > 0) {
            System.out.println("CONCLUSAO: Sistema manteve-se operacional");
        } else {
            System.out.println("CONCLUSAO: Sistema foi bloqueado");
        }
    }

    // TESTE 4: CACHE
    private static void testeCache() throws Exception {
        System.out.println("\n=== TESTE 5: CACHE ===\n");

        BibliotecaCliente cliente = new BibliotecaCliente(HOST, PORT);
        cliente.start();
        cliente.login("admin", "admin");

        System.out.println("Criando 7 dias de historico...");
        for (int dia = 0; dia < 7; dia++) {
            for (int i = 0; i < 20; i++) {
                cliente.registarVenda("Produto" + (i % 3), 1, 10.0);
            }
            if (dia < 6) cliente.novoDia();
        }
        System.out.println("Historico criado\n");

        System.out.println("Testando acesso a cache...");
        long inicioCache = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            cliente.consultarAgregacao(1, "Produto0", 1);
        }
        long fimCache = System.nanoTime();
        long tempoCache = (fimCache - inicioCache) / 1_000_000;

        System.out.println("Testando acesso a disco...");
        long inicioDisco = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            cliente.consultarAgregacao(1, "Produto0", 6);
        }
        long fimDisco = System.nanoTime();
        long tempoDisco = (fimDisco - inicioDisco) / 1_000_000;

        cliente.close();

        System.out.println("\n--- RESULTADOS ---");
        System.out.println("100 consultas (cache): " + tempoCache + " ms (" +
                (tempoCache/100.0) + " ms/op)");
        System.out.println("100 consultas (disco): " + tempoDisco + " ms (" +
                (tempoDisco/100.0) + " ms/op)");
        System.out.println("Overhead disco: " +
                String.format("%.1fx", (double)tempoDisco/tempoCache));
    }

    // TESTE 5: SUBSCRICOES
    private static void testeSubscricoes() throws Exception {
        System.out.println("\n=== TESTE 6: SUBSCRICOES ===\n");

        System.out.println("Teste 6.1: Vendas Simultaneas");

        BibliotecaCliente subscritor = new BibliotecaCliente(HOST, PORT);
        subscritor.start();
        subscritor.login("user", "user");

        final String[] resultado = new String[1];

        Thread threadSub = new Thread(new Runnable() {
            public void run() {
                try {
                    resultado[0] = subscritor.subscreverSimultaneas("ProdutoA", "ProdutoB");
                } catch (Exception e) {
                    resultado[0] = "ERRO";
                }
            }
        });

        threadSub.start();
        Thread.sleep(1000);

        BibliotecaCliente vendedor = new BibliotecaCliente(HOST, PORT);
        vendedor.start();
        vendedor.login("admin", "admin");

        long inicio = System.nanoTime();
        vendedor.registarVenda("ProdutoA", 1, 10.0);
        Thread.sleep(100);
        vendedor.registarVenda("ProdutoB", 1, 15.0);

        threadSub.join(5000);
        long tempo = (System.nanoTime() - inicio) / 1_000_000;

        System.out.println("Resultado: " + resultado[0]);
        System.out.println("Tempo: " + tempo + " ms\n");

        vendedor.close();
        subscritor.close();

        System.out.println("Teste 6.2: Vendas Consecutivas");

        BibliotecaCliente subscritor2 = new BibliotecaCliente(HOST, PORT);
        subscritor2.start();
        subscritor2.login("user", "user");

        final String[] resultado2 = new String[1];

        Thread threadSub2 = new Thread(new Runnable() {
            public void run() {
                try {
                    resultado2[0] = subscritor2.subscreverConsecutivas(3);
                } catch (Exception e) {
                    resultado2[0] = "ERRO";
                }
            }
        });

        threadSub2.start();
        Thread.sleep(1000);

        BibliotecaCliente vendedor2 = new BibliotecaCliente(HOST, PORT);
        vendedor2.start();
        vendedor2.login("admin", "admin");

        inicio = System.nanoTime();
        vendedor2.registarVenda("ProdutoC", 1, 5.0);
        vendedor2.registarVenda("ProdutoC", 1, 5.0);
        vendedor2.registarVenda("ProdutoC", 1, 5.0);

        threadSub2.join(5000);
        tempo = (System.nanoTime() - inicio) / 1_000_000;

        System.out.println("Resultado: " + resultado2[0]);
        System.out.println("Tempo: " + tempo + " ms");

        vendedor2.close();
        subscritor2.close();
    }

    // UTILITARIOS

    private static void resetMetricas() {
        synchronized(lock) {
            sucessos = 0;
            falhas = 0;
            tempoTotal = 0;
        }
    }

    private static void imprimirResultados(String nome, int clientes, int ops,
                                           long inicio, long fim) {
        int total;
        int suc;
        int fal;
        long tempo;

        synchronized(lock) {
            total = sucessos + falhas;
            suc = sucessos;
            fal = falhas;
            tempo = tempoTotal;
        }

        double tempoSeg = (fim - inicio) / 1000.0;
        double throughput = total / tempoSeg;
        double latencia = total > 0 ? tempo / (double)total : 0;
        double taxaSucesso = total > 0 ? (suc * 100.0) / total : 0;

        System.out.println("\n--- RESULTADOS: " + nome + " ---");
        System.out.println("Clientes: " + clientes);
        System.out.println("Operacoes: " + total + " (" + suc + " OK, " + fal + " FALHA)");
        System.out.println("Tempo total: " + String.format("%.2f", tempoSeg) + " s");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " op/s");
        System.out.println("Latencia media: " + String.format("%.2f", latencia) + " ms");
        System.out.println("Taxa sucesso: " + String.format("%.2f", taxaSucesso) + " %");
    }

    private static void imprimirLinhaEscalabilidade(int clientes, long inicio, long fim) {
        int total;
        long tempo;

        synchronized(lock) {
            total = sucessos + falhas;
            tempo = tempoTotal;
        }

        double tempoSeg = (fim - inicio) / 1000.0;
        double throughput = total / tempoSeg;
        double latencia = total > 0 ? tempo / (double)total : 0;
        double taxaSucesso = total > 0 ? (sucessos * 100.0) / total : 0;

        System.out.printf("%8d | %10.2f | %8.2f | %6.1f%%\n",
                clientes, throughput, latencia, taxaSucesso);
    }
}