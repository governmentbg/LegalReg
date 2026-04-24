package com.ib.urireg.db.dao;

import com.ib.system.SysConstants;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.db.dto.Lice;
import com.ib.urireg.db.dto.LiceDoc;
import com.ib.urireg.db.dto.Staj;
import com.ib.urireg.system.UriregClassifAdapter;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.SearchUtils;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.ib.system.SysConstants.CODE_CLASSIF_EKATTE;
import static com.ib.system.utils.SearchUtils.trimToNULL;
import static com.ib.urireg.system.UriregConstants.*;

public class LiceDAO extends AbstractDAO<Lice> {

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(LiceDAO.class);

	public LiceDAO(ActiveUser user) {
		super(Lice.class, user);
	}

	/**
	 * Търсене на лице по ЕГН/ЛНЧ
	 *
	 * @param egn
	 * @param lnc
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public Lice findByEgnLnc(String egn, String lnc) throws DbErrorException {
		egn = trimToNULL(egn);
		lnc = trimToNULL(lnc);
		try {
			Query query;
			if (egn != null) {
				query = JPA.getUtil().getEntityManager().createQuery( //
						" select x from Lice x where x.egn = :egn").setParameter("egn", egn);
			} else if (lnc != null) {
				query = JPA.getUtil().getEntityManager().createQuery( //
						" select x from Lice x where x.lnc = :lnc").setParameter("lnc", lnc);
			} else {
				return null;
			}

			List<Lice> list = query.getResultList();
			if (list.isEmpty()) {
				return null;
			}
			return list.get(0);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на лице по ЕГН/ЛНЧ", e);
		}
	}

	/**
	 * проверка на лице по ЕГН/ЛНЧ
	 *
	 * @param egn
	 * @param lnc
	 * @return false/true
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public boolean chckByEgnLnc(String egn, String lnc, Integer idLice) throws DbErrorException {
		egn = trimToNULL(egn);
		lnc = trimToNULL(lnc);
		try {
			Query query;
			String q;
			if (egn != null) {
				q = " select 1 from Lice x where x.egn = :egn ";
				if (idLice != null) {
					q += " and x.id <> :idLice ";
				}
				query = JPA.getUtil().getEntityManager().createQuery(q);
				query.setParameter("egn", egn);

			} else if (lnc != null) {
				q = " select 1 from Lice x where x.lnc = :lnc";
				if (idLice != null) {
					q += " and x.id <> :idLice ";
				}
				query = JPA.getUtil().getEntityManager().createQuery(q);
				query.setParameter("lnc", lnc);
			} else {
				return false;
			}

			if (idLice != null) {
				query.setParameter("idLice", idLice);
			}

			List<Object> list = query.getResultList();
			if (list.isEmpty()) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на лице по ЕГН/ЛНЧ", e);
		}
	}


	/**
	 * @param entity
	 * @return
	 * @throws DbErrorException
	 */
	@Override
	public Lice save(Lice entity) throws DbErrorException {
		if (entity != null) {

			entity.setEgn(trimToNULL(entity.getEgn()));
			entity.setLnc(trimToNULL(entity.getLnc()));

			StringBuilder names = new StringBuilder();
			if (!SearchUtils.isEmpty(entity.getFirstname())) {
				names.append(entity.getFirstname().trim());
			}
			if (!SearchUtils.isEmpty(entity.getSurname())) {
				if (names.length() > 0) {
					names.append(" ");
				}
				names.append(entity.getSurname().trim());
			}
			if (!SearchUtils.isEmpty(entity.getLastname())) {
				if (names.length() > 0) {
					names.append(" ");
				}
				names.append(entity.getLastname().trim());
			}
			if (names.length() > 0) {
				entity.setNames(names.toString());
			}

			if (!Objects.equals(entity.getStatus(), entity.getDbStatus())) {
				entity.setStatusDate(new Date());
			}
			entity.setDbStatus(entity.getStatus());
		}
		return super.save(entity);
	}

