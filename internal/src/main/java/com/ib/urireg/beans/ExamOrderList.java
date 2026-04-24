package com.ib.urireg.beans;

import com.ib.urireg.search.DocSearch;
import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
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
public class ExamOrderList extends IndexUIbean  {


    private static final long serialVersionUID = -1768009119247673779L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExamOrderList.class);

    private Integer periodZapoved;
    private Integer periodProtTest;
    private Integer periodProtKasus;
    private LazyDataModelSQL2Array zapovedList;
    private DocSearch docSearch;
    private List<SelectItem> vidDocList;// за вид заповед
    private List<SelectItem> vidProtList;//за вид протокол
    private List<Integer> visitedIds;

    @PostConstruct
    void initData() {
        docSearch = new DocSearch();
        vidDocList = new ArrayList<>();
        vidProtList =  new ArrayList<>();
        visitedIds = new ArrayList<>();
        try {
            //за вид документ-заповед
            String nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT, getCurrentLang(), new Date());
            vidDocList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT, nameItem));
            nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019, getCurrentLang(), new Date());
            vidDocList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019, nameItem));
            //за вид документ-протокол
            nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT_TEST, getCurrentLang(), new Date());
            vidProtList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT_TEST, nameItem));
            nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT, getCurrentLang(), new Date());
            vidProtList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT, nameItem));




        } catch (DbErrorException e) {
            throw new RuntimeException(e);
        }
    }

    public void actionClear(){
        docSearch = new DocSearch();
        zapovedList = null;
        periodZapoved = null;
        periodProtTest = null;
        periodProtKasus = null;
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

    /** Метод за смяна на датите при избор на период за търсене на дата на протокол - тест.
     *
     */
    public void changePeriodProtTest () {

        if (this.periodProtTest != null) {
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodProtTest);
            docSearch.setProtTestDateFrom(di[0]);
            docSearch.setProtTestDateTo(di[1]);
        } else {
            docSearch.setProtTestDateFrom(null);
            docSearch.setProtTestDateTo(null);
        }
    }

    public void changeDateProtTest() {

        this.setPeriodProtTest(null);
    }

    public void actionSearch(){
        visitedIds=new ArrayList<>();
        docSearch.buildZapovedIzpitQuery();
        zapovedList = new LazyDataModelSQL2Array(docSearch, "a2 desc");
    }

    /** Метод за смяна на датите при избор на период за търсене на дата на протокол - казус.
     *
     */
    public void changePeriodProtKazus () {

        if (this.periodProtKasus != null) {
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodProtKasus);
            docSearch.setProtCaseDateFrom(di[0]);
            docSearch.setProtCaseDateTo(di[1]);
        } else {
            docSearch.setProtCaseDateFrom(null);
            docSearch.setProtCaseDateTo(null);
        }
    }

    public String actionGoto(Object[] row) {
        Integer idObj = ((Number) row[0]).intValue();
        visitedIds.add(idObj);
        String result = "examOrderEdit.xhtml?faces-redirect=true&idObj=" + idObj;

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

        String title = getMessageResourceString(LABELS, "examOrderList.reportTitle");
        new CustomExpPreProcess().postProcessXLSX(document, title, null, null, null);

    }

//    /**
//     * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
//     */
//    public void preProcessPDF(Object document)  {
//        try{
//
//            String title = getMessageResourceString(LABELS, "examOrderList.reportTitle");
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

    public void changeDateProtKazus() {

        this.setPeriodProtKasus(null);
    }

    public Integer getPeriodZapoved() {

        return periodZapoved;
    }

    public void setPeriodZapoved(Integer periodZapoved) {

        this.periodZapoved = periodZapoved;
    }

    public Integer getPeriodProtTest() {
        return periodProtTest;
    }

    public void setPeriodProtTest(Integer periodProtTest) {
        this.periodProtTest = periodProtTest;
    }

    public Integer getPeriodProtKasus() {
        return periodProtKasus;
    }

    public void setPeriodProtKasus(Integer periodProtKasus) {
        this.periodProtKasus = periodProtKasus;
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

    public List<SelectItem> getVidProtList() {
        return vidProtList;
    }

    public void setVidProtList(List<SelectItem> vidProtList) {
        this.vidProtList = vidProtList;
    }

    public List<Integer> getVisitedIds() {
        return visitedIds;
    }

    public void setVisitedIds(List<Integer> visitedIds) {
        this.visitedIds = visitedIds;
    }

    public String getReportFileName(){
        return "Заповеди_изпит_" +  new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
    }
}
