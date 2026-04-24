package com.ib.urireg.search;

import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.DateUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.ib.system.utils.SearchUtils.trimToNULL;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

public class RegUDSearch extends SelectMetadata {

    private static final long serialVersionUID = 1L;
    private static final int CASE_RESULT_PASSED = 1;
    private Date udDateFrom;
    private Date udDateTo;
    private String egn;
    private String names;

    /**
     * Регистър за издадени удостоверения
     * Резултат от заявката:
     * [0]-lice.lice_id
     * [1]-lice.egn
     * [2]-lice.lnc
     * [3]-lice.firstname
     * [4]-lice.surname
     * [5]-lice.lastname
     * [6]-lice.birth_date
     * [7]-lice.birth_place
     * [8]-staj.staj_id
     * [9]-staj.osn_institution
     * [10]-staj.osn_end_date
     * [11]-izpit.case_date
     * [12]-udost.udost_id
     * [13]-udsot.rn_udost
     * [14]-udost.udost_date
     * [15]-udost.original - В страницата е обърнато(ако original=1/null - Да, ако original=0 - Не, в страницата е обратно Да-Не; Не-Да)
     */


    public void buildFilterQuery() {
        Map<String, Object> params = new HashMap<>();

        StringBuilder select = new StringBuilder();
        StringBuilder from = new StringBuilder();
        StringBuilder where = new StringBuilder();

        select.append("select lice.lice_id a0, lice.egn a1, lice.lnc a2");
        select.append(",lice.firstname a3, lice.surname a4, lice.lastname a5, lice.birth_date a6, lice.birth_place a7");
        select.append(",staj.staj_id a8, staj.osn_institution a9, staj.osn_end_date a10, izpit.case_date a11");
        select.append(", udost.doc_id a12, udost.rn_doc a13, udost.doc_date a14, udost.original a15 ");


        from.append("from lice ");

        from.append("LEFT OUTER JOIN doc udost on udost.doc_id = lice.udost_id ");
        from.append("LEFT OUTER JOIN staj on staj.zaiav_staj_id=lice.last_zaiav_staj_id ");
        from.append("LEFT OUTER JOIN  izpit_result resultat ON resultat.staj_id = staj.staj_id and case_result=:caseResultPassed ");
        params.put("caseResultPassed", CASE_RESULT_PASSED);
        from.append("LEFT OUTER JOIN  izpit ON  izpit.izpit_id=resultat.izpit_id ");
        where.append("where lice.udost_id is not null "); // ако има udost_id - значи има издадено УП


//        from.append("join staj on staj.zaiav_staj_id=lice.last_zaiav_staj_id ");
//        from.append("join izpit_result resultat on resultat.staj_id = staj.staj_id and case_result=:caseResultPassed ");
//        params.put("caseResultPassed", CASE_RESULT_PASSED);
//        from.append("left outer join doc udost on udost.doc_id = lice.udost_id ");
//        from.append("left join izpit on izpit.izpit_id=resultat.izpit_id ");
//        where.append("where udost.doc_id is not null ");


        if (this.udDateFrom != null) {
            where.append("and udost.doc_date >= :udDateFrom ");
            params.put("udDateFrom", DateUtils.startDate(this.udDateFrom));
        }
        if (this.udDateTo != null) {
            where.append("and udost.doc_date <= :udDateTo ");
            params.put("udDateTo", DateUtils.endDate(this.udDateTo));
        }

        String t = trimToNULL(this.egn);
        if (t != null) {
            where.append(" and lice.egn like :egn ");
            params.put("egn", "%" + t + "%");
        }
        t = trimToNULL_Upper(this.names);
        if (t != null) {
            t = t.replaceAll("\\s+", "%");
            where.append(" and upper(lice.names) like :names ");
            params.put("names", "%" + t + "%");
        }

        setSqlCount("select count(*) " + from + where);
        setSql(select.toString() + from + where);
        setSqlParameters(params);
    }


    public Date getUdDateFrom() {
        return udDateFrom;
    }

    public void setUdDateFrom(Date udDateFrom) {
        this.udDateFrom = udDateFrom;
    }

    public Date getUdDateTo() {
        return udDateTo;
    }

    public void setUdDateTo(Date udDateTo) {
        this.udDateTo = udDateTo;
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
}
