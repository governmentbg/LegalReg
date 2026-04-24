package com.ib.urireg.soap;

import java.util.Date;


public class PersonWS implements java.io.Serializable  ,Cloneable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3506876818675555305L;
	
	private String egnLnch="";
	private String name="";  //трите отделно
	private String prezime="";
	private String familia="";
	private String upNomer="";
	private Date   upDate=new Date();
	private String lishenOtPravo=""; //да /не
    private String dublikat; //да /не



	public void PersonWS(){}

	public String getEgnLnch() {
		return egnLnch;
	}

	public void setEgnLnch(String egnLnch) {
		this.egnLnch = egnLnch;
	}

	public String getName() {
		return name;
	}

	public void setName(String names) {
		this.name = names;
	}

	public String getUpNomer() {
		return upNomer;
	}

	public void setUpNomer(String upNomer) {
		this.upNomer = upNomer;
	}

	public Date getUpDate() {
		return upDate;
	}

	public void setUpDate(Date upDate) {
		this.upDate = upDate;
	}

	public String getLishenOtPravo() {
		return lishenOtPravo;
	}

	public void setLishenOtPravo(String lishenOtPravo) {
		this.lishenOtPravo = lishenOtPravo;
	}

	public PersonWS clone() throws CloneNotSupportedException {
		return (PersonWS)super.clone();
	}

	public String getPrezime() {
		return prezime;
	}

	public void setPrezime(String prezime) {
		this.prezime = prezime;
	}

	public String getFamilia() {
		return familia;
	}

	public void setFamilia(String familia) {
		this.familia = familia;
	}

	public String getDublikat() {
		return dublikat;
	}

	public void setDublikat(String dublikat) {
		this.dublikat = dublikat;
	}
}
