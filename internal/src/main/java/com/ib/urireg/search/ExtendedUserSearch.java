package com.ib.urireg.search;

import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.search.UserSearch;
import com.ib.indexui.system.Constants;
import com.ib.system.utils.DateUtils;

import java.util.*;

/**
 * Предоставя разширение на стандартното търсене на потребител
 *
 * @author belev
 */
public class ExtendedUserSearch extends UserSearch {

	/**  */
	private static final long serialVersionUID = -6734028006280514175L;

	private Integer registratura;

	private Integer businessRole;

	private List<Integer> defPravaList;

	/** */
	public ExtendedUserSearch() {
		super();
	}

	/** @return the businessRole */
	public Integer getBusinessRole() {
		return this.businessRole;
	}

	/** @return the defPravaList */
	public List<Integer> getDefPravaList() {
		return this.defPravaList;
	}

	/** @return the registratura */
	public Integer getRegistratura() {
		return this.registratura;
	}

	/** @param businessRole the businessRole to set */
	public void setBusinessRole(Integer businessRole) {
		this.businessRole = businessRole;
	}

	/** @param defPravaList the defPravaList to set */
	public void setDefPravaList(List<Integer> defPravaList) {
		this.defPravaList = defPravaList;
	}

	/** @param registratura the registratura to set */
	public void setRegistratura(Integer registratura) {
		this.registratura = registratura;
	}

	/** */
	@Override
	protected void extendQueryUserList(StringBuilder select, StringBuilder from, StringBuilder where, Map<String, Object> params) {
		select.append(", zveno.REF_REGISTRATURA ");

		from.append(" left outer join ADM_REFERENTS ref on ref.CODE = u.USER_ID and ref.DATE_OT <= :dateArg and ref.DATE_DO > :dateArg ");
		from.append(" left outer join ADM_REFERENTS zveno on zveno.CODE = ref.CODE_PARENT and zveno.DATE_OT <= :dateArg and zveno.DATE_DO > :dateArg ");
		params.put("dateArg", DateUtils.startDate(new Date()));

		if (this.registratura != null) {
			where.append(" and zveno.REF_REGISTRATURA = :registratura "); // това е така, защото регистратурата е в звеното а не в
																			// служителя (вече)
			params.put("registratura", this.registratura);
		}

		if (this.businessRole != null) {
			from.append(" inner join ADM_USER_ROLES rol on rol.USER_ID = u.USER_ID and rol.CODE_CLASSIF = :codeClassif ");
			params.put("codeClassif", Constants.CODE_CLASSIF_BUSINESS_ROLE);

			where.append(" and rol.CODE_ROLE = :businessRole ");
			params.put("businessRole", this.businessRole);
		}

		if (this.defPravaList != null && !this.defPravaList.isEmpty()) {
			where.append(" and EXISTS ( ");
			where.append(" select defu.ROLE_ID from ADM_USER_ROLES defu where defu.USER_ID = u.USER_ID and defu.CODE_CLASSIF = :classifDefp and defu.CODE_ROLE in (:defPravaList) ");
			where.append(" union all ");
			where.append(" select defg.ROLE_ID from ADM_USER_GROUP defug inner join ADM_GROUP_ROLES defg on defg.GROUP_ID = defug.GROUP_ID ");
			where.append(" where defug.USER_ID = u.USER_ID and defg.CODE_CLASSIF = :classifDefp and defg.CODE_ROLE in (:defPravaList) ");
			where.append(" ) ");

			params.put("classifDefp", UriregConstants.CODE_CLASSIF_DEF_PRAVA);
			params.put("defPravaList", this.defPravaList);
		}
	}
}
