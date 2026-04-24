package com.ib.urireg.beans;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.utils.DateUtils;
import com.ib.urireg.search.StatReports;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named
@ViewScoped
public class StatReportsView extends IndexUIbean  {


    private static final long serialVersionUID = -1768009119247673779L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StatReportsView.class);


    private Integer period;
    private Date startDate;
    private Date endDate;
    private Integer vidReport;
    private List<Object[]> resultList;// за вид заповед

    @PostConstruct
    void initData() {
        resultList  = new ArrayList<>();
        vidReport = 1; // 1 - статистика по университети ; 2 - статистика по протоколи и университети
        this.period = 18;
        calcDates();
    }

    public void actionClear(){
        resultList = null;
        period = null;
        startDate = null;
        endDate = null;
        vidReport = 1;
    }

    public void actionSearch(){
        StatReports stat = new StatReports();
        try {
            if(startDate == null || endDate == null || startDate.after(endDate)){
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "Въведете валиден период за справката!")  ;
            } else {
                if (vidReport == 1) {
                    resultList = stat.selectStatUniversitetIzpiti(startDate, endDate);
                } else {
                    resultList = stat.selectStatProtUniversitetIzpiti(startDate, endDate);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Грешка при извеждане на статистика! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }
    }

    /** Метод за смяна на датите при избор на период за търсене
     *
     */
    public void changePeriod() {
        if (this.period != null) {
            calcDates();
        } else {
            startDate = null;
            endDate = null;
        }
    }

    public void changeDate() {
        this.setPeriod(null);
    }

    protected void  calcDates(){
        Date[] di;
        di = DateUtils.calculatePeriod(this.period);
        startDate = di[0];
        endDate = di[1];
    }

    public void changeVid(){
        resultList = null;
    }

    private String titleRep(){
        String title = vidReport == 1 ? "Справка за изпити по университети " : "Справака за изпити по протоколи и университети ";
        String strPeriod = "за период: "+DateUtils.printDate(startDate) + " - " +DateUtils.printDate(endDate) ;
        return title + strPeriod;
    }

    /**
     * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
     */
    public void postProcessXLSX(Object document) {
        new CustomExpPreProcess().postProcessXLSX(document, titleRep(), null, null, null);
    }

    /**
     * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
     */
    public void preProcessPDF(Object document)  {
        try{
            new CustomExpPreProcess().preProcessPDF(document, titleRep(),  null, null, null);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(),e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(),e);
        }
    }

    /**
     * за експорт в pdf
     * @return
     */
    public PDFOptions pdfOptions() {
        PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
        return pdfOpt;
    }


    public String getReportFileName1(){
        return "Статистика_университети_" +  new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
    }

    public String getReportFileName2(){
        return "Статистика_протоколи_" +  new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
    }



    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getVidReport() {
        return vidReport;
    }

    public void setVidReport(Integer vidReport) {
        this.vidReport = vidReport;
    }

    public List<Object[]>  getResultList() {
        return resultList;
    }

    public void setResultList(List<Object[]>  resultList) {
        this.resultList = resultList;
    }
}
