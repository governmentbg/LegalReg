package com.ib.urireg.beans;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.DateUtils;
import com.ib.urireg.system.UriregConstants;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;
import static com.ib.urireg.system.UriregConstants.*;
import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT;

@Named
@ViewScoped
public class PublicRegister extends IndexUIbean  {


    private static final long serialVersionUID = -1768009119247673779L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicRegister.class);
    private String imena;
    private String nomerUP;
    private boolean nomerUpEQ = true;    // ако е true се търси по пълно съвпадение по номер на документ
    private Integer periodUP;
    private Date dateFrom;
    private Date dateTo;
    private Integer periodProtokol;
    private Date dateFromProtokol;
    private Date dateToProtokol;
    private LazyDataModelSQL2Array resultList;

    @PostConstruct
    void initData() {

    }

    /**
     * Метод за търсене. Може би временно тук? <br>
     *
     * [0]-udost.rn_doc<br>
     * [1]-udost.doc_date<br>
     * [2]-l.firstname<br>
     * [3]-l.surname<br>
     * [4]-l.lastname<br>
     * [5]-prot.doc_date<br>
     * [6]-udost.original<br>
     * [7]-l.status<br>
     */
    public LazyDataModelSQL2Array buildPublicRegisterQuery() {
        Map<String, Object> params = new HashMap<>();

        StringBuilder select = new StringBuilder();
        StringBuilder from = new StringBuilder();
        StringBuilder where = new StringBuilder();

        select.append(" select udost.doc_id a0, ")
                .append(" l.lice_id a1,  ")
                .append(" udost.rn_doc a2, ")
                .append(" udost.doc_date a3, ")
                .append(" l.firstname a4, ")
                .append(" l.surname a5, ")
                .append(" l.lastname a6, ")
                .append(" max(prot.doc_date) a7, ") //izpit_result->case_result = издържал евентуално да се вади така
                .append("CASE WHEN  udost.original = ")//заради сортирането
                .append(UriregConstants.CODE_ZNACHENIE_NE)
                .append(" THEN udost.original ELSE null END a8, ")
                .append("CASE WHEN  l.status = ")//заради сортирането
                .append(UriregConstants.CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN)
                .append(" THEN l.status ELSE null END a9 ");

        from.append(" from lice l ")
            .append(" JOIN doc udost on l.udost_id = udost.doc_id ")
            .append(" LEFT JOIN izpit_result ir on l.lice_id = ir.lice_id ")
            .append(" LEFT JOIN izpit i on i.izpit_id = ir.izpit_id ")
            .append(" LEFT JOIN doc prot on prot.doc_id = i.case_prot_id ");

        where.append(" where 1=1 ");
        String t = trimToNULL_Upper(this.imena);
        if (t != null) {
            t = t.replaceAll("\\s+", "%");
            where.append(" and upper(l.names) like :names ");
            params.put("names", "%" + t + "%");
        }

        if (this.dateFrom != null) {
            where.append(" and udost.doc_date >= :dateFrom ");
            params.put("dateFrom", DateUtils.startDate(this.dateFrom));
        }
        if (this.dateTo != null) {
            where.append(" and udost.doc_date <= :dateTo ");
            params.put("dateTo", DateUtils.endDate(this.dateTo));
        }

        if (this.dateFromProtokol != null) {
            where.append(" and prot.doc_date >= :dateFromProtokol ");
            params.put("dateFromProtokol", DateUtils.startDate(this.dateFromProtokol));
        }
        if (this.dateToProtokol != null) {
            where.append(" and prot.doc_date <= :dateToProtokol ");
            params.put("dateToProtokol", DateUtils.endDate(this.dateToProtokol));
        }

        t = trimToNULL_Upper(this.nomerUP);
        if (t != null) {
            if (this.nomerUpEQ) { // пълно съвпадение
                where.append(" and upper(udost.rn_doc) = :nomerUP ");
                params.put("nomerUP", t);

            } else {
                where.append(" and upper(udost.rn_doc) like :nomerUP ");
                params.put("nomerUP", "%" + t + "%");
            }
        }

        where.append(" group by udost.doc_id,l.lice_id,udost.rn_doc, udost.doc_date, l.firstname,l.surname,l.lastname,udost.original,l.status ");
        String sql = select.toString() + from  + where;
        String sqlCount = " select count(x.*) from ( " + sql + " ) as x ";
        SelectMetadata smd=new SelectMetadata();
        smd.setSql(sql);
        smd.setSqlCount(sqlCount);

        smd.setSqlParameters(params);

        return new LazyDataModelSQL2Array(smd,"udost.doc_date desc");
    }

    public void actionSearch(){
        resultList = buildPublicRegisterQuery();
    }

    public void actionClear(){
        imena = "";
        periodUP = null;
        dateFrom = null;
        dateTo = null;
        periodProtokol = null;
        dateFromProtokol = null;
        dateToProtokol = null;
        nomerUP = "";
        nomerUpEQ = true;
        resultList = null;
    }



    /** Метод за смяна на датите при избор на период за търсене на дата.
     *
     */
    public void changePeriodUP () {

        if (this.periodUP != null) {
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodUP);
            dateFrom = di[0];
            dateTo = di[1];
        } else {
            dateFrom = null;
            dateTo = null;
        }
    }

    public void changeDateUP() {

        this.setPeriodUP(null);
    }

    /** Метод за смяна на датите при избор на период за търсене на дата на протокол.
     *
     */
    public void changePeriodProtokol () {

        if (this.periodProtokol != null) {
            Date[] di;
            di = DateUtils.calculatePeriod(this.periodProtokol);
            dateFromProtokol = di[0];
            dateToProtokol = di[1];
        } else {
            dateFromProtokol = null;
            dateToProtokol = null;
        }
    }

    public void changeDateProtokol() {

        this.setPeriodProtokol(null);
    }

    /**
     * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
     */
    public void postProcessXLSX(Object document) {

        String title = getMessageResourceString(LABELS, "examOrderList.reportTitle");
        new CustomExpPreProcess().postProcessXLSX(document, title, null, null, null);

    }

    public String getNomerUP() {
        return nomerUP;
    }

    public void setNomerUP(String nomerUP) {
        this.nomerUP = nomerUP;
    }

    public String getImena() {
        return imena;
    }

    public void setImena(String imena) {
        this.imena = imena;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Integer getPeriodUP() {
        return periodUP;
    }

    public void setPeriodUP(Integer periodUP) {
        this.periodUP = periodUP;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public Integer getPeriodProtokol() {
        return periodProtokol;
    }

    public void setPeriodProtokol(Integer periodProtokol) {
        this.periodProtokol = periodProtokol;
    }

    public Date getDateFromProtokol() {
        return dateFromProtokol;
    }

    public void setDateFromProtokol(Date dateFromProtokol) {
        this.dateFromProtokol = dateFromProtokol;
    }

    public Date getDateToProtokol() {
        return dateToProtokol;
    }

    public void setDateToProtokol(Date dateToProtokol) {
        this.dateToProtokol = dateToProtokol;
    }

    public LazyDataModelSQL2Array getResultList() {
        return resultList;
    }

    public void setResultList(LazyDataModelSQL2Array resultList) {
        this.resultList = resultList;
    }

    public boolean isNomerUpEQ() {
        return nomerUpEQ;
    }

    public void setNomerUpEQ(boolean nomerUpEQ) {
        this.nomerUpEQ = nomerUpEQ;
    }

    public String getReportFileName(){
        return "Регистър-ЮП_" +  new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
    }
}
