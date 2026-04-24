package com.ib.urireg.db.dao;

import com.ib.system.exceptions.InvalidParameterException;
import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.db.dto.Lice;
import com.ib.urireg.db.dto.LiceDoc;
import com.ib.urireg.db.dto.Staj;
import com.ib.urireg.system.UriregConstants;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.SearchUtils;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ib.urireg.system.UriregConstants.*;

public class StajDAO extends AbstractDAO<Staj> {

	public StajDAO(ActiveUser user) {
		super(Staj.class, user);
	}

	/**
	 * Запис на нова заповед за стаж, като маркира за избраните редове
	 *
	 * @param zapStaj
	 * @param selected
	 * @throws DbErrorException
	 * @see #findStajList(Integer, Integer)
	 */
	public Doc saveStajList(Doc zapStaj, List<Object[]> selected) throws DbErrorException {
		DocDAO docDao = new DocDAO(getUser());

		zapStaj = docDao.save(zapStaj);

		if (selected != null) {
			LiceDocDAO liceDocDao = new LiceDocDAO(getUser());

			for (Object[] obj : selected) {
				if (obj[10] != null) {
					continue; // няма причина но все пак
				}
				Staj staj = findById(SearchUtils.asInteger(obj[5]));
				staj.setZapStajId(zapStaj.getId());
				save(staj);

				LiceDoc liceDoc = new LiceDoc(SearchUtils.asInteger(obj[0]), zapStaj.getId());
				liceDocDao.save(liceDoc);
			}
		}
		return zapStaj;
	}

	/**
	 * Премахва избран ред от заповедта
	 *
	 * @param selected
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 * @see #findStajList(Integer, Integer)
	 */
	@SuppressWarnings("unchecked")
	public void removeStajList(Object[] selected) throws DbErrorException, ObjectInUseException {
		if (selected == null || selected[10] == null) {
			return;
		}

		if (selected[15] != null) { // staj.zaiav_izp_id
			throw new ObjectInUseException("Лицето вече е подало заявление за изпит и не може да се премахне от стажа.");
		}

		LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
		Query relQuery = JPA.getUtil().getEntityManager().createQuery( //
				"select r from LiceDoc r where r.liceId = ?1 and r.docId = ?2");

		Staj staj = findById(SearchUtils.asInteger(selected[5]));
		staj.setZapStajId(null);
		save(staj);

		List<LiceDoc> liceDocs = relQuery.setParameter(1, selected[0]).setParameter(2, selected[10]).getResultList();
		if (!liceDocs.isEmpty()) {
			for (LiceDoc liceDoc : liceDocs) {
				liceDocDao.delete(liceDoc);
			}
		}
	}

