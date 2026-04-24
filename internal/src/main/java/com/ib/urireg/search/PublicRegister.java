package com.ib.urireg.search;

import com.ib.system.SysConstants;
import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

import java.util.*;

import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;
import static com.ib.urireg.system.UriregConstants.*;

/**
 * Формира селект за публичен регистър
 */
public class PublicRegister extends SelectMetadata {
	private static final long serialVersionUID = -6919383027765778864L;

	private Integer liceStatus; // CODE_ZNACHENIE_LICE_STATUS_PRAVO или CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN

	private String rnUdost; // УП номер
	private boolean rnUdostEQ = true;    // ако е true се търси по пълно съвпадение по номер на документ
	private Date udostDateFrom; // Период – дата от - УП дата
	private Date udostDateTo; // Период – дата до - УП дата

	private String firstname;
	private String surname;
	private String lastname;

	private Date protDateFrom; // Период – дата от - Протокол дата
	private Date protDateTo; // Период – дата до - Протокол дата

	private Integer udostStatus; // Статус на УП
	private Integer dublicat; // Дубликат (ДА/НЕ)

	// ако се подаде нещо за тези ще ги направи директно в селекта и няма да може да се използва за LazyDataModelSQL2Array
	// ако са празни си е ОК за LazyDataModelSQL2Array
	private String sortColumn;
	private String sortDirection;

	public PublicRegister() {
	}

