package com.ib.urireg.db.dao;

import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import jakarta.persistence.Query;

import java.util.Date;
import java.util.List;

/**
 * За проверка, заключване и отключване на обекти. Използва само прости заявки без работа с обекти.
 *
 * @author belev
 */
public class LockObjectDAO {

	private static final String	CHECK_SQL		= "SELECT USER_ID, LOCK_DATE FROM LOCK_OBJECTS WHERE OBJECT_TIP = ?1 AND OBJECT_ID = ?2";
	private static final String	LOCK_SQL		= "INSERT INTO LOCK_OBJECTS(OBJECT_TIP, OBJECT_ID, USER_ID, LOCK_DATE) VALUES(?1, ?2, ?3, ?4)";
	private static final String	UNLOCK_SQL		= "DELETE FROM LOCK_OBJECTS WHERE USER_ID = ?1";
	private static final String	UNLOCK_SQL_TIP	= "DELETE FROM LOCK_OBJECTS WHERE USER_ID = ?1 AND OBJECT_TIP = ?2";

	/**
	 * Проверка за заключен обект. Ако върне NULL значи не е заключен <b>!!! Ако е заключен от подаденият userId се смята че не е
	 * заключен и пак дава NULL !!!</b> Ако е заключен от друг потребител връща данни от вида:<br>
	 * [0]-USER_ID<br>
	 * [1]-LOCK_DATE<br>
	 *
	 * @param userId
	 * @param objectTip
	 * @param objectId
	 * @return
	 * @throws DbErrorException
	 */
	public Object[] check(Integer userId, Integer objectTip, Integer objectId) throws DbErrorException {
		try {
			// проверката
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(CHECK_SQL) //
				.setParameter(1, objectTip).setParameter(2, objectId);

			@SuppressWarnings("unchecked")
			List<Object[]> rows = query.getResultList();
			if (rows.isEmpty()) {
				return null;
			}
			Object[] row = rows.get(0);

			int lockUser = ((Number) row[0]).intValue();
			if (lockUser == userId.intValue()) { // ако текущия усер е заключилият, то тогава няма проблем
				return null;
			}
			return row; // явно все пак е заключен

		} catch (Exception e) {
			throw new DbErrorException("Грешка при отключване на обект!", e);
		}
	}

	/**
	 * заключване на обект
	 *
	 * @param userId
	 * @param objectTip
	 * @param objectId
	 * @param unlockTip ако е NULL преди да заключи подадения прави отключване на всички за потребителя, а ако е конкретно число
	 *                  отключва само за подадения тип обкет
	 * @throws DbErrorException
	 */
	public void lock(Integer userId, Integer objectTip, Integer objectId, Integer unlockTip) throws DbErrorException {
		try {
			if (unlockTip == null) { // всичко да се отключи
				Query unlockQuery = JPA.getUtil().getEntityManager().createNativeQuery(UNLOCK_SQL) //
					.setParameter(1, userId);

				unlockQuery.executeUpdate();

			} else { // само от избрания тип обект
				Query query = JPA.getUtil().getEntityManager().createNativeQuery(UNLOCK_SQL_TIP) //
					.setParameter(1, userId).setParameter(2, objectTip);

				query.executeUpdate();
			}

			Query lockQuery = JPA.getUtil().getEntityManager().createNativeQuery(LOCK_SQL) //
				.setParameter(1, objectTip).setParameter(2, objectId).setParameter(3, userId).setParameter(4, new Date());

			lockQuery.executeUpdate();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при заключване на обект!", e);
		}
	}

	/**
	 * отключване на обекти - всичко за подадения усер
	 *
	 * @param userId
	 * @throws DbErrorException
	 */
	public void unlock(Integer userId) throws DbErrorException {
		try {
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(UNLOCK_SQL) //
				.setParameter(1, userId);

			query.executeUpdate();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при отключване на обекти!", e);
		}
	}

	/**
	 * отключване на обекти - за подадения усер всичко от този тип обект
	 *
	 * @param userId
	 * @param objectTip
	 * @throws DbErrorException
	 */
	public void unlock(Integer userId, Integer objectTip) throws DbErrorException {
		try {
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(UNLOCK_SQL_TIP) //
				.setParameter(1, userId).setParameter(2, objectTip);

			query.executeUpdate();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при отключване на обекти!", e);
		}
	}
}
