package com.ib.urireg.udostDocs;

import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author n.kanev
 */
public class OnCompleteMethods {

	private static final Logger LOGGER = LoggerFactory.getLogger(OnCompleteMethods.class);
	private static final String DB_ERROR_MSG = "Грешка при работа с базата";

    private final UserData userData;
    private final SystemData systemData;

    public OnCompleteMethods(UserData userData, SystemData systemData) {
        this.userData = userData;
        this.systemData = systemData;
    }

}
