package com.ib.urireg.migr;

import com.ib.system.db.JPA;
import com.ib.system.utils.SearchUtils;

import java.util.List;

public class UpdateLiceStatuses {

    public static void main(String[] args) {


        String sql = "SELECT DISTINCT ON (lice.lice_id)\n" +
                "       lice.lice_id,\n" +
                "       ir.case_result,\n" +
                "       i.case_date\n" +
                "FROM izpit_result ir join lice on ir.lice_id = lice.lice_id\n" +
                "JOIN izpit i ON i.izpit_id = ir.izpit_id\n" +
                "where lice.status in (1,2,3)\n" +
                "ORDER BY lice.lice_id, i.case_date DESC";


        List<Object[]> result = JPA.getUtil().getEntityManager().createNativeQuery(sql).getResultList();
        System.out.println("*********************" + result.size());
        try {
            JPA.getUtil().begin();
            int cnt = 0;
            for (Object[] row : result){
                 cnt++;
                System.out.println("Обработка на лице " +  cnt + " oт " + result.size());
                int liceId = SearchUtils.asInteger(row[0]);
                Integer caseresult = SearchUtils.asInteger(row[1]);

                if (caseresult != null){
                    Integer status = 3;
                    switch(caseresult) {
                        case 1:
                            // Издържал
                            status = 5;
                            break;
                        case 2:
                            // Неиздържал
                            status = 6;
                            break;
                    }

                    JPA.getUtil().getEntityManager().createNativeQuery("update lice set status = ? where lice_id = ?")
                            .setParameter(1, status)
                            .setParameter(2, liceId)
                            .executeUpdate();


                }

            }
            System.out.println("... Doing final update ....");
            JPA.getUtil().getEntityManager().createNativeQuery("update lice set broi_izpit =(select count (izpit_result) from izpit_result where case_result in (1,2) and izpit_result.lice_id = lice.lice_id)").executeUpdate();
            JPA.getUtil().commit();
        } catch (Exception e) {
            e.printStackTrace();
            JPA.getUtil().rollback();
        }finally {
            JPA.getUtil().closeConnection();
        }


    }
}
