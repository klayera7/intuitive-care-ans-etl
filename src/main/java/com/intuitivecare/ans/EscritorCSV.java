package com.intuitivecare.ans;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class EscritorCSV {

    public void salvarArquivoConsolidado(List<ProcessadorCSV.DadosDespesa> dados, String caminhoArquivo) {
        System.out.println("Iniciando gravacao do arquivo consolidado: " + caminhoArquivo);

        CSVFormat format = CSVFormat.Builder.create()
                .setDelimiter(';')
                .setQuote('"')
                .setHeader("DATA", "REG_ANS", "CD_CONTA_CONTABIL", "DESCRICAO", "VL_SALDO_FINAL")
                .build();

        try (FileWriter writer = new FileWriter(caminhoArquivo);
             CSVPrinter printer = new CSVPrinter(writer, format)) {

            for (ProcessadorCSV.DadosDespesa linha : dados) {
                printer.printRecord(
                        linha.data,
                        linha.regAns,
                        linha.codigoConta,
                        linha.descricao,
                        String.format("%.2f", linha.valor).replace('.', ',')
                );
            }

            System.out.println("Arquivo gerado com sucesso! Total de linhas: " + dados.size());

        } catch (IOException e) {
            System.err.println("Erro ao gravar arquivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}