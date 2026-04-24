package com.ib.urireg.db.dto;

import com.ib.indexui.system.Constants;
import com.ib.system.SysConstants;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import jakarta.persistence.*;

import java.util.Objects;

import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * Адрес на референт
 *
 * @author belev
 */
@Entity
@Table(name = "ADM_REF_ADDRS")
public class ReferentAddress extends TrackableEntity {
	/**  */
	private static final long serialVersionUID = 6743971282999605172L;

	@JournalAttr(label = "id", defaultText = "Системен идентификатор", isId = "true")
	@SequenceGenerator(name = "ReferentAddress", sequenceName = "SEQ_ADM_REF_ADDRS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ReferentAddress")
	@Column(name = "ADDR_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "CODE_REF", updatable = false)
	private Integer codeRef;

	@Column(name = "ADDR_TYPE")
	private Integer addrType;

	@JournalAttr(label = "global.country", defaultText = "Държава", classifID = "" + Constants.CODE_CLASSIF_COUNTRIES)
	@Column(name = "ADDR_COUNTRY")
	private Integer addrCountry;

	@JournalAttr(label = "missing.addrText", defaultText = "Адрес")
	@Column(name = "ADDR_TEXT")
	private String addrText;

	@JournalAttr(label = "missing.postCode", defaultText = "ПК")
	@Column(name = "POST_CODE")
	private String postCode;

	@JournalAttr(label = "regMailboxes.mailbox", defaultText = "Пощенска кутия")
	@Column(name = "POST_BOX")
	private String postBox;

	@JournalAttr(label = "missing.ekatte", defaultText = "Населено място", classifID = "" + SysConstants.CODE_CLASSIF_EKATTE)
	@Column(name = "EKATTE")
	private Integer ekatte;

	@Column(name = "RAION")
	private String raion;

	/**  */
	public ReferentAddress() {
		super();
	}

	/** */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ReferentAddress other = (ReferentAddress) obj;
		return Objects.equals(this.addrCountry, other.addrCountry) && Objects.equals(this.addrText, other.addrText) && Objects.equals(this.addrType, other.addrType)
			&& Objects.equals(this.codeRef, other.codeRef) && Objects.equals(this.ekatte, other.ekatte) && Objects.equals(this.id, other.id) && Objects.equals(this.postBox, other.postBox)
			&& Objects.equals(this.postCode, other.postCode) && Objects.equals(this.raion, other.raion);
	}

	/**
	 * Всички стрингови полета, които са празен стринг ги прави на null
	 */
	public void fixEmptyStringValues() {
		if (this.addrText != null && this.addrText.length() == 0) {
			this.addrText = null;
		}
		if (this.postCode != null && this.postCode.length() == 0) {
			this.postCode = null;
		}
		if (this.postBox != null && this.postBox.length() == 0) {
			this.postBox = null;
		}
		if (this.raion != null && this.raion.length() == 0) {
			this.raion = null;
		}
	}

	/** @return the addrCountry */
	public Integer getAddrCountry() {
		return this.addrCountry;
	}

	/** @return the addrText */
	public String getAddrText() {
		return this.addrText;
	}

	/** @return the addrType */
	public Integer getAddrType() {
		return this.addrType;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return null;
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the ekatte */
	public Integer getEkatte() {
		return this.ekatte;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the postBox */
	public String getPostBox() {
		return this.postBox;
	}

	/** @return the postCode */
	public String getPostCode() {
		return this.postCode;
	}

	/** @return the raion */
	public String getRaion() {
		return this.raion;
	}

	/** */
	@Override
	public int hashCode() {
		return Objects.hash(this.addrCountry, this.addrText, this.addrType, this.codeRef, this.ekatte, this.id, this.postBox, this.postCode, this.raion);
	}

	/** */
	@Override
	public boolean isAuditable() {
		return false; // ще се журналира в контекста на участника в процеса
	}

	/** @param addrCountry the addrCountry to set */
	public void setAddrCountry(Integer addrCountry) {
		this.addrCountry = addrCountry;
	}

	/** @param addrText the addrText to set */
	public void setAddrText(String addrText) {
		this.addrText = addrText;
	}

	/** @param addrType the addrType to set */
	public void setAddrType(Integer addrType) {
		this.addrType = addrType;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param ekatte the ekatte to set */
	public void setEkatte(Integer ekatte) {
		this.ekatte = ekatte;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param postBox the postBox to set */
	public void setPostBox(String postBox) {
		this.postBox = postBox;
	}

	/** @param postCode the postCode to set */
	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}

	/** @param raion the raion to set */
	public void setRaion(String raion) {
		this.raion = raion;
	}
}
