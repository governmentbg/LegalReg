package com.ib.urireg.beans;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.system.utils.DateUtils;
import com.ib.urireg.search.RegUDSearch;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Named("regUD")
@ViewScoped
public class RegUD extends IndexUIbean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RegUD.class);

    private LazyDataModelSQL2Array udList;
    private RegUDSearch udSearch;
    private Integer periodUD;


    @PostConstruct
    public void init() {
        LOGGER.debug("init - RegUD");
        udSearch = new RegUDSearch();
    }

    public void changePeriod() {
        if (this.periodUD != null) {
            LOGGER.debug("periodUd = {}", this.periodUD);
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodUD);
            udSearch.setUdDateFrom(di[0]);
            udSearch.setUdDateTo(di[1]);
        } else {
            udSearch.setUdDateFrom(null);
            udSearch.setUdDateTo(null);
        }
    }

    public void actionClearPeriod() {
        this.periodUD = null;
    }

    public void actionSearch() {
//        if (udSearch.getUdDateFrom() == null || udSearch.getUdDateTo() == null) {
//            FacesContext.getCurrentInstance().addMessage(null,
//                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
//                            getMessageResourceString(beanMessages, "regUD.dateFromTo"), ""));
//            return;
//        }
//
//        if (udSearch.getUdDateFrom().after(udSearch.getUdDateTo())) {
//            FacesContext.getCurrentInstance().addMessage(null,
//                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
//                            getMessageResourceString(beanMessages, "regUD.dateFromBeforeDateTo"), ""));
//            return;
//        } // за сега да може и без период, за засечем резултатите с досието
        udSearch.buildFilterQuery();
        udList = new LazyDataModelSQL2Array(udSearch, "a3");
    }

    public void actionClear() {
        udSearch = new RegUDSearch();
        periodUD = null;
        udList = null;
    }

    /******************************** EXPORTS **********************************/

    /**
     * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
     */
    public void postProcessXLSX(Object document) {
        Date selectedDateFrom = udSearch.getUdDateFrom();
        Date selectedDateTo = udSearch.getUdDateTo();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("bg", "BG"));

        String formattedDateFrom = (selectedDateFrom != null) ? dateFormat.format(selectedDateFrom).toUpperCase() : "";
        String formattedDateTo = (selectedDateTo != null) ? dateFormat.format(selectedDateTo).toUpperCase() : "";

        String title = getMessageResourceString(LABELS, "regUP.titleIn", formattedDateFrom, formattedDateTo);
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

    public LazyDataModelSQL2Array getUdList() {
        return udList;
    }

    public void setUdList(LazyDataModelSQL2Array udList) {
        this.udList = udList;
    }

    public RegUDSearch getUdSearch() {
        return udSearch;
    }

    public void setUdSearch(RegUDSearch udSearch) {
        this.udSearch = udSearch;
    }

    public Integer getPeriodUD() {
        return periodUD;
    }

    public void setPeriodUD(Integer periodUD) {
        this.periodUD = periodUD;
    }
}
