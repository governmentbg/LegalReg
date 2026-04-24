package com.ib.urireg.udostDocs;

import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.urireg.db.dao.IzpitDAO;
import com.ib.urireg.db.dao.ReferentDAO;
import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.db.dto.Izpit;
import com.ib.urireg.db.dto.Lice;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.UserData;
import jakarta.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.ib.system.utils.ValidationUtils.isNotBlank;

/**
 * Всички методи трябва да връщат String при налична стойност
 * или null при липсваща стойност и да не приемат параметри.
 * Да се прихващат всички exceptions и вместо тях да връщат null.
 * <br/>
 * <b>Ако се преименува метод</b>,
 * трябва да се оправи в екрана за попълване на шаблони като се избере
 * бутонът за преименуване на метод и се смени старото име с ново.
 * Това ще оправи всички записани досега шаблони.
 *
 * @author n.kanev
 */
@SuppressWarnings("unused")
public class UdostDokumentMethods {

	private static final Logger LOGGER = LoggerFactory.getLogger(UdostDokumentMethods.class);
	private static final String DB_ERROR_MSG = "Грешка при работа с базата";

	private static final SimpleDateFormat DATE_FORMAT_DD_MM_YYYY = new SimpleDateFormat("dd.MM.yyyy");
	private static final SimpleDateFormat DATE_FORMAT_HH_MM = new SimpleDateFormat("HH:mm");

	public static final String EMPTY_METHOD_NAME = "getEmpty";

	private final Helpers helpers;
	private final UserData userData;
	private final SystemData systemData;
	private final Map<String, Object> additionalData;
	private final Date date;

	public UdostDokumentMethods(UserData userData, SystemData systemData, Map<String, Object> additionalData) {
		this.userData = userData;
		this.systemData = systemData;
		this.additionalData = additionalData;
		this.date = new Date();
		this.helpers = new Helpers(userData, systemData);
	}


	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


	/* ! Всички методи да са public, да връщат String или null и да не приемат параметри! */

