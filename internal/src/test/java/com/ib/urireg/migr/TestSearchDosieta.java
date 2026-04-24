package com.ib.urireg.migr;

import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.FileUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;
import com.ib.system.utils.ValidationUtils;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.utils.MigrUtils;
import jakarta.persistence.Query;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestSearchDosieta {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestSearchDosieta.class);

    private static HashMap<String, String> foundEgn = new HashMap<String, String>();
    private static HashMap<String, Integer> zapovedi = new HashMap<String, Integer>();
    private static HashMap<String, Integer> sadilista = new HashMap<String, Integer>();

    private static Integer lastCodeUser = 0;

    private static Date dat2020 = null;

    public static void main(String[] args) {


        try {

            SystemData sd = new SystemData();

            MigrUtils.init(5000000, sd);


            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                dat2020 = sdf.parse("01.01.2020");
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            List<SystemClassif> sadClassif = sd.getSysClassification(206 , new Date(), 1);
            for (SystemClassif classif : sadClassif) {
                sadilista.put(classif.getTekst().toUpperCase().trim(), classif.getCode());
            }


            File dir = new File("D:\\_mp\\08.10.2025\\Dosieta");
            File[] allFiles = dir.listFiles();


            for (File file : allFiles) {
                if (file.getName().toLowerCase().contains("dosie")) {
                    System.out.println();
                    System.out.println();
                    System.out.println("*** Proccessing file: " + file.getName());
                    byte[] bytes = FileUtils.getBytesFromFile(file);
                    parseFile(bytes, file.getName());
                    //break;
                }
            }




            Iterator<Map.Entry<String, Integer>> it = zapovedi.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = it.next();
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }

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

            XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));  // for xlsx


            if (workbook.getNumberOfSheets() > 0) {

                XSSFSheet sheet = workbook.getSheetAt(0);
                for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {
                    //System.out.println("row " + i);
                    XSSFRow row = sheet.getRow(i);


                    String nom = MigrUtils.getString(row.getCell(1));
                    try {
                        Integer.parseInt(nom.trim());
                    } catch (NumberFormatException e) {
                        System.out.println("************************* СЧУПЕН РЕД *************************************");
                        continue;
                    }

                    String ime = MigrUtils.getString(row.getCell(4));
                    String prezime = MigrUtils.getString(row.getCell(5));
                    String familia = MigrUtils.getString(row.getCell(6));
                    String egn = MigrUtils.getString(row.getCell(7));
                    if (egn != null && egn.length() == 8) {
                        egn = "00" + egn;
                    }
                    if (egn != null && egn.length() == 9) {
                        egn = "0" + egn;
                    }


                    if (egn != null && egn.equals("8807314516")) {
                        System.out.println("found ************************** 1001664895 **************************************");
                    }

                    Date datRajd = null;
                    boolean isEgn = false;
                    boolean valid =  ValidationUtils.isValidEGN(egn);
                    if (!valid) {
                        valid = ValidationUtils.isValidLNCH(egn);
                        if (! valid) {
                            System.out.println("Невалидно ЕГН/ЛНЧ: " + egn + " за лице " + ime + " " + prezime + " " + familia);
                        }else{
                            //System.out.println("ЕГН: " + egn + " за лице " + ime + " " + prezime + " " + familia + " e ЛНЧ") ;
                        }


                    }else{
                        isEgn = true;
                        datRajd = StringUtils.birthdayFromEGN(egn);
                    }



                    String userReg = MigrUtils.getString(row.getCell(3));
                    String rnZaiavStaj = MigrUtils.getString(row.getCell(8));
                    String rnZapStaj = MigrUtils.getString(row.getCell(10));
                    String sad = MigrUtils.getString(row.getCell(12));
                    String rnZaiavIzpit = MigrUtils.getString(row.getCell(14));
                    String rnUdost = MigrUtils.getString(row.getCell(16));
                    Date datReg = null;
                    Date datZaiavStaj = null;
                    Date datZapStaj = null;
                    Date datKraiStaj = null;
                    Date datZaiavIzpit = null;
                    Date datUdost = null;

                    Integer sadId = null;
                    if (! SearchUtils.isEmpty(sad) ) {

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

                    Integer userId = MigrUtils.checkAndCreateUser(userReg);

                    if (foundEgn.containsKey(egn)){
                        System.out.println("ПОВТОРНО СРЕЩАНЕ НА ЕГН " + egn + " в предишен файл: " +  foundEgn.get(egn));
                        continue;
                    }else{
                        foundEgn.put(egn, fileName);
                    }

                    try {
                        datReg = MigrUtils.getDate(row.getCell(2));
                    } catch (UnexpectedResultException e) {
                        System.out.println("Грешна дата на регистрация за лице с ЕГН " + egn + ": " + e.getMessage());
                    }

                    Integer liceBefore = 2;
                    if (datReg != null && datReg.getTime() < dat2020.getTime()) {
                        liceBefore = 1;
                    }


                    try {
                        datZaiavStaj = MigrUtils.getDate(row.getCell(9));
                    } catch (UnexpectedResultException e) {
                        System.out.println("Грешна дата на завление за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                    }

                    try {
                        datZapStaj = MigrUtils.getDate(row.getCell(11));
                    } catch (UnexpectedResultException e) {
                        System.out.println("Грешна дата на заповед за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                    }
                    try {
                        datZaiavIzpit = MigrUtils.getDate(row.getCell(15));
                    } catch (UnexpectedResultException e) {
                        System.out.println("Грешна дата на завление за изпит за лице с ЕГН " + egn + ": " + e.getMessage());
                    }
                    try {
                        datUdost = MigrUtils.getDate(row.getCell(17));
                    } catch (UnexpectedResultException e) {
                        //LOGGER.error("Грешна дата на удостоверение за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                        System.out.println("Грешна дата на удостоверение за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                    }






                }








            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertLiceStatus(ArrayList<Object[]> allStat) {

        try {
            for (Object[] obj : allStat) {

                int id = MigrUtils.getSeq("seq_lice_status");
                Query q = JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO lice_status (status_id, lice_id, status, status_date) VALUES (?,?,?,?)");
                q.setParameter(1, id);
                q.setParameter(2, obj[0]);
                q.setParameter(3, obj[1]);
                q.setParameter(4, obj[2]);
                q.executeUpdate();


            }
        } catch (DbErrorException e) {
            throw new RuntimeException(e);
        }
    }


    public static void insertLiceDoc(int idLice, int idDoc) throws DbErrorException {

        int id = MigrUtils.getSeq("seq_lice_doc");
        Query q = JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO lice_doc (id, lice_id, doc_id, user_reg, date_reg) VALUES (?,?,?,?,?)");
        q.setParameter(1, id);
        q.setParameter(2, idLice);
        q.setParameter(3, idDoc);
        q.setParameter(4, -1);
        q.setParameter(5, new Date());
        q.executeUpdate();
    }


}
