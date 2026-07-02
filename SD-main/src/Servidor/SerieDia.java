package Servidor;

import Main.Event;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

public class SerieDia {

    private final List<Event> eventos = new ArrayList<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private boolean diaAberto = true;
    private final int diaID;
    private final Set<String> produtosVendidos = new HashSet<>();
    private String ultimoProduto;
    private int contadorConsecutivas = 0;
    private final Condition novaVendaOuFim = writeLock.newCondition();
    private final Map<String, Map<Integer, Double>> cacheAgregacoes = new HashMap<>();

    public SerieDia(int diaID) {
        this.diaID = diaID;
    }

    public void adicionarEvento(Event e) {
        writeLock.lock();
        try {
            if (!diaAberto) return;
            eventos.add(e);
            produtosVendidos.add(e.getNomeProduto());
            if (e.getNomeProduto().equals(ultimoProduto)) {
                contadorConsecutivas++;
            } else {
                ultimoProduto = e.getNomeProduto();
                contadorConsecutivas = 1;
            }
            novaVendaOuFim.signalAll();
        } finally {
            writeLock.unlock();
        }
    }

    public void fecharDia(){
        writeLock.lock();
        try {
            this.diaAberto = false;
            novaVendaOuFim.signalAll();
        } finally {
            writeLock.unlock();
        }
    }

    public boolean esperarVendasSimultaneas(String p1, String p2) throws InterruptedException {
        writeLock.lock();
        try {
            while (diaAberto && (!produtosVendidos.contains(p1) || !produtosVendidos.contains(p2))) {
                novaVendaOuFim.await();
            }
            return produtosVendidos.contains(p1) && produtosVendidos.contains(p2);
        } finally {
            writeLock.unlock();
        }
    }

    public String esperarVendasConsecutivas(int n) throws InterruptedException {
        writeLock.lock();
        try {
            while (diaAberto && contadorConsecutivas < n) {
                novaVendaOuFim.await();
            }

            if (contadorConsecutivas >= n) {
                return ultimoProduto;
            }
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    public int getQuantidade(String produto) {
        readLock.lock();
        try {
            if (!diaAberto && cacheAgregacoes.containsKey(produto)) {
                Map<Integer, Double> produtoCache = cacheAgregacoes.get(produto);
                if (produtoCache != null && produtoCache.containsKey(1)) {
                    return produtoCache.get(1).intValue();
                }
            }

            int total = 0;
            for (Event e : eventos) {
                if (e.getNomeProduto().equals(produto)) {
                    total += e.getQuantidade();
                }
            }

            if (!diaAberto) {
                cacheAgregacoes.computeIfAbsent(produto, k -> new HashMap<>()).put(1, (double) total);
            }

            return total;
        } finally {
            readLock.unlock();
        }
    }

    public double getVolume(String produto) {
        readLock.lock();
        try {
            if (!diaAberto && cacheAgregacoes.containsKey(produto)) {
                Map<Integer, Double> produtoCache = cacheAgregacoes.get(produto);
                if (produtoCache != null && produtoCache.containsKey(2)) {
                    return produtoCache.get(2);
                }
            }

            double total = 0.0;
            for (Event e : eventos) {
                if (e.getNomeProduto().equals(produto)) {
                    total += (e.getQuantidade() * e.getPrecoUnidade());
                }
            }

            if (!diaAberto) {
                cacheAgregacoes.computeIfAbsent(produto, k -> new HashMap<>()).put(2, total);
            }

            return total;
        } finally {
            readLock.unlock();
        }
    }

    public double getMaxPreco(String produto) {
        readLock.lock();
        try {
            if (!diaAberto && cacheAgregacoes.containsKey(produto)) {
                Map<Integer, Double> produtoCache = cacheAgregacoes.get(produto);
                if (produtoCache != null && produtoCache.containsKey(3)) {
                    return produtoCache.get(3);
                }
            }

            double max = -1.0;
            for (Event e : eventos) {
                if (e.getNomeProduto().equals(produto)) {
                    if (e.getPrecoUnidade() > max) {
                        max = e.getPrecoUnidade();
                    }
                }
            }

            if (!diaAberto) {
                cacheAgregacoes.computeIfAbsent(produto, k -> new HashMap<>()).put(3, max);
            }

            return max;
        } finally {
            readLock.unlock();
        }
    }

    public List<Event> filtrarEventos(Set<String> produtosInteresse) {
        readLock.lock();
        try {
            List<Event> resultado = new ArrayList<>();
            for (Event e : eventos) {
                if (produtosInteresse.contains(e.getNomeProduto())) {
                    resultado.add(e);
                }
            }
            return resultado;
        } finally {
            readLock.unlock();
        }
    }

    public void saveToDisk() throws IOException {
        readLock.lock();
        try {
            String filename = "dia_" + this.diaID + ".dat";
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)))) {
                out.writeInt(eventos.size());

                for (Event e : eventos) {
                    e.serialize(out);
                }

                out.writeBoolean(this.diaAberto);

                System.out.println("[SerieDia " + diaID + "] Persistido no disco.");
            }
        } finally {
            readLock.unlock();
        }
    }

    public static SerieDia loadFromDisk(int diaID) throws IOException {
        String filename = "dia_" + diaID + ".dat";
        File f = new File(filename);
        if (!f.exists()) return null;

        SerieDia dia = new SerieDia(diaID);

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)))) {
            int numEventos = in.readInt();

            for (int i = 0; i < numEventos; i++) {
                dia.eventos.add(Event.deserialize(in));
            }

            dia.diaAberto = in.readBoolean();
        }

        System.out.println("[SerieDia " + diaID + "] Carregado do disco.");
        return dia;
    }
}
