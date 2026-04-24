package com.ib.urireg.search;

import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.DateUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

public class MessagesSearch extends SelectMetadata  {

    private Integer vid;
    private Integer status;
    private Date dateFrom;
    private Date dateTo;
    private String title;
    private String text;
    private Integer lang;


    public void buildQuery() {
        Map<String, Object> params = new HashMap<>();

        StringBuilder select = new StringBuilder();
        StringBuilder from = new StringBuilder();
        StringBuilder where = new StringBuilder();

        select.append(" select message.id a0, message.message_vid a1, message.date_from a2, message.date_to a3, ");
        select.append(" message.status a4, message.message_info a5, lang.title a6, lang.message_text a7, lang.lang a8 ");
        from.append(" from message ");
        from.append(" left outer join message_lang lang on lang.message_id = message.id ");
        where.append(" where 1=1 ");


        if (this.vid != null) {
            where.append(" and message.message_vid = :vid ");
            params.put("vid", this.vid);
        }

        if (this.status != null) {
            where.append(" and message.status = :status ");
            params.put("status", this.status);
        }

        if (this.dateFrom != null) {
            where.append(" and message.date_from >= :dateFrom ");
            params.put("dateFrom", DateUtils.startDate(this.dateFrom));
        }

        if (this.dateTo != null) {
            where.append(" and message.date_to <= :dateTo ");
            params.put("dateTo", DateUtils.endDate(this.dateTo));
        }

        String t = trimToNULL_Upper(this.title);
        if (t != null) {
            where.append(" and upper(lang.title) like :title  ");
            params.put("title", "%" + t + "%");
        }

        t = trimToNULL_Upper(this.text);
        if (t != null) {
            where.append("and upper(lang.message_text) like :text ");
            params.put("text", "%" + t + "%");
        }

        if (this.lang != null) {
            where.append(" and lang.lang = :lang ");
            params.put("lang", this.lang);
        }


        setSqlCount(" select count(*) " + from + where);
        setSql(select.toString() + from + where);
        setSqlParameters(params);
    }



    public Integer getVid() {
        return vid;
    }

    public void setVid(Integer vid) {
        this.vid = vid;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getLang() {
        return lang;
    }

    public void setLang(Integer lang) {
        this.lang = lang;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
