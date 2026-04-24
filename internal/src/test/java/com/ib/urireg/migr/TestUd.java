package com.ib.urireg.migr;

import com.ib.urireg.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.FileUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;
import com.ib.system.utils.ValidationUtils;
import com.ib.urireg.utils.MigrUtils;
import jakarta.persistence.NoResultException;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;

public class TestUd {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestUd.class);


    private static HashMap<String, Integer> sadilista = new HashMap<String, Integer>();
    private static HashMap<String, String> foundEgn = new HashMap<String, String>();


    public static void main(String[] args) {


        try {

            Integer userId = -1;
            Date datReg = new Date();

            SystemData sd = new SystemData();

            MigrUtils.init(0, sd);



            List<SystemClassif> sadClassif = sd.getSysClassification(206 , new Date(), 1);
            for (SystemClassif classif : sadClassif) {
                sadilista.put(classif.getTekst().toUpperCase().trim(), classif.getCode());
            }


            File dir = new File("D:\\_mp\\08.10.2025\\Udostoverenia");
            File[] allFiles = dir.listFiles();

            JPA.getUtil().begin();
            for (File file : allFiles) {
                if (file.getName().toLowerCase().contains("izdadeni")) {
                    System.out.println();
                    System.out.println();
                    System.out.println("*** Proccessing file: " + file.getName());
                    byte[] bytes = FileUtils.getBytesFromFile(file);
                    parseFile(bytes, file.getName());
                    //break;
                }
            }


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

                    String nomUd = MigrUtils.getString(row.getCell(0));
                    try {
                        Integer.parseInt(nomUd.trim());
                        started = true;
                    } catch (NumberFormatException e) {
                        if (started) {
                            System.out.println("************************* СЧУПЕН РЕД *************************************");
                        }
                        continue;
                    }


                    Date datRajd = null;
                    Date datUd = null;
                    Date datRajd2 = null;
                    Date datProt = null;
                    Date datKraiStaj = null;

                    String ime = MigrUtils.getString(row.getCell(2));
                    String prezime = MigrUtils.getString(row.getCell(3));
                    String familia = MigrUtils.getString(row.getCell(4));
                    String miasto = MigrUtils.getString(row.getCell(7));

                    String dubl = MigrUtils.getString(row.getCell(13));
                    String zabelejka = MigrUtils.getString(row.getCell(14));



                    String egn = MigrUtils.getString(row.getCell(5));

//                    if (egn.equals("8702076535")){
//                        System.out.println("*** <UNK> <UNK> <UNK> <UNK> <UNK> <UNK> <UNK> <UNK>");
//                    }

                    if (egn == null) {
                        egn = egn.trim();
                    }


                    if (egn != null && egn.length() == 8) {
                        egn = "00" + egn;
                    }
                    if (egn != null && egn.length() == 9) {
                        egn = "0" + egn;
                    }


                    boolean isEgn = false;
                    boolean valid = ValidationUtils.isValidEGN(egn);
                    if (!valid) {
                        valid = ValidationUtils.isValidLNCH(egn);
                        if (!valid) {
                            System.out.println("Невалидно ЕГН/ЛНЧ: " + egn + " за лице " + ime + " " + prezime + " " + familia);
                        } else {
                            //System.out.println("ЕГН: " + egn + " за лице " + ime + " " + prezime + " " + familia + " e ЛНЧ") ;
                        }


                    } else {
                        isEgn = true;
                        datRajd = StringUtils.birthdayFromEGN(egn);
                    }



                    String sad = MigrUtils.getString(row.getCell(8));




                    Integer sadId = null;
                    if (!SearchUtils.isEmpty(sad)) {

                        if (sad.equals("Окръжен съд Софийски окръжен съд")) {
                            sad = "Софийски градски съд";
                        }

                        if (sad.equals("Окръжен съд Софийски градски съд")) {
                            sad = "Софийски градски съд";
                        }

                        if (sad.contains("СГС")) {
                            sad = "Софийски градски съд";
                        }

                        if (sad.equals("Софийски градски съд София")) {
                            sad = "Софийски градски съд";
                        }

                        if (sad.equals("Софийски окръжен съд София")) {
                            sad = "Софийски окръжен съд";
                        }

                        sadId = sadilista.get(sad.trim().toUpperCase());
                        if (sadId == null) {
                            System.out.println("*****************************" + sad);
                            throw new RuntimeException(sad + " is not found !!!");
                        }
                    }


//                    if (foundEgn.containsKey(egn)) {
//                        System.out.println("ПОВТОРНО СРЕЩАНЕ НА ЕГН " + egn + " в предишен файл: " + foundEgn.get(egn));
//                        continue;
//                    } else {
//                        foundEgn.put(egn, fileName);
//                    }


                    try {
                        datUd = MigrUtils.getDate(row.getCell(1));
                    } catch (UnexpectedResultException e) {
                        System.out.println("Грешна или липсваща дата на удостоверение за лице с ЕГН " + egn + ": " + e.getMessage());
                    }

                    try {
                        datRajd2 = MigrUtils.getDate(row.getCell(6));
                    } catch (UnexpectedResultException e) {
                        //LOGGER.error("Грешна дата на удостоверение за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                        System.out.println("Грешна или липсваща дата на раждане за лице с ЕГН " + egn + ": " + e.getMessage());
                    }

                    try {
                        datKraiStaj = MigrUtils.getDate(row.getCell(9));
                    } catch (UnexpectedResultException e) {
                        System.out.println("Грешна или липсваща дата за край на стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                    }

                    try {
                        datProt = MigrUtils.getDate(row.getCell(10));
                    } catch (UnexpectedResultException e) {
                        System.out.println("Грешна или липсваща дата на протокол за лице с ЕГН " + egn + ": " + e.getMessage());
                    }

                    if (datRajd == null && datRajd2 != null){
                        datRajd = datRajd2;
                    }

                    TreeMap<String, Object> columns = new TreeMap<String, Object>();
                    TreeMap<String, Object> keys = new TreeMap<String, Object>();
                    String tableName = null;



                    Integer liceId = SearchUtils.asInteger(JPA.getUtil().getEntityManager().createNativeQuery("select lice_id from lice where egn = ? or lnc = ?").setParameter(1, egn).setParameter(2,egn).getSingleResult());
                    if (liceId == null){
                        System.out.println("Не може да се намер лице с ЕГН/ЛНЧ: " + egn + " в БД !");
                    }else {

                        //ЛИЦЕ
                        columns.clear();
                        keys.clear();
                        tableName = "lice";

                        keys.put("lice_id", liceId);
                        if (datRajd != null) {
                            columns.put("birth_date", datRajd);
                        }

                        if (miasto != null) {
                            columns.put("birth_place", miasto);
                        }

                        columns.put("status", 7);
                        columns.put("status_date", datUd);

                        MigrUtils.updateTable(tableName, columns, keys);
                    }


                    //СТАЖ
                    Integer stajId = SearchUtils.asInteger(JPA.getUtil().getEntityManager().createNativeQuery("select staj_id from staj where lice_id = ? ").setParameter(1, liceId).getSingleResult());
                    if (stajId == null){
                        System.out.println("Не може да се намери стаж за лице с ЕГН/ЛНЧ: " + egn + " в БД !");
                    }else {
                        columns.clear();
                        keys.clear();
                        tableName = "staj";
                        keys.put("staj_id", stajId);


                        if (sadId != null) {
                            columns.put("osn_institution", sadId);
                        }

                        if (datKraiStaj != null) {
                            columns.put("osn_end_date", datKraiStaj);
                        }

                        MigrUtils.updateTable(tableName, columns, keys);

                    }

                    //УД-та

                    if (datUd != null){
                        Integer idUd = null;

                        try {
                            idUd = SearchUtils.asInteger(JPA.getUtil().getEntityManager().createNativeQuery("select doc.doc_id from doc join lice_doc on doc.doc_id = lice_doc.doc_id where doc.doc_vid = 9 and lice_doc.lice_id = ?")
                                                .setParameter(1,liceId).getSingleResult());
                        } catch (NoResultException e) {
                            //throw new RuntimeException(e);
                        }

                        if (idUd == null){
                            System.out.println("********** ЛИПСВА УД за ЕГН/ЛНЧ " +  egn );
                            //todo add
                        }else{
                            int orig = 1;
                            if (dubl != null && dubl.trim().equalsIgnoreCase("ДА")){
                                orig = 2;
                            }

                            JPA.getUtil().getEntityManager().createNativeQuery("update doc set original  = ?, doc_info = ? where doc_id = ?")
                                    .setParameter(1,orig)
                                    .setParameter(2,zabelejka)
                                    .setParameter(3,idUd)
                                    .executeUpdate();



                        }

                    }



                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }









}
