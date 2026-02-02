package com.intuitivecare.ans;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServicoAgregacao {

    private record ChaveAgregacao(String razaoSocial, String uf) {}

    private record ResultadoAgregacao(String razaoSocial, String uf, double total, double media, double desvioPadrao) {}

    public void gerarRelatorioEstatistico(String arquivoEntrada, String arquivoSaida) {
        System.out.println("INICIANDO AGREGACAO E ESTATISTICA");

        try {
            Map<ChaveAgregacao, List<Double>> agrupamento = lerEAgrupar(arquivoEntrada);
            List<ResultadoAgregacao> resultados = calcularEstatisticas(agrupamento);
            ordenarPorTotalDecrescente(resultados);
            escreverRelatorio(resultados, arquivoSaida);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<ChaveAgregacao, List<Double>> lerEAgrupar(String arquivoEntrada) throws IOException {
        System.out.println("Lendo e agrupando dados...");
        Map<ChaveAgregacao, List<Double>> mapa = new HashMap<>();

        try (Reader reader = new FileReader(arquivoEntrada, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).build())) {

            for (CSVRecord record : parser) {
                String razao = record.get("RAZAO_SOCIAL");
                String uf = record.get("UF");
                String valorStr = record.get("VL_SALDO_FINAL");

                if (razao == null || razao.isEmpty() || valorStr == null) continue;

                try {
                    double valor = Double.parseDouble(valorStr.replace(",", "."));
                    ChaveAgregacao chave = new ChaveAgregacao(razao, uf);

                    mapa.computeIfAbsent(chave, k -> new ArrayList<>()).add(valor);

                } catch (NumberFormatException ignored) {
                }
            }
        }
        return mapa;
    }

    private List<ResultadoAgregacao> calcularEstatisticas(Map<ChaveAgregacao, List<Double>> agrupamento) {
        System.out.println("Calculando estatisticas...");
        List<ResultadoAgregacao> lista = new ArrayList<>();

        for (Map.Entry<ChaveAgregacao, List<Double>> entry : agrupamento.entrySet()) {
            ChaveAgregacao chave = entry.getKey();
            List<Double> valores = entry.getValue();

            double soma = 0.0;
            for (double v : valores) soma += v;

            double media = soma / valores.size();

            double somaQuadrados = 0.0;
            for (double v : valores) {
                somaQuadrados += Math.pow(v - media, 2);
            }
            double desvioPadrao = (valores.size() > 1) ? Math.sqrt(somaQuadrados / valores.size()) : 0.0;

            lista.add(new ResultadoAgregacao(chave.razaoSocial, chave.uf, soma, media, desvioPadrao));
        }
        return lista;
    }

    private void ordenarPorTotalDecrescente(List<ResultadoAgregacao> lista) {
        System.out.println("Ordenando resultados...");
        lista.sort((a, b) -> Double.compare(b.total, a.total));
    }

    private void escreverRelatorio(List<ResultadoAgregacao> lista, String arquivoSaida) throws IOException {
        System.out.println("Gerando relatorio estatistico: " + arquivoSaida);

        CSVFormat formato = CSVFormat.Builder.create()
                .setDelimiter(';')
                .setHeader("RAZAO_SOCIAL", "UF", "TOTAL_DESPESAS", "MEDIA_TRIMESTRAL", "DESVIO_PADRAO")
                .build();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(arquivoSaida), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, formato)) {

            for (ResultadoAgregacao r : lista) {
                printer.printRecord(
                        r.razaoSocial,
                        r.uf,
                        String.format(Locale.US, "%.2f", r.total),
                        String.format(Locale.US, "%.2f", r.media),
                        String.format(Locale.US, "%.2f", r.desvioPadrao)
                );
            }
        }
    }
}