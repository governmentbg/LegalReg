package com.ib.urireg.utils;

import com.ib.indexui.db.dto.AdmUser;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.SysClassifUtils;
import com.ib.urireg.db.dto.Referent;
import com.ib.urireg.system.SystemData;
import jakarta.persistence.Query;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MigrUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrUtils.class);

    private static HashMap<String, Integer> foundUsers = new HashMap<String, Integer>();
    private static Integer lastCodeUser = 0;

    public static int seqVal = 55000000;

    public static boolean isTest = true;

    private static  HashMap<Integer, Object[]> sadilista = new HashMap<Integer, Object[]>();



    public static void init(SystemData sd) throws UnexpectedResultException, DbErrorException {
        init(0, sd);
    }

    public static void init(int intialSeq, SystemData sd) throws UnexpectedResultException, DbErrorException {

        if (intialSeq == 0){
            isTest = false;
        }else{
            isTest = true;
        }
        seqVal = intialSeq;




        ArrayList<AdmUser> result = (ArrayList<AdmUser>) JPA.getUtil().getEntityManager().createQuery("from AdmUser").getResultList();
        for (AdmUser u  : result){
            foundUsers.put(u.getUsername(), u.getId());
        }

        List<SystemClassif> classif = sd.getSysClassification(24, new Date(), 1);
        SysClassifUtils.doSortClassifPrev(classif);
        for (SystemClassif sc : classif){
            if (sc.getCodeParent() == 1){
                lastCodeUser = sc.getCode();
            }
        }

        System.out.println("****************** LAST REF CODE = " + lastCodeUser);

    }

    public static Integer checkAndCreateUser(String userName) throws BaseException {

        try {
            if (foundUsers.containsKey(userName)) {
                return foundUsers.get(userName);
            }


            AdmUser admUser = new AdmUser();

            String names = userName.replace("/MoJ", "");
            names = names.replace("/PERFECT", "");
            names = names.replace("  ", " ");
            admUser.setUsername(names);

            Referent ref = new Referent();
            ref.setCodePrev(lastCodeUser);
            ref.setCode(getSeq("seq_adm_referents_code"));
            ref.setCodeParent(1);
            ref.setCodeClassif(24);
            ref.setRefType(2);
            ref.setRefName(names);
            ref.setDateOt(DateUtils.systemMinDate());
            ref.setDateDo(DateUtils.systemMaxDate());
            ref.setUserReg(-1);
            ref.setDateReg(DateUtils.systemMinDate());


            admUser.setUsername(userName);
            admUser.setPassword("df008b201205d70c47009b8972ae3d11:09a6e05af87726dca06c95c4abc98cbda5826c3e6e5630c68a08df474a48883d27985389e233ef1f8aae99a73431b4551b825e31c3ceacf4ce059cf54d0fe41e");
            admUser.setUserType(1);
            admUser.setLang(1);
            admUser.setStatus(2);
            admUser.setStatusDate(new Date());
            admUser.setPassLastChange(new Date());
            admUser.setUserReg(-1);
            admUser.setDateReg(new Date());
            admUser.setPassIsNew(false);
            admUser.setConfirmed(true);
            admUser.setNames(names);
            admUser.setLoginAttempts(0);



            JPA.getUtil().getEntityManager().persist(ref);
            admUser.setId(ref.getId());
            JPA.getUtil().getEntityManager().persist(admUser);


            foundUsers.put(userName, admUser.getId());
            lastCodeUser = ref.getCode();

            return admUser.getId();



        } catch (Exception e) {
            LOGGER.error("Грешка при създаване на нов потребител !!!", e);
            e.printStackTrace();
            throw new BaseException(e);
        }



    }




    public static int getSeq(String seqName) throws DbErrorException {

        if (isTest) {
            seqVal ++;

            if (seqVal == 7000756){
                System.out.println(seqVal);
            }

            return seqVal;
        }else {
            String sql = "SELECT nextval(:seqName)";
            try {
                Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql);
                query.setParameter("seqName", seqName);

                return SearchUtils.asInteger(query.getSingleResult());
            }catch (Exception e) {
                e.printStackTrace();
                throw new DbErrorException("Грешка при взимане на sequence " + seqName, e);
            }
        }


    }



    public static String getString(Cell cell) {
        if (cell != null ) {
            DataFormatter dataFormatter = new DataFormatter();
            return dataFormatter.formatCellValue(cell);

        }else {
            return "";
        }
    }

    public static Date getDate(Cell cell) throws UnexpectedResultException {

        Date date = null;
        try {
            if (cell == null ) {
                return null;
            }

            String s = cell.toString();
            if (SearchUtils.isEmpty(s)) {
                return null;
            }


            String datS = cell.toString();
            if (datS != null && datS.toUpperCase().contains("Г.")) {
                datS = datS.replace("г." , "");
                datS = datS.replace("Г." , "");
                date = getDate(datS);
                if (date == null) {
                    throw new UnexpectedResultException("****  Грешна дата " + cell.toString());
                }else{
                    return date;
                }
            }

            date = cell.getDateCellValue() ;
//            if (date.getTime() > new Date().getTime()) {
//                throw new UnexpectedResultException("****  Бъдеща дата " + cell.toString());
//            }

            return date;

        } catch (Exception e) {
            //LOGGER.error("****  Грешна дата " + cell.toString());
            throw new UnexpectedResultException("****  Грешна дата " + cell.toString());
        }
    }


    public static Date getDate(String datS) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("MM/DD.YY");

        Date date = null;

        try {
            date = sdf.parse(datS);
        } catch (ParseException e) {      }

        if (date == null) {
            try {
                date = sdf2.parse(datS);
            } catch (ParseException e) {      }
        }

        return date;
    }

    public static String getDateHash(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");

        return sdf.format(date);
    }


    public static int updateTable(String tableName, TreeMap<String, Object> columns, TreeMap<String, Object> keys ) throws DbErrorException {

        if (tableName == null || columns == null || keys == null || keys.size() == 0 || columns.size() == 0) {
            //throw new DbErrorException("Грешни параметри на метода за update");
            return 0;
        }

        String sql = "UPDATE " + tableName + " SET ";
        try {


            Iterator<Map.Entry<String, Object>> it = columns.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<String, Object> entry = it.next();
                sql += entry.getKey() + "= ?,";
            }
            sql = sql.substring(0, sql.length() - 1);
            sql += " WHERE ";
            it = keys.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<String, Object> entry = it.next();
                sql += entry.getKey() + "= ?,";
            }
            sql = sql.substring(0, sql.length() - 1);

            Query q = JPA.getUtil().getEntityManager().createNativeQuery(sql);

            int nom = 0;

            it = columns.entrySet().iterator();
            while (it.hasNext()){
                nom++;
                Map.Entry<String, Object> entry = it.next();
                q.setParameter(nom, entry.getValue());
            }

            it = keys.entrySet().iterator();
            while (it.hasNext()){
                nom++;
                Map.Entry<String, Object> entry = it.next();
                q.setParameter(nom, entry.getValue());
            }

            int count = q.executeUpdate();


            return count;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DbErrorException("Грешка при изпълнение на update " + sql, e);
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


    public static ArrayList<Object[]> recognizeMesto(ArrayList<Object[]> ekatte, String name, Integer sad) throws DbErrorException {


        if (name != null) {
            name = name.trim();
        }

        if (name != null && name.equalsIgnoreCase("Троян")) {
           // System.out.println(name);
        }

        ArrayList<Object[]> foundekkates = new ArrayList<Object[]>();
        for (Object[] obj : ekatte){
            String eGrad = SearchUtils.asString(obj[0]);
            String tvm = SearchUtils.asString(obj[4]);
            if (eGrad.equalsIgnoreCase(name) &&  tvm.equalsIgnoreCase("гр.")) {
                foundekkates.add(obj);
            }
        }

        if (foundekkates.size() == 1) {
            return foundekkates;
        }else{
            foundekkates.clear();
        }


        for (Object[] obj : ekatte){
            String eGrad = SearchUtils.asString(obj[0]);
            if (eGrad.equalsIgnoreCase(name)){
                foundekkates.add(obj);
            }
        }

        return foundekkates;


    }

    private static void loadSadilista() {

        ArrayList<Object[]> all = (ArrayList<Object[]>) JPA.getUtil().getEntityManager().createNativeQuery("select code, obl from temp_ekatte_sad").getResultList();
        for (Object[] obj : all) {
            sadilista.put(SearchUtils.asInteger(obj[0]), obj);
        }

    }


}