	/**
	 * Връща лицата, които са включени в подадената заповед за стаж или всички лица,
	 * които могат да бъдат включени в заповед за стаж, ако не е подадена такава.</br>
	 * [0]-lice_id</br>
	 * [1]-egn</br>
	 * [2]-lnc</br>
	 * [3]-names</br>
	 * [4]-zaiav_staj_id</br>
	 * [5]-staj_id</br>
	 * [6]-staj_vid</br>
	 * [7]-osn_institution</br>
	 * [8]-mentor</br>
	 * [9]-pro_location</br>
	 * [10]-zap_staj_id</br>
	 * [11]-lice.firstname<br>
	 * [12]-lice.surname<br>
	 * [13]-lice.lastname<br>
	 * [14]-lice.do_2019<br>
	 * [15]-staj.zaiav_izp_id<br>
	 * [16]-file_id<br>
	 * [17]-filename<br>
	 *
	 * @param zapStajId
	 * @param zapStajVid
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> findStajList(Integer zapStajId, Integer zapStajVid) throws DbErrorException {
		if (zapStajId == null && zapStajVid == null) {
			return new ArrayList<>();
		}

		List<Object[]> list;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select lice.lice_id a0, lice.egn a1, lice.lnc a2, lice.names a3 ");
			sql.append(" , staj.zaiav_staj_id a4 ");
			sql.append(" , staj.staj_id a5, staj.staj_vid a6, staj.osn_institution a7, staj.mentor a8, staj.pro_location a9 ");
			sql.append(" , staj.zap_staj_id a10, lice.firstname a11, lice.surname a12, lice.lastname a13 ");
			sql.append(" , lice.do_2019 a14, staj.zaiav_izp_id a15 ");
			sql.append(" , f.file_id a16, f.filename a17 ");
			sql.append(" from lice ");

			Integer stajVid = null;

			if (zapStajId != null) { // точно стажа за конкретна заповед за включените лица
				sql.append(" inner join staj on staj.lice_id = lice.lice_id and staj.zap_staj_id = :zapStajId ");

			} else { // търсим тези със статус Одобрен за стаж и да е за последното заявление за стаж и вече да не е включено в заповед
				sql.append(" inner join staj on staj.zaiav_staj_id = lice.last_zaiav_staj_id and lice.status = :stajApproved and staj.zap_staj_id is null ");
				sql.append(" and staj.staj_vid = :stajVid "); // и стажа да е спрямо вида на заповедта

				if (zapStajVid.equals(CODE_ZNACHENIE_DOC_VID_ZAP_STAJ)) {
					stajVid = CODE_ZNACHENIE_STAJ_VID_INITIAL;
				} else {
					stajVid = CODE_ZNACHENIE_STAJ_VID_ADDITIONAL;
				}
			}
			sql.append(" left outer join file_objects fo on fo.object_id = staj.staj_id and fo.object_code = :objectCode ");
			sql.append(" left outer join files f on f.file_id = fo.file_id ");

			sql.append(" order by lice.names ");


			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString());


			if (zapStajId != null) {
				query.setParameter("zapStajId", zapStajId);
			} else {
				query.setParameter("stajApproved", UriregConstants.CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED);
				query.setParameter("stajVid", stajVid);
			}
			query.setParameter("objectCode", CODE_ZNACHENIE_JOURNAL_STAJ);

			list = (List<Object[]>) query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на лица за заповед за провеждане на стаж", e);
		}
		return list;
	}


	/**
	 * Връща стажовете за дадено лице
	 *
	 * @param liceId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> findStajByLiceList(Integer liceId) throws DbErrorException {

		List<Object[]> result;
		try {

			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT staj_id ,staj_vid ,doc_zayav.rn_doc rn_doc_zayav, doc_zayav.doc_date doc_date_zayav, ");
			sql.append(" doc_zapoved.rn_doc rn_doc_zap, doc_zapoved.doc_date doc_date_zap, ");
			sql.append(" osn_institution ,osn_start_date ,osn_end_date, ");
			sql.append(" mentor ,mentor_email ,pro_location, pro_start_date ,pro_end_date, staj_info ,zaiav_staj_id ,zap_staj_id ,pro_start_date ,pro_end_date ,staj.zaiav_izp_id");
			sql.append(" FROM staj JOIN doc doc_zayav ON doc_zayav.DOC_ID = staj.zaiav_staj_id ");
			sql.append(" LEFT JOIN doc doc_zapoved ON doc_zapoved.DOC_ID = staj.zap_staj_id ");
			sql.append(" WHERE staj.lice_id= :liceId ORDER BY staj.date_reg desc ");

			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()).setParameter("liceId", liceId);


			result = query.getResultList();

			if (result.isEmpty()) {
				return result;
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане на стажовете за дадено лице", e);
		}

		return result;
	}

	/**
	 * Връща приложените видове документи към заявление за стаж
	 *
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> selectDocPrilByDocId(Integer docId) throws DbErrorException {

		List<Integer> result;
		try {

			Query query = JPA.getUtil().getEntityManager().createNativeQuery("SELECT  pril_vid FROM  doc_pril where  doc_id =:docId").setParameter("docId", docId);


			result = query.getResultList();

			if (result.isEmpty()) {
				return result;
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане на приложени видове документи! ", e);
		}

		return result;
	}


	/**
	 * Създаване на ново заявление за допълнител стаж
	 *
	 * @param zaiavStaj
	 * @param lice
	 * @param staj
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	public void createDopZayavStaj(Doc zaiavStaj, Lice lice, Staj staj) throws DbErrorException {
		DocDAO docDao = new DocDAO(getUser());
		LiceDAO liceDao = new LiceDAO(getUser());

		docDao.save(zaiavStaj, lice.getId());

		lice.setLastZaiavStajId(zaiavStaj.getId());

		//ако не са посочили статус на лицето от екрана слагам по подразбиране "кандидат"
		if(lice.getStatus()!=null){
			if(lice.getStatus().intValue() !=CODE_ZNACHENIE_LICE_STATUS_CANDIDATE && lice.getStatus().intValue() !=CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED ){
				lice.setStatus(CODE_ZNACHENIE_LICE_STATUS_CANDIDATE);
			}
		}


		lice.setStatusInfo("");
		lice.setStatusPri(null);
		lice.setStatusDate(new Date());
		liceDao.save(lice);

		LiceDoc liceDoc = new LiceDoc(lice.getId(), zaiavStaj.getId());
		new LiceDocDAO(getUser()).save(liceDoc);


		staj.setLiceId(lice.getId());
		staj.setZaiavStajId(zaiavStaj.getId());

		save(staj);
	}

	/**
	 * Премахва избран ред от заповедта
	 *
	 * @param idZayavIzpit
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public Staj findStajIzpitId(Integer idZayavIzpit) throws DbErrorException {
		if (idZayavIzpit == null) {
			return null;
		}

		Query relQuery = JPA.getUtil().getEntityManager().createQuery( "select s from Staj s where s.zaiavIzpId =?1 ");
		List<Staj> rez = relQuery.setParameter(1, idZayavIzpit).getResultList();

		if (!rez.isEmpty()) {
			return rez.get(0);
		}

		return null;
	}

}
