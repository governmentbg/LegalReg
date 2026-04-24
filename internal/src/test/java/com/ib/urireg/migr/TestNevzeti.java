package com.ib.urireg.migr;

import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.FileUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;
import com.ib.system.utils.ValidationUtils;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.utils.MigrUtils;
import jakarta.persistence.NoResultException;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class TestNevzeti {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestNevzeti.class);





    public static void main(String[] args) {


        try {



            SystemData sd = new SystemData();

            MigrUtils.init(0, sd);






            File dir = new File("D:\\_mp\\08.10.2025\\Nevzeti");
            File[] allFiles = dir.listFiles();

            JPA.getUtil().begin();
            for (File file : allFiles) {
                if (file.getName().toLowerCase().contains("nevzeti")) {
                    System.out.println();
                    System.out.println();
                    System.out.println("*** Proccessing file: " + file.getName());
                    byte[] bytes = FileUtils.getBytesFromFile(file);
                    parseFile(bytes, file.getName());
                    //break;
                }
            }

            JPA.getUtil().getEntityManager().createNativeQuery("update lice set broi_izpit = 0 where broi_izpit is null").executeUpdate();


            JPA.getUtil().commit();


            System.out.println("-------- end ------");

        } catch (Exception e) {
            e.printStackTrace();
            JPA.getUtil().rollback();
        }finally {
            JPA.getUtil().closeConnection();
        }
    }


    public static void parseFile(byte[] bytes, String fileName) {

        try{
            boolean started = false;
            XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));  // for xlsx


            if (workbook.getNumberOfSheets() > 0) {

                XSSFSheet sheet = workbook.getSheetAt(0);
                for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {
                    //System.out.println("row " + i);
                    XSSFRow row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }

                    String egn = MigrUtils.getString(row.getCell(5));

                    if (egn == null) {
                        egn = egn.trim();
                    }

                    if (egn != null && egn.length() == 8) {
                        egn = "00" + egn;
                    }
                    if (egn != null && egn.length() == 9) {
                        egn = "0" + egn;
                    }


                    Integer broi = Integer.parseInt(MigrUtils.getString(row.getCell(6)));

                    Integer liceId = null;

                    try {
                        liceId = SearchUtils.asInteger(JPA.getUtil().getEntityManager().createNativeQuery("select lice_id from lice where egn = ? or lnc = ?").setParameter(1, egn).setParameter(2,egn).getSingleResult());
                    } catch (Exception e) {

                    }
                    if (liceId == null){
                        System.out.println("Не може да се намер лице с ЕГН/ЛНЧ: " + egn + " в БД !");
                    }else {

                        TreeMap<String, Object> columns = new TreeMap<String, Object>();
                        TreeMap<String, Object> keys = new TreeMap<String, Object>();
                        String tableName = "lice";

                        keys.put("lice_id", liceId);
                        columns.put("broi_izpit", broi);

                        MigrUtils.updateTable(tableName, columns, keys);
                    }

                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }









}
