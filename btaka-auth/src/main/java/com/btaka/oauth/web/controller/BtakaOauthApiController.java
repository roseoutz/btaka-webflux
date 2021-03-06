package com.btaka.oauth.web.controller;

import com.btaka.board.common.dto.ResponseDTO;
import com.btaka.domain.service.LoginService;
import com.btaka.domain.web.dto.AuthRequestDTO;
import com.btaka.oauth.factory.SnsServiceFactory;
import com.btaka.oauth.service.OauthSnsService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/sns")
public class BtakaOauthApiController {

    private final SnsServiceFactory snsServiceFactory;

    private final LoginService loginService;

    protected String newNonce() {
        return newState();
    }

    protected String newState() {
        String uuidStr = UUID.randomUUID().toString();

        return Base64.encodeBase64URLSafeString(uuidStr.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping
    public Mono<ResponseEntity<ResponseDTO>> allAuthUrl() {
        return Mono.just(snsServiceFactory.getAll())
                .publishOn(Schedulers.single())
                .map(snsServiceMap -> {
                    ResponseDTO.Builder builder = ResponseDTO.builder();
                    snsServiceMap.forEach((key, value) -> builder.set(key, value.getAuthUrl(newState(), newNonce())));
                    return ResponseEntity.ok(builder.build());
                });
    }

    @GetMapping("/{site}")
    public Mono<ResponseEntity<ResponseDTO>> authUrl(@PathVariable(name = "site") String site) {
        return Mono.just(site)
                .publishOn(Schedulers.single())
                .map(siteName -> snsServiceFactory.get(site))
                .filter(Objects::nonNull)
                .map(snsService -> {
                    String authUrl = snsService.getAuthUrl(newState(), newNonce());

                    return ResponseEntity.ok(ResponseDTO.builder().set("authUrl", authUrl).build());
                });
    }

    @GetMapping("/register/{site}/{userOid}")
    public Mono<ResponseEntity<ResponseDTO>> register(@PathVariable(name = "site") String site, @PathVariable(name = "userOid") String userOid, @RequestParam("code") String authCode, @RequestParam(name = "state", required = false) String state, ServerWebExchange serverWebExchange) {
        return Mono.just(site)
                .publishOn(Schedulers.single())
                .map(siteName -> snsServiceFactory.get(site))
                .filter(Objects::nonNull)
                .flatMap(snsService -> snsService.register(userOid, authCode, state, newNonce()))
                .filter(Objects::nonNull)
                .flatMap(snsUser -> loginService.authByOauth(serverWebExchange,
                        AuthRequestDTO.builder()
                                .isOauth(true)
                                .oauthId(snsUser.getId())
                                .token(snsUser.getToken())
                                .build()
                ))
                .flatMap(responseDTO -> Mono.just(ResponseEntity.ok(responseDTO)))
                .doOnError(throwable -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(throwable.getMessage()));
    }

    @GetMapping("/result/{site}")
    public Mono<ResponseEntity<ResponseDTO>> result(
            @PathVariable(name = "site") String site,
            @RequestParam(value = "code", required = false) String authCode,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "prompt", required = false) String prompt,
            ServerWebExchange serverWebExchange
    ) {
        if (!Objects.isNull(error) || !Objects.isNull(errorDescription)) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.builder().error(error).errorMessage(errorDescription).success(false).build()));
        }
        return Mono.just(site)
                .map(siteName -> snsServiceFactory.get(site))
                .filter(Objects::nonNull)
                .flatMap(snsService -> snsService.auth(authCode, state, newNonce()))
                .filter(Objects::nonNull)
                .flatMap(snsUser -> loginService.authByOauth(serverWebExchange,
                            AuthRequestDTO.builder().isOauth(true).oauthId(snsUser.getId()).token(snsUser.getToken()).build())
                            .map(responseDTO -> ResponseEntity.ok().body(responseDTO))
                )
                .doOnError(throwable -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(throwable.getMessage()));
    }

}
