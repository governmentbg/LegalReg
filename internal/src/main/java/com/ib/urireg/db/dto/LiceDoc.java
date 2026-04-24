package com.ib.urireg.db.dto;

import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import jakarta.persistence.*;

import static com.ib.urireg.system.UriregConstants.*;

@Entity
@Table(name = "lice_doc")
public class LiceDoc extends TrackableEntity implements AuditExt {
	private static final long serialVersionUID = 2504173457379856432L;

	@SequenceGenerator(name = "LiceDoc", sequenceName = "seq_lice_doc", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LiceDoc")
	@Column(name = "id", unique = true, nullable = false)
	private Integer id;

	@Column(name = "lice_id")
	@JournalAttr(label = "lice_id", defaultText = "Лице", codeObject = CODE_ZNACHENIE_JOURNAL_LICE)
	private Integer liceId; // int8

	@Column(name = "doc_id")
	@JournalAttr(label = "doc_id", defaultText = "Документ", codeObject = CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer docId; // int8

	public LiceDoc(Integer liceId, Integer docId) {
		this.liceId = liceId;
		this.docId = docId;
	}

	public LiceDoc() {
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

	public Integer getDocId() {
		return docId;
	}

	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		return "Лице(Id=" + this.liceId + "), Документ(Id=" + this.docId + ")";
	}

	@Override
	public Integer getCodeMainObject() {
		return CODE_ZNACHENIE_JOURNAL_LICE_DOC;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedIdObject1(this.liceId);
		journal.setJoinedCodeObject1(CODE_ZNACHENIE_JOURNAL_LICE);

		journal.setJoinedIdObject2(this.docId);
		journal.setJoinedCodeObject2(CODE_ZNACHENIE_JOURNAL_DOC);

		return journal;
	}
}
