package com.ib.urireg.system;

import com.ib.system.BaseUserData;
import com.ib.system.IBUserPrincipal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.faces.annotation.FacesConfig;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.LoginToContinue;

import java.util.Date;

/**
 * Настройки на системата
 *
 * @author belev
 */
@CustomFormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(loginPage = "/login", useForwardToLogin = false, errorPage = ""))
@FacesConfig()
@ApplicationScoped
public class ApplicationConfig {

	@Inject
	private SecurityContext securityContext; // дава ни информация за логнатия потребител

	/** */
	public ApplicationConfig() {
		super();
	}

	/**
	 * Дава информация за текущия логнат потребител. Може да се използва за inject-ване!
	 *
	 * @return
	 */
	@Produces
	@Named("userData")
	public BaseUserData getUserData() {
		return ((IBUserPrincipal) this.securityContext.getCallerPrincipal()).getUserData();
	}

    /**
     * Използва се в страници, където е нужна текущата дата. Слага се директно nowib.<br/>
     *
     * @return index implementation
     */
    @Produces
    @Named("nowib")
    @RequestScoped
    public Date getNowib() {
        return new Date();
    }
}
