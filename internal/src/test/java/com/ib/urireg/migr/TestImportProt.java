package com.ib.urireg.migr;

import com.ib.system.utils.*;
import com.ib.urireg.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.urireg.utils.MigrUtils;
import jakarta.persistence.Query;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestImportProt {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestImportProt.class);


    private static HashMap<String, Integer> sadilista = new HashMap<String, Integer>();


    private static Integer counter =0;

    public static void main(String[] args) {


        try {



            SystemData sd = new SystemData();

            MigrUtils.init(5000000, sd);



            List<SystemClassif> sadClassif = sd.getSysClassification(206 , new Date(), 1);
            for (SystemClassif classif : sadClassif) {
                sadilista.put(classif.getTekst().toUpperCase().trim(), classif.getCode());
            }


            File dir = new File("D:\\_mp\\08.10.2025\\Protokoli");
            File[] allFiles = dir.listFiles();

            JPA.getUtil().begin();

            JPA.getUtil().getEntityManager().createNativeQuery("delete from temp_prot").executeUpdate();
            int cnt = 0;
            for (File file : allFiles) {
                if (file.getName().toLowerCase().contains("protokol") && ! file.getName().contains("~")) {
                    System.out.println();
                    System.out.println();
                    System.out.println("*** Proccessing file: " + file.getName());
                    byte[] bytes = FileUtils.getBytesFromFile(file);
                    parseFile(bytes, file.getName());
                    cnt++;
                    //break;
                }
            }

            JPA.getUtil().getEntityManager().createNativeQuery("delete from temp_spis").executeUpdate();
            File spisFile = new File("D:\\_mp\\08.10.2025\\SpisakProt\\spisak Protokoli.xlsx");
            byte[] bytes = FileUtils.getBytesFromFile(spisFile);
            parseFileSpis(bytes);


            JPA.getUtil().commit();


            System.out.println("-------- end ------" + cnt);

        } catch (Exception e) {
            e.printStackTrace();
            JPA.getUtil().rollback();
        }finally {
            JPA.getUtil().closeConnection();
        }
    }

    private static void parseFileSpis(byte[] bytes) {

        try {

            System.out.println("****** Започваме списъка с протоколите ..... ");

            XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));  // for xlsx


            if (workbook.getNumberOfSheets() > 0) {

                XSSFSheet sheet = workbook.getSheetAt(0);

                //Първо въртене за да видим протоколите
                for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {



                    XSSFRow row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }

                    Date datProt = null;
                    try {
                        datProt = MigrUtils.getDate(row.getCell(1));



                    } catch (Exception e) {
                        //LOGGER.error("Грешна дата на удостоверение за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                        System.out.println("Грешна дата на протокол: "  + row.getCell(1).toString() + "   " + e.getMessage());
                    }

                    String preds = MigrUtils.getString(row.getCell(3));
                    String chlen1 = MigrUtils.getString(row.getCell(4));
                    String chlen2 = MigrUtils.getString(row.getCell(5));
                    String chlen3 = MigrUtils.getString(row.getCell(6));
                    String chlen4 = MigrUtils.getString(row.getCell(7));

                    String members = "";
                    Integer nomChl = 0;
                    if (!SearchUtils.isEmpty(chlen1)) {
                        nomChl++;
                        members += nomChl + ". " +chlen1.trim() + "\r\n";
                    }
                    if (!SearchUtils.isEmpty(chlen2)) {
                        nomChl++;
                        members += nomChl + ". " +chlen2.trim() + "\r\n";
                    }
                    if (!SearchUtils.isEmpty(chlen3)) {
                        nomChl++;
                        members += nomChl + ". " +chlen3.trim() + "\r\n";
                    }
                    if (!SearchUtils.isEmpty(chlen4)) {
                        nomChl++;
                        members += nomChl + ". " +chlen4.trim() + "\r\n";
                    }

                    String zapoved = MigrUtils.getString(row.getCell(2));

                    String nomZap = null;
                    Date datZap = null;

                    if (!SearchUtils.isEmpty(zapoved) && ! zapoved.equals("СД-03")) {
                        if (! zapoved.contains("/")){
                            System.out.println("Грешен формат на заповед: " + zapoved);
                        }else{

                            zapoved = zapoved.replace("СД-03-533/12.10.2015","СД-03-534/12.10.2015");

                            String[] parts =  zapoved.split("\\/");
                            //System.out.println(parts.length);
                            nomZap = parts[0];
                            if (nomZap != null) {
                                nomZap = nomZap.replace("№", "").trim();
                            }else{
                                nomZap = "N/A";
                            }

                            if (nomZap.equals("СД-03.582")) {
                                nomZap = "СД-03-582";
                            }

                            if (nomZap.equals("СД-03.392")) {
                                nomZap = "СД-03-392";
                            }
                            if (nomZap.equals("СД-01-489")) {
                                nomZap = "СД-03-489";
                            }



                            nomZap = nomZap.replace("–", "-");
                            nomZap = nomZap.replace(" ", "");



                            String datS = parts[1].trim().toLowerCase();
                            if (datS.contains("г")){
                                datS = datS.substring(0,datS.indexOf("г")).trim();
                            }


                            if (datS.length()>14){
                                System.out.println("Странна дата: " + datS);
                            }

                            if (datS.equals("1007.2015")){
                                datS = "10.07.2015";
                            }

                            if (datS.equals("11.012013")){
                                datS = "11.01.2013";
                            }

                            if (datS.equals("07.02.11")){
                                datS = "07.02.2011";
                            }

                            if (datS.equals("28,09,2023")){
                                datS = "28.09.2023";
                            }

                            try {
                                datZap = MigrUtils.getDate(datS);
                            } catch (Exception e) {
                                //LOGGER.error("Грешна дата на удостоверение за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                                System.out.println("Грешна дата на заповед: "  + datS + "   " + e.getMessage());
                            }

                            if (datZap ==null || datZap.getTime() < DateUtils.systemMinDate().getTime()){
                                System.out.println(datS + "--> " + datZap);
                            }




                        }

                    }

                    JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO temp_spis (id, zap_full, zap_nom, zap_date, preds, chlenove, dat_prot) VALUES (?, ?, ?, ?, ?, ?, ?)")
                            .setParameter(1, i)
                            .setParameter(2, zapoved)
                            .setParameter(3, nomZap)
                            .setParameter(4, datZap)
                            .setParameter(5, preds)
                            .setParameter(6, members)
                            .setParameter(7, datProt)
                            .executeUpdate();


                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public static void parseFile(byte[] bytes, String fileName) {

        TreeSet<Date> dates = new TreeSet<Date>();

        try{
            boolean started = false;
            XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));  // for xlsx


            if (workbook.getNumberOfSheets() > 0) {

                XSSFSheet sheet = workbook.getSheetAt(0);

                //Първо въртене за да видим протоколите
                for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {

                    counter++;

                    XSSFRow row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }

                    String nom = MigrUtils.getString(row.getCell(0));
                    try {
                        Integer.parseInt(nom.trim());
                        started = true;
                    } catch (NumberFormatException e) {
                        if (started) {
                            System.out.println("************************* СЧУПЕН РЕД *************************************");
                        }
                        continue;
                    }

                    Date datProt = null;
                    try {
                        datProt = MigrUtils.getDate(row.getCell(2));
                    } catch (UnexpectedResultException e) {
                        System.out.println(" *******************    Празна дата на протокол");
                    }


                    String ime = MigrUtils.getString(row.getCell(3));
                    String prezime = MigrUtils.getString(row.getCell(4));
                    String familia = MigrUtils.getString(row.getCell(5));

                    Date datRajd = null;

                    String egn = MigrUtils.getString(row.getCell(8));

                    if (egn == null) {
                        egn = egn.trim();
                    }

                    if (egn != null && egn.length() == 8) {
                        egn = "00" + egn;
                    }
                    if (egn != null && egn.length() == 9) {
                        egn = "0" + egn;
                    }

                    //System.out.println("-->" + egn);


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



                    Date datRajd2 = null;
                    try {
                        datRajd2 = MigrUtils.getDate(row.getCell(6));
                    } catch (UnexpectedResultException e) {
                        //LOGGER.error("Грешна дата на удостоверение за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                        System.out.println("Грешна или липсваща дата на раждане за лице с ЕГН " + egn + ": " + e.getMessage());
                    }


                    String miasto = MigrUtils.getString(row.getCell(7));
                    String grad = MigrUtils.getString(row.getCell(9));
                    String adres = MigrUtils.getString(row.getCell(10));
                    String sad = MigrUtils.getString(row.getCell(11));

                    //System.out.println("sad-->" + sad);

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

                        Date datKraiStaj = null;
                        try {
                            datKraiStaj = MigrUtils.getDate(row.getCell(12));
                        } catch (UnexpectedResultException e) {
                            System.out.println("Грешна или липсваща дата за край на стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                        }

                        String izdarjal = MigrUtils.getString(row.getCell(13));
                        String uni = MigrUtils.getString(row.getCell(14));

                        if (datRajd == null || datRajd2 != null) {
                            datRajd = datRajd2;
                        }

                        Query q = JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO temp_prot (id, dat_prot, ime, prezime, familia, dat_rajd, miasto_rajd, egn, grad, adres, sad, kraj_staj, izdarjal, zavarshil, file_name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                        q.setParameter(1, counter);
                        q.setParameter(2, datProt);
                        q.setParameter(3, ime);
                        q.setParameter(4, prezime);
                        q.setParameter(5, familia);
                        q.setParameter(6, datRajd);
                        q.setParameter(7, miasto);
                        q.setParameter(8, egn);
                        q.setParameter(9, grad);
                        q.setParameter(10,adres);
                        q.setParameter(11,sadId);
                        q.setParameter(12,datKraiStaj);
                        q.setParameter(13,izdarjal);
                        q.setParameter(14,uni);
                        q.setParameter(15,fileName);
                        q.executeUpdate();


                    }


                }


            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }









}
