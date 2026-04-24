package com.ib.urireg.migr;

import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.*;
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

public class TestDosieta {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDosieta.class);

    private static HashMap<String, String> foundEgn = new HashMap<String, String>();
    private static HashMap<String, Integer> zapovedi = new HashMap<String, Integer>();
    private static HashMap<String, Integer> sadilista = new HashMap<String, Integer>();

    private static Integer lastCodeUser = 0;

    private static Date dat2020 = null;

    public static void main(String[] args) {


        try {

            SystemData sd = new SystemData();

            MigrUtils.init(0, sd);


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

            JPA.getUtil().begin();
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


            JPA.getUtil().commit();

//            Iterator<Map.Entry<String, Integer>> it = zapovedi.entrySet().iterator();
//            while (it.hasNext()) {
//                Map.Entry<String, Integer> entry = it.next();
//                System.out.println(entry.getKey() + ": " + entry.getValue());
//            }

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

                    String dopInfo = "";

                    //System.out.println("row " + i);
                    XSSFRow row = sheet.getRow(i);




                    String nom = MigrUtils.getString(row.getCell(1));
                    try {
                        Integer.parseInt(nom.trim());
                    } catch (NumberFormatException e) {
                        System.out.println("************************* СЧУПЕН РЕД *************************************");
                        continue;
                    }

                    String userReg = MigrUtils.getString(row.getCell(3));
                    dopInfo = "Регистрирал досието: " + userReg + "\r\n";

                    String ime = MigrUtils.getString(row.getCell(4));
                    String prezime = MigrUtils.getString(row.getCell(5));
                    String familia = MigrUtils.getString(row.getCell(6));
                    String egn = MigrUtils.getString(row.getCell(7));
                    if (egn == null) {
                        egn = egn.trim();
                    }


                    if (egn != null && egn.length() == 8) {
                        egn = "00" + egn;
                    }
                    if (egn != null && egn.length() == 9) {
                        egn = "0" + egn;
                    }

                    Date datRajd = null;
                    boolean isEgn = false;
                    boolean valid =  ValidationUtils.isValidEGN(egn);
                    if (!valid) {
                        valid = ValidationUtils.isValidLNCH(egn);
                        if (! valid) {
                            System.out.println("Невалидно ЕГН/ЛНЧ: " + egn + " за лице " + ime + " " + prezime + " " + familia);
                            dopInfo += "Невалидно ЕГН/ЛНЧ: " + egn + "\r\n";
                        }else{
                            //System.out.println("ЕГН: " + egn + " за лице " + ime + " " + prezime + " " + familia + " e ЛНЧ") ;
                        }


                    }else{
                        isEgn = true;
                        datRajd = StringUtils.birthdayFromEGN(egn);
                    }





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

                    Integer userId = -1;//MigrUtils.checkAndCreateUser(userReg);


                    if (foundEgn.containsKey(egn)){
                        //System.out.println("ПОВТОРНО СРЕЩАНЕ НА ЕГН " + egn + " в предишен файл: " +  foundEgn.get(egn));
                        continue;
                    }else{
                        foundEgn.put(egn, fileName);
                    }

                    try {
                        datReg = MigrUtils.getDate(row.getCell(2));
                    } catch (UnexpectedResultException e) {
                        dopInfo += "Грешна дата на регистрация: " + row.getCell(2).toString() + "\r\n";
                        System.out.println("Грешна дата на регистрация за лице с ЕГН " + egn + ": " + e.getMessage());
                        datReg = DateUtils.systemMinDate();

                    }

                    Integer liceBefore = 2;
                    if (datReg != null && datReg.getTime() < dat2020.getTime()) {
                        liceBefore = 1;
                    }


                    try {
                        datZaiavStaj = MigrUtils.getDate(row.getCell(9));
                    } catch (UnexpectedResultException e) {
                        dopInfo += "Грешна дата за заявление за стаж: " + row.getCell(9).toString() + "\r\n";
                        System.out.println("Грешна дата на завление за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                        datZaiavStaj = DateUtils.systemMinDate();
                    }

                    try {
                        datZapStaj = MigrUtils.getDate(row.getCell(11));
                    } catch (UnexpectedResultException e) {
                        dopInfo += "Грешна дата на заповед за стаж: " + row.getCell(11).toString() + "\r\n";
                        System.out.println("Грешна дата на заповед за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                        datZapStaj = DateUtils.systemMinDate();
                    }
                    try {
                        datZaiavIzpit = MigrUtils.getDate(row.getCell(15));
                    } catch (UnexpectedResultException e) {
                        dopInfo += "Грешна дата завление за изпит: " + row.getCell(15).toString() + "\r\n";
                        System.out.println("Грешна дата на завление за изпит за лице с ЕГН " + egn + ": " + e.getMessage());
                        datZaiavIzpit = DateUtils.systemMinDate();
                    }
                    try {
                        datUdost = MigrUtils.getDate(row.getCell(17));
                    } catch (UnexpectedResultException e) {
                        //LOGGER.error("Грешна дата на удостоверение за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                        dopInfo += "Грешна дата на удостоверение за стаж: " + row.getCell(17).toString() + "\r\n";
                        System.out.println("Грешна дата на удостоверение за стаж за лице с ЕГН " + egn + ": " + e.getMessage());
                        datUdost = DateUtils.systemMinDate();
                    }


                    //Заявление за стаж
                    Integer seqDocZaiavStaj = MigrUtils.getSeq("seq_doc");
                    String sqlDoc = "INSERT INTO doc (doc_id, doc_vid, zaiav_date, rn_doc, doc_date, doc_info, user_reg, date_reg) VALUES (?,?,?,?,?,?,?,?)";
                    Query query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
                    query.setParameter(1, seqDocZaiavStaj);
                    query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAIAV_STAJ);
                    query.setParameter(3, null);
                    query.setParameter(4, rnZaiavStaj);
                    query.setParameter(5, datZaiavStaj);
                    query.setParameter(6, "Заявление за стаж от " + ime + " " + prezime + " " + familia + "(ЕГН/ЛНЧ: " +egn + ")");
                    query.setParameter(7, userId);
                    query.setParameter(8, datReg);
                    //System.out.println(seqDoc + " Заявление за стаж от " + ime + " " + prezime + " " + familia);
                    query.executeUpdate();

                    //Заповед за стаж
                    Integer seqDocZapStaj = null;
                    if (datZapStaj != null) {

                        String hash = MigrUtils.getDateHash(datZapStaj);
                        Integer id = zapovedi.get(hash);
                        if (id != null) {
                            seqDocZapStaj = id;
                        }else {

                            seqDocZapStaj = MigrUtils.getSeq("seq_doc");
                            query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
                            query.setParameter(1, seqDocZapStaj);

                            query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_STAJ);
                            query.setParameter(3, null);
                            query.setParameter(4, rnZapStaj);
                            query.setParameter(5, datZapStaj);
                            query.setParameter(6, null);
                            query.setParameter(7, userId);
                            query.setParameter(8, datReg);
                            //System.out.println(seqDoc + " Заявление за стаж от " + ime + " " + prezime + " " + familia);
                            query.executeUpdate();
                            zapovedi.put(hash, seqDocZapStaj);
                        }
                    }


                    //Заявление за изпит
                    Integer seqDocZaiavIzpit = null;
                    if (datZaiavIzpit != null) {
                        seqDocZaiavIzpit = MigrUtils.getSeq("seq_doc");
                        query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
                        query.setParameter(1, seqDocZaiavIzpit);
                        query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAIAV_IZPIT);
                        query.setParameter(3, datZaiavIzpit);
                        query.setParameter(4, rnZaiavIzpit);
                        query.setParameter(5, datZaiavIzpit);
                        query.setParameter(6, "Заявление за изпит от " + ime + " " + prezime + " " + familia + "(ЕГН/ЛНЧ: " + egn + ")");
                        query.setParameter(7, userId);
                        query.setParameter(8, datReg);
                        //System.out.println(seqDoc + " Заявление за стаж от " + ime + " " + prezime + " " + familia);
                        query.executeUpdate();
                    }

                    //Удостоверение
                    Integer seqDocUdost = null;
                    if (datUdost != null || ! SearchUtils.isEmpty(rnUdost)) {
                        seqDocUdost = MigrUtils.getSeq("seq_doc");
                        query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
                        query.setParameter(1, seqDocUdost);
                        query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_UDOST_PRAVO);
                        query.setParameter(3, null);
                        query.setParameter(4, rnUdost);
                        query.setParameter(5, datUdost);
                        query.setParameter(6, "Удостоверение на " + ime + " " + prezime + " " + familia + "(ЕГН/ЛНЧ: " + egn + ")");
                        query.setParameter(7, userId);
                        query.setParameter(8, datReg);
                        //System.out.println(seqDoc + " Заявление за стаж от " + ime + " " + prezime + " " + familia);
                        query.executeUpdate();
                    }


