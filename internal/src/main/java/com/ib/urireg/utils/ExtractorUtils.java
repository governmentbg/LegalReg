package com.ib.urireg.utils;

import com.ib.urireg.rest.client.SimpleRestClient;
import com.ib.urireg.system.SystemData;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.JSonUtils;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class ExtractorUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractorUtils.class);

    public static void extractData(Integer fileId, SystemData sd ) throws Exception {

        try {
            String extractorUrl = sd.getSettingsValue("ib_indexer_addres");
            String extractorDSN = sd.getSettingsValue("ib_indexer_dsn");

            if (extractorUrl  == null || extractorDSN == null || extractorUrl.equals("") || extractorDSN.equals("")) {
                LOGGER.error("Extractor URL or DSN not found in system settings");
                throw new BaseException("Extractor URL or DSN not found in system settings");
            }

            SimpleRestClient instance = SimpleRestClient.getInstance();
            WebTarget webTarget = instance.getClient().target(extractorUrl).path("/extract/doit");
            webTarget = webTarget.queryParam("id", fileId);
            webTarget = webTarget.queryParam("connection", extractorDSN);
            Response response = webTarget.request().get();

            if (response.getStatus() != 200) {
                String errorAsString = "Error while extraction date from file !";
                if (response.hasEntity()) {
                    String entityAsString = response.readEntity(String.class);
                    Map<String, Object> errorMap = (Map<String, Object>) JSonUtils.json2Object(entityAsString, Map.class);
                    LOGGER.error("corId:" + errorMap.get("corelationId"));
                    LOGGER.error("customMessage:" + errorMap.get("customMessage"));
                    LOGGER.error("exception:" + errorMap.get("stackTrace"));
                    errorAsString += "\t" + entityAsString;
                }
                throw new BaseException(errorAsString);
            }


        } catch (DbErrorException e) {
            LOGGER.error("Error while extracting system options values", e);
            throw e;
        } catch (IOException e) {
            LOGGER.error("Error while catching rest service error", e);
            throw e;
        }


    }
}
