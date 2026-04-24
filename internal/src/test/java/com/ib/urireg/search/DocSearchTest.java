package com.ib.urireg.search;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DocSearchTest {

	@Test
	public void buildZapovedStajQuery() {
		try {
			DocSearch search = new DocSearch();

			search.buildZapovedStajQuery();

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

	@Test
	public void buildZapovedIzpitQuery() {
		try {
			DocSearch search = new DocSearch();

			search.buildZapovedIzpitQuery();

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
