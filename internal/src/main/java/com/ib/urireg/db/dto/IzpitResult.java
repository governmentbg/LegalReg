package com.ib.urireg.db.dto;

import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import jakarta.persistence.*;

import static com.ib.urireg.system.UriregConstants.*;

@Entity
@Table(name = "izpit_result")
public class IzpitResult extends TrackableEntity implements AuditExt {
	private static final long serialVersionUID = -8897048253138945637L;

	@SequenceGenerator(name = "IzpitResult", sequenceName = "seq_izpit_result", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IzpitResult")
	@Column(name = "result_id", unique = true, nullable = false)
	private Integer id;

	@Column(name = "izpit_id", updatable = false)
	@JournalAttr(label = "izpit_id", defaultText = "Изпит", codeObject = CODE_ZNACHENIE_JOURNAL_IZPIT)
	private Integer izpitId; // int8

	@Column(name = "staj_id")
	private Integer stajId; // int8

	@Column(name = "lice_id")
	@JournalAttr(label = "lice_id", defaultText = "Лице", codeObject = CODE_ZNACHENIE_JOURNAL_LICE)
	private Integer liceId; // int8

	@Column(name = "zaiav_izp_id")
	@JournalAttr(label = "zaiav_izp_id", defaultText = "Заявление за изпит", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer zaiavIzpId; // int8

	@Column(name = "test_pismena_nom")
	@JournalAttr(label = "test_pismena_nom", defaultText = "№ на писмена работа - тест")
	private Integer testPismenaNom; // int8

	@Column(name = "test_correct_answers")
	@JournalAttr(label = "test_correct_answers", defaultText = "Бр. верни отговори")
	private Integer testCorrectAnswers; // int8

	@Column(name = "test_result")
	@JournalAttr(label = "test_result", defaultText = "Резултат от тест", classifID = "" + CODE_CLASSIF_IZPIT_RESULT)
	private Integer testResult; // int8

	@Column(name = "test_remark")
	@JournalAttr(label = "test_remark", defaultText = "Забележка - тест")
	private String testRemark; // VARCHAR(1000)

	@Column(name = "case_pismena_nom")
	@JournalAttr(label = "case_pismena_nom", defaultText = "№ на писмена работа - казус")
	private Integer casePismenaNom; // int8

	@Column(name = "case_result")
	@JournalAttr(label = "case_result", defaultText = "Резултат от казус", classifID = "" + CODE_CLASSIF_IZPIT_RESULT)
	private Integer caseResult; // int8

	@Column(name = "case_remark")
	@JournalAttr(label = "case_remark", defaultText = "Забележка - казус")
	private String caseRemark; // VARCHAR(1000)

	@Column(name = "result_info")
	@JournalAttr(label = "result_info", defaultText = "Допълнителна информация")
	private String resultInfo; // varchar

	// това е да се журналира действието в контекста на заповед
	@Transient
	private Integer zapIzpId;

	public IzpitResult() {
	}

	@Override
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getIzpitId() {
		return izpitId;
	}

	public void setIzpitId(Integer izpitId) {
		this.izpitId = izpitId;
	}

	public Integer getStajId() {
		return stajId;
	}

	public void setStajId(Integer stajId) {
		this.stajId = stajId;
	}

	public Integer getLiceId() {
		return liceId;
	}

	public void setLiceId(Integer liceId) {
		this.liceId = liceId;
	}

	public Integer getTestPismenaNom() {
		return testPismenaNom;
	}

	public void setTestPismenaNom(Integer testPismenaNom) {
		this.testPismenaNom = testPismenaNom;
	}

	public Integer getTestCorrectAnswers() {
		return testCorrectAnswers;
	}

	public void setTestCorrectAnswers(Integer testCorrectAnswers) {
		this.testCorrectAnswers = testCorrectAnswers;
	}

	public Integer getTestResult() {
		return testResult;
	}

	public void setTestResult(Integer testResult) {
		this.testResult = testResult;
	}

	public String getTestRemark() {
		return testRemark;
	}

	public void setTestRemark(String testRemark) {
		this.testRemark = testRemark;
	}

	public Integer getCasePismenaNom() {
		return casePismenaNom;
	}

	public void setCasePismenaNom(Integer casePismenaNom) {
		this.casePismenaNom = casePismenaNom;
	}

	public Integer getCaseResult() {
		return caseResult;
	}

	public void setCaseResult(Integer caseResult) {
		this.caseResult = caseResult;
	}

	public String getCaseRemark() {
		return caseRemark;
	}

	public void setCaseRemark(String caseRemark) {
		this.caseRemark = caseRemark;
	}

	public String getResultInfo() {
		return resultInfo;
	}

	public void setResultInfo(String resultInfo) {
		this.resultInfo = resultInfo;
	}

	public Integer getZaiavIzpId() {
		return zaiavIzpId;
	}

	public void setZaiavIzpId(Integer zaiavIzpId) {
		this.zaiavIzpId = zaiavIzpId;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		StringBuilder sb = new StringBuilder();

		sb.append("Лице(Id=" + this.getLiceId() + ")");

		if (this.testPismenaNom != null) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("Тест №" + this.testPismenaNom);
		}
		if (this.casePismenaNom != null) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("Казус №" + this.casePismenaNom);
		}

		return sb.toString();
	}

	@Override
	public Integer getCodeMainObject() {
		return CODE_ZNACHENIE_JOURNAL_IZPIT_RESULT;
	}

	public Integer getZapIzpId() {
		return zapIzpId;
	}

	public void setZapIzpId(Integer zapIzpId) {
		this.zapIzpId = zapIzpId;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		if (this.zapIzpId != null) {
			journal.setJoinedIdObject1(this.zapIzpId);
			journal.setJoinedCodeObject1(CODE_ZNACHENIE_JOURNAL_DOC);
		}

		journal.setJoinedIdObject2(this.liceId);
		journal.setJoinedCodeObject2(CODE_ZNACHENIE_JOURNAL_LICE);

		return journal;
	}
}
