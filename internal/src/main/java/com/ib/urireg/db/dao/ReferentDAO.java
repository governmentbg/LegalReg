package com.ib.urireg.db.dao;

import com.ib.urireg.db.dto.Referent;
import com.ib.urireg.db.dto.ReferentAddress;
import com.ib.urireg.system.UriregClassifAdapter;
import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.system.Constants;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.SysClassifUtils;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.ib.urireg.system.UriregConstants.*;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_POSITION;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.trimToNULL;

/**
 * DAO for {@link Referent}
 *
 * @author belev
 */
public class ReferentDAO extends AbstractDAO<Referent> {

	/**
	 * За да се предостави DAO за запис/търсене/изтриване на адреси, като всичко ще се прави в контекста на участник в процеса
	 */
	static class ReferentAddressDAO extends AbstractDAO<ReferentAddress> {

		/** @param user */
		ReferentAddressDAO(ActiveUser user) {
			super(ReferentAddress.class, user);
		}
	}

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(ReferentDAO.class);

	/** @param user */
	public ReferentDAO(ActiveUser user) {
		super(Referent.class, user);
	}

	/**
	 * Търсене на служители по длъжност
	 * @param position
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> selectRefDataByPosition(Integer position) throws DbErrorException {
		if (position == null) {
			return new ArrayList<>();
		}
		List<Object[]> result;
		try {
			result = JPA.getUtil().getEntityManager().createNativeQuery(
					"select r.code, r.ref_name from adm_referents r" +
							" where r.code_classif = :admClassif and r.empl_position = :emplPosition" +
							" and r.date_ot <= :dateArg and r.date_do > :dateArg")
				    .setParameter("admClassif", CODE_CLASSIF_ADMIN_STR)
				    .setParameter("emplPosition", position)
				    .setParameter("dateArg", DateUtils.startDate(new Date()))
				    .getResultList();
		} catch (Exception e) {
		    throw new DbErrorException("Грешка при търсене на служители по длъжност=" + position, e);
		}
		return result;
	}

	/**
	 * Проверка дали служителят може да се изтрие
	 *
	 * @param referent
	 * @param dateAction
	 * @param systemData
	 * @return текста на грешка ако не може да се изтрие
	 * @throws DbErrorException
	 */
	public String checkEmplDeleteAllowed(Referent referent, Date dateAction, BaseSystemData systemData) throws DbErrorException {
		if (!Objects.equals(referent.getRefType(), CODE_ZNACHENIE_REF_TYPE_EMPL)) {
			return null;
		}

		Date dateOt = DateUtils.startDate(referent.getDateOt());
		Date dateDo = DateUtils.startDate(dateAction);

		if (dateOt.getTime() != dateDo.getTime()) { // това трябва да е валидира от екрана, но за всеки случай
			return "Служителят има история и изтриването не е позволено!";
		}
		if (systemData.matchClassifItems(CODE_CLASSIF_USERS, referent.getCode(), dateDo)) { // и това трябва да се валидиа от
																							// екрана, но за всеки случай 2
			return "За служителя има регистриран потребител и изтриването не е позволено!";
		}

		Number count;
		try {
			count = (Number) createNativeQuery("select count (*) as cnt from ADM_REFERENTS where CODE = ?1") //
				.setParameter(1, referent.getCode()).getResultList().get(0);
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на история за служител.", e);
		}
		if (count != null && count.intValue() > 1) { // това трябва да е валидира от екрана, но за всеки случай 3
			return "Служителят има история и изтриването не е позволено!";
		}

		return null; // явно може
	}

