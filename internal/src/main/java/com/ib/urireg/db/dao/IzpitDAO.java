package com.ib.urireg.db.dao;

import com.ib.system.ActiveUser;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;
import com.ib.system.utils.ValidationUtils;
import com.ib.urireg.db.dto.*;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.utils.MigrUtils;
import jakarta.persistence.Query;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.urireg.system.UriregConstants.*;

public class IzpitDAO extends AbstractDAO<Izpit> {


	private static final Logger LOGGER = LoggerFactory.getLogger(IzpitDAO.class);

	static class IzpitResultDAO extends AbstractDAO<IzpitResult> {

		public IzpitResultDAO(ActiveUser user) {
			super(IzpitResult.class, user);
		}

		/** за да го има за журнала след мерге */
		@Override
		protected IzpitResult merge(IzpitResult entity) throws DbErrorException {
			IzpitResult merged = super.merge(entity);
			merged.setZapIzpId(entity.getZapIzpId());
			return merged;
		}
	}

	public IzpitDAO(ActiveUser user) {
		super(Izpit.class, user);
	}

	/**
	 * Изтрива протокол казус, който е присъщ само за лица след 2019г
	 *
	 * @param izpit
	 * @param zapIzpit
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 * @throws InvalidParameterException
	 */
	@SuppressWarnings("unchecked")
	public Izpit deleteCaseProtocolSled2019(Izpit izpit, Doc zapIzpit) throws DbErrorException, ObjectInUseException, InvalidParameterException {
		boolean sled2019 = isSled2019(zapIzpit);
		if (!sled2019) {
			throw new InvalidParameterException("Изтриването не е позволено.");
		}
		if (izpit.getCaseProtId() == null) {
			return izpit;
		}

		List<Object[]> list;
		try {
			Query query = JPA.getUtil().getEntityManager().createQuery( //
					"select ld, ir" + //
							" from LiceDoc ld" + //
							" left outer join IzpitResult ir on ir.liceId = ld.liceId and ir.izpitId = ?1" + //
							" where ld.docId = ?2");
			list = query.setParameter(1, izpit.getId()).setParameter(2, izpit.getCaseProtId()).getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани данни за протокола!", e);
		}

		if (!list.isEmpty()) {
			LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
			IzpitResultDAO resultDao = new IzpitResultDAO(getUser());

			for (Object[] obj : list) {
				liceDocDao.delete((LiceDoc) obj[0]); // връзката с лицата е изтривам

				IzpitResult result = (IzpitResult) obj[1]; // резултатите от протокола нулирам
				result.setCaseResult(null);
				result.setCaseRemark(null);
				result.setCasePismenaNom(null);

				result.setZapIzpId(izpit.getZapIzpId());
				resultDao.save(result);
			}
		}

		Integer caseProtId = izpit.getCaseProtId();

		izpit.setCaseProtId(null); // махам протокола от изпита
		Izpit saved = save(izpit);

		new DocDAO(getUser()).deleteById(caseProtId); // и после го изтривам

		return saved;
	}

	/**
	 * Изтрива заповедта за изпит и изпита и всичко останало.
	 *
	 * @param izpit
	 * @param zapIzpit
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 * @throws InvalidParameterException
	 */
	public void deleteZapovedIzpit(Izpit izpit, Doc zapIzpit) throws DbErrorException, ObjectInUseException, InvalidParameterException {
		boolean sled2019 = isSled2019(zapIzpit);

		if (sled2019 && izpit.getCaseProtId() != null) {
			throw new InvalidParameterException("Изтриването не е позволено.");
		}
		Integer protId = sled2019 ? izpit.getTestProtId() : izpit.getCaseProtId();

		List<Object[]> lica = selectLicaByProtId(izpit.getId(), protId);

		removeLicaFromIzpit(izpit, protId, lica); // махам всички лица от изпита

		delete(izpit);

		DocDAO docDao = new DocDAO(getUser());
		docDao.deleteById(protId);
		docDao.delete(zapIzpit);
	}

	/**
	 * Намира данни за лице, за включване в заповед за изпит.</br>
	 * [0]-lice_id</br>
	 * [1]-staj_id</br>
	 * [2]-lice.names</br>
	 * [3]-zaiavizp.doc_id</br>
	 * [4]-zaiavizp.rn_doc</br>
	 * [5]-zaiavizp.doc_date</br>
	 *
	 * @param egnlnc
	 * @param zapIzpit
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	@SuppressWarnings("unchecked")
	public Object[] findLiceForIzpit(String egnlnc, Doc zapIzpit) throws DbErrorException, InvalidParameterException {
		egnlnc = SearchUtils.trimToNULL(egnlnc);
		if (egnlnc == null) {
			return null;
		}
		boolean sled2019 = isSled2019(zapIzpit);

		Object[] result = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select lice.lice_id a0, staj.staj_id a1, lice.names a2 ");
			sql.append(" , zaiavizp.doc_id a3, zaiavizp.rn_doc a4, zaiavizp.doc_date a5 ");
			sql.append(" from lice ");
			sql.append(" inner join staj on staj.zaiav_staj_id = lice.last_zaiav_staj_id ");
			sql.append(" inner join doc zaiavizp on zaiavizp.doc_id = staj.zaiav_izp_id ");
			sql.append(" left outer join izpit_result ir on ir.zaiav_izp_id = staj.zaiav_izp_id "); // това заявление за изпит
			sql.append(" where (lice.egn = :egnlnc or lice.lnc = :egnlnc) ");
			sql.append(" and lice.status = :testApproved ");
			sql.append(" and ir.result_id is null "); // да не е вкарано вече в заповед за изпит

			if (sled2019) {
				sql.append(" and (lice.do_2019 is null or lice.do_2019 = 2) ");
			} else {
				sql.append(" and lice.do_2019 = 1 "); // По законодателство до 2019г.
			}

			List<Object[]> list = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
					.setParameter("egnlnc", egnlnc) //
					.setParameter("testApproved", CODE_ZNACHENIE_LICE_STATUS_TEST_APPROVED) //
					.getResultList();
			if (!list.isEmpty()) {
				result = list.get(0);
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на лице за включване в изпит", e);
		}
		return result;
	}

	/**
	 * Добавя лицето в заповедта за изпит
	 *
	 * @param izpit
	 * @param protId
	 * @param liceData
	 * @throws DbErrorException
	 * @see #findLiceForIzpit(String, Doc)
	 */
	public void addLiceToIzpit(Izpit izpit, Integer protId, Object[] liceData) throws DbErrorException {
		if (izpit.getTestProtId() != null && izpit.getCaseProtId() != null) {
			return; // няма как да се добавя ако са вече двата протокола
		}

		LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
		IzpitResultDAO resultDao = new IzpitResultDAO(getUser());

		IzpitResult result = new IzpitResult();

		result.setIzpitId(izpit.getId());
		result.setStajId(asInteger(liceData[1]));
		result.setLiceId(asInteger(liceData[0]));
		result.setZaiavIzpId(asInteger(liceData[3]));

		result.setZapIzpId(izpit.getZapIzpId());
		resultDao.save(result);

		LiceDoc liceDocZapIzp = new LiceDoc(result.getLiceId(), izpit.getZapIzpId());
		liceDocDao.save(liceDocZapIzp);


		LiceDoc liceDocProt = new LiceDoc(result.getLiceId(), protId);
		liceDocDao.save(liceDocProt);
	}

