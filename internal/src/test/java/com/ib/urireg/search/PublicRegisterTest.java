package com.ib.urireg.search;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.system.db.JPA;
import jakarta.persistence.Query;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test for {@link PublicRegister}
 */
public class PublicRegisterTest {

    @Test
    public void buildQueryKG() {

        // Default pagination values
        Integer page = 1;
        Integer pageSize = 2; // Set to null to get all rows
        Integer offset = (pageSize != null && pageSize > 0 ? (page * pageSize) : 0);


        String myJsonSQL = "" +
                "WITH total_count AS (\n" +
                "{SQL_FOR_COUNT}" +
                ")" +
                "SELECT \n" +
                "    json_build_object( 'data',json_agg(myResult), 'totalCount', \n" +
                "    (  select cnt from total_count  ),\n" +
                "        'page',:page,\n" +
                "        'pageSize',:pageSize," +
                "        'totalPages', CASE \n" +
                "            WHEN :pageSize IS NULL OR :pageSize <= 0 THEN 1\n" +
                "            ELSE CEIL((SELECT cnt FROM total_count)::numeric / :pageSize)\n" +
                "        END )\n" +
                "FROM \n" +
                "    (   {SQL_FOR_DATA} \n" +
                "        {PAGINATION}" +
                "        ) myResult";

        try {
            PublicRegister search = new PublicRegister();
            search.setFirstname("Гинка");
            search.buildQuery();
            myJsonSQL = myJsonSQL.replace("{SQL_FOR_COUNT}", search.getSqlCount());
            myJsonSQL = myJsonSQL.replace("{SQL_FOR_DATA}", search.getSql());

        if (pageSize != null && pageSize > 0) {
            myJsonSQL = myJsonSQL.replace("{PAGINATION}", "LIMIT :pageSize OFFSET :offset\n");
        } else {
            myJsonSQL = myJsonSQL.replace("{PAGINATION}", "");
        }

        System.out.println(myJsonSQL);
        Map<String, Object> sqlParameters = search.getSqlParameters();
        Query query = JPA.getUtil().getEntityManager().createNativeQuery(myJsonSQL);
        sqlParameters.forEach((key, value) -> {
            query.setParameter(key, value);

        });

        query.setParameter("page", page);

        if (pageSize != null && pageSize > 0) {
            query.setParameter("offset", offset);
            query.setParameter("pageSize", pageSize);
        } else {
            query.setParameter("pageSize", -1);
        }

        String singleResult = (String) query.getSingleResult();
        System.out.println(singleResult);
    } catch (Exception e) {
        fail(e.getMessage());
    } finally {
        JPA.getUtil().closeConnection();
    }
}
	/**
	 * Test for {@link PublicRegister#buildQuery()}
	 */
	@Test
	public void buildQuery() {
		try {
			PublicRegister search = new PublicRegister();
            search.setFirstname("Гинка");
			search.buildQuery();
            System.out.println(search.getSql());
            System.out.println(search.getSqlCount());

            Map<String, Object> sqlParameters = search.getSqlParameters();
            System.out.println("params:"+ sqlParameters.size());
            sqlParameters.forEach((key, value) -> System.out.println(key + " = " + value));

            Query query = JPA.getUtil().getEntityManager().createNativeQuery(search.getSql());
            sqlParameters.forEach((key, value) -> query.setParameter(key, value));
            List resultList = query.getResultList();
            System.out.println("resultList:"+resultList.size());
            JPA.getUtil().closeConnection();


//			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "a0");
//			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);
//
//			for (Object[] row : result) {
//				System.out.println(Arrays.toString(row));
//			}
//			System.out.println(result.size());

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
