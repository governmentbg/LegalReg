package com.ib.urireg.system;

import com.ib.urireg.experimental.GlobalHolder;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.BaseSystemData;
import com.ib.system.SystemDataSynchronizer;
import com.ib.system.db.JPA;
import jakarta.faces.application.Application;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.*;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Слуша за стартиране и спиране на приложението.
 */
public class MySystemListener implements SystemEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(MySystemListener.class);

	/** @see SystemEventListener#isListenerForSource(Object) */
	@Override
	public boolean isListenerForSource(Object source) {
		return source instanceof Application;
	}

	/** @see SystemEventListener#processEvent(SystemEvent) */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {

		if (event instanceof PostConstructApplicationEvent) {
			myApplicationInit(event);

		} else if (event instanceof PreDestroyApplicationEvent) {
			myApplicationDestroy(event);
		}
	}

	/** @param event */
	protected void myApplicationDestroy(final SystemEvent event) {
		LOGGER.info("myApplicationDestroy - START");

		try {
			LOGGER.debug("Trying to RESET H2DataContainer ...");

			ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
			BaseSystemData systemData = (BaseSystemData) servletContext.getAttribute("systemData");

			if (systemData != null) { // ! това се прави, защото H2-ката е от сървъра не се зачиства при махане на приложението
				systemData.resetH2DC();
				servletContext.removeAttribute("systemData");
			}
			GlobalHolder globalHolder = (GlobalHolder) servletContext.getAttribute("globalDocHolder");
			if (globalHolder!=null){
				servletContext.removeAttribute("globalDocHolder");
			}
		} catch (Exception e) {
			LOGGER.error("Error - RESET H2DataContainer !!!", e);
		}

		try {
			LOGGER.debug("Trying to STOP SystemDataSynchronizer ...");

			ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
			SystemDataSynchronizer synchronizer = (SystemDataSynchronizer) servletContext.getAttribute("systemDataSynchronizer");

			if (synchronizer != null) {
				servletContext.removeAttribute("systemDataSynchronizer");

				synchronizer.stopIt();
			}
		} catch (Exception e) {
			LOGGER.error("Error - STOP SystemDataSynchronizer !!!", e);
		}

		try {
			LOGGER.debug("Trying to SHUTDOWN JPA ...");

			JPA.shutdown();
		} catch (Exception e) {
			LOGGER.error("Error - SHUTDOWN JPA !!!", e);
		}

		LOGGER.info("myApplicationDestroy - END");
	}

	/** @param event */
	protected void myApplicationInit(final SystemEvent event) {
		LOGGER.info("myApplicationInit - START");

		BaseSystemData systemData = (BaseSystemData) JSFUtils.getManagedBean("systemData");
		GlobalHolder globalHolder = (GlobalHolder) JSFUtils.getManagedBean("globalDocHolder");

		ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
		servletContext.setAttribute("systemData", systemData);
		servletContext.setAttribute("globalDocHolder", globalHolder);

		try {
			LOGGER.debug("Trying to START SystemDataSynchronizer ...");

			String setting = systemData.getSettingsValue("system.syncSysCache");
			if (setting != null) { // само ако се иска това да работи по този начин
				SystemDataSynchronizer synchronizer = new UriregSystemDataSynchronizer(systemData, Long.parseLong(setting) * 60);

				servletContext.setAttribute("systemDataSynchronizer", synchronizer);

			} else {
				LOGGER.debug("SystemDataSynchronizer is TURNED OFF by setting 'system.syncSysCache' !!! ");
			}
		} catch (Exception e) {
			LOGGER.error("Error - START SystemDataSynchronizer !!!", e);
		}

		LOGGER.info("myApplicationInit - END");
	}
}
