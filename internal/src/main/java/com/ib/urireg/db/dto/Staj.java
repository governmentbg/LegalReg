package com.ib.urireg.db.dto;

import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.urireg.system.UriregConstants;
import jakarta.persistence.*;

import java.util.Date;

import static com.ib.urireg.system.UriregConstants.*;

@Entity
@Table(name = "staj")
public class Staj extends TrackableEntity implements AuditExt {
	private static final long serialVersionUID = -1490394860924297510L;

	@SequenceGenerator(name = "Staj", sequenceName = "seq_staj", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Staj")
	@Column(name = "staj_id", unique = true, nullable = false)
	private Integer id;

	@Column(name = "lice_id")
	@JournalAttr(label = "lice_id", defaultText = "Лице", codeObject = CODE_ZNACHENIE_JOURNAL_LICE)
	private Integer liceId; // int8

	@Column(name = "zaiav_staj_id")
	@JournalAttr(label = "zaiav_staj_id", defaultText = "Заявление за стаж", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer zaiavStajId; // int8

	@Column(name = "zap_staj_id")
	@JournalAttr(label = "zap_staj_id", defaultText = "Заповед за стаж", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer zapStajId; // int8

	@Column(name = "staj_vid")
	@JournalAttr(label = "staj_vid", defaultText = "Вид на стаж", classifID = "" + CODE_CLASSIF_STAJ_VID)
	private Integer stajVid; // int8

	@Column(name = "mentor")
	@JournalAttr(label = "mentor", defaultText = "Наставник")
	private String mentor; // VARCHAR(500)

	@Column(name = "mentor_email")
	@JournalAttr(label = "mentor_email", defaultText = "Наставник имейл")
	private String mentorEmail; // VARCHAR(255)

	@Column(name = "osn_institution")
	@JournalAttr(label = "osn_institution", defaultText = "Място на провеждане на основния стаж", classifID = "" + UriregConstants.CODE_CLASSIF_INSTITUTION)
	private Integer osnInstitution; // int8

	@Column(name = "osn_start_date")
	@JournalAttr(label = "osn_start_date", defaultText = "Начална дата на общ стаж")
	private Date osnStartDate; // timestamp

	@Column(name = "osn_end_date")
	@JournalAttr(label = "osn_end_date", defaultText = "Крайна дата на общ стаж")
	private Date osnEndDate; // timestamp

	@Column(name = "pro_location")
	@JournalAttr(label = "pro_location", defaultText = "Място на провеждане на професионалния стаж")
	private String proLocation; // VARCHAR(1000)

	@Column(name = "pro_start_date")
	@JournalAttr(label = "pro_start_date", defaultText = "Начална дата на професионален стаж")
	private Date proStartDate; // timestamp

	@Column(name = "pro_end_date")
	@JournalAttr(label = "pro_end_date", defaultText = "Крайна дата на професионален стаж")
	private Date proEndDate; // timestamp

	@Column(name = "zaiav_izp_id")
	@JournalAttr(label = "zaiav_izp_id", defaultText = "Заявление за изпит", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer zaiavIzpId; // int8

	@Column(name = "staj_info")
	@JournalAttr(label = "staj_info", defaultText = "Допълнителна информация")
	private String stajInfo; // varchar

	public Staj() {
	}

	@Override
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getLiceId() {
		return liceId;
	}

	public void setLiceId(Integer liceId) {
		this.liceId = liceId;
	}

	public Integer getZaiavStajId() {
		return zaiavStajId;
	}

	public void setZaiavStajId(Integer zaiavStajId) {
		this.zaiavStajId = zaiavStajId;
	}

	public Integer getZapStajId() {
		return zapStajId;
	}

	public void setZapStajId(Integer zapStajId) {
		this.zapStajId = zapStajId;
	}

	public Integer getStajVid() {
		return stajVid;
	}

	public void setStajVid(Integer stajVid) {
		this.stajVid = stajVid;
	}

	public String getMentor() {
		return mentor;
	}

	public void setMentor(String mentor) {
		this.mentor = mentor;
	}

	public Integer getOsnInstitution() {
		return osnInstitution;
	}

	public void setOsnInstitution(Integer osnInstitution) {
		this.osnInstitution = osnInstitution;
	}

	public Date getOsnStartDate() {
		return osnStartDate;
	}

	public void setOsnStartDate(Date osnStartDate) {
		this.osnStartDate = osnStartDate;
	}

	public Date getOsnEndDate() {
		return osnEndDate;
	}

	public void setOsnEndDate(Date osnEndDate) {
		this.osnEndDate = osnEndDate;
	}

	public String getProLocation() {
		return proLocation;
	}

	public void setProLocation(String proLocation) {
		this.proLocation = proLocation;
	}

	public Date getProStartDate() {
		return proStartDate;
	}

	public void setProStartDate(Date proStartDate) {
		this.proStartDate = proStartDate;
	}

	public Date getProEndDate() {
		return proEndDate;
	}

	public void setProEndDate(Date proEndDate) {
		this.proEndDate = proEndDate;
	}

	public Integer getZaiavIzpId() {
		return zaiavIzpId;
	}

	public void setZaiavIzpId(Integer zaiavIzpId) {
		this.zaiavIzpId = zaiavIzpId;
	}

	public String getStajInfo() {
		return stajInfo;
	}

	public void setStajInfo(String stajInfo) {
		this.stajInfo = stajInfo;
	}

	public String getMentorEmail() {
		return mentorEmail;
	}

	public void setMentorEmail(String mentorEmail) {
		this.mentorEmail = mentorEmail;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		return "Лице(Id=" + this.liceId + "), Заявление(Id=" + this.zaiavStajId + ")";
	}

	@Override
	public Integer getCodeMainObject() {
		return CODE_ZNACHENIE_JOURNAL_STAJ;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedIdObject1(this.liceId);
		journal.setJoinedCodeObject1(CODE_ZNACHENIE_JOURNAL_LICE);

//		if (this.zapStajId != null) {
//			journal.setJoinedIdObject2(this.zapStajId);
//			journal.setJoinedCodeObject2(CODE_ZNACHENIE_JOURNAL_DOC);
//		}

		return journal;
	}
}
