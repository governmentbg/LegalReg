package com.ib.urireg.utils;

import com.ib.system.BaseSystemData;
import com.ib.urireg.db.dto.Lice;
import com.ib.urireg.system.SystemData;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class RegixUtils2Test {

	private static BaseSystemData systemData;

	@BeforeClass
	public static void setUp() {
		systemData = new SystemData();
	}

	@Test
	public void loadLiceByEgn() {
		try {
			Lice lice = new Lice();

			RegixUtils2.loadLiceByEgn(lice, "8506258485", true, true, systemData);

			System.out.println(lice.getNames());
			System.out.println(lice.getAddrText());

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
