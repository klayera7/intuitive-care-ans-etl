package com.intuitivecare.ans;

import org.apache.commons.io.FileUtils;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {

    private static final String BASE_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/";
    private static final String DOWNLOAD_DIR = "downloads_ans";

    public static void main(String[] args) {
        System.out.println("Iniciando o Sistema ETL...");

        try {
            String accountingUrl = findLinkByText(BASE_URL, "demonstracoes_contabeis");
            if (accountingUrl == null) {
                System.err.println("Erro: Pasta nao encontrada.");
                return;
            }
            System.out.println("Pasta encontrada: " + accountingUrl);

            List<String> targetZipUrls = getLatest3Quarters(accountingUrl);

            for (String zipUrl : targetZipUrls) {
                downloadAndUnzip(zipUrl);
            }

            System.out.println("\nIniciando Processamento e Consolidacao dos Dados...");

            ProcessadorCSV processador = new ProcessadorCSV();
            List<ProcessadorCSV.DadosDespesa> todosDados = new ArrayList<>();

            File pastaExtracted = new File(DOWNLOAD_DIR + "/extracted");
            File[] arquivosCSV = pastaExtracted.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

            if (arquivosCSV != null && arquivosCSV.length > 0) {
                for (File arquivo : arquivosCSV) {
                    todosDados.addAll(processador.processarArquivo(arquivo));
                }
            } else {
                System.err.println("Nenhum arquivo CSV encontrado para processar.");
            }

            System.out.println("Total Consolidado: " + todosDados.size() + " registros de despesas carregados.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String resolveUrl(String base, String rel) {
        if (rel.startsWith("http")) return rel;
        return base + (base.endsWith("/") ? "" : "/") + rel;
    }

    private static String findLinkByText(String url, String partialText) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            if (link.attr("href").toLowerCase().contains(partialText.toLowerCase())) {
                return resolveUrl(url, link.attr("href"));
            }
        }
        return null;
    }

    private static List<String> getLatest3Quarters(String baseUrl) throws IOException {
        System.out.println("Buscando os ultimos 3 arquivos ZIP...");
        Document doc = Jsoup.connect(baseUrl).get();
        Elements links = doc.select("a[href]");
        TreeMap<Integer, String> yearsMap = new TreeMap<>(Collections.reverseOrder());

        for (Element link : links) {
            String text = link.text().trim().replaceAll("[/\\\\]", "");
            if (text.matches("\\d{4}")) {
                int year = Integer.parseInt(text);
                yearsMap.put(year, resolveUrl(baseUrl, link.attr("href")));
            }
        }

        List<String> zipUrlsToProcess = new ArrayList<>();
        int count = 0;

        for (Map.Entry<Integer, String> entry : yearsMap.entrySet()) {
            if (count >= 3) break;
            int year = entry.getKey();
            String yearUrl = entry.getValue();

            if (!yearUrl.endsWith("/")) yearUrl += "/";

            System.out.println("Verificando ano: " + year);
            Document yearDoc = Jsoup.connect(yearUrl).get();
            Elements zipLinks = yearDoc.select("a[href$='.zip']");
            List<String> yearZips = new ArrayList<>();

            for (Element zipLink : zipLinks) {
                String href = zipLink.attr("href");
                if (href.toUpperCase().matches(".*\\d{1}T.*")) {
                    yearZips.add(href);
                }
            }
            yearZips.sort(Collections.reverseOrder());

            for (String zipName : yearZips) {
                if (count >= 3) break;
                zipUrlsToProcess.add(resolveUrl(yearUrl, zipName));
                System.out.println("   -> Alvo identificado: " + zipName);
                count++;
            }
        }
        return zipUrlsToProcess;
    }

    private static void downloadAndUnzip(String zipUrl) throws IOException {
        String fileName = zipUrl.substring(zipUrl.lastIndexOf("/") + 1);
        File zipFile = new File(DOWNLOAD_DIR, fileName);

        if (!zipFile.exists()) {
            System.out.println("Baixando: " + fileName);
            FileUtils.copyURLToFile(new URL(zipUrl), zipFile);
        } else {
            System.out.println("Arquivo ja baixado: " + fileName);
        }

        System.out.println("Extraindo: " + fileName);
        unzip(zipFile.getAbsolutePath(), DOWNLOAD_DIR + "/extracted");
    }

    private static void unzip(String zipFilePath, String destDir) throws IOException {
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(destDir, zipEntry.getName());
                if (!newFile.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
                    throw new IOException("Entrada ZIP fora do destino");
                }
                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }
}