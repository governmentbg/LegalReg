/*
 * Copyright (c) 2025. Index - Bulgaria Ltd. All rights reserved.
 *
 */

package com.ib.urireg.soap;

import com.ib.system.db.JPA;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.urireg.system.UriregConstants;
import jakarta.jws.WebService;
import jakarta.persistence.Query;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

/**
 * СОАП сървис за regix.
 */
@WebService(
        endpointInterface = "com.ib.urireg.soap.UriRegWebService",
        serviceName = "UriRegWebService",
        portName = "UriRegWebServicePort",
        targetNamespace = "http://mjs.com/soap")
public class UriRegWebServiceImpl implements UriRegWebService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UriRegWebServiceImpl.class);


    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public PersonWS findUriPerson(String egnLnch) throws ServiceFaultException {

        if (egnLnch==null || egnLnch.isEmpty()){
            ServiceFault fault = new ServiceFault(
                    "VALIDATION_ERROR",
                    "Input must not be blank",
                    "CID-" + System.currentTimeMillis()
            );
            LOGGER.error(fault.toString());
            throw new ServiceFaultException("Invalid request", fault);
        }

        try {

            StringBuilder sql = new StringBuilder();

            sql.append(" select udost.doc_id a0, ")
                    .append(" l.lice_id a1,  ")
                    .append(" udost.rn_doc a2, ")
                    .append(" udost.doc_date a3, ")
                    .append(" l.firstname a4, ")
                    .append(" l.surname a5, ")
                    .append(" l.lastname a6, ")
                    .append(" l.egn a7, ")
                    .append(" l.lnc a8, ")
                    .append("CASE WHEN  l.status = ")//заради сортирането
                    .append(UriregConstants.CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN)
                    .append(" THEN 'да'  ELSE '' END a9, ")
                    .append("CASE WHEN  udost.original = ")//заради сортирането
                    .append(UriregConstants.CODE_ZNACHENIE_NE)
                    .append(" THEN 'да' ELSE 'не' END a10 ")
                    .append(" from lice l ")
                    .append(" JOIN doc udost on l.udost_id = udost.doc_id ")
                    .append(" where (l.egn = :egnlnc or l.lnc = :egnlnc)");


            Query q = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString());
            q.setParameter("egnlnc", egnLnch);

            List<Object[]> all = q.getResultList();
            PersonWS p = new PersonWS();

            if (!all.isEmpty()) {

                Object[] rez = all.get(0);

                String egnLnch1 =SearchUtils.asString(rez[7]);
                if(egnLnch1==null|| egnLnch1.trim().isEmpty()){
                    egnLnch1 = SearchUtils.asString(rez[8]);
                }
                p.setEgnLnch(egnLnch1);

                p.setName(SearchUtils.asString(rez[4]));
                p.setPrezime(SearchUtils.asString(rez[5]));
                p.setFamilia(SearchUtils.asString(rez[6]));

                p.setUpNomer(SearchUtils.asString(rez[2]));
                p.setUpDate(SearchUtils.asDate(rez[3]));
                p.setDublikat(SearchUtils.asString(rez[10]));
                p.setLishenOtPravo(SearchUtils.asString(rez[9]));
            }


            return p;
        } catch (Exception e){
            LOGGER.error(e.toString());
            ServiceFault fault = new ServiceFault(
                    "DATABASE_ERROR",
                    e.getMessage(),
                    "CID-" + System.currentTimeMillis()
            );
            LOGGER.error(fault.toString());
            throw new ServiceFaultException("Invalid request", fault);
        }

    }
}
