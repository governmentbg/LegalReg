package com.ib.urireg.db.dto;

import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

import static com.ib.urireg.system.UriregConstants.*;

@Entity
@Table(name = "message")
public class Message extends TrackableEntity implements AuditExt {
	private static final long serialVersionUID = 7558665182443557488L;

	@SequenceGenerator(name = "Message", sequenceName = "seq_message", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Message")
	@Column(name = "id", unique = true, nullable = false)
	private Integer id;

	@Column(name = "message_vid")
	@JournalAttr(label = "message_vid", defaultText = "Вид съобщение", classifID = "" + CODE_CLASSIF_MESSAGE_VID)
	private Integer messageVid; // int8

	@Column(name = "date_from")
	@JournalAttr(label = "date_from", defaultText = "Начална дата на публикуване", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date dateFrom; // timestamp

	@Column(name = "date_to")
	@JournalAttr(label = "date_to", defaultText = "Крайна дата на публикуване", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date dateTo; // timestamp

	@Column(name = "status")
	@JournalAttr(label = "status", defaultText = "Статус на съобщение", classifID = "" + CODE_CLASSIF_MESSAGE_STATUS)
	private Integer status; // int8

	@Column(name = "message_info")
	@JournalAttr(label = "message_info", defaultText = "Забележка/доп. информация")
	private String messageInfo; // varchar(5000)

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "message_id", referencedColumnName = "id", nullable = false)
	@JournalAttr(label = "messageLangs", defaultText = "Съобщение")
	private List<MessageLang> messageLangs;

	public Message() {
	}

	@Override
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getMessageVid() {
		return messageVid;
	}

	public void setMessageVid(Integer messageVid) {
		this.messageVid = messageVid;
	}

	public Date getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}

	public Date getDateTo() {
		return dateTo;
	}

	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getMessageInfo() {
		return messageInfo;
	}

	public void setMessageInfo(String messageInfo) {
		this.messageInfo = messageInfo;
	}

	@Override
	public Integer getCodeMainObject() {
		return CODE_ZNACHENIE_JOURNAL_MESSAGE;
	}

	public List<MessageLang> getMessageLangs() {
		return messageLangs;
	}

	public void setMessageLangs(List<MessageLang> messageLangs) {
		this.messageLangs = messageLangs;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		return journal;
	}
}