	/**
	 * Премахва избраните лица от изпита
	 *
	 * @param izpit
	 * @param protId
	 * @param selected
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 * @see #selectLicaByProtId(Integer, Integer)
	 */
	@SuppressWarnings("unchecked")
	public void removeLicaFromIzpit(Izpit izpit, Integer protId, List<Object[]> selected) throws DbErrorException, ObjectInUseException {
		if (izpit.getTestProtId() != null && izpit.getCaseProtId() != null) {
			return; // няма как да се маха ако са вече двата протокола
		}

		IzpitResultDAO resultDao = new IzpitResultDAO(getUser());
		LiceDocDAO liceDocDao = new LiceDocDAO(getUser());

		List<Integer> docIds = Arrays.asList(izpit.getZapIzpId(), protId);

		Query query = JPA.getUtil().getEntityManager().createQuery( //
				"select ld from LiceDoc ld where ld.liceId = :liceId and ld.docId in (:docIdList)");

		for (Object[] row : selected) {
			IzpitResult result = resultDao.findById(asInteger(row[11]));

			List<LiceDoc> liceDocs = query.setParameter("liceId", row[0]) //
					.setParameter("docIdList", docIds).getResultList();

			result.setZapIzpId(izpit.getZapIzpId());
			resultDao.delete(result);

			for (LiceDoc liceDoc : liceDocs) {
				liceDocDao.delete(liceDoc);
			}
		}
	}

	/**
	 * Запис на изпит, като записва заповедта, прокола за тест и добавя лицата в него.
	 *
	 * @param izpit
	 * @param zapIzpit
	 * @param protTest
	 * @param protCase
	 * @param newIzpitLica
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 * @see #selectNewIzpitLica(Doc)
	 */
	@SuppressWarnings("unchecked")
	public void saveIzpitData(Izpit izpit, Doc zapIzpit, Doc protTest, Doc protCase, List<Object[]> newIzpitLica) throws DbErrorException, InvalidParameterException {
		if (zapIzpit.getZaiavDate() == null) {
			throw new InvalidParameterException("Моля въведете дата, до която са подадени заявленията");
		}
		boolean sled2019 = isSled2019(zapIzpit);

		if ((sled2019 && protTest != null && protTest.getId() == null)  // за сега не правим корекция на лицата след първи запис
				|| (!sled2019 && protCase != null && protCase.getId() == null)) {
//			newIzpitLica = selectNewIzpitLica(zapIzpit); // вече идват от екрана и няма нужда да се селектват

		} else if (newIzpitLica != null && !newIzpitLica.isEmpty()) {
			throw new InvalidParameterException("Не е позволено добавянето на нови лица при корекция на заповедта");
		}

		List<Object[]> protCaseLica = null;
		if (sled2019 && izpit.getId() != null && protCase != null && protCase.getId() == null) { // за сега не правим корекция на лицата след първи запис
			try {
				StringBuilder sql = new StringBuilder();
				sql.append(" select lice.lice_id, ir.result_id ");
				sql.append(" from lice ");
				sql.append(" inner join izpit_result ir on ir.lice_id = lice.lice_id ");
				sql.append(" where lice.status = :caseApproved ");
				sql.append(" and ir.izpit_id = :izpitId ");

				protCaseLica = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
						.setParameter("caseApproved", CODE_ZNACHENIE_LICE_STATUS_CASE_APPROVED) //
						.setParameter("izpitId", izpit.getId()) //
						.getResultList();
			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на лица за включване в протокол за казус", e);
			}
		}

		DocDAO docDao = new DocDAO(getUser());

		docDao.save(zapIzpit);
		izpit.setZapIzpId(zapIzpit.getId());

		if (protTest != null) {
			docDao.save(protTest);
			izpit.setTestProtId(protTest.getId());
		}
		if (protCase != null) {
			docDao.save(protCase);
			izpit.setCaseProtId(protCase.getId());
		}

		save(izpit);

		if (newIzpitLica != null) {
			LiceDocDAO liceDocDao = new LiceDocDAO(getUser());

			IzpitResultDAO resultDao = new IzpitResultDAO(getUser());
			for (Object[] row : newIzpitLica) {
				IzpitResult result = new IzpitResult();

				// lice.lice_id - 0
				// staj.staj_id - 20
				// staj.zaiav_izp_id - 7

				result.setIzpitId(izpit.getId());
				result.setStajId(asInteger(row[20]));
				result.setLiceId(asInteger(row[0]));
				result.setZaiavIzpId(asInteger(row[7]));

				result.setZapIzpId(izpit.getZapIzpId());
				resultDao.save(result);

				LiceDoc liceDocZapIzp = new LiceDoc(result.getLiceId(), zapIzpit.getId());
				liceDocDao.save(liceDocZapIzp);

				Integer protId = sled2019 ? protTest.getId() : protCase.getId();

				LiceDoc liceDocProt = new LiceDoc(result.getLiceId(), protId);
				liceDocDao.save(liceDocProt);
			}
		}

		if (protCaseLica != null) {
			LiceDocDAO liceDocDao = new LiceDocDAO(getUser());

			for (Object[] row : protCaseLica) {

				LiceDoc liceDocProtCase = new LiceDoc(asInteger(row[0]), protCase.getId());
				liceDocDao.save(liceDocProtCase);
			}
		}
	}

	/**
	 * Намира списък от лица, които отговарят на условията да се включат в нова заповед за изпит
	 *
	 * @param zapIzpit
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> selectNewIzpitLica(Doc zapIzpit) throws DbErrorException, InvalidParameterException {
		if (zapIzpit.getZaiavDate() == null) {
			throw new InvalidParameterException("Моля въведете дата, до която са подадени заявленията");
		}
		boolean sled2019 = isSled2019(zapIzpit);

		List<Object[]> newIzpitLica;
		try {
			StringBuilder sql = new StringBuilder();
			// lice.lice_id - 0
			// staj.staj_id - 20
			// staj.zaiav_izp_id - 7

			sql.append(" select lice.lice_id a0, lice.egn a1, lice.lnc a2, lice.firstname a3, lice.surname a4, lice.lastname a5, lice.names a6 ");
			sql.append(" , zaiavizp.doc_id a7, zaiavizp.rn_doc a8, zaiavizp.doc_date a9, zaiavizp.zaiav_date a10 ");
			sql.append(" , ir.result_id a11 ");
			sql.append(" , ir.test_pismena_nom a12, test_correct_answers a13, test_result a14, test_remark a15 ");
			sql.append(" , case_pismena_nom a16, case_result a17, case_remark a18 ");
			sql.append(" , result_info a19 ");
			sql.append(" , staj.staj_id a20 ");

			sql.append(" from lice ");
			sql.append(" inner join staj on staj.zaiav_staj_id = lice.last_zaiav_staj_id ");
			sql.append(" inner join doc zaiavizp on zaiavizp.doc_id = staj.zaiav_izp_id ");
			sql.append(" left outer join izpit_result ir on ir.zaiav_izp_id = staj.zaiav_izp_id "); // тоза заявление за изпит
			sql.append(" where lice.status = :testApproved ");
			sql.append(" and zaiavizp.zaiav_date <= :zaiavDate ");
			sql.append(" and ir.result_id is null "); // да не е вкарано вече в заповед за изпит

			if (sled2019) {
				sql.append(" and (lice.do_2019 is null or lice.do_2019 = 2) ");
			} else {
				sql.append(" and lice.do_2019 = 1 "); // По законодателство до 2019г.
			}

			newIzpitLica = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
					.setParameter("testApproved", CODE_ZNACHENIE_LICE_STATUS_TEST_APPROVED) //
					.setParameter("zaiavDate", DateUtils.endDate(zapIzpit.getZaiavDate())) //
					.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на лица за включване в изпит", e);
		}
		return newIzpitLica;
	}

	/**
	 * Проверява дали заповедта е за лица преди 2019г.
	 *
	 * @param zapIzpit
	 * @return
	 * @throws InvalidParameterException
	 */
	private boolean isSled2019(Doc zapIzpit) throws InvalidParameterException {
		boolean sled2019;
		if (Objects.equals(zapIzpit.getDocVid(), UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT)) {
			sled2019 = true;
		} else if (Objects.equals(zapIzpit.getDocVid(), CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019)) {
			sled2019 = false;
		} else {
			throw new InvalidParameterException("Непознат вид на заповед за изпит.");
		}
		return sled2019;
	}