	/**
	 * Валидация дали смяната на статуса е коректна. Ако върне != null значи не може да се сменя и се показва текста.
	 *
	 * @param lice
	 * @param systemData
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 * @see Lice#getStatus()
	 * @see Lice#getDbStatus()
	 */
	public String checkStatusChangeAllowed(Lice lice, BaseSystemData systemData) throws DbErrorException, InvalidParameterException {
		if (lice.getStatus() == null || lice.getDbStatus() == null) {
			throw new InvalidParameterException("Некоректни данни. Липсва статус.");
		}
		if (lice.getStatus().equals(lice.getDbStatus())) {
			return null; // няма смяна на статуса и сме ок
		}

		if (lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_CANDIDATE)
				|| lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_CASE_APPROVED) //
				|| lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_EXAM_PASSED) //
				|| lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_EXAM_FAILED)) { //
			return "Този статус не може да бъде избран. Определя се автоматично от системата.";
		}

		String err;
		if (lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED)) {
			err = checkStajApproved(lice, systemData);

		} else if (lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_TEST_APPROVED)) {
			err = checkTestApproved(lice);

		} else if (lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN)) {
			err = checkPravoLishen(lice);

		} else if (lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_PRAVO)) {
			err = checkPravo(lice);

		} else {
			err = "Непознат статус.";
		}
		return err;
	}

	/**
	 * Проверява дали лицето може да премине в статус {@link com.ib.urireg.system.UriregConstants#CODE_ZNACHENIE_LICE_STATUS_PRAVO}
	 *
	 * @param lice
	 * @return
	 */
	private String checkPravo(Lice lice) {
		String err;
		if (lice.getDbStatus().equals(CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN)) {
			if (lice.getUdostId() != null) {
				err = null; // текущо е лишен, но има уд и явно се поправя грешка
			} else {
				err = "Този статус не може да бъде избран. За лицето няма издадено УП.";
			}
		} else {
			err = "Този статус не може да бъде избран. Определя се автоматично от системата.";
		}
		return err;
	}

	/**
	 * Проверява дали лицето може да премине в статус {@link com.ib.urireg.system.UriregConstants#CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN}
	 *
	 * @param lice
	 * @return
	 */
	private String checkPravoLishen(Lice lice) {
		String err;
		if (lice.getDbStatus().equals(CODE_ZNACHENIE_LICE_STATUS_PRAVO)) {
			err = null;
		} else {
			err = "Този статус не може да бъде избран. Лицето все още не е правосподобно.";
		}
		return err;
	}

	/**
	 * Проверява дали лицето може да премине в статус {@link com.ib.urireg.system.UriregConstants#CODE_ZNACHENIE_LICE_STATUS_TEST_APPROVED}
	 *
	 * @param lice
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String checkTestApproved(Lice lice) {
		List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery( //
						"select lice.lice_id, lice.status, lice.broi_izpit, lice.do_2019, ir.result_id" + //
								" from lice" + //
								" inner join staj on staj.lice_id = lice.lice_id and staj.zaiav_staj_id = lice.last_zaiav_staj_id" + //
								" left outer join izpit_result ir on ir.lice_id = lice.lice_id and ir.zaiav_izp_id = staj.zaiav_izp_id" + //
								" where lice.lice_id = :liceId" +  //
								" and staj.zaiav_izp_id is not null") // има заявление за изпит
				.setParameter("liceId", lice.getId()) //
				.getResultList();

		String err;

		if (rows.isEmpty()) {
			err = "Този статус не може да бъде избран. За лицето не е регистрирано заявление за изпит.";
		} else {
			Integer resultId = SearchUtils.asInteger(rows.get(0)[4]);
			if (resultId != null) {
				err = "Този статус не може да бъде избран. Лицето е включено в заповед за изпит.";
			} else {
				err = null;
			}
		}
		return err;
	}

	/**
	 * Проверява дали лицето може да премине в статус {@link com.ib.urireg.system.UriregConstants#CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED}
	 *
	 * @param lice
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	private String checkStajApproved(Lice lice, BaseSystemData systemData) throws DbErrorException {
		List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery( //
						"select lice.lice_id, lice.status, lice.broi_izpit, lice.do_2019, ir.result_id" + //
								" from lice" + //
								" inner join staj on staj.lice_id = lice.lice_id and staj.zaiav_staj_id = lice.last_zaiav_staj_id" + //
								" left outer join izpit_result ir on ir.lice_id = lice.lice_id and ir.zaiav_izp_id = staj.zaiav_izp_id " + //
								" where lice.lice_id = :liceId") //
				.setParameter("liceId", lice.getId()) //
				.getResultList();

		String err;

		if (rows.isEmpty()) {
			err = "Този статус не може да бъде избран. За лицето не е регистрирано заявление за стаж.";
		} else {
			String setting = systemData.getSettingsValue("urireg.maxIzpitCount");
			int max = SearchUtils.isEmpty(setting) ? 3 : Integer.parseInt(setting);

			Object[] row = rows.get(0);

			Integer broiIzpit = SearchUtils.asInteger(row[2]);
			Integer do2019 = SearchUtils.asInteger(row[3]);
			Integer resultId = SearchUtils.asInteger(row[4]);

			if (resultId != null) {
				err = "Този статус не може да бъде избран. Лицето е включено в изпит. Моля, регистрирайте ново заявление за допълнителен стаж.";
			} else {
				if (Objects.equals(do2019, SysConstants.CODE_ZNACHENIE_DA)) {
					err = null; // за тези няма ограничение

				} else if (broiIzpit != null && broiIzpit >= max) {
					err = "Този статус не може да бъде избран." + //
							" Лицето вече се е явявало " + broiIzpit + " пъти на изпит.";
				} else {
					err = null; // явно още може да се явява
				}
			}
		}
		return err;
	}

	/**
	 * Запис на лице с пълната логика
	 *
	 * @param entity
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public Lice save(Lice entity, BaseSystemData systemData) throws DbErrorException {
		if (entity.getAddrEkatte() != null) { // определям област и община по кода на екатте
			entity.setAddrObstina((String) systemData.getItemSpecific(CODE_CLASSIF_EKATTE, entity.getAddrEkatte(), getUserLang(), null, UriregClassifAdapter.EKATTE_INDEX_OBSTINA));
			entity.setAddrOblast((String) systemData.getItemSpecific(CODE_CLASSIF_EKATTE, entity.getAddrEkatte(), getUserLang(), null, UriregClassifAdapter.EKATTE_INDEX_OBLAST));
		}
		return save(entity);
	}

	/**
	 * Създаване на досие за лице спрямо завлението за стаж.
	 *
	 * @param zaiavStaj
	 * @param lice
	 * @param staj
	 * @param systemData
	 * @throws DbErrorException
	 */
	public void createNewDossier(Doc zaiavStaj, Lice lice, Staj staj, BaseSystemData systemData) throws DbErrorException {
		DocDAO docDao = new DocDAO(getUser());
		StajDAO stajDao = new StajDAO(getUser());

		if (zaiavStaj.getId() == null) { // за да се вземе ИД-то и да е ок в журнала после
			zaiavStaj.setUserReg(getUserId());
			zaiavStaj.setDateReg(new Date());
			JPA.getUtil().getEntityManager().persist(zaiavStaj);
		}

		lice.setZaiavStajId(zaiavStaj.getId());
		lice.setLastZaiavStajId(zaiavStaj.getId());
		if (lice.getStatus() == null) {
			lice.setStatus(CODE_ZNACHENIE_LICE_STATUS_CANDIDATE);
		}
		lice.setStatusDate(new Date());
		save(lice, systemData);


		docDao.save(zaiavStaj, lice.getId());


		LiceDoc liceDoc = new LiceDoc(lice.getId(), zaiavStaj.getId());
		new LiceDocDAO(getUser()).save(liceDoc);


		staj.setLiceId(lice.getId());
		staj.setZaiavStajId(zaiavStaj.getId());
		stajDao.save(staj);
	}

	/**
	 * Премахва данните от БД, които не са мапнати с JPA
	 *
	 * @param entity
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@Override
	protected void remove(Lice entity) throws DbErrorException, ObjectInUseException {
		try {
			int deleted = createNativeQuery("delete from lice_status where lice_id = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} статуси на лице с lice_id={}", deleted, entity.getId());

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на свързани обекти на лице!", e);
		}

		super.remove(entity);
	}
}