	/**
	 * За Бутон <Напускане> на служители
	 *
	 * @param selected
	 * @param dateAction
	 * @param closeNote
	 * @param systemData
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void closeAdmEmployeeList(List<Object[]> selected, Date dateAction, String closeNote, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		for (Object[] row : selected) {
			Integer code = SearchUtils.asInteger(row[0]);

			Referent empl = findByCode(code, dateAction, false);
			empl.setRefInfo(closeNote);

			closeAdmReferent(empl, dateAction, systemData, false);
		}
	}

	/**
	 * За Бутон <Закриване> на звено
	 *
	 * @param referent
	 * @param dateAction
	 * @param systemData
	 * @param delete
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public Referent closeAdmReferent(Referent referent, Date dateAction, BaseSystemData systemData, boolean delete) throws DbErrorException, ObjectInUseException {
		boolean zveno = Objects.equals(referent.getRefType(), CODE_ZNACHENIE_REF_TYPE_ZVENO);

		dateAction = DateUtils.startDate(dateAction);

		if (zveno) { // да се валидира дали има подчинени в него към дата
			int count;
			try {
				String sql = " select count (*) from ADM_REFERENTS where CODE_PARENT = :codeParent and DATE_OT <= :dateAction and DATE_DO > :dateAction ";

				Query query = createNativeQuery(sql) //
					.setParameter("codeParent", referent.getCode()).setParameter("dateAction", dateAction); //

				count = ((Number) query.getResultList().get(0)).intValue();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на подчинени обекти!", e);
			}

			if (count > 0) {
				throw new ObjectInUseException("В звеното или в подчинените му звена има назначени служители."
					+ " Преди да го закриете трябва да се преместят/напуснат назначените служители и да се преместят/закрият подчинените му звена.");
			}
		}

		Referent next = findElement(referent.getCodeParent(), referent.getCode(), dateAction);
		if (next != null) { // за да не остане дупка от там където този се маха
			reorderElement(referent.getCodePrev(), dateAction, next);
		}

		if (delete) {
			delete(referent);

		} else {
			referent.setDateDo(dateAction);

			Referent saved = save(referent);

			saved.setDbRefName(referent.getRefName());
			if (zveno) {
				saved.setDbRefRegistratura(referent.getRefRegistratura());
			} else {
				saved.setDbEmplPosition(referent.getEmplPosition());
				saved.setDbContactEmail(referent.getContactEmail());
				saved.setDbEmplContract(referent.getEmplContract());
			}
		}

		if (!zveno) { // за служител ако има потребител го правя неактивен
			if (systemData.matchClassifItems(CODE_CLASSIF_USERS, referent.getCode(), dateAction)) {
				AdmUserDAO userDao = new AdmUserDAO(getUser());
				AdmUser user = userDao.findById(referent.getCode());
				if (user != null) {

					user.setStatus(Constants.CODE_ZNACHENIE_STATUS_INACTIVE);
					user.setStatusDate(new Date());
					user.setStatusExplain("Напускане на служител");

					userDao.save(user);

					systemData.reloadClassif(CODE_CLASSIF_USERS, false, false);
				}
			}
		}

		return referent;
	}

	/**
	 * Дава списък история на променитете за подадения обект, като изтегля само данните от вида:<br>
	 * <b>Звено</b><br>
	 * [0]-REF_ID<br>
	 * [1]-REF_NAME<br>
	 * [2]-PARENT_NAME<br>
	 * [3]-REF_REGISTRATURA<br>
	 * [4]-NFL_EIK<br>
	 * [5]-REF_INFO<br>
	 * [6]-DATE_OT<br>
	 * [7]-DATE_DO<br>
	 * [8]-PRED_NAME<br>
	 * <br>
	 * <b>Служител</b><br>
	 * [0]-REF_ID<br>
	 * [1]-REF_NAME<br>
	 * [2]-ZVENO_NAME<br>
	 * [3]-EMPL_POSITION<br>
	 * [4]-NFL_EIK<br>
	 * [5]-REF_INFO<br>
	 * [6]-DATE_OT<br>
	 * [7]-DATE_DO<br>
	 * [8]-PRED_NAME<br>
	 *
	 * @param code
	 * @param type
	 * @return
	 */
	public SelectMetadata createSelectAdmHistory(Integer code, Integer type) {
		String select;
		if (Objects.equals(type, CODE_ZNACHENIE_REF_TYPE_ZVENO)) {
			select = " select r.REF_ID a0, r.REF_NAME a1, p.REF_NAME a2, r.REF_REGISTRATURA a3, r.NFL_EIK a4, r.REF_INFO a5, r.DATE_OT a6, r.DATE_DO a7, pred.REF_NAME a8 ";
		} else {
			select = " select r.REF_ID a0, r.REF_NAME a1, p.REF_NAME a2, r.EMPL_POSITION a3, r.FZL_EGN a4, r.REF_INFO a5, r.DATE_OT a6, r.DATE_DO a7, pred.REF_NAME a8 ";
		}

		String from = " from ADM_REFERENTS r " //
			+ " left outer join ADM_REFERENTS p on p.CODE = r.CODE_PARENT and p.DATE_OT <= r.DATE_OT and p.DATE_DO > r.DATE_OT " //
			+ " left outer join ADM_REFERENTS pred on pred.CODE = r.CODE_PREV and pred.DATE_OT <= r.DATE_OT and pred.DATE_DO > r.DATE_OT ";

		String where = " where r.CODE = :codeRef ";

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSql(select + from + where);
		sm.setSqlParameters(Collections.singletonMap("codeRef", code));

		return sm;
	}

