package com.ib.urireg.experimental;

import com.ib.system.db.JPA;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSimpleSave {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSimpleSave.class);





	@Test
	public void testHelloSecret (){


		try {

			JPA.getUtil().begin();

			JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO delem(id) 	VALUES(0)").executeUpdate();
			JPA.getUtil().getEntityManager().flush();

			try {
				JPA.getUtil().getEntityManager().createNativeQuery("select idd from delem").getResultList();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}


			JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO delem(id) 	VALUES(1)").executeUpdate();
			JPA.getUtil().getEntityManager().flush();



			JPA.getUtil().commit();

			System.out.println("end");

		} catch (Exception e) {
			e.printStackTrace();
			JPA.getUtil().rollback();
		}






	}







}
