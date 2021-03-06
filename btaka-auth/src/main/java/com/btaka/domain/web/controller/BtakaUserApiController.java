package com.btaka.domain.web.controller;

import com.btaka.board.common.dto.ResponseDTO;
import com.btaka.board.common.dto.User;
import com.btaka.common.exception.BtakaException;
import com.btaka.constant.AuthErrorCode;
import com.btaka.constant.UserParamConst;
import com.btaka.domain.service.UserService;
import com.btaka.domain.web.dto.PasswordChangeRequestDTO;
import com.btaka.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user")
public class BtakaUserApiController {

    private final UserService userService;
    private final JwtService jwtService;

    /*
     * Todo: Security를 통하여 header에 jwt Token을 검증한다..
     *  현재 AccessToken 이 없을 시 oid로 정보 주게 처리해놨는데 향후 accessToken을 이용하여 가져올수 있게 수정
     */

    @GetMapping("/{oid}")
    public Mono<ResponseEntity<ResponseDTO>> get(@PathVariable(name = "oid") String oid, ServerWebExchange webExchange) {
        return  userService.findByOid(oid)
                .map(user -> ResponseEntity.ok(
                                ResponseDTO.builder()
                                        .set(UserParamConst.PARAM_USER_INFO.getKey(), user)
                                        .build()))
                .switchIfEmpty(Mono.error(new BtakaException(AuthErrorCode.USER_NOT_FOUND)));
        /*
        return Mono.just(webExchange)
                .map(exchange -> exchange.getRequest().getHeaders())
                .filter(headers -> headers.containsKey("Authorization"))
                .map(headers -> Objects.requireNonNull(headers.get("Authorization")).get(0))
                .filter(jwtService::isValidToken)
                .map(token -> {
                    Claims claims = jwtService.getClaims(token);
                    Map<String, Object>  userInfo = (Map<String, Object>) Collections.unmodifiableMap(claims.get("userinfo", Map.class));
                    return ResponseEntity.ok(new ResponseDTO(userInfo));
                }).switchIfEmpty(Mono.just(oid)
                        .filter(userOid -> !Objects.isNull(oid))
                        .flatMap(userService::findByOid)
                        .map(user -> ResponseEntity.ok(ResponseDTO.builder().build()))
                        .switchIfEmpty(Mono.just(ResponseEntity.internalServerError().body(ResponseDTO.builder().error("user_not_found").errorMessage("user not found").success(false).statusCode(500).build())))
                );
         */
    }

    @PostMapping
    public Mono<ResponseEntity<ResponseDTO>> changePassword(@RequestBody PasswordChangeRequestDTO passwordChangeRequestDTO) {
        return userService.changePassword(passwordChangeRequestDTO)
                .map(userInfo -> ResponseEntity.ok(
                        ResponseDTO.builder()
                                .set(UserParamConst.PARAM_USER_INFO.getKey(), userInfo)
                                .build())
                );
    }

    @PutMapping("/{oid}")
    public Mono<ResponseEntity<ResponseDTO>> put(@PathVariable(name = "oid") String oid, @RequestBody User user) {
        return userService.updateUser(oid, user)
                .map(userInfo -> ResponseEntity.ok(
                        ResponseDTO.builder()
                                .set(UserParamConst.PARAM_USER_INFO.getKey(), userInfo)
                                .build())
                );
    }
}
