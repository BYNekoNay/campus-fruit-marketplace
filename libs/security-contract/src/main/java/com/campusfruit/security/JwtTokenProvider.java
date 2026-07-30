package com.campusfruit.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * JWT 令牌工具类。
 * <p>
 * 提供从 JWT access_token 中提取用户 ID、角色以及验证令牌的方法。
 * 供 Gateway 等依赖模块使用，避免它们直接依赖 identity-service。
 * <p>
 * 提供两类方法：
 * <ul>
 *   <li>无公钥参数：仅解析 JWT 格式，提取 claims，不验证签名</li>
 *   <li>带公钥参数：完整验证签名和过期时间</li>
 * </ul>
 */
public final class JwtTokenProvider {

    private JwtTokenProvider() {
        // 工具类禁止实例化
    }

    // ========== 无签名验证（格式解析） ==========

    /**
     * 从 JWT 令牌中提取用户 ID (subject)，不验证签名。
     *
     * @param token JWT 令牌字符串
     * @return subject（用户 ID），解析失败返回空
     */
    public static Optional<String> getUserIdFromToken(String token) {
        return parseTokenWithoutVerification(token).map(JWTClaimsSet::getSubject);
    }

    /**
     * 从 JWT 令牌中提取角色列表，不验证签名。
     *
     * @param token JWT 令牌字符串
     * @return 角色列表，解析失败返回空列表
     */
    public static List<String> getRolesFromToken(String token) {
        return parseTokenWithoutVerification(token)
                .map(claims -> {
                    Object rolesObj = claims.getClaim("roles");
                    if (rolesObj instanceof List<?> list) {
                        return list.stream().map(Object::toString).toList();
                    }
                    if (rolesObj instanceof String rolesStr) {
                        return Arrays.asList(rolesStr.split(","));
                    }
                    return Collections.<String>emptyList();
                })
                .orElse(Collections.emptyList());
    }

    // ========== 带签名验证 ==========

    /**
     * 从 JWT 令牌中提取用户 ID (subject)。
     *
     * @param token      JWT 令牌字符串
     * @param publicKey  RSA 公钥用于验证签名
     * @return subject（用户 ID），解析失败返回空
     */
    public static Optional<String> getUserIdFromToken(String token, RSAPublicKey publicKey) {
        return parseToken(token, publicKey).map(JWTClaimsSet::getSubject);
    }

    /**
     * 从 JWT 令牌中提取角色列表。
     *
     * @param token      JWT 令牌字符串
     * @param publicKey  RSA 公钥
     * @return 角色列表，解析失败返回空列表
     */
    public static List<String> getRolesFromToken(String token, RSAPublicKey publicKey) {
        return parseToken(token, publicKey)
                .map(claims -> {
                    Object rolesObj = claims.getClaim("roles");
                    if (rolesObj instanceof List<?> list) {
                        return list.stream().map(Object::toString).toList();
                    }
                    if (rolesObj instanceof String rolesStr) {
                        return Arrays.asList(rolesStr.split(","));
                    }
                    return Collections.<String>emptyList();
                })
                .orElse(Collections.emptyList());
    }

    /**
     * 验证 JWT 令牌是否有效（签名正确且未过期）。
     *
     * @param token      JWT 令牌字符串
     * @param publicKey  RSA 公钥
     * @return true 如果令牌有效
     */
    public static boolean validateToken(String token, RSAPublicKey publicKey) {
        return parseToken(token, publicKey)
                .map(claims -> {
                    Date expiration = claims.getExpirationTime();
                    return expiration != null && expiration.after(new Date());
                })
                .orElse(false);
    }

    private static Optional<JWTClaimsSet> parseToken(String token, RSAPublicKey publicKey) {
        if (token == null || token.isBlank() || publicKey == null) {
            return Optional.empty();
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                return Optional.empty();
            }
            return Optional.ofNullable(signedJWT.getJWTClaimsSet());
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }

    /**
     * 仅解析 JWT 格式并提取 claims，不验证签名。
     */
    private static Optional<JWTClaimsSet> parseTokenWithoutVerification(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return Optional.ofNullable(signedJWT.getJWTClaimsSet());
        } catch (ParseException e) {
            return Optional.empty();
        }
    }
}
