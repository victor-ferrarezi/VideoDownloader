package com.videodownloader;

import com.videodownloader.model.VideoFormat;
import com.videodownloader.model.VideoInfo;
import com.videodownloader.service.DownloadService;
import com.videodownloader.service.YtService;
import com.videodownloader.validator.YtValidator;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        int opcao = 0;

        while (opcao != 5){

            System.out.println("==============================");
            System.out.println("       Video Downloader       ");
            System.out.println("==============================");
            System.out.println("1- Download video");
            System.out.println("2- Listar formatos");
            System.out.println("3- Baixar melhor formato");
            System.out.println("4- Escolher formato e baixar");
            System.out.println("5- sair");
            System.out.println("Escolha uma opção: ");

            /// opcoes
            if (!sc.hasNextInt()) {
                System.out.println("Digite uma opção numérica: ");
                sc.nextLine();
                continue;
            }

            opcao = sc.nextInt();
            sc.nextLine();

            if (opcao == 5) {

                System.out.println("Encerrando...");
                break;
            }

            if (opcao < 1 || opcao > 5) {
                System.out.println("Opção inválida...");
                break;
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
                        VideoInfo video = YtService.buscarInformacoes(url);

                        System.out.println("Título: " + video.getTitulo());
                        System.out.println("Canal: " + video.getCanal());
                        System.out.println("Duração: " + video.getDuracaoFormatada());

                    }
                    case 2 -> {
                        System.out.println("Buscando formatos disponíveis...");

                        List<VideoFormat> formatos =
                                YtService.buscarFormatos(url);

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


                    case 3 -> DownloadService.baixarVideo(url);

                    case 4 -> {
                        List<VideoFormat> formatos =
                                YtService.buscarFormatos(url);

                        if (formatos.isEmpty()) {
                            System.out.println("Nenhum formato de video foi encontrado.");
                            break;
                        }

                        System.out.println("\nEscolha um formato");

                        for (int i = 0; i < formatos.size(); i++) {
                            System.out.println((i + 1) + " - " + formatos.get(i));
                        }

                        System.out.println("Numero do formato: ");

                        if (!sc.hasNextInt()) {
                            System.out.println("Digite um numero válido");
                            sc.nextLine();
                            break;
                        }

                        int escolha = sc.nextInt();
                        sc.nextLine();

                        if (escolha < 1 || escolha > formatos.size()) {
                            System.out.println("Formato inválido");
                            break;
                        }

                        VideoFormat selecionado = formatos.get(escolha - 1);

                        System.out.println("Formato escolhido: " + selecionado);

                        DownloadService.baixarFormato(
                                url,
                                selecionado.getId(),
                                selecionado.isTemAudio()
                        );
                    }
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


