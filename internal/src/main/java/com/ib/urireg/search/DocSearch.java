package com.ib.urireg.search;

import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.DateUtils;

import java.util.*;

import static com.ib.urireg.system.UriregConstants.*;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

/**
 * Търсене на документи от различните видовед
 */
public class DocSearch extends SelectMetadata {
	private static final long serialVersionUID = 6736260704536384658L;

	private Integer zapDocVid;

	private String rnZap; // •	Номер на заповед
	private boolean rnZapEQ = true;    // ако е true се търси по пълно съвпадение по номер на документ
	private Date zapDateFrom; // •	Период – дата от – дата до
	private Date zapDateTo;

	private Date protTestDateFrom; // •	Дата на протокол - тест - период – дата от -до
	private Date protTestDateTo;
	private Date protCaseDateFrom; // •	Дата на протокол - казус - период – дата от -до
	private Date protCaseDateTo;

	private String predsProt; // •	Председател

	private Integer protDocVid;

	public DocSearch() {
	}

	/**
	 * Филтър – заповеди за стаж<br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-doc_id<br>
	 * [1]-rn_doc<br>
	 * [2]-doc_date<br>
	 * [3]-doc_vid<br>
	 * [4]-code_izgotvil<br>
	 * [5]-doc_info<br>
	 */
	public void buildZapovedStajQuery() {
		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select d.doc_id a0, d.rn_doc a1, d.doc_date a2, d.doc_vid a3 ");
		select.append(" , d.code_izgotvil a4, d.doc_info a5 ");

		from.append(" from doc d ");

		where.append(" where d.doc_vid in (:docVidList) ");
		List<Integer> docVidList = this.zapDocVid != null //
				? List.of(this.zapDocVid) //
				: Arrays.asList(CODE_ZNACHENIE_DOC_VID_ZAP_STAJ, CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ);
		params.put("docVidList", docVidList);


		String t = trimToNULL_Upper(this.rnZap);
		if (t != null) {
			if (this.rnZapEQ) { // пълно съвпадение
				where.append(" and upper(d.rn_doc) = :rnZap ");
				params.put("rnZap", t);

			} else {
				where.append(" and upper(d.rn_doc) like :rnZap ");
				params.put("rnZap", "%" + t + "%");
			}
		}
		if (this.zapDateFrom != null) {
			where.append(" and d.doc_date >= :zapDateFrom ");
			params.put("zapDateFrom", DateUtils.startDate(this.zapDateFrom));
		}
		if (this.zapDateTo != null) {
			where.append(" and d.doc_date <= :zapDateTo ");
			params.put("zapDateTo", DateUtils.endDate(this.zapDateTo));
		}

		setSqlCount(" select count(*) " + from + where); // на този етап бройката е готова
		setSql(select.toString() + from + where);
		setSqlParameters(params);
	}