	/**
	 * @return номер на документ rnDoc
	 */
	public String getRnDoc() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getRnDoc", UdostDocumentCreator.KEY_DOC_ID);
		}

		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
			Doc doc = this.helpers.getDocDao().findById(zapId);
			if (doc != null && doc.getRnDoc() != null) {
				return doc.getRnDoc();
			}
			else return null;
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	/**
	 * @return дата на документ docDate
	 */
	public String getDocDate() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getDocDate", UdostDocumentCreator.KEY_DOC_ID);
		}

		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
			Doc doc = this.helpers.getDocDao().findById(zapId);
			if (doc != null && doc.getDocDate() != null) {
				return DATE_FORMAT_DD_MM_YYYY.format(doc.getDocDate());
			}
			else return null;
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	/**
	 * @return Имената на лице
	 */
	public String getLiceNames() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getLiceNames", UdostDocumentCreator.KEY_LICE_ID);
		}

		try {
			Integer liceId = (Integer)  this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID);
			Lice lice = this.helpers.getLiceDao().findById(liceId);

			if (isNotBlank(lice.getNames())) {
				return lice.getNames();
			}
			else return null;
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	/**
	 * @return Рождена дата на лице
	 */
	public String getLiceBirthDate() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getLiceBirthDate", UdostDocumentCreator.KEY_LICE_ID);
		}

		try {
			Integer liceId = (Integer)  this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID);
			Lice lice = this.helpers.getLiceDao().findById(liceId);

			if (lice != null && lice.getBirthDate() != null) {
				return DATE_FORMAT_DD_MM_YYYY.format(lice.getBirthDate());
			}
			else return null;
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	/**
	 * @return Място на раждане на лице
	 */
	public String getLiceBirthPlace() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getLiceBirthPlace", UdostDocumentCreator.KEY_LICE_ID);
		}

		try {
			Integer liceId = (Integer)  this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID);
			Lice lice = this.helpers.getLiceDao().findById(liceId);

			if (isNotBlank(lice.getBirthPlace())) {
				return lice.getBirthPlace();
			}
			else return null;
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	/**
	 * @return стринга "ЕГН" или "ЛНЧ" в зависимост от това лицето какво притежава
	 */
	public String getLiceEgnLncLiteral() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getLiceEgnLncLiteral", UdostDocumentCreator.KEY_LICE_ID);
		}

		try {
			Integer liceId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID);
			Lice lice = this.helpers.getLiceDao().findById(liceId);

			if (isNotBlank(lice.getEgn())) {
				return "ЕГН";
			}
			else if(isNotBlank(lice.getLnc())) {
				return "ЛНЧ";
			}
			else return null;
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	/**
	 * @return ЕГН или ЛНЧ на лицето
	 */
	public String getLiceEgnLnc() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getLiceEgnLnc", UdostDocumentCreator.KEY_LICE_ID);
		}

		try {
			Integer liceId = (Integer)  this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID);
			Lice lice = this.helpers.getLiceDao().findById(liceId);

			if (isNotBlank(lice.getEgn())) {
				return lice.getEgn();
			}
			else if(isNotBlank(lice.getLnc())) {
				return lice.getLnc();
			}
			else return null;
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	/**
	 * @return датата на протокола от казус на лицето
	 */
	public String getLiceProtokolDate() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getLiceProtokolDate", UdostDocumentCreator.KEY_LICE_ID);
		}

		try {
			Integer liceId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID);
			Lice lice = this.helpers.getLiceDao().findById(liceId);

			String sql =
					"select doc.doc_date " +
					"from lice " +
					"inner join izpit_result izrez on izrez.lice_id = lice.lice_id " +
					"inner join izpit iz on iz.izpit_id = izrez.izpit_id " +
					"inner join doc on doc_id = iz.case_prot_id " +
					"where izrez.case_result = :result " +
					"and lice.lice_id = :liceId";
			Object o = JPA.getUtil().getEntityManager().createNativeQuery(sql)
					.setParameter("liceId", liceId)
					.setParameter("result", UriregConstants.CODE_ZNACHENIE_IZPIT_RESULT_PASSED)
					.getSingleResult();

			if(o != null) {
				Timestamp timestamp = (Timestamp) o;
				return DATE_FORMAT_DD_MM_YYYY.format(timestamp);
			}
			else return null;
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		catch(NoResultException e) {
			return null;
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	/**
	 * @return Член - основание, за придобиване на юридическа правоспособност от системната настройка
	 */
	public String getChlenUdostPravosp() {
		try {
			return this.systemData.getSettingsValue("urireg.chlenOsnPravo");
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}

		return null;
	}

	public String getDokPredsedatel() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getDokPredsedatel", UdostDocumentCreator.KEY_DOC_ID);
		}

		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
			String sql = "select predsedatel from doc pr"
					+ " inner join izpit on izpit.case_prot_id = pr.doc_id"
					+ " where izpit.zap_izp_id = :docId";

			String predsedatel = (String) JPA.getUtil().getEntityManager().createNativeQuery(sql)
					.setParameter("docId", zapId)
					.getSingleResult();

			if (isNotBlank(predsedatel)) {
				return predsedatel;
			}
			else return null;
		}
		catch(NoResultException e) {
			return null;
		}
		catch(Exception e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	public String getDokMembers() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getDokMembers", UdostDocumentCreator.KEY_DOC_ID);
		}

		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
			String sql = "select members from doc pr"
					+ " inner join izpit on izpit.case_prot_id = pr.doc_id"
					+ " where izpit.zap_izp_id = :docId";

			String members = (String) JPA.getUtil().getEntityManager().createNativeQuery(sql)
					.setParameter("docId", zapId)
					.getSingleResult();

			if (isNotBlank(members)) {
				return members;
			}
			else return null;
		}
		catch(NoResultException e) {
			return null;
		}
		catch(Exception e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	public String getLicaIzpitTableData() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getLicaIzpitTableData", UdostDocumentCreator.KEY_DOC_ID);
		}

		List<String> tableData = new ArrayList<>();

		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

			String sql = "select firstname, lastname, rn_doc, doc_date"
					+ " from lice"
					+ " inner join izpit_result on izpit_result.lice_id = lice.lice_id"
					+ " inner join izpit on izpit.izpit_id = izpit_result.izpit_id"
					+ " inner join staj on staj.staj_id = izpit_result.staj_id"
					+ " inner join doc on doc.doc_id = staj.zaiav_izp_id"
					+ " where izpit.zap_izp_id = :docId"
					+ " order by firstname, lastname";

			List<Object[]> data = JPA.getUtil().getEntityManager().createNativeQuery(sql)
					.setParameter("docId", zapId)
					.getResultList();

			for (int i = 0; i < data.size(); i++) {

				Object[] row = data.get(i);
				List<String> currentLice = new ArrayList<>();

				String firstName = (String) row[0];
				String lastName = (String) row[1];
				String zaiavlenie = String.format("%s/%s",
						row[2],
						new SimpleDateFormat("dd.MM.yyyy").format((Date) row[3]));

				currentLice.add(String.valueOf(i + 1));
				currentLice.add(firstName);
				currentLice.add(lastName);
				currentLice.add(zaiavlenie);

				tableData.add(String.join(UdostDocumentCreator.LIST_DELIMITER, currentLice));
			}
		}
		catch(Exception e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return tableData.isEmpty() ? null : String.join(UdostDocumentCreator.TABLE_ROW_DELIMITER, tableData);
	}

	public String getDateUp() {
		if(this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID) == null) {
			throw new UdostDocExceptions.MissingDataException("getDateUp", UdostDocumentCreator.KEY_DOC_ID);
		}
		if(this.additionalData.get(UdostDocumentCreator.KEY_DUBLIKAT) == null) {
			throw new UdostDocExceptions.MissingDataException("getDateUp", UdostDocumentCreator.KEY_DUBLIKAT);
		}


		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
			Boolean isDublikat = (Boolean) this.additionalData.get(UdostDocumentCreator.KEY_DUBLIKAT);

			if(isDublikat != null && isDublikat) {
				return DATE_FORMAT_DD_MM_YYYY.format(new Date());
			}
			else {
				Doc doc = this.helpers.getDocDao().findById(zapId);
				if (doc != null && doc.getDocDate() != null) {
					return DATE_FORMAT_DD_MM_YYYY.format(doc.getDocDate());
				} else return null;
			}
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	public String getMinistar() {
		try {
			ReferentDAO refDao = new ReferentDAO(this.userData);
			List<Object[]> data = refDao.selectRefDataByPosition(UriregConstants.CODE_ZNACHENIE_DLAJN_MINISTAR);

			if (data.isEmpty()) {
				return null;
			} else {
				Object[] ministar = data.get(0);
				if (isNotBlank((String) ministar[1])) {
					return (String) ministar[1];
				}
			}
		}
		catch(DbErrorException e) {
			this.helpers.handleException(e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	public String getIzpitRegTime() {
		final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

		if(docId == null) {
			throw new UdostDocExceptions.MissingDataException("getIzpitRegTime", UdostDocumentCreator.KEY_DOC_ID);
		}

		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
			Izpit izpit = new IzpitDAO(this.userData).findByZapIzpId(zapId);

			LocalTime time = izpit.getTestDate().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalTime();
			LocalTime newTime = time.minusMinutes(90);

			String result = DateTimeFormatter.ofPattern("HH:mm").format(newTime);
			return result;
		}
		catch(Exception e) {
			LOGGER.error("Грешка при попълване на заповед за изпит doc_id=" + docId, e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	public String getIzpitRegEnd() {
		final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

		if(docId == null) {
			throw new UdostDocExceptions.MissingDataException("getIzpitRegEnd", UdostDocumentCreator.KEY_DOC_ID);
		}

		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
			Izpit izpit = new IzpitDAO(this.userData).findByZapIzpId(zapId);

			LocalTime time = izpit.getTestDate().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalTime();
			LocalTime newTime = time.minusMinutes(15);

			String result = DateTimeFormatter.ofPattern("HH:mm").format(newTime);
			return result;
		}
		catch(Exception e) {
			LOGGER.error("Грешка при попълване на заповед за изпит doc_id=" + docId, e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	public String getKazusRegTime() {
		final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

		if(docId == null) {
			throw new UdostDocExceptions.MissingDataException("getKazusRegTime", UdostDocumentCreator.KEY_DOC_ID);
		}

		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
			Izpit izpit = new IzpitDAO(this.userData).findByZapIzpId(zapId);

			LocalTime time = izpit.getCaseDate().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalTime();
			LocalTime newTime = time.minusMinutes(90);

			String result = DateTimeFormatter.ofPattern("HH:mm").format(newTime);
			return result;
		}
		catch(Exception e) {
			LOGGER.error("Грешка при попълване на заповед за изпит doc_id=" + docId, e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	public String getKazusRegEnd() {
		final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

		if(docId == null) {
			throw new UdostDocExceptions.MissingDataException("getKazusRegEnd", UdostDocumentCreator.KEY_DOC_ID);
		}

		try {
			Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
			Izpit izpit = new IzpitDAO(this.userData).findByZapIzpId(zapId);

			LocalTime time = izpit.getCaseDate().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalTime();
			LocalTime newTime = time.minusMinutes(15);

			String result = DateTimeFormatter.ofPattern("HH:mm").format(newTime);
			return result;
		}
		catch(Exception e) {
			LOGGER.error("Грешка при попълване на заповед за изпит doc_id=" + docId, e);
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		return null;
	}

	/**
	 * Връща празен стринг; за тестване
	 * @return ""
	 */
	public String getEmpty() {
		return "";
	}

}
