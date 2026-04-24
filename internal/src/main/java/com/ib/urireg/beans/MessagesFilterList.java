package com.ib.urireg.beans;


import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.utils.DateUtils;
import com.ib.urireg.search.MessagesSearch;
import com.ib.urireg.system.UserData;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;


@Named
@ViewScoped
public class MessagesFilterList extends IndexUIbean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesFilterList.class);

    private UserData ud;
    private Integer period;
    private LazyDataModelSQL2Array messagesList;
    private MessagesSearch messagesSearch;


    @PostConstruct
    private void init() {
        LOGGER.debug("MessagesFilterList postConstruct");

        this.ud = getUserData(UserData.class);
        this.messagesSearch = new MessagesSearch();
        try {

            actionSearch();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при инициализиране на филтър за търсене на съобщения!");
        }
    }

    public void actionSearch() {
        try {

            messagesSearch.buildQuery();
            messagesSearch.setLang(1); //в Мантис е описано, че за сега ще се работи само с български
            messagesList = new LazyDataModelSQL2Array(messagesSearch, "a2 desc"); //сортирани по дата

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при търсене на съобщения в базата данни!");
        }
    }

    public void actionClear(){
        this.messagesSearch = new MessagesSearch();
        setPeriod(null);
        actionSearch();
    }


    public void changePeriod () {
        if (this.period != null) {
            Date[] di;
            di = DateUtils.calculatePeriod(this.period);
            messagesSearch.setDateFrom(di[0]);
            messagesSearch.setDateTo(di[1]);
        } else {
            messagesSearch.setDateFrom(null);
            messagesSearch.setDateTo(null);
        }
    }

    public void changeDate() {
        this.setPeriod(null);
    }



    public void postProcessXLS(Object document) {
        String title = getMessageResourceString(LABELS, "messagesFilterList.exportTitle");
        new CustomExpPreProcess().postProcessXLS(document, title, null , null, null);
    }

    public void preProcessPDF(Object document)  {
        try {

            String title = getMessageResourceString(LABELS, "messagesFilterList.exportTitle");
            new CustomExpPreProcess().preProcessPDF(document, title,  null, null, null);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(),e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(),e);
        }
    }

    public PDFOptions pdfOptions() {
        PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
        return pdfOpt;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public LazyDataModelSQL2Array getMessagesList() {
        return messagesList;
    }

    public void setMessagesList(LazyDataModelSQL2Array messagesList) {
        this.messagesList = messagesList;
    }

    public MessagesSearch getMessagesSearch() {
        return messagesSearch;
    }

    public void setMessagesSearch(MessagesSearch messagesSearch) {
        this.messagesSearch = messagesSearch;
    }


}
