package Servidor;

import Main.Event;

import java.util.LinkedList;
import java.util.Queue;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Gestor {

    private final Map<String, String> utilizadores = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final int D_DIAS;
    private final int S_MEMORIA;
    private final SerieDia[] historicoDias;
    private int diaCorrenteIndex;
    private int totalDiasPassados = 0;
    private final Lock timeLock = new ReentrantLock();
    private final Queue<Integer> diasEmMemoria = new LinkedList<>();
    private final int[] mapDiaIDs;

    public Gestor(int D, int S) {
        utilizadores.put("admin", "admin");
        utilizadores.put("user", "user");

        this.D_DIAS = D;
        this.S_MEMORIA = S;
        this.historicoDias = new SerieDia[D];

        this.mapDiaIDs = new int[D];
        Arrays.fill(mapDiaIDs, -1);

        this.diaCorrenteIndex = 0;
        this.historicoDias[0] = new SerieDia(totalDiasPassados);
        this.mapDiaIDs[0] = totalDiasPassados;
    }

    public boolean autenticar(String user, String pass) {
        readLock.lock();
        try {
            System.out.println("[GESTOR] A tentar login: " + user);
            return utilizadores.containsKey(user) && utilizadores.get(user).equals(pass);
        } finally {
            readLock.unlock();
        }
    }

    public boolean registar(String user, String pass) {
        writeLock.lock();
        try {
            System.out.println("[GESTOR] A tentar registo: " + user);

            if (!utilizadores.containsKey(user)) {
                utilizadores.put(user, pass);
                return true;
            }
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    public void registarEvento(Main.Event e) {
        timeLock.lock();
        try {
            historicoDias[diaCorrenteIndex].adicionarEvento(e);
        } finally {
            timeLock.unlock();
        }
        System.out.println("[GESTOR] Evento recebido: " + e.toString());
    }

    public void novoDia() {
        timeLock.lock();
        try {
            SerieDia diaQueAcabou = historicoDias[diaCorrenteIndex];
            if (diaQueAcabou != null) {
                diaQueAcabou.fecharDia();
                try { diaQueAcabou.saveToDisk(); } catch (Exception e) {}

                diasEmMemoria.add(diaCorrenteIndex);

                if (diasEmMemoria.size() > S_MEMORIA) {
                    int indexVitima = diasEmMemoria.poll();
                    historicoDias[indexVitima] = null;
                    System.out.println("[CACHE] Novo dia forçou expulsão do índice " + indexVitima);
                }
            }

            totalDiasPassados++;
            diaCorrenteIndex = totalDiasPassados % D_DIAS;

            historicoDias[diaCorrenteIndex] = new SerieDia(totalDiasPassados);
            mapDiaIDs[diaCorrenteIndex] = totalDiasPassados;
            System.out.println(">>> NOVO DIA: " + totalDiasPassados);

        } finally {
            timeLock.unlock();
        }
    }

    public boolean subscreverVendasSimultaneas(String p1, String p2) throws InterruptedException {
        SerieDia diaAlvo;
        timeLock.lock();
        try {
            diaAlvo = historicoDias[diaCorrenteIndex];
        } finally {
            timeLock.unlock();
        }
        return diaAlvo.esperarVendasSimultaneas(p1, p2);
    }

    public String subscreverVendasConsecutivas(int n) throws InterruptedException {
        SerieDia diaAlvo;
        timeLock.lock();
        try {
            diaAlvo = historicoDias[diaCorrenteIndex];
        } finally {
            timeLock.unlock();
        }

        return diaAlvo.esperarVendasConsecutivas(n);
    }

    public double agregar(int tipo, String produto, int diasParaTras){
        timeLock.lock();
        try {
            double acumulador = 0;
            double maxGlobal = -1;

            double totalVolumeMedia = 0;
            double totalQtdMedia = 0;

            if (diasParaTras >= D_DIAS) diasParaTras = D_DIAS - 1;

            for (int i = 1; i <= diasParaTras; i++) {
                int indexAlvo = (diaCorrenteIndex - i + D_DIAS) % D_DIAS;
                SerieDia dia = obterSerieDia(indexAlvo);

                if (dia == null) continue;

                switch (tipo) {
                    case 1:
                        acumulador += dia.getQuantidade(produto);
                        break;
                    case 2:
                        acumulador += dia.getVolume(produto);
                        break;
                    case 3:
                        double maxDia = dia.getMaxPreco(produto);
                        if (maxDia > maxGlobal) maxGlobal = maxDia;
                        break;
                    case 4:
                        totalVolumeMedia += dia.getQuantidade(produto);
                        totalQtdMedia += dia.getVolume(produto);
                        break;
                }
            }

            if (tipo == 3) return maxGlobal;

            if (tipo == 4){
                if (totalQtdMedia == 0) return 0.0;
                return totalVolumeMedia / totalQtdMedia;
            }
            return acumulador;

        } finally {
            timeLock.unlock();
        }
    }

    public List<Event> getEventosFiltrados(int diasParaTras, Set<String> produtos) {
        timeLock.lock();
        try {
            if (diasParaTras >= D_DIAS) diasParaTras = D_DIAS - 1;
            if (diasParaTras < 1) return new ArrayList<>();

            int indexAlvo = (diaCorrenteIndex - diasParaTras + D_DIAS) % D_DIAS;
            SerieDia dia = obterSerieDia(indexAlvo);

            if (dia == null) {
                return new ArrayList<>();
            }

            return dia.filtrarEventos(produtos);

        } finally {
            timeLock.unlock();
        }
    }

    private SerieDia obterSerieDia(int index) {
        timeLock.lock();
        try {
            if (index == diaCorrenteIndex) {
                return historicoDias[index];
            }

            if (historicoDias[index] != null) {
                return historicoDias[index];
            }

            System.out.println("[CACHE] O dia no índice " + index + " está no disco. A carregar...");

            if (diasEmMemoria.size() >= S_MEMORIA) {

                int indexVitima = diasEmMemoria.poll();

                try {
                    if (historicoDias[indexVitima] != null) {
                        historicoDias[indexVitima].saveToDisk();
                        historicoDias[indexVitima] = null;
                        System.out.println("[CACHE] Memória cheia. Dia (index " + indexVitima + ") foi para o disco.");
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao persistir dia vítima: " + e.getMessage());
                }
            }

            try {

                int idDoDia = calcularIDPeloIndex(index);

                SerieDia carregado = SerieDia.loadFromDisk(idDoDia);
                if (carregado == null) return null;

                historicoDias[index] = carregado;
                diasEmMemoria.add(index);

                return carregado;

            } catch (IOException e) {
                System.err.println("Erro ao carregar dia do disco: " + e.getMessage());
                return null;
            }

        } finally {
            timeLock.unlock();
        }
    }

    private int calcularIDPeloIndex(int index) {
        return this.mapDiaIDs[index];
    }
}

