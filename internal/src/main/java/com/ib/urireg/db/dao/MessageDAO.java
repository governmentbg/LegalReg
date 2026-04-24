package com.ib.urireg.db.dao;

import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.Files;
import com.ib.system.exceptions.DbErrorException;
import com.ib.urireg.db.dto.Message;
import com.ib.urireg.db.dto.MessageLang;
import com.ib.urireg.system.UriregConstants;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * ДАО за {@link Message}
 */
public class MessageDAO extends AbstractDAO<Message> {

	public MessageDAO(ActiveUser user) {
		super(Message.class, user);
	}


	/**
	 * Зарежда съобщението по id и подаден език.
	 *
	 * @param messageId
	 * @param lang
	 * @return Message
	 * @throws DbErrorException
	 */
	public Message loadMessageWithLang(Integer messageId, Integer lang) throws DbErrorException {
		if (messageId == null || lang == null) {
			return null;
		}

		try {
			String sql = "SELECT DISTINCT m " +
					"FROM Message m " +
					"LEFT JOIN FETCH m.messageLangs ml " +
					"WHERE m.id = :id AND (ml.lang = :lang OR ml.lang IS NULL)";

			TypedQuery<Message> query = JPA.getUtil().getEntityManager().createQuery(sql, Message.class);
			query.setParameter("id", messageId);
			query.setParameter("lang", lang);

			List<Message> result = query.getResultList();
			return result.isEmpty() ? null : result.get(0);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при зареждане на съобщение с ID=" + messageId + " и език=" + lang, e);
		}
	}



	/**
	 * Записва съобщение и свързаните с него MessageLang
	 *
	 * @param message
	 * @throws DbErrorException
	 */
	public void saveMessageWithLangs(Message message) throws DbErrorException {
		if (message == null) {
			throw new IllegalArgumentException("Съобщението не може да бъде null");
		}

		try {

			Message persisted = saveMessage(message);
			saveMessageLangs(persisted);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при запис на Message и MessageLang: " + e.getMessage(), e);
		}
	}


	private Message saveMessage(Message message) throws DbErrorException {
		return super.save(message);
	}

	private void saveMessageLangs(Message message) {
		if (message.getMessageLangs() != null) {
			for (MessageLang ml : message.getMessageLangs()) {
				ml.setMessageId(message.getId());
				JPA.getUtil().getEntityManager().merge(ml);
			}
		}
	}


	/**
	 * Изтрива съобщение и всички негови messageLangs
	 *
	 * @param messageId
	 * @throws DbErrorException
	 */
	public void deleteMessageWithLangs(Integer messageId) throws DbErrorException {

		if (messageId == null) {
			throw new IllegalArgumentException("ID на съобщението не може да бъде null");
		}

		try {

			Message message = loadMessageWithLang(messageId, null);

			if (message != null && message.getMessageLangs() != null) {
				for (MessageLang ml : message.getMessageLangs()) {
					JPA.getUtil().getEntityManager().remove(JPA.getUtil().getEntityManager().contains(ml) ? ml : JPA.getUtil().getEntityManager().merge(ml));
				}
			}

			deleteById(messageId);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на Message с ID=" + messageId, e);
		}
	}



	/**
	 * Търси всички активни съобщения за показване във външната част
	 *
	 * @param lang /за сега се използва само български/
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> findMessagesRest(Integer lang) throws DbErrorException {

		try {

			List<Object[]> resultList;
			StringBuilder sql = new StringBuilder();

			sql.append(" select ");
			sql.append("    message.id as a0, ");
			sql.append("    message.date_from as a2, ");
			sql.append("    message.date_to as a3, ");
			sql.append("    lang.title as a4, ");
			sql.append("    lang.message_text as a5, ");
			sql.append("    lang.lang as a6, ");
			sql.append("    json_agg( ");
			sql.append("        json_build_object( ");
			sql.append("            'file_id', f.file_id, ");
			sql.append("            'file_info', f.file_info, ");
			sql.append("            'filename', f.filename ");
			sql.append("        ) ");
			sql.append("    ) as files ");
			sql.append(" from message ");
			sql.append(" left join message_lang lang ");
			sql.append("    on lang.message_id = message.id and lang.lang = :lang ");
			sql.append(" left join file_objects fo ");
			sql.append("    on fo.object_id = message.id and fo.object_code = :objectCode ");
			sql.append(" left join files f ");
			sql.append("    on f.file_id = fo.file_id ");
			sql.append(" where message.status = :status ");
			sql.append(" group by ");
			sql.append("    message.id, message.date_from, message.date_to, ");
			sql.append("    lang.title, lang.message_text, lang.lang ");
			sql.append(" order by message.date_from desc ");
			sql.append(" ; ");


			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString())
					.setParameter("lang", lang)
					.setParameter("status", UriregConstants.CODE_ZNACHENIE_MESSAGE_ACTIVE)
					.setParameter("objectCode", UriregConstants.CODE_ZNACHENIE_JOURNAL_MESSAGE);

			resultList = (List<Object[]>) query.getResultList();

			return resultList;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на съобщения");
		}
	}


	
	public Files downloadFileRest(Integer id) throws DbErrorException {
		try {

			String sql = "SELECT f " +
					"FROM Files f inner join FileObject fo on fo.fileId = f.id " +
					"WHERE f.id = :fileId " +
					"AND fo.objectCode = :objectCode";

			TypedQuery<Files> typedQuery = getEntityManager()
					.createQuery(sql, Files.class)
					.setParameter("fileId", id)
					.setParameter("objectCode", UriregConstants.CODE_ZNACHENIE_JOURNAL_MESSAGE);

			Files file = typedQuery.getSingleResult();

			if (file == null) {
				throw new DbErrorException("Файлът не е намерен");
			}

			return file;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при сваляне на файл");
		}
	}

}

