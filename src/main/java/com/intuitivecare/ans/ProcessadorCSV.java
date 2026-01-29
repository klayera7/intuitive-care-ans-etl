package com.intuitivecare.ans;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProcessadorCSV {

    public static class DadosDespesa {
        public String data;
        public String regAns;
        public String codigoConta;
        public String descricao;
        public double valor;

        public DadosDespesa(String data, String regAns, String codigoConta, String descricao, double valor) {
            this.data = data;
            this.regAns = regAns;
            this.codigoConta = codigoConta;
            this.descricao = descricao;
            this.valor = valor;
        }
    }

    public List<DadosDespesa> processarArquivo(File arquivo) {
        List<DadosDespesa> listaFiltrada = new ArrayList<>();

        System.out.println("Processando: " + arquivo.getName());

        CSVFormat format = CSVFormat.Builder.create()
                .setDelimiter(';')
                .setQuote('"')
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        // Encoding ISO-8859-1 para suportar acentos
        try (Reader reader = new InputStreamReader(new FileInputStream(arquivo), "ISO-8859-1");
             CSVParser parser = new CSVParser(reader, format)) {

            for (CSVRecord record : parser) {
                String descricao = record.get("DESCRICAO");
                String codigoConta = record.get("CD_CONTA_CONTABIL");
                String valorStr = record.get("VL_SALDO_FINAL");

                boolean ehDespesa = codigoConta.startsWith("4") ||
                        descricao.toUpperCase().contains("EVENTO") ||
                        descricao.toUpperCase().contains("SINISTRO");

                if (ehDespesa) {
                    double valor = parseValorMonetario(valorStr);

                    if (valor != 0) {
                        listaFiltrada.add(new DadosDespesa(
                                record.get("DATA"),
                                record.get("REG_ANS"),
                                codigoConta,
                                descricao,
                                valor
                        ));
                    }
                }
            }
            System.out.println("   Itens de despesa encontrados: " + listaFiltrada.size());

        } catch (Exception e) {
            System.err.println("Erro ao ler CSV: " + arquivo.getName() + " -> " + e.getMessage());
        }

        return listaFiltrada;
    }

    private double parseValorMonetario(String valor) {
        try {
            if (valor == null || valor.isEmpty()) return 0.0;
            String limpo = valor.replace(".", "").replace(",", ".");
            return Double.parseDouble(limpo);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}