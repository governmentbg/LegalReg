package com.ib.urireg.udostDocs;

import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 * @author n.kanev
 */
public class AdvancedUdostMethods {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedUdostMethods.class);
    private static final String DB_ERROR_MSG = "Грешка при работа с базата";

    private final UserData userData;
    private final SystemData systemData;
    private final Date date;

    public AdvancedUdostMethods(UserData userData, SystemData systemData, Map<String, Object> additionalData) {
        this.userData = userData;
        this.systemData = systemData;
        this.date = new Date();
    }


}
