package com.ib.urireg.db.dao;

import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.FileObject;
import com.ib.system.db.dto.Files;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.urireg.db.dto.*;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.exceptions.DbErrorException;
import com.ib.urireg.system.UriregConstants;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;

import java.util.*;

import static com.ib.urireg.system.UriregConstants.*;

public class DocDAO extends AbstractDAO<Doc> {

	/**
	 * Това ще даде възможност да се генерира номер в отделна транзакция
	 *
	 * @author belev
	 */
	private class GenTransact extends Thread {
		private final Doc doc;
		Exception ex; // и като е в отделна нишка и гръмне няма как да знам и за това тука ще се пази грешката ако е има

		/**
		 * @param doc
		 */
		GenTransact(Doc doc) {
			this.doc = doc;
		}

		@Override
		public void run() {
			try {
				JPA.getUtil().begin();

				genRnDocUdost(this.doc);

				JPA.getUtil().commit();

			} catch (Exception e) {
				JPA.getUtil().rollback();
				this.ex = e;

			} finally {
				JPA.getUtil().closeConnection(); // това си е в отделна нишка и задължително трябва да си се затвори само
			}
		}
	}

	public DocDAO(ActiveUser user) {
		super(Doc.class, user);
	}

	/**
	 * Освен валидация дали действието е позволено изтрива/коригира свързани обекти
	 *
	 * @param entity
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@Override
	public void delete(Doc entity) throws DbErrorException, ObjectInUseException {
		Integer liceId = null;

		if (Objects.equals(entity.getDocVid(), CODE_ZNACHENIE_DOC_VID_ZAP_STAJ) || Objects.equals(entity.getDocVid(), CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ)) {
			deleteZapStaj(entity);

		} else if (Objects.equals(entity.getDocVid(), CODE_ZNACHENIE_DOC_VID_ZAIAV_IZPIT)) {
			liceId = deleteZaiavIzpit(entity);

		} else if (Objects.equals(entity.getDocVid(), CODE_ZNACHENIE_DOC_VID_ZAIAV_DOP_STAJ)) {
			liceId = deleteZaiavStajDop(entity);

		} else if (Objects.equals(entity.getDocVid(), CODE_ZNACHENIE_DOC_VID_ZAIAV_STAJ)) {
			liceId = deleteZaiavStaj(entity);

		} else if (Objects.equals(entity.getDocVid(), CODE_ZNACHENIE_DOC_VID_UDOST_PRAVO)) {
			liceId = deleteUdostPravo(entity);

		} else if (Objects.equals(entity.getDocVid(), CODE_ZNACHENIE_DOC_VID_ZAIAV_DUBL_UDOST)) {
			liceId = deleteZaiavDublUdost(entity);
		}

		if (liceId != null) { // за да може на тези видове документи да се журналира изтриването към досието на лицето
			entity.setJoinedIdObject1(liceId);
			entity.setJoinedCodeObject1(CODE_ZNACHENIE_JOURNAL_LICE);
		}
		super.delete(entity); // и накрая основния документ
	}

	/**
	 * Изтрива Заявление за дубликат на удостоверение за юридическа правоспособност
	 *
	 * @param entity
	 * @return ИД на свързаното лице
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	private Integer deleteZaiavDublUdost(Doc entity) throws DbErrorException, ObjectInUseException {
		Integer liceId = null;
		List<Object[]> list;
		List<Object[]> filesList;
		boolean markOriginal;
		try {
			list = JPA.getUtil().getEntityManager().createQuery( //
							"select ld, l.udostId from LiceDoc ld" + //
									" inner join Lice l on l.id = ld.liceId" + //
									" where ld.docId = ?1") //
					.setParameter(1, entity.getId()).getResultList();

			if (list.isEmpty()) {
				throw new ObjectInUseException("Не е намерено лице за това заявление за дубликат.");
			}

			LiceDoc liceDoc = (LiceDoc) list.get(0)[0];

			List<LiceDoc> otherList = JPA.getUtil().getEntityManager().createQuery( //
							"select ld from LiceDoc ld inner join Doc d on d.id = ld.docId" + //
									" where ld.liceId = ?1 and d.id <> ?2 and d.docVid = ?3") //
					.setParameter(1, liceDoc.getLiceId()).setParameter(2, liceDoc.getDocId()) //
					.setParameter(3, CODE_ZNACHENIE_DOC_VID_ZAIAV_DUBL_UDOST).getResultList();
			markOriginal = otherList.isEmpty(); // ако няма други то основния трябва да стане оригинал

			filesList = JPA.getUtil().getEntityManager().createQuery( //
							"select f, ufo from FileObject fo" + //
									" inner join FileObject ufo on ufo.fileId = fo.fileId" + //
									" inner join Files f on f.id = ufo.fileId" + //
									" where fo.objectId = ?1 and fo.objectCode = ?2 ") //
					.setParameter(1, entity.getId()).setParameter(2, CODE_ZNACHENIE_JOURNAL_DOC) //
					.getResultList();

		} catch (ObjectInUseException e) {
			throw e;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти към заявление за дубликат!", e);
		}

		// изтривам връзката на дока с лицето
		LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
		for (Object[] row : list) { // трябва да е само една, но все пак
			LiceDoc liceDoc = (LiceDoc) row[0];

			liceId = liceDoc.getLiceId();
			liceDocDao.delete(liceDoc);
		}

		FilesDAO filesDao = new FilesDAO(getUser());
		for (Object[] row : filesList) {
			Files files = (Files) row[0];
			FileObject fo = (FileObject) row[1];
			
			files.setFileObjectId(fo.getId());
			files.setParrentID(fo.getObjectId());
			files.setParentObjCode(fo.getObjectCode());

			filesDao.deleteFileObject(files);
		}

		if (markOriginal) {
			DocDAO docDao = new DocDAO(getUser());
			Doc udost = docDao.findById(SearchUtils.asInteger(list.get(0)[1]));

			udost.setOriginal(SysConstants.CODE_ZNACHENIE_DA);
			docDao.save(udost, liceId);
		}
		return liceId;
	}

	/**
	 * Изтриване на Удост.Док. за лице
	 *
	 * @param entity
	 * @return ИД на свързаното лице
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	private Integer deleteUdostPravo(Doc entity) throws DbErrorException, ObjectInUseException {
		Integer liceId;
		List<Lice> liceList;
		List<LiceDoc> liceDocList;
		try {
			liceList = JPA.getUtil().getEntityManager().createQuery( //
							"select l from Lice l where l.udostId = ?1")//
					.setParameter(1, entity.getId()).getResultList();
			if (liceList.isEmpty()) {
				throw new ObjectInUseException("Не е намерено лице за този удостоверителен документ.");
			}

			liceDocList = JPA.getUtil().getEntityManager().createQuery( //
							"select ld from LiceDoc ld where ld.docId = ?1") //
					.setParameter(1, entity.getId()).getResultList();

		} catch (ObjectInUseException e) {
			throw e;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти към удостоверителен документ!", e);
		}

		// изтривам връзката на дока с лицето
		LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
		for (LiceDoc liceDoc : liceDocList) { // трябва да е само една, но все пак
			liceDocDao.delete(liceDoc);
		}


		LiceDAO liceDao = new LiceDAO(getUser());
		Lice lice = liceList.get(0);
		liceId = lice.getId();

		lice.setUdostId(null); // и нулирам УД за лицето
		if (lice.getStatus().equals(CODE_ZNACHENIE_LICE_STATUS_PRAVO)) {
			lice.setStatus(CODE_ZNACHENIE_LICE_STATUS_EXAM_PASSED); // като коригирам и статуса ако е бил правосподобен
		} // иначе си остава какъвто си е бил преди

		liceDao.save(lice);

		return liceId;
	}

	/**
	 * Изтрива свързаните данни към заявление за допълнителн стаж
	 *
	 * @param entity
	 * @return ИД на свързаното лице
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	private Integer deleteZaiavStajDop(Doc entity) throws DbErrorException, ObjectInUseException {
		Integer liceId;
		List<Object[]> liceList;
		List<Object[]> stajList;
		List<LiceDoc> liceDocList;
		try {
			liceList = JPA.getUtil().getEntityManager().createQuery( // търсим предпоследното заявление за стаж
							"select l, s.zaiavStajId from Lice l left outer join Staj s on s.liceId = l.id" + //
									" and s.zaiavStajId <> l.lastZaiavStajId" + //
									" where l.lastZaiavStajId = ?1 order by s.dateReg desc")//
					.setParameter(1, entity.getId()).setMaxResults(1).getResultList();
			if (liceList.isEmpty()) {
				throw new ObjectInUseException("Може да изтриете само последното заявление за допълнителен стаж.");
			}

			stajList = JPA.getUtil().getEntityManager().createQuery( //
					"select s, ir.id from Staj s left outer join IzpitResult ir on ir.stajId = s.id" + //
							" where s.zaiavStajId = ?1").setParameter(1, entity.getId()).getResultList();

			liceDocList = JPA.getUtil().getEntityManager().createQuery( //
							"select ld from LiceDoc ld where ld.docId = ?1") //
					.setParameter(1, entity.getId()).getResultList();

		} catch (ObjectInUseException e) {
			throw e;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти към заявлението!", e);
		}

		// трябва да се изтрие стажа
		StajDAO stajDao = new StajDAO(getUser());

		if (!stajList.isEmpty()) { // трябва да е точно  един, но все пак
			Object[] row = stajList.get(0);

			Staj staj = (Staj) row[0];
			if (staj.getZapStajId() != null) {
				throw new ObjectInUseException("Заявлението не може да бъде изтрито, защото лицето вече е включено в заповед за стаж.");
			}
			if (staj.getZaiavIzpId() != null) {
				throw new ObjectInUseException("Заявлението не може да бъде изтрито, защото лицето вече е подало заявление за изпит.");
			}
			if (row[1] != null) {
				throw new ObjectInUseException("Заявлението не може да бъде изтрито, защото лицето вече е включено в заповед за изпит.");
			}
			stajDao.delete(staj);
		}

		// изтривам и връзката на дока с лицето
		LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
		for (LiceDoc liceDoc : liceDocList) { // трябва да е само една, но все пак
			liceDocDao.delete(liceDoc);
		}

		// и данните за лицето
		LiceDAO liceDao = new LiceDAO(getUser());
		Object[] row = liceList.get(0);

		Lice lice = (Lice) row[0];
		liceId = lice.getId();
		Integer prevZaiavStajId = SearchUtils.asInteger(row[1]);

		if (prevZaiavStajId != null) {
			lice.setLastZaiavStajId(prevZaiavStajId);
		} else {
			lice.setLastZaiavStajId(lice.getZaiavStajId());
		}
		lice.setStatus(CODE_ZNACHENIE_LICE_STATUS_EXAM_FAILED);
		liceDao.save(lice);

		return liceId;
	}

	/**
	 * Изтрива свързаните данни към заявление за стаж. Изтрива цялото !!! Досие !!!.
	 *
	 * @param entity
	 * @return ИД на свързаното лице
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	private Integer deleteZaiavStaj(Doc entity) throws DbErrorException, ObjectInUseException {
		Integer liceId;
		List<Object[]> liceList;
		List<Object[]> stajList;
		List<LiceDoc> liceDocList;
		try {
			liceList = JPA.getUtil().getEntityManager().createQuery( // проверявам дали са няколко стажа
							"select l, s.zaiavStajId from Lice l left outer join Staj s on s.liceId = l.id" + //
									" where l.lastZaiavStajId = ?1")//
					.setParameter(1, entity.getId()).getResultList();
			if (liceList.size() != 1) {
				throw new ObjectInUseException("Моля, първо изтрийте заявленията за допълнителен стаж.");
			}

			stajList = JPA.getUtil().getEntityManager().createQuery( //
					"select s, ir.id from Staj s left outer join IzpitResult ir on ir.stajId = s.id" + //
							" where s.zaiavStajId = ?1").setParameter(1, entity.getId()).getResultList();

			liceDocList = JPA.getUtil().getEntityManager().createQuery( //
							"select ld from LiceDoc ld where ld.docId = ?1") //
					.setParameter(1, entity.getId()).getResultList();

		} catch (ObjectInUseException e) {
			throw e;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти към заявлението!", e);
		}

		// трябва да се изтрие стажа
		StajDAO stajDao = new StajDAO(getUser());

		if (!stajList.isEmpty()) { // трябва да е точно  един, но все пак
			Object[] row = stajList.get(0);

			Staj staj = (Staj) row[0];
			if (staj.getZapStajId() != null) {
				throw new ObjectInUseException("Досието не може да бъде изтрито, защото лицето вече е включено в заповед за стаж.");
			}
			if (staj.getZaiavIzpId() != null) {
				throw new ObjectInUseException("Досието не може да бъде изтрито, защото лицето вече е подало заявление за изпит.");
			}
			if (row[1] != null) {
				throw new ObjectInUseException("Досието не може да бъде изтрито, защото лицето вече е включено в заповед за изпит.");
			}
			stajDao.delete(staj);
		}

		// изтривам и връзката на дока с лицето
		LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
		for (LiceDoc liceDoc : liceDocList) { // трябва да е само една, но все пак
			liceDocDao.delete(liceDoc);
		}

		// и данните за лицето
		LiceDAO liceDao = new LiceDAO(getUser());
		Lice lice = (Lice) liceList.get(0)[0];

		liceId = lice.getId();
		liceDao.delete(lice);

		return liceId;
	}

	/**
	 * Изтрива свързаните данни към заявление за изпит
	 *
	 * @param entity
	 * @return ИД на свързаното лице
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	private Integer deleteZaiavIzpit(Doc entity) throws DbErrorException, ObjectInUseException {
		Integer liceId = null;
		List<Object[]> stajList;
		List<LiceDoc> liceDocList;
		try {
			int cnt = ((Number) JPA.getUtil().getEntityManager().createNativeQuery( //
							"select count (*) cnt from izpit_result where zaiav_izp_id = ?1") //
					.setParameter(1, entity.getId()).getResultList().get(0)).intValue();
			if (cnt > 0) {
				throw new ObjectInUseException("Заявлението не може да бъде изтрито, защото лицето вече е включено в заповед за изпит.");
			}

			stajList = JPA.getUtil().getEntityManager().createQuery( //
							"select s, ir.zaiavIzpId, l.status from Staj s" + //
									" inner join Lice l on l.id = s.liceId" + //
									" left outer join IzpitResult ir on ir.stajId = s.id" + //
									" where s.zaiavIzpId = ?1 order by ir.zaiavIzpId desc NULLS LAST") //
					.setParameter(1, entity.getId()).setMaxResults(1).getResultList();

			liceDocList = JPA.getUtil().getEntityManager().createQuery( //
							"select ld from LiceDoc ld where ld.docId = ?1") //
					.setParameter(1, entity.getId()).getResultList();

		} catch (ObjectInUseException e) {
			throw e;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти към заявлението!", e);
		}

		// трябва да се нулира стажа
		StajDAO stajDao = new StajDAO(getUser());

		for (Object[] row : stajList) { // трябва да е само един, но все пак
			Integer id = SearchUtils.asInteger(row[2]); // статуса на лицеро
			if (!Objects.equals(id, CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED)) {
				throw new ObjectInUseException("За да изтриете заявлението, лицето трябва да бъде в статус Одобрен за стаж.");
			}

			Staj staj = (Staj) row[0];

			Integer zaiavIzpId = SearchUtils.asInteger(row[1]);
			staj.setZaiavIzpId(zaiavIzpId); // взимам последното заявление за изпит по този стаж (може и да няма)

			stajDao.save(staj);
		}

		// изтривам и връзката на дока с лицето
		LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
		for (LiceDoc liceDoc : liceDocList) { // трябва да е само една, но все пак
			liceId = liceDoc.getLiceId();
			liceDocDao.delete(liceDoc);
		}
		return liceId;
	}

	/**
	 * Изтрива заповед за стаж
	 *
	 * @param entity
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	private void deleteZapStaj(Doc entity) throws DbErrorException, ObjectInUseException {
		StajDAO stajDao = new StajDAO(getUser());

		List<Object[]> stajList = stajDao.findStajList(entity.getId(), null);
		for (Object[] staj : stajList) {
			if (staj[15] != null) { // staj.zaiav_izp_id
				throw new ObjectInUseException("В заповедта има лица, които са подали заявления за изпит." + //
						" Изтриването не е позолено.");
			}
			stajDao.removeStajList(staj);
		}
	}

	/**
	 * Търсене на документ по номер, дата и вид.
	 *
	 * @param rnDoc
	 * @param docDate
	 * @param docVid
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	@SuppressWarnings("unchecked")
	public Doc findByNomDateVid(String rnDoc, Date docDate, Integer docVid) throws DbErrorException, InvalidParameterException {
		if (SearchUtils.isEmpty(rnDoc) || docDate == null || docVid == null) {
			throw new InvalidParameterException("За търсене на документ са необходими номер, дата и вид на документа.");
		}
		rnDoc = SearchUtils.trimToNULL_Upper(rnDoc);

		try {
			Query query = JPA.getUtil().getEntityManager().createQuery("select d from Doc d" + //
					" where upper(d.rnDoc) = :rnDocArg and d.docVid = :docVidArg" + //
					" and d.docDate >= :docDateFrom and d.docDate <= :docDateTo ");

			query.setParameter("rnDocArg", rnDoc);
			query.setParameter("docVidArg", docVid);
			query.setParameter("docDateFrom", DateUtils.startDate(docDate));
			query.setParameter("docDateTo", DateUtils.endDate(docDate));

			List<Doc> docList = query.getResultList();
			if (docList.isEmpty()) {
				return null;
			}
			Doc doc = docList.get(0);

			if (doc.getCodeSaglList() != null) {
				doc.setCodeSaglList(new ArrayList<>(doc.getCodeSaglList()));
			}
			if (doc.getPrilVidList() != null) {
				doc.setPrilVidList(new ArrayList<>(doc.getPrilVidList()));
			}
			return doc;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при Търсене на документ по номер, дата и вид.", e);
		}
	}

	/**
	 * Допълнителни екстри се зарежат при необходимост
	 *
	 * @param id
	 * @return
	 * @throws DbErrorException
	 */
	@Override
	public Doc findById(Object id) throws DbErrorException {
		Doc doc = super.findById(id);
		if (doc != null) {
			if (doc.getCodeSaglList() != null) {
				doc.setCodeSaglList(new ArrayList<>(doc.getCodeSaglList()));
			}
			if (doc.getPrilVidList() != null) {
				doc.setPrilVidList(new ArrayList<>(doc.getPrilVidList()));
			}
		}
		return doc;
	}

