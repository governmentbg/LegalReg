package com.ib.urireg.search;

import com.ib.system.utils.DateUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test for {@link StatReports}
 */
public class StatReportsTest {

//	@Test
	public void selectStatUniversitetIzpiti() {
		StatReports stat = new StatReports();
		try {
			List<Object[]> list = stat.selectStatUniversitetIzpiti(DateUtils.parse("01.01.2025"), DateUtils.parse("31.12.2025"));
			for (Object[] obj : list) {
				System.out.println(Arrays.toString(obj));
			}
			System.out.println(list.size());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

//	@Test
	public void selectStatProtUniversitetIzpiti() {
		StatReports stat = new StatReports();
		try {
			List<Object[]> list = stat.selectStatProtUniversitetIzpiti(DateUtils.parse("01.01.2020"), DateUtils.parse("31.12.2025"));
			for (Object[] obj : list) {
				System.out.println(Arrays.toString(obj));
			}
			System.out.println(list.size());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
