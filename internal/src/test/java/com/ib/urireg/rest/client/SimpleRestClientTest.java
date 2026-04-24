package com.ib.urireg.rest.client;

import com.ib.urireg.system.SystemData;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.JSonUtils;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Map;

public class SimpleRestClientTest extends TestCase {

    private static final String SRV_TARGET = "https://10.29.1.160:8443/ibextractor/api/hello";

    public void testHttps() {
        SimpleRestClient instance = SimpleRestClient.getInstance();
        WebTarget webTarget = instance.getClient().target(SRV_TARGET).path("/echo");
        webTarget = webTarget.queryParam("message", "aaaa");
        Response response = webTarget.request().get();
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));

    }


    public void testDoit() {
        try {
            //Първо го търся в системните настройки.
            String local_SRV_TARGET = new SystemData().getSettingsValue("ib_indexer_addres");
            //Ако го няма го забивам твърдо за да мине теста
            if (local_SRV_TARGET == null) {
                local_SRV_TARGET = "http://localhost:8080/ibextractor/api";
            }
            SimpleRestClient instance = SimpleRestClient.getInstance();
            WebTarget webTarget = instance.getClient().target(local_SRV_TARGET).path("/extract/doit");
            webTarget = webTarget.queryParam("id", "-111");
            webTarget = webTarget.queryParam("connection", "DocuOracle");
            Response response = webTarget.request().get();

                if (response.getStatus() != 200) {
                    if (response.hasEntity()) {
                        String errorAsString = response.readEntity(String.class);
                        System.out.println(errorAsString);
                        Map<String, Object> errorMap = (Map<String, Object>) JSonUtils.json2Object(errorAsString, Map.class);
                        System.out.println("corId:" + errorMap.get("corelationId"));
                        System.out.println("customMessage:" + errorMap.get("customMessage"));
                        System.out.println("exception:" + errorMap.get("stackTrace"));

                    }
                    fail();
                }else{
                    System.out.println("**** " + response.getEntity().toString());
                    assertTrue(response.getStatus() == 200);
                }
        } catch (DbErrorException e) {
            fail("bd error:" + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            fail();
            throw new RuntimeException(e);
        }
    }


}
