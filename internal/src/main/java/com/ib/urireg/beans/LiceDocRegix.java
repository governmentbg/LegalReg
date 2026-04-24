package com.ib.urireg.beans;

import java.io.Serializable;
import java.util.Date;


public class LiceDocRegix implements Serializable {
	private static final long serialVersionUID = -810369829725306038L;


	private Integer id;

	private String egn;

	private String names;

	private String identityDocumentNumber;

	private Date issueDate;

	private String issuerPlace;

	private Date validDate;

	public LiceDocRegix() {
	}

	public LiceDocRegix(Integer id) {
		this.id = id;
	}

	public LiceDocRegix(String egn , String names, String identityDocumentNumber, Date issueDate, String issuerPlace, Date validDate) {
		this.egn = egn;
		this.names = names;
		this.identityDocumentNumber = identityDocumentNumber;
		this.issueDate = issueDate;
		this.issuerPlace = issuerPlace;
		this.validDate = validDate;

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getEgn() {
		return egn;
	}

	public void setEgn(String egn) {
		this.egn = egn;
	}

	public String getNames() {
		return names;
	}

	public void setNames(String names) {
		this.names = names;
	}

	public String getIdentityDocumentNumber() {
		return identityDocumentNumber;
	}

	public void setIdentityDocumentNumber(String identityDocumentNumber) {
		this.identityDocumentNumber = identityDocumentNumber;
	}

	public Date getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(Date issueDate) {
		this.issueDate = issueDate;
	}

	public String getIssuerPlace() {
		return issuerPlace;
	}

	public void setIssuerPlace(String issuerPlace) {
		this.issuerPlace = issuerPlace;
	}

	public Date getValidDate() {
		return validDate;
	}

	public void setValidDate(Date validDate) {
		this.validDate = validDate;
	}
}
