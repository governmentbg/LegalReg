package com.ib.urireg.db.dao;

import com.ib.urireg.db.dto.Lice;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import org.junit.Test;

import static org.junit.Assert.*;

public class LiceDAOTest {

	@Test
	public void findByEgnLnc() {
		try {
			Lice lice = new LiceDAO(ActiveUser.DEFAULT).findByEgnLnc("1111111111", null);
			if (lice != null) {
				System.out.println(lice.getNames());
			}
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}

	}
}
