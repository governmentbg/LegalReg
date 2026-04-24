package com.ib.urireg.utils;

import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.DateUtils;
import jakarta.persistence.Query;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author belev
 */
public class EkatteLoader {

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(EkatteLoader.class);

	private static final String	EK_OBL_XLSX		= "EK_OBL.XLSX";
	private static final String	EK_OBST_XLSX	= "EK_OBST.XLSX";
	private static final String	EK_ATTE_XLSX	= "EK_ATTE.XLSX";

//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		EkatteLoader loader = new EkatteLoader();
//
//		try {
//			JPA.getUtil().begin();
//			loader.load(new File("D:/Ekatte_xlsx.zip"), DateUtils.parse("01.01.2000"));
//			JPA.getUtil().commit();
//
//		} catch (Exception e) {
//			JPA.getUtil().rollback();
//			LOGGER.error("", e);
//		} finally {
//			JPA.getUtil().closeConnection();
//		}
//
//	}

	/**
	 * Зарежда подадения файл <b>Ekatte_xlsx.zip</b> към дата. Трябва да има следните файлове вътре:<br>
	 * Ek_obl.xlsx<br>
	 * Ek_obst.xlsx<br>
	 * Ek_atte.xlsx<br>
	 * След като се направи импорт трябва пак да се изтеглят датите на промяна.
	 *
	 * @param file
	 * @param date
	 * @throws IOException
	 * @throws EncryptedDocumentException
	 * @throws InvalidFormatException
	 * @throws InvalidParameterException
	 * @throws DbErrorException
	 * @see #selectDateImportList()
	 */
	public void load(File file, Date date) throws IOException, EncryptedDocumentException, InvalidFormatException, InvalidParameterException, DbErrorException {
		Map<String, LinkedHashMap<String, String[]>> content = new HashMap<>();

		try (ZipFile zip = new ZipFile(file)) { // изчитам файловете за да пускам после анализ на промените

			Enumeration<? extends ZipEntry> entries = zip.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();

				String entryName = entry.getName().toUpperCase();

				if (EK_OBL_XLSX.equals(entryName) || EK_OBST_XLSX.equals(entryName) || EK_ATTE_XLSX.equals(entryName)) {

					InputStream stream = zip.getInputStream(entry);

					LinkedHashMap<String, String[]> data = readSheet(stream, EK_ATTE_XLSX.equals(entryName));

					content.put(entryName, data);
				}
			}
		} catch (IOException | EncryptedDocumentException e) {
			LOGGER.error("Грешка при изчитане на архив за импорт!", e);

			throw e;
		}

		if (content.size() != 3) { // за момента ми трябват само трите
			throw new InvalidParameterException("Моля, проверете дали в прикачения архивен файл присъстват файловете: " //
				+ EK_OBL_XLSX + "," + EK_OBST_XLSX + "," + EK_ATTE_XLSX);
		}

		date = DateUtils.startDate(date);

		loadOblasti(content.get(EK_OBL_XLSX), date);
		loadObstini(content.get(EK_OBST_XLSX), date);
		loadNaseleni(content.get(EK_ATTE_XLSX), date);

