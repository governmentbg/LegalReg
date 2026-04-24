package com.ib.urireg.db.dto;

import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.system.Constants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;
import jakarta.persistence.*;

import java.util.Date;

import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC;
import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_JOURNAL_LICE;

@Entity
@Table(name = "lice")
public class Lice extends TrackableEntity implements AuditExt {
	private static final long serialVersionUID = -810369829725306038L;

	@SequenceGenerator(name = "Lice", sequenceName = "seq_lice", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Lice")
	@Column(name = "lice_id", unique = true, nullable = false)
	private Integer id;

	@Column(name = "zaiav_staj_id")
	@JournalAttr(label = "zaiav_staj_id", defaultText = "Заявление за стаж", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer zaiavStajId; // int8

	@Column(name = "egn")
	@JournalAttr(label = "egn", defaultText = "ЕГН")
	private String egn; // VARCHAR(50)

	@Column(name = "lnc")
	@JournalAttr(label = "lnc", defaultText = "ЛНЧ")
	private String lnc; // VARCHAR(50)

	@Column(name = "firstname")
	@JournalAttr(label = "firstname", defaultText = "Име")
	private String firstname; // VARCHAR(255)

	@Column(name = "surname")
	@JournalAttr(label = "surname", defaultText = "Презиме")
	private String surname; // VARCHAR(255)

	@Column(name = "lastname")
	@JournalAttr(label = "lastname", defaultText = "Фамилия")
	private String lastname; // VARCHAR(255)

	@Column(name = "names")
	@JournalAttr(label = "names", defaultText = "Пълно име")
	private String names; // VARCHAR(500)

	@Column(name = "birth_date")
	@JournalAttr(label = "birth_date", defaultText = "Дата на раждане")
	private Date birthDate; // timestamp

	@Column(name = "birth_place")
	@JournalAttr(label = "birth_place", defaultText = "Място на раждане")
	private String birthPlace; // VARCHAR(500)

	@Column(name = "broi_izpit")
	@JournalAttr(label = "broi_izpit", defaultText = "Брой явявания на изпит")
	private Integer broiIzpit; // int8

	@Column(name = "do_2019")
	@JournalAttr(label = "do_2019", defaultText = "По законодателство до 2019г.")
	private Integer do2019; // int8

	@Column(name = "universitet")
	@JournalAttr(label = "universitet", defaultText = "Учебно заведение")
	private Integer universitet; // int8

	@Column(name = "addr_country")
	@JournalAttr(label = "addr_country", defaultText = "Държава", classifID = "" + Constants.CODE_CLASSIF_COUNTRIES)
	private Integer addrCountry; // int8

	@Column(name = "addr_oblast")
	@JournalAttr(label = "addr_oblast", defaultText = "Област")
	private String addrOblast; // VARCHAR(10)

	@Column(name = "addr_obstina")
	@JournalAttr(label = "addr_obstina", defaultText = "Община")
	private String addrObstina; // VARCHAR(10)

	@Column(name = "addr_ekatte")
	@JournalAttr(label = "addr_ekatte", defaultText = "Населено място", classifID = "" + Constants.CODE_CLASSIF_EKATTE)
	private Integer addrEkatte; // int8

	@Column(name = "addr_text")
	@JournalAttr(label = "addr_text", defaultText = "Улица/сграда и номер")
	private String addrText; // VARCHAR(1000)

	@Column(name = "phone")
	@JournalAttr(label = "phone", defaultText = "Телефон")
	private String phone; // VARCHAR(100)

	@Column(name = "email")
	@JournalAttr(label = "email", defaultText = "Имейл адрес")
	private String email; // VARCHAR(255)

	@Column(name = "last_zaiav_staj_id")
	@JournalAttr(label = "last_zaiav_staj_id", defaultText = "Последно заявление за стаж", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer lastZaiavStajId; // int8

	@Column(name = "udost_id")
	@JournalAttr(label = "udost_id", defaultText = "Удостоверителен документ", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer udostId; // int8

	@Column(name = "status")
	@JournalAttr(label = "status", defaultText = "Статус", classifID = "" + UriregConstants.CODE_CLASSIF_LICE_STATUS)
	private Integer status; // int8

	@Column(name = "status", insertable = false, updatable = false)
	private Integer dbStatus; // int8

	@Column(name = "status_date")
	@JournalAttr(label = "status_date", defaultText = "Дата на статус", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date statusDate; // timestamp

	@Column(name = "status_pri")
	@JournalAttr(label = "status_pri", defaultText = "Причина за статус", classifID = "" + UriregConstants.CODE_CLASSIF_LICE_PRI)
	private Integer statusPri; // int8

	@Column(name = "status_info")
	@JournalAttr(label = "status_info", defaultText = "Информация за статус")
	private String statusInfo; // VARCHAR(1000)

	@Column(name = "lice_info")
	@JournalAttr(label = "lice_info", defaultText = "Допълнителна информация")
	private String liceInfo; // varchar

	public Lice() {
	}

	@Override
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getZaiavStajId() {
		return zaiavStajId;
	}

	public void setZaiavStajId(Integer zaiavStajId) {
		this.zaiavStajId = zaiavStajId;
	}

	public String getEgn() {
		return egn;
	}

	public void setEgn(String egn) {
		this.egn = egn;
	}

	public String getLnc() {
		return lnc;
	}

	public void setLnc(String lnc) {
		this.lnc = lnc;
	}

	public String getNames() {
		return names;
	}

	public void setNames(String names) {
		this.names = names;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public String getBirthPlace() {
		return birthPlace;
	}

	public void setBirthPlace(String birthPlace) {
		this.birthPlace = birthPlace;
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

	public Integer getUniversitet() {
		return universitet;
	}

	public void setUniversitet(Integer universitet) {
		this.universitet = universitet;
	}

	public Integer getAddrCountry() {
		return addrCountry;
	}

	public void setAddrCountry(Integer addrCountry) {
		this.addrCountry = addrCountry;
	}

	public String getAddrOblast() {
		return addrOblast;
	}

	public void setAddrOblast(String addrOblast) {
		this.addrOblast = addrOblast;
	}

	public String getAddrObstina() {
		return addrObstina;
	}

	public void setAddrObstina(String addrObstina) {
		this.addrObstina = addrObstina;
	}

	public Integer getAddrEkatte() {
		return addrEkatte;
	}

	public void setAddrEkatte(Integer addrEkatte) {
		this.addrEkatte = addrEkatte;
	}

	public String getAddrText() {
		return addrText;
	}

	public void setAddrText(String addrText) {
		this.addrText = addrText;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getUdostId() {
		return udostId;
	}

	public void setUdostId(Integer udostId) {
		this.udostId = udostId;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Date getStatusDate() {
		return statusDate;
	}

	public void setStatusDate(Date statusDate) {
		this.statusDate = statusDate;
	}

	public Integer getStatusPri() {
		return statusPri;
	}

	public void setStatusPri(Integer statusPri) {
		this.statusPri = statusPri;
	}

	public String getStatusInfo() {
		return statusInfo;
	}

	public void setStatusInfo(String statusInfo) {
		this.statusInfo = statusInfo;
	}

	public String getLiceInfo() {
		return liceInfo;
	}

	public void setLiceInfo(String liceInfo) {
		this.liceInfo = liceInfo;
	}

	public Integer getLastZaiavStajId() {
		return lastZaiavStajId;
	}

	public void setLastZaiavStajId(Integer lastZaiavStajId) {
		this.lastZaiavStajId = lastZaiavStajId;
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

	@Override
	public String getIdentInfo() throws DbErrorException {
		StringBuilder sb = new StringBuilder();

		if (this.egn != null) {
			sb.append(this.egn);
		} else if (this.lnc != null) {
			sb.append(this.lnc);
		}
		if (!SearchUtils.isEmpty(this.names)) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(this.names);
		}

		return sb.toString();
	}

	@Override
	public Integer getCodeMainObject() {
		return CODE_ZNACHENIE_JOURNAL_LICE;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		return journal;
	}

	public Integer getDbStatus() {
		return dbStatus;
	}
	public void setDbStatus(Integer dbStatus) {
		this.dbStatus = dbStatus;
	}
}
