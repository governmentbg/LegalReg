package com.ib.urireg.experimental;

import com.ib.urireg.system.SystemData;
import com.ib.indexui.db.dto.UniversalReport;
import com.ib.indexui.report.uni.SprAttributes;
import com.ib.indexui.report.uni.SprObject;
import com.ib.indexui.report.uni.SprValues;
import com.ib.system.ObjectsDifference;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.utils.JAXBHelper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.fail;

public class TestUserXML {



	@Test
	public void testUserXML(){
		try {
			SystemData sd = new SystemData();
			Integer lang = SysConstants.CODE_DEFAULT_LANG;

			UniversalReport ur = (UniversalReport) JPA.getUtil().getEntityManager().createQuery("from UniversalReport where id = 601").getSingleResult();
			System.out.println(ur.getContentXml());

			SprObject spr = JAXBHelper.xmlToObject(SprObject.class, ur.getContentXml());


//			DocSearch dsEmpty = new DocSearch();
//

			List<ObjectsDifference> diffs = convertSprObjectToDifferences(spr, "", sd, new Date()) ;
			for (ObjectsDifference diff : diffs) {
				System.out.println(diff.getFieldName());
			}




		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}


	}

	private List<ObjectsDifference> convertSprObjectToDifferences(SprObject spr, String path, SystemData sd, Date dat) {

		path = path + spr.getNameObject() + "\\";

		List<ObjectsDifference> diffs = new ArrayList<ObjectsDifference>();
		for (SprAttributes attr : spr.getAttributes()) {

			if (attr.getNameAttr() == null) {
				if (attr.getObject() != null) {
					//System.out.println(attr.getObject().getNameObject());
					diffs.addAll(convertSprObjectToDifferences(attr.getObject(), path, sd, dat));
				}
			}else {
				//System.out.println(attr.getNameAttr());
				ObjectsDifference diff = new ObjectsDifference();
				diff.setFieldName(path + attr.getNameAttr());
				if (attr.getValues().size() == 1) {
					diff.setNewVal(attr.getValues().get(0).toString(sd,dat));
					diffs.add(diff);
				}else {
					for (SprValues val : attr.getValues()) {
						ObjectsDifference diffVal = new ObjectsDifference();
						diffVal.setNewVal(val.toString(sd, dat));
						diff.getCoplexDif().add(diffVal);
					}
				}
			}


		}



		return diffs;
	}

}