	/** за да го има за журнала след мерге */
	@Override
	protected Doc merge(Doc entity) throws DbErrorException {
		Doc merged = super.merge(entity);
		merged.setJoinedIdObject1(entity.getJoinedIdObject1());
		merged.setJoinedCodeObject1(entity.getJoinedCodeObject1());
		return merged;
	}

	/**
	 * Важно е този метод да се вика при запис/корекция на документи в лицето, защото така ще се навърже към неговия журнал
	 *
	 * @param entity
	 * @param liceId
	 * @return
	 * @throws DbErrorException
	 */
	public Doc save(Doc entity, Integer liceId) throws DbErrorException {
		entity.setJoinedIdObject1(liceId);
		entity.setJoinedCodeObject1(CODE_ZNACHENIE_JOURNAL_LICE);

		return super.save(entity);
	}

	/**
	 * Запис на УД с генериране на номер
	 *
	 * @param lice
	 * @param doc
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 * @throws ObjectInUseException
	 */
	public void saveUdost(Lice lice, Doc doc) throws DbErrorException, InvalidParameterException, ObjectInUseException {
		if (doc.getDocDate() == null) {
			throw new InvalidParameterException("Моля, въведете дата на документа.");
		}

		boolean newDoc = doc.getId() == null;

		if (SearchUtils.isEmpty(doc.getRnDoc())) {
			GenTransact gt;
			try { // ще се генерира в отделна нишка защото в процедурите на постгрето няма вътрешно управление на транзакции
				gt = new GenTransact(doc);
				gt.start();
				gt.join();

			} catch (Exception e) {
				throw new DbErrorException("Системна грешка при генериране на регистрационен номер", e);
			}
			if (gt.ex != null) {
				if (gt.ex instanceof ObjectInUseException) {
					throw (ObjectInUseException) gt.ex;
				} else if (gt.ex instanceof DbErrorException) {
					throw (DbErrorException) gt.ex;
				}
				throw new DbErrorException(gt.ex); // някакво друго чудо е
			}
		}

		doc = save(doc, lice.getId()); // запис на документа, за да вземем ИД-то

		if (newDoc) { // смяна на статус и връзка само за при нов документ
			LiceDAO liceDao = new LiceDAO(getUser());

			lice.setStatus(CODE_ZNACHENIE_LICE_STATUS_PRAVO);
			lice.setUdostId(doc.getId());
			liceDao.save(lice);


			LiceDocDAO liceDocDao = new LiceDocDAO(getUser());
			LiceDoc liceDoc = new LiceDoc(lice.getId(), doc.getId());
			liceDocDao.save(liceDoc);
		}
	}

