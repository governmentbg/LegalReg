package com.ib.urireg.beans;

import com.ib.urireg.db.dao.LockObjectDAO;
import com.ib.urireg.system.UserData;
import com.ib.indexui.system.IndexUIbean;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@ViewScoped
public class SideBar extends IndexUIbean   {

    private static final Logger LOGGER = LoggerFactory.getLogger(SideBar.class);

    /**
     * Да отключи всички заключени обекти за текущия потребител при натискане на точка от менюто
     */
    public void unlockDoc() {
        // налага се защото при излизане от работния плот остават заключени обекти - там  preDestroy не може да се използва заради проблеми с навигацията и затова ViewScoped не може да е на omnifaces
        try {

            JPA.getUtil().runInTransaction(() -> new LockObjectDAO().unlock(getUserData(UserData.class).getUserId()));

        } catch (BaseException e) {
            LOGGER.error("Грешка при отключване на обекти! ", e);
        }

    }
}
