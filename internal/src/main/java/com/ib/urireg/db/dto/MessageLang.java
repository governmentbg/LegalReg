package com.ib.urireg.db.dto;

import com.ib.system.SysConstants;
import com.ib.system.db.JournalAttr;
import jakarta.persistence.*;

@Entity
@Table(name = "message_lang")
public class MessageLang implements java.io.Serializable {
	private static final long serialVersionUID = 5947962773137127688L;

	@SequenceGenerator(name = "MessageLang", sequenceName = "seq_message_lang", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MessageLang")
	@Column(name = "id", unique = true, nullable = false)
	@JournalAttr(label = "id", defaultText = "Системен идентификатор", isId = "true")
	private Integer id;

	@Column(name = "message_id", insertable = false, updatable = false)
	private Integer messageId; // int8

	@Column(name = "lang")
	private Integer lang = SysConstants.CODE_LANG_BG; // int8

	@Column(name = "title")
	@JournalAttr(label = "title", defaultText = "Заглавие")
	private String title; // varchar(500)

	@Column(name = "message_text")
	@JournalAttr(label = "message_text", defaultText = "Текст")
	private String messageText; // text

	public MessageLang() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getMessageId() {
		return messageId;
	}

	public void setMessageId(Integer messageId) {
		this.messageId = messageId;
	}

	public Integer getLang() {
		return lang;
	}

	public void setLang(Integer lang) {
		this.lang = lang;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}
}
