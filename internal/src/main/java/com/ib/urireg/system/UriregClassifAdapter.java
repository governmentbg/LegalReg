package com.ib.urireg.system;

import com.ib.indexui.system.Constants;
import com.ib.system.BaseSystemData;
import com.ib.system.SysClassifAdapter;
import com.ib.system.SysConstants;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static com.ib.indexui.system.Constants.*;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.trimToNULL;

/**
 * Конкретния адаптер за динамични класификации
 *
 * @author belev
 */
public class UriregClassifAdapter extends SysClassifAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(UriregClassifAdapter.class);

	/** индекс от спецификите, в които стои код на Община. String ! */
	public static final int	EKATTE_INDEX_OBSTINA	= 1;
	/** индекс от спецификите, в които стои кода на Област. String ! */
	public static final int	EKATTE_INDEX_OBLAST	    = 2;

	/** индекс от спецификите, в които стои ID на регистратура. Integer ! */
	public static final int	USERS_INDEX_REGISTRATURA	= 0;
	/** индекс от спецификите, в които стои флаг за деловодител (ДА(1)/НЕ(2)). Integer ! */
	public static final int	USERS_INDEX_DELOVODITEL		= 1;
	/** индекс от спецификите, в които стои флаг за Дублиране на съобщения по e-mail (ДА(1)/НЕ(2)). Integer ! */
	public static final int	USERS_INDEX_DUBL_MAIL		= 2;
	/** индекс от спецификите, в които стои флаг за Статус). Integer ! */
	public static final int	USERS_INDEX_STATUS		    = 3;

	/** индекс от спецификите, в които стои вид на участник (служител/звено). Integer ! */
	public static final int	ADM_STRUCT_INDEX_REF_TYPE		= 0;
	/** индекс от спецификите, в които стои ID на регистратура. Integer ! */
	public static final int	ADM_STRUCT_INDEX_REGISTRATURA	= 1;
	/** индекс от спецификите, в които стои имейл за контакт. String ! */
	public static final int	ADM_STRUCT_INDEX_CONTACT_EMAIL	= 2;
	/** индекс от спецификите, в които стои длъжност. Integer ! */
	public static final int	ADM_STRUCT_INDEX_POSITION		= 3;

	/** индекс от спецификите, в които стои вид на участник (фзл/нфл). Integer ! */
	public static final int	REFERENTS_INDEX_REF_TYPE		= 0;
	/** индекс от спецификите, в които стои имейл за контакт. String ! */
	public static final int	REFERENTS_INDEX_CONTACT_EMAIL	= 1;
	/** индекс от спецификите, в които стои ЕИК/ЕГН в зависимост от вида. String ! */
	public static final int	REFERENTS_INDEX_EIK_EGN			= 2;

	/** индекс от спецификите, в които стои регистратурата за групата. Integer! */
	public static final int	REG_GROUP_INDEX_REGISTRATURA	= 0;
	/** индекс от спецификите, в които стоят списъка от кодове на членове разделени със запетая. String ! */
	public static final int	REG_GROUP_INDEX_MEMBERS			= 1;

	UriregClassifAdapter(BaseSystemData sd) {
		super(sd);
	}

	/**
	 * Административна структура -specifics:<br>
	 * [0]=REF_TYPE<br>
	 * [1]=REF_REGISTRATURA<br>
	 * [2]=CONTACT_EMAIL<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassAdmStruct(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassAdmStruct(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();

		try {
			// селекта за мигрираните и затворени значения
			String migSql = " select r.REF_ID, r.CODE, r.CODE_PREV, r.CODE_PARENT, r.LEVEL_NUMBER, r.REF_NAME, r.DATE_OT, r.DATE_DO, r.DATE_REG, r.DATE_LAST_MOD " //
				+ " from ADM_REFERENTS r where r.CODE_CLASSIF = :codeClassif and r.REF_TYPE = :migRefType ";

			@SuppressWarnings("unchecked")
			Stream<Object[]> migRows = JPA.getUtil().getEntityManager().createNativeQuery(migSql) //
				.setParameter("codeClassif", codeClassif) //
				.setParameter("migRefType", UriregConstants.CODE_ZNACHENIE_REF_TYPE_MIG) //
				.setHint(HibernateHints.HINT_FETCH_SIZE, 5000) //
				.getResultStream();

			Iterator<Object[]> migIter = migRows.iterator();
			while (migIter.hasNext()) {
				Object[] row = migIter.next();

				SystemClassif item = new SystemClassif();

				item.setCodeClassif(codeClassif);

				item.setId(((Number) row[0]).intValue());
				item.setCode(((Number) row[1]).intValue());
				item.setCodePrev(((Number) row[2]).intValue());
				item.setCodeParent(((Number) row[3]).intValue());
				item.setLevelNumber(((Number) row[4]).intValue());

				item.setTekst((String) row[5]);
				item.setDateOt((Date) row[6]);
				item.setDateDo((Date) row[7]);

				item.setDateReg((Date) row[8]);
				item.setDateLastMod((Date) row[9]);

				// !НЕ ТРЯБВА ДА ИМА СПЕЦИФИКИ, ДОРИ И ПРАЗНИ!
				// Това е единственият начин да се разбере, че даните са от миграция, след като влезе в кеша!

				classif.add(item);
			}

			// селекта за първо ниво
			String level1sql = " select r.REF_ID, r.CODE, r.CODE_PARENT, r.REF_NAME, r.DATE_OT, r.DATE_DO "
				+ " , r.REF_TYPE, r.REF_REGISTRATURA, r.CONTACT_EMAIL, r.EMPL_POSITION, NULL ZVENO_NAME, r.DATE_REG, r.DATE_LAST_MOD, r.CODE_PREV " //
				+ " from ADM_REFERENTS r where r.CODE_CLASSIF = :codeClassif and r.CODE_PARENT = 0 and r.REF_TYPE != :migRefType ";

			@SuppressWarnings("unchecked")
			List<Object[]> level1rows = JPA.getUtil().getEntityManager().createNativeQuery(level1sql) //
				.setParameter("codeClassif", codeClassif) //
				.setParameter("migRefType", UriregConstants.CODE_ZNACHENIE_REF_TYPE_MIG) //
				.getResultList();

			// селекта за следващите нива, където трябват данни за родителя
			String sql = " select t.* from ( select r.REF_ID, r.CODE, r.CODE_PARENT, r.REF_NAME " //
				+ " , r.DATE_OT DATE_OT " //
				+ " , r.DATE_DO DATE_DO " //
				+ " , r.REF_TYPE, r.REF_REGISTRATURA, r.CONTACT_EMAIL, r.EMPL_POSITION, p.REF_NAME ZVENO_NAME, r.DATE_REG, r.DATE_LAST_MOD, r.CODE_PREV, p.REF_ID KEY_ID " //
				+ " from ADM_REFERENTS p " //
				+ " inner join ADM_REFERENTS r on r.CODE_PARENT = p.CODE " //
				+ " where p.CODE_CLASSIF = :codeClassif and p.REF_TYPE != :migRefType and r.REF_TYPE != :migRefType) t where t.DATE_OT < t.DATE_DO "
				+ " or (t.DATE_OT = t.DATE_DO and t.REF_TYPE = 2) "; // допълвам и служители с равни дати от-до

			@SuppressWarnings("unchecked")
			Stream<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql) //
				.setParameter("codeClassif", codeClassif) //
				.setParameter("migRefType", UriregConstants.CODE_ZNACHENIE_REF_TYPE_MIG) //
				.setHint(HibernateHints.HINT_FETCH_SIZE, 500) //
				.getResultStream();

			Map<Integer, List<Object[]>> map = new HashMap<>(); // в този мап ще има всички данни от БД, за да може рекурсивно да
																// се направи дървото и то да е правилно
			Iterator<Object[]> iter = rows.iterator();
			while (iter.hasNext()) {
				Object[] row = iter.next();

				Integer key = asInteger(row[14]);

				List<Object[]> temp = map.get(key);
				if (temp == null) {
					temp = new ArrayList<>();
					map.put(key, temp);
				}
				temp.add(row);
			}

			boolean appendPostion = "1".equals(getSd().getSettingsValue("system.admStructEmplPosition")); // ще се лепи ли
																											// длъжностат към
																											// името на човека

			boolean appendZveno = "1".equals(getSd().getSettingsValue("delo.admStructEmplZveno"));

			List<SystemClassif> oneDayEmpl = new ArrayList<>();
			Set<String> added = new HashSet<>();
			for (Object[] row : level1rows) { // рекурсивно трябва цялото дърво да се зареди, като се започва от данните на първо
												// ниво
				SystemClassif item = new SystemClassif();

				item.setCodeClassif(codeClassif);
				item.setLevelNumber(1);

				setAdmStructItemData(row, item, null, appendPostion, appendZveno); // сетвам специфичните данни на елемента

				createAdmStructTree(classif, item, map, added, appendPostion, appendZveno, oneDayEmpl); // рекусрсивно за подчинените
			}

			// иска се и служители с един ден в админ структурата да присъстват за разкодиране
			for (SystemClassif empl : oneDayEmpl) {
				String key = empl.getCode() + "_" + empl.getDateOt().getTime();
				if (!added.add(key)) { // това гарантира че няма да има за тази датаот с друга датадо
					continue;
				}
				classif.add(empl);
			}

		} catch (DbErrorException e) {
			throw e;
		} catch (Exception e) {
			throw new DbErrorException(e);
		}

		// това до края има за цел да намали обема на данните в H2
		Collections.sort(classif, new Comparator<SystemClassif>() {
			@Override
			public int compare(SystemClassif o1, SystemClassif o2) {
				if(o1.getCode() != o2.getCode()) {
					return o1.getCode() - o2.getCode();
				}
				return o1.getDateOt().compareTo(o2.getDateOt());
			}
		});

		int i = 0;
		while(i < classif.size()-1) {
			SystemClassif item = classif.get(i);
			SystemClassif next = classif.get(i+1);

			if (item.getId().equals(next.getId()) && item.getLevelNumber() == next.getLevelNumber()
				&& Objects.equals(item.getCodeExt(), next.getCodeExt()) && Objects.equals(item.getDopInfo(), next.getDopInfo())
				&& item.getDateDo().getTime() == next.getDateOt().getTime()) {

				item.setDateDo(next.getDateDo()); // намалявам размножаването
				classif.remove(i+1);
			} else {
				i++;
			}
		}
		LOGGER.info("AdmStruct H2 rows={}", classif.size());

		return classif;
	}

	/**
	 * Административна структура за справки (+напуснали)
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassAdmStructReports(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassAdmStructReports(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();

		Date today = DateUtils.startDate(new Date());
		Date systemMinDate = DateUtils.systemMinDate();

		List<SystemClassif> items = buildClassAdmStruct(Constants.CODE_CLASSIF_ADMIN_STR, lang);

		Set<Integer> actualCodes = new HashSet<>(); // към днешна дата

		Map<Integer, SystemClassif> closedItems = new HashMap<>(); // затворените, като накрая от тях ще останат само напусналите

		Map<Integer, Integer> level1prev = new HashMap<>(); // това ми трябва за да намеря след кой елемент ще се сложи звено
															// напуснали
		for (SystemClassif current : items) {
			if (current.getDateDo().getTime() <= today.getTime()) {

				if (current.getSpecifics() == null // това са мигрираните, които не ги знаем какви са и трябва да влезнат и те
					|| current.getSpecifics()[ADM_STRUCT_INDEX_REF_TYPE].equals(UriregConstants.CODE_ZNACHENIE_REF_TYPE_EMPL)) { // само
																																// служители
					SystemClassif temp = closedItems.get(current.getCode());

					if (temp == null || temp.getDateOt().getTime() < current.getDateOt().getTime()) {
						closedItems.put(current.getCode(), current); // последното им срещане в класификацията
					}
				}
				continue;
			}

			actualCodes.add(current.getCode());

			if (current.getLevelNumber() == 1) { // за момента напусналите ще се слагат на първо ниво на последно място
				level1prev.put(current.getCodePrev(), current.getCode());
			}

			SystemClassif item = new SystemClassif();

			item.setId(current.getId());
			item.setCode(current.getCode());
			item.setCodeParent(current.getCodeParent());
			item.setCodePrev(current.getCodePrev());
			item.setCodeClassif(codeClassif);

			item.setLevelNumber(current.getLevelNumber());
			item.setDateOt(today); // винаги от днешна дата
			item.setTekst(current.getTekst());
			item.setDopInfo(current.getDopInfo());
			item.setDateReg(systemMinDate);
			item.setCodeExt(current.getCodeExt());

			classif.add(item); // тук добавям актуален
		}

		List<SystemClassif> napusnali = new ArrayList<>();
		for (Entry<Integer, SystemClassif> entry : closedItems.entrySet()) {
			if (!actualCodes.contains(entry.getKey())) {
				napusnali.add(entry.getValue());
				actualCodes.add(entry.getKey()); // този ще се добави и реално става актуален
			}
		}

		if (!napusnali.isEmpty()) { // може и да няма, защото това че нещо е затворено не значи че е напуснал
			SystemClassif zvenoNapusnali = new SystemClassif();

			zvenoNapusnali.setId(-1000);
			zvenoNapusnali.setCode(zvenoNapusnali.getId());
			zvenoNapusnali.setCodeParent(0);
			zvenoNapusnali.setCodeClassif(codeClassif);

			for (Integer x : level1prev.values()) {
				if (!level1prev.containsKey(x)) { // ако не се съдържа означава че е последния, защото никой не е казал този код
													// за предходен
					zvenoNapusnali.setCodePrev(x);
					break;
				}
			}
			zvenoNapusnali.setLevelNumber(1);

			zvenoNapusnali.setDateOt(today); // винаги от днешна дата
			zvenoNapusnali.setTekst("Напуснали");
			zvenoNapusnali.setDopInfo("");
			zvenoNapusnali.setDateReg(systemMinDate);

			classif.add(zvenoNapusnali); // корена на напусналите е напослесно място след останалите на първо ниво

			@SuppressWarnings("unchecked") // тука трябва да излезат само тези, които имат само един ред с равни дати !!!
			List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(
				"select distinct r.REF_ID, r.CODE, r.REF_NAME, r.DATE_DO, r.CODE_PARENT, r.EMPL_POSITION from ADM_REFERENTS r"
				+ " where r.CODE_CLASSIF = ?1 and r.REF_TYPE = ?2 and r.DATE_OT = r.DATE_DO"
				+ " and not exists (select v.REF_ID from ADM_REFERENTS v where v.CODE = r.CODE and v.REF_ID <> r.REF_ID)")
				.setParameter(1, Constants.CODE_CLASSIF_ADMIN_STR).setParameter(2, UriregConstants.CODE_ZNACHENIE_REF_TYPE_EMPL)
				.getResultList();

			for (Object[] row : rows) {
				int code = ((Number) row[1]).intValue();
				if (actualCodes.contains(code)) {
					continue; // това ще гарантира че няма да има никакво повтаряне на кодове
				}
				actualCodes.add(code);

				SystemClassif sc = new SystemClassif();
				sc.setId(((Number) row[0]).intValue());
				sc.setCode(code);
				sc.setTekst((String) row[2]);
				sc.setDateDo((Date) row[3]);

				if (row[4] != null) {
					int codeParent = ((Number) row[4]).intValue();

					sc.setDopInfo(getSd().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, codeParent, lang, sc.getDateDo()));
					Integer registratura = (Integer) getSd().getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, codeParent, lang, sc.getDateDo(), ADM_STRUCT_INDEX_REGISTRATURA);
					if (registratura != null) {
						sc.setCodeExt(registratura.toString());
					}
				}
				if (row[5] != null) {
					sc.setTekst(sc.getTekst() +" ("+ getSd().decodeItem(CODE_CLASSIF_POSITION, ((Number) row[5]).intValue(), lang, sc.getDateDo()) +")");
				}
				napusnali.add(sc);
			}

			napusnali.sort((sc1, sc2) -> sc1.getTekst().compareTo(sc2.getTekst())); // !!! сортиране по име

			int codePrev = 0;
			for (SystemClassif current : napusnali) {
				SystemClassif item = new SystemClassif();

				item.setId(current.getId());
				item.setCode(current.getCode());
				item.setCodeParent(zvenoNapusnali.getCode());
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(zvenoNapusnali.getLevelNumber() + 1);
				item.setDateOt(today); // винаги от днешна дата
				item.setTekst(current.getTekst() + " (до " + DateUtils.printDate(current.getDateDo()) + ")");
				item.setDopInfo(current.getDopInfo());
				item.setDateReg(systemMinDate);
				item.setCodeExt(current.getCodeExt());

				codePrev = item.getCode();

				classif.add(item);  // тук добавям напуснал
			}
		}

		return classif;
	}

	@Override
	public List<SystemClassif> buildClassEKATTE(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassEKATTE(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> list = new ArrayList<>();
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select att.EKATTE, att.TVM, att.IME, att.OBSTINA, att.OBSTINA_IME, att.OBLAST, att.OBLAST_IME, 3 as TIP, att.DATE_OT, att.DATE_DO ");
			sql.append(" from EKATTE_ATT att ");
			sql.append(" union all ");
			sql.append(" select 0, 'обл.', oblasti.IME, null, null, oblasti.OBLAST, oblasti.IME, 1 as TIP, oblasti.DATE_OT, oblasti.DATE_DO ");
			sql.append(" from EKATTE_OBLASTI oblasti ");
			sql.append(" union all ");
			sql.append(" select 0, 'общ.', obstini.IME, obstini.OBSTINA, obstini.IME, ");
			sql.append(DialectConstructor.convertSQLSubstring(JPA.getUtil().getDbVendorName(), "obstini.OBSTINA", 1, 3));
			sql.append(" , obstini.OBLAST_IME, 2 as TIP, obstini.DATE_OT, obstini.DATE_DO ");
			sql.append(" from EKATTE_OBSTINI obstini ");
			sql.append(" order by 8,2,3"); // !!! много важно

			// за тест и за бързодействие го правя на Stream като е доста важно да има QueryHints.HINT_FETCH_SIZE, за да се получи
			// реален ефект от цялата игра
			@SuppressWarnings("unchecked")
			Stream<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
					.setHint(QueryHints.HINT_FETCH_SIZE, 5000) //
					.getResultStream();

			int id = 0;
			int codeOblObst = 100000;

			Map<String, Integer> parentMap = new HashMap<>();
			Map<Integer, Integer> prevMap = new HashMap<>();

			Iterator<Object[]> iter = rows.iterator();
			while (iter.hasNext()) {
				Object[] row = iter.next();

				SystemClassif item = new SystemClassif();

				int tip = ((Number) row[7]).intValue();

				final int code;
				if (tip == 3) { // за населените места е кода по еката
					code = ((Number) row[0]).intValue();

				} else { // за областите и общините се генерира
					code = ++codeOblObst;
				}

				item.setId(++id);
				item.setCode(code);
				item.setCodeClassif(codeClassif);

				item.setDateOt((Date) row[8]);
				item.setDateDo((Date) row[9]);

				item.setTekst(row[1] + " " + row[2]);

				Integer codeParent = null;
				int levelNumber = 1;

				if (tip == 1) { // област

					item.setCodeExt((String) row[5]); // кода на областа

					parentMap.put(item.getCodeExt(), code);
					codeParent = 0;
					levelNumber = 1;

				} else if (tip == 2) { // за общините се казва коя е областа в dopInfo

					item.setCodeExt((String) row[3]); // кода на общината
					item.setDopInfo("обл. " + row[6]);

					parentMap.put(item.getCodeExt(), code);
					codeParent = parentMap.get(row[5]); // през кода на областта
					levelNumber = 2;

				} else if (tip == 3) { // за населените места всичко им се казва
					item.setDopInfo("общ. " + row[4] + ", обл. " + row[6]);

					codeParent = parentMap.get(row[3]); // през кода на общината
					levelNumber = 3;
				}

				// сет парент
				if (codeParent == null) {
					codeParent = 0; // няма причина да се случи, но все пак
				}
				item.setCodeParent(codeParent);

				// сет прев
				Integer codePrev = prevMap.get(codeParent);
				if (codePrev == null) {
					codePrev = 0; // първи път е така
				}
				item.setCodePrev(codePrev);
				prevMap.put(codeParent, item.getCode());

				// сет левел
				item.setLevelNumber(levelNumber);

				item.setSpecifics(new Object[] { tip, row[3], row[5] });

				list.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return list;
	}

	/**
	 * Динамична класификация за Регистратури. <br>
	 * specifics:<br>
	 * [0]=VALID<br>
	 * [1]=registerId1,registerId2,...,<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassRegistraturi(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassRegistraturi(codeClassif={},lang={})", codeClassif, lang);

		Date systemMinDate = DateUtils.systemMinDate();

		List<SystemClassif> classif = new ArrayList<>();

		SystemClassif item = new SystemClassif();

		item.setId(1);
		item.setCode(item.getId());
		item.setCodeParent(0);
		item.setCodePrev(0);
		item.setCodeClassif(codeClassif);

		item.setLevelNumber(1);

		item.setDateOt(systemMinDate);
		item.setTekst("Регистратура");
		item.setDopInfo("Организация");

		item.setDateReg(systemMinDate);

		Integer valid = SysConstants.CODE_ZNACHENIE_DA;
		String registers = "1";

		item.setSpecifics(new Object[] { valid, registers });

		classif.add(item);

		return classif;
	}

	/**
	 * Динамична класификация за Регистри. <br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassRegistri(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassRegistri(codeClassif={},lang={})", codeClassif, lang);

		Date systemMinDate = DateUtils.systemMinDate();

		List<SystemClassif> classif = new ArrayList<>();

		SystemClassif item = new SystemClassif();

		item.setId(1);
		item.setCode(item.getId());
		item.setCodeParent(0);
		item.setCodePrev(0);
		item.setCodeClassif(codeClassif);

		item.setLevelNumber(1);
		item.setDateOt(systemMinDate);
		item.setTekst("Регистър");

		item.setDateReg(systemMinDate);
		item.setDopInfo("Р");

		classif.add(item);

		return classif;
	}

	/**
	 * Допълнително като специфика се добавя и: <br>
	 * [0]=REF_REGISTRATURA<br>
	 */
	@Override
	public List<SystemClassif> buildClassUsers(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassUsers(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();

		try {
			Date date = new Date(); // за регистратура към момента
			Date systemMinDate = DateUtils.systemMinDate();

			StringBuilder sql = new StringBuilder();
			sql.append(" select distinct u.USER_ID, u.USERNAME, u.NAMES, u.DATE_REG, u.DATE_LAST_MOD, ur.CODE_ROLE DELOVODITEL, 2 DUBL_MAIL, z.REF_REGISTRATURA, u.STATUS ");
			sql.append(" from ADM_USERS u ");
			sql.append(" left outer join ADM_REFERENTS r on r.CODE = u.USER_ID and r.DATE_OT <= :nowParam and r.DATE_DO > :nowParam ");
			sql.append(" left outer join ADM_REFERENTS z on z.CODE = r.CODE_PARENT and z.DATE_OT <= :nowParam and z.DATE_DO > :nowParam ");
			sql.append(" left outer join ADM_USER_ROLES ur on ur.USER_ID = u.USER_ID and ur.CODE_CLASSIF = :businessRole and ur.CODE_ROLE = :delovoditel ");
			sql.append(" order by u.NAMES, u.USERNAME ");

			@SuppressWarnings("unchecked")
			List<Object[]> rez = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
				.setParameter("businessRole", CODE_CLASSIF_BUSINESS_ROLE).setParameter("delovoditel", UriregConstants.CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL) //
				.setParameter("nowParam", DateUtils.startDate(date))
				.getResultList();

			Set<Integer> codeSet = new HashSet<>();

			int codePrev = 0;
			for (Object[] row : rez) {
				SystemClassif item = new SystemClassif();

				item.setId(SearchUtils.asInteger(row[0]));
				item.setCode(SearchUtils.asInteger(row[0]));

				if (codeSet.contains(item.getCode())) {
					continue; // все пак има някаква вероятност да се размножи заради регистратурата и да не изгърмим
				}
				codeSet.add(item.getCode());

				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);

				item.setTekst(row[2] + " (" + row[1] + ")");

				item.setDateReg(SearchUtils.asDate(row[3]));
				item.setDateLastMod(SearchUtils.asDate(row[4]));

				codePrev = item.getCode();

				Object registraturaId = SearchUtils.asInteger(row[7]);
				if (registraturaId == null) {
					registraturaId = getSd().getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), lang, date, ADM_STRUCT_INDEX_REGISTRATURA);
				}

				Integer delovoditel = row[5] != null ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE;
				Integer dublMail = row[6] != null ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE;

				Integer status = SearchUtils.asInteger(row[8]);

				item.setSpecifics(new Object[] { registraturaId, delovoditel, dublMail, status });

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}

		return classif;
	}

	/**
	 * @param classif
	 * @param element
	 * @param map
	 * @param added
	 * @param appendPostion
	 * @param oneDayEmpl
	 * @throws DbErrorException
	 */
	void createAdmStructTree(List<SystemClassif> classif, SystemClassif element, Map<Integer, List<Object[]>> map, Set<String> added, boolean appendPostion, boolean appendZveno, List<SystemClassif> oneDayEmpl) throws DbErrorException {
		if (element.getDateOt().getTime() == element.getDateDo().getTime()) {
			oneDayEmpl.add(element); // тези накрая ще се добавят, за да не бъркат схемата. ще бъдат само за разкодиране.
			return;
		}

		String key = element.getCode() + "_" + element.getDateOt().getTime();
		if (!added.add(key)) {
			return;
		}

		classif.add(element);

		List<Object[]> rows = map.get(element.getId());
		if (rows == null) {
			return;
		}

		for (Object[] row : rows) {
			SystemClassif item = new SystemClassif();

			item.setCodeClassif(element.getCodeClassif());
			item.setLevelNumber(element.getLevelNumber() + 1);

			// ако подчинените са служители ще се изпозлва регистратурата, която идва от родителя
			setAdmStructItemData(row, item, (Integer) element.getSpecifics()[ADM_STRUCT_INDEX_REGISTRATURA], appendPostion, appendZveno);

			if (item.getDateOt().getTime() < element.getDateOt().getTime()) {
				item.setDateOt(element.getDateOt());
			}
			if (item.getDateDo().getTime() > element.getDateDo().getTime()) {
				item.setDateDo(element.getDateDo());
			}

			boolean cycle;
			if (item.getDateOt().getTime() < item.getDateDo().getTime()) {
				cycle = true;

			} else if (item.getDateOt().getTime() == item.getDateDo().getTime()
				&& ((Integer)item.getSpecifics()[ADM_STRUCT_INDEX_REF_TYPE]).intValue() == UriregConstants.CODE_ZNACHENIE_REF_TYPE_EMPL) {
				cycle = true; // служители с един ден само за разкодиране

			} else {
				cycle = false;
			}

			if (cycle) {
				createAdmStructTree(classif, item, map, added, appendPostion, appendZveno, oneDayEmpl);
			}
		}
	}

	/**
	 * @param classif
	 * @param parent
	 * @param sql
	 * @param guid
	 * @param parents
	 */
	void createEgovOrgsTree(List<SystemClassif> classif, SystemClassif parent, String sql, String guid, List<String> parents) {
		classif.add(parent);

		if (guid == null || !parents.contains(guid)) {
			return;
		}

		@SuppressWarnings("unchecked") // за всички подчинени на това звено
		List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql) //
			.setParameter(1, guid) //
			.getResultList();

		int codePrev = 0;
		for (Object[] row : rows) {
			SystemClassif item = new SystemClassif();

			String eik = trimToNULL((String) row[1]);

			item.setId(asInteger(row[0]));
			item.setCode(item.getId());
			item.setCodeParent(parent.getCode());
			item.setCodePrev(codePrev);
			item.setCodeClassif(parent.getCodeClassif());

			item.setLevelNumber(parent.getLevelNumber() + 1);
			item.setDateOt(parent.getDateOt());
			item.setTekst(row[2] + " (" + eik + ")");

			codePrev = item.getCode();

			item.setDateReg((Date) row[5]);

			item.setSpecifics(new Object[] { eik, row[3], row[2] });

			createEgovOrgsTree(classif, item, sql, (String) row[3], parents);
		}
	}

	private SystemClassif setAdmStructItemData(Object[] row, SystemClassif item, Integer registratura, boolean appendPostion, boolean appendZveno) throws DbErrorException {
		// r.REF_ID(0), r.CODE(1), r.CODE_PARENT(2), r.REF_NAME(3), r.DATE_OT(4), r.DATE_DO(5), r.REF_TYPE(6),
		// r.REF_REGISTRATURA(7), r.CONTACT_EMAIL(8), r.EMPL_POSITION(9), p.REF_NAME ZVENO_NAME(10), r.DATE_REG(11),
		// r.DATE_LAST_MOD(12)

		item.setCode(asInteger(row[1]));
		item.setCodePrev(asInteger(row[13]));

		item.setId(asInteger(row[0]));
		item.setCodeParent(asInteger(row[2]));
		item.setTekst((String) row[3]);
		item.setDateOt((Date) row[4]);
		item.setDateDo((Date) row[5]);

		Integer refType = asInteger(row[6]);
		String email = trimToNULL((String) row[8]);

		Integer position = null;
		item.setDopInfo(trimToNULL((String) row[10])); // горестоящото звено

		if (Objects.equals(refType, UriregConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO)) {
			registratura = asInteger(row[7]); // за звеното си се взима каквато е на ред, а за служителя каквато дойде от горе
		} else {

			if (row[9] != null) { // има длъжност
				position = ((Number) row[9]).intValue();

				String positionName = getSd().decodeItem(CODE_CLASSIF_POSITION, position, CODE_DEFAULT_LANG, item.getDateOt());

				if (appendPostion) { // трябва да се сложи длъжността след името в скоби

					if (appendZveno && item.getDopInfo() != null) { // иска се и звеното да е там
						item.setTekst(item.getTekst() + " (" + positionName + ", " + item.getDopInfo() + ")");

					} else { // остава да е само длъжността
						item.setTekst(item.getTekst() + " (" + positionName + ")");
					}

				} else { // не се иска длъжност
					if (appendZveno && item.getDopInfo() != null) { // но се иска да се добави звеното
						item.setTekst(item.getTekst() + " (" + item.getDopInfo() + ")");
					}
				}
			} else { // няма въведена длъжност
				if (appendZveno && item.getDopInfo() != null) { // но се иска да се добави звеното
					item.setTekst(item.getTekst() + " (" + item.getDopInfo() + ")");
				}
			}
		}

		item.setDateReg((Date) row[11]);
		item.setDateLastMod((Date) row[12]);

		item.setSpecifics(new Object[] { refType, registratura, email, position });

		item.setCodeExt(registratura+"");

		return item;
	}

	private SystemClassif setReferentItemData(Object[] row, SystemClassif item, Integer lang, Date date, int countryBg) throws DbErrorException {
		// r.REF_ID(0), r.CODE(1), r.CODE_PARENT(2), r.REF_NAME(3), r.DATE_OT(4), r.DATE_DO(5), r.REF_TYPE(6),
		// r.NFL_EIK(7), r.FZL_EGN(8), r.FZL_LNC(9), r.CONTACT_EMAIL(10), a.ADDR_COUNTRY(11),
		// a.ADDR_TEXT(12), a.EKATTE(13), r.DATE_REG(14), r.DATE_LAST_MOD(15)

		item.setId(asInteger(row[0]));
		item.setCode(asInteger(row[1]));
		item.setTekst((String) row[3]);
		item.setDateOt((Date) row[4]);
		item.setDateDo((Date) row[5]);

		Integer refType = asInteger(row[6]);

		boolean eik = false;
		boolean egn = false;
		String number = trimToNULL((String) row[7]); // ЕИК
		if (number == null) {
			number = trimToNULL((String) row[8]); // ЕГН
			if (number == null) {
				number = trimToNULL((String) row[9]); // ЛНЧ
			} else {
				egn = true;
			}
		} else {
			eik = true;
		}

		if (number != null && eik) { // само за ЕИК-то след името
			item.setTekst(item.getTekst() + " (" + number + ")");
		}

		String email = trimToNULL((String) row[10]);

		item.setDateReg((Date) row[14]);
		item.setDateLastMod((Date) row[15]);

		item.setCodeExt(number);

		item.setSpecifics(new Object[] { refType, email, number });

		// данните за адреса, които ще се сложат в допълнителната информация
		Integer country = asInteger(row[11]);
		String address = trimToNULL((String) row[12]);
		Integer ekatte = asInteger(row[13]);

		StringBuilder dopInfo = new StringBuilder(); // тука слагам адреса събран с екатте и държава ако не е БГ

		if (number != null && !eik) { // ЕИК/ЕГН
			if (egn) {
				dopInfo.append("ЕГН ");
			} else {
				dopInfo.append("ЛНЧ ");
			}
			dopInfo.append(number);
		}

		if (ekatte != null) { // ако има ЕКАТТЕ значи е БГ и не се занимавам с държавите
			String location = getSd().decodeItem(CODE_CLASSIF_EKATTE, ekatte, lang, date);
			if (location != null) {
				if (dopInfo.length() > 0) {
					dopInfo.append(", "); // за да се раздели от ЕИК/ЕГН
				}
				dopInfo.append(location);
			}

		} else if (country != null && !country.equals(countryBg)) { // само ако има и то да е чужда
																	// държава
			String countryText = getSd().decodeItem(CODE_CLASSIF_COUNTRIES, country, lang, date);
			if (countryText != null) {
				if (dopInfo.length() > 0) {
					dopInfo.append(", "); // за да се раздели от ЕИК/ЕГН
				}
				dopInfo.append(countryText);
			}
		}
		if (address != null) {
			if (dopInfo.length() > 0) {
				dopInfo.append(", "); // за да се раздели от реални адрес
			}
			dopInfo.append(address);
		}
		if (dopInfo.length() > 0) { // все пак може и да няма нищо изчислено като адрес
			item.setDopInfo(dopInfo.toString());
		}

		return item;
	}
}
