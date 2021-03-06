package com.btaka.oauth.service.impl;

import com.btaka.board.common.dto.SnsUser;
import com.btaka.common.exception.BtakaException;
import com.btaka.config.OauthConfig;
import com.btaka.constant.AuthParamConst;
import com.btaka.constant.Social;
import com.btaka.domain.service.UserOauthService;
import com.btaka.domain.service.UserService;
import com.btaka.oauth.service.AbstractOauthSnsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.GsonJsonParser;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FacebookOauthSnsService extends AbstractOauthSnsService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GsonJsonParser gsonJsonParser = new GsonJsonParser();

    public FacebookOauthSnsService(UserService userService, UserOauthService userOauthService, String clientId, String clientSecret, String authUrl, String tokenUrl, String userInfoUrl, String redirectUri) {
        super(userService, userOauthService, clientId, clientSecret, authUrl, tokenUrl, userInfoUrl, redirectUri);
    }

    public FacebookOauthSnsService(UserService userService, UserOauthService userOauthService, String redirectUrl, OauthConfig.SocialInfo socialInfo) {
        super(userService, userOauthService, socialInfo.getClientId(), socialInfo.getClientSecret(), socialInfo.getAuthUrl(), socialInfo.getTokenUrl(), socialInfo.getUserInfoUrl(), redirectUrl);
    }

    private String getTokenUrl(String code, String state) {
        return tokenUrl + "?" + getTokenParam(code, state, "");
    }

    protected String getTokenParamMap(String code, String state, String grantType) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put(AuthParamConst.PARAM_OAUTH_GRANT_TYPE.getKey(), "code");
        paramMap.put(AuthParamConst.PARAM_OAUTH_CLIENT_ID.getKey(), getClientId());
        paramMap.put(AuthParamConst.PARAM_OAUTH_CLIENT_SECRET.getKey(), getClientSecret());
        paramMap.put(AuthParamConst.PARAM_OAUTH_AUTHORIZATION_CODE.getKey(), code);
        paramMap.put(AuthParamConst.PARAM_OAUTH_REDIRECT_URL.getKey(), getRedirectUri());
        return getTokenParamStr(paramMap);
    }

    @Override
    protected Mono<String> getAccessToken(String code, String state, String paramStr) {
        return getWebClient(getTokenUrl(code, state))
                .get()
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(respone -> logger.debug("[BTAKA Oauth Token Response]" + respone))
                .doOnError(BtakaException::new);
    }

    @Override
    public String getAuthUrl(String state, String nonce) {
        String url = authUrl +"?response_type=code&client_id=" + clientId + "&state=" + state + "&client_id = " + getClientId() +
                "&redirect_uri=" + URLEncoder.encode(redirectUri + "/" + getSite(), StandardCharsets.UTF_8);

        logger.debug("[BTAKA] facebook Auth Url = " + url);
        return url;
    }


    @Override
    public String getSite() {
        return Social.FACEBOOK.getName();
    }

    @Override
    protected SnsUser convertUserInfo(String token, Map<String, Object> userInfoMap) {

        String id = userInfoMap.get("id")+"";

        Map<String, String> kakaoAccountMap = ((Map<String,String>)userInfoMap.get("kakao_account"));
        String email = kakaoAccountMap.get("email");
        return SnsUser.builder()
                .token(token)
                .id(id)
                .email(email)
                .infoMap(convertObjectMap(kakaoAccountMap)).build();
    }

    @Override
    protected Map<String, Object> convertTokenToMap(String token) {
        return gsonJsonParser.parseMap(token);
    }
}
