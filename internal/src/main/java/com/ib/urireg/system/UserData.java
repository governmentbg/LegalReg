package com.ib.urireg.system;

import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.ClientInfo;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.BaseUserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static com.ib.system.SysConstants.CODE_DEIN_UNAUTHORIZED;

/**
 * Конкретната за системата. В случая DocuWork!
 */
public class UserData extends BaseUserData {

	/** */
	static final Logger LOGGER = LoggerFactory.getLogger(UserData.class);

	/** */
	private static final long serialVersionUID = 2989532705334584877L;

	/** Регистратура в която се работи текущо */
	private Integer	registratura;
	/** звеното в което е назначен по време на логин */
	private Integer	zveno;

	/**
	 * Използва се при логин в системата. За да се направи първоначална идентификация на потребителя
	 *
	 * @param userId
	 * @param loginName
	 * @param liceNames
	 */
	public UserData(Integer userId, String loginName, String liceNames) {
		super(userId, loginName, liceNames);
	}

	/**
	 * Проверява дали потребителя има достъп до страницата. Ако няма го праща на index.xhtml
	 *
	 * @param codePage
	 */
	public String checkPageAccess(Integer codePage) {
		boolean ok = hasAccess(Constants.CODE_CLASSIF_MENU, codePage);

		if (!ok) {
			HttpServletRequest request = (HttpServletRequest) JSFUtils.getExternalContext().getRequest();

			LOGGER.info("!!! UNAUTHORIZED PAGE ACCESS !!! username={};codePage={};url={}", getLoginName(), codePage, request.getRequestURL());

			StringBuilder sb = new StringBuilder();

			sb.append("От потребител \"" + getLoginName() + "\" е направен опит за достъп до страница ");
			sb.append(request.getRequestURL());

			SystemData sd = (SystemData) JSFUtils.getManagedBean("systemData");
			if (codePage != null) {
				try {
					sb.append(" (" + sd.decodeItem(Constants.CODE_CLASSIF_MENU, codePage, getCurrentLang(), new Date()) + ")");
				} catch (DbErrorException e) {
					sb.append(" код=" + codePage + "!");
				}
			}

			String userIP = ClientInfo.getClientIpAddr(request);
			String sessionId = ((HttpSession) JSFUtils.getExternalContext().getSession(false)).getId();
			String clientBrowser = ClientInfo.getClientBrowser(request);
			String clientOS = ClientInfo.getClientOS(request);

			sb.append("</br>IP=" + userIP + "; Browser=" + clientBrowser + "; OS=" + clientOS + "; SESSID=" + sessionId);

			try {
				String setting = sd.getSettingsValue("delo.unauthorizedNotifUser");
				if (setting != null) { // нотификция
//					sendUnauthorizedPageAccessNotif(request.getRequestURL().toString(), setting.split(","), sd);
				}
			} catch (Exception e) {
				LOGGER.error("Грешка при формиране на нотификация: Опит за неоторизиран достъп до страница.", e);
			}

			SystemJournal journal = new SystemJournal(getUserId(), CODE_DEIN_UNAUTHORIZED, Constants.CODE_ZNACHENIE_JOURNAL_USER, getUserId(), sb.toString(), null);

			try {
				JPA.getUtil().runInTransaction(() -> new AdmUserDAO(this).saveAudit(journal));

			} catch (BaseException e) {
				LOGGER.error("Грешка при журналиране на Опит за неоторизиран достъп", e);
			}

			ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
			try {
				context.redirect(context.getRequestContextPath() + "/pages/empty.xhtml");
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return "@NO@";
	}




	/**
	 * true - презареждане на страницата
	 */
	private boolean reloadPage = false;
	private String previousPage = null;

   /**
    * Проверка дали отворената страница се презарежда
    * Извикването е:
    * <f:metadata><f:viewAction action="#{userData.checkReloadPage}" onPostback="false" /></f:metadata>
    * Използва се за проверка дали заключен за редакция обект (в @PostConstruct)  трябва да бъде отключен (в @PredDestroy)
    * Ако страницата се презарежда - да прескочи отключването.
    * reloadPage и previousPage трябва да се нулират в @PredDestroy, т.е. при излизане от страницата
    */
	public String checkReloadPage() {
		LOGGER.debug("checkReloadPage");
		FacesContext context = FacesContext.getCurrentInstance();
		if(context != null) {
		    UIViewRoot viewRoot = context.getViewRoot();
		    String id = viewRoot.getViewId();
		    if (previousPage != null && (previousPage.equals(id))) {
		    	setReloadPage(true);
		    }else {
		    	setReloadPage(false);
		    }
		    previousPage = id;
		}
		return "@NO@";
    }

	/** @return the registratura */
	public Integer getRegistratura() {
		return this.registratura;
	}

	/** @return the zveno */
	public Integer getZveno() {
		return this.zveno;
	}

	/** @param registratura the registratura to set */
	public void setRegistratura(Integer registratura) {
		this.registratura = registratura;
	}

	/** @param zveno the zveno to set */
	public void setZveno(Integer zveno) {
		this.zveno = zveno;
	}

	public String getPreviousPage() {
		return previousPage;
	}

	public void setPreviousPage(String previousPage) {
		this.previousPage = previousPage;
	}

	public boolean isReloadPage() {
		return reloadPage;
	}

	public void setReloadPage(boolean reloadPage) {
		this.reloadPage = reloadPage;
	}
}
