package com.ib.urireg.utils;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Този клас, трябва да предостави помощни методи за работа с токени
 * Всички необходими полета имат дефолтни стойности
 * Има конструктори с които да се променят
 *
 * @author krasig
 */
public  class JWTUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(JWTUtil.class);

    final Key jwt_key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    /** Издател      */
    private String jwt_issuer="MySystem";
    /* */
    String jwt_subject = "zxc";
    /* audience */
    String jwt_aud = "http://mysystem.kg";
    Date jwt_notBefore = new Date();
    Date jwt_issuedAt = new Date();
    String jwt_jti = String.valueOf(UUID.randomUUID());

    Date jwt_expiration=Date.from(Instant.now().plus(60, ChronoUnit.MINUTES));
    final String jwt_Secret_Key="If you want to fuck the sky, you must teach your dick to fly";
    SecretKey s_key = Keys.hmacShaKeyFor(jwt_Secret_Key.getBytes(StandardCharsets.UTF_8));



    Map<String,Object> claims =new HashMap<>();
    /**
     * Дефолтен конструктор. Ако се ползва, всичко ще дойде от константите
     */
    public JWTUtil() {

    }

    /**
     * Използва се най-вече при валидиране/декодиране
     * @param secret_key
     */
    public JWTUtil(String secret_key) {

        if (null!=secret_key && !secret_key.trim().isEmpty()){
            s_key=Keys.hmacShaKeyFor(secret_key.getBytes(StandardCharsets.UTF_8));
        }
    }
    public JWTUtil(String jwt_issuer, String jwt_subject, String jwt_aud, Date jwt_notBefore, Date jwt_issuedAt, String jwt_jti, int validInMinutes, String jwt_Secret_Key) {
        if (jwt_issuer!=null && jwt_issuer.isEmpty()) {
            this.jwt_issuer = jwt_issuer;
        }
        if (jwt_subject!=null && jwt_subject.isEmpty()) {
            this.jwt_subject = jwt_subject;
        }
        if (jwt_aud!=null && jwt_aud.isEmpty()) {
            this.jwt_aud = jwt_aud;
        }
        if (jwt_notBefore!=null ) {
            this.jwt_notBefore = jwt_notBefore;
        }
        if (jwt_issuedAt!=null ) {
            this.jwt_issuedAt = jwt_issuedAt;
        }
        if (jwt_jti !=null && jwt_jti.isEmpty()) {
            this.jwt_jti = jwt_jti;
        }
        if (jwt_expiration!=null ) {
            this.jwt_expiration = Date.from(Instant.now().plus(validInMinutes, ChronoUnit.MINUTES));
        }
        if (jwt_Secret_Key!=null && jwt_Secret_Key.isEmpty()) {
            this.s_key = Keys.hmacShaKeyFor(jwt_Secret_Key.getBytes(StandardCharsets.UTF_8));
        }
    }

    public JWTUtil setIssuer(String issuer){
        jwt_issuer=issuer;
        return this;
    }


    public JWTUtil setJti(String jwt_jti) {
        this.jwt_jti = jwt_jti;
        return this;
    }




    public JWTUtil(String secret_key, int valid_in_minutes){
        if (null!=secret_key && !secret_key.trim().isEmpty()){
            s_key=Keys.hmacShaKeyFor(secret_key.getBytes(StandardCharsets.UTF_8));
        }
        if (valid_in_minutes>0){
            jwt_expiration=Date.from(Instant.now().plus(valid_in_minutes, ChronoUnit.MINUTES));
        }
    }
    /**Генерира JWS. Ако някой параметъре нулл или празен, взема се дефолтната стойност
     * @return
     */
    public String generateJWT(){



        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer(jwt_issuer)
                .subject(jwt_subject)

                .expiration(jwt_expiration) //a java.util.Date
                .notBefore(jwt_notBefore) //a java.util.Date
                .issuedAt(jwt_issuedAt) // for example, now
                .id(jwt_jti)
                ;//just an example id
        jwtBuilder.audience().add(jwt_aud);
        if (getClaims()!=null && !getClaims().isEmpty()){
            jwtBuilder.claims(getClaims());
        }

        //If necessary
        //    jwtBuilder.claim("SomeKey1","SomeValue1");
        //    jwtBuilder.claim("SomeKey2","SomeValue2");




        String jwsString = jwtBuilder.signWith(s_key,SignatureAlgorithm.HS256).compact();
        LOGGER.debug("Generated JWT:{}",jwsString);
        return jwsString;
    }

    /**
     * Децодира токен.
     * Ако е невалиден - ще имаме Exception.
     * Под невалиден се има предвид да е изтекъл, да не съвпадне секрет ключа ....
     * //TODO Да се обмисли да се прихванат различните exceptioni за да се обработват!!!
     * @param jwt_string
     * @return
     */
    public Jws<Claims> decodeJWT(String jwt_string){
        if (jwt_string == null || jwt_string.isEmpty()) {
            throw new IllegalArgumentException("JWT string cannot be null or empty");
        }
        Jws<Claims> claimsJws = null;
       try {

           claimsJws = Jwts.parser().setSigningKey(s_key).build().parseSignedClaims(jwt_string);

       }catch (ExpiredJwtException e){
           LOGGER.error(e.getMessage(),e);
           throw e;
       }
        return claimsJws;

    }

    /**
     * Валидиране
     * Ако нещо не е наред - ще пише в лог-а
     * @param jws
     */
    public boolean validate(String jws){
        try {
            // Validate the JWS. This will throw an exception if not valid
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey(s_key)
                    .build()
                    .parseClaimsJws(jws);

            // If we get here without any exception, JWS is valid
            return true;
        } catch (Exception e) {
            LOGGER.error("JWS is not valid!!!{}",e);
        }
        return false;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }
}
