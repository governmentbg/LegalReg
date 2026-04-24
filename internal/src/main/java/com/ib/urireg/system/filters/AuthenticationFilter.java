package com.ib.urireg.system.filters;

import com.ib.urireg.rest.common.Secured;
import com.ib.urireg.utils.JWTUtil;
import com.ib.system.BaseUserData;
import com.ib.system.IBUserPrincipal;
import com.ib.system.auth.TokenCredential;
import jakarta.annotation.Priority;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.Credential;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static jakarta.security.enterprise.AuthenticationStatus.SUCCESS;

/**
 * Контролира достъпа до рест услугите
 * Прилага се само за услуги, които са анотирани със @Secured
 */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);
    @Inject
    private SecurityContext securityContext;
    @Context
    HttpServletRequest httpServletRequest;

    @Context
    HttpServletResponse httpServletResponse;
    @Context
    ResourceInfo resourceInfo;
    private static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";

//    @Inject
//    private SystemData sd;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        //javax.ws.rs.core.SecurityContext securityContext = requestContext.getSecurityContext();
        LOGGER.info("AuthenticationFilter -- Start");

        Method method = resourceInfo.getResourceMethod();
        if(method.isAnnotationPresent(RolesAllowed.class)){
            RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
            Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAnnotation.value()));
            LOGGER.error("AuthenticationFilter -- RolesAllowed rolesSet: {}", rolesSet);
        }
        // Get the Authorization header from the request
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Validate the Authorization header
        if (!isTokenBasedAuthentication(authorizationHeader)) {
            LOGGER.info("AuthenticationFilter - no token based auhentication:missin {}", AUTHENTICATION_SCHEME_BEARER);
            abortWithUnauthorized(requestContext);
            return;
        }

        // Extract the token from the Authorization header
        String token = authorizationHeader.substring(AUTHENTICATION_SCHEME_BEARER.length()).trim();
        LOGGER.info("AuthenticationFilter - token:{}",token);
        try {

            // Validate the token
            validateToken(token);
            LOGGER.info("AuthenticationFilter - We have valid token!!!! Go to load simple  UserData in SecureContext");

            Credential tokenCredential = new TokenCredential(token);
            AuthenticationParameters authParms = AuthenticationParameters.withParams().credential(tokenCredential);

            AuthenticationStatus authenticationStatus = securityContext.authenticate(httpServletRequest, httpServletResponse, authParms);

            if (authenticationStatus==SUCCESS) {
                    LOGGER.info("AuthenticationFilter - authenticationStatus:{}",authenticationStatus.name());
                    Set<IBUserPrincipal> principals = this.securityContext.getPrincipalsByType(IBUserPrincipal.class);
                    BaseUserData userData = principals.iterator().next().getUserData();
                    LOGGER.info("AuthenticationFilter - authenticated userId:{},subject:{},names:{}",userData.getUserId(),userData.getLoginName(),userData.getLiceNames());
            }else {
                LOGGER.info("AuthenticationFilter - authenticationStatus:{}",authenticationStatus.name());
                abortWithUnauthorized(requestContext);
            }


        } catch (Exception e) {
            LOGGER.info("AuthenticationFilter - invalid token:{}",token);
            abortWithUnauthorized(requestContext);
        }

    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {

        // Check if the Authorization header is valid
        // It must not be null and must be prefixed with "Bearer" plus a whitespace
        // Authentication scheme comparison must be case-insensitive
        return authorizationHeader != null && authorizationHeader.toLowerCase()
                .startsWith(AUTHENTICATION_SCHEME_BEARER.toLowerCase() + " ");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext) {

        // Abort the filter chain with a 401 status code
        // The "WWW-Authenticate" is sent along with the response
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BEARER)
                        .build());
    }

    /**
     * Тук единствено проверяваме дали няма да получим EXception. Ако има такъв- значи нещо не е наред и диекно изхвърляме
     * @param token
     * @throws Exception
     */
    private void validateToken(String token) throws Exception {
        new JWTUtil().decodeJWT(token);
    }

}
