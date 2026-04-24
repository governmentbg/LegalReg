package com.ib.urireg.beans;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.utils.DateUtils;
import com.ib.urireg.search.DossierSearch;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named("dossierFilterList")
@ViewScoped
public class DossierFilterList extends IndexUIbean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DossierFilterList.class);

    private LazyDataModelSQL2Array dossierList;
    private Integer periodZaiavStaj;
    private Integer periodZapStaj;
    private Integer periodZaiavIzp;
    private Integer periodUd;
    private Integer periodPodavaneIspit;
    private Integer periodStat;
    private DossierSearch dossSearch;
    private List<Integer> visitedIds;


    @PostConstruct
    public void init() {
        dossSearch = new DossierSearch();
        visitedIds = new ArrayList<>();

        String udostVal = JSFUtils.readCookie("udostCookie");
        if (udostVal.isEmpty()) {
            actionDefSearch();
        }else{
            readCookieValues();
        }
    }

    public void changePeriod() {
        LOGGER.debug("changePeriod");
        if (this.periodZaiavStaj != null) {
            LOGGER.debug("periodZaiavStaj = {}", this.periodZaiavStaj);
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodZaiavStaj);
            dossSearch.setZaiavStajRegDateFrom(di[0]);
            dossSearch.setZaiavStajRegDateTo(di[1]);

        } else {
            dossSearch.setZaiavStajRegDateFrom(null);
            dossSearch.setZaiavStajRegDateTo(null);
        }

        if (this.periodStat != null) {
            LOGGER.debug("periodStat = {}", this.periodStat);
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodStat);
            dossSearch.setStatusDateFrom(di[0]);
            dossSearch.setStatusDateTo(di[1]);
        } else {
            dossSearch.setStatusDateFrom(null);
            dossSearch.setStatusDateTo(null);
        }

        if (this.periodZapStaj != null) {
            LOGGER.debug("periodZapStaj = {}", this.periodZapStaj);
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodZapStaj);
            dossSearch.setZapStajRegDateFrom(di[0]);
            dossSearch.setZapStajRegDateTo(di[1]);
        } else {
            dossSearch.setZapStajRegDateFrom(null);
            dossSearch.setZapStajRegDateTo(null);
        }

        if (this.periodZaiavIzp != null) {
            LOGGER.debug("periodZaiavIzp = {}", this.periodZaiavIzp);
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodZaiavIzp);
            dossSearch.setZaiavIzpRegDateFrom(di[0]);
            dossSearch.setZaiavIzpRegDateTo(di[1]);
        } else {
            dossSearch.setZaiavIzpRegDateFrom(null);
            dossSearch.setZaiavIzpRegDateTo(null);
        }

        if (this.periodUd != null) {
            LOGGER.debug("periodUd = {}", this.periodUd);
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodUd);
            dossSearch.setUdostRegDateFrom(di[0]);
            dossSearch.setUdostRegDateTo(di[1]);
        } else {
            dossSearch.setUdostRegDateFrom(null);
            dossSearch.setUdostRegDateTo(null);
        }

        if (this.periodPodavaneIspit != null) {
            LOGGER.debug("periodPodavaneIspit = {}", this.periodPodavaneIspit);
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodPodavaneIspit);
            dossSearch.setZaiavIzpPodDateFrom(di[0]);
            dossSearch.setZaiavIzpPodDateTo(di[1]);
        } else {
            dossSearch.setZaiavIzpPodDateFrom(null);
            dossSearch.setZaiavIzpPodDateTo(null);
        }

    }


    public void changePeriodStat(String periodName) {
        if (periodName == null) {
            return;
        }
        switch (periodName) {
            case "periodZaiavStaj":
                this.setPeriodZaiavStaj(null);
                break;
            case "periodZapStaj":
                this.setPeriodZapStaj(null);
                break;
            case "periodZaiavIzp":
                this.setPeriodZaiavIzp(null);
                break;
            case "periodUd":
                this.setPeriodUd(null);
                break;
            case "periodPodavaneIspit":
                this.setPeriodPodavaneIspit(null);
                break;
            case "periodStat":
                this.setPeriodStat(null);
        }
    }

    public void actionDefSearch() {
        dossSearch.setBezUdost(true);
        dossSearch.buildFilterQuery();
        dossierList = new LazyDataModelSQL2Array(dossSearch, "a6 DESC");
    }

    public void actionSearch() {
        LOGGER.debug("actionSearch");
        visitedIds = new ArrayList<>();
        if (dossSearch.getUdostRegDateFrom() != null && dossSearch.getUdostRegDateTo() != null) {
            if (dossSearch.getUdostRegDateFrom().after(dossSearch.getUdostRegDateTo())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                getMessageResourceString(beanMessages, "dossier.dateFromBeforeDateTo"), ""));
                return;
            }
        }

        if (dossSearch.getZaiavIzpRegDateFrom() != null && dossSearch.getZaiavIzpRegDateTo() != null) {
            if (dossSearch.getZaiavIzpRegDateFrom().after(dossSearch.getZaiavIzpRegDateTo())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                getMessageResourceString(beanMessages, "dossier.dateFromBeforeDateTo"), ""));
                return;
            }
        }

        if (dossSearch.getZaiavIzpPodDateFrom() != null && dossSearch.getZaiavIzpPodDateTo() != null) {
            if (dossSearch.getZaiavIzpPodDateFrom().after(dossSearch.getZaiavIzpPodDateTo())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                getMessageResourceString(beanMessages, "dossier.dateFromBeforeDateTo"), ""));
                return;
            }
        }

        if (dossSearch.getZapStajRegDateFrom() != null && dossSearch.getZapStajRegDateTo() != null) {
            if (dossSearch.getZapStajRegDateFrom().after(dossSearch.getZapStajRegDateTo())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                getMessageResourceString(beanMessages, "dossier.dateFromBeforeDateTo"), ""));
                return;
            }
        }


        if (dossSearch.getZaiavStajRegDateFrom() != null && dossSearch.getZaiavStajRegDateTo() != null) {
            if (dossSearch.getZaiavStajRegDateFrom().after(dossSearch.getZaiavStajRegDateTo())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                getMessageResourceString(beanMessages, "dossier.dateFromBeforeDateTo"), ""));
                return;
            }
        }


        if (dossSearch.getStatusDateFrom() != null && dossSearch.getStatusDateTo() != null) {
            if (dossSearch.getStatusDateFrom().after(dossSearch.getStatusDateTo())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                getMessageResourceString(beanMessages, "dossier.dateFromBeforeDateTo"), ""));
                return;
            }
        }
        JSFUtils.saveCookie("udostCookie", String.valueOf(dossSearch.getBezUdost()), null, 12, null);
        dossSearch.buildFilterQuery();
        dossierList = new LazyDataModelSQL2Array(dossSearch, "a6 DESC");
    }

    public void readCookieValues() {
        String val = JSFUtils.readCookie("udostCookie");
        if (!val.isEmpty()) {
            if(val.equals("null")){
                dossSearch.setBezUdost(null);
            }else{
                dossSearch.setBezUdost(Boolean.parseBoolean(val));
                dossSearch.buildFilterQuery();
                dossierList = new LazyDataModelSQL2Array(dossSearch, "a6 DESC");
            }
        }
    }

    public void actionClear() {
        dossSearch = new DossierSearch();
        periodZaiavStaj = null;
        periodZapStaj = null;
        periodZaiavIzp = null;
        periodUd = null;
        periodPodavaneIspit = null;
        periodStat = null;
        dossierList = null;
        dossSearch.setBezUdost(true);
        visitedIds = new ArrayList<>();
    }

    public boolean isVisited(Integer id) {
        if (id == null) {
            return false;
        }
        return visitedIds.contains(id);
    }

    /**
     * Препраща към страницата с доисето
     *
     * @param idLice
     * @return
     */
    public String actionGoto(Integer idLice) {
        visitedIds.add(idLice);
        String result = "dossierPerson.xhtml?faces-redirect=true&idLice=" + idLice;
        return result;
    }

    /******************************** EXPORTS **********************************/

    /**
     * за експорт в excel XLSX - добавя заглавие и дата на изготвяне на справката и др.
     */
    public void postProcessXLSX(Object document) {

        String title = getMessageResourceString(LABELS, "dossier.title");
        new CustomExpPreProcess().postProcessXLSX(document, title, null, null, null);

    }

    /**
     * Метод за форматиране на дата за името на файла
     * @param dateFromPage
     * @return
     */
    public String getDocDateFormatted(Date dateFromPage) {
        if(dateFromPage != null) {
            return new SimpleDateFormat("yyyyMMdd").format(dateFromPage);
        } else{
            return "";
        }
    }

    public List<Integer> getVisitedIds() {
        return visitedIds;
    }

    public void setVisitedIds(List<Integer> visitedIds) {
        this.visitedIds = visitedIds;
    }

    public Integer getPeriodStat() {
        return periodStat;
    }

    public void setPeriodStat(Integer periodStat) {
        this.periodStat = periodStat;
    }

    public LazyDataModelSQL2Array getDossierList() {
        return dossierList;
    }

    public void setDossierList(LazyDataModelSQL2Array dossierList) {
        this.dossierList = dossierList;
    }

    public Integer getPeriodZaiavStaj() {
        return periodZaiavStaj;
    }

    public void setPeriodZaiavStaj(Integer periodZaiavStaj) {
        this.periodZaiavStaj = periodZaiavStaj;
    }

    public DossierSearch getDossSearch() {
        return dossSearch;
    }

    public void setDossSearch(DossierSearch dossSearch) {
        this.dossSearch = dossSearch;
    }

    public Integer getPeriodZapStaj() {
        return periodZapStaj;
    }

    public void setPeriodZapStaj(Integer periodZapStaj) {
        this.periodZapStaj = periodZapStaj;
    }

    public Integer getPeriodZaiavIzp() {
        return periodZaiavIzp;
    }

    public void setPeriodZaiavIzp(Integer periodZaiavIzp) {
        this.periodZaiavIzp = periodZaiavIzp;
    }

    public Integer getPeriodUd() {
        return periodUd;
    }

    public void setPeriodUd(Integer periodUd) {
        this.periodUd = periodUd;
    }

    public Integer getPeriodPodavaneIspit() {
        return periodPodavaneIspit;
    }

    public void setPeriodPodavaneIspit(Integer periodPodavaneIspit) {
        this.periodPodavaneIspit = periodPodavaneIspit;
    }
}
