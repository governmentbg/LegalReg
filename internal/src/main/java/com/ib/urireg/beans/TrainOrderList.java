package com.ib.urireg.beans;

import com.ib.urireg.search.DocSearch;
import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
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
public class TrainOrderList extends IndexUIbean  {


    private static final long serialVersionUID = -1768009119247673779L;
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainOrderList.class);

    private Integer periodZapoved;
    private LazyDataModelSQL2Array zapovedList;
    private DocSearch docSearch;
    private List<SelectItem> vidDocList;
    private List<Integer> visitedIds;

    @PostConstruct
    void initData() {

        docSearch = new DocSearch();
        vidDocList = new ArrayList<>();
        visitedIds = new ArrayList<>();
        try {
            String nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_STAJ, getCurrentLang(), new Date());
            vidDocList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_STAJ, nameItem));
            nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ, getCurrentLang(), new Date());
            vidDocList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ, nameItem));
        } catch (DbErrorException e) {
            throw new RuntimeException(e);
        }
    }

    public void actionClear(){
        docSearch = new DocSearch();
        periodZapoved = null;

    }

    /** Метод за смяна на датите при избор на период за търсене на дата на заповед.
     *
     */
    public void changePeriodZap () {

        if (this.periodZapoved != null) {
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodZapoved);
            docSearch.setZapDateFrom(di[0]);
            docSearch.setZapDateTo(di[1]);
        } else {
            docSearch.setZapDateFrom(null);
            docSearch.setZapDateTo(null);
        }
    }

    public void changeDateZap() {

        this.setPeriodZapoved(null);
    }

    public void actionSearch(){
        visitedIds=new ArrayList<>();
        docSearch.buildZapovedStajQuery();
        zapovedList = new LazyDataModelSQL2Array(docSearch, "a2 desc");
    }

    public String actionGoto(Object[] row) {

        Integer idObj = ((Number) row[0]).intValue();
        visitedIds.add(idObj);
        String result = "traineeshipOrder.xhtml?faces-redirect=true&idObj=" + idObj;

        return result;
    }

    public boolean isVisited(Integer id) {
        if (id == null) {
            return false;
        }
        return visitedIds.contains(id);
    }

    /**
     * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
     */
    public void postProcessXLSX(Object document) {

        String title = getMessageResourceString(LABELS, "trainOrderList.reportTitle");
        new CustomExpPreProcess().postProcessXLSX(document, title, null , null, null);

    }
//
//    /**
//     * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
//     */
//    public void preProcessPDF(Object document)  {
//        try{
//
//            String title = getMessageResourceString(LABELS, "trainOrderList.reportTitle");
//            new CustomExpPreProcess().preProcessPDF(document, title,  null, null, null);
//        } catch (UnsupportedEncodingException e) {
//            LOGGER.error(e.getMessage(),e);
//        } catch (IOException e) {
//            LOGGER.error(e.getMessage(),e);
//        }
//    }
//
//    /**
//     * за експорт в pdf
//     * @return
//     */
//    public PDFOptions pdfOptions() {
//        PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
//        return pdfOpt;
//    }


    public Integer getPeriodZapoved() {

        return periodZapoved;
    }

    public void setPeriodZapoved(Integer periodZapoved) {

        this.periodZapoved = periodZapoved;
    }

    public LazyDataModelSQL2Array getZapovedList() {
        return zapovedList;
    }

    public void setZapovedList(LazyDataModelSQL2Array zapovedList) {
        this.zapovedList = zapovedList;
    }

    public DocSearch getDocSearch() {
        return docSearch;
    }

    public void setDocSearch(DocSearch docSearch) {
        this.docSearch = docSearch;
    }

    public List<SelectItem> getVidDocList() {
        return vidDocList;
    }

    public void setVidDocList(List<SelectItem> vidDocList) {
        this.vidDocList = vidDocList;
    }

    public List<Integer> getVisitedIds() {
        return visitedIds;
    }

    public void setVisitedIds(List<Integer> visitedIds) {
        this.visitedIds = visitedIds;
    }

    public String getReportFileName(){
        return "Заповеди_стаж_" +  new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
    }
}