	/**
	 * Генериране на регистров номер на УД
	 *
	 * @param doc
	 * @throws ObjectInUseException
	 */
	void genRnDocUdost(Doc doc) throws ObjectInUseException {
		try {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(doc.getDocDate());

			int yyyy = gc.get(Calendar.YEAR);

			StoredProcedureQuery storedProcedure = getEntityManager().createStoredProcedureQuery("gen_nom_udost") //
					.registerStoredProcedureParameter(0, Integer.class, ParameterMode.IN) //
					.registerStoredProcedureParameter(1, String.class, ParameterMode.OUT); //

			storedProcedure.setParameter(0, yyyy);

			storedProcedure.execute();

			doc.setRnDoc((String) storedProcedure.getOutputParameterValue(1));

		} catch (Exception e) {
			throw new ObjectInUseException("Не може да бъде генериран регистрационен номер. Грешка: " + e.getMessage());
		}
	}


	/**
	 * По идентификатор на документ връща рег. номер / дата на документа
	 *
	 * @param docId
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public String decodeRnDateDoc(Integer docId) throws DbErrorException {

		if (docId == null) {
			return null;
		}

		List<Object[]> result;
		String rnDateDoc = "";
		try {

			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT doc_id ,rn_doc , TO_CHAR(doc_date, 'DD-MM-YYYY')  FROM doc  WHERE doc_id =:docId ");


			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()).setParameter("docId", docId);


			result = query.getResultList();

			if (!result.isEmpty()) {
				Object[] docData = result.get(0);

				rnDateDoc = (String) docData[1];

				if (docData[2] != null) {
					rnDateDoc += " / " + docData[2];
				}

			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане на данни за документ decodeRnDateDoc", e);
		}

		return rnDateDoc;

	}

	/**
	 * По идентификатор на документ  проверява дали има прикачени файлове към него
	 *
	 * @param docId
	 * @throws DbErrorException
	 */
	public boolean checkDocForFile(Integer docId) throws DbErrorException {

		if (docId == null) {
			return true;  // za da ne pozwolqwa
		}

		Query query = createNativeQuery("select count (*) as cnt from FILE_OBJECTS fo where fo.object_id = :objectId and fo.object_code = :objectCode") //
				.setParameter("objectId", docId) //
				.setParameter("objectCode", UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC); //

		int count = ((Number) query.getResultList().get(0)).intValue();

		if (count > 0) {
			return true;
		}
		return false;

	}

