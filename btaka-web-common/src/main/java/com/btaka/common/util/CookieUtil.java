package com.btaka.common.util;

import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;

public class CookieUtil {

    private CookieUtil() {}

    public static void saveSessionCookie(ServerHttpResponse serverResponse, String key, String value) {
        serverResponse.addCookie
                (
                        ResponseCookie
                                .from(key, value)
                                .httpOnly(true)
                                .build()
                );
    }

    public static void saveCookie(ServerHttpResponse serverResponse, String key, String value, long duration) {
        serverResponse.addCookie
                (
                        ResponseCookie
                                .from(key, value)
                                .httpOnly(true)
                                .maxAge(duration)
                                .build()
                );
    }

    public static void deleteCookie(ServerHttpResponse serverResponse, String key) {
        serverResponse.addCookie
                (
                        ResponseCookie
                                .from(key, "-")
                                .maxAge(0)
                                .httpOnly(true)
                                .build()
                );
    }
}
