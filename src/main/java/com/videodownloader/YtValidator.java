package com.videodownloader;

public class YtValidator {
    public static boolean isValid(String url){
        return url.contains("youtube.com")
                || url.contains("youtu.be");
    }
}
