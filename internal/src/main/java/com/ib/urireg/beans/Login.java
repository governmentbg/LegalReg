package com.ib.urireg.beans;

import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.UserData;
import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.system.IndexLoginBean;
import com.ib.indexui.utils.ClientInfo;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.ActiveUser;
import com.ib.system.BaseUserData;
import com.ib.system.IBUserPrincipal;
import com.ib.system.auth.DBCredential;
import com.ib.system.auth.LDAPCredential;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.Credential;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Логин със SecurityContext
 *
 * @author belev
 */
@Named
@ViewScoped
public class Login extends IndexLoginBean implements java.io.Serializable {

	/**  */
	private static final long serialVersionUID = 8191901936895268740L;

	private static final Logger LOGGER = LoggerFactory.getLogger(Login.class);

	@Inject
	private SecurityContext	securityContext;
	@Inject
	private ExternalContext	externalContext;
	@Inject
	private FacesContext	facesContext;

    /**
     *
     */
    public Login() {
        super();
    }

	/** */
	@PostConstruct
	void initData() {
		LOGGER.debug("------------------------------ initData ----------------------");

		boolean logout = true; // ако е логнат и в урл-то сложи входната страница реално е логнат и за да се направи реален логин
		// трябва първо да се направи изход!
		try {
			getUserData();
		} catch (Exception e) { // ако даде грешка няма проблем
			logout = false; // това е знак, че няма потребител и не е нужно да се инвалидира сесията
		}
		if (logout) {
			HttpServletRequest request = (HttpServletRequest) this.externalContext.getRequest();
			try { // ще се направи изход, за да се провокира винаги реален вход
				request.logout();
				request.getSession().invalidate();
			} catch (ServletException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Логин със SecurityContext
	 */
	@Override
	protected BaseUserData login() {
		LOGGER.info("---> login start <---");

		HttpServletRequest request = (HttpServletRequest) this.externalContext.getRequest();
		HttpServletResponse response = (HttpServletResponse) this.externalContext.getResponse();

		try { // винаги е нов логин. може и да има и по хитър начин!
			request.logout();
		} catch (ServletException e) {
			LOGGER.error(e.getMessage(), e);
		}

		AuthenticationStatus status = continueAuthentication(request, response);

		switch (status) {

		case SEND_CONTINUE:
			LOGGER.debug("actionLogin-SEND_CONTINUE");

			this.facesContext.responseComplete();

			if (getUsername() != null) { // така ще сработи да журналира, защото като мине от тук няма усер дата !?!?!
				AdmUser user = null;
				try {
					user = new AdmUserDAO(ActiveUser.DEFAULT).findByUsername(getUsername().trim());
				} catch (Exception e) {
					LOGGER.error("Грешка при определяне на потребител за журналиране на вход", e);
				} finally {
					JPA.getUtil().closeConnection();
				}
				if (user != null) {
					String userIP = ClientInfo.getClientIpAddr(request); // TODO може да се направят проверки за достъп

					journalLoginSuccess(request, userIP, new UserData(user.getId(), user.getUsername(), user.getNames()));
				}
			}

			break;

		case SEND_FAILURE:
			LOGGER.debug("actionLogin-SEND_FAILURE");

			JSFUtils.addErrorMessage( getMessageResourceString(UI_beanMessages, "login.fail"));

			break;

		case SUCCESS:
			LOGGER.debug("actionLogin-SUCCESS");

			try {
				this.externalContext.redirect(this.externalContext.getRequestContextPath() + "/pages/dashboard");
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}

			break;

		case NOT_DONE:
			// тука няма да влезне
		}

		// трябва да върна данните за логнатия потребител
		Set<IBUserPrincipal> principals = this.securityContext.getPrincipalsByType(IBUserPrincipal.class);

		if (principals.isEmpty()) {
			return null; // явно не се е логнал успешно
		}

		LOGGER.info("---> SecurityContext login END <---");

		return principals.iterator().next().getUserData(); // ето го усера
	}


    private AuthenticationStatus continueAuthentication(HttpServletRequest request, HttpServletResponse response) {
        LOGGER.debug("actionLogin-continueAuthentication");
        try {
            Credential credential;
            // get login type from system_option table
            String loginType = getSystemData().getSettingsValue(UriregConstants.LOGIN_TYPE);
			LOGGER.debug("loginType={}", loginType);
			if (UriregConstants.LOGIN_TYPE_DATABASE.equals(loginType)) {
				credential = new DBCredential(getUsername(), getPassword());
			} else if (UriregConstants.LOGIN_TYPE_LDAP.equals(loginType)) {
				credential = new LDAPCredential(getUsername(), getPassword());
			} else{
				credential = new DBCredential(getUsername(), getPassword());// default
			}

			AuthenticationParameters parameters = AuthenticationParameters.withParams().credential(credential);

			return this.securityContext.authenticate(request, response, parameters);

		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			return AuthenticationStatus.SEND_FAILURE;
		}
	}
}
