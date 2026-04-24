package com.ib.urireg.db.dto;

import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.system.Constants;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLRestriction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AGREED;
import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC;

@Entity
@Table(name = "doc")
public class Doc extends TrackableEntity implements AuditExt {
	private static final long serialVersionUID = -1648212087687890675L;

	@SequenceGenerator(name = "Doc", sequenceName = "seq_doc", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Doc")
	@Column(name = "doc_id", unique = true, nullable = false)
	private Integer id;

	@Column(name = "doc_vid")
	@JournalAttr(label = "doc_vid", defaultText = "Вид на документ", classifID = "" + UriregConstants.CODE_CLASSIF_DOC_VID)
	private Integer docVid; // int8

	@Column(name = "zaiav_date")
	@JournalAttr(label = "zaiav_date", defaultText = "Дата на заявление")
	private Date zaiavDate; // timestamp

	@Column(name = "rn_doc")
	@JournalAttr(label = "rn_doc", defaultText = "Регистрационен номер")
	private String rnDoc; // VARCHAR(50)

	@Column(name = "doc_date")
	@JournalAttr(label = "doc_date", defaultText = "Дата на регистрация", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date docDate; // timestamp

	@Column(name = "code_izgotvil")
	@JournalAttr(label = "code_izgotvil", defaultText = "Изготвил", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer codeIzgotvil; // int8

	@Column(name = "code_podpisal")
	@JournalAttr(label = "code_podpisal", defaultText = "Подписал", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer codePodpisal; // int8

	// това долното е малко за проба дали работи, защото доста често има подобни връзки
	// и ако е ОК така ще може да се работи лесно и ще се спести доста код
	@ElementCollection
	@CollectionTable(name = "doc_referents", joinColumns = @JoinColumn(name = "doc_id"))
	@Column(name = "code_ref")
	@SQLRestriction(" role_ref = " + CODE_ZNACHENIE_DOC_REF_ROLE_AGREED)
	@SQLInsert(sql = "insert into doc_referents (doc_id, code_ref, role_ref) values (?, ?, " + CODE_ZNACHENIE_DOC_REF_ROLE_AGREED + ")")
	@JournalAttr(label = "codeSaglList", defaultText = "Съгласували", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private List<Integer> codeSaglList;

	@Column(name = "original")
	@JournalAttr(label = "original", defaultText = "Оригинал УД", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	private Integer original; // int8

	@Column(name = "predsedatel")
	@JournalAttr(label = "predsedatel", defaultText = "Комисия - председател")
	private String predsedatel; // VARCHAR(500)

	@Column(name = "members")
	@JournalAttr(label = "members", defaultText = "Членове на комисия")
	private String members; // varchar

	@Column(name = "status")
	@JournalAttr(label = "status", defaultText = "Статус", classifID = "" + UriregConstants.CODE_CLASSIF_DOC_STATUS)
	private Integer status; // int8

	@Column(name = "status_date")
	@JournalAttr(label = "status_date", defaultText = "Дата на статус", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date statusDate; // timestamp

	@Column(name = "status_pri")
	@JournalAttr(label = "status_pri", defaultText = "Причина за статус", classifID = "" + UriregConstants.CODE_CLASSIF_DOC_STATUS_PRI)
	private Integer statusPri; // int8

	@Column(name = "status_info")
	@JournalAttr(label = "status_info", defaultText = "Информация за статус")
	private String statusInfo; // VARCHAR(1000)

	@ElementCollection
	@CollectionTable(name = "doc_pril", joinColumns = @JoinColumn(name = "doc_id"))
	@Column(name = "pril_vid")
	@JournalAttr(label = "prilVidList", defaultText = "Предоставени документи", classifID = "" + UriregConstants.CODE_CLASSIF_DADENI_DOC)
	private List<Integer> prilVidList;

	@Column(name = "doc_info")
	@JournalAttr(label = "doc_info", defaultText = "Допълнителна информация")
	private String docInfo; // varchar

	// това е да се журналира действието на дока в контекста на обект
	@Transient
	private Integer joinedIdObject1;
	@Transient
	private Integer joinedCodeObject1;

	public Doc() {
	}

	@Override
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Date getZaiavDate() {
		return zaiavDate;
	}

	public void setZaiavDate(Date zaiavDate) {
		this.zaiavDate = zaiavDate;
	}

	public Integer getDocVid() {
		return docVid;
	}

	public void setDocVid(Integer docVid) {
		this.docVid = docVid;
	}

	public String getRnDoc() {
		return rnDoc;
	}

	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	public Date getDocDate() {
		return docDate;
	}

	public void setDocDate(Date docDate) {
		this.docDate = docDate;
	}

	public Integer getOriginal() {
		return original;
	}

	public void setOriginal(Integer original) {
		this.original = original;
	}

	public String getPredsedatel() {
		return predsedatel;
	}

	public void setPredsedatel(String predsedatel) {
		this.predsedatel = predsedatel;
	}

	public String getMembers() {
		return members;
	}

	public void setMembers(String members) {
		this.members = members;
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

	public String getDocInfo() {
		return docInfo;
	}

	public void setDocInfo(String docInfo) {
		this.docInfo = docInfo;
	}

	public Integer getCodeIzgotvil() {
		return codeIzgotvil;
	}

	public void setCodeIzgotvil(Integer codeIzgotvil) {
		this.codeIzgotvil = codeIzgotvil;
	}

	public Integer getCodePodpisal() {
		return codePodpisal;
	}

	public void setCodePodpisal(Integer codePodpisal) {
		this.codePodpisal = codePodpisal;
	}

	public List<Integer> getCodeSaglList() {
		return codeSaglList;
	}

	public void setCodeSaglList(List<Integer> codeSaglList) {
		this.codeSaglList = codeSaglList;
	}

	public List<Integer> getPrilVidList() {
		return prilVidList;
	}

	public void setPrilVidList(List<Integer> prilVidList) {
		this.prilVidList = prilVidList;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		StringBuilder sb = new StringBuilder();

		if (!SearchUtils.isEmpty(this.rnDoc)) {
			sb.append(this.rnDoc);
		}
		if (this.docDate != null) {
			if (sb.length() > 0) {
				sb.append("/");
			}
			sb.append(new SimpleDateFormat("dd.MM.yyyy").format(this.docDate));
		}

		if (this.zaiavDate != null) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("заявл.от " + new SimpleDateFormat("dd.MM.yyyy").format(this.zaiavDate));
		}

		return sb.toString();
	}

	@Override
	public Integer getCodeMainObject() {
		return CODE_ZNACHENIE_JOURNAL_DOC;
	}

	public Integer getJoinedIdObject1() {
		return joinedIdObject1;
	}

	public void setJoinedIdObject1(Integer joinedIdObject1) {
		this.joinedIdObject1 = joinedIdObject1;
	}

	public Integer getJoinedCodeObject1() {
		return joinedCodeObject1;
	}

	public void setJoinedCodeObject1(Integer joinedCodeObject1) {
		this.joinedCodeObject1 = joinedCodeObject1;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		if (this.joinedIdObject1 != null && this.joinedCodeObject1 != null) {
			journal.setJoinedIdObject1(this.joinedIdObject1);
			journal.setJoinedCodeObject1(this.joinedCodeObject1);
		}
		return journal;
	}
}
