package com.ib.urireg.beans;

import com.ib.urireg.db.dao.ReferentDAO;
import com.ib.urireg.db.dto.Referent;
import com.ib.urireg.system.UriregClassifAdapter;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.UserData;
import com.ib.indexui.beans.UserEdit;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.ValidationUtils;
import com.ib.system.utils.X;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;

/**
 * Предоставя разширение на стандартния екран за потребител
 *
 * @author belev
 */
@Named("userEdit")
@ViewScoped
public class ExtendedUserEdit extends UserEdit {

	/**  */
	private static final long serialVersionUID = 8923973254687230924L;
	private static final String SETTING_2FA = "system.2fa";

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedUserEdit.class);

	private boolean freeAcces; // ако флага е вдигнат значи при неизбрани регистри се дава право до всички

	private String				admStructInfo;		// къде е назначен и длъжността

	private Map<Integer, Object> specificsEmployeesOnly;

	// ще се вдигне флага ако логина е настроен през лдап и тогава на екрана и в логиката има специфики заради паролите
	private boolean ldapLogin;

	private boolean quitUser; // ще се вдигне ако е в режим на корекция и потребителя е напуснал

	private boolean refreshSetting; // за да се знае след запис дали има пипано в потребителските настройки

	/**
	 * това е името на лицето, което се показва в режим актуализация на екрана причините да е така са:
	 * 1. За активни потребители се разкодира през класификацията на Админ Структурата, а там се вижда и длъжността
	 * 2. За неактивни потребители се използва класификацията за Напуснали, а там се вижда до коя дата е напуснал
	 * 3. За мигрирани потребители, които не са изобщо в админ структурата ще се вижда името от adm_users
	 */
	private String liceNames;

	private List<Object[]> allRegsForRegistratura; // всички регистри към регистратура, за да махна неактивните

	/** */
	@Override
	@PostConstruct
	protected void initData() {
		super.initData();

		try {
			if (getClassifList() != null) {
				getClassifList().add(new SelectItem(UriregConstants.CODE_CLASSIF_USER_SETTINGS, getSystemData().getNameClassification(UriregConstants.CODE_CLASSIF_USER_SETTINGS, getCurrentLang())));
			}

			this.ldapLogin = UriregConstants.LOGIN_TYPE_LDAP.equals(getSystemData().getSettingsValue(UriregConstants.LOGIN_TYPE));
			if (this.ldapLogin) {
				setChangePass(false);
			}

			this.quitUser = getUser().getId() != null && !getSystemData().matchClassifItems(Constants.CODE_CLASSIF_ADMIN_STR, getUser().getId(), getCurrentDate());

			if (getUser().getId() != null) {
				if (getSystemData().matchClassifItems(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, getUser().getId(), getCurrentDate())) {

					this.liceNames = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, getUser().getId(), getCurrentLang(), getCurrentDate());

				} else {
					this.liceNames = getUser().getNames();
				}
			} else {
				this.liceNames = null;
			}

			if (this.quitUser) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "extendedUserEdit.quitUser"));
			}

			this.specificsEmployeesOnly = Collections.singletonMap(UriregClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, X.of(UriregConstants.CODE_ZNACHENIE_REF_TYPE_EMPL));

			this.freeAcces = false;//isClassifFreeAccess(CODE_CLASSIF_REGISTRI);


			Integer id = getUser().getReferentId() != null ? getUser().getReferentId() : getUser().getId();

			if (getUser().getId() != null) {
				getUser().setReferentId(getUser().getId());
			}

			// ако има административна структура
			this.admStructInfo = getSystemData().decodeItemDopInfo(CODE_CLASSIF_ADMIN_STR, id, getCurrentLang(), getCurrentDate());
			if (this.quitUser && SearchUtils.isEmpty(this.admStructInfo)) {
				this.admStructInfo = getSystemData().decodeItemDopInfo(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, id, getCurrentLang(), getCurrentDate());
			}

			// настройки за 2FA
			String setting = this.getSystemData().getSettingsValue(SETTING_2FA);
			if(ValidationUtils.isNotBlank(setting) &&
					(setting.equals("1") || setting.equals("2") || setting.equals("3"))) {
				this.setTwoFactorAuthSetting(Integer.parseInt(setting));
			}
			if(this.getUser().getTwo_fa() != null && this.getUser().getTwo_fa() == Constants.TWO_FA_AUTH) {
				generateAuthenticatorQrCode();
			}

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}

	}



	/** */
	@Override
	public void loadAdmStrData() {
		try {
			AdmUser user = getDao().findById(getUser().getReferentId());
			if (user != null) { // намерен е и ще се отваря в редакция
				JSFUtils.addFlashScopeValue("objectID", user.getId());
				initData();

				setChangePass(false);

			} else { // нов ще се прави

				JSFUtils.addFlashScopeValue("refID", getUser().getReferentId());

				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(UriregConstants.CODE_CLASSIF_ADMIN_STR, getUser().getReferentId(), getCurrentLang(), getCurrentDate(), UriregClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (refType.equals(Integer.valueOf(UriregConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {

					initData();

					String mail = (String) getSystemData().getItemSpecific(CODE_CLASSIF_ADMIN_STR, getUser().getReferentId(), getCurrentLang(), getCurrentDate(),
						UriregClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL);
					getUser().setEmail(mail);

					@SuppressWarnings("unchecked")
					List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(
						"select REF_ID, REF_NAME, DATE_OT from ADM_REFERENTS where CODE = ? order by DATE_OT desc, REF_ID desc")
						.setParameter(1, getUser().getReferentId()).setMaxResults(1).getResultList();
					if (rows.isEmpty()) { // ако по някаква чудна причина се окаже че няма си остава по стария начин
						getUser().setNames(getSystemData().decodeItem(CODE_CLASSIF_ADMIN_STR, getUser().getReferentId(), getCurrentLang(), getCurrentDate()));
					} else {
						getUser().setNames((String) rows.get(0)[1]);
					}

				} else {

					getUser().setReferentId(null);
					JSFUtils.addMessage("formUserEdit" + ":chooseAdmStr:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "extendedUserEdit.plsChoiceOnlySluj"));

				}
			}

			PrimeFaces.current().ajax().update("formUserEdit");

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/** */
	@Override
	public void newUser(int mode) {
		super.newUser(mode);

		if (this.ldapLogin) {
			setChangePass(false); // така за нов няма да има полетата с паролите
		}
		this.quitUser = false;
		this.liceNames = null;

		try {
			getUser().setStatus(Constants.CODE_ZNACHENIE_STATUS_ACTIVE); // така ще се правят новите

			this.setTypePanelData(1);


			Integer refID = (Integer) JSFUtils.getFlashScopeValue("refID");
			if (refID != null) {
				getUser().setReferentId(refID);
			}

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);
			LOGGER.error(e.getMessage(), e);
		}
	}



	/**
	 * При промяна на панела
	 *
	 * @return
	 */
	public void actionChangeTypePanel(ValueChangeEvent event) {
		Integer newTypeDoc = (Integer) event.getNewValue();
		this.setTypePanelData(newTypeDoc);
		LOGGER.debug("actionChangeTypePanel = {} ", newTypeDoc);
	}





	@Override
	public void actionSave() {
		boolean newUser = getUser().getId() == null;
		String newPass = isChangePass() ? SearchUtils.trimToNULL(getPassPlain()) : null;
		String email = SearchUtils.trimToNULL(getUser().getEmail());

		super.actionSave();

		syncAdmStruct(email);

		if (this.refreshSetting
			&& getUser().getId().equals(getUserData(UserData.class).getUserId())) { // става много интересно защото редактира себе си

			List<Integer> selectedClassif = new ArrayList<>(); // и трябва да се рефрешне в узер датата

			if (this.refreshSetting) {
				selectedClassif.add(UriregConstants.CODE_CLASSIF_USER_SETTINGS);
				getUserData().getAccessValues().remove(UriregConstants.CODE_CLASSIF_USER_SETTINGS);
			}

			try {
				Map<Integer, Map<Integer, Boolean>> replacment = getDao().findUserAccessMap(getUser().getId(), selectedClassif);

				getUserData().getAccessValues().putAll(replacment);

			} catch (Exception e) {
				LOGGER.error("Грешка при синхронизиране на избрани настройки.", e);
			} finally {
				JPA.getUtil().closeConnection();
			}
		}
	}

	/**
	 * @param email
	 */
	private void syncAdmStruct(String email) {
		List<FacesMessage> msgList = FacesContext.getCurrentInstance().getMessageList();
		if (msgList != null && !msgList.isEmpty()) {
			for (FacesMessage msg : msgList) {
				if (FacesMessage.SEVERITY_ERROR.equals(msg.getSeverity())) {
					return; // има грешка от записа и няма как да се случи това надолу
				}
			}
		}

		try {
			SystemClassif emplItem = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, getUser().getId(), getCurrentLang(), getCurrentDate(), true);
			if (emplItem == null) {
				return;
			}
			this.liceNames = emplItem.getTekst();

			if (!Objects.equals(email, emplItem.getSpecifics()[UriregClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL])) {
				// има разлика и трябва да се синхроницира мейла
				ReferentDAO dao = new ReferentDAO(getUserData());

				JPA.getUtil().runInTransaction(()-> {
					Referent referent = dao.findByCode(getUser().getId(), getCurrentDate(), true);
					if (referent != null) {
						referent.setContactEmail(email);
						dao.save(referent);
					}
				});

				getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
				getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при синхронизиране на данни с административната структура", e);
		}
	}

	/** @return the admStructInfo */
	public String getAdmStructInfo() {
		return this.admStructInfo;
	}

	/** */
	@Override
	public String getDivStatusExplainClass() {
		return DIV_CLASS_P_COL_8;
	}


	/** @return the specificsEmployeesOnly */
	public Map<Integer, Object> getSpecificsEmployeesOnly() {
		return this.specificsEmployeesOnly;
	}


	/**  */
	@Override
	public boolean isExtended() {
		return true;
	}

	/** @return the freeAcces */
	public boolean isFreeAcces() {
		return this.freeAcces;
	}

	/** */
	@Override
	public boolean isRenderUserType() {
		return false; // за деловодството няма да има вид потребител в този екран
	}


	/** @return the ldapLogin */
	public boolean isLdapLogin() {
		return this.ldapLogin;
	}

	/** @return the quitUser */
	public boolean isQuitUser() {
		return this.quitUser;
	}

	/** @return the liceNames */
	public String getLiceNames() {
		return this.liceNames;
	}

}
