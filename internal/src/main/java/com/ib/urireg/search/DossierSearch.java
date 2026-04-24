package com.ib.urireg.search;

import com.ib.system.SysConstants;
import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.DateUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.ib.system.utils.SearchUtils.trimToNULL;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

/**
 * Клас за търсене на досиета
 */
public class DossierSearch extends SelectMetadata {
	private static final long serialVersionUID = -286297465898845465L;

	private String egn;
	private String lnc;
	private String egnlnc;

	private String firstname;
	private String surname;
	private String lastname;
	private String names;

	private Integer status;
	private Date statusDateFrom;
	private Date statusDateTo;

	private Boolean bezUdost;

	private String rnZaiavStaj;
	private boolean rnZaiavStajEQ = true;    // ако е true се търси по пълно съвпадение по номер на документ
	private Date zaiavStajRegDateFrom;
	private Date zaiavStajRegDateTo;

	private String rnZapStaj;
	private boolean rnZapStajEQ = true;    // ако е true се търси по пълно съвпадение по номер на документ
	private Date zapStajRegDateFrom;
	private Date zapStajRegDateTo;

	private String rnZaiavIzp;
	private boolean rnZaiavIzpEQ = true;    // ако е true се търси по пълно съвпадение по номер на документ
	private Date zaiavIzpRegDateFrom;
	private Date zaiavIzpRegDateTo;
	private Date zaiavIzpPodDateFrom;
	private Date zaiavIzpPodDateTo;

	private String rnUdost;
	private boolean rnUdostEQ = true;    // ако е true се търси по пълно съвпадение по номер на документ
	private Date udostRegDateFrom;
	private Date udostRegDateTo;

	private Integer osnInstitution;
	private Integer universitet;
	private Integer addrEkatte;

	private Integer broiIzpit;
	private Integer do2019;

	private Integer emptyAddrEkatte; // без населено място - SysConstants.CODE_ZNACHENIE_DA
	private Integer emptyAddrText; // без адрес - текст - SysConstants.CODE_ZNACHENIE_DA
	private Integer emptyUniversitet; // без учебно заведение - SysConstants.CODE_ZNACHENIE_DA
	private Integer emptyBirthPlace; // без местораждане - SysConstants.CODE_ZNACHENIE_DA

	public DossierSearch() {
	}

