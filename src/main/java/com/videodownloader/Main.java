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

            /// criando opcoes
            opcao = sc.nextInt();
            if (opcao == 1) {
                System.out.println("Escolha uma opção: ");

            } else if (opcao == 2) {
                System.out.println("Sair");

            } else {
                System.out.println("invalido");
            }
        }

            while (opcao != 2) ;

            sc.close();


    }
}
