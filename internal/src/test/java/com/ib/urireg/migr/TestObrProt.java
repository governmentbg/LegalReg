package com.ib.urireg.migr;

import com.ib.system.utils.DateUtils;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;
import com.ib.urireg.utils.MigrUtils;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestObrProt {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestObrProt.class);

    public static void main(String[] args) {

        TreeSet<Date> lipsi;
        try {

            SystemData sd = new SystemData();

            lipsi = new TreeSet<Date>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            Date dat2020 = null;
            try {
                dat2020 = sdf.parse("01.01.2020");
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }


            MigrUtils.init(0, sd);

            ArrayList<Object[]> spis = (ArrayList<Object[]>) JPA.getUtil().getEntityManager().createNativeQuery("select distinct dat_prot, zap_date, zap_nom from temp_spis order by temp_spis.dat_prot").getResultList();
            ArrayList<Object> egns = (ArrayList<Object>) JPA.getUtil().getEntityManager().createNativeQuery("select distinct egn from advoreg.temp_prot order by temp_prot.egn").getResultList();
            HashMap<Date, Integer> protsMap = new HashMap<Date, Integer>();
            ArrayList<Object[]> ekkate = (ArrayList<Object[]>) JPA.getUtil().getEntityManager().createNativeQuery("select ekatte_att.ime, ekatte_att.ekatte, ekatte_att.oblast, ekatte_att.obstina, tvm from ekatte_att").getResultList();
            HashMap<String, Integer> allZap = new HashMap<String, Integer>();

            //key idProt+"|"+idZap value = idIzpit
            HashMap<String, Integer> izpitMap = new HashMap<String, Integer>();

            HashMap<Date, Integer> datProt2Izpit = new HashMap<Date, Integer>();
            HashMap<Date, Integer> datProt2Prot = new HashMap<Date, Integer>();
            HashMap<Date, Integer> datProt2Zapoved = new HashMap<Date, Integer>();



            String sqlDoc = "INSERT INTO doc (doc_id, doc_vid, zaiav_date, rn_doc, doc_date, doc_info, user_reg, date_reg) VALUES (?,?,?,?,?,?,?,?)";
            String sqlDocZapovedIzpit = "INSERT INTO doc (doc_id, doc_vid, zaiav_date, rn_doc, doc_date, doc_info, user_reg, date_reg, zaiav_date) VALUES (?,?,?,?,?,?,?,?,?)";
            String sqlDocProt = "INSERT INTO doc (doc_id, doc_vid, zaiav_date, rn_doc, doc_date, doc_info, user_reg, date_reg, predsedatel, members) VALUES (?,?,?,?,?,?,?,?,?,?)";



            try {

                JPA.getUtil().begin();

                JPA.getUtil().getEntityManager().createNativeQuery("delete from izpit_result where izpit_id > 6000000").executeUpdate();
                JPA.getUtil().getEntityManager().createNativeQuery("delete from izpit where izpit_id > 6000000").executeUpdate();
                JPA.getUtil().getEntityManager().createNativeQuery("delete from lice_doc where doc_id > 6000000").executeUpdate();
                JPA.getUtil().getEntityManager().createNativeQuery("delete from doc where doc_vid in(8,6,11) and doc_id > 6000000").executeUpdate();


                for (Object[] obj : spis) {
                    //Протокол
                    Integer seqDocProt = null;
                    Integer seqDocZap = null;
                    Integer seqIzpit = null;
                    Date datProt = null;
                    if (obj != null) {

                        datProt = SearchUtils.asDate(obj[0]);
                        datProt = doDatProtFix(datProt);
                        //System.out.println(date);
                        seqDocProt = protsMap.get(datProt);

                        ArrayList<Object[]> koms = (ArrayList<Object[]>) JPA.getUtil().getEntityManager().createNativeQuery("select temp_spis.preds, temp_spis.chlenove from advoreg.temp_spis where dat_prot = ?")
                                .setParameter(1, datProt).getResultList();

                        int cnt = 0;
                        String preds = "";
                        String chlen = "";
                        for (Object[] kom : koms) {
                            cnt++;
                            String nomKom = "";
                            if (koms.size() > 1){
                                nomKom = "Комисия " + cnt + ": ";
                            }

                            preds += nomKom + kom[0] + "\r\n";
                            chlen += nomKom + "\r\n" + kom[1] + "\r\n";
                        }




                        if (seqDocProt == null) {

                            seqDocProt = MigrUtils.getSeq("seq_doc");
                            Query query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDocProt);
                            query.setParameter(1, seqDocProt);
                            query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT);
                            query.setParameter(3, null);
                            query.setParameter(4, null);
                            query.setParameter(5, datProt);
                            query.setParameter(6, "Протокол за казус");
                            query.setParameter(7, -1);
                            query.setParameter(8, new Date());
                            query.setParameter(9, preds);
                            query.setParameter(10, chlen);

                            query.executeUpdate();
                            protsMap.put(datProt, seqDocProt);
                        }

                        datProt2Prot.put(datProt, seqDocProt);
                    }

                    Date dateZap = SearchUtils.asDate(obj[1]);
                    String rnZap = SearchUtils.asString(obj[2]);

                    if (dateZap == null) {
                        System.out.println("Внимание !!!! Дата на заповед " + rnZap + " не е въведене. Слагаме минимална");
                        dateZap = DateUtils.systemMinDate();
                    }


                    String hash = rnZap + "|" + MigrUtils.getDateHash(dateZap);

                    seqDocZap = allZap.get(hash);
                    if (seqDocZap == null) {

                        seqDocZap = MigrUtils.getSeq("seq_doc");
                        Query query = JPA.getUtil().getEntityManager().createNativeQuery(sqlDoc);
                        query.setParameter(1, seqDocZap);
                        if (dateZap != null && dateZap.getTime() < dat2020.getTime()) {
                            query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019);
                        } else {
                            query.setParameter(2, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT);
                        }
                        query.setParameter(3, null);
                        query.setParameter(4, rnZap);
                        query.setParameter(5, dateZap);
                        query.setParameter(6, "Заповед за тест");
                        query.setParameter(7, -1);
                        query.setParameter(8, new Date());


                        query.executeUpdate();
                        allZap.put(hash, seqDocZap);
                        datProt2Zapoved.put(datProt, seqDocZap);
                    }else{
                        datProt2Zapoved.put(datProt, seqDocZap);
                    }

                    String hashIzpit = seqDocProt + "|" + seqDocZap;

                    seqIzpit = izpitMap.get(hashIzpit);
                    if (seqIzpit == null) {
                        seqIzpit = MigrUtils.getSeq("seq_izpit");
                        Query query = JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO izpit (izpit_id, zap_izp_id, case_date, case_prot_id, user_reg, date_reg) VALUES (?,?,?,?,?,?)");
                        query.setParameter(1, seqIzpit);
                        query.setParameter(2, seqDocZap);
                        query.setParameter(3, datProt);
                        query.setParameter(4, seqDocProt);
                        query.setParameter(5, -1);
                        query.setParameter(6, new Date());
                        query.executeUpdate();
                    }

                    datProt2Izpit.put(datProt, seqIzpit);


                }

                JPA.getUtil().getEntityManager().createNativeQuery("update lice set broi_izpit = (select count(izpit_result.case_result) from advoreg.izpit_result where izpit_result.case_result = 2 and  izpit_result.lice_id = lice.lice_id )").executeUpdate();


                JPA.getUtil().commit();

            } catch (DbErrorException e) {
                LOGGER.error(e.getMessage());
                JPA.getUtil().rollback();
            } finally {
                JPA.getUtil().closeConnection();
            }


            try {

                JPA.getUtil().begin();


                for (Object tek : egns) {
                    String egn = SearchUtils.asString(tek);
                    if (egn == null) {
                        egn = egn.trim();
                    }
                    Integer liceId = null;
                    //System.out.println("Обработваме ЕГН" + egn);

                    if (liceId == null) {
                        try {
                            liceId = SearchUtils.asInteger(JPA.getUtil().getEntityManager().createNativeQuery("select lice_id from lice where egn = ? or lnc = ?").setParameter(1, egn).setParameter(2, egn).getSingleResult());
                        } catch (Exception e) {
                            System.out.println("ВНИМАНИЕ ГРЕШКА В ЛОГИКАТА !!!!  Не може да се намер лице с ЕГН/ЛНЧ: " + egn + " в БД !");
                            continue;
                        }
                    }
                    if (liceId == null) {
                        System.out.println("ВНИМАНИЕ ГРЕШКА В ЛОГИКАТА !!!!  Не може да се намер лице с ЕГН/ЛНЧ: " + egn + " в БД !");
                        continue;
                    }

                    //ТУК РАЗЧИТАМЕ НА ЕДИН СТАЖ .....
                    Integer stajId = null;
                    Integer zIzpitId = null;
                    try {
                        Object[] staj = (Object[]) JPA.getUtil().getEntityManager().createNativeQuery("select staj_id, zaiav_izp_id from staj where lice_id = ?").setParameter(1, liceId).getSingleResult();

                        stajId =  SearchUtils.asInteger(staj[0]);
                        zIzpitId  = SearchUtils.asInteger(staj[1]);

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Грешка при търсене на стаж за лице с ЕГН: " + egn);
                    }

                    if (stajId == null) {
                        System.out.println("Липсва стаж за лице с ЕГН: " + egn);
                    }


                    ArrayList<Object[]> result = (ArrayList<Object[]>) JPA.getUtil().getEntityManager().createNativeQuery("select distinct dat_prot, grad, adres, kraj_staj, izdarjal, zavarshil, sad from temp_prot where temp_prot.egn = ? order by dat_prot desc")
                            .setParameter(1, egn).getResultList();

                    String grad = null;
                    String adres = null;
                    Integer sad = null;
                    String zavarshil = null;
                    String izdarjal = null;

                    for (Object[] obj : result) {
                        if (obj[1] != null) {
                            grad = SearchUtils.asString(obj[1]);
                        }
                        if (obj[2] != null) {
                            adres = SearchUtils.asString(obj[2]);
                        }

                        if (obj[6] != null) {
                            sad = SearchUtils.asInteger(obj[6]);
                        }

                        if (obj[5] != null) {
                            zavarshil = SearchUtils.asString(obj[5]);
                        }

                        if (obj[4] != null) {
                            izdarjal = SearchUtils.asString(obj[4]);
                        }


                        Date datProt = SearchUtils.asDate(obj[0]);
                        datProt = doDatProtFix(datProt);
                        Integer idDoc = protsMap.get(datProt);
                        if (idDoc == null) {
                            System.out.println("ВНИМАНИЕ ГРЕШКА В ЛОГИКАТА !!!! Протокол на може да се намери !!!!" + datProt);
                            lipsi.add(datProt);
                            continue;
                        }

                        Integer izpitId = datProt2Izpit.get(datProt);

                        Integer izdCode = null;
                        if (izdarjal != null) {
                            if (izdarjal.equalsIgnoreCase("да")) {
                                izdCode = 1;
                            }else{
                                if (izdarjal.equalsIgnoreCase("не")) {
                                    izdCode = 2;
                                }else{
                                    if (izdarjal.equalsIgnoreCase("Неявил се")) {
                                        izdCode = 3;
                                    }
                                }
                            }
                        }


                        if (izpitId != null) {
                            Integer seqIzpitR = MigrUtils.getSeq("seq_izpit_result");
                            Query query = JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO izpit_result (result_id, izpit_id, staj_id, lice_id, case_result, test_result,user_reg, date_reg, zaiav_izp_id) VALUES (?,?,?,?,?,?,?,?,?)");
                            query.setParameter(1, seqIzpitR);
                            query.setParameter(2, izpitId);
                            query.setParameter(3, stajId);
                            query.setParameter(4, liceId);
                            query.setParameter(5, izdCode);
                            query.setParameter(6, izdCode);
                            query.setParameter(7, -1);
                            query.setParameter(8, new Date());
                            query.setParameter(9, zIzpitId);
                            query.executeUpdate();

                            zIzpitId = null;
                        }

                        Integer idZapoved = datProt2Zapoved.get(datProt);
                        if (idZapoved != null) {
                            MigrUtils.insertLiceDoc(liceId, idZapoved);
                        }else{
                            System.out.println("Заповед ид не може да се намери за протокол " + datProt + " ЕГН " + egn);
                        }

                        Integer idProt = datProt2Prot.get(datProt);
                        if (idProt != null) {
                            MigrUtils.insertLiceDoc(liceId, idProt);
                        }else{
                            System.out.println("Протокол ид не може да се намери за протокол " + datProt + " ЕГН " + egn);
                        }


                        //
                    }


                    TreeMap<String, Object> columns = new TreeMap<String, Object>();
                    TreeMap<String, Object> keys = new TreeMap<String, Object>();
                    String tableName = "lice";

                    keys.put("lice_id", liceId);


                    Integer nasMesto = null;
                    String obl = null;
                    String obsht = null;


                    if (!SearchUtils.isEmpty(grad)) {

                        ArrayList<Object[]> foundEkatte = MigrUtils.recognizeMesto(ekkate, grad, sad);
                        if (foundEkatte.size() != 1) {
                            if (!SearchUtils.isEmpty(adres)) {
                                adres = grad + ", " + adres;
                            } else {
                                adres = grad;
                            }
                        }else{
                            nasMesto = SearchUtils.asInteger(foundEkatte.get(0)[1]);
                            obl = SearchUtils.asString(foundEkatte.get(0)[2]);
                            obsht = SearchUtils.asString(foundEkatte.get(0)[3]);

                        }
                    }

                    if (!SearchUtils.isEmpty(zavarshil)) {
                        Integer uni = recognizeUni(zavarshil);
                        if (uni == null) {
                            System.out.println("Не може да бъде разпознат университет: " + zavarshil);
                        } else {
                            columns.put("universitet", uni);
                        }

                    }


                    if (obl != null) {
                        columns.put("addr_oblast", obl);
                    }

                    if (obsht != null) {
                        columns.put("addr_obstina", obsht);
                    }

                    if (nasMesto != null) {
                        columns.put("addr_ekatte", nasMesto);
                    }

                    if (adres != null) {
                        columns.put("addr_text", adres);
                    }


                    MigrUtils.updateTable(tableName, columns, keys);

                }
                JPA.getUtil().commit();
            } catch (DbErrorException e) {
                JPA.getUtil().rollback();
                throw new RuntimeException(e);
            } finally {
                JPA.getUtil().closeConnection();
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("----Несъответствие-------------------");
        for (Date dat : lipsi){
            System.out.println(new SimpleDateFormat("dd.MM.yyyy").format(dat));
        }

            System.out.println("--------- end ------------------------");
    }

    private static Integer recognizeUni(String zavarshil) {

        if (zavarshil == null) {
            return null;
        }

        zavarshil = zavarshil.toLowerCase();


        if (zavarshil.contains("черноризец") || zavarshil.contains("храбър") || zavarshil.contains("всу")){
            return 2; //ВСУ "Черноризец Храбър", гр. Варна
        }

        if (zavarshil.contains("русе") || zavarshil.contains("кънчев")){
            return 3; //РУ "Ангел Кънчев", гр. Русе
        }

        if (zavarshil.contains("търново") || zavarshil.contains("методий") || zavarshil.contains("вту")){
            return 4; //ВТУ "Св. Св. Кирил и Методий", гр. Велико Търново
        }

        if (zavarshil.contains("нов български") ){
            return 5; // Нов български университет, гр. София
        }

        if (zavarshil.contains("паисий") || zavarshil.contains("хилендарски") || zavarshil.contains("пловдив")){
            return 6; //ПУ "Паисий Хилендарски", гр. Пловдив
        }

        if (zavarshil.contains("унсс") || zavarshil.contains("стопан")){
            return 7; //УНСС, гр. София
        }

        if (zavarshil.contains("чужд") || zavarshil.contains("друго") ){
            return 8; //Чуждестранно учебно заведение
        }

        if (zavarshil.contains("мвр") ){
            return 12; //Академия на МВР
        }

        if (zavarshil.contains("бургас")){
            return 9; // Бургаски свободен университет, гр. Бургас
        }

        if (zavarshil.contains("благоевград") || zavarshil.contains("неофит") || zavarshil.contains("юзу")){
            return 10; //ЮЗУ "Неофит Рилски", гр. Благоевград
        }

        if (zavarshil.contains("технически") ){
            return 11; //Технически университет, гр. Варна
        }

        if (zavarshil.contains("софийски университет") || zavarshil.contains("охридски")){
            return 1; //Софийски университет "Св. Климент Охридски"
        }

        return null;
    }


    public static Date doDatProtFix(Date dat){
        if (dat == null){
            return null;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            String datS = sdf.format(dat);
            if (datS.equals("01.04.2022")){
                return sdf.parse("14.04.2022");
            }
            if (datS.equals("24.08.2025")){
                return sdf.parse("03.09.2025");
            }


            if (datS.equals("14.11.2018")){
                return sdf.parse("15.11.2018");
            }

            if (datS.equals("12.12.2018")){
                return sdf.parse("13.12.2018");
            }

            if (datS.equals("11.05.2012")){
                return sdf.parse("13.12.2018");
            }





        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return dat;


    }
}
