package com.ib.urireg.db.dto;

import com.ib.urireg.system.UriregConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.List;

import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * Логика за попълване на шаблон
 */
@Entity
@Table(name = "shablon_logic")
public class ShablonLogic extends TrackableEntity implements AuditExt {

	private static final long serialVersionUID = 6616772917958272580L;

	@SequenceGenerator(name = "ShablonLogic", sequenceName = "seq_shablon_logic", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ShablonLogic")
	@Column(name = "id", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "doc_vid", defaultText = "Вид документ", classifID = "" + UriregConstants.CODE_CLASSIF_DOC_VID)
	@Column(name = "doc_vid")
	private Integer docVid;

	@Column(name = "filename")
	@JournalAttr(label = "filename", defaultText = "Име на файл")
	private String filename;

	@JournalAttr(label = "bookmarks", defaultText = "Bookmark в шаблон")
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "shablon_logic_id", referencedColumnName = "id", nullable = false)
	private List<ShablonBookmark> bookmarks;

	/** */
	public ShablonLogic() {
		super();
	}

	/** @return the bookmarks */
	public List<ShablonBookmark> getBookmarks() {
		return this.bookmarks;
	}

	@Override
	public Integer getCodeMainObject() {
		return UriregConstants.CODE_ZNACHENIE_JOURNAL_SHABLON_LOGIC;
	}

	/** @return the docVid */
	public Integer getDocVid() {
		return this.docVid;
	}

	/** @return the filename */
	public String getFilename() {
		return this.filename;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		return null;
	}

	/** @param bookmarks the bookmarks to set */
	public void setBookmarks(List<ShablonBookmark> bookmarks) {
		this.bookmarks = bookmarks;
	}

	/** @param docVid the docVid to set */
	public void setDocVid(Integer docVid) {
		this.docVid = docVid;
	}

	/** @param filename the filename to set */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		return journal;
	}
}
