package com.ib.urireg.beans;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.system.SysConstants;
import com.ib.urireg.search.DossierSearch;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named("regMigr")
@ViewScoped
public class RegMigr extends IndexUIbean {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RegMigr.class);

    private LazyDataModelSQL2Array dossierList;
    DossierSearch dossSearch;
    private List<Integer> visitedIds;

    private boolean emptyAddrEkatteBean = true;
    private boolean emptyAddrTextBean = true;
    private boolean emptyUniversitetBean = true;
    private boolean emptyBirthPlaceBean = true;

    @PostConstruct
    public void init() {
        dossSearch = new DossierSearch();
        visitedIds = new ArrayList<>();

    }

    public void actionSearch() {
        visitedIds = new ArrayList<>();
        LOGGER.debug("actionSearch");

        if (!emptyAddrEkatteBean && !emptyAddrTextBean && !emptyUniversitetBean && !emptyBirthPlaceBean) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            getMessageResourceString(beanMessages, "regMigr.checkBoxMess"), ""));
            return;
        }

        if (emptyAddrEkatteBean) {
            dossSearch.setEmptyAddrEkatte(SysConstants.CODE_ZNACHENIE_DA);
        } else {
            dossSearch.setEmptyAddrEkatte(null);
        }

        if (emptyAddrTextBean) {
            dossSearch.setEmptyAddrText(SysConstants.CODE_ZNACHENIE_DA);
        } else {
            dossSearch.setEmptyAddrText(null);
        }

        if (emptyUniversitetBean) {
            dossSearch.setEmptyUniversitet(SysConstants.CODE_ZNACHENIE_DA);
        } else {
            dossSearch.setEmptyUniversitet(null);
        }

        if (emptyBirthPlaceBean) {
            dossSearch.setEmptyBirthPlace(SysConstants.CODE_ZNACHENIE_DA);
        } else {
            dossSearch.setEmptyBirthPlace(null);
        }

        dossSearch.buildFilterQuery();
        dossierList = new LazyDataModelSQL2Array(dossSearch, null);
    }

    public void actionClear() {
        dossSearch = new DossierSearch();
        dossierList = null;
        emptyAddrEkatteBean = false;
        emptyAddrTextBean = false;
        emptyUniversitetBean = false;
        emptyBirthPlaceBean = false;
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

    public boolean isVisited(Integer id) {
        if (id == null) {
            return false;
        }
        return visitedIds.contains(id);
    }

    public void postProcessXLSX(Object document) {
        String filterInfo = "";
        if (emptyAddrTextBean) {
            filterInfo = filterInfo + getMessageResourceString(LABELS, "regMigr.bezAddr") + "; ";
        }
        if (emptyAddrEkatteBean) {
            filterInfo = filterInfo + getMessageResourceString(LABELS, "regMigr.bezNasM") + "; ";
        }
        if (emptyUniversitetBean) {
            filterInfo = filterInfo + getMessageResourceString(LABELS, "regMigr.bezUni") + "; ";
        }
        if (emptyBirthPlaceBean) {
            filterInfo = filterInfo + getMessageResourceString(LABELS, "regMigr.bezBPlace") + "; ";
        }

        Object[] dopInfo = new Object[]{"Избрани критерии: " + filterInfo};

        String title = getMessageResourceString(LABELS, "regMigr.title");
        new CustomExpPreProcess().postProcessXLSX(document, title, dopInfo, null, null);

    }

    /**
     * Метод за форматиране на дата за името на файла
     *
     * @param dateFromPage
     * @return
     */
    public String getDocDateFormatted(Date dateFromPage) {
        if (dateFromPage != null) {
            return new SimpleDateFormat("yyyyMMdd").format(dateFromPage);
        } else {
            return "";
        }
    }

    public List<Integer> getVisitedIds() {
        return visitedIds;
    }

    public void setVisitedIds(List<Integer> visitedIds) {
        this.visitedIds = visitedIds;
    }

    public LazyDataModelSQL2Array getDossierList() {
        return dossierList;
    }

    public void setDossierList(LazyDataModelSQL2Array dossierList) {
        this.dossierList = dossierList;
    }

    public DossierSearch getDossSearch() {
        return dossSearch;
    }

    public void setDossSearch(DossierSearch dossSearch) {
        this.dossSearch = dossSearch;
    }

    public boolean isEmptyAddrEkatteBean() {
        return emptyAddrEkatteBean;
    }

    public void setEmptyAddrEkatteBean(boolean emptyAddrEkatteBean) {
        this.emptyAddrEkatteBean = emptyAddrEkatteBean;
    }

    public boolean isEmptyAddrTextBean() {
        return emptyAddrTextBean;
    }

    public void setEmptyAddrTextBean(boolean emptyAddrTextBean) {
        this.emptyAddrTextBean = emptyAddrTextBean;
    }

    public boolean isEmptyUniversitetBean() {
        return emptyUniversitetBean;
    }

    public void setEmptyUniversitetBean(boolean emptyUniversitetBean) {
        this.emptyUniversitetBean = emptyUniversitetBean;
    }

    public boolean isEmptyBirthPlaceBean() {
        return emptyBirthPlaceBean;
    }

    public void setEmptyBirthPlaceBean(boolean emptyBirthPlaceBean) {
        this.emptyBirthPlaceBean = emptyBirthPlaceBean;
    }
}