//                    if (egn.equals("8506216807")){
//                        System.out.println("**************************************************************************" + egn);
//                    }

                    ArrayList<Object[]> allStat = new ArrayList<Object[]>();


                    int seqLice = MigrUtils.getSeq("seq_lice");
                    int status = 1;
                    Date datStatus = new Date();

                    if (datZapStaj != null) {
                        Object[] old = {seqLice, status, datStatus};
                        allStat.add(old);
                        status = 2;
                        datStatus = datZapStaj;
                    }

                    if (datZaiavIzpit != null) {
                        Object[] old = {seqLice, status, datStatus};
                        allStat.add(old);
                        status = 3;
                        datStatus = datZaiavIzpit;
                    }

                    if (datUdost != null || ! SearchUtils.isEmpty(rnUdost)) {
                        Object[] old = {seqLice, status, datStatus};
                        allStat.add(old);
                        status = 7;
                        if (datUdost != null) {
                            datStatus = datUdost;
                        }else{
                            datStatus = DateUtils.systemMinDate();
                        }

                    }




                    //Лице
                    String sqlLice = "INSERT INTO lice (lice_id, zaiav_staj_id, egn, lnc, firstname, surname, lastname, names, birth_date, last_zaiav_staj_id, udost_id, user_reg, date_reg, birth_place, status, status_date,do_2019, lice_info) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

                    query = JPA.getUtil().getEntityManager().createNativeQuery(sqlLice);
                    query.setParameter(1, seqLice);
                    query.setParameter(2, seqDocZaiavStaj);
                    if (isEgn){
                        query.setParameter(3, egn);
                    }else{
                        query.setParameter(3, null);
                    }

                    if (isEgn){
                        query.setParameter(4, null);
                    }else{
                        query.setParameter(4, egn);
                    }


                    query.setParameter(5, ime);
                    query.setParameter(6, prezime);
                    query.setParameter(7, familia);
                    query.setParameter(8, ime + " " + prezime + " " + familia);
                    query.setParameter(9, datRajd);
                    query.setParameter(10, seqDocZaiavStaj);
                    query.setParameter(11, seqDocUdost);
                    query.setParameter(12, userId);
                    query.setParameter(13, datReg);
                    query.setParameter(14, "");

                    query.setParameter(15, status); //??????????????????????????????????
                    query.setParameter(16, datStatus); //??????????????????????????????????
                    query.setParameter(17, liceBefore);
                    query.setParameter(18, dopInfo);


                    query.executeUpdate();

                    insertLiceStatus(allStat);



                    if (seqDocZaiavIzpit != null) {
                        insertLiceDoc(seqLice, seqDocZaiavIzpit);
                    }

                    if (seqDocUdost != null) {
                        insertLiceDoc(seqLice, seqDocUdost);
                    }

                    if (seqDocZaiavStaj != null) {
                        insertLiceDoc(seqLice, seqDocZaiavStaj);
                    }

                    if (seqDocZapStaj != null) {
                        insertLiceDoc(seqLice, seqDocZapStaj);
                    }

                    String sqlStaj = "INSERT INTO staj (staj_id, lice_id, zaiav_staj_id, zap_staj_id, staj_vid, osn_institution, osn_end_date, user_reg, date_reg, zaiav_izp_id) VALUES (?,?,?,?,?,?,?,?,?, ?)";
                    int seqStaj = MigrUtils.getSeq("seq_staj");
                    query = JPA.getUtil().getEntityManager().createNativeQuery(sqlStaj);
                    query.setParameter(1, seqStaj);
                    query.setParameter(2, seqLice);
                    query.setParameter(3, seqDocZaiavStaj);
                    query.setParameter(4, seqDocZapStaj);
                    query.setParameter(5, 1);
                    query.setParameter(6, sadId);
                    query.setParameter(7, datKraiStaj);
                    query.setParameter(8, userId);
                    query.setParameter(9, datReg);
                    query.setParameter(10, seqDocZaiavIzpit);
                    query.executeUpdate();



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
