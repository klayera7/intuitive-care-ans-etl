package com.intuitivecare.ans;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ServicoEnriquecimento {

    private static final String CADASTRO_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/operadoras_de_plano_de_saude_ativas/";
    private static final String ARQUIVO_CADASTRO = "cadastro_operadoras.csv";

    private record DadosCadastrais(String registroAns, String cnpj, String razaoSocial, String modalidade, String uf) {}

    public void executarEnriquecimento(String arquivoEntrada, String arquivoSaida) {
        System.out.println("INICIANDO ENRIQUECIMENTO DE DADOS");

        try {
            baixarArquivoCadastro();
            Map<String, DadosCadastrais> mapaOperadoras = carregarMapaOperadoras();
            realizarJoin(arquivoEntrada, arquivoSaida, mapaOperadoras);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void baixarArquivoCadastro() throws IOException {
        System.out.println("Buscando arquivo de cadastro na ANS...");
        Document doc = Jsoup.connect(CADASTRO_URL).timeout(30000).get();
        Elements links = doc.select("a[href$='.csv']");

        String urlCsv = null;
        for (Element link : links) {
            if (link.attr("href").toLowerCase().contains("relatorio_cadop")) {
                urlCsv = CADASTRO_URL + link.attr("href");
                break;
            }
        }

        if (urlCsv != null) {
            System.out.println("Baixando cadastro: " + urlCsv);
            FileUtils.copyURLToFile(new URL(urlCsv), new File(ARQUIVO_CADASTRO));
        } else {
            throw new IOException("Link do CSV de cadastro nao encontrado.");
        }
    }

    private Map<String, DadosCadastrais> carregarMapaOperadoras() throws IOException {
        System.out.println("Carregando cadastro em memoria...");
        Map<String, DadosCadastrais> mapa = new HashMap<>();

        try (Reader reader = new InputStreamReader(new FileInputStream(ARQUIVO_CADASTRO), "ISO-8859-1");
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).build())) {

            for (CSVRecord record : parser) {
                String regAns = "";
                if (record.isMapped("REGISTRO_OPERADORA")) {
                    regAns = record.get("REGISTRO_OPERADORA");
                } else if (record.isMapped("Registro_ANS")) {
                    regAns = record.get("Registro_ANS");
                }

                if (regAns == null || regAns.isEmpty()) continue;

                DadosCadastrais dados = new DadosCadastrais(
                        regAns,
                        record.isMapped("CNPJ") ? record.get("CNPJ") : "",
                        record.isMapped("Razao_Social") ? record.get("Razao_Social") : "",
                        record.isMapped("Modalidade") ? record.get("Modalidade") : "",
                        record.isMapped("UF") ? record.get("UF") : ""
                );

                mapa.put(regAns, dados);
            }
        }
        System.out.println("Operadoras carregadas no mapa: " + mapa.size());
        return mapa;
    }

    private void realizarJoin(String entrada, String saida, Map<String, DadosCadastrais> mapa) throws IOException {
        System.out.println("Cruzando dados...");

        CSVFormat formatoSaida = CSVFormat.Builder.create()
                .setDelimiter(';')
                .setHeader("DATA", "REG_ANS", "CD_CONTA_CONTABIL", "DESCRICAO", "VL_SALDO_FINAL", "CNPJ", "RAZAO_SOCIAL", "MODALIDADE", "UF")
                .build();

        try (Reader reader = new FileReader(entrada);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true).build());
             Writer writer = new OutputStreamWriter(new FileOutputStream(saida), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, formatoSaida)) {

            int totalProcessado = 0;
            int semMatch = 0;

            for (CSVRecord linha : parser) {
                totalProcessado++;
                String regAns = linha.get("REG_ANS");

                DadosCadastrais infoExtra = mapa.get(regAns);

                String cnpj = "";
                String razao = "";
                String modalidade = "";
                String uf = "";

                if (infoExtra != null) {
                    cnpj = infoExtra.cnpj;
                    razao = infoExtra.razaoSocial;
                    modalidade = infoExtra.modalidade;
                    uf = infoExtra.uf;
                } else {
                    semMatch++;
                    razao = "NAO_ENCONTRADO_NO_CADASTRO";
                }

                printer.printRecord(
                        linha.get("DATA"),
                        regAns,
                        linha.get("CD_CONTA_CONTABIL"),
                        linha.get("DESCRICAO"),
                        linha.get("VL_SALDO_FINAL"),
                        cnpj,
                        razao,
                        modalidade,
                        uf
                );
            }

            System.out.println("Enriquecimento concluido!");
            System.out.println("   - Linhas processadas: " + totalProcessado);
            System.out.println("   - Registros sem match no cadastro: " + semMatch);
        }
    }
}