	/**
	 * Намира изпита по заповедта за изпит
	 *
	 * @param zapIzpId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public Izpit findByZapIzpId(int zapIzpId) throws DbErrorException {
		try {
			List<Izpit> list = JPA.getUtil().getEntityManager().createQuery( //
							"select i from Izpit i where i.zapIzpId = ?1").setParameter(1, zapIzpId) //
					.getResultList();
			if (list != null && !list.isEmpty()) {
				return list.get(0);
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на изпит за заповед с ИД=" + zapIzpId, e);
		}
		return null;
	}

	/**
	 * Предоставя списък на лицата в протокола</br>
	 * [0]-lice_id</br>
	 * [1]-egn</br>
	 * [2]-lnc</br>
	 * [3]-firstname</br>
	 * [4]-surname</br>
	 * [5]-lastname</br>
	 * [6]-names</br>
	 * [7]-zaiavizp.doc_id</br>
	 * [8]-zaiavizp.rn_doc</br>
	 * [9]-zaiavizp.doc_date</br>
	 * [10]-zaiavizp.zaiav_date</br>
	 * [11]-result_id</br>
	 * [12]-test_pismena_nom</br>
	 * [13]-test_correct_answers</br>
	 * [14]-test_result</br>
	 * [15]-test_remark</br>
	 * [16]-case_pismena_nom</br>
	 * [17]-case_result</br>
	 * [18]-case_remark</br>
	 * [19]-result_info</br>
	 *
	 * @param izpitId
	 * @param protId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> selectLicaByProtId(Integer izpitId, Integer protId) throws DbErrorException {
		List<Object[]> list;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select lice.lice_id a0, lice.egn a1, lice.lnc a2, lice.firstname a3, lice.surname a4, lice.lastname a5, lice.names a6 ");
			sql.append(" , zaiavizp.doc_id a7, zaiavizp.rn_doc a8, zaiavizp.doc_date a9, zaiavizp.zaiav_date a10 ");
			sql.append(" , ir.result_id a11 ");
			sql.append(" , ir.test_pismena_nom a12, test_correct_answers a13, test_result a14, test_remark a15 ");
			sql.append(" , case_pismena_nom a16, case_result a17, case_remark a18 ");
			sql.append(" , result_info a19 ");
			sql.append(" from lice ");
			sql.append(" inner join izpit_result ir on ir.lice_id = lice.lice_id ");
			sql.append(" inner join staj on staj.staj_id = ir.staj_id ");
			sql.append(" left outer join doc zaiavizp on zaiavizp.doc_id = ir.zaiav_izp_id ");
			sql.append(" inner join lice_doc rel on rel.lice_id = lice.lice_id ");
			sql.append(" where ir.izpit_id = :izpitId and rel.doc_id = :protId ");
			sql.append(" order by lice.names ");

			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
					.setParameter("izpitId", izpitId).setParameter("protId", protId);

			list = (List<Object[]>) query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на лица от проткол за изпит", e);
		}
		return list;
	}

	/**
	 * Връща броя на въведените резултати от тест
	 *
	 * @param izpitId
	 * @return
	 * @throws DbErrorException
	 */
	public int selectCountTestResultEntered(Integer izpitId) throws DbErrorException {
		if (izpitId == null) {
			return 0;
		}
		try {
			Integer cnt = asInteger(JPA.getUtil().getEntityManager().createNativeQuery( //
							"select count (*) cnt from izpit_result ir where ir.izpit_id = ?1 and ir.test_result is not null") //
					.setParameter(1, izpitId).getResultList().get(0));

			return cnt.intValue();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на резултати от тест за изпит с ИД=" + izpitId, e);
		}
	}

	/**
	 * Връща броя на въведените резултати от казус
	 *
	 * @param izpitId
	 * @return
	 * @throws DbErrorException
	 */
	public int selectCountCaseResultEntered(Integer izpitId) throws DbErrorException {
		if (izpitId == null) {
			return 0;
		}
		try {
			Integer cnt = asInteger(JPA.getUtil().getEntityManager().createNativeQuery( //
							"select count (*) cnt from izpit_result ir where ir.izpit_id = ?1 and ir.case_result is not null") //
					.setParameter(1, izpitId).getResultList().get(0));

			return cnt.intValue();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на резултати от казус за изпит с ИД=" + izpitId, e);
		}
	}

	/**
	 * Проверява дали са въведени всички резултатаи от тест за изпита
	 *
	 * @param izpitId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public boolean isTestResultEntered(Integer izpitId) throws DbErrorException {
		if (izpitId == null) {
			return false;
		}
		try {
			List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery( //
							"select ir.test_result, count (*) cnt from izpit_result ir where ir.izpit_id = ?1 group by ir.test_result") //
					.setParameter(1, izpitId).getResultList();
			if (rows.isEmpty()) {
				return false; // няма никакви редове и не може да се смята, че е въведено
			}

			for (Object[] row : rows) {
				if (row[0] == null) {
					return false; // щом има поне един с празно значи не е въведенона всички
				}
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на резултата от тест за изпит с ИД=" + izpitId, e);
		}
		return true;
	}

	/**
	 * Намира протокол по дата. По принцип трябва да е един, но все пак може да има повече от един на тази дата.
	 *
	 * @param protDate
	 * @param docVid
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Doc> findProtocolList(Date protDate, int docVid) throws DbErrorException {
		try {
			Query query = JPA.getUtil().getEntityManager().createQuery( //
					"select p from Doc p where p.docVid = :vid" + //
							" and p.docDate >= :dateStart and p.docDate <= :dateEnd");

			query.setParameter("vid", docVid);
			query.setParameter("dateStart", DateUtils.startDate(protDate));
			query.setParameter("dateEnd", DateUtils.endDate(protDate));

			return query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на протокол от изпит", e);
		}
	}

	/**
	 * Намира резултатите за този протокол</br>
	 * [0]-liceId</br>
	 * [1]-firstname</br>
	 * [2]-surname</br>
	 * [3]-lastname</br>
	 * [4]-names</br>
	 * [5]-birthDate</br>
	 * [6]-zaiavIzpId</br>
	 * [7]-zaiavIzpRn</br>
	 * [8]-zaiavIzpDate</br>
	 * [9]-zaiavDate</br>
	 * [10]-IzpitResult</br>
	 * [11]-egn</br>
	 * [12]-lnc</br>
	 * [13]-udost.id</br>
	 * [14]-udost.rnDoc</br>
	 * [15]-udost.docDate</br>
	 * [16]-universitet</br>
	 *
	 * @param izpitId
	 * @param protId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> findIzpitResults(Integer izpitId, Integer protId) throws DbErrorException {
		try {
			Query query = JPA.getUtil().getEntityManager().createQuery( //
					"select lice.id liceId, lice.firstname, lice.surname, lice.lastname, lice.names, lice.birthDate" + //
							" , zaiavizp.id zaiavIzpId, zaiavizp.rnDoc zaiavIzpRn, zaiavizp.docDate zaiavIzpDate, zaiavizp.zaiavDate" + //
							" , ir, lice.egn, lice.lnc" + //
							" , udost.id udostId, udost.rnDoc udostRnDoc, udost.docDate udostDocDate, lice.universitet " + //
							" from Lice lice" + //
							" inner join IzpitResult ir on ir.liceId = lice.id" + //
							" inner join Staj staj on staj.id = ir.stajId" + //
							" left outer join Doc zaiavizp on zaiavizp.id = ir.zaiavIzpId" + //
							" inner join LiceDoc rel on rel.liceId = lice.id" + //
							" left outer join Doc udost on udost.id = lice.udostId and ir.caseResult = :resultPassed" + //
							" where ir.izpitId = :izpitId and rel.docId = :protId");
			query.setParameter("izpitId", izpitId);
			query.setParameter("protId", protId);
			query.setParameter("resultPassed", CODE_ZNACHENIE_IZPIT_RESULT_PASSED);

			return query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на резултати за протокол от изпит", e);
		}
	}

	/**
	 * Над таблицата с протоколи, до полето за общ брой, да се изведе и брой за издържли, неиздържали, неявили се.
	 * Тази инфромация да се показва на екрана след като са въведени резултатите за всички лица.</br>
	 * [0]-брой невъведени - трябва да е 0, ако се показват след като всичко е въведено</br>
	 * [1]-брой издържали</br>
	 * [2]-брой неиздържали</br>
	 * [3]-брой неявили се</br>
	 *
	 * @param resultList
	 * @param protVid спрямо вида на протокола се определя от къде се взима резултата
	 * @return
	 * @see #findIzpitResults(Integer, Integer)
	 */
	public int[] countIzpitResults(List<Object[]> resultList, Integer protVid) {
		int[] cnts = new int[4];
		if (resultList == null || resultList.isEmpty() || protVid == null) {
			return cnts; // няма какво да се смята
		}

		for (Object[] row : resultList) {
			IzpitResult ir = (IzpitResult) row[10];

			Integer result = null;
			if (ir != null) {
				result = protVid.equals(CODE_ZNACHENIE_DOC_VID_PROT_TEST) ? ir.getTestResult() : ir.getCaseResult();
			}
			if (result == null) {
				cnts[0]++;
				continue;
			}

			if (result.equals(CODE_ZNACHENIE_IZPIT_RESULT_PASSED)) {
				cnts[1]++;
			} else if (result.equals(CODE_ZNACHENIE_IZPIT_RESULT_FAILED)) {
				cnts[2]++;
			} else if (result.equals(CODE_ZNACHENIE_IZPIT_RESULT_MISSED)) {
				cnts[3]++;
			}
		}
		return cnts;
	}

