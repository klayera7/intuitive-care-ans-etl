package com.intuitivecare.ans;

import java.util.InputMismatchException;

public class ValidadorDados {

    public static boolean isCNPJValido(String cnpj) {
        cnpj = cnpj.replaceAll("[^0-9]", "");

        if (cnpj.length() != 14 || cnpj.matches("(\\d)\\1{13}")) return false;

        char dig13, dig14;
        int sm, i, r, num, peso;

        try {
            sm = 0;
            peso = 2;
            for (i = 11; i >= 0; i--) {
                num = (int) (cnpj.charAt(i) - 48);
                sm = sm + (num * peso);
                peso = peso + 1;
                if (peso == 10) peso = 2;
            }

            r = sm % 11;
            if ((r == 0) || (r == 1)) dig13 = '0';
            else dig13 = (char) ((11 - r) + 48);

            sm = 0;
            peso = 2;
            for (i = 12; i >= 0; i--) {
                num = (int) (cnpj.charAt(i) - 48);
                sm = sm + (num * peso);
                peso = peso + 1;
                if (peso == 10) peso = 2;
            }

            r = sm % 11;
            if ((r == 0) || (r == 1)) dig14 = '0';
            else dig14 = (char) ((11 - r) + 48);

            return (dig13 == cnpj.charAt(12)) && (dig14 == cnpj.charAt(13));

        } catch (InputMismatchException erro) {
            return false;
        }
    }

    public static boolean isValorPositivo(double valor) {
        return valor > 0;
    }

    public static boolean isTextoValido(String texto) {
        return texto != null && !texto.trim().isEmpty() && texto.length() > 2;
    }
}