package com.ib.urireg.db.dao;

import com.ib.urireg.db.dto.Doc;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.urireg.system.UriregConstants;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.ib.urireg.system.UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT;

public class IzpitDAOTest {

	@Test
	public void isTestResultEntered() {
		try {
			IzpitDAO dao = new IzpitDAO(ActiveUser.DEFAULT);

			boolean testResultEntered = dao.isTestResultEntered(123);

			System.out.println("testResultEntered: " + testResultEntered);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void selectLicaByProtId() {
		try {
			IzpitDAO dao = new IzpitDAO(ActiveUser.DEFAULT);
			List<Object[]> list = dao.selectLicaByProtId(1, 1);
			for (Object[] obj : list) {
				System.out.println(Arrays.toString(obj));
			}
			System.out.println("selectLicaByProtId: " + list.size());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	@Test
	public void findProtocolList() {
		try {
			IzpitDAO dao = new IzpitDAO(ActiveUser.DEFAULT);
			List<Doc> list = dao.findProtocolList(new Date(), 1);
			System.out.println("findProtocolList: " + list.size());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	@Test
	public void findIzpitResults() {
		try {
			IzpitDAO dao = new IzpitDAO(ActiveUser.DEFAULT);
			List<Object[]> list = dao.findIzpitResults(7000589, 7000587);
			System.out.println("findIzpitResults: " + list.size());

			int[] cnts = dao.countIzpitResults(list, UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT);
			System.out.println("countIzpitResults: " + Arrays.toString(cnts));

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	@Test
	public void selectCountTestResultEntered() {
		try {
			IzpitDAO dao = new IzpitDAO(ActiveUser.DEFAULT);
			int cnt = dao.selectCountTestResultEntered(5);
			System.out.println("selectCountTestResult: " + cnt);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	@Test
	public void selectNewIzpitLica() {
		try {
			Doc zapIzpit = new Doc();
			zapIzpit.setZaiavDate(new Date());
			zapIzpit.setDocVid(CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT);

			IzpitDAO dao = new IzpitDAO(ActiveUser.DEFAULT);
			List<Object[]> list = dao.selectNewIzpitLica(zapIzpit);
			for (Object[] obj : list) {
				System.out.println(Arrays.toString(obj));
			}
			System.out.println("selectNewIzpitLica: " + list.size());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
}
