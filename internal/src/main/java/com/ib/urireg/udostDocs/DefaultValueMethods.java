package com.ib.urireg.udostDocs;

import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author n.kanev
 */
public class DefaultValueMethods {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultValueMethods.class);
    private static final String DB_ERROR_MSG = "Грешка при работа с базата";

    private final UserData userData;
    private final SystemData systemData;

    public DefaultValueMethods(UserData userData, SystemData systemData, Map<String, Object> additionalData) {
        this.userData = userData;
        this.systemData = systemData;
    }

}
