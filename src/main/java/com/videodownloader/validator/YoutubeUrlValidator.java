package com.videodownloader.validator;

import java.net.URI;

public class YoutubeUrlValidator {
    public static boolean isValid(String url){

        if (url == null || url.isEmpty()){
            return false;

        }

        try {
            URI uri = URI.create(url.strip());

            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null){
                return false;
            }

            boolean protocoloValido =
                    scheme.equals("http") || scheme.equals("https");

            return protocoloValido && hostPermitido (host.toLowerCase());

            // URI.create vai lança essa excecao quando o texto n for URL
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean hostPermitido (String host){
        return dominioOuSubdominio(host, "youtube.com")
                || dominioOuSubdominio(host, "youtu.be");

    }
    //aqui aceita youtube.com e n virusyoutube.com
    private static boolean dominioOuSubdominio (String host, String dominio){
        return host.equals(dominio) || host.endsWith("." + dominio);
    }

}