		fixAttOblastObstinaImena(date);
		fixObstinaOblastIme(date);
	}

	/**
	 * Дава списък с датите, на които вече са направени импорти на ЕКАТТЕ. Сортирани в намаляващ ред (първата е най голямата). Не
	 * трябва да се позволява да се пуска нов импорт с по малка дата от първата в списъка. Ако няма направен нито един импорт се
	 * връща само заредена минималната дата за системата(01.01.1901).
	 *
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Date> selectDateImportList() throws DbErrorException {
		List<Date> dates;
		try {
			dates = JPA.getUtil().getEntityManager().createNativeQuery( //
				"select DATE_OT from EKATTE_ATT union select DATE_OT from EKATTE_OBSTINI union select DATE_OT from EKATTE_OBLASTI order by 1 desc") //
				.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на дати на импорт", e);
		}
		if (dates.isEmpty()) {
			dates.add(DateUtils.systemMinDate());
		}
		return dates;
	}

	/**
	 * Изчислява на новите редове в таблица EKATTE_ATT - OBLAST_IME и OBSTINA_IME
	 *
	 * @param dateArg
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void fixAttOblastObstinaImena(Date dateArg) throws DbErrorException {
		List<Object[]> rows;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select att.EKATTE, att.DATE_OT, obstina.IME OBSTINA_IME, oblast.IME OBLAST_IME ");
			sql.append(" from EKATTE_ATT att ");
			sql.append(" inner join EKATTE_OBSTINI obstina on obstina.OBSTINA = att.OBSTINA ");
			sql.append(" inner join EKATTE_OBLASTI oblast on oblast.OBLAST = att.OBLAST ");
			sql.append(" where att.OBLAST_IME is null ");
			sql.append(" and att.DATE_OT <= :dateArg and att.DATE_DO > :dateArg ");
			sql.append(" and obstina.DATE_OT <= :dateArg and obstina.DATE_DO > :dateArg ");
			sql.append(" and oblast.DATE_OT <= :dateArg and oblast.DATE_DO > :dateArg ");

			rows = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
				.setParameter("dateArg", dateArg) //
				.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсне на редове без OBLAST_IME и OBSTINA_IME", e);
		}

		if (rows.isEmpty()) {
			return;
		}

		Query updateQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
			"update EKATTE_ATT set OBSTINA_IME = :obstina, OBLAST_IME = :oblast where EKATTE = :ekatte and DATE_OT = :dateOt");
		for (Object[] row : rows) {
			updateQuery.setParameter("obstina", row[2]).setParameter("oblast", row[3]);
			updateQuery.setParameter("ekatte", row[0]).setParameter("dateOt", row[1]);

			updateQuery.executeUpdate();
		}
	}

	/**
	 * Изчислява на новите редове в таблица EKATTE_OBSTINI - OBLAST_IME
	 *
	 * @param dateArg
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void fixObstinaOblastIme(Date dateArg) throws DbErrorException {
		List<Object[]> rows;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select obstina.OBSTINA, obstina.DATE_OT, oblast.IME OBLAST_IME ");
			sql.append(" from EKATTE_OBSTINI obstina ");
			sql.append(" inner join EKATTE_OBLASTI oblast on oblast.OBLAST = ");
			sql.append(DialectConstructor.convertSQLSubstring(JPA.getUtil().getDbVendorName(), "obstina.OBSTINA", 1, 3));
			sql.append(" where obstina.OBLAST_IME is null ");
			sql.append(" and obstina.DATE_OT <= :dateArg and obstina.DATE_DO > :dateArg ");
			sql.append(" and oblast.DATE_OT <= :dateArg and oblast.DATE_DO > :dateArg ");

			rows = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
				.setParameter("dateArg", dateArg) //
				.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсне на редове без OBLAST_IME", e);
		}

		if (rows.isEmpty()) {
			return;
		}

		Query updateQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
			"update EKATTE_OBSTINI set OBLAST_IME = :oblastIme where OBSTINA = :obstina and DATE_OT = :dateOt");
		for (Object[] row : rows) {
			updateQuery.setParameter("oblastIme", row[2]);
			updateQuery.setParameter("obstina", row[0]).setParameter("dateOt", row[1]);

			updateQuery.executeUpdate();
		}
	}

	/**
	 * @param map     първата колона като стринг!, value=целия ред от файла
	 * @param dateArg
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void loadNaseleni(LinkedHashMap<String, String[]> map, Date dateArg) throws DbErrorException {
		List<Object[]> dbList;
		try {
			Query query = JPA.getUtil().getEntityManager().createNativeQuery( //
				"select EKATTE, TVM, IME, DATE_OT, OBLAST, OBSTINA from EKATTE_ATT where DATE_OT <= :dateArg and DATE_DO > :dateArg order by IME") //
				.setParameter("dateArg", dateArg);

			dbList = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при селект в таблица EKATTE_ATT", e);
		}

		Query updateQuery = null;
		Query deleteQuery = null;

		for (Object[] dbRow : dbList) {
			Integer ekatte = ((Number) dbRow[0]).intValue();
			String key = ekatte.toString();

			String[] fileRow = map.get(key); // COLS: ekatte, t_v_m, name, oblast, obstina, kmetstvo, kind, category, altitude,
												// document, tsb, abc

			if (fileRow != null && dbRow[1].equals(fileRow[1]) && dbRow[2].equals(fileRow[2]) //
				&& dbRow[4].equals(fileRow[3]) && dbRow[5].equals(fileRow[4])) { // няма разлика

				map.remove(key); // накрая ще останат само новите за запис

			} else { // разлика в име и затварям реда

				try {
					if (dateArg.equals(dbRow[3])) { // на една и съща дата не се пази история и трябва да се изтрие този ред

						if (deleteQuery == null) {
							deleteQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
								"delete from EKATTE_ATT where EKATTE = :ekatte and DATE_OT = :dateOt");
						}
						deleteQuery.setParameter("ekatte", ekatte).setParameter("dateOt", dbRow[3]) //
							.executeUpdate();

					} else { // корекция със запазване на история

						if (updateQuery == null) {
							updateQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
								"update EKATTE_ATT set VALID = 0, DATE_DO = :dateArg where EKATTE = :ekatte and DATE_OT = :dateOt");
						}
						updateQuery.setParameter("dateArg", dateArg).setParameter("ekatte", ekatte).setParameter("dateOt", dbRow[3]) //
							.executeUpdate();
					}

				} catch (Exception e) {
					throw new DbErrorException("Грешка при корекция в таблица EKATTE_ATT", e);
				}
			}
		}

		if (!map.isEmpty()) {

			Query insertQuery = null;
			Date dateDo = DateUtils.systemMaxDate();
			try {
				for (String[] fileRow : map.values()) { // COLS: ekatte, t_v_m, name, oblast, obstina, kmetstvo, kind, category,
														// altitude, document, tsb, abc
					if (insertQuery == null) {
						String sql = "insert into EKATTE_ATT (EKATTE, TVM, IME, OBLAST, OBSTINA, KMETSTVO, KIND, CATEGORY, ALTITUDE, DOCUMENT, TSB, ABC, VALID, DATE_OT, DATE_DO)" //
							+ " values (:ekatte, :tvm, :ime, :oblast, :obstina, :kmetstvo, :kind, :category, :altitude, :document, :tsb, :abc, 1, :dateOt, :dateDo)";
						insertQuery = JPA.getUtil().getEntityManager().createNativeQuery(sql);
					}
					insertQuery.setParameter("ekatte", Integer.valueOf(fileRow[0]));
					insertQuery.setParameter("tvm", fileRow[1]);
					insertQuery.setParameter("ime", fileRow[2]);
					insertQuery.setParameter("oblast", fileRow[3]);
					insertQuery.setParameter("obstina", fileRow[4]);
					insertQuery.setParameter("kmetstvo", fileRow[5]);
					insertQuery.setParameter("kind", Integer.valueOf(fileRow[6]));
					insertQuery.setParameter("category", Integer.valueOf(fileRow[7]));
					insertQuery.setParameter("altitude", Integer.valueOf(fileRow[8]));
					insertQuery.setParameter("document", Integer.valueOf(fileRow[9]));
					insertQuery.setParameter("tsb", fileRow[10]);
					if (fileRow.length == 12) { // има редове без тази колона
						insertQuery.setParameter("abc", Integer.valueOf(fileRow[11]));
					} else {
						insertQuery.setParameter("abc", -1);
					}
					insertQuery.setParameter("dateOt", dateArg);
					insertQuery.setParameter("dateDo", dateDo);

					insertQuery.executeUpdate();
				}
			} catch (Exception e) {
				throw new DbErrorException("Грешка при запис в таблица EKATTE_ATT", e);
			}
		}
	}

	/**
	 * @param map     първата колона като стринг!, value=целия ред от файла
	 * @param dateArg
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void loadOblasti(LinkedHashMap<String, String[]> map, Date dateArg) throws DbErrorException {
		List<Object[]> dbList;
		try {
			Query query = JPA.getUtil().getEntityManager().createNativeQuery( //
				"select OBLAST, EKATTE, IME, DATE_OT from EKATTE_OBLASTI where DATE_OT <= :dateArg and DATE_DO > :dateArg order by IME") //
				.setParameter("dateArg", dateArg);

			dbList = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при селект в таблица EKATTE_OBLASTI", e);
		}

		Query updateQuery = null;
		Query deleteQuery = null;

		for (Object[] dbRow : dbList) {
			String oblast = (String) dbRow[0];

			String[] fileRow = map.get(oblast); // COLS: oblast, ekatte, name, region, document, abc

			if (fileRow != null && dbRow[2].equals(fileRow[2])) { // няма разлика

				map.remove(oblast); // накрая ще останат само новите за запис

			} else { // разлика в име и затварям реда

				try {
					if (dateArg.equals(dbRow[3])) { // на една и съща дата не се пази история и трябва да се изтрие този ред

						if (deleteQuery == null) {
							deleteQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
								"delete from EKATTE_OBLASTI where OBLAST = :oblast and DATE_OT = :dateOt");
						}
						deleteQuery.setParameter("oblast", oblast).setParameter("dateOt", dbRow[3]) //
							.executeUpdate();

					} else { // корекция със запазване на история

						if (updateQuery == null) {
							updateQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
								"update EKATTE_OBLASTI set VALID = 0, DATE_DO = :dateArg where OBLAST = :oblast and DATE_OT = :dateOt");
						}
						updateQuery.setParameter("dateArg", dateArg).setParameter("oblast", oblast).setParameter("dateOt", dbRow[3]) //
							.executeUpdate();
					}

				} catch (Exception e) {
					throw new DbErrorException("Грешка при корекция в таблица EKATTE_OBLASTI", e);
				}
			}
		}

		if (!map.isEmpty()) {

			Query insertQuery = null;
			Date dateDo = DateUtils.systemMaxDate();

			try {
				for (String[] fileRow : map.values()) { // COLS: oblast, ekatte, name, region, document, abc

					if (insertQuery == null) {
						String sql = "insert into EKATTE_OBLASTI (OBLAST, EKATTE, IME, REGION, DOCUMENT, ABC, VALID, DATE_OT, DATE_DO)" //
							+ " values (:oblast, :ekatte, :ime, :region, :document, :abc, 1, :dateOt, :dateDo)";
						insertQuery = JPA.getUtil().getEntityManager().createNativeQuery(sql);
					}
					insertQuery.setParameter("oblast", fileRow[0]);
					insertQuery.setParameter("ekatte", Integer.valueOf(fileRow[1]));
					insertQuery.setParameter("ime", fileRow[2]);
					insertQuery.setParameter("region", fileRow[3]);
					insertQuery.setParameter("document", Integer.valueOf(fileRow[4]));
					insertQuery.setParameter("abc", Integer.valueOf(fileRow[5]));
					insertQuery.setParameter("dateOt", dateArg);
					insertQuery.setParameter("dateDo", dateDo);

					insertQuery.executeUpdate();
				}
			} catch (Exception e) {
				throw new DbErrorException("Грешка при запис в таблица EKATTE_OBLASTI", e);
			}
		}
	}

	/**
	 * @param map     първата колона като стринг!, value=целия ред от файла
	 * @param dateArg
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void loadObstini(LinkedHashMap<String, String[]> map, Date dateArg) throws DbErrorException {
		List<Object[]> dbList;
		try {
			Query query = JPA.getUtil().getEntityManager().createNativeQuery( //
				"select OBSTINA, EKATTE, IME, DATE_OT from EKATTE_OBSTINI where DATE_OT <= :dateArg and DATE_DO > :dateArg order by IME") //
				.setParameter("dateArg", dateArg);

			dbList = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при селект в таблица EKATTE_OBSTINI", e);
		}

		Query updateQuery = null;
		Query deleteQuery = null;

		for (Object[] dbRow : dbList) {
			String obstina = (String) dbRow[0];

			String[] fileRow = map.get(obstina); // COLS: obstina, ekatte, name, category, document, abc

			if (fileRow != null && dbRow[2].equals(fileRow[2])) { // няма разлика

				map.remove(obstina); // накрая ще останат само новите за запис

			} else { // разлика в име и затварям реда

				try {
					if (dateArg.equals(dbRow[3])) { // на една и съща дата не се пази история и трябва да се изтрие този ред

						if (deleteQuery == null) {
							deleteQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
								"delete from EKATTE_OBSTINI where OBSTINA = :obstina and DATE_OT = :dateOt");
						}
						deleteQuery.setParameter("obstina", obstina).setParameter("dateOt", dbRow[3]) //
							.executeUpdate();

					} else { // корекция със запазване на история

						if (updateQuery == null) {
							updateQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
								"update EKATTE_OBSTINI set VALID = 0, DATE_DO = :dateArg where OBSTINA = :obstina and DATE_OT = :dateOt");
						}
						updateQuery.setParameter("dateArg", dateArg).setParameter("obstina", obstina).setParameter("dateOt", dbRow[3]) //
							.executeUpdate();
					}

				} catch (Exception e) {
					throw new DbErrorException("Грешка при корекция в таблица EKATTE_OBSTINI", e);
				}
			}
		}

		if (!map.isEmpty()) {

			Query insertQuery = null;
			Date dateDo = DateUtils.systemMaxDate();

			try {
				for (String[] fileRow : map.values()) { // COLS: obstina, ekatte, name, category, document, abc

					if (insertQuery == null) {
						String sql = "insert into EKATTE_OBSTINI (OBSTINA, EKATTE, IME, CATEGORY, DOCUMENT, ABC, VALID, DATE_OT, DATE_DO)" //
							+ " values (:obstina, :ekatte, :ime, :category, :document, :abc, 1, :dateOt, :dateDo)";
						insertQuery = JPA.getUtil().getEntityManager().createNativeQuery(sql);
					}
					insertQuery.setParameter("obstina", fileRow[0]);
					insertQuery.setParameter("ekatte", Integer.valueOf(fileRow[1]));
					insertQuery.setParameter("ime", fileRow[2]);
					insertQuery.setParameter("category", Integer.valueOf(fileRow[3]));
					insertQuery.setParameter("document", Integer.valueOf(fileRow[4]));
					insertQuery.setParameter("abc", Integer.valueOf(fileRow[5]));
					insertQuery.setParameter("dateOt", dateArg);
					insertQuery.setParameter("dateDo", dateDo);

					insertQuery.executeUpdate();
				}
			} catch (Exception e) {
				throw new DbErrorException("Грешка при запис в таблица EKATTE_OBSTINI", e);
			}
		}
	}

	/**
	 * Изчита съдържанието @param stream @param naseleni @return key=първата колона като стринг!, value=целия ред от файла @throws
	 * IOException @throws InvalidFormatException @throws
	 */
	private LinkedHashMap<String, String[]> readSheet(InputStream stream, boolean naseleni) throws InvalidFormatException, IOException {
		LinkedHashMap<String, String[]> data = new LinkedHashMap<>();

		try (Workbook workbook = WorkbookFactory.create(stream)) {

			Sheet sheet = workbook.getSheetAt(0);

			int fromRowNum = naseleni ? 2 : 1;

			for (int rowNum = fromRowNum; rowNum < sheet.getLastRowNum() + 1; rowNum++) {

				Row row = sheet.getRow(rowNum);

				String key = null;
				String[] values = new String[row.getLastCellNum()];

				for (int i = 0; i < row.getLastCellNum(); i++) {
					Cell cell = row.getCell(i);

					if (cell == null) {
						continue;
					}

					if (cell.getCellType() == CellType.STRING) {
						values[i] = cell.getStringCellValue();

						if (i == 0) {
							if (naseleni) {
								key = Integer.valueOf(values[i]).toString(); // така е за да се махнат водещите нули, които не се
																				// пазят в БД
							} else {
								key = values[i];
							}
						}

					} else {
						LOGGER.warn("Unknown CellType={}", cell.getCellType());
					}
				}
				data.put(key, values);
			}
		}

		return data;
	}
}
