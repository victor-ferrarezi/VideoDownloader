package com.videodownloader;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        int opcao;

        do {

            System.out.println("==============================");
            System.out.println("      Video Downloader");
            System.out.println("==============================");
            System.out.println("1- Download video");
            System.out.println("2- sair");
            System.out.println("Escolha uma opção: ");

            opcao = sc.nextInt();
            sc.nextLine();

            /// opcoes
            if (opcao == 1) {

                    String url;
                    /// loop url
                    do {
                        System.out.println("Digite uma URL valida: ");
                        url = sc.nextLine();

                    if (!YtValidator.isValid(url)) {
                        System.out.println("URL invalida. Tente novamente.");
                    }
                }while (!YtValidator.isValid(url));
                System.out.println("URL aceita!");
                System.out.println("URL: " + url);



            } else if (opcao == 2) {
                System.out.println("Sair");

            } else {
                System.out.println("invalido");
            }
        }
            /// loop invalido
            while (opcao != 2) ;

            sc.close();


    }
}