	/**
	 * Филтър – заповеди за изпит<br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-zap.doc_id<br>
	 * [1]-zap.rn_doc<br>
	 * [2]-zap.doc_date<br>
	 * [3]-zap.doc_vid<br>
	 * [4]-prot.doc_id<br>
	 * [5]-prot.rn_doc<br>
	 * [6]-prot.doc_date<br>
	 * [7]-prot.doc_vid<br>
	 * [8]-prot.predsedatel<br>
	 * [9]-prot.members<br>
	 * [10]-zap.zaiav_date<br>
	 */
	public void buildZapovedIzpitQuery() {
		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select d.doc_id a0, d.rn_doc a1, d.doc_date a2, d.doc_vid a3 ");
		select.append(" , p.doc_id a4, p.rn_doc a5, p.doc_date a6, p.doc_vid a7 ");
		select.append(" , p.predsedatel a8, p.members a9 ");
		select.append(" , d.zaiav_date a10");

		from.append(" from doc d ");
		from.append(" inner join izpit i on i.zap_izp_id = d.doc_id ");

		String fromTest = "inner join doc p on p.doc_id = i.test_prot_id";
		String fromCase = "inner join doc p on p.doc_id = i.case_prot_id";

		where.append(" where d.doc_vid in (:docVidList) ");
		List<Integer> docVidList = this.zapDocVid != null //
				? List.of(this.zapDocVid) //
				: Arrays.asList(CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT, CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019);
		params.put("docVidList", docVidList);

		String t = trimToNULL_Upper(this.rnZap);
		if (t != null) {
			if (this.rnZapEQ) { // пълно съвпадение
				where.append(" and upper(d.rn_doc) = :rnZap ");
				params.put("rnZap", t);

			} else {
				where.append(" and upper(d.rn_doc) like :rnZap ");
				params.put("rnZap", "%" + t + "%");
			}
		}
		if (this.zapDateFrom != null) {
			where.append(" and d.doc_date >= :zapDateFrom ");
			params.put("zapDateFrom", DateUtils.startDate(this.zapDateFrom));
		}
		if (this.zapDateTo != null) {
			where.append(" and d.doc_date <= :zapDateTo ");
			params.put("zapDateTo", DateUtils.endDate(this.zapDateTo));
		}

		String whereTest = "";
		if (this.protTestDateFrom != null) {
			whereTest += " and p.doc_date >= :protTestDateFrom ";
			params.put("protTestDateFrom", DateUtils.startDate(this.protTestDateFrom));
		}
		if (this.protTestDateTo != null) {
			whereTest += " and p.doc_date <= :protTestDateTo ";
			params.put("protTestDateTo", DateUtils.endDate(this.protTestDateTo));
		}
		t = trimToNULL_Upper(this.predsProt);
		if (t != null) {
			whereTest += " and upper(p.predsedatel) like :predsProt ";
			params.put("predsProt", "%" + t + "%");
		}

		String whereCase = "";
		if (this.protCaseDateFrom != null) {
			whereCase += " and p.doc_date >= :protCaseDateFrom ";
			params.put("protCaseDateFrom", DateUtils.startDate(this.protCaseDateFrom));
		}
		if (this.protCaseDateTo != null) {
			whereCase += " and p.doc_date <= :protCaseDateTo ";
			params.put("protCaseDateTo", DateUtils.endDate(this.protCaseDateTo));
		}
		t = trimToNULL_Upper(this.predsProt);
		if (t != null) {
			whereCase += " and upper(p.predsedatel) like :predsProt ";
			params.put("predsProt", "%" + t + "%");
		}

		if (this.protDocVid != null) {
			if (this.protDocVid.equals(CODE_ZNACHENIE_DOC_VID_PROT_TEST)) {
				whereCase += " and 1=2"; // за да не върне нищо от прот.казус
			} else if (this.protDocVid.equals(CODE_ZNACHENIE_DOC_VID_PROT)) {
				whereTest += " and 1=2"; // за да не върне нищо от прот.тест
			}
		}

		setSql(select.toString() + from + fromTest + where + whereTest //
				+ " union all " //
				+ select + from + fromCase + where + whereCase);

		setSqlCount(" select count(x.*) from ( " + getSql() + " ) as x "); // на този етап бройката е готова

		setSqlParameters(params);
	}

	public String getRnZap() {
		return rnZap;
	}

	public void setRnZap(String rnZap) {
		this.rnZap = rnZap;
	}

	public boolean isRnZapEQ() {
		return rnZapEQ;
	}

	public void setRnZapEQ(boolean rnZapEQ) {
		this.rnZapEQ = rnZapEQ;
	}

	public Date getZapDateFrom() {
		return zapDateFrom;
	}

	public void setZapDateFrom(Date zapDateFrom) {
		this.zapDateFrom = zapDateFrom;
	}

	public Date getZapDateTo() {
		return zapDateTo;
	}

	public void setZapDateTo(Date zapDateTo) {
		this.zapDateTo = zapDateTo;
	}

	public String getPredsProt() {
		return predsProt;
	}

	public void setPredsProt(String predsProt) {
		this.predsProt = predsProt;
	}

	public Date getProtTestDateFrom() {
		return protTestDateFrom;
	}

	public void setProtTestDateFrom(Date protTestDateFrom) {
		this.protTestDateFrom = protTestDateFrom;
	}

	public Date getProtTestDateTo() {
		return protTestDateTo;
	}

	public void setProtTestDateTo(Date protTestDateTo) {
		this.protTestDateTo = protTestDateTo;
	}

	public Date getProtCaseDateFrom() {
		return protCaseDateFrom;
	}

	public void setProtCaseDateFrom(Date protCaseDateFrom) {
		this.protCaseDateFrom = protCaseDateFrom;
	}

	public Date getProtCaseDateTo() {
		return protCaseDateTo;
	}

	public void setProtCaseDateTo(Date protCaseDateTo) {
		this.protCaseDateTo = protCaseDateTo;
	}

	public Integer getZapDocVid() {
		return zapDocVid;
	}

	public void setZapDocVid(Integer zapDocVid) {
		this.zapDocVid = zapDocVid;
	}

	public Integer getProtDocVid() {
		return protDocVid;
	}

	public void setProtDocVid(Integer protDocVid) {
		this.protDocVid = protDocVid;
	}
}
