package com.ib.urireg.db.dto;

import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.system.Constants;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import jakarta.persistence.*;

import java.util.Date;

import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_JOURNAL_REFERENT;
import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_REF_TYPE_NFL;
import static com.ib.system.utils.SearchUtils.trimToNULL;
import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * Участник в процеса
 *
 * @author belev
 */
@Entity
@Table(name = "ADM_REFERENTS")
public class Referent extends TrackableEntity implements AuditExt {
	/**  */
	private static final long serialVersionUID = 8671296280803403457L;

	@SequenceGenerator(name = "Referent", sequenceName = "SEQ_ADM_REFERENTS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Referent")
	@Column(name = "REF_ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "missing.code", defaultText = "Код")
	@Column(name = "CODE", updatable = false)
	private Integer code;

	@JournalAttr(label = "admStruct.itemBefore", defaultText = "Предходен елемент", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "CODE_PREV")
	private Integer codePrev;

	@JournalAttr(label = "users.zveno", defaultText = "Звено", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "CODE_PARENT")
	private Integer codeParent;

	@Column(name = "CODE_CLASSIF")
	private Integer codeClassif;

	@JournalAttr(label = "missing.refType", defaultText = "Тип", classifID = "" + UriregConstants.CODE_CLASSIF_REF_TYPE)
	@Column(name = "REF_TYPE")
	private Integer refType;

	@Column(name = "REF_REGISTRATURA")
	private Integer refRegistratura;

	@JournalAttr(label = "refCorr.nameUL", defaultText = "Наименование")
	@Column(name = "REF_NAME")
	private String refName;

	@JournalAttr(label = "refCorr.nameLatinUL", defaultText = "Наименование на латиница")
	@Column(name = "REF_LATIN")
	private String refLatin;

	@JournalAttr(label = "refCorr.regCountry", defaultText = "Държава на регистрация", classifID = "" + Constants.CODE_CLASSIF_COUNTRIES)
	@Column(name = "REF_GRJ")
	private Integer refGrj;

	@JournalAttr(label = "docu.note", defaultText = "Забележка")
	@Column(name = "REF_INFO")
	private String refInfo;

	@JournalAttr(label = "refDeleg.dateFrom", defaultText = "От дата")
	@Temporal(TemporalType.DATE)
	@Column(name = "DATE_OT")
	private Date dateOt;

	@JournalAttr(label = "refDeleg.dateTo", defaultText = "До дата")
	@Temporal(TemporalType.DATE)
	@Column(name = "DATE_DO")
	private Date dateDo;

	@JournalAttr(label = "refCorr.taxOfficeNum", defaultText = "Данъчен служебен номер")
	@Column(name = "TAX_OFFICE_NO")
	private String taxOfficeNo;

	@JournalAttr(label = "admStruct.telefon", defaultText = "Телефон")
	@Column(name = "CONTACT_PHONE")
	private String contactPhone;

	@JournalAttr(label = "admStruct.email", defaultText = "e-mail")
	@Column(name = "CONTACT_EMAIL")
	private String contactEmail;

	@Column(name = "MAX_UPLOAD_SIZE")
	private Integer maxUploadSize;

	@JournalAttr(label = "regGrSluj.position", defaultText = "Длъжност", classifID = "" + Constants.CODE_CLASSIF_POSITION)
	@Column(name = "EMPL_POSITION")
	private Integer emplPosition;

	@JournalAttr(label = "admStruct.grDogovor", defaultText = "Граждански договор", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "EMPL_CONTRACT")
	private Integer emplContract;

	@JournalAttr(label = "admStruct.eik", defaultText = "ЕИК")
	@Column(name = "NFL_EIK")
	private String nflEik;

	@JournalAttr(label = "admStruct.egn", defaultText = "ЕГН")
	@Column(name = "FZL_EGN")
	private String fzlEgn;

	@JournalAttr(label = "missing.fzlLnc", defaultText = "ЛНЧ")
	@Column(name = "FZL_LNC")
	private String fzlLnc;

	@JournalAttr(label = "refCorr.fzlLnEs", defaultText = "ЛН от ЕС")
	@Column(name = "FZL_LN_ES")
	private String fzlLnEs;

	@JournalAttr(label = "missing.fzlBirthDate", defaultText = "Дата на раждане")
	@Temporal(TemporalType.DATE)
	@Column(name = "FZL_BIRTH_DATE")
	private Date fzlBirthDate;

	@Transient
	private transient Boolean auditable; // за да може да се включва и изключва журналирането

	@JournalAttr(label = "address", defaultText = "Контакти")
	@Transient
	private ReferentAddress		address;		// адреса в момента е 1:1. Ако се появи необходимост от множествени адреси, то
												// този ще си остане и в списък ще има другите адреси. Базата позволява, защото
												// адреса е в друга таблица.
	@Transient
	private transient Integer	dbAddressId;	// за да се знае имало ли адрес. може и цял обект копие да се използва, за да се
												// знае имало ли е реална промяна.

	// данни за елементи на административна структура, чиято промяна прави история
	@Transient
	private transient String	dbRefName;
	@Transient
	private transient Integer	dbRefRegistratura;
	@Transient
	private transient Integer	dbEmplPosition;
	@Transient
	private transient String	dbContactEmail;
	@Transient
	private transient Integer	dbEmplContract;

	/**  */
	public Referent() {
		super();
	}

	/**
	 * Всички стрингови полета, които са празен стринг ги прави на null
	 */
	public void fixEmptyStringValues() {
		this.nflEik = trimToNULL(this.nflEik);
		this.fzlEgn = trimToNULL(this.fzlEgn);
		this.fzlLnc = trimToNULL(this.fzlLnc);
		this.fzlLnEs = trimToNULL(this.fzlLnEs);
	}

	/** @return the address */
	public ReferentAddress getAddress() {
		return this.address;
	}

	/** @return the code */
	public Integer getCode() {
		return this.code;
	}

	/** @return the codeClassif */
	public Integer getCodeClassif() {
		return this.codeClassif;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return CODE_ZNACHENIE_JOURNAL_REFERENT;
	}

	/** @return the codeParent */
	public Integer getCodeParent() {
		return this.codeParent;
	}

	/** @return the codePrev */
	public Integer getCodePrev() {
		return this.codePrev;
	}

	/** @return the contactEmail */
	public String getContactEmail() {
		return this.contactEmail;
	}

	/** @return the contactPhone */
	public String getContactPhone() {
		return this.contactPhone;
	}

	/** @return the dateDo */
	public Date getDateDo() {
		return this.dateDo;
	}

	/** @return the dateOt */
	public Date getDateOt() {
		return this.dateOt;
	}

	/** @return the dbAddressId */
	public Integer getDbAddressId() {
		return this.dbAddressId;
	}

	/** @return the dbContactEmail */
	public String getDbContactEmail() {
		return this.dbContactEmail;
	}

	/** @return the dbEmplContract */
	public Integer getDbEmplContract() {
		return this.dbEmplContract;
	}

	/** @return the dbEmplPosition */
	public Integer getDbEmplPosition() {
		return this.dbEmplPosition;
	}

	/** @return the dbRefName */
	public String getDbRefName() {
		return this.dbRefName;
	}

	/** @return the dbRefRegistratura */
	public Integer getDbRefRegistratura() {
		return this.dbRefRegistratura;
	}

	/** @return the emplContract */
	public Integer getEmplContract() {
		return this.emplContract;
	}

	/** @return the emplPosition */
	public Integer getEmplPosition() {
		return this.emplPosition;
	}

	/** @return the fzlBirthDate */
	public Date getFzlBirthDate() {
		return this.fzlBirthDate;
	}

	/** @return the fzlEgn */
	public String getFzlEgn() {
		return this.fzlEgn;
	}

	/** @return the fzlLnc */
	public String getFzlLnc() {
		return this.fzlLnc;
	}

	/** @return the fzlLnEs */
	public String getFzlLnEs() {
		return this.fzlLnEs;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		boolean isNfl = this.refType != null && this.refType.equals(CODE_ZNACHENIE_REF_TYPE_NFL);

		String s;
		if (isNfl) {
			s = this.nflEik;
		} else {
			s = this.fzlEgn;
			if (s == null) {
				s = this.fzlLnc;
			}
		}

		StringBuilder ident = new StringBuilder();
		if (s != null) { // и все пак може да няма
			ident.append(s + " ");
		}
		ident.append(this.refName);

		return ident.toString();
	}

	/** @return the maxUploadSize */
	public Integer getMaxUploadSize() {
		return this.maxUploadSize;
	}

	/** @return the nflEik */
	public String getNflEik() {
		return this.nflEik;
	}

	/** @return the refGrj */
	public Integer getRefGrj() {
		return this.refGrj;
	}

	/** @return the refInfo */
	public String getRefInfo() {
		return this.refInfo;
	}

	/** @return the refLatin */
	public String getRefLatin() {
		return this.refLatin;
	}

	/** @return the refName */
	public String getRefName() {
		return this.refName;
	}

	/** @return the refRegistratura */
	public Integer getRefRegistratura() {
		return this.refRegistratura;
	}

	/** @return the refType */
	public Integer getRefType() {
		return this.refType;
	}

	/** @return the taxOfficeNo */
	public String getTaxOfficeNo() {
		return this.taxOfficeNo;
	}

	/** @return the auditable */
	@Override
	public boolean isAuditable() {
		return this.auditable == null ? super.isAuditable() : this.auditable.booleanValue();
	}

	/** @param address the address to set */
	public void setAddress(ReferentAddress address) {
		this.address = address;
	}

	/** @param auditable the auditable to set */
	public void setAuditable(Boolean auditable) {
		this.auditable = auditable;
	}

	/** @param code the code to set */
	public void setCode(Integer code) {
		this.code = code;
	}

	/** @param codeClassif the codeClassif to set */
	public void setCodeClassif(Integer codeClassif) {
		this.codeClassif = codeClassif;
	}

	/** @param codeParent the codeParent to set */
	public void setCodeParent(Integer codeParent) {
		this.codeParent = codeParent;
	}

	/** @param codePrev the codePrev to set */
	public void setCodePrev(Integer codePrev) {
		this.codePrev = codePrev;
	}

	/** @param contactEmail the contactEmail to set */
	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	/** @param contactPhone the contactPhone to set */
	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	/** @param dateDo the dateDo to set */
	public void setDateDo(Date dateDo) {
		this.dateDo = dateDo;
	}

	/** @param dateOt the dateOt to set */
	public void setDateOt(Date dateOt) {
		this.dateOt = dateOt;
	}

	/** @param dbAddressId the dbAddressId to set */
	public void setDbAddressId(Integer dbAddressId) {
		this.dbAddressId = dbAddressId;
	}

	/** @param dbContactEmail the dbContactEmail to set */
	public void setDbContactEmail(String dbContactEmail) {
		this.dbContactEmail = dbContactEmail;
	}

	/** @param dbEmplContract the dbEmplContract to set */
	public void setDbEmplContract(Integer dbEmplContract) {
		this.dbEmplContract = dbEmplContract;
	}

	/** @param dbEmplPosition the dbEmplPosition to set */
	public void setDbEmplPosition(Integer dbEmplPosition) {
		this.dbEmplPosition = dbEmplPosition;
	}

	/** @param dbRefName the dbRefName to set */
	public void setDbRefName(String dbRefName) {
		this.dbRefName = dbRefName;
	}

	/** @param dbRefRegistratura the dbRefRegistratura to set */
	public void setDbRefRegistratura(Integer dbRefRegistratura) {
		this.dbRefRegistratura = dbRefRegistratura;
	}

	/** @param emplContract the emplContract to set */
	public void setEmplContract(Integer emplContract) {
		this.emplContract = emplContract;
	}

	/** @param emplPosition the emplPosition to set */
	public void setEmplPosition(Integer emplPosition) {
		this.emplPosition = emplPosition;
	}

	/** @param fzlBirthDate the fzlBirthDate to set */
	public void setFzlBirthDate(Date fzlBirthDate) {
		this.fzlBirthDate = fzlBirthDate;
	}

	/** @param fzlEgn the fzlEgn to set */
	public void setFzlEgn(String fzlEgn) {
		this.fzlEgn = fzlEgn;
	}

	/** @param fzlLnc the fzlLnc to set */
	public void setFzlLnc(String fzlLnc) {
		this.fzlLnc = fzlLnc;
	}

	/** @param fzlLnEs the fzlLnEs to set */
	public void setFzlLnEs(String fzlLnEs) {
		this.fzlLnEs = fzlLnEs;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param maxUploadSize the maxUploadSize to set */
	public void setMaxUploadSize(Integer maxUploadSize) {
		this.maxUploadSize = maxUploadSize;
	}

	/** @param nflEik the nflEik to set */
	public void setNflEik(String nflEik) {
		this.nflEik = nflEik;
	}

	/** @param refGrj the refGrj to set */
	public void setRefGrj(Integer refGrj) {
		this.refGrj = refGrj;
	}

	/** @param refInfo the refInfo to set */
	public void setRefInfo(String refInfo) {
		this.refInfo = refInfo;
	}

	/** @param refLatin the refLatin to set */
	public void setRefLatin(String refLatin) {
		this.refLatin = refLatin;
	}

	/** @param refName the refName to set */
	public void setRefName(String refName) {
		this.refName = refName;
	}

	/** @param refRegistratura the refRegistratura to set */
	public void setRefRegistratura(Integer refRegistratura) {
		this.refRegistratura = refRegistratura;
	}

	/** @param refType the refType to set */
	public void setRefType(Integer refType) {
		this.refType = refType;
	}

	/** @param taxOfficeNo the taxOfficeNo to set */
	public void setTaxOfficeNo(String taxOfficeNo) {
		this.taxOfficeNo = taxOfficeNo;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getCode(), getIdentInfo());

		return journal;
	}
}