	/**
	 * Филтър Досиета <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-lice_id<br>
	 * [1]-egn<br>
	 * [2]-lnc<br>
	 * [3]-lice.names<br>
	 * [4]-zaiav_staj.doc_id<br>
	 * [5]-zaiav_staj.rn_doc<br>
	 * [6]-zaiav_staj.doc_date<br>
	 * [7]-zap_staj.doc_id<br>
	 * [8]-zap_staj.rn_doc<br>
	 * [9]-zap_staj.doc_date <br>
	 * [10]-osn_institution<br>
	 * [11]-zaiav_izp.doc_id<br>
	 * [12]-zaiav_izp.rn_doc<br>
	 * [13]-zaiav_izp.doc_date<br>
	 * [14]-udost.doc_id a14<br>
	 * [15]-udost.rn_doc<br>
	 * [16]-udost.doc_date<br>
	 * [17]-lice.status<br>
	 * [18]-lice.firstname<br>
	 * [19]-lice.surname<br>
	 * [20]-lice.lastname<br>
	 * [21]-zaiav_izp.zaiav_date<br>
	 */
	public void buildFilterQuery() {
		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select lice.lice_id a0, lice.egn a1, lice.lnc a2, lice.names a3 ");
		select.append(" , zaiav_staj.doc_id a4, zaiav_staj.rn_doc a5 ");
		select.append(" , COALESCE(zaiav_staj.doc_date, TO_DATE('1900-01-01', 'YYYY-MM-DD')) a6 ");
		select.append(" , zap_staj.doc_id a7, zap_staj.rn_doc a8, zap_staj.doc_date a9 ");
		select.append(" , staj.osn_institution a10 ");
		select.append(" , zaiav_izp.doc_id a11, zaiav_izp.rn_doc a12, zaiav_izp.doc_date a13 ");
		select.append(" , udost.doc_id a14, udost.rn_doc a15, udost.doc_date a16 ");
		select.append(" , lice.status a17, lice.firstname a18, lice.surname a19, lice.lastname a20 ");
		select.append(" , zaiav_izp.zaiav_date a21 ");

		from.append(" from lice ");
		from.append(" left outer join doc zaiav_staj on zaiav_staj.doc_id = lice.last_zaiav_staj_id ");
		from.append(" left outer join staj on staj.lice_id = lice.lice_id and staj.zaiav_staj_id = zaiav_staj.doc_id ");
		from.append(" left outer join doc zap_staj on zap_staj.doc_id = staj.zap_staj_id ");
		from.append(" left outer join doc zaiav_izp on zaiav_izp.doc_id = staj.zaiav_izp_id ");
		from.append(" left outer join doc udost on udost.doc_id = lice.udost_id ");

		where.append(" where 1=1 ");

		String t = trimToNULL(this.egn);
		if (t != null) {
			where.append(" and lice.egn like :egn ");
			params.put("egn", "%" + t + "%");
		}
		t = trimToNULL(this.lnc);
		if (t != null) {
			where.append(" and lice.lnc like :lnc ");
			params.put("lnc", "%" + t + "%");
		}

		t = trimToNULL(this.egnlnc);
		if (t != null) {
			where.append(" and (lice.egn like :egnlnc or lnc like :egnlnc) ");
			params.put("egnlnc", "%" + t + "%");
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
		t = trimToNULL_Upper(this.names);
		if (t != null) {
			t = t.replaceAll("\\s+", "%");
			where.append(" and upper(lice.names) like :names ");
			params.put("names", "%" + t + "%");
		}


		if (this.status != null) {
			where.append(" and lice.status = :status ");
			params.put("status", this.status);
		}
		if (this.statusDateFrom != null) {
			where.append(" and lice.status_date >= :statusDateFrom ");
			params.put("statusDateFrom", DateUtils.startDate(this.statusDateFrom));
		}
		if (this.statusDateTo != null) {
			where.append(" and lice.status_date <= :statusDateTo ");
			params.put("statusDateTo", DateUtils.endDate(this.statusDateTo));
		}

		if (this.bezUdost != null) {
			if (this.bezUdost) {
				where.append(" and lice.udost_id is null ");
			} else {
				where.append(" and lice.udost_id is not null ");
			}
		}


		t = trimToNULL_Upper(this.rnZaiavStaj);
		if (t != null) {
			if (this.rnZaiavStajEQ) { // пълно съвпадение
				where.append(" and upper(zaiav_staj.rn_doc) = :rnZaiavStaj ");
				params.put("rnZaiavStaj", t);

			} else {
				where.append(" and upper(zaiav_staj.rn_doc) like :rnZaiavStaj ");
				params.put("rnZaiavStaj", "%" + t + "%");
			}
		}
		if (this.zaiavStajRegDateFrom != null) {
			where.append(" and zaiav_staj.doc_date >= :zaiavStajRegDateFrom ");
			params.put("zaiavStajRegDateFrom", DateUtils.startDate(this.zaiavStajRegDateFrom));
		}
		if (this.zaiavStajRegDateTo != null) {
			where.append(" and zaiav_staj.doc_date <= :zaiavStajRegDateTo ");
			params.put("zaiavStajRegDateTo", DateUtils.endDate(this.zaiavStajRegDateTo));
		}


		t = trimToNULL_Upper(this.rnZapStaj);
		if (t != null) {
			if (this.rnZapStajEQ) { // пълно съвпадение
				where.append(" and upper(zap_staj.rn_doc) = :rnZapStaj ");
				params.put("rnZapStaj", t);

			} else {
				where.append(" and upper(zap_staj.rn_doc) like :rnZapStaj ");
				params.put("rnZapStaj", "%" + t + "%");
			}
		}
		if (this.zapStajRegDateFrom != null) {
			where.append(" and zap_staj.doc_date >= :zapStajRegDateFrom ");
			params.put("zapStajRegDateFrom", DateUtils.startDate(this.zapStajRegDateFrom));
		}
		if (this.zapStajRegDateTo != null) {
			where.append(" and zap_staj.doc_date <= :zapStajRegDateTo ");
			params.put("zapStajRegDateTo", DateUtils.endDate(this.zapStajRegDateTo));
		}


		t = trimToNULL_Upper(this.rnZaiavIzp);
		if (t != null) {
			if (this.rnZaiavIzpEQ) { // пълно съвпадение
				where.append(" and upper(zaiav_izp.rn_doc) = :rnZaiavIzp ");
				params.put("rnZaiavIzp", t);

			} else {
				where.append(" and upper(zaiav_izp.rn_doc) like :rnZaiavIzp ");
				params.put("rnZaiavIzp", "%" + t + "%");
			}
		}
		if (this.zaiavIzpRegDateFrom != null) {
			where.append(" and zaiav_izp.doc_date >= :zaiavIzpRegDateFrom ");
			params.put("zaiavIzpRegDateFrom", DateUtils.startDate(this.zaiavIzpRegDateFrom));
		}
		if (this.zaiavIzpRegDateTo != null) {
			where.append(" and zaiav_izp.doc_date <= :zaiavIzpRegDateTo ");
			params.put("zaiavIzpRegDateTo", DateUtils.endDate(this.zaiavIzpRegDateTo));
		}
		if (this.zaiavIzpPodDateFrom != null) {
			where.append(" and zaiav_izp.zaiav_date >= :zaiavIzpPodDateFrom ");
			params.put("zaiavIzpPodDateFrom", DateUtils.startDate(this.zaiavIzpPodDateFrom));
		}
		if (this.zaiavIzpPodDateTo != null) {
			where.append(" and zaiav_izp.zaiav_date <= :zaiavIzpPodDateTo ");
			params.put("zaiavIzpPodDateTo", DateUtils.endDate(this.zaiavIzpPodDateTo));
		}


		t = trimToNULL_Upper(this.rnUdost);
		if (t != null) {
			if (this.rnUdostEQ) { // пълно съвпадение
				where.append(" and upper(udost.rn_doc) = :rnUdost ");
				params.put("rnUdost", t);

			} else {
				where.append(" and upper(udost.rn_doc) like :rnUdost ");
				params.put("rnUdost", "%" + t + "%");
			}
		}
		if (this.udostRegDateFrom != null) {
			where.append(" and udost.doc_date >= :udostRegDateFrom ");
			params.put("udostRegDateFrom", DateUtils.startDate(this.udostRegDateFrom));
		}
		if (this.udostRegDateTo != null) {
			where.append(" and udost.doc_date <= :udostRegDateTo ");
			params.put("udostRegDateTo", DateUtils.endDate(this.udostRegDateTo));
		}


		if (this.osnInstitution != null) {
			where.append(" and staj.osn_institution = :osnInstitution ");
			params.put("osnInstitution", this.osnInstitution);
		}
		if (this.universitet != null) {
			where.append(" and lice.universitet = :universitet ");
			params.put("universitet", this.universitet);
		}
		if (this.addrEkatte != null) {
			where.append(" and lice.addr_ekatte = :addrEkatte ");
			params.put("addrEkatte", this.addrEkatte);
		}

		if (this.broiIzpit != null) {
			where.append(" and lice.broi_izpit = :broiIzpit ");
			params.put("broiIzpit", this.broiIzpit);
		}
		if (this.do2019 != null) {
			where.append(" and lice.do_2019 = :do2019 ");
			params.put("do2019", this.do2019);
		}


		if (Objects.equals(this.emptyAddrEkatte, SysConstants.CODE_ZNACHENIE_DA)) {
			where.append(" and lice.addr_ekatte is null ");
		}
		if (Objects.equals(this.emptyAddrText, SysConstants.CODE_ZNACHENIE_DA)) {
			where.append(" and (lice.addr_text is null or trim(lice.addr_text) = '') ");
		}
		if (Objects.equals(this.emptyUniversitet, SysConstants.CODE_ZNACHENIE_DA)) {
			where.append(" and lice.universitet is null ");
		}
		if (Objects.equals(this.emptyBirthPlace, SysConstants.CODE_ZNACHENIE_DA)) {
			where.append(" and (lice.birth_place is null or trim(lice.birth_place) = '') ");
		}


		setSqlCount(" select count(*) " + from + where); // на този етап бройката е готова
		setSql(select.toString() + from + where);
		setSqlParameters(params);
	}

	public String getLnc() {
		return lnc;
	}

	public void setLnc(String lnc) {
		this.lnc = lnc;
	}

	public String getEgn() {
		return egn;
	}

	public void setEgn(String egn) {
		this.egn = egn;
	}

	public String getNames() {
		return names;
	}

	public void setNames(String names) {
		this.names = names;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Boolean getBezUdost() {
		return bezUdost;
	}

	public void setBezUdost(Boolean bezUdost) {
		this.bezUdost = bezUdost;
	}

	public String getRnZaiavStaj() {
		return rnZaiavStaj;
	}

	public void setRnZaiavStaj(String rnZaiavStaj) {
		this.rnZaiavStaj = rnZaiavStaj;
	}

	public boolean isRnZaiavStajEQ() {
		return rnZaiavStajEQ;
	}

	public void setRnZaiavStajEQ(boolean rnZaiavStajEQ) {
		this.rnZaiavStajEQ = rnZaiavStajEQ;
	}

	public Date getZaiavStajRegDateFrom() {
		return zaiavStajRegDateFrom;
	}

	public void setZaiavStajRegDateFrom(Date zaiavStajRegDateFrom) {
		this.zaiavStajRegDateFrom = zaiavStajRegDateFrom;
	}

	public Date getZaiavStajRegDateTo() {
		return zaiavStajRegDateTo;
	}

	public void setZaiavStajRegDateTo(Date zaiavStajRegDateTo) {
		this.zaiavStajRegDateTo = zaiavStajRegDateTo;
	}

	public String getRnZapStaj() {
		return rnZapStaj;
	}

	public void setRnZapStaj(String rnZapStaj) {
		this.rnZapStaj = rnZapStaj;
	}

	public boolean isRnZapStajEQ() {
		return rnZapStajEQ;
	}

	public void setRnZapStajEQ(boolean rnZapStajEQ) {
		this.rnZapStajEQ = rnZapStajEQ;
	}

	public Date getZapStajRegDateFrom() {
		return zapStajRegDateFrom;
	}

	public void setZapStajRegDateFrom(Date zapStajRegDateFrom) {
		this.zapStajRegDateFrom = zapStajRegDateFrom;
	}

	public Date getZapStajRegDateTo() {
		return zapStajRegDateTo;
	}

	public void setZapStajRegDateTo(Date zapStajRegDateTo) {
		this.zapStajRegDateTo = zapStajRegDateTo;
	}

	public String getRnZaiavIzp() {
		return rnZaiavIzp;
	}

	public void setRnZaiavIzp(String rnZaiavIzp) {
		this.rnZaiavIzp = rnZaiavIzp;
	}

	public boolean isRnZaiavIzpEQ() {
		return rnZaiavIzpEQ;
	}

	public void setRnZaiavIzpEQ(boolean rnZaiavIzpEQ) {
		this.rnZaiavIzpEQ = rnZaiavIzpEQ;
	}

	public Date getZaiavIzpRegDateFrom() {
		return zaiavIzpRegDateFrom;
	}

	public void setZaiavIzpRegDateFrom(Date zaiavIzpRegDateFrom) {
		this.zaiavIzpRegDateFrom = zaiavIzpRegDateFrom;
	}

	public Date getZaiavIzpRegDateTo() {
		return zaiavIzpRegDateTo;
	}

	public void setZaiavIzpRegDateTo(Date zaiavIzpRegDateTo) {
		this.zaiavIzpRegDateTo = zaiavIzpRegDateTo;
	}

	public Date getZaiavIzpPodDateFrom() {
		return zaiavIzpPodDateFrom;
	}

	public void setZaiavIzpPodDateFrom(Date zaiavIzpPodDateFrom) {
		this.zaiavIzpPodDateFrom = zaiavIzpPodDateFrom;
	}

	public Date getZaiavIzpPodDateTo() {
		return zaiavIzpPodDateTo;
	}

	public void setZaiavIzpPodDateTo(Date zaiavIzpPodDateTo) {
		this.zaiavIzpPodDateTo = zaiavIzpPodDateTo;
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

	public Date getUdostRegDateFrom() {
		return udostRegDateFrom;
	}

	public void setUdostRegDateFrom(Date udostRegDateFrom) {
		this.udostRegDateFrom = udostRegDateFrom;
	}

	public Date getUdostRegDateTo() {
		return udostRegDateTo;
	}

	public void setUdostRegDateTo(Date udostRegDateTo) {
		this.udostRegDateTo = udostRegDateTo;
	}

	public Integer getOsnInstitution() {
		return osnInstitution;
	}

	public void setOsnInstitution(Integer osnInstitution) {
		this.osnInstitution = osnInstitution;
	}

	public Integer getUniversitet() {
		return universitet;
	}

	public void setUniversitet(Integer universitet) {
		this.universitet = universitet;
	}

	public Integer getAddrEkatte() {
		return addrEkatte;
	}

	public void setAddrEkatte(Integer addrEkatte) {
		this.addrEkatte = addrEkatte;
	}

	public Integer getBroiIzpit() {
		return broiIzpit;
	}

	public void setBroiIzpit(Integer broiIzpit) {
		this.broiIzpit = broiIzpit;
	}

	public Integer getDo2019() {
		return do2019;
	}

	public void setDo2019(Integer do2019) {
		this.do2019 = do2019;
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

	public String getEgnlnc() {
		return egnlnc;
	}

	public void setEgnlnc(String egnlnc) {
		this.egnlnc = egnlnc;
	}

	public Date getStatusDateFrom() {
		return statusDateFrom;
	}

	public void setStatusDateFrom(Date statusDateFrom) {
		this.statusDateFrom = statusDateFrom;
	}

	public Date getStatusDateTo() {
		return statusDateTo;
	}

	public void setStatusDateTo(Date statusDateTo) {
		this.statusDateTo = statusDateTo;
	}

	public Integer getEmptyAddrEkatte() {
		return emptyAddrEkatte;
	}

	public void setEmptyAddrEkatte(Integer emptyAddrEkatte) {
		this.emptyAddrEkatte = emptyAddrEkatte;
	}

	public Integer getEmptyAddrText() {
		return emptyAddrText;
	}

	public void setEmptyAddrText(Integer emptyAddrText) {
		this.emptyAddrText = emptyAddrText;
	}

	public Integer getEmptyUniversitet() {
		return emptyUniversitet;
	}

	public void setEmptyUniversitet(Integer emptyUniversitet) {
		this.emptyUniversitet = emptyUniversitet;
	}

	public Integer getEmptyBirthPlace() {
		return emptyBirthPlace;
	}

	public void setEmptyBirthPlace(Integer emptyBirthPlace) {
		this.emptyBirthPlace = emptyBirthPlace;
	}
}