	/**
	 * По идентификатор и предназначение на документ връща броят прикачени файлове  към него
	 *
	 * @param docId
	 * @param purpose
	 * @throws DbErrorException
	 */
	public Integer countDocForFile(Integer docId , Integer purpose) throws DbErrorException {

		if (docId == null) {
			return -1;  // za da ne pozwolqwa
		}
		String sql ="select count (*) as cnt from FILE_OBJECTS fo where fo.object_id = :docId and fo.object_code = :objectCode ";

		if(purpose!=null){
			sql += " and fo.file_purpose = :purpose ";
		}

		Query query = createNativeQuery(sql) //
				.setParameter("docId", docId) //
				.setParameter("objectCode", UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC); //

		if(purpose!=null){
			query.setParameter("purpose", purpose);
		}

		return ((Number) query.getResultList().get(0)).intValue();



	}

	/**
	 * Връща боят документи по вид и лице
	 *
	 * @param vid
	 * @param liceId
	 * @throws DbErrorException
	 */
	public Integer countDocByVidAndLice(Integer vid , Integer liceId) throws DbErrorException {

		if (vid == null || liceId == null) {
			return -1;  // za da ne pozwolqwa
		}
		String sql ="SELECT count(1) FROM lice_doc ld join doc d on ld.doc_id = d.doc_id and ld.lice_id = :liceId WHERE d.doc_vid = :vid";

		Query query = createNativeQuery(sql) //
				.setParameter("liceId", liceId) //
				.setParameter("vid", vid); //

		return ((Number) query.getResultList().get(0)).intValue();



	}


	/**
	 * Връща списък документи по вид и лице
	 *
	 * @param liceId
	 * @throws DbErrorException
	 */
	public ArrayList<Object[]> listDublikatByLice( Integer liceId) throws DbErrorException {

		if ( liceId == null) {
			return null;  // za da ne pozwolqwa
		}
		String sql ="SELECT d.doc_id , d.rn_doc ,d.doc_date ,d.doc_info FROM lice_doc ld join doc d on ld.doc_id = d.doc_id and ld.lice_id = :liceId WHERE d.doc_vid = :vid order by d.rn_doc desc";

		Query query = createNativeQuery(sql) //
				.setParameter("liceId", liceId) //
				.setParameter("vid", CODE_ZNACHENIE_DOC_VID_ZAIAV_DUBL_UDOST); //

		ArrayList<Object[]> result = (ArrayList<Object[]>) query.getResultList();
		return result;



	}



}
