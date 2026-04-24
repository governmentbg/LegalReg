package com.ib.urireg.db.dao;

import com.ib.urireg.db.dto.ShablonFile;
import com.ib.urireg.db.dto.ShablonLogic;
import com.ib.urireg.system.UriregConstants;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DAO for {@link ShablonLogic}
 *
 * @author belev
 */
public class ShablonLogicDAO extends AbstractDAO<ShablonLogic> {

	/** DAO for {@link ShablonFile} */
	static class ShablonFileDAO extends AbstractDAO<ShablonFile> {

		/** @param user */
		protected ShablonFileDAO(ActiveUser user) {
			super(ShablonFile.class, user);
		}
	}

	/** @param user */
	public ShablonLogicDAO(ActiveUser user) {
		super(ShablonLogic.class, user);
	}

	/**
	 * Зарежда обекта за конкретен файл
	 *
	 * @param docVid
	 * @return
	 * @throws DbErrorException
	 */ // TODO
	public List<ShablonLogic> findByDocVid(Integer docVid) throws DbErrorException {
		if (docVid == null) {
			return null;
		}

		List list;
		try {
			list = createQuery("select x from ShablonLogic x where x.docVid = ?1") //
					.setParameter(1, docVid).getResultList();
			if (list.isEmpty()) {
				return null;
			}

			for(Object obj : list) {
				((ShablonLogic) obj).getBookmarks().size(); // lazy
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на ShablonLogic по docVid=" + docVid, e);
		}
		return list;
	}

	/** */
	@Override
	public ShablonLogic findById(Object id) throws DbErrorException {
		if (id == null) {
			return null;
		}

		ShablonLogic entity = super.findById(id);
		if (entity == null) {
			return entity;
		}
		entity.getBookmarks().size(); // lazy

		return entity;
	}

	/**
	 * @return distinct na всички имена на методи
	 * @throws DbErrorException
	 */
	public List<Object> findCurrentlyUsedMethodNames() throws DbErrorException {
		try {
			@SuppressWarnings("unchecked")
			List<Object> list = createNativeQuery( //
					"select distinct(method_name) from shablon_bookmarks where method_name is not null order by 1").getResultList();
			return list;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на имена на методи на шаблони.", e);
		}
	}

	/**
	 * Търсене на Логика за попълване на шаблон по име на метод, който се използва.</br>
	 * [0]-logic_id</br>
	 * [1]-logic_file_id=logic_id</br>
	 * [2]-filename</br>
	 * [3]-doc_vid</br>
	 *
	 * @param methodName
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findMethodUsage(String methodName) throws DbErrorException {
		methodName = SearchUtils.trimToNULL(methodName);
		if (methodName == null) {
			return new ArrayList<>();
		}

		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select distinct l.id logic_id, l.id logic_file_id, l.filename, l.doc_vid ");
			sql.append(" from shablon_bookmarks b ");
			sql.append(" inner join shablon_logic l on l.id = b.shablon_logic_id ");
			sql.append(" where upper(b.method_name) = ?1 ");
			sql.append(" order by l.filename ");

			@SuppressWarnings("unchecked")
			List<Object[]> list = createNativeQuery(sql.toString()).setParameter(1, methodName.toUpperCase()) //
					.getResultList();
			return list;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на Логика за попълване на шаблон по име на метод = " + methodName, e);
		}
	}

	/**
	 * Търсене на файлове темплейти през класификация Видове удостоверителни документи (688).</br>
	 * [0]- doc_vid</br>
	 * [1]- doc_vid_tekst</br>
	 * [2]- shablon_count </br>
	 *
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findTemplates(BaseSystemData systemData) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select sc.code doc_vid, '' doc_vid_tekst, count(l) shablon_count ");
			sql.append(" from system_classif sc ");
			sql.append(" left outer join shablon_logic l on l.doc_vid = sc.code ");
			sql.append(" where sc.code_classif = ?1 and sc.date_do is null ");
			sql.append(" group by sc.code ");
			sql.append(" order by sc.code ");

			@SuppressWarnings("unchecked")
			List<Object[]> list = createNativeQuery(sql.toString()) //
					.setParameter(1, UriregConstants.CODE_CLASSIF_SHABLONI) //
					.getResultList();

			for (Object[] row : list) {
				row[1] = systemData.decodeItem(UriregConstants.CODE_CLASSIF_SHABLONI,
						SearchUtils.asInteger(row[0]), getUserLang(), null);
			}

			return list;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на файлове темплейти", e);
		}
	}

	/**
	 * Получава ИД на файл с уърдовски шаблон и връща списък с деловодните документите,
	 * които могат да го ползват за генериране на удостоверителен документ.
	 * Използва се от екрана за попълване на шаблоните за УД с тестова цел.
	 *
	 * <ul>
	 *   <li>[0] Doc.id</li>
	 *   <li>[1] Vpisvane.id</li>
	 *   <li>[2] Doc.otnosno</li>
	 *   <li>[3] Doc.rnDoc</li>
	 *   <li>[4] Vpisvane.status</li>
	 *   <li>[5] Vpisvane.code_page</li>
	 * </ul>
	 *
	 * @param logicId ИД на файла от ShablonLogic.id
	 */ // TODO
	public List<Object[]> getDocAndVpisvaneThatUseShablon(Integer logicId) throws DbErrorException {
		try {

			String sql = "select d.id, v.id, d.otnosno, d.rnDoc, v.status, v.codePage"
					+ " from ShablonLogic f"
					+ " inner join Doc d on d.docVid = f.docVid"
					+ " inner join VpisvaneDoc vd on vd.idDoc = d.id"
					+ " inner join Vpisvane v on v.id = vd.idVpisvane"
					+ " where f.id = :logicId";

			@SuppressWarnings("unchecked")
			List<Object[]> results = createQuery(sql)
					.setParameter("logicId", logicId)
					.getResultList();
			return results;
		}
		catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на документи, които използват файла като шаблон за УД", e);
		}
	}

