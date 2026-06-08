package com.scmcloud.common.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 *
 *
 * @author Deng
 * createData 2025/11/10 10:36
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class PkceAuthorizationCodeTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate;
    private final PkceChallengeStore pkceChallengeStore; // 鑷畾涔夋帴鍙ｏ紝鐢ㄤ簬瀛樺偍/璇诲彇 code_challenge

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest grantRequest) {
        var registration = grantRequest.getClientRegistration();
        var authRequest = grantRequest.getAuthorizationExchange().getAuthorizationRequest();

        // 1锔忊儯 璇诲彇 code_verifier
        String codeVerifier = authRequest.getAttribute("code_verifier");

        if (registration.getClientSettings().isRequireProofKey() && codeVerifier == null) {
            throw new OAuth2AuthorizationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "Missing code_verifier", null)
            );
        }

        // 2锔忊儯 鏍￠獙 PKCE 鎸戞垬锟?
        String storedChallenge = pkceChallengeStore.load(authRequest.getAuthorizationRequestUri());
        if (storedChallenge == null) {
            throw new OAuth2AuthorizationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Missing stored code_challenge",
                            null)
            );
        }

        String computedChallenge = calculateCodeChallenge(codeVerifier);
        if (!computedChallenge.equals(storedChallenge)) {
            throw new OAuth2AuthorizationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Invalid code_verifier", null)
            );
        }

        // 3锔忊儯 楠岃瘉閫氳繃锛屼氦鐢遍粯璁ゅ鎴风鍙戞斁 Token
        return delegate.getTokenResponse(grantRequest);
    }

    /**
     * 璁＄畻 PKCE 锟絊HA-256 challenge 鍊硷紙Base64Url 缂栫爜锟?
     */
    private String calculateCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}