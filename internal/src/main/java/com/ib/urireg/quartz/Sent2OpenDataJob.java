package com.ib.urireg.quartz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ib.system.db.JPA;
import com.ib.system.quartz.BaseJobResult;
import com.ib.system.utils.JSonUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.urireg.opendata.OpenDataObj;
import com.ib.urireg.rest.client.SimpleRestClient;
import com.ib.urireg.search.PublicRegister;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.Query;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Извличане и запис в отделна колона на съдържание на файлове.
 * Предполага се че в тази колона ще има текстов индекс , по който да се търси
 *
 * @author krasig
 */
@DisallowConcurrentExecution
public class Sent2OpenDataJob implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sent2OpenDataJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LOGGER.info("==== Start Sent2OpenDataJob ====");

		SystemData systemData=null;
		try {
			ServletContext servletContext = (ServletContext) jobExecutionContext.getScheduler().getContext().get("servletContext");
			if (servletContext == null) {
				LOGGER.warn("*** jobExecutionContext is null !!!");
				return;
			}
			systemData = (SystemData) servletContext.getAttribute("systemData");
		} catch (Exception e) {

		}finally {
			//Ако не стане - правим чисто нова
			if (null==systemData){
				LOGGER.info("*** SystemData is null. Generating new one !!!");
				systemData=new SystemData();
			}
		}



		try {
			BaseJobResult result = proccessPublicRegister(systemData);


			jobExecutionContext.setResult(result);

		} catch (Exception e) {
			LOGGER.error("Error sending data to OpenData server !", e);
			JobExecutionException ex = new JobExecutionException(e);
			ex.setRefireImmediately(false);
			throw ex;
		}




        LOGGER.info("==== End Sent2OpenDataJob ====");
    }


    @SuppressWarnings("unchecked")
	public BaseJobResult proccessPublicRegister(SystemData sd) throws JobExecutionException {

    	BaseJobResult jobResult = new BaseJobResult();
		jobResult.setStatus(UriregConstants.CODE_ZNACHENIE_JOB_STATUS_OK);
		jobResult.setComment(null);
		jobResult.setDescription("");

    	try {
//			Jsonb jsonb = JsonbBuilder.create();
//			System.out.println("Provider: " + jsonb.getClass().getName());
			String srvTarget = sd.getSettingsValue("openData.srvTarget");
			if (SearchUtils.isEmpty(srvTarget)){
				throw new JobExecutionException("openData.srvTarget key not found in table SYSTEM_OPTIONS !!!");
			}

			String apiKey = sd.getSettingsValue("openData.apiKey");
			if (SearchUtils.isEmpty(apiKey)){
				throw new JobExecutionException("openData.apiKey key not found in table SYSTEM_OPTIONS !!!");
			}

			String resourseUri = sd.getSettingsValue("openData.ResourseUri");
			if (SearchUtils.isEmpty(resourseUri)){
				throw new JobExecutionException("openData.ResourseUri key not found in table SYSTEM_OPTIONS !!!");
			}

			SimpleRestClient instance = SimpleRestClient.getInstance();
// updateResourceData
// addResourceData
			WebTarget webTarget = instance.getClient().target(srvTarget).path("/updateResourceData");
			OpenDataObj oData = new OpenDataObj();
			oData.setApi_key(apiKey);
			oData.setResource_uri(resourseUri);

			String registerAsJson = null;
			List<Map<String,Object>> mappedResult = getData();


			jobResult.setComment("Брой записи в регистъра: " + mappedResult.size());


			try{
				registerAsJson = JSonUtils.object2json(mappedResult, false);
			} catch (JsonProcessingException e) {
				LOGGER.error("Error when converting public register to JSON", e);
				throw new JobExecutionException("Error when converting public register to JSON", e);
			}

//        oData.setExtension_format("csv");
//        String tmpData = "{"
//        + "\"headers\": [\"Данни\", \"Месец\", \"Брой\"],"
//        + "\"row1\": [\"тестови данни\", \"Юни\", 7]"
//        + "}";
			oData.setExtension_format("json");
			String tmpData = ""
//					 +"["
					+ registerAsJson
//                +"{\"Данни\":1, \"Месец\":2, \"Брой\":3},"
//                +"{\"Данни\":11, \"Месец\":21, \"Брой\":31}"
//					+"]"
					;

			oData.setData(tmpData);

//        Response response = webTarget.request().post(Entity.entity(oData, MediaType.APPLICATION_JSON));
			Response response = null;
			int status = Response.Status.OK.getStatusCode();
			try {
				response = webTarget.request().post(Entity.json(oData));
				status = response.getStatus();
				if (status != Response.Status.OK.getStatusCode()) {
					String errorMsg = response.readEntity(String.class);
					LOGGER.error("Error response: " + status + " - " + unescapeUnicode(errorMsg));
					throw new JobExecutionException("Expected status 200 but got " + status + ": " + unescapeUnicode(errorMsg));
				}
			} catch (Exception e) {
				LOGGER.error("Error executing openData rest service", e);
				throw new JobExecutionException("Error executing openData rest service", e);
			} finally {
				if (response != null) {
					response.close();
				}
			}





		} catch (JobExecutionException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("Unexpected error while sending public register to OpenData", e);
			JobExecutionException ex =  new JobExecutionException("Unexpected error while sending public register to OpenData", e);
			ex.setRefireImmediately(false);
			throw ex;
		}finally {
			JPA.getUtil().closeConnection();
		}

		return jobResult;

    }

	public String unescapeUnicode(String input) {
		// Prefix with a dummy property name so Properties can parse it
		Properties p = new Properties();
		try {
			p.load(new StringReader("x=" + input));
			return p.getProperty("x");
		} catch (Exception e) {
			return input; // fallback if something goes wrong
		}

	}

	public List<Map<String,Object>> getData() throws JobExecutionException {


		try{

			PublicRegister search = new PublicRegister();
			search.buildQuery();
			String sql = search.getSql();

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

			return mappedResult;


		} catch (Exception e) {
			LOGGER.error("Error when reading public register from DB", e);
			throw new JobExecutionException("Error when reading public register from DB", e);
		} finally {
			JPA.getUtil().closeConnection();
		}



	}

}