	/**
	 * Публичен регистър <br>
	 * Подготвя селект за получаване на резултат от вида: <br>
	 * [0]-lice_id<br>
	 * [1]-firstname<br>
	 * [2]-surname<br>
	 * [3]-lastname<br>
	 * [4]-lice.status <b>при status=CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN следва "Лишен от право"="Да"</b><br>
	 * [5]-lice.status_date<br>
	 * [6]-udost.doc_id<br>
	 * [7]-udost.rn_doc<br>
	 * [8]-udost.doc_date<br>
	 * [9]-udost.status <b>при status=??? следва "Изгубено/Унищожено"="???"</b> TODO не е ясен статуса<br>
	 * [10]-udost.status_date<br>
	 * [11]-udost.original <b>при original=CODE_ZNACHENIE_NE следва "Дубликат"="Да"</b><br>
	 * [12]-prot.prot_id<br>
	 * [13]-prot.prot_date<br>
	 */
	@Override
	public void buildQuery() {
		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select lice.lice_id a0, lice.firstname a1, lice.surname a2, lice.lastname a3, lice.status a4, lice.status_date a5 ");
		select.append(" , udost.doc_id a6, udost.rn_doc a7, udost.doc_date a8, udost.status a9, udost.status_date a10, udost.original a11 ");
		select.append(" , prot.prot_id a12, prot.prot_date a13 ");

		from.append(" from lice ");
		from.append(" inner join doc udost on udost.doc_id = lice.udost_id ");
		from.append(" left outer join ( ");
		from.append("     select distinct on (ld.lice_id) ld.lice_id, d.doc_id prot_id, d.doc_date prot_date ");
		from.append("     from lice_doc ld ");
		from.append("     inner join doc d on d.doc_id = ld.doc_id ");
		from.append("     where d.doc_vid = :protVid ");
		from.append("     order by ld.lice_id, d.doc_date desc ");
		from.append(" ) prot on prot.lice_id = lice.lice_id ");
		params.put("protVid", CODE_ZNACHENIE_DOC_VID_PROT);

		where.append(" where lice.status in (:liceStatusList) ");

		List<Integer> liceStatusList;
		if (this.liceStatus != null) {
			liceStatusList = List.of(this.liceStatus);
		} else {
			liceStatusList = Arrays.asList(CODE_ZNACHENIE_LICE_STATUS_PRAVO, CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN);
		}
		params.put("liceStatusList", liceStatusList);


		String t = trimToNULL_Upper(this.rnUdost);
		if (t != null) {
			if (this.rnUdostEQ) { // пълно съвпадение
				where.append(" and upper(udost.rn_doc) = :rnUdost ");
				params.put("rnUdost", t);

			} else {
				where.append(" and upper(udost.rn_doc) like :rnUdost ");
				params.put("rnUdost", "%" + t + "%");
			}
		}
		if (this.udostDateFrom != null) {
			where.append(" and udost.doc_date >= :udostDateFrom ");
			params.put("udostDateFrom", DateUtils.startDate(this.udostDateFrom));
		}
		if (this.udostDateTo != null) {
			where.append(" and udost.doc_date <= :udostDateTo ");
			params.put("udostDateTo", DateUtils.endDate(this.udostDateTo));
		}


		t = trimToNULL_Upper(this.firstname);
		if (t != null) {
			where.append(" and upper(lice.firstname) like :firstname ");
			params.put("firstname", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.surname);
		if (t != null) {
			where.append(" and upper(lice.surname) like :surname ");
			params.put("surname", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.lastname);
		if (t != null) {
			where.append(" and upper(lice.lastname) like :lastname ");
			params.put("lastname", "%" + t + "%");
		}


		if (this.protDateFrom != null) {
			where.append(" and prot.prot_date >= :protDateFrom ");
			params.put("protDateFrom", DateUtils.startDate(this.protDateFrom));
		}
		if (this.protDateTo != null) {
			where.append(" and prot.prot_date <= :protDateTo ");
			params.put("protDateTo", DateUtils.endDate(this.protDateTo));
		}


		if (this.udostStatus != null) {
			where.append(" and udost.status = :udostStatus ");
			params.put("udostStatus", this.udostStatus);
		}


		if (Objects.equals(this.dublicat, SysConstants.CODE_ZNACHENIE_DA)) {
			where.append(" and udost.original = :originalArg ");
			params.put("originalArg", SysConstants.CODE_ZNACHENIE_NE);

		} else if (Objects.equals(this.dublicat, SysConstants.CODE_ZNACHENIE_NE)) {
			where.append(" and (udost.original is null or udost.original = :originalArg) ");
			params.put("originalArg", SysConstants.CODE_ZNACHENIE_DA);
		}


		StringBuilder customOrder = new StringBuilder();
		if (!SearchUtils.isEmpty(this.sortColumn)) {
			customOrder.append(" order by " + this.sortColumn);

			if (SearchUtils.isEmpty(this.sortDirection)) {
				customOrder.append(" asc ");
			} else {
				customOrder.append(" " + this.sortDirection);
			}

            customOrder.append(" ").append(" NULLS LAST ");
		}


		setSqlCount(" select count(*) as cnt " + from + where);
		setSql(select.toString() + from + where + customOrder);
		setSqlParameters(params);
	}

	public String getRnUdost() {
		return rnUdost;
	}

	public void setRnUdost(String rnUdost) {
		this.rnUdost = rnUdost;
	}

	public boolean isRnUdostEQ() {
		return rnUdostEQ;
	}

	public void setRnUdostEQ(boolean rnUdostEQ) {
		this.rnUdostEQ = rnUdostEQ;
	}

	public Date getUdostDateFrom() {
		return udostDateFrom;
	}

	public void setUdostDateFrom(Date udostDateFrom) {
		this.udostDateFrom = udostDateFrom;
	}

	public Date getUdostDateTo() {
		return udostDateTo;
	}

	public void setUdostDateTo(Date udostDateTo) {
		this.udostDateTo = udostDateTo;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public Date getProtDateFrom() {
		return protDateFrom;
	}

	public void setProtDateFrom(Date protDateFrom) {
		this.protDateFrom = protDateFrom;
	}

	public Date getProtDateTo() {
		return protDateTo;
	}

	public void setProtDateTo(Date protDateTo) {
		this.protDateTo = protDateTo;
	}

	public Integer getUdostStatus() {
		return udostStatus;
	}

	public void setUdostStatus(Integer udostStatus) {
		this.udostStatus = udostStatus;
	}

	public Integer getDublicat() {
		return dublicat;
	}

	public void setDublicat(Integer dublicat) {
		this.dublicat = dublicat;
	}

	public Integer getLiceStatus() {
		return liceStatus;
	}

	public void setLiceStatus(Integer liceStatus) {
		this.liceStatus = liceStatus;
	}

	public String getSortColumn() {
		return sortColumn;
	}

	public void setSortColumn(String sortColumn) {
		this.sortColumn = sortColumn;
	}

	public String getSortDirection() {
		return sortDirection;
	}

	public void setSortDirection(String sortDirection) {
		this.sortDirection = sortDirection;
	}
}
