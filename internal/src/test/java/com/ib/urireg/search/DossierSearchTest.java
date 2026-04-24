package com.ib.urireg.search;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.system.SysConstants;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DossierSearchTest {

	@Test
	public void buildFilterQuery() {
		try {
			DossierSearch search = new DossierSearch();

			search.setEmptyAddrEkatte(SysConstants.CODE_ZNACHENIE_DA);
			search.setEmptyAddrText(SysConstants.CODE_ZNACHENIE_DA);
			search.setEmptyUniversitet(SysConstants.CODE_ZNACHENIE_DA);
			search.setEmptyBirthPlace(SysConstants.CODE_ZNACHENIE_DA);

			search.buildFilterQuery();

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "a0");
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);

			for (Object[] row : result) {
				System.out.println(Arrays.toString(row));
			}
			System.out.println(result.size());

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