	/**
	 * Дава списък на служителите в звеното, като изтегля само данните от вида:<br>
	 * [0]-CODE<br>
	 * [1]-SORT_NO<br>
	 * [2]-REF_NAME<br>
	 * [3]-EMPL_POSITION<br>
	 * [4]-CONTACT_EMAIL<br>
	 * [5]-DATE_OT<br>
	 * !!! Ако резултата е NULL значи няма подчинени служители. Това е така за да не се пуска излишен селект.
	 *
	 * @param zvenoCode
	 * @param dateAction
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws UnexpectedResultException
	 */
	public SelectMetadata createSelectEmployeeList(Integer zvenoCode, Date dateAction, BaseSystemData systemData) throws DbErrorException, UnexpectedResultException {
		List<SystemClassif> items = selectEmployees(zvenoCode, dateAction, null, false);
		if (items.isEmpty()) {
			return null; // текущото няма подчинени служители
		}
		SysClassifUtils.doSortClassifPrev(items); // правилната подредба !!!

		List<Integer> ids = new ArrayList<>(); // тези ще се изтеглят от БД

		StringBuilder caseSql = new StringBuilder(" , case ");
		for (int i = 0; i < items.size(); i++) {
			SystemClassif item = items.get(i);

			ids.add(item.getCode());

			if (i < 255) {
				caseSql.append(" when r.CODE=" + item.getCode() + " then " + i);
			}
		}
		caseSql.append(" else 100000 end "); // който и да е ще е на последно място и вече по азбучен ред

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		select.append(" select r.CODE a0 ");
		select.append(caseSql + " a1 ");
		select.append(" , r.REF_NAME a2, r.EMPL_POSITION a3, r.CONTACT_EMAIL a4, r.DATE_OT a5 ");

		String from = " from ADM_REFERENTS r ";
		String where = " where r.CODE_CLASSIF = :codeClassif and r.CODE in (:ids) and DATE_OT <= :dateAction and DATE_DO > :dateAction ";

		params.put("codeClassif", Constants.CODE_CLASSIF_ADMIN_STR);
		params.put("ids", ids);
		params.put("dateAction", dateAction);

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSql(select + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Иизтегля само данните от вида:<br>
	 * [0]-CODE<br>
	 * [1]-REF_NAME<br>
	 * [2]-REF_TYPE<br>
	 * [3]-CODE_PARENT<br>
	 *
	 * @param refName
	 * @param dateAction
	 * @return
	 */
	public SelectMetadata createSelectReferentSearch(String refName, Date dateAction) {
		dateAction = DateUtils.startDate(dateAction);

		Map<String, Object> params = new HashMap<>();

		String select = " select r.CODE a0, r.REF_NAME a1, r.REF_TYPE a2, r.CODE_PARENT a3 ";
		String from = " from ADM_REFERENTS r ";
		StringBuilder where = new StringBuilder();

		where.append(" where r.CODE_CLASSIF = :codeClassif and r.DATE_OT <= :dateAction and r.DATE_DO > :dateAction ");
		params.put("codeClassif", CODE_CLASSIF_ADMIN_STR);
		params.put("dateAction", dateAction);

		refName = SearchUtils.trimToNULL_Upper(refName);
		if (refName != null) {
			where.append(" and upper(r.REF_NAME) like :refName ");
			params.put("refName", "%" + refName + "%");
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSql(select + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/** Изписват се валидации преди реално да се извика изтриванто. Ако не е позволено да се трие се дава ObjectInUseException */
	@Override
	public void delete(Referent entity) throws DbErrorException, ObjectInUseException {
		try {
			Integer cnt;

				cnt = asInteger( // ADM_USERS.USER_ID
					createNativeQuery("select count (*) as cnt from ADM_USERS where USER_ID = :codeRef") //
						.setParameter("codeRef", entity.getCode()) //
						.getResultList().get(0));
				if (cnt != null && cnt.intValue() > 0) {
					throw new ObjectInUseException("За лицето има регистриран потребител и не може да бъде изтрито!");
				}

		} catch (ObjectInUseException e) {
			throw e; // за да не се преопакова

		} catch (Exception e) {
			throw new DbErrorException("Грешка при проверка за подчинени обекти при изтриване на участник в процеса!", e);
		}

		super.delete(entity);
	}

	/**
	 * Изтриване на елемент от админ структ. Отделен метод е защото се прави презсвързване на елементите на дървото
	 *
	 * @param referent
	 * @param dateAction
	 * @param systemData
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void deleteAdmReferent(Referent referent, Date dateAction, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		boolean delete = false; // може да има изтриване, но може и да има само затваряне на интервали

		if (Objects.equals(referent.getRefType(), CODE_ZNACHENIE_REF_TYPE_EMPL)) {
			String msg = checkEmplDeleteAllowed(referent, dateAction, systemData);
			if (msg != null) {
				throw new ObjectInUseException(msg);
			}
			delete = true; // ако има само един ред и равни дати и няма потребител имаме реално изтриване
		}

//		try { // изтриването е затваряне с днешна дата !
			closeAdmReferent(referent, dateAction, systemData, delete);

//		} catch (ObjectInUseException e) {
//			boolean zveno = Objects.equals(referent.getRefType(), CODE_ZNACHENIE_REF_TYPE_ZVENO);
//			if (zveno) {
//				throw new ObjectInUseException("Звеното не може да бъде изтрито, тъй като е свързано с други данни системата.");
//			}
//			throw new ObjectInUseException("Служителят не може да бъде изтрит, тъй като е свързан с други данни в системата.");
//		}
	}

	/**
	 * Търсене на референт по код
	 *
	 * @param code
	 * @param dateAction
	 * @param edit       при <code>true</code> се зареждат данни за екрана за редакция
	 * @return
	 * @throws DbErrorException
	 */
	public Referent findByCode(Integer code, Date dateAction, boolean edit) throws DbErrorException {
		Referent referent;
		try {
			Query query = createQuery("select r from Referent r where r.code = :code and r.dateOt <= :dateAction and r.dateDo > :dateAction") //
				.setParameter("code", code).setParameter("dateAction", dateAction);

			@SuppressWarnings("unchecked")
			List<Referent> list = query.getResultList();
			if (list.isEmpty()) {
				return null;
			}
			referent = list.get(0);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на участник в процеса по код!", e);
		}

		if (edit) {
			referent.setDbRefName(referent.getRefName());

			if (Objects.equals(referent.getRefType(), CODE_ZNACHENIE_REF_TYPE_ZVENO)) {
				referent.setDbRefRegistratura(referent.getRefRegistratura());

			} else if (Objects.equals(referent.getRefType(), CODE_ZNACHENIE_REF_TYPE_EMPL)) {

				referent.setDbEmplPosition(referent.getEmplPosition());
				referent.setDbContactEmail(referent.getContactEmail());
				referent.setDbEmplContract(referent.getEmplContract());
			}

			findReferentAddress(referent);
		}

		return referent;
	}

	/** Допълнително добавя и адреса ако го има */
	@Override
	public Referent findById(Object id) throws DbErrorException {
		Referent referent = super.findById(id);
		if (referent == null) {
			return referent;
		}

		findReferentAddress(referent);

		return referent;
	}

	/**
	 * Дава списък на звената в подходящ вид за показване в дърво
	 *
	 * @param dateAction към тази дата се искат звената
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> findZvenoList(Date dateAction, BaseSystemData systemData) throws DbErrorException {
		// трябват ми само звената и пускам специфика по тип=звено
		Map<Integer, Object> map = Collections.singletonMap(UriregClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, CODE_ZNACHENIE_REF_TYPE_ZVENO);

		List<SystemClassif> items = systemData.queryClassification(CODE_CLASSIF_ADMIN_STR, null, dateAction, getUserLang(), map);

		for (SystemClassif item : items) { // липсват служители и трябва да се оправи дървото за да се покаже коректно
			if (item.getCodePrev() == 0) {
				continue; // щом е първи значи е ОК
			}

			Integer refType = (Integer) systemData.getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCodePrev(), getUserLang(), dateAction, UriregClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);
			if (Objects.equals(refType, CODE_ZNACHENIE_REF_TYPE_EMPL)) {
				item.setCodePrev(0); // ако предходният е служител значи този става първи
			}
		}
		return items;
	}

	/**
	 * Преместване на списък служители в ново звено
	 *
	 * @param selected   {@link #createSelectEmployeeList(Integer, Date, BaseSystemData)}
	 * @param parent
	 * @param dateAction
	 * @param moveNote   бележка при местенето (сетва се еднаква на всички)
	 * @param systemData
	 * @throws DbErrorException
	 * @throws UnexpectedResultException
	 * @throws InvalidParameterException
	 */
	public void moveAdmEmployeeList(List<Object[]> selected, Integer parent, Date dateAction, String moveNote, BaseSystemData systemData)
		throws DbErrorException, InvalidParameterException, UnexpectedResultException {
		dateAction = DateUtils.startDate(dateAction);

		for (Object[] row : selected) {
			Integer code = SearchUtils.asInteger(row[0]);

			Referent referent = findByCode(code, dateAction, false);
			JPA.getUtil().getEntityManager().detach(referent);

			boolean newDate = referent.getDateOt().getTime() != dateAction.getTime();

			Referent next1 = findElement(referent.getCodeParent(), referent.getCode(), dateAction);
			if (next1 != null) { // за да не остане дупка от там където този се маха
				reorderElement(referent.getCodePrev(), dateAction, next1);
			}

			referent.setCodeParent(parent);
			referent.setDateOt(dateAction);

			calculateCodePrev(referent, referent.getCode(), systemData);
			Referent next2 = findElement(referent.getCodeParent(), referent.getCodePrev(), dateAction);
			if (next2 != null) { // новия се пъха пред този
				reorderElement(referent.getCode(), dateAction, next2);
			}

			if (newDate) { // ако е нова дата трябва и текущия да се затвори и да му се направи нов ред
				updateElement(referent.getId(), null, dateAction); // старият ред се затваря

				referent.setId(null); // трябва да е нов ред
				referent.setUserLastMod(null);
				referent.setDateLastMod(null);
			}

			referent.setRefInfo(moveNote);

			super.save(referent);
		}
	}

	/**
	 * Преместване на звено. Забележката се сетва в referent.refInfo
	 *
	 * @param referent
	 * @param prev
	 * @param parent
	 * @param dateAction
	 * @param useParentRegistratura ако е <code>true</code> това означава че се актуализира реда като му се сменя регистратурата,
	 *                              като се взима тази от родителя
	 * @param moveOnlyChildren      при <code>true</code> се местят само подчинените
	 * @param systemData
	 * @return
	 * @throws UnexpectedResultException
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 * @throws ObjectInUseException
	 */
	public Referent moveAdmZveno(Referent referent, Integer prev, Integer parent, Date dateAction, boolean useParentRegistratura, boolean moveOnlyChildren, BaseSystemData systemData)
		throws DbErrorException, UnexpectedResultException, InvalidParameterException, ObjectInUseException {

		if (!moveOnlyChildren && referent.getCodeParent().equals(parent) //
			&& (referent.getCode().equals(prev) || referent.getCodePrev().equals(prev))) {

			return referent; // нищо не се мести
		}

		if (moveOnlyChildren && referent.getCode().equals(parent)) {
			return referent; // нищо не се мести
		}

		dateAction = DateUtils.startDate(dateAction);

		if (moveOnlyChildren) {

			List<SystemClassif> children = systemData.getChildrenOnNextLevel(CODE_CLASSIF_ADMIN_STR, referent.getCode(), dateAction, getUserLang());
			for (SystemClassif item : children) {
				item.setCodeParent(0); // за да мога после тази редица да е сортирам po codePrev
			}
			SysClassifUtils.doSortClassifPrev(children);

			for (int i = children.size() - 1; i >= 0; i--) {

				Referent child = findByCode(children.get(i).getCode(), dateAction, false);

				JPA.getUtil().getEntityManager().detach(child);

				if (Objects.equals(child.getRefType(), CODE_ZNACHENIE_REF_TYPE_EMPL)) {
					List<Object[]> tmp = new ArrayList<>();
					tmp.add(new Object[] { child.getCode() });

					moveAdmEmployeeList(tmp, parent, dateAction, "Преместване на всички подчинени в ново звено.", systemData);
				} else {
					// тези двете ще трябват при последващите анализи
					child.setDbRefName(child.getRefName());
					child.setDbRefRegistratura(child.getRefRegistratura());

					moveAdmZveno(child, prev, parent, dateAction, useParentRegistratura, false, systemData);
				}
			}

			return referent; // няма промяна в този
		}

		boolean newDate = referent.getDateOt().getTime() != dateAction.getTime();

		if (useParentRegistratura && parent != null && parent.intValue() != 0) { // само ако се иска и то не се мести на първо
																					// ниво
			Integer parentRegistratura = (Integer) systemData.getItemSpecific(CODE_CLASSIF_ADMIN_STR, parent, getUserLang(), dateAction, UriregClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
			referent.setRefRegistratura(parentRegistratura);

		}

		Referent next1 = findElement(referent.getCodeParent(), referent.getCode(), dateAction);
		if (next1 != null) { // за да не остане дупка от там където този се маха
			reorderElement(referent.getCodePrev(), dateAction, next1);
		}

		referent.setCodeParent(parent);
		referent.setCodePrev(prev);
		referent.setDateOt(dateAction);

		calculateCodePrev(referent, referent.getCode(), systemData);

		Referent next2 = findElement(referent.getCodeParent(), referent.getCodePrev(), dateAction);
		if (next2 != null) { // новия се пъха пред този
			reorderElement(referent.getCode(), dateAction, next2);
		}

		if (newDate) { // ако е нова дата трябва и текущия да се затвори и да му се направи нов ред
			updateElement(referent.getId(), null, dateAction); // старият ред се затваря

			referent.setId(null); // трябва да е нов ред
			referent.setUserLastMod(null);
			referent.setDateLastMod(null);
		}

		if (!Objects.equals(referent.getDbRefRegistratura(), referent.getRefRegistratura())) {
			updateZveno(referent, dateAction, systemData); // за да оправи регистратурата и на подчинените звена, като това
															// извикване няма да промени нищо в текущия обект
		}

		Referent saved = save(referent);

		saved.setDbRefName(referent.getRefName());
		saved.setDbRefRegistratura(referent.getRefRegistratura());

		return saved;
	}

	/**
	 * Възстановяване на напуснал служител в актуалното състояние на Административната структура
	 *
	 * @param item
	 * @param codeParent
	 * @param dateAction
	 * @param systemData
	 * @param eraseAccess за изтриване на достъпа
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 * @throws UnexpectedResultException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	public Referent returnLeftEmployee(SystemClassif item, Integer codeParent, Date dateAction, BaseSystemData systemData, boolean eraseAccess)
		throws DbErrorException, InvalidParameterException, UnexpectedResultException, ObjectInUseException {

		List<Referent> employees;
		try {
			employees = createQuery("select r from Referent r where r.code = :code order by r.dateOt desc, r.id desc") //
				.setParameter("code", item.getCode()).setMaxResults(1).getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на последно състояние на напуснал служител.", e);
		}
		if (employees.isEmpty()) {
			throw new InvalidParameterException("Не е намерен напуснал служител с код=" + item.getCode());
		}
		Referent employee = employees.get(0);

		// трябва да се валидира дали със сигурност е напуснал, че ще се намажат нещата
		Date dateOt = DateUtils.startDate(employee.getDateOt());
		Date dateDo = DateUtils.startDate(employee.getDateDo());
		dateAction = DateUtils.startDate(dateAction);

		if (dateDo.getTime() > dateAction.getTime()) {
			throw new InvalidParameterException("Избраният служител с код=" + item.getCode() + " не е напуснал.");
		}

		if (dateOt.getTime() == dateAction.getTime()) { // този безмислен ред трябда да се изтрие, защото новия ще съвпадне по
														// дата от
			remove(employee);
			JPA.getUtil().flush();
		}

		if (JPA.getUtil().isAttached(employee)) {
			JPA.getUtil().getEntityManager().detach(employee);
		}

		employee.setId(null);
		employee.setDateLastMod(null);
		employee.setUserLastMod(null);
		employee.setCodeParent(codeParent); // трябва да знам къде влиза
		employee.setCodePrev(null); // ще се изчисли при записа
		employee.setDateOt(null);
		employee.setDateDo(null);
		employee.setRefType(CODE_ZNACHENIE_REF_TYPE_EMPL); // задължително, защото ако има мигрирани и става мазало

		employee = saveAdmReferent(employee, dateAction, systemData);

		return employee;
	}

	/** */
	@Override
	public Referent save(Referent referent) throws DbErrorException {
		referent.fixEmptyStringValues();

		referent.setAuditable(false); // за да не запише ред в журнала при записа на обекта

		int codeAction;
		if (referent.getId() == null) {
			codeAction = CODE_DEIN_ZAPIS;

			if (referent.getCode() == null) {
				referent.setCode(nextVal("SEQ_ADM_REFERENTS_CODE"));
			}
			if (referent.getCodePrev() == null) {
				referent.setCodePrev(0);
			}
			if (referent.getCodeParent() == null) {
				referent.setCodeParent(0);
			}
			if (referent.getDateOt() == null) {
				referent.setDateOt(DateUtils.systemMinDate());
			} else if (referent.getRefType().equals(CODE_ZNACHENIE_REF_TYPE_NFL) || referent.getRefType().equals(CODE_ZNACHENIE_REF_TYPE_FZL)) {
				referent.setDateOt(DateUtils.startDate(referent.getDateOt()));
			}
			if (referent.getDateDo() == null) {
				referent.setDateDo(DateUtils.systemMaxDate());
			}

		} else {
			codeAction = CODE_DEIN_KOREKCIA;
		}

		Referent saved = super.save(referent); // основния запис

		ReferentAddressDAO addressDao = new ReferentAddressDAO(getUser());
		ReferentAddress address = referent.getAddress(); // адреса се взима обекта преди да е направен запис!!!
		if (address != null //
			&& address.getAddrCountry() != null) { // критерия да има въведен адрес

			if (address.getId() == null) { // при нов му задавам вид и участник
				address.setCodeRef(saved.getCode());
				address.setAddrType(CODE_ZNACHENIE_ADDR_TYPE_CORRESP);
			}
			address.fixEmptyStringValues();

			address = addressDao.save(address);
			saved.setAddress(address);

		} else if (referent.getDbAddressId() != null) { // не е въведен сега, но преди е имало. ще се прави изтриване на адрес
			try {
				addressDao.delete(address);
			} catch (ObjectInUseException e) {
				throw new DbErrorException(e);
			}
			saved.setAddress(new ReferentAddress());

		} else {
			saved.setAddress(referent.getAddress());
		}

		if (saved.getAddress() != null) {
			saved.setDbAddressId(saved.getAddress().getId());
		} else {
			saved.setDbAddressId(null);
		}
		referent.setDbAddressId(saved.getDbAddressId());
		referent.setAddress(saved.getAddress());

		saveAudit(saved, codeAction); // тука вече трябва да всичко да е насетвано и записа в журнала е ОК
		saved.setAuditable(true);
		return saved;
	}

	/**
	 * Запис на елемент от административна структура
	 *
	 * @param referent
	 * @param dateAction
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 * @throws UnexpectedResultException
	 * @throws ObjectInUseException
	 */
	public Referent saveAdmReferent(Referent referent, Date dateAction, BaseSystemData systemData) throws DbErrorException, InvalidParameterException, UnexpectedResultException, ObjectInUseException {
		if (referent.getRefName() != null) {
			referent.setRefName(referent.getRefName().trim()); // важно е да няма интервали в началото и края
		}
		boolean zveno = Objects.equals(referent.getRefType(), CODE_ZNACHENIE_REF_TYPE_ZVENO);

		dateAction = DateUtils.startDate(dateAction);

		if (zveno) {
			checkEikDuplicate(referent, dateAction);
		}

		Referent next = null; // ако има нещо тука ще се пресвързват

		if (referent.getId() == null) { // чисто нов, като се предполага че codeParent и codePrev(само за звено) са сетнати,
										// както и всичко останало по полетата в таблицата

			referent.setCodeClassif(CODE_CLASSIF_ADMIN_STR);
			referent.setDateOt(dateAction);

			calculateCodePrev(referent, referent.getCode(), systemData); // за да се сложи на правилното място

			next = findElement(referent.getCodeParent(), referent.getCodePrev(), referent.getDateOt()); // трябва да се види
																										// новият къде влиза
		} else { // корекция

			if (zveno) {
				updateZveno(referent, dateAction, systemData);

			} else {
				next = updateEmployee(referent, dateAction, systemData);
			}
		}

		Referent saved = save(referent);

		if (next != null) { // има нещо което трябва да се размести, защото текущия с пъха там
			reorderElement(saved.getCode(), dateAction, next);
		}

		saved.setDbRefName(referent.getRefName());
		if (zveno) {
			saved.setDbRefRegistratura(referent.getRefRegistratura());
		} else {
			saved.setDbEmplPosition(referent.getEmplPosition());
			saved.setDbContactEmail(referent.getContactEmail());
			saved.setDbEmplContract(referent.getEmplContract());
		}

		return saved;
	}

	/**
	 * Преди да се изтрие документа трябва да се изтрият и други данни, които не са мапнати дирекно през JPA
	 */
	@Override
	protected void remove(Referent entity) throws DbErrorException, ObjectInUseException {
		try {
			if (entity.getDbAddressId() != null) { // трябва да се трие и адреса, защото го има
				int deleted = createNativeQuery("delete from ADM_REF_ADDRS where ADDR_ID = ?1").setParameter(1, entity.getDbAddressId()).executeUpdate();
				LOGGER.debug("Изтрит е {} адрес с ADDR_ID={} за CODE_REF={}", deleted, entity.getDbAddressId(), entity.getCode());
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на свързани обекти на участник в процеса!", e);
		}

		super.remove(entity);
	}

	/**
	 * @param referent
	 * @throws DbErrorException
	 */
	void findReferentAddress(Referent referent) throws DbErrorException {
		try {
			@SuppressWarnings("unchecked")
			List<ReferentAddress> addresses = createQuery("select x from ReferentAddress x where x.codeRef = :codeRefArg and x.addrType = :addrTypeArg") //
				.setParameter("codeRefArg", referent.getCode()).setParameter("addrTypeArg", CODE_ZNACHENIE_ADDR_TYPE_CORRESP) //
				.getResultList();
			if (addresses != null && !addresses.isEmpty()) {
				referent.setAddress(addresses.get(0));
				referent.setDbAddressId(referent.getAddress().getId());
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на адрес за участник в процеса!", e);
		}
	}

	/**
	 * @param referent
	 * @param dateAction
	 * @throws DbErrorException
	 * @throws UnexpectedResultException
	 * @throws InvalidParameterException
	 */
	Referent updateEmployee(Referent referent, Date dateAction, BaseSystemData systemData) throws DbErrorException, InvalidParameterException, UnexpectedResultException {
		Referent next = null;

		boolean dateChanged = referent.getDateOt().getTime() != dateAction.getTime();
		boolean nameChanged = !Objects.equals(referent.getRefName(), referent.getDbRefName());
		boolean emplPositionChanged = !Objects.equals(referent.getEmplPosition(), referent.getDbEmplPosition());
		boolean contactEmailChanged = !Objects.equals(referent.getContactEmail(), referent.getDbContactEmail());
		boolean emplContractChanged = !Objects.equals(referent.getEmplContract(), referent.getDbEmplContract());

		if (nameChanged || emplPositionChanged) { // има смяна на име или смяна на длъжност и ще се гледа трябва ли да се
													// пренарежда списъка със служителите, като няма значение датата

			Integer currentPrev = referent.getCodePrev();

			referent.setDateOt(dateAction);

			calculateCodePrev(referent, null, systemData);

			if (!Objects.equals(currentPrev, referent.getCodePrev())) { // лошо! ще се мести

				Referent move = findElement(referent.getCodeParent(), referent.getCode(), dateAction);
				if (move != null) { // за да не остане дупка от там където този се маха
					reorderElement(currentPrev, dateAction, move);
				}

				// променения ще се мушне пред този (ако се открие следващ)
				next = findElement(referent.getCodeParent(), referent.getCodePrev(), dateAction);
			}
		}

		if (dateChanged && (nameChanged || emplPositionChanged || emplContractChanged)) {

			updateElement(referent.getId(), null, dateAction); // старият ред се затваря

			referent.setId(null); // трябва да е нов ред
			referent.setUserLastMod(null);
			referent.setDateLastMod(null);
			referent.setDateOt(dateAction);
		}

		if (nameChanged || contactEmailChanged) { // ако вече има потребител за този слжужител трябва да се синхронизират данните
			if (systemData.matchClassifItems(CODE_CLASSIF_USERS, referent.getCode(), dateAction)) {
				AdmUserDAO userDao = new AdmUserDAO(getUser());
				AdmUser user = userDao.findById(referent.getCode());
				if (user != null) {

					user.setNames(referent.getRefName());
					user.setEmail(referent.getContactEmail());

					userDao.save(user);

					systemData.reloadClassif(CODE_CLASSIF_USERS, false, false);
				}
			}
		}

		return next;
	}

	/**
	 * @param referent
	 * @param systemData
	 * @param dateAction
	 * @throws DbErrorException
	 * @throws UnexpectedResultException
	 * @throws InvalidParameterException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	void updateZveno(Referent referent, Date dateAction, BaseSystemData systemData) throws DbErrorException, InvalidParameterException, UnexpectedResultException, ObjectInUseException {

		boolean dateChanged = referent.getDateOt().getTime() != dateAction.getTime();
		boolean nameChanged = !Objects.equals(referent.getRefName(), referent.getDbRefName());
		boolean registraturaChanged = !Objects.equals(referent.getDbRefRegistratura(), referent.getRefRegistratura());

		if (dateChanged && (nameChanged || registraturaChanged)) {
			updateElement(referent.getId(), null, dateAction); // старият ред се затваря

			referent.setDateOt(dateAction);

			referent.setId(null); // трябва да е нов запис в БД
			referent.setUserLastMod(null);
			referent.setDateLastMod(null);
		}

		if (registraturaChanged) { // има смяна на регистратура трябва подчинените звена да се оправят с новата регистратура
			List<Referent> podZvenaList;
			try {
				String sql = " select r from Referent r where r.codeParent = :codeParent and r.refType = :zvenoType " //
					+ " and r.refRegistratura = :registratura and r.dateOt <= :dateAction and r.dateDo > :dateAction ";

				Query query = createQuery(sql) //
					.setParameter("codeParent", referent.getCode()).setParameter("zvenoType", CODE_ZNACHENIE_REF_TYPE_ZVENO) //
					.setParameter("registratura", referent.getDbRefRegistratura()) // само тези, които са били към старата
																					// регистратура
					.setParameter("dateAction", dateAction); //

				podZvenaList = query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на подчинени звена!", e);
			}

			for (Referent zveno : podZvenaList) {
				JPA.getUtil().getEntityManager().detach(zveno);

				// данните от БД за анализ в метода за запис
				zveno.setDbRefName(zveno.getRefName());
				zveno.setDbRefRegistratura(zveno.getRefRegistratura());

				// новата регистратура
				zveno.setRefRegistratura(referent.getRefRegistratura());

				saveAdmReferent(zveno, dateAction, systemData);
			}
		}
	}

	/**
	 * Изчислява код предходен заради по особения вид на дървото
	 *
	 * @param referent
	 * @param excludeCode
	 * @param systemData
	 * @throws InvalidParameterException
	 * @throws DbErrorException
	 * @throws UnexpectedResultException
	 */
	private void calculateCodePrev(Referent referent, Integer excludeCode, BaseSystemData systemData) throws InvalidParameterException, DbErrorException, UnexpectedResultException {
		boolean zveno = Objects.equals(referent.getRefType(), CODE_ZNACHENIE_REF_TYPE_ZVENO);

		if (referent.getCodeParent() == null) {
			throw new InvalidParameterException("Задължително е да се посочи място в дървото! CodeParent is NULL !");
		}
		if (zveno && referent.getCodePrev() == null) {
			throw new InvalidParameterException("Задължително е да се посочи място в дървото! CodePrev is NULL !");
		}

		if (zveno && referent.getCodePrev().intValue() == 0) { // ако е звено и код прев е 0 трябва да се сложи след последния
																// служител (ако има) !!!
			List<SystemClassif> items = selectEmployees(referent.getCodeParent(), referent.getDateOt(), excludeCode, false);
			if (items.isEmpty()) {
				return;
			}

			SysClassifUtils.doSortClassifPrev(items); // правилната подредба !!!

			referent.setCodePrev(items.get(items.size() - 1).getCode()); // отива след последния
		}

		if (!zveno) { // служител и трябва да се определи къде ще се бутне в списъка
			referent.setCodePrev(0); // ако не му се открие място ще е първи

			List<SystemClassif> items = selectEmployees(referent.getCodeParent(), referent.getDateOt(), excludeCode, true);
			if (items.isEmpty() || excludeCode == null && items.size() == 1) {
				return;
			}

			SysClassifUtils.doSortClassifPrev(items); // правилната подредба !!!

			String referentSort = null;
			if (referent.getEmplPosition() != null) {
				SystemClassif position = systemData.decodeItemLite(CODE_CLASSIF_POSITION, referent.getEmplPosition(), getUserLang(), referent.getDateOt(), false);
				if (position != null) {
					referentSort = trimToNULL(position.getCodeExt());
				}
			}
			if (referentSort == null) {
				referentSort = "___" + referent.getRefName();
			} else {
				String s = referentSort.length() == 1 ? "0" : "";
				referentSort = s + referentSort + "___" + referent.getRefName();
			}

			for (int i = items.size() - 1; i >= 0; i--) {
				SystemClassif item = items.get(i);

				String itemSort;
				if (item.getCodeExt() == null) {
					itemSort = "___" + item.getTekst();
				} else {
					String s = item.getCodeExt().length() == 1 ? "0" : "";
					itemSort = s + item.getCodeExt() + "___" + item.getTekst();
				}

				if (itemSort.compareTo(referentSort) < 0 && !Objects.equals(referent.getCode(), item.getCode())) {
					referent.setCodePrev(item.getCode());
					break;
				}
			}
		}
	}

	/**
	 * @param referent
	 * @param dateAction
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	private void checkEikDuplicate(Referent referent, Date dateAction) throws DbErrorException, ObjectInUseException {
		String eik = SearchUtils.trimToNULL(referent.getNflEik());
		if (eik == null) {
			return;
		}
		boolean found;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select count (*) from ADM_REFERENTS where NFL_EIK = :eik and DATE_OT <= :dateAction and DATE_DO > :dateAction ");
			if (referent.getId() != null) {
				sql.append(" and CODE != :codeRef ");
			}

			Query query = createNativeQuery(sql.toString()).setParameter("eik", eik).setParameter("dateAction", dateAction);
			if (referent.getId() != null) {
				query.setParameter("codeRef", referent.getCode());
			}

			int count = ((Number) query.getResultList().get(0)).intValue();
			found = count > 0;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при проверка на уникалност на ЕИК на звено!", e);
		}

		if (found) {
			throw new ObjectInUseException("В Административната структура вече има създадено звено с този ЕИК.");
		}
	}

	/**
	 * Намира реда(ако има) по подадените аргументи
	 *
	 * @param codeParent
	 * @param codePrev
	 * @param dateAction
	 * @return
	 * @throws DbErrorException
	 */
	private Referent findElement(Integer codeParent, Integer codePrev, Date dateAction) throws DbErrorException {
		try {
			String sql = " select x from Referent x " //
				+ " where x.codeClassif = :codeClassif and x.codeParent = :codeParent and x.codePrev = :codePrev " //
				+ " and x.dateOt <= :dateAction and x.dateDo > :dateAction ";

			Query query = createQuery(sql).setParameter("codeClassif", CODE_CLASSIF_ADMIN_STR) //
				.setParameter("codeParent", codeParent).setParameter("codePrev", codePrev) //
				.setParameter("dateAction", dateAction);

			@SuppressWarnings("unchecked")
			List<Referent> list = query.getResultList();

			return list.isEmpty() ? null : list.get(0);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на на ред в административна структура!", e);
		}
	}

	/**
	 * Пренарежда елемента защото някой се пъха пред него
	 *
	 * @param codePrev
	 * @param dateAction
	 * @param next
	 * @throws DbErrorException
	 * @throws CloneNotSupportedException
	 */
	private void reorderElement(Integer codePrev, Date dateAction, Referent next) throws DbErrorException {

		if (next.getDateOt().getTime() == dateAction.getTime()) {
			updateElement(next.getId(), codePrev, null); // само се пренареждат

			JPA.getUtil().getEntityManager().detach(next);
			next.setCodePrev(codePrev);

		} else { // трябва да се направи история

			updateElement(next.getId(), null, dateAction); // този се затваря

			JPA.getUtil().getEntityManager().detach(next);

			next.setId(null);
			next.setUserLastMod(null);
			next.setDateLastMod(null);

			next.setDateOt(dateAction);
			next.setDateDo(DateUtils.systemMaxDate());
			next.setCodePrev(codePrev);

			super.save(next);
		}
	}

	/**
	 * Намира елементи от дървото на админ структурата, за да се прави анализ при подрежданията.
	 *
	 * @param codeParent
	 * @param dateAction
	 * @param excludeCode
	 * @param loadSortData
	 * @return
	 * @throws DbErrorException
	 */
	private List<SystemClassif> selectEmployees(Integer codeParent, Date dateAction, Integer excludeCode, boolean loadSortData) throws DbErrorException {
		List<SystemClassif> items = new ArrayList<>();

		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select r.CODE, r.CODE_PREV ");
			if (loadSortData) {
				sql.append(" , r.REF_NAME, p.CODE_EXT ");
			}
			sql.append(" from ADM_REFERENTS r ");
			if (loadSortData) {
				sql.append(
					" left outer join SYSTEM_CLASSIF p on p.CODE = r.EMPL_POSITION and p.CODE_CLASSIF = :positionClassif and p.DATE_OT <= :dateAction and (p.DATE_DO is null or p.DATE_DO > :dateAction) ");
			}
			sql.append(" where r.CODE_PARENT = :codeParent and r.REF_TYPE = :refType and r.DATE_OT <= :dateAction and r.DATE_DO > :dateAction");
			if (excludeCode != null) {
				sql.append(" and r.CODE != :excludeCode ");
			}

			Query query = createNativeQuery(sql.toString()) //
				.setParameter("codeParent", codeParent).setParameter("refType", CODE_ZNACHENIE_REF_TYPE_EMPL) //
				.setParameter("dateAction", dateAction);
			if (loadSortData) {
				query.setParameter("positionClassif", CODE_CLASSIF_POSITION);
			}
			if (excludeCode != null) {
				query.setParameter("excludeCode", excludeCode);
			}

			@SuppressWarnings("unchecked")
			List<Object[]> rows = query.getResultList();

			for (Object[] row : rows) {
				SystemClassif item = new SystemClassif();

				item.setCode(SearchUtils.asInteger(row[0]));
				item.setCodePrev(SearchUtils.asInteger(row[1]));

				if (loadSortData) {
					item.setTekst((String) row[2]);
					item.setCodeExt(trimToNULL((String) row[3])); // това не е добре тука да се сетва, но няма друг начин
				}

				items.add(item);
			}

		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return items;
	}

	/**
	 * Актуализира реда в зависимост от подадените аргументи
	 *
	 * @param refId
	 * @param dateDo
	 * @param codePrev
	 * @throws DbErrorException
	 */
	private void updateElement(Integer refId, Integer codePrev, Date dateDo) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();

			sql.append(" update ADM_REFERENTS set USER_LAST_MOD = :userLastMod, DATE_LAST_MOD = :dateLastMod ");
			if (codePrev != null) {
				sql.append(" , CODE_PREV = :codePrev ");
			}
			if (dateDo != null) {
				sql.append(" , DATE_DO = :dateDo ");
			}
			sql.append(" where REF_ID = :refId ");

			Query query = createNativeQuery(sql.toString()) //
				.setParameter("userLastMod", getUserId()).setParameter("dateLastMod", new Date()) //
				.setParameter("refId", refId);

			if (codePrev != null) {
				query.setParameter("codePrev", codePrev);
			}
			if (dateDo != null) {
				query.setParameter("dateDo", dateDo);
			}

			query.executeUpdate();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при актуализация на ред в административна структура!", e);
		}
	}
}
