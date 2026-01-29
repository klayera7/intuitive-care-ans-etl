package com.intuitivecare.ans;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class Auditoria {

    public void realizarAuditoria(String caminhoArquivo) {
        System.out.println("\n --- INICIANDO AUDITORIA DE DADOS ---");
        File arquivo = new File(caminhoArquivo);

        if (!arquivo.exists()) {
            System.err.println("❌ Arquivo para auditoria não encontrado.");
            return;
        }

        int linhasTotais = 0;
        int linhasComGlosa = 0;
        double somaTotal = 0.0;
        Set<String> operadorasUnicas = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha = br.readLine();

            while ((linha = br.readLine()) != null) {
                linhasTotais++;
                String[] colunas = linha.split(";");

                if (colunas.length < 5) {
                    System.out.println("⚠️ Inconsistência na linha " + linhasTotais + ": Colunas faltando.");
                    continue;
                }

                String regAns = colunas[1];
                String valorStr = colunas[4];

                operadorasUnicas.add(regAns);

                try {
                    double valor = Double.parseDouble(valorStr.replace(",", "."));
                    somaTotal += valor;
                    if (valor < 0) linhasComGlosa++;
                } catch (NumberFormatException e) {
                    System.out.println("Valor inválido na linha " + linhasTotais + ": " + valorStr);
                }
            }

            System.out.println("Auditoria Concluída com Sucesso!");
            System.out.println("RELATÓRIO:");
            System.out.println("   - Total de Registros Processados: " + linhasTotais);
            System.out.println("   - Operadoras Distintas Encontradas: " + operadorasUnicas.size());
            System.out.println("   - Registros de Glosas/Estornos (Negativos): " + linhasComGlosa);
            System.out.println("   - Soma Total dos Valores (R$): " + String.format("%.2f", somaTotal));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}