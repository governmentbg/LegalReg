package com.ib.urireg.udostDocs;

import com.ib.urireg.db.dao.DocDAO;
import com.ib.urireg.db.dao.IzpitDAO;
import com.ib.urireg.db.dao.LiceDAO;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Помощен клас с методи, които не се разкриват публично в екрана за шаблони.
 * Тук методите могат да хвърлят грешки.
 *
 * @author n.kanev
 */
public class Helpers {

	private static final Logger LOGGER = LoggerFactory.getLogger(Helpers.class);
	private static final String DB_ERROR_MSG = "Грешка при работа с базата";
	private static final String NENAMERENO_ZNACHENIE = "Ненамерено значение";

	private final UserData userData;
	private final SystemData systemData;
	private final Date date;

	private IzpitDAO izpitDao;
	private DocDAO docDao;
	private LiceDAO liceDao;

	public Helpers(UserData userData, SystemData systemData) {
		this.userData = userData;
		this.systemData = systemData;
		this.date = new Date();
	}

	public IzpitDAO getIzpitDao() {
		if(this.izpitDao == null) {
			this.izpitDao = new IzpitDAO(this.userData);
		}
		return this.izpitDao;
	}

	public DocDAO getDocDao() {
		if(this.docDao == null) {
			this.docDao = new DocDAO(this.userData);
		}
		return this.docDao;
	}

	public LiceDAO getLiceDao() {
		if(this.liceDao == null) {
			this.liceDao = new LiceDAO(this.userData);
		}
		return this.liceDao;
	}


	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	public void handleException(Exception e) {
		LOGGER.error(DB_ERROR_MSG, e);
	}
}
