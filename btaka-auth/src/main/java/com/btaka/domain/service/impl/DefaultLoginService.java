package com.btaka.domain.service.impl;

import com.btaka.board.common.dto.ResponseDTO;
import com.btaka.board.common.dto.User;
import com.btaka.cache.dto.AuthInfo;
import com.btaka.cache.service.AuthCacheService;
import com.btaka.common.exception.BtakaException;
import com.btaka.constant.AuthErrorCode;
import com.btaka.domain.service.UserOauthService;
import com.btaka.domain.service.UserService;
import com.btaka.domain.web.dto.AuthRequestDTO;
import com.btaka.jwt.JwtService;
import com.btaka.domain.service.LoginService;
import com.mongodb.internal.HexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Service("defaultLoginService")
@RequiredArgsConstructor
public class DefaultLoginService implements LoginService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final UserOauthService userOauthService;
    private final AuthCacheService authCacheService;

    protected Mono<ResponseDTO> processLogin(User user, ServerWebExchange webExchange) {
        return Mono.just(user)
                .map(dto -> jwtService.generateToken(user))
                .flatMap(jwtDTO -> {
                    String sid = webExchange.getRequest().getId();
                    String encodeSid = HexUtils.toHex(sid.getBytes(StandardCharsets.UTF_8));
                    AuthInfo authInfo = AuthInfo.builder()
                            .userId(jwtDTO.getUserId())
                            .loginAt(jwtDTO.getLoginAt())
                            .expiredAt(jwtDTO.getExpiredAt())
                            .accessToken(jwtDTO.getAccessToken())
                            .build();
                    return authCacheService.saveAuthInfo(encodeSid, authInfo)
                            .doOnSuccess(cacheDTO -> webExchange.getResponse().addCookie(
                                    ResponseCookie
                                            .from("psid", encodeSid)
                                            .httpOnly(true)
                                            .build()))
                            .then(Mono.just(ResponseDTO.builder().set("oid", user.getOid()).set("accessToken", jwtDTO.getAccessToken()).build()));
                });
    }

    @Override
    public Mono<ResponseDTO> auth(ServerWebExchange webExchange, AuthRequestDTO authRequestDTO) {
        return userService.findByEmail(authRequestDTO.getEmail())
                .switchIfEmpty(Mono.error(new BtakaException(AuthErrorCode.USER_NOT_FOUND)))
                .filter(user -> passwordEncoder.matches(authRequestDTO.getPassword(), user.getPassword()))
                .switchIfEmpty(Mono.error(new BtakaException(AuthErrorCode.PASSWORD_NOT_MATCH)))
                .flatMap(user -> this.processLogin(user, webExchange))
                .onErrorResume(throwable ->
                    Mono.error(new BtakaException(HttpStatus.UNAUTHORIZED, throwable))
                );
    }

    @Override
    public Mono<ResponseDTO> authByOauth(ServerWebExchange webExchange, AuthRequestDTO authRequestDTO) {
        return userOauthService.getByOauthId(authRequestDTO.getOauthId())
                .filter(userOauthDTO -> !Objects.isNull(userOauthDTO.getOauthId()) && !Objects.isNull(userOauthDTO.getUserOid()))
                .switchIfEmpty(Mono.error(new BtakaException(AuthErrorCode.NOT_REGISTER_OAUTH_USER)))
                .flatMap(userOauthDTO -> userService.findByOid(userOauthDTO.getUserOid()))
                .flatMap(user -> this.processLogin(user, webExchange))
                .switchIfEmpty(Mono.just(new ResponseDTO()))
                .onErrorResume(throwable ->
                        Mono.error(new BtakaException(HttpStatus.INTERNAL_SERVER_ERROR, throwable))
                );
    }

    @Override
    public Mono<ResponseDTO> isLogin(String psid) {
        if (Objects.isNull(psid)) {
            return Mono.just(ResponseDTO.builder().success(false).build());
        }
        return authCacheService.isLogin(psid)
                .filter(authCacheDTO -> !Objects.isNull(authCacheDTO.getSid()))
                .flatMap(authCacheDTO -> Mono.just(ResponseDTO.builder().set("userId", authCacheDTO.getAuthInfo().getUserId()).set("accessToken", authCacheDTO.getAuthInfo().getAccessToken()).build()))
                .switchIfEmpty(Mono.just(ResponseDTO.builder().success(false).build()));
    }

}