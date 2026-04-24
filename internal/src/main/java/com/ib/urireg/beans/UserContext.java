package com.ib.urireg.beans;

import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexLoginBean;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.utils.PasswordUtils;
import com.ib.system.utils.X;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * В този клас са показани начините за достъпване на х-ки от т.н. потребителски контекст.
 *
 * @author belev
 */
@Named
@SessionScoped
public class UserContext extends IndexUIbean {

	/**  */
	private static final long serialVersionUID = 1400459123152353504L;

	private static final Logger LOGGER = LoggerFactory.getLogger(UserContext.class);

	private boolean ldapLogin;

	// смяната на парола от портебител през диалгова в настройките ще се прави от този бийн, а не от логина бийна
	private String	oldPassword;
	private String	newPassword1;
	private String	newPassword2;

	private List<SelectItem> periodNoFuture;
	public List<SelectItem> getPeriodNoFuture() {
		return periodNoFuture;
	}

	/**  */
	public UserContext() {
		try {
			this.periodNoFuture = createItemsList(false, Constants.CODE_CLASSIF_PERIOD_NOFUTURE, null, false);

			this.ldapLogin = UriregConstants.LOGIN_TYPE_LDAP.equals(getSystemData().getSettingsValue(UriregConstants.LOGIN_TYPE));

		} catch (Exception e) { //
			LOGGER.error("init error!", e);
		}
	}

	/** @return the ldapLogin */
	public boolean isLdapLogin() {
		return this.ldapLogin;
	}


	/** @return the oldPassword */
	public String getOldPassword() {
		return this.oldPassword;
	}
	/** @param oldPassword the oldPassword to set */
	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}
	/** @return the newPassword1 */
	public String getNewPassword1() {
		return this.newPassword1;
	}
	/** @param newPassword1 the newPassword1 to set */
	public void setNewPassword1(String newPassword1) {
		this.newPassword1 = newPassword1;
	}
	/** @return the newPassword2 */
	public String getNewPassword2() {
		return this.newPassword2;
	}
	/** @param newPassword2 the newPassword2 to set */
	public void setNewPassword2(String newPassword2) {
		this.newPassword2 = newPassword2;
	}

	/**
	 * Смяна на собствена парола
	 */
	public void actionChangePassword() {
		LOGGER.debug("---> changePassword <---");

		X<AdmUser> xUser = X.empty();

		try { // търсим по името
			AdmUserDAO dao = new AdmUserDAO(ActiveUser.DEFAULT);

			JPA.getUtil().runWithClose(() -> xUser.set(dao.findByUsername(getUserData().getLoginName())));

		} catch (Exception e) {
			LOGGER.error("Грешка при търсене на потребител по потребителско име", e);

			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);
			return;
		}

		if (!xUser.isPresent()) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"));
			return;
		}

		if (!IndexLoginBean.checkPassword(xUser.get(), this.oldPassword, this.newPassword1, this.newPassword2, getSystemData())) {
			return; // явно нещо не е въведено като хората
		}

		final String newPasswordCrypted;
		try {
			String pass_crypt_alg=getSystemData().getSettingsValue("system.password_crypt_algorithm");
			PasswordUtils.USE_ALGORITHM useAlgorithm = PasswordUtils.USE_ALGORITHM.get(pass_crypt_alg);
			LOGGER.debug("Will encrypt password with algorythm:{} . If is null will be used USE_ALGORITHM.SHA512",useAlgorithm);
			newPasswordCrypted = PasswordUtils.hashPassword(this.newPassword1,useAlgorithm);
		} catch (Exception e) {
			LOGGER.error("Грешка при криптиране на парола", e);
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.exception"), e);
			return;
		}

		try { // готов е за запис
			Integer userId = xUser.get().getId();

			AdmUserDAO dao = new AdmUserDAO(ActiveUser.of(userId)); // по този начин ще се журналира че логнатия потребител си
																	// сменя паролата
			JPA.getUtil().runInTransaction(() -> dao.changePassword(xUser.get(), newPasswordCrypted));

			PrimeFaces.current().executeScript("PF('dlg-change-pass').hide();");
			PrimeFaces.current().ajax().update("messagesGl");

			JSFUtils.addInfoMessage("Паролата е сменена успешно!");


		} catch (Exception e) {
			LOGGER.error("Грешка при смяна на парола на потребител", e);

			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);
		}
	}

	/** */
	public void actionClear() {
		setOldPassword(null);
		setNewPassword1(null);
		setNewPassword2(null);
	}
}
