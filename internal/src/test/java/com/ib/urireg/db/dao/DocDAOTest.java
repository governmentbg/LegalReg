package com.ib.urireg.db.dao;

import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.utils.DateUtils;
import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.system.UriregConstants;
import org.junit.Test;

import java.util.Date;

import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_STAJ;
import static org.junit.Assert.*;

public class DocDAOTest {

//		@Test
	public void delete() {
		try {
			DocDAO docDao = new DocDAO(ActiveUser.DEFAULT);

			Doc doc = docDao.findById(260);
			JPA.getUtil().closeConnection();

			JPA.getUtil().begin();

			docDao.delete(doc);

			JPA.getUtil().commit();

		} catch (Exception e) {
			JPA.getUtil().rollback();
			fail(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}

	}

//	@Test
	public void findByNomDateVid() {
		try {
			DocDAO docDao = new DocDAO(ActiveUser.DEFAULT);

			Doc doc = docDao.findByNomDateVid("СД-03-74", DateUtils.parse("21.08.2024"), CODE_ZNACHENIE_DOC_VID_ZAP_STAJ);

			if (doc != null) {
				System.out.println(doc.getId());
			}

		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
}
