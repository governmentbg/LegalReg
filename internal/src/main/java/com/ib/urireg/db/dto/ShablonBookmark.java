package com.ib.urireg.db.dto;

import com.ib.urireg.udostDocs.StringListConverter;
import com.ib.system.db.JournalAttr;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.List;

import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * Bookmark в шаблон
 */
@Entity
@Table(name = "shablon_bookmarks")
public class ShablonBookmark implements Serializable {

	/**
	 * Възможните видове bookmark-ове, които може да се попълват в word-овския документ. <br/>
	 * Ползват се само ако {@link #fillStrategy} = {@link FillStrategies#NORMAL}
	 */
	public enum BookmarkTypes {
		STRING, LIST, TABLE, TREE
	}

	/**
	 * Начинът, по който ще се попълват данните. <br/>
	 *   {@link #NORMAL} - 99% от случаите се ползва това. Значи, че се избира type,  име на метод и готово. <br/>
	 *   {@link #ADVANCED} - рядък случай. Позвва се, когато букмаркът трябва да се попълва по специален начин,
	 *   	да предлага редакция / ръчно въвеждане, да трябва да извика друг метод след генериране, да сетне
	 *   	друг букмарк и т.н. Например - букмаркът "подписал" се попълва ръчно чрез компонент за избор от
	 *   	адм. структура. След това попълва стойност и на букмарка "длъжност на подписал", а накрая след
	 *   	генериране на документа трябва да извика специален метод, който да въведе подписалия в обекта вписване. <br/>
	 *   {@link #PASSIVE} - рядък случай. Ползва се, когато друг ADVANCED букмарк попълва този.
	 *   	Например - букмаркът "подписал" ще сетва стойност и на себе си и на букмарка "длъжност на подписал".
	 *   	Не се избира нищо допълнително.
	 */
	public enum FillStrategies {
		NORMAL, ADVANCED, PASSIVE, CUSTOM_IMPL, CONTAINER, SIGN_FIELD
	}

	public enum FillComponents {
		ADM_STRUCT
	}

	private static final long serialVersionUID = 6963983748534944465L;

	@SequenceGenerator(name = "ShablonBookmark", sequenceName = "seq_shablon_bookmarks", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ShablonBookmark")
	@Column(name = "id", unique = true, nullable = false)
	@JournalAttr(label = "id", defaultText = "Системен идентификатор", isId = "true")
	private Integer id;

	@Column(name = "shablon_logic_id", insertable = false, updatable = false)
	private Integer shablonLogicId;

	@Column(name = "label")
	@JournalAttr(label = "label", defaultText = "Bookmark")
	private String label; // (varchar(100))

	@Column(name = "bookmark_type")
	@Enumerated(EnumType.STRING)
	@JournalAttr(label = "bookmark_type", defaultText = "Тип")
	private BookmarkTypes bookmarkType; // (varchar(100))

	@Column(name = "method_name")
	@JournalAttr(label = "method_name", defaultText = "Име на метод")
	private String methodName; // (varchar(100))

	@Column(name = "description")
	@JournalAttr(label = "description", defaultText = "Описание")
	private String description; // (varchar(255))

	@Column(name = "fill_strategy")
	@Enumerated(EnumType.STRING)
	@JournalAttr(label = "fill_strategy", defaultText = "Начин на попълване")
	private FillStrategies fillStrategy; // varchar(100)

	@Column(name = "fill_component")
	@Enumerated(EnumType.STRING)
	@JournalAttr(label = "fill_component", defaultText = "Компонента за попълване")
	private FillComponents fillComponent; // varchar(100)

	@Column(name = "fills_also")
	@Convert(converter = StringListConverter.class)
	@JournalAttr(label = "fills_also", defaultText = "Имена на други букмаркове, които се попълват от този")
	private List<String> fillsAlso; // varchar(255)

	@Column(name = "oncomplete_method_name")
	@JournalAttr(label = "oncomplete_method_name", defaultText = "Име на метод, който да се изпълни след генериране на файла")
	private String onCompleteMethodName;

	@Column(name = "default_value_method_name")
	@JournalAttr(label = "default_value_method_name", defaultText = "Име на метод, който да генерира начална стойност по подразбиране")
	private String defaultValueMethodName;

	/**  */
	public ShablonBookmark() {
		super();
		this.fillStrategy = FillStrategies.NORMAL;
	}

	@Override
	public String toString() {
		return "Bookmark: [id: " + this.getId()
				+ ", shablonLogicId: " + this.getShablonLogicId()
				+ ", label: " + this.getLabel()
				+ ", type: " + this.getBookmarkType()
				+ ", methodName: " + this.getMethodName()
				+ ", description: " + this.getDescription()
				+ ", fillStrategy: " + this.getFillStrategy()
				+ ", fillComponent: " + this.getFillComponent()
				+ ", fillsAlso: " + this.getFillsAlso()
				+ ", onCompleteMethodName: " + this.getOnCompleteMethodName()
				+ ", defaultValueMethodName: " + this.getDefaultValueMethodName() + "]";
	}

	/** @return the bookmarkType */
	public BookmarkTypes getBookmarkType() {
		return this.bookmarkType;
	}

	/** @return the description */
	public String getDescription() {
		return this.description;
	}

	/** @return the id */
	public Integer getId() {
		return this.id;
	}

	/** @return the label */
	public String getLabel() {
		return this.label;
	}

	/** @return the methodName */
	public String getMethodName() {
		return this.methodName;
	}

	/** @return the shablonLogicId */
	public Integer getShablonLogicId() {
		return this.shablonLogicId;
	}

	/** @return the fillStrategy */
	public FillStrategies getFillStrategy() {
		return fillStrategy;
	}

	/** @return the fillComponent */
	public FillComponents getFillComponent() {
		return fillComponent;
	}

	/** @return the fillsAlso */
	public List<String> getFillsAlso() {
		return this.fillsAlso;
	}

	/** @return the onCompleteMethodName */
	public String getOnCompleteMethodName() {
		return this.onCompleteMethodName;
	}

	/** @return the defaultValueMethodName */
	public String getDefaultValueMethodName() {
		return this.defaultValueMethodName;
	}

	/** @param bookmarkType the bookmarkType to set */
	public void setBookmarkType(BookmarkTypes bookmarkType) {
		this.bookmarkType = bookmarkType;
	}

	/** @param description the description to set */
	public void setDescription(String description) {
		this.description = description;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param label the label to set */
	public void setLabel(String label) {
		this.label = label;
	}

	/** @param methodName the methodName to set */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/** @param shablonLogicId the shablonLogicId to set */
	public void setShablonLogicId(Integer shablonLogicId) {
		this.shablonLogicId = shablonLogicId;
	}

	/** @param fillStrategy the fillStrategy to set */
	public void setFillStrategy(FillStrategies fillStrategy) {
		this.fillStrategy = fillStrategy;
	}

	/** @param fillComponent the fillComponent to set */
	public void setFillComponent(FillComponents fillComponent) {
		this.fillComponent = fillComponent;
	}

	/** @param fillsAlso the fillsAlso to set */
	public void setFillsAlso(List<String> fillsAlso) {
		this.fillsAlso = fillsAlso;
	}

	/** @param onCompleteMethodName the onCompleteMethodName to set */
	public void setOnCompleteMethodName(String onCompleteMethodName) {
		this.onCompleteMethodName = onCompleteMethodName;
	}

	/** @param defaultValueMethodName the defaultValueMethodName to set */
	public void setDefaultValueMethodName(String defaultValueMethodName) {
		this.defaultValueMethodName = defaultValueMethodName;
	}

}
