package com.ib.urireg.search;

import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.DateUtils;
import jakarta.persistence.Query;

import java.util.Date;
import java.util.List;

import static com.ib.urireg.system.UriregConstants.CODE_CLASSIF_UNIVERSITETI;

public class StatReports {

	/**
	 * Връща разпределение на проведени изпити разпределени по университети. Връща цялата таблица без странициране.</br>
	 * Резултат:</br>
	 * [0]-universitet</br>
	 * [1]-total</br>
	 * [2]-passed</br>
	 * [3]-failed</br>
	 * [4]-missed (не участват в тотала)</br>
	 *
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> selectStatUniversitetIzpiti(Date dateFrom, Date dateTo) throws DbErrorException, InvalidParameterException {
		if (dateFrom == null || dateTo == null) {
			throw new InvalidParameterException("Моля, въведете период.");
		}

		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select replace(x.universitet_tekst, 'Я - ', '') universitet ");
			sql.append(" , sum (x.passed + x.failed) total ");
			sql.append(" , sum(x.passed) passed ");
			sql.append(" , sum(x.failed) failed ");
			sql.append(" , sum(x.missed) missed ");
			sql.append(" from ( ");

			sql.append(buildInnerGroup()); // това е еднакво и за двете справки

			sql.append(" ) x ");
			sql.append(" group by x.universitet, x.universitet_tekst ");
			sql.append(" order by x.universitet_tekst ");

			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString());

			query.setParameter("universiteti", CODE_CLASSIF_UNIVERSITETI);
			query.setParameter("dateFrom", DateUtils.startDate(dateFrom));
			query.setParameter("dateTo", DateUtils.endDate(dateTo));

			List<Object[]> results = query.getResultList();
			return results;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изпълнение на справка!", e);
		}
	}

	/**
	 * Връща разпределение на проведени изпити разпределени по протоколи и университети. Връща цялата таблица без странициране.</br>
	 * Резултат:</br>
	 * [0]-prot_date</br>
	 * [1]-universitet</br>
	 * [2]-passed</br>
	 * [3]-failed</br>
	 * [4]-missed (не участват в тотала)</br>
	 *
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> selectStatProtUniversitetIzpiti(Date dateFrom, Date dateTo) throws DbErrorException, InvalidParameterException {
		if (dateFrom == null || dateTo == null) {
			throw new InvalidParameterException("Моля, въведете период.");
		}

		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select case when grouping(x.universitet_tekst) = 1 then to_char(x.prot_date, 'dd.mm.yyyy') || ' - ' || (sum(x.passed + x.failed)) else '' end prot_date ");
			sql.append(" , case when grouping(x.universitet_tekst) = 1 then '' else replace(x.universitet_tekst, 'Я - ', '') end universitet ");
			sql.append(" , sum(x.passed) passed ");
			sql.append(" , sum(x.failed) failed ");
			sql.append(" , sum(x.missed) missed ");
			sql.append(" from ( ");

			sql.append(buildInnerGroup()); // това е еднакво и за двете справки

			sql.append(" ) x ");
			sql.append(" group by grouping sets ( ");
			sql.append("    (x.prot_id, x.prot_date, x.universitet_tekst), ");
			sql.append("    (x.prot_id, x.prot_date) ");
			sql.append(" ) ");
			sql.append(" order by x.prot_id, x.prot_date, grouping(x.universitet_tekst) desc, x.universitet_tekst ");

			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString());

			query.setParameter("universiteti", CODE_CLASSIF_UNIVERSITETI);
			query.setParameter("dateFrom", DateUtils.startDate(dateFrom));
			query.setParameter("dateTo", DateUtils.endDate(dateTo));

			List<Object[]> results = query.getResultList();
			return results;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изпълнение на справка!", e);
		}
	}

	private String buildInnerGroup() {
		StringBuilder sql = new StringBuilder();
		sql.append(" select t.prot_id, t.prot_date, t.universitet, t.universitet_tekst ");
		sql.append(" , case when t.real_result = 1 then 1 else 0 end passed ");
		sql.append(" , case when t.real_result = 2 then 1 else 0 end failed ");
		sql.append(" , case when t.real_result = 3 then 1 else 0 end missed ");
		sql.append(" from ( ");

		//
		sql.append(" select p.doc_id prot_id, p.doc_date prot_date ");
		sql.append(" , l.lice_id ");
		sql.append(" , l.universitet ");
		sql.append(" , case when l.universitet is not null then sc.tekst else 'Я - Без университет' end universitet_tekst ");
		sql.append(" , l.do_2019 ");
		sql.append(" , ir.result_id, ir.test_result, ir.case_result ");
		sql.append(" , case  when l.do_2019 = 1 then coalesce(ir.case_result, 3) "); // за тези е какво си е, но ако е празно го броим за неявил се
		sql.append("       when ir.case_result = 1 then ir.case_result "); // Издържал
		sql.append("       when ir.test_result = 2 or ir.case_result = 2 then 2 "); // Неиздържал
		sql.append("       when ir.test_result = 3 or ir.case_result = 3 then 3 "); // Неявил се
		sql.append("       when ir.test_result is null then 3 "); // Неявил се
		sql.append("       when ir.test_result = 1 and ir.case_result is null then 3 "); // издържал теста, но Неявил се на казус
		sql.append("       else -1 "); // такива не трябва да има
		sql.append(" end real_result ");
		sql.append(" from lice l ");
		sql.append(" inner join izpit_result ir on ir.lice_id = l.lice_id ");
		sql.append(" inner join izpit i on i.izpit_id = ir.izpit_id ");
		sql.append(" inner join doc p on p.doc_id = i.case_prot_id ");
		sql.append(" left outer join v_system_classif sc on sc.code = l.universitet and sc.code_classif = :universiteti and sc.date_do is null ");
		sql.append(" where p.doc_date >= :dateFrom and p.doc_date <= :dateTo ");
		//

		sql.append(" ) t ");

		return sql.toString();
	}
}
