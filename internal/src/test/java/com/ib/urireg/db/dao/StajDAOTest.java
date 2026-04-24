package com.ib.urireg.db.dao;

import com.ib.system.ActiveUser;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class StajDAOTest {

//	@Test
	public void findStajList() {
		try {
			StajDAO dao = new StajDAO(ActiveUser.DEFAULT);

			List<Object[]> list = dao.findStajList(null, null);

			for (Object[] obj : list) {
				System.out.println(Arrays.toString(obj));
			}
			System.out.println(list.size());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