	/**
	 * Извличане на съдържание на файл
	 *
	 * @param id
	 * @return
	 * @throws DbErrorException
	 */
	public ShablonFile loadFile(Integer id) throws DbErrorException {
		if (id == null) {
			return null;
		}
		ShablonFile found = new ShablonFileDAO(getUser()).findById(id);
		return found;
	}

	/**
	 * Коригира имената на методите
	 *
	 * @return броя на коригираните
	 * @throws DbErrorException
	 */
	public int renameUsedMethod(Integer logicId, String oldName, String newName) throws DbErrorException {
		try {
			int cnt = createNativeQuery( //
					"update shablon_bookmarks set method_name = :newName where method_name = :oldName") //
					.setParameter("oldName", oldName).setParameter("newName", newName).executeUpdate();

			String ident = "oldName=" + oldName + "; newName=" + newName;
			SystemJournal journal = new SystemJournal(UriregConstants.CODE_ZNACHENIE_JOURNAL_SHABLON_LOGIC, logicId, ident);

			journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			journal.setDateAction(new Date());
			journal.setIdUser(getUserId());

			saveAudit(journal);

			return cnt;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на имена на методи на шаблони.", e);
		}
	}

	/** */
	@Override
	public ShablonLogic save(ShablonLogic entity) throws DbErrorException {
		if (entity.getBookmarks() == null) {
			entity.setBookmarks(new ArrayList<>()); // за да може да се направи повторен запис след първият
		}
		return super.save(entity);
	}

	/**
	 * Запис на съдържание на файл
	 *
	 * @param shablonFile
	 * @return
	 * @throws DbErrorException
	 */
	public ShablonFile saveFile(ShablonFile shablonFile) throws DbErrorException {
		ShablonFile saved = new ShablonFileDAO(getUser()).save(shablonFile);

		String ident = shablonFile.getFilename();
		SystemJournal journal = new SystemJournal(UriregConstants.CODE_ZNACHENIE_JOURNAL_SHABLON_LOGIC, shablonFile.getId(), ident);

		journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
		journal.setDateAction(new Date());
		journal.setIdUser(getUserId());

		saveAudit(journal);

		return saved;
	}
}
