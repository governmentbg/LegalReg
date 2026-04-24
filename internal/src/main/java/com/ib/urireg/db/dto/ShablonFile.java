package com.ib.urireg.db.dto;

import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * Фаил за попълване на шаблон
 */
@Entity
@Table(name = "shablon_logic") // !! върху същата таблица е !!!
public class ShablonFile extends TrackableEntity implements AuditExt {

	private static final long serialVersionUID = -5981365142481373820L;

	@Id
	@Column(name = "id", unique = true, nullable = false)
	private Integer id;

	@Column(name = "filename")
	@JournalAttr(label = "filename", defaultText = "Име на файл")
	private String filename;

	@JournalAttr(label = "contentType", defaultText = "Тип на съдържанието")
	@Column(name = "content_type")
	private String contentType;

	@Column(name = "content")
	private byte[] content; // @XmlTransient в getter, за да не пълни xml-a

	/** */
	public ShablonFile() {
		super();
	}

	@Override
	public Integer getCodeMainObject() {
		return null;
	}

	/** @return the content */
	@XmlTransient
	public byte[] getContent() {
		return this.content;
	}

	/** @return the contentType */
	public String getContentType() {
		return this.contentType;
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

	@Override
	public boolean isAuditable() {
		return false;
	}

	/** @param content the content to set */
	public void setContent(byte[] content) {
		this.content = content;
	}

	/** @param contentType the contentType to set */
	public void setContentType(String contentType) {
		this.contentType = contentType;
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
