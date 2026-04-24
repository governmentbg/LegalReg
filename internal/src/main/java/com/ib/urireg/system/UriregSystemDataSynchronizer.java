package com.ib.urireg.system;

import com.ib.indexui.system.Constants;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.SystemDataSynchronizer;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Допълнително си синхронизира динамичните класификации
 *
 * @author belev
 */
public class UriregSystemDataSynchronizer extends SystemDataSynchronizer {

	private static final Logger LOGGER = LoggerFactory.getLogger(UriregSystemDataSynchronizer.class);

	/**
	 * @param systemData
	 * @param seconds
	 */
	UriregSystemDataSynchronizer(BaseSystemData systemData, Long seconds) {
		super(systemData, seconds);
	}

	/** */
	@Override
	protected void processDynamicClassif(Date h2Date) throws DbErrorException {
		super.processDynamicClassif(h2Date);

		List<?> list; // ще се използва за всички
		Set<Integer> codes = new HashSet<>(); // тък се събират тези, които трябва да се рефрешнат

		String h2DateParam = "h2Date";

//		ADM_USERS - SysConstants.CODE_CLASSIF_USERS
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.USER_ID) as syncme from ADM_USERS a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(SysConstants.CODE_CLASSIF_USERS);
			codes.add(Constants.CODE_CLASSIF_ADMIN_STR);
		}

//		ADM_REFERENTS - Constants.CODE_CLASSIF_ADMIN_STR
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.REF_ID) as syncme from ADM_REFERENTS a where a.CODE_CLASSIF = :codeClassif having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter("codeClassif", Constants.CODE_CLASSIF_ADMIN_STR).setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(Constants.CODE_CLASSIF_ADMIN_STR);
			codes.add(SysConstants.CODE_CLASSIF_USERS);
			codes.add(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS);
		}

//		MODEL_AIS - Constants.CODE_CLASSIF_INF_MODEL
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.AIS_ID) as syncme from MODEL_AIS a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(Constants.CODE_CLASSIF_INF_MODEL);
			this.systemData.getModel().reset();
		}

		if (!codes.isEmpty()) { // само ако има нещо
			LOGGER.info("Start reset dynamic classifications SystemData={}, h2Date={}", this.systemData.getClass().getName(), h2Date);

			for (Integer code : codes) {
				LOGGER.info("\tCODE_CLASSIF={}", code);

				this.systemData.reloadClassif(code, false, true);
			}
		}
	}
}
