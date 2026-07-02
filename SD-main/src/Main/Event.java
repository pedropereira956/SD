package Main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Event {
    private String nomeProduto;
    private int quantidade;
    private double precoUnidade;

    public Event(String nomeProduto, int quantidade, double precoUnidade) {
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnidade = precoUnidade;
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeUTF(this.nomeProduto);
        out.writeInt(this.quantidade);
        out.writeDouble(this.precoUnidade);
    }

    public static Event deserialize(DataInputStream in) throws IOException {
        String nomeProduto = in.readUTF();
        int quantidade = in.readInt();
        double precoUnidade = in.readDouble();

        return new Event(nomeProduto, quantidade, precoUnidade);
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public double getPrecoUnidade() {
        return precoUnidade;
    }

    public String toString() {
        return "Venda: " + nomeProduto + " | Qtd: " + quantidade + " | Preço: " + precoUnidade;
    }

}
