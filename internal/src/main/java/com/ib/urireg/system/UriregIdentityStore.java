package com.ib.urireg.system;

import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.system.ActiveUser;
import com.ib.system.BaseUserData;
import com.ib.system.auth.*;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.AuthenticationException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.cert.CertUtils;
import com.ib.system.utils.cert.X509CertificateInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.*;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;

/**
 * Прави логин чрез имплементираните по потребителско име и парола
 *
 * @author belev
 */
@Named
@ApplicationScoped
public class UriregIdentityStore extends IBIdentityStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(UriregIdentityStore.class);

	@Inject
	private ServletContext servletContext;

	/** */
	@Override
	protected Optional<BaseUserData> findUserDB(DBCredential credential) throws AuthenticationException {
		String username = credential.getCaller();
		String password = credential.getPasswordAsString();

		if (username == null || password == null) {
			return Optional.empty();
		}

		SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");
		try {

			AdmUserDAO dao = new AdmUserDAO(ActiveUser.DEFAULT);

			AdmUser user = dao.validateUser(systemData, username, password, false, false); // намира потребителя и валидира дали
																							// може да се направи логин
			BaseUserData userData = createUserData(systemData, user);

			return Optional.of(userData);

		} catch (AuthenticationException e) {


			LOGGER.error(e.getMessage());
			throw e; // трябва да се знае че това е проблема

		} catch (Exception e) {
			LOGGER.error("DBCredential login ERROR!", e);
			throw new AuthenticationException(e);

		} finally {
			JPA.getUtil().closeConnection();
		}
	}


	/** */
	@Override
	protected Optional<BaseUserData> findUserEAuth(EAuthCredential credential) throws AuthenticationException {
		Integer userId = credential.getUserId();

		if (userId == null) {
			throw new AuthenticationException(new InvalidParameterException("UserId can not be null!"));
		}

		try {
			SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");

			// TODO може да се наложи да се вика нещо подобно на AdmUserDAO.validateUser заради брой опити за достъп, проверка на
			// статуси и т.н.
			AdmUser user = JPA.getUtil().getEntityManager().find(AdmUser.class, userId); // логин

			// initialize user's accessValues
			AdmUserDAO admUserDAO = new AdmUserDAO(ActiveUser.of(userId));
			final Map<Integer, Map<Integer, Boolean>> userAccessMap = admUserDAO.findUserAccessMap(userId);
			user.setAccessValues(userAccessMap);

			BaseUserData userData = createUserData(systemData, user);

			return Optional.of(userData);

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new AuthenticationException(e);

		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/** */
	@Override
	protected Optional<BaseUserData> findUserLDAP(LDAPCredential credential) throws AuthenticationException {
		LOGGER.debug("findUserLDAP");
		SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");
		String domain;
		String username;
		InitialContext ldapContext = null;
		try {
			LOGGER.debug("Begin findUserLDAP...");
			String defaultDomain = systemData.getSettingsValue(UriregConstants.DEFAULT_LDAP_DOMAIN);

			if (credential.getCaller().contains("\\")){
				String[] parts = credential.getCaller().split("\\\\");
				domain = parts[0];
				username = parts[1];
			}else{
				domain = defaultDomain;
				username = credential.getCaller();
			}

			// get default domain of the user from system_options


			// setup properties for the initial context
			Hashtable<String, String> ldapContextProperties = new Hashtable<>();

			ldapContextProperties.put(Context.PROVIDER_URL, systemData.getSettingsValue(Context.PROVIDER_URL));// url of the ldap server
			ldapContextProperties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");// initial context factory
			ldapContextProperties.put(Context.SECURITY_AUTHENTICATION, systemData.getSettingsValue(Context.SECURITY_AUTHENTICATION));
			LOGGER.debug("Trying to use LDAP on address:{}",systemData.getSettingsValue(Context.PROVIDER_URL));
			LOGGER.debug("Domain:{}, user:{},pass:{}",domain,username,credential.getPasswordAsString());
			// username of the user
			ldapContextProperties.put(Context.SECURITY_PRINCIPAL, domain +"\\"+ username);

			// password of the user
			ldapContextProperties.put(Context.SECURITY_CREDENTIALS, credential.getPasswordAsString());

			// if we can create the context it means that user is present into the ldap directory
			ldapContext = new InitialDirContext(ldapContextProperties);

			LOGGER.debug("End findUserLDAP ... done!");

		// TODO тука с грешките надолу не е добре
		} catch (javax.naming.AuthenticationException e) {
			LOGGER.error(e.getMessage());
			LOGGER.debug("User not authorized!!!");
			throw new AuthenticationException(AuthenticationException.CODE_USER_UNKNOWN, null);

		} catch (Exception e) {
			LOGGER.error("LDAPCredential login ERROR!", e);
			throw new AuthenticationException(e);

		} finally {
			if (ldapContext != null) {
				try {
					ldapContext.close();
				} catch (NamingException e) {
					// ignore
				}
			}
		}

		try { // load user data from database

			AdmUserDAO dao = new AdmUserDAO(ActiveUser.DEFAULT);
			LOGGER.debug("Begin validate user with:username={}",credential.getCaller());
			AdmUser user = dao.validateUser(systemData, null, credential.getCaller());
			LOGGER.debug("User was found!!!");
			BaseUserData userData = createUserData(systemData, user);

			return Optional.of(userData);

		// TODO тука с грешките надолу не е добре
		} catch (AuthenticationException e) {
			LOGGER.debug("User was NOT found!!!");
			LOGGER.error(e.getMessage());
			if (e.getCode() == AuthenticationException.CODE_UNAUTHORIZED_STATUS) {
				throw e;
			}
			throw new AuthenticationException("Потребителят не е регистриран в системата.", null);

		} catch (Exception e) {
			LOGGER.error("LDAPCredential login ERROR - db select!", e);
			throw new AuthenticationException(e);

		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/** 2FA логване с код получен по мейл
	 * @param credential
	 * @return
	 * @throws AuthenticationException
	 */
	@Override
	protected Optional<BaseUserData> findUser2faMailCode(TwoFAMailCredential credential) throws AuthenticationException {
		String username = credential.getUsername();
		String codeFromMail = credential.getCodeFromMail();
		if (username == null || codeFromMail == null) {
			return Optional.empty();
		}
		SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");

		try {

			AdmUserDAO dao = new AdmUserDAO(ActiveUser.DEFAULT);

			AdmUser user = dao.validateUser2faMailCode(systemData, username, codeFromMail, false, false); // намира потребителя и валидира дали
			// може да се направи логин
			BaseUserData userData = createUserData(systemData, user);

			return Optional.of(userData);

		} catch (AuthenticationException e) {


			LOGGER.error(e.getMessage());
			throw e; // трябва да се знае че това е проблема

		} catch (Exception e) {
			LOGGER.error("DBCredential login ERROR!", e);
			throw new AuthenticationException(e);

		} finally {
			JPA.getUtil().closeConnection();
		}


	}

	/** 2FA логване с код получен по мейл
	 * @param credential
	 * @return
	 * @throws AuthenticationException
	 */
	@Override
	protected Optional<BaseUserData> findUser2faTOTPCode(TwoFATOTPCredential credential) throws AuthenticationException {
		String username = credential.getUsername();
		String totpCode = credential.getTotpCode();
		if (username == null || totpCode == null) {
			return Optional.empty();
		}
		SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");

		try {

			AdmUserDAO dao = new AdmUserDAO(ActiveUser.DEFAULT);

			AdmUser user = dao.validateUser2faTOTPCode(systemData, username, totpCode, false, false); // намира потребителя и валидира дали
			// може да се направи логин
			BaseUserData userData = createUserData(systemData, user);

			return Optional.of(userData);

		} catch (AuthenticationException e) {


			LOGGER.error(e.getMessage());
			throw e; // трябва да се знае че това е проблема

		} catch (Exception e) {
			LOGGER.error("DBCredential login ERROR!", e);
			throw new AuthenticationException(e);

		} finally {
			JPA.getUtil().closeConnection();
		}


	}

	/**
	 * @param credential Тук е сертификата с който ще се логваме. Трябва да  имплементираме логиката
	 * @return
	 * @throws AuthenticationException
	 */
	@Override
	protected Optional<BaseUserData> findUserClientCert(CertCredential credential) throws AuthenticationException {
		LOGGER.debug("findUserClientCert...");
		X509CertificateInfo x509CertificateInfo;

		if (credential == null ) {
			LOGGER.error("findUserClientCert ERROR! Credential is null!");
			return Optional.empty();
		}

        try {
			x509CertificateInfo = CertUtils.parseX509Certificate(credential.getCert(),false);
		} catch (Exception e) {
			LOGGER.error("DBCredential login ERROR!", e);
			throw new AuthenticationException(e);
        }
        SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");
		try {

			AdmUserDAO dao = new AdmUserDAO(ActiveUser.DEFAULT);
			LOGGER.debug("Begin validate user with:email={}",x509CertificateInfo.getEmail());
			AdmUser user = dao.validateUser(systemData, x509CertificateInfo.getEmail(), false, false); // намира потребителя и валидира дали
			// може да се направи логин
			BaseUserData userData = createUserData(systemData, user);

			return Optional.of(userData);

		} catch (AuthenticationException e) {


			LOGGER.error(e.getMessage());
			throw e; // трябва да се знае че това е проблема

		} catch (Exception e) {
			LOGGER.error("DBCredential login ERROR!", e);
			throw new AuthenticationException(e);

		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	/**
	 * Създава усерДатата и сетва всичко което е нужно за работата на системата
	 *
	 * @param sd
	 * @param user
	 * @return
	 * @throws DbErrorException
	 */
	public UserData createUserData(SystemData sd, AdmUser user) throws DbErrorException {
		UserData userData = new UserData(user.getId(), user.getUsername(), user.getNames());

		Date today = DateUtils.startDate(new Date());

		SystemClassif row = sd.decodeItemLite(CODE_CLASSIF_ADMIN_STR, user.getId(), userData.getCurrentLang(), today, true);

		if (row != null) {
			userData.setRegistratura((Integer) row.getSpecifics()[UriregClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA]);
			userData.setZveno(row.getCodeParent()); // CODE_PARENT се явява звеното на човека
		}

		userData.setAccessValues(new HashMap<>(user.getAccessValues())); // задавам правата
		user.setAccessValues(null); // зачиствам ги от ентитито!

		return userData;
	}
}
