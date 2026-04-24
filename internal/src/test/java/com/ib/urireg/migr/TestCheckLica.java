package com.ib.urireg.migr;

import com.ib.system.db.JPA;
import com.ib.system.utils.FileUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.urireg.utils.MigrUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.File;

public class TestCheckLica {

    public static void main(String[] args) {
        try {

            File spisFile = new File("D:\\_mp\\Vsichki lica - rezultati.xlsx");
            byte[] bytes = FileUtils.getBytesFromFile(spisFile);

            XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));  // for xlsx


            if (workbook.getNumberOfSheets() > 0) {

                XSSFSheet sheet = workbook.getSheetAt(0);

                //Първо въртене за да видим протоколите
                for (int i = 4; i < sheet.getLastRowNum() + 1; i++) {


                    XSSFRow row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }

                    String egn = MigrUtils.getString(row.getCell(8));
                    //System.out.println(egn);


                    try {
                        Integer id = SearchUtils.asInteger(JPA.getUtil().getEntityManager().createNativeQuery("select lice_id from lice where egn = ? or lnc = ?").setParameter(1, egn).setParameter(2, egn).getSingleResult());
                        if (id == null){
                            System.out.println("ВНИМАНИЕ ГРЕШКА В ЛОГИКАТА !!!!  Не може да се намер лице с ЕГН/ЛНЧ: " + egn + " в БД !");
                        }
                    } catch (Exception e) {
                        System.out.println("ВНИМАНИЕ ГРЕШКА В ЛОГИКАТА !!!!  Не може да се намер лице с ЕГН/ЛНЧ: " + egn + " в БД !");
                    }



                }
            }


        } catch (Exception e) {
            e.printStackTrace();

        }

    }
}
