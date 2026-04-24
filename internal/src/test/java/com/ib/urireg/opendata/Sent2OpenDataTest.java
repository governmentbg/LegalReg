package com.ib.urireg.opendata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ib.system.db.JPA;
import com.ib.system.utils.JSonUtils;
import com.ib.urireg.rest.client.SimpleRestClient;
import com.ib.urireg.search.PublicRegister;
import jakarta.persistence.Query;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;

public class Sent2OpenDataTest {

    /*
    How to...
    1. набори данни
    2. Добавя се ресурс  и се взема неговия "Уникален идентификатор"м който се слага в RESOURCE_URI
     */

    //TestOpenData (IndexBG)
    private final String SRV_TARGET = "https://testdata.egov.bg/api";
    private final String API_KEY = "4a34ab9a-e3ab-4444-b074-21e5092357e4";
    //    private final String RESOURCE_RUI = "baf009ae-63a4-408f-9838-c400d2ffbb68";
    private final String RESOURCE_URI = "29f9d248-352f-4989-ba25-1d285be35d85";

//    //ProdOpenData (MP)
//    private final String SRV_TARGET = "https://data.egov.bg/api";
//    private final String API_KEY = "e9d34b62-3c73-4ecc-a65c-07e926e3f23e";
//    private final String RESOURCE_URI = "81c79702-cb51-444d-ad9f-02611f12eba0";



    @Test
    public void testSent2Opendata() {
        SimpleRestClient instance = SimpleRestClient.getInstance();
// updateResourceData
// addResourceData
        WebTarget webTarget = instance.getClient().target(SRV_TARGET).path("/updateResourceData");

        OpenDataObj oData = new OpenDataObj();
        oData.setApi_key(API_KEY);
        oData.setResource_uri(RESOURCE_URI);
//        oData.setExtension_format("csv");
//        String tmpData = "{"
//        + "\"headers\": [\"Данни\", \"Месец\", \"Брой\"],"
//        + "\"row1\": [\"тестови данни\", \"Юни\", 7]"
//        + "}";
        oData.setExtension_format("json");
        String tmpData = ""
                + "[" +
                 getData() +
//                "{\"Данни\":1, \"Месец\":2, \"Брой\":3}," +
//                "{\"Данни\":11, \"Месец\":21, \"Брой\":31}" +
                "]";

        oData.setData(tmpData);
        String s = "";
        try {
            s = JSonUtils.object2json(oData);
           // System.out.println("=======:" + s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
//        Response response = webTarget.request().post(Entity.entity(oData, MediaType.APPLICATION_JSON));
        Response response = webTarget.request().post(Entity.json(oData));

        try {
            int status = response.getStatus();
            if (status != Response.Status.OK.getStatusCode()) {
                String errorMsg = response.readEntity(String.class);

                System.err.println("Error response: " + status + " - " + unescapeUnicode(errorMsg));
                throw new AssertionError("Expected status 200 but got " + status);
            }

        } finally {
            response.close();
        }
    }

//////////////////////////////////
//Извличаме регистъра и го праим на json
public String getData(){

    PublicRegister search = new PublicRegister();
    search.buildQuery();
    String sql = search.getSql();
    try{
        Map<String, Object> sqlParameters = search.getSqlParameters();
        Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql);

        sqlParameters.forEach((key, value) -> {

            query.setParameter(key, value);
        });
        List<Object[]> resultList = query.getResultList();

        //Транформирам го в удобен за json формат

        List<Map<String,Object>> mappedResult = new ArrayList<>();
        SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
        for (Object[] row : resultList) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("up_number",row[7]);
            Date upDate = (Date) row[8];
            map.put("up_date", upDate!=null?DATE_FMT.format(upDate):"");
            map.put("name",row[1]);
            map.put("secon_name",row[2]);
            map.put("last_name",row[3]);
            Date protocoName = (Date) row[13];
            map.put("protoco_name",protocoName!=null?DATE_FMT.format(protocoName):"");
            Object raw = row[11];
            Integer duplicate =
                    raw == null
                            ? null          // or 0 if you prefer
                            : (((Number) raw).intValue() == 2 ? 1 : 0);

            map.put("duplicate", duplicate);
             raw = row[4];
            Integer disqualified =
                    raw == null
                            ? null          // or 0 if you prefer
                            : (((Number) raw).intValue() == 8 ? 1 : 0);

            map.put("disqualified",disqualified);


            mappedResult.add(map);
        }
        String json = JSonUtils.object2json(mappedResult, true);
       return json;


    } catch (JsonProcessingException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
    } finally {
        JPA.getUtil().closeConnection();
    }

  //  return null;

}

public static String unescapeUnicode(String input) {
    // Prefix with a dummy property name so Properties can parse it
    Properties p = new Properties();
    try {
        p.load(new StringReader("x=" + input));
        return p.getProperty("x");
    } catch (Exception e) {
        return input; // fallback if something goes wrong
    }

}






}
