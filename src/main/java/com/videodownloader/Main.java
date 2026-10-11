package com.videodownloader;

import com.videodownloader.service.downloadService;
import com.videodownloader.service.ytService;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        int opcao = 0;

        while (opcao != 4){

            System.out.println("==============================");
            System.out.println("      Video Downloader");
            System.out.println("==============================");
            System.out.println("1- Download video");
            System.out.println("2- Listar formatos");
            System.out.println("3- Baixar melhor formato");
            System.out.println("4- sair");
            System.out.println("Escolha uma opção: ");

            /// opcoes
            if (!sc.hasNextInt()) {
                System.out.println("Digite uma opção numérica: ");
                sc.nextLine();
                continue;
            }

            opcao = sc.nextInt();
            sc.nextLine();

            if (opcao == 4) {

                System.out.println("Encerrando...");
                break;
            }

            if (opcao < 1 || opcao > 4) {
                System.out.println("Opção inválida...");
                continue;
            }

            System.out.println("Digite a URL: ");
            String url = sc.nextLine().strip();

            if (!YtValidator.isValid(url)) {
                System.out.println("URL invalida. Tente novamente.");
                continue;
            }
            try {
                switch (opcao) {
                    case 1 -> {
                        VideoInfo video = ytService.buscarInformacoes(url);

                        System.out.println("Título: " + video.getTitulo());
                        System.out.println("Canal: " + video.getCanal());
                        System.out.println("Duração: " + video.getDuracaoFormatada());

                    }
                    case 2 -> {
                        System.out.println("Buscando formatos disponíveis...");

                        List<VideoFormat> formatos =
                                ytService.buscarFormatos(url);

                        if (formatos.isEmpty()) {
                            System.out.println(
                                    "Nenhum formato de video foi encontrado."
                            );
                        } else {
                            System.out.println("\nformatos disponíveis");
                            for (VideoFormat formato : formatos) {
                                System.out.println(formato);
                            }
                            System.out.println("\nTotal de formatos: " + formatos.size());
                        }
                    }


                    case 3 -> downloadService.baixarVideo(url);
                }
            } catch (IOException e) {
                System.out.println("Erro ao baixar. " +
                        e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Download Interrompido " +
                        e.getMessage());
            }
        }
        sc.close();
    }
}


