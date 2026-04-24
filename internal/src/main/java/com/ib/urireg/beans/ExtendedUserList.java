package com.ib.urireg.beans;

import com.ib.urireg.search.ExtendedUserSearch;
import com.ib.indexui.beans.UserList;
import com.ib.indexui.search.UserSearch;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.db.dto.SystemClassif;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Предоставя разширение на стандартния екран за търсене на потребител
 *
 * @author belev
 */
@Named("userList")
@ViewScoped
public class ExtendedUserList extends UserList {

	/**  */
	private static final long serialVersionUID = -7652770038607795221L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedUserList.class);

	private List<SystemClassif> defPravaClassif;

	public List<SystemClassif> getDefPravaClassif() {
		return defPravaClassif;
	}

	public void setDefPravaClassif(List<SystemClassif> defPravaClassif) {
		this.defPravaClassif = defPravaClassif;
	}

	/** */
	@Override
	public boolean isExtendedArgs() {
		return true;
	}

	/** */
	@Override
	public boolean isExtendedCols() {
		return true;
	}

	/** */
	@Override
	public boolean isRenderArgType() {
		return false; // за деловодството няма да има вид потребител в този екран
	}

	/** */
	@Override
	public boolean isRenderColDateReg() {
		return false;
	}

	/** */
	@Override
	public boolean isRenderColType() {
		return false; // за деловодството няма да има вид потребител в този екран
	}

	/** */
	@Override
	protected UserSearch createSearchObject() {
		this.defPravaClassif = new ArrayList<>();
		return new ExtendedUserSearch();
	}

	/** */
	@PostConstruct
	@Override
	protected void initData() {
		super.initData();

		try {

			this.defPravaClassif = new ArrayList<>();

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);

			LOGGER.error(e.getMessage(), e);
		}
	}
}
