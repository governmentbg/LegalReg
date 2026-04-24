package com.ib.urireg.quartz;

import com.ib.urireg.rest.client.SimpleRestClient;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.quartz.BaseJobResult;
import com.ib.system.utils.JSonUtils;
import com.ib.system.utils.SearchUtils;
import jakarta.persistence.Query;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

/**
 * Извличане и запис в отделна колона на съдържание на файлове.
 * Предполага се че в тази колона ще има текстов индекс , по който да се търси
 *
 * @author krasig
 */
@DisallowConcurrentExecution
public class IndexFileContentJob implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexFileContentJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LOGGER.info("==== Start IndexFileContentJob ====");
		SystemData sd = null;

		try {
			ServletContext servletContext = (ServletContext) jobExecutionContext.getScheduler().getContext().get("servletContext");
			if (servletContext == null) {
				LOGGER.error("********** servletcontext is null job IndexFileContentJob will be terminated!!!! **********");
				return;
			}
			sd = (SystemData) servletContext.getAttribute("systemData");
			if (sd == null) {
				LOGGER.error("Cannot get SystemData from context. Creating new SystemData !!!!!");
				sd = new SystemData();
			}

		} catch (Exception e) {
			LOGGER.error("Error accesing ServletContext!", e);
			JobExecutionException ex = new JobExecutionException(e);
			ex.setRefireImmediately(false);
			throw ex;
		}

		//Обработка на файловете
		BaseJobResult result = proccessFiles(sd);
		jobExecutionContext.setResult(result);

        LOGGER.info("==== End IndexFileContentJob ====");
    }


    @SuppressWarnings("unchecked")
	public BaseJobResult proccessFiles(SystemData sd) throws JobExecutionException {

    	BaseJobResult jobResult = new BaseJobResult();
		jobResult.setStatus(UriregConstants.CODE_ZNACHENIE_JOB_STATUS_EMPTY);
		jobResult.setComment(null);

		int brAll = 0;
		int brGreshni = 0;
		int brOk = 0;

		int maxItemsInBatch = 30;

		ArrayList<String> errors = new ArrayList<String>();
		ArrayList<Object[]> files = new ArrayList<Object[]> ();
		int cnt = 0;

		String info = "";

		String targetServerURL = null;
		String dbDSN = null;
		SimpleRestClient restClient = SimpleRestClient.getInstance();
		//WebTarget webTarget = null;
    	try {

			Query q = JPA.getUtil().getEntityManager().createNativeQuery("select FILE_ID, FILENAME from files where PROCESSED is null order by FILE_ID");
			q.setMaxResults(maxItemsInBatch);
			files = (ArrayList<Object[]>) q.getResultList();
			cnt = files.size();
			LOGGER.info("Returned files on this run (max "+maxItemsInBatch+"): " + cnt);

			targetServerURL = new SystemData().getSettingsValue("ib_indexer_addres");
			if (targetServerURL == null || targetServerURL.equals("")) {
				LOGGER.error("ib_indexer_addres not found in system settings");
				JobExecutionException ex = new JobExecutionException("ib_indexer_addres not found in system settings");
				ex.setRefireImmediately(false);
				throw ex;
			}

			dbDSN = new SystemData().getSettingsValue("ib_indexer_dsn");
			if (dbDSN == null || dbDSN.equals("")) {
				LOGGER.error("ib_indexer_dsn not found in system settings");
				JobExecutionException ex = new JobExecutionException("ib_indexer_dsn not found in system settings");
				ex.setRefireImmediately(false);
				throw ex;
			}

			//webTarget = restClient.getClient().target(targetServerURL).path("/extract/doit");

		} catch (JobExecutionException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("Unexpected error getting file info and settings", e);
			JobExecutionException ex =  new JobExecutionException("Unexpected error getting file info and settings", e);
			ex.setRefireImmediately(false);
			throw ex;
		}finally {
			JPA.getUtil().closeConnection();
		}

		for (Object[] tek : files) {
			String filename = null;
			Integer id = null;
			brAll++;
			try {

				id = SearchUtils.asInteger(tek[0]);
				filename = SearchUtils.asString(tek[1]);

				LOGGER.info(brAll + ". Proccessing file with id= " + id + ", fileName= " + filename);
				WebTarget webTarget = restClient.getClient().target(targetServerURL).path("/extract/doit");
				webTarget = webTarget.queryParam("id", id);
				webTarget = webTarget.queryParam("connection", dbDSN);
				Response response = webTarget.request().get();
				LOGGER.info("Response Status="+response.getStatus());
				LOGGER.info("Response Entity="+response.getEntity());
				if (response.getStatus() != 200) {
					brGreshni++;
					info += "(id=" + id + ") " + filename + "\tERROR\r\n";

					if (response.hasEntity()) {
						String errorAsString = response.readEntity(String.class);
						Map<String, Object> errorMap = (Map<String, Object>) JSonUtils.json2Object(errorAsString, Map.class);
						LOGGER.info("corId:" + errorMap.get("corelationId"));
						LOGGER.info("customMessage:" + errorMap.get("customMessage"));
						LOGGER.info("exception:" + errorMap.get("stackTrace"));

						String error = filename + ": " + errorMap.get("customMessage") + "\r\n";
						errors.add(error);

					}

				}else{
					brOk++;
					info += "(id=" + id + ") " + filename + "\tOK\r\n";

				}



			} catch (Exception e) {
				LOGGER.error("Unexpected error on file {} with id {}", filename, id, e);
				if (e.getCause() != null) {
					errors.add(filename + ": " + e.getCause().getMessage() + "\r\n");
				}else{
					errors.add(filename + ": " + e.getMessage() + "\r\n");
				}
			}
		}




    	if  (brGreshni > 0) {
			if (brGreshni == brAll){
				jobResult.setStatus(UriregConstants.CODE_ZNACHENIE_JOB_STATUS_ERROR);
			}else {
				jobResult.setStatus(UriregConstants.CODE_ZNACHENIE_JOB_STATUS_WARN);
			}
		}else {
			if (brAll > 0) {
				jobResult.setStatus(UriregConstants.CODE_ZNACHENIE_JOB_STATUS_OK);
			}else {
				jobResult.setStatus(UriregConstants.CODE_ZNACHENIE_JOB_STATUS_EMPTY);
			}

		}

		String comment = "";




		comment += "Общо обработени: " + brAll + "\r\n";
		comment += "Брой обработени с грешка: " + brGreshni + "\r\n";
		comment += "Общо обработени без грешка: " + brOk + "\r\n";

		comment+= "\r\n" + info;

		jobResult.setComment(comment);


		String desc = "";
		for (String row : errors) {
			desc += "***** " + row + "\r\n";
		}

		jobResult.setDescription(desc);


		LOGGER.info("Comment:\r\n"  + jobResult.getComment());
		LOGGER.info("Description:\r\n"  + jobResult.getDescription());

		return jobResult;

    }

}