	/**
	 * Генерира УД за издържалите.
	 *
	 * @param rows
	 * @param udostDate
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 * @throws ObjectInUseException
	 * @see #findIzpitResults(Integer, Integer)
	 */
	public void genLicaUdostDoc(List<Object[]> rows, Date udostDate) throws DbErrorException, InvalidParameterException, ObjectInUseException {
		LiceDAO liceDao = new LiceDAO(getUser());
		DocDAO docDao = new DocDAO(getUser());

		for (Object[] row : rows) {
			Integer liceId = SearchUtils.asInteger(row[0]);

			Lice lice = liceDao.findById(liceId);

			if (lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_EXAM_PASSED)) {
				Doc udost = new Doc();
				udost.setDocVid(CODE_ZNACHENIE_DOC_VID_UDOST_PRAVO);
				udost.setDocDate(udostDate);
				udost.setOriginal(SysConstants.CODE_ZNACHENIE_DA);

				docDao.saveUdost(lice, udost);
			}
		}
	}


	/**
	 * Записва данните за резултата
	 *
	 * @param result
	 * @param zapIzpit
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	public IzpitResult saveResult(IzpitResult result, Doc zapIzpit) throws DbErrorException, InvalidParameterException {
		LiceDAO liceDao = new LiceDAO(getUser());

		Lice lice = liceDao.findById(result.getLiceId());

		boolean zapDo2019 = Objects.equals(zapIzpit.getDocVid(), CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019);
		boolean liceDo2019 = Objects.equals(lice.getDo2019(), SysConstants.CODE_ZNACHENIE_DA);

		if (zapDo2019 && !liceDo2019) {
			throw new InvalidParameterException("Заповедта е за лица до 2019г., а лицето е по законодателство след 2019г.");
		} else if (liceDo2019 && !zapDo2019) {
			throw new InvalidParameterException("Лицето е по законодателство до 2019г., а заповедта е за лица след 2019г.");
		}
		Integer newLiceStatus = null;

		if (result.getTestResult() == null) { // нулираме резултата
			newLiceStatus = CODE_ZNACHENIE_LICE_STATUS_TEST_APPROVED;
		}

		if (result.getCaseResult() == null && result.getTestResult() != null) { // въвеждаме резултат от тест
			if (result.getTestResult().equals(CODE_ZNACHENIE_IZPIT_RESULT_PASSED)) {
				if (liceDo2019) {
					newLiceStatus = CODE_ZNACHENIE_LICE_STATUS_EXAM_PASSED;
				} else {
					newLiceStatus = CODE_ZNACHENIE_LICE_STATUS_CASE_APPROVED;
				}

			} else if (result.getTestResult().equals(CODE_ZNACHENIE_IZPIT_RESULT_FAILED)) {
				newLiceStatus = CODE_ZNACHENIE_LICE_STATUS_EXAM_FAILED;

			} else if (result.getTestResult().equals(CODE_ZNACHENIE_IZPIT_RESULT_MISSED)) {
				newLiceStatus = CODE_ZNACHENIE_LICE_STATUS_TEST_APPROVED;
			}

		} else if (result.getCaseResult() != null) {  // въвеждаме резултат от казус
			if (result.getCaseResult().equals(CODE_ZNACHENIE_IZPIT_RESULT_PASSED)) {
				newLiceStatus = CODE_ZNACHENIE_LICE_STATUS_EXAM_PASSED;

			} else if (result.getCaseResult().equals(CODE_ZNACHENIE_IZPIT_RESULT_FAILED)) {
				newLiceStatus = CODE_ZNACHENIE_LICE_STATUS_EXAM_FAILED;

			} else if (result.getCaseResult().equals(CODE_ZNACHENIE_IZPIT_RESULT_MISSED)) {
				newLiceStatus = CODE_ZNACHENIE_LICE_STATUS_TEST_APPROVED;
			}
		}

		if (newLiceStatus != null && !newLiceStatus.equals(lice.getStatus())) { // само ако има реална корекция на статуса
			lice.setStatus(newLiceStatus); // нов статус
			lice.setStatusDate(new Date()); // нова дата

			getEntityManager().merge(result); // за да мога после да направя селекта за брой явявания

			Integer newBroiIzpiti = selectBroiIzpiti(lice.getId());
			lice.setBroiIzpit(newBroiIzpiti);

			liceDao.save(lice);
		}

		result.setZapIzpId(zapIzpit.getId());
		return new IzpitResultDAO(getUser()).save(result);
	}

	/**
	 * Намира на колко изпита се е явило лицето
	 *
	 * @param liceId
	 * @return
	 * @throws DbErrorException
	 */
	private Integer selectBroiIzpiti(Integer liceId) throws DbErrorException {
		try {
			Query query = JPA.getUtil().getEntityManager().createNativeQuery( //
							"select count (*) cnt from izpit_result" + //
									" where lice_id = :liceId and (test_result = :resultFailed or case_result in (:results))") //
					.setParameter("liceId", liceId).setParameter("resultFailed", CODE_ZNACHENIE_IZPIT_RESULT_FAILED) //
					.setParameter("results", Arrays.asList(CODE_ZNACHENIE_IZPIT_RESULT_PASSED, CODE_ZNACHENIE_IZPIT_RESULT_FAILED));

			return ((Number) query.getSingleResult()).intValue();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при определяне на брой явявания на изпит!", e);
		}
	}

	/**
	 * Запис на заявление за изпит
	 *
	 * @param liceId
	 * @param zayavIzpit
	 * @param staj
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	public void saveZayavIzpit(Integer liceId, Staj staj, Doc zayavIzpit) throws DbErrorException, InvalidParameterException {

		if (liceId == null || staj == null || zayavIzpit == null) {
			throw new InvalidParameterException("Неподадени валидни параметри");
		}

		try {
			DocDAO docDao = new DocDAO(getUser());
			StajDAO stajDao = new StajDAO(getUser());

			boolean isNew = (zayavIzpit.getId() == null ? true : false);

			docDao.save(zayavIzpit, liceId);

			//когато е ново заявление ще добавяме връзките
			if (isNew) {

				staj.setZaiavIzpId(zayavIzpit.getId());

				LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
				LiceDoc liceDocZyav = new LiceDoc(liceId, zayavIzpit.getId());
				liceDocDao.save(liceDocZyav);
			}

			stajDao.save(staj);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при запис на заявление за изпит", e);
		}
	}


	@SuppressWarnings("unchecked")
	public List<Object[]> loadIzpitiDosie(Integer liceId) throws DbErrorException {
		List<Object[]> result;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select -1 staj_id ");
			sql.append(" , zaiav_izp.rn_doc, zaiav_izp.doc_date, zaiav_izp.zaiav_date, zaiav_izp.doc_info, zap_izp.rn_doc, zap_izp.doc_date ");
			sql.append(" , test_prot.doc_date, izpit_result.test_result, izpit_result.test_pismena_nom, izpit_result.test_correct_answers, izpit_result.test_remark ");
			sql.append(" , case_prot.doc_date, izpit_result.case_result, izpit_result.case_remark, izpit_result.zaiav_izp_id, izpit.zap_izp_id ");
			sql.append(" , zaiav_izp.doc_id ,izpit_result.case_pismena_nom");
			sql.append(" from izpit_result ");
			sql.append(" inner join izpit on izpit.izpit_id = izpit_result.izpit_id ");
			sql.append(" left outer join doc zap_izp on zap_izp.doc_id = izpit.zap_izp_id ");
			sql.append(" left outer join doc zaiav_izp on zaiav_izp.doc_id = izpit_result.zaiav_izp_id ");
			sql.append(" left outer join doc test_prot on test_prot.doc_id = izpit.test_prot_id ");
			sql.append(" left outer join doc case_prot on case_prot.doc_id = izpit.case_prot_id ");
			sql.append(" where izpit_result.lice_id = :liceId ");
			sql.append(" union all ");
			sql.append(" select -1 staj_id ");
			sql.append(" , zaiav_izp.rn_doc, zaiav_izp.doc_date, zaiav_izp.zaiav_date, zaiav_izp.doc_info, null rn_doc, null doc_date ");
			sql.append(" , null test_date, null test_result, null test_pismena_nom, null test_correct_answers, null test_remark ");
			sql.append(" , null case_date, null case_result, null case_remark, zaiav_izp.doc_id zaiav_izp_id, null zap_izp_id ");
			sql.append(" , zaiav_izp.doc_id ,izpit_result.case_pismena_nom");
			sql.append(" from lice_doc rel ");
			sql.append(" inner join doc zaiav_izp on zaiav_izp.doc_id = rel.doc_id ");
			sql.append(" left outer join izpit_result on izpit_result.lice_id = rel.lice_id and izpit_result.zaiav_izp_id = zaiav_izp.doc_id ");
			sql.append(" where rel.lice_id = :liceId and zaiav_izp.doc_vid = :vidZaiavIzp ");
			sql.append(" and izpit_result.result_id is null ");
			sql.append(" order by 3 desc NULLS LAST, 18 desc NULLS LAST, 7 desc NULLS LAST ");

			result = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
					.setParameter("liceId", liceId).setParameter("vidZaiavIzp", CODE_ZNACHENIE_DOC_VID_ZAIAV_IZPIT) //
					.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане на изпити за лице", e);
		}
		return result;
	}


	public String uploadIzpitResults(byte[] xlsxFile, ArrayList<Object[]> personsList, int examType, SystemData sd) {

		String systemOptionPorNom 	= 	"exam.col.prot.nom";
		String systemOptionEgn 		=	"exam.col.prot.egn";
		String systemOptionNomPis 	= 	"exam.col.prot.nomPis";
		String systemOptionCntOtg 	= 	"exam.col.prot.cntOtg";
		String systemOptionResult 	= 	"exam.col.prot.result";
		String systemOptionComment 	= 	"exam.col.prot.comment";

		String suffixTest = ".test";
		String suffixCase = ".case";
		String suffixOld =  ".old";

		int colNom 			= 0;
		int colEgn 			= 5;
		int colNomPis 		= 6;
		int colCntOtg 	= 7;
		int colResult 	= 8;
		int colComment 	= 9;



		Workbook workbook = null;



		String result = "";

		if (xlsxFile == null || xlsxFile.length == 0) {
			return "Подаден е празен файл с данни за изпит";
		}

		if (personsList == null || personsList.size() == 0) {
			return "За изпита няма въведен списък от хора";
		}

		if (personsList.get(0).length <= 11){
			return "Подаден е грешен формат на списъка с лица. Размер на данни за лице: " + personsList.get(0).length;
		}

		Object obj10 = personsList.get(0)[10];

		if (obj10 == null) {
			return "Подаден е грешен формат на списъка с лица. obj[10] is null ";
		}


		if (obj10 instanceof IzpitResult) {}else{
			return "Подаден е грешен формат на списъка с лица. obj[10].class " + obj10.getClass();
		}


		if (examType == 1){
			//Тест
			systemOptionPorNom 	+= suffixTest;
			systemOptionEgn 	+= suffixTest;
			systemOptionNomPis 	+= suffixTest;
			systemOptionCntOtg 	+= suffixTest;
			systemOptionResult 	+= suffixTest;
			systemOptionComment += suffixTest;
		}

		if (examType == 2){
			//Казус
			systemOptionPorNom 	+= suffixCase;
			systemOptionEgn 	+= suffixCase;
			systemOptionNomPis 	+= suffixCase;
			systemOptionResult 	+= suffixCase;
			systemOptionComment += suffixCase;
		}

		if (examType == 3){
			//Стар
				systemOptionPorNom 	+= suffixOld;
				systemOptionEgn 	+= suffixOld;
				systemOptionNomPis 	+= suffixOld;
				systemOptionResult 	+= suffixOld;
				systemOptionComment += suffixOld;
		}

        try {
            String valStr = sd.getSettingsValue(systemOptionPorNom);
			if (valStr != null) {
				colNom = Integer.valueOf(valStr.trim());
			}else{
				return "Не е намерена системна настройка: " + systemOptionPorNom;
			}
        } catch (DbErrorException e) {
            return "Грешка при извличане на системна настройка : " + systemOptionPorNom;
        }catch (NumberFormatException e) {
			return "Системната настройка " + systemOptionPorNom + " не е число !";
		}


		try {
			String valStr = sd.getSettingsValue(systemOptionEgn);
			if (valStr != null) {
				colEgn = Integer.valueOf(valStr.trim());
			}else{
				return "Не е намерена системна настройка: " + systemOptionEgn;
			}
		} catch (DbErrorException e) {
			return "Грешка при извличане на системна настройка: " + systemOptionEgn ;
		}catch (NumberFormatException e) {
			return "Системната настройка " + systemOptionEgn + " не е число !";
		}

		if(examType != 3) { // за изпит до 2019 няма номер на писм. работа
			try {
				String valStr = sd.getSettingsValue(systemOptionNomPis);
				if (valStr != null) {
					colNomPis = Integer.valueOf(valStr.trim());
				} else {
					return "Не е намерена системна настройка: " + systemOptionNomPis;
				}
			} catch (DbErrorException e) {
				return "Грешка при извличане на системна настройка: " + systemOptionNomPis;
			} catch (NumberFormatException e) {
				return "Системната настройка " + systemOptionNomPis + " не е число !";
			}
		}
		if (examType == 1) {
			//Само за тест изпита
			try {
				String valStr = sd.getSettingsValue(systemOptionCntOtg);
				if (valStr != null) {
					colCntOtg = Integer.valueOf(valStr.trim());
				} else {
					return "Не е намерена системна настройка: " + systemOptionCntOtg;
				}
			} catch (DbErrorException e) {
				return "Грешка при извличане на системна настройка: " + systemOptionCntOtg;
			} catch (NumberFormatException e) {
				return "Системната настройка " + systemOptionCntOtg + " не е число !";
			}
		}


		try {
			String valStr = sd.getSettingsValue(systemOptionResult);
			if (valStr != null) {
				colResult = Integer.valueOf(valStr.trim());
			}else{
				return "Не е намерена системна настройка: " + systemOptionResult;
			}
		} catch (DbErrorException e) {
			return "Грешка при извличане на системна настройка: " + systemOptionResult;
		}catch (NumberFormatException e) {
			return "Системната настройка " + systemOptionResult + " не е число !";
		}


		try {
			String valStr = sd.getSettingsValue(systemOptionComment);
			if (valStr != null) {
				colComment = Integer.valueOf(valStr.trim());
			}else{
				return "Не е намерена системна настройка: " + systemOptionComment;
			}
		} catch (DbErrorException e) {
			return "Грешка при извличане на системна настройка: " + systemOptionComment;
		}catch (NumberFormatException e) {
			return "Системната настройка " + systemOptionComment + " не е число !";
		}










		try {


            try {
                workbook = new HSSFWorkbook(new ByteArrayInputStream(xlsxFile));
            } catch (Exception e) {
                try {
                    workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxFile));
                } catch (Exception ex) {
					LOGGER.error("Подаденият файл не е XLSX/XLS или файлът е счупен !", e);
					return "Подаденият файл не е XLSX/XLS или файлът е счупен !";
                }


            }


            if (workbook.getNumberOfSheets() > 0) {

				Sheet sheet = workbook.getSheetAt(0);

				if (sheet.getLastRowNum() <= 1 ){
					return "В sheet 1 на ексела файла няма данни за лица !";
				}


				for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {



					//System.out.println("row " + i);
					Row row = sheet.getRow(i);

					if (row == null) {
						//System.out.println("skipping null row " + i);
						continue;
					}

					String nom = MigrUtils.getString(row.getCell(colNom));
					//System.out.println("nom: " + nom);

					if (nom== null){
						continue;
					}

					nom = nom.replace(".", "")	.trim();
                    try {
                        Integer.parseInt(nom);
                    } catch (NumberFormatException e) {
                        continue;
                    }

					//Вече сме в лицата

					String egnLnch = MigrUtils.getString(row.getCell(colEgn));
					if (egnLnch != null){
						egnLnch = egnLnch.replace(" ","").trim();
					}
					boolean found = false;
					for (Object[] person : personsList){

						obj10 = person[10];
						String listEgn = (String)person[11];
						String listLnch = (String)person[12];
						if (listEgn != null){
							listEgn = listEgn.replace(" ","").trim();
						}
						if (listLnch!= null){
							listLnch = listLnch.replace(" ","").trim();
						}
						if (egnLnch.equals(listEgn) || egnLnch.equals(listLnch) ){
							found = true;
						}else{
							continue;
						}

						//Лицето е намерено
						if (obj10 == null) {
							result += "Подаден е грешен формат на списъка с лица. obj[10] is null за лице с ЕГН/ЛНЧ: " + egnLnch + "\r\n";
						}

						if (obj10 instanceof IzpitResult) {

							IzpitResult ir = (IzpitResult) obj10;


							String comment = MigrUtils.getString(row.getCell(colComment));
							if (examType == 1) {
								ir.setTestRemark(comment);
							} else {
								if (examType == 2) {
									ir.setCaseRemark(comment);
								} else {
									ir.setCaseRemark(comment);
								}
							}


							if (examType != 3 && row.getCell(colNomPis) != null  ){ // за izpit do 2019 няма номер на писм. работа
								String nomPisStr =   row.getCell(colNomPis).toString().trim();
								if (nomPisStr != null && ! nomPisStr.isEmpty() ) {
									try {
										Integer nomPis = (int) Double.parseDouble(nomPisStr);

										if (examType == 1) {
											ir.setTestPismenaNom(nomPis);
										} else {
											if (examType == 2) {
												ir.setCasePismenaNom(nomPis);
											}
											//else {
											//	ir.setCasePismenaNom(nomPis);
											//}
										}
									} catch (NumberFormatException e) {
										result += "Номер на писмена работа за лице с ЕГН/ЛНЧ " + egnLnch + " не е число: "+nomPisStr+" \r\n";
										continue;
									}
								}
                            }else{
								//return "За лице с ЕГН/ЛНЧ " + egnLnch + " не е въведен номер на писмена работа !";
							}

							if (examType == 1) {
								if (row.getCell(colCntOtg) != null) {
									String cntOtgStr = row.getCell(colCntOtg).toString().trim();
									if (cntOtgStr != null && ! cntOtgStr.isEmpty() ) {
										try {
											Integer cntOtg = (int) Double.parseDouble(cntOtgStr);
											ir.setTestCorrectAnswers(cntOtg);
										} catch (NumberFormatException e) {
											result += "Брой верни отговори за лице с ЕГН/ЛНЧ " + egnLnch + " не е число: "+cntOtgStr+"\r\n";
											continue;
										}
									}
								} else {
									//return "За лице с ЕГН/ЛНЧ " + egnLnch + " не е въведен брой верни отговори !";
								}
							}



							if (row.getCell(colResult) != null){
								String resultStr = row.getCell(colResult).toString().trim();
								Integer resultInt = null;
								if (resultStr.equalsIgnoreCase("ДА") || resultStr.equalsIgnoreCase("ИЗДЪРЖАЛ") ){
									resultInt = 1;
								}

								if (resultStr.equalsIgnoreCase("НЕ") || resultStr.equalsIgnoreCase("НЕИЗДЪРЖАЛ") || resultStr.equalsIgnoreCase("НЕ ИЗДЪРЖАЛ") ){
									resultInt = 2;
								}

								if (resultStr.equalsIgnoreCase("неявилсе") || resultStr.equalsIgnoreCase("НЕЯВИЛ СЕ") || resultStr.equalsIgnoreCase("НЕ ЯВИЛ СЕ") || resultStr.equalsIgnoreCase("НЕ СЕ ЯВИЛ")){
									resultInt = 3;
								}

								if (resultInt == null){
									result +=  "За лице с ЕГН/ЛНЧ " + egnLnch + " не може да бъде обработена оценка: " + resultStr + "\r\n";
								}

								if (examType == 1) {
									ir.setTestResult(resultInt);
								}else{
									ir.setCaseResult(resultInt);
								}
								}else{
									result +=  "За лице с ЕГН/ЛНЧ " + egnLnch + " не е въведенa оценка !\r\n";
								}

							}else{
									result +=  "Подаден е грешен формат на списъка с лица. obj[10].class " + obj10.getClass() + " за лице с ЕГН/ЛНЧ: " + egnLnch + "\r\n";
							}
					}

					if (!found && egnLnch!=""){
						result +=  "Лице с ЕГН/ЛНЧ " + egnLnch + " не е намерен за този изпит!\r\n";
					}

                }
			}else{
				return "Грешка в структурата на ексел файла";
			}
		} catch (Exception e) {
			LOGGER.error("Неочаквана грешка при обработка на файла !", e);
			return "Неочаквана грешка при обработка на файла ! " + e.getMessage();
		}

		if (result.isEmpty()) {
			return null;
		}else{
			return result;
		}

	}


	public String proccessFileBefore2011(String fileName, byte[] fileData, String nomPar, boolean isTest ) {


		HashMap<String, Integer> sadilista = new HashMap<String, Integer>();
		String warnings = "";
		SystemData sd = new SystemData();



		HashMap<Date, Integer>  zapovediStaj = new HashMap<Date, Integer>();
		HashMap<Date, Integer>  zapovediIzpit = new HashMap<Date, Integer>();
		HashMap<Date, Integer>  protokoliIzpit = new HashMap<Date, Integer>();
		HashMap<Date, Integer>  izpitMap = new HashMap<Date, Integer>();

		Integer liceBefore = 1;

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");




		String dopInfoDosie = "Мигрирация на лица преди 2011 от файл " + fileName + " на " + sdf.format(new Date());
		int cntSaved = 0;
		try {

			JPA.getUtil().begin();

			Files file = new Files();
			file.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
			file.setFilename(fileName);
			file.setContent(fileData);
			file.setFileInfo(fileName);

			FilesDAO fdao = new FilesDAO(ActiveUser.DEFAULT);
			fdao.save(file);


			if(isTest){
				MigrUtils.init(6000000, sd);
			}else{
				MigrUtils.init(0, sd);
			}


			List<SystemClassif> sadClassif = sd.getSysClassification(206 , new Date(), 1);
			for (SystemClassif classif : sadClassif) {
				sadilista.put(classif.getTekst().toUpperCase().trim(), classif.getCode());
			}




			XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileData));  // for xlsx


			if (workbook.getNumberOfSheets() > 0) {

				XSSFSheet sheet = workbook.getSheetAt(0);
				for (int i = 0; i < sheet.getLastRowNum() + 1; i++) {
					XSSFRow row = sheet.getRow(i);
					if (row == null){
						continue;
					}
					String nom = MigrUtils.getString(row.getCell(0));
					try {
						int porNom = Integer.parseInt(nom.trim());
						//System.out.println(porNom);
					} catch (NumberFormatException e) {
						//System.out.println("************************* СЧУПЕН РЕД *************************************" + row.getCell(0));
						continue;
					}

					Date datProt = null;
					try {
						datProt = MigrUtils.getDate(row.getCell(1));
					} catch (UnexpectedResultException e) {
						warnings += "Грешна дата на протокол за пореден номер на лице:  " +nom + "\r\n";
						System.out.println("Грешна дата на протокол за пореден номер на лице:  " +nom);
						datProt = DateUtils.systemMinDate();

					}
					//System.out.println(datProt);

					String rnUdost = MigrUtils.getString(row.getCell(2));
					String egnLnch = MigrUtils.getString(row.getCell(3));
					String ime = MigrUtils.getString(row.getCell(4));
					String prezime = MigrUtils.getString(row.getCell(5));
					String familia = MigrUtils.getString(row.getCell(6));
					String mestorajdane = MigrUtils.getString(row.getCell(7));
					String sad = MigrUtils.getString(row.getCell(8));
					String zabelejka = MigrUtils.getString(row.getCell(9));

					if (egnLnch != null && egnLnch.length() == 8) {
						egnLnch = "00" + egnLnch;
					}
					if (egnLnch != null && egnLnch.length() == 9) {
						egnLnch = "0" + egnLnch;
					}

					List lica = JPA.getUtil().getEntityManager().createNativeQuery("select lice_id from lice where egn = ? or lnc = ?")
							.setParameter(1, egnLnch)
							.setParameter(2, egnLnch)
							.getResultList();

					if (lica.size() >  0){
						warnings += "Лице с ЕГН/ЛНЧ "+egnLnch+" вече е записано в системата. Пропуска се\r\n";
						continue;
					}


					Date datRajd = null;
					boolean isEgn = false;
					boolean valid =  ValidationUtils.isValidEGN(egnLnch);
					if (!valid) {
						valid = ValidationUtils.isValidLNCH(egnLnch);
						if (! valid) {
							warnings += "Невалидно ЕГН/ЛНЧ: " + egnLnch + " за лице " + ime + " " + prezime + " " + familia + ". Лицето ще бъде записано като чужд гражданин\r\n";
						}else{
							//System.out.println("ЕГН: " + egn + " за лице " + ime + " " + prezime + " " + familia + " e ЛНЧ") ;
						}


					}else{
						isEgn = true;
						try {
							datRajd = StringUtils.birthdayFromEGN(egnLnch);
						} catch (ParseException e) {
							warnings += "Грешка при извличане на дата на раждане от ЕГН/ЛНЧ: " + egnLnch + "\r\n";
						}
					}

					Integer sadId = null;
					if (! SearchUtils.isEmpty(sad) ) {

						if (sad.equals("Окръжен съд Софийски окръжен съд")) {
							sad = "Софийски градски съд";
						}

						if (sad.equals("Окръжен съд Софийски градски съд")) {
							sad = "Софийски градски съд";
						}

						if (sad.contains("СГС")) {
							sad = "Софийски градски съд";
						}




						if (sad.equals("Софийски градски съд София")) {
							sad = "Софийски градски съд";
						}

						if (sad.equals("Софийски окръжен съд София")) {
							sad = "Софийски окръжен съд";
						}

						sadId = sadilista.get(sad.trim().toUpperCase());
						if (sadId == null) {
							warnings += "Не може да бъде намерен съд с наименование " + sad + " в списъка със съдилища \r\n";

						}
					}

					Integer userId = -1;//MigrUtils.checkAndCreateUser(userReg);
					Date datReg = new Date();


					String rnZaiavStaj = "ЗАЯВЛС_" + nomPar + "-" + nom;
					Date datZaiavStaj = datProt;

					String rnZapStaj = "ЗАПС_" + nomPar;
					Date datZapStaj = datProt;

					String rnZaiavIzpit = "ЗАЯВЛИ_" + nomPar + "-" + nom;
					Date datZaiavIzpit = datProt;

					String rnZapIzpit = "ЗАПИ_" + nomPar;
					Date datZapIzpit = datProt;



					Date datUdost = datProt;

					Date datKraiStaj = null;



					/// /////////////////////////////////////////////////////////////////////////////////////////////////////////


					//Заявление за стаж
					Integer seqDocZaiavStaj = MigrUtils.getSeq("seq_doc");
					String sqlDoc = "INSERT INTO doc (doc_id, doc_vid, zaiav_date, rn_doc, doc_date, doc_info, user_reg, date_reg) VALUES (?,?,?,?,?,?,?,?)";
					Query query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
					query.setParameter(1, seqDocZaiavStaj);
					query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAIAV_STAJ);
					query.setParameter(3, null);
					query.setParameter(4, rnZaiavStaj);
					query.setParameter(5, datZaiavStaj);
					query.setParameter(6, "Заявление за стаж от " + ime + " " + prezime + " " + familia + "(ЕГН/ЛНЧ: " +egnLnch + ")");
					query.setParameter(7, userId);
					query.setParameter(8, datReg);
					//System.out.println(seqDoc + " Заявление за стаж от " + ime + " " + prezime + " " + familia);
					query.executeUpdate();

					//Заповед за стаж
					Integer seqDocZapStaj = null;
					if (datZapStaj != null) {
						Integer id = zapovediStaj.get(datZapStaj);
						if (id != null) {
							seqDocZapStaj = id;
						}else {

							seqDocZapStaj = MigrUtils.getSeq("seq_doc");
							query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
							query.setParameter(1, seqDocZapStaj);

							query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_STAJ);
							query.setParameter(3, null);
							query.setParameter(4, rnZapStaj);
							query.setParameter(5, datZapStaj);
							query.setParameter(6, null);
							query.setParameter(7, userId);
							query.setParameter(8, datReg);
							//System.out.println(seqDoc + " Заявление за стаж от " + ime + " " + prezime + " " + familia);
							query.executeUpdate();
							zapovediStaj.put(datZapStaj, seqDocZapStaj);

							fdao.saveFileObject(file, seqDocZapStaj, UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC);

						}
					}


					//Заявление за изпит
					Integer seqDocZaiavIzpit = null;
					if (datZaiavIzpit != null) {
						seqDocZaiavIzpit = MigrUtils.getSeq("seq_doc");
						query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
						query.setParameter(1, seqDocZaiavIzpit);
						query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAIAV_IZPIT);
						query.setParameter(3, datZaiavIzpit);
						query.setParameter(4, rnZaiavIzpit);
						query.setParameter(5, datZaiavIzpit);
						query.setParameter(6, "Заявление за изпит от " + ime + " " + prezime + " " + familia + "(ЕГН/ЛНЧ: " + egnLnch + ")");
						query.setParameter(7, userId);
						query.setParameter(8, datReg);
						//System.out.println(seqDoc + " Заявление за стаж от " + ime + " " + prezime + " " + familia);
						query.executeUpdate();
					}


					//Протокол за изпит
					Integer seqProtIzpit = null;
					if (datProt != null) {
						Integer id = protokoliIzpit.get(datProt);
						if (id != null) {
							seqProtIzpit = id;
						}else {
							seqProtIzpit = MigrUtils.getSeq("seq_doc");
							query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
							query.setParameter(1, seqProtIzpit);
							query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT_TEST);
							query.setParameter(3, datProt);
							query.setParameter(4, null);
							query.setParameter(5, datProt);
							query.setParameter(6, "Протокол от изпит от " + sdf.format(datProt));
							query.setParameter(7, userId);
							query.setParameter(8, datReg);
							query.executeUpdate();
							protokoliIzpit.put(datProt, seqProtIzpit);
						}
					}



					//Заповед за изпит
					Integer seqDocZapIzpit = zapovediIzpit.get(datZapIzpit);
					Integer seqIzpit = izpitMap.get(datZapIzpit);

					if (seqDocZapIzpit == null) {

						seqDocZapIzpit = MigrUtils.getSeq("seq_doc");
						query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
						query.setParameter(1, seqDocZapIzpit);
						query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019);
						query.setParameter(3, datZapIzpit);
						query.setParameter(4, rnZapIzpit);
						query.setParameter(5, datZapIzpit);
						query.setParameter(6, "Заповед");
						query.setParameter(7, userId);
						query.setParameter(8, datReg);
						query.executeUpdate();
						zapovediIzpit.put(datZapIzpit, seqDocZapIzpit);
					}


					if (seqIzpit == null) {
						seqIzpit = MigrUtils.getSeq("seq_izpit");
						query = JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO izpit (izpit_id, zap_izp_id, case_date, case_prot_id, user_reg, date_reg) VALUES (?,?,?,?,?,?)");
						query.setParameter(1, seqIzpit);
						query.setParameter(2, seqDocZapIzpit);
						query.setParameter(3, datProt);
						query.setParameter(4, seqProtIzpit);
						query.setParameter(5, userId);
						query.setParameter(6, datReg);
						query.executeUpdate();
						izpitMap.put(datZapIzpit, seqIzpit);
					}





					//Удостоверение
					Integer seqDocUdost = null;
					if (datUdost != null || ! SearchUtils.isEmpty(rnUdost)) {
						seqDocUdost = MigrUtils.getSeq("seq_doc");
						query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
						query.setParameter(1, seqDocUdost);
						query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_UDOST_PRAVO);
						query.setParameter(3, null);
						query.setParameter(4, rnUdost);
						query.setParameter(5, datUdost);
						query.setParameter(6, "Удостоверение на " + ime + " " + prezime + " " + familia + "(ЕГН/ЛНЧ: " + egnLnch + ")");
						query.setParameter(7, userId);
						query.setParameter(8, datReg);
						//System.out.println(seqDoc + " Заявление за стаж от " + ime + " " + prezime + " " + familia);
						query.executeUpdate();
					}






					int seqLice = MigrUtils.getSeq("seq_lice");
					int status = 7;
					Date datStatus = new Date();


					String sqlLice = "INSERT INTO lice (lice_id, zaiav_staj_id, egn, lnc, firstname, surname, lastname, names, birth_date, last_zaiav_staj_id, udost_id, user_reg, date_reg, birth_place, status, status_date,do_2019, lice_info) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

					query = JPA.getUtil().getEntityManager().createNativeQuery(sqlLice);
					query.setParameter(1, seqLice);
					query.setParameter(2, seqDocZaiavStaj);
					if (isEgn){
						query.setParameter(3, egnLnch);
					}else{
						query.setParameter(3, null);
					}

					if (isEgn){
						query.setParameter(4, null);
					}else{
						query.setParameter(4, egnLnch);
					}

					String dopInfo = dopInfoDosie;
					if (! SearchUtils.isEmpty(zabelejka)) {
						dopInfo += "\r\n" + zabelejka;
					}

					query.setParameter(5, ime);
					query.setParameter(6, prezime);
					query.setParameter(7, familia);
					query.setParameter(8, ime + " " + prezime + " " + familia);
					query.setParameter(9, datRajd);
					query.setParameter(10, seqDocZaiavStaj);
					query.setParameter(11, seqDocUdost);
					query.setParameter(12, userId);
					query.setParameter(13, datReg);
					query.setParameter(14, mestorajdane);
					query.setParameter(15, status); //??????????????????????????????????
					query.setParameter(16, datStatus); //??????????????????????????????????
					query.setParameter(17, liceBefore);
					query.setParameter(18, dopInfo);
					query.executeUpdate();

					//insertLiceStatus(allStat);     ДА СЕ ПОМИСЛИ

					if (seqDocZaiavStaj != null) {
						insertNativeLiceDoc(seqLice, seqDocZaiavStaj);
					}

					if (seqDocZapStaj != null) {
						insertNativeLiceDoc(seqLice, seqDocZapStaj);
					}

					if (seqDocZaiavIzpit != null) {
						insertNativeLiceDoc(seqLice, seqDocZaiavIzpit);
					}

					if (seqDocZapIzpit != null) {
						insertNativeLiceDoc(seqLice, seqDocZapIzpit);
					}

					if (seqProtIzpit!= null) {
						insertNativeLiceDoc(seqLice, seqProtIzpit);
					}

					if (seqDocUdost!= null) {
						insertNativeLiceDoc(seqLice, seqDocUdost);
					}

					String sqlStaj = "INSERT INTO staj (staj_id, lice_id, zaiav_staj_id, zap_staj_id, staj_vid, osn_institution, osn_end_date, user_reg, date_reg, zaiav_izp_id) VALUES (?,?,?,?,?,?,?,?,?, ?)";
					int seqStaj = MigrUtils.getSeq("seq_staj");
					query = JPA.getUtil().getEntityManager().createNativeQuery(sqlStaj);
					query.setParameter(1, seqStaj);
					query.setParameter(2, seqLice);
					query.setParameter(3, seqDocZaiavStaj);
					query.setParameter(4, seqDocZapStaj);
					query.setParameter(5, 1);
					query.setParameter(6, sadId);
					query.setParameter(7, datKraiStaj);
					query.setParameter(8, userId);
					query.setParameter(9, datReg);
					query.setParameter(10, seqDocZaiavIzpit);
					query.executeUpdate();


					if (seqIzpit != null) {
						Integer seqIzpitR = MigrUtils.getSeq("seq_izpit_result");
						query = JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO izpit_result (result_id, izpit_id, staj_id, lice_id, case_result, test_result,user_reg, date_reg, zaiav_izp_id) VALUES (?,?,?,?,?,?,?,?,?)");
						query.setParameter(1, seqIzpitR);
						query.setParameter(2, seqIzpit);
						query.setParameter(3, seqStaj);
						query.setParameter(4, seqLice);
						query.setParameter(5, 1);
						query.setParameter(6, 1);
						query.setParameter(7, userId);
						query.setParameter(8, datReg);
						query.setParameter(9, seqDocZaiavIzpit);
						query.executeUpdate();
					}

					cntSaved++;
				}
			}

			JPA.getUtil().commit();

		} catch (Exception e) {
			JPA.getUtil().rollback();
			e.printStackTrace();
		}finally {
			JPA.getUtil().closeConnection();
		}

		return "Брой записани лица: " + cntSaved + "\r\n" + warnings;

	}


	public void insertNativeLiceDoc(int idLice, int idDoc) throws DbErrorException {

		int id = MigrUtils.getSeq("seq_lice_doc");
		Query q = JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO lice_doc (id, lice_id, doc_id, user_reg, date_reg) VALUES (?,?,?,?,?)");
		q.setParameter(1, id);
		q.setParameter(2, idLice);
		q.setParameter(3, idDoc);
		q.setParameter(4, -1);
		q.setParameter(5, new Date());
		q.executeUpdate();
	}

}
