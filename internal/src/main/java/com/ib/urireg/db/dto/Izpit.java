package com.ib.urireg.db.dto;

import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import jakarta.persistence.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC;
import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_JOURNAL_IZPIT;

@Entity
@Table(name = "izpit")
public class Izpit extends TrackableEntity implements AuditExt {
	private static final long serialVersionUID = -9211503942187511647L;

	@SequenceGenerator(name = "Izpit", sequenceName = "seq_izpit", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Izpit")
	@Column(name = "izpit_id", unique = true, nullable = false)
	private Integer id;

	@Column(name = "zap_izp_id")
	@JournalAttr(label = "zap_izp_id", defaultText = "Заповед за изпит", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer zapIzpId; // int8

	@Column(name = "test_date")
	@JournalAttr(label = "test_date", defaultText = "Дата и час на провеждане на тест", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date testDate; // timestamp

	@Column(name = "test_location")
	@JournalAttr(label = "test_location", defaultText = "Място на провеждане на тест")
	private String testLocation; // VARCHAR(1000)

	@Column(name = "test_prot_id")
	@JournalAttr(label = "test_prot_id", defaultText = "Протокол-тест", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer testProtId; // int8

	@Column(name = "case_date")
	@JournalAttr(label = "case_date", defaultText = "Дата и час на провеждане на казус", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date caseDate; // timestamp

	@Column(name = "case_location")
	@JournalAttr(label = "case_location", defaultText = "Място на провеждане на казус")
	private String caseLocation; // VARCHAR(1000)

	@Column(name = "case_prot_id")
	@JournalAttr(label = "case_prot_id", defaultText = "Протокол-казус", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer caseProtId; // int8

	@Column(name = "izpit_info")
	@JournalAttr(label = "izpit_info", defaultText = "Допълнителна информация")
	private String izpitInfo; // varchar


	public Izpit() {
	}

	@Override
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getZapIzpId() {
		return zapIzpId;
	}

	public void setZapIzpId(Integer zapIzpId) {
		this.zapIzpId = zapIzpId;
	}

	public Date getTestDate() {
		return testDate;
	}

	public void setTestDate(Date testDate) {
		this.testDate = testDate;
	}

	public String getTestLocation() {
		return testLocation;
	}

	public void setTestLocation(String testLocation) {
		this.testLocation = testLocation;
	}

	public Integer getTestProtId() {
		return testProtId;
	}

	public void setTestProtId(Integer testProtId) {
		this.testProtId = testProtId;
	}

	public Date getCaseDate() {
		return caseDate;
	}

	public void setCaseDate(Date caseDate) {
		this.caseDate = caseDate;
	}

	public String getCaseLocation() {
		return caseLocation;
	}

	public void setCaseLocation(String caseLocation) {
		this.caseLocation = caseLocation;
	}

	public Integer getCaseProtId() {
		return caseProtId;
	}

	public void setCaseProtId(Integer caseProtId) {
		this.caseProtId = caseProtId;
	}

	public String getIzpitInfo() {
		return izpitInfo;
	}

	public void setIzpitInfo(String izpitInfo) {
		this.izpitInfo = izpitInfo;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		StringBuilder sb = new StringBuilder();

		if (this.testDate != null) {
			sb.append("Тест: ");
			sb.append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(this.testDate));
		}
		if (this.caseDate != null) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("Казус: ");
			sb.append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(this.caseDate));
		}

		return sb.toString();
	}

	@Override
	public Integer getCodeMainObject() {
		return CODE_ZNACHENIE_JOURNAL_IZPIT;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		if (this.zapIzpId != null) {
			journal.setJoinedIdObject1(this.zapIzpId);
			journal.setJoinedCodeObject1(CODE_ZNACHENIE_JOURNAL_DOC);
		}
		return journal;
	}
}
