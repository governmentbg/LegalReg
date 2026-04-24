package com.ib.urireg.udostDocs;

import com.aspose.words.Bookmark;
import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.Font;
import com.aspose.words.Underline;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.urireg.db.dao.DocDAO;
import com.ib.urireg.db.dao.IzpitDAO;
import com.ib.urireg.db.dao.ReferentDAO;
import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.db.dto.Izpit;
import com.ib.urireg.db.dto.Referent;
import com.ib.urireg.db.dto.ShablonBookmark;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.ib.system.utils.ValidationUtils.isNotBlank;

/**
 * @author n.kanev
 */
public class CustomFillMethods {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomFillMethods.class);

    public static final String SIGN_METHOD_NAME = "buildSignField";
    private static final double SIGN_FIELD_WIDTH = 60;

    private final UserData userData;
    private final SystemData systemData;
    private final Date date;
    Map<String, Object> additionalData;

    public CustomFillMethods(UserData userData, SystemData systemData, Map<String, Object> additionalData) {
        this.userData = userData;
        this.systemData = systemData;
        this.date = new Date();
        this.additionalData = additionalData;
    }

    public void buildLicaZapStaj(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }
        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildLicaZapStaj", UdostDocumentCreator.KEY_DOC_ID);
        }

        try {
            String sql =
                    "select staj.osn_institution, staj.lice_id, staj.mentor, staj.mentor_email,"
                    + " lice.names, lice.egn, lice.lnc, lice.universitet, lice.addr_country,"
                    + " lice.addr_oblast, lice.addr_obstina, lice.addr_ekatte, lice.addr_text, staj.pro_location"
                    + " from staj"
                    + " inner join lice on lice.lice_id = staj.lice_id"
                    + " where staj.osn_institution is not null"
                    + " and staj.zap_staj_id = :docId";


            /*
             * [00] staj.osn_institution    // Long
             * [01] staj.lice_id            // Long
             * [02] staj.mentor             // String
             * [03] staj.mentor_email       // String
             * [04] lice.names              // String
             * [05] lice.egn                // String
             * [06] lice.lnc                // String
             * [07] lice.universitet        // Long
             * [08] lice.addr_country       // Long
             * [09] lice.addr_oblast        // String
             * [10] lice.addr_obstina       // String
             * [11] lice.addr_ekatte        // Long
             * [12] lice.addr_text          // String
             * [13] staj.pro_location       // String
             */
            List<Object[]> data = JPA.getUtil().getEntityManager().createNativeQuery(sql)
                    .setParameter("docId", docId)
                    .getResultList();

            DocumentBuilder builder = new DocumentBuilder(document);
            builder.moveToBookmark(bookmark.getLabel());

            Map<String, List<Object[]>> map =
                    data.stream()
                            .collect(Collectors.groupingBy(
                                    //o -> ((Long) o[0]).intValue(),
                                    o -> {
                                        try {
                                            return systemData.decodeItem(
                                                    UriregConstants.CODE_CLASSIF_INSTITUTION,
                                                    ((Long) o[0]).intValue(),
                                                    userData.getCurrentLang(),
                                                    new Date());
                                        }
                                        catch(DbErrorException e) {
                                            LOGGER.error("Грешка при попълване на УД id=" + docId, e);
                                        }
                                        return "";
                                    },
                                    TreeMap::new,
                                    Collectors.toList()));

            int licaCount = 0;

            // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
            for(Map.Entry<String, List<Object[]>> institution : map.entrySet()) {
                AsposeUtils.resetParagraphFontFormatting(builder);
                Font font = builder.getFont();

                font.setBold(true);
                font.setUnderline(Underline.SINGLE);
                builder.writeln(institution.getKey());
                builder.writeln();

                List<Object[]> institutionLica = institution.getValue();
                institutionLica.sort(Comparator.comparing(c -> (String) c[4]));
                for(Object[] liceDanni : institutionLica) {
                    font.setUnderline(Underline.NONE);
                    font.setBold(true);

                    // Номер
                    builder.write(++licaCount + ". ");

                    // Име
                    builder.write(liceDanni[4] + ", ");

                    // ЕГН / ЛНЧ
                    if(isNotBlank((String) liceDanni[5])) {
                        builder.writeln("ЕГН " + liceDanni[5]);
                    }
                    else {
                        builder.writeln("ЛНЧ " + liceDanni[6]);
                    }

                    // Университет
                    builder.write("Завършил: ");
                    font.setBold(false);
                    if(liceDanni[7] != null) {
                        builder.write(
                                systemData.decodeItem(
                                        UriregConstants.CODE_CLASSIF_UNIVERSITETI,
                                        ((Long) liceDanni[7]).intValue(),
                                        userData.getCurrentLang(),
                                        new Date())
                        );
                    }
                    builder.writeln();

                    // институция
                    font.setBold(true);
                    builder.write("за стажант-юрист в: ");
                    font.setBold(false);
                    builder.writeln(institution.getKey());

                    font.setBold(true);
                    builder.write("за срок от ");
                    if(isNotBlank((String) liceDanni[2])) {
                        builder.write("два");
                    }
                    else {
                        builder.write("шест");
                    }
                    builder.writeln(" месеца, считано от датата на постъпването.");

                    // Адрес
                    builder.writeln("Постоянен адрес:");
                    font.setBold(false);
                    if(liceDanni[8] != null && !liceDanni[8].toString().equals(this.systemData.getSettingsValue("delo.countryBG"))) { // не е България
                        builder.write(systemData.decodeItem(
                                UriregConstants.CODE_CLASSIF_COUNTRIES,
                                ((Long) liceDanni[8]).intValue(),
                                userData.getCurrentLang(),
                                new Date()));
                        builder.write(", ");
                        if(isNotBlank((String) liceDanni[12])) {
                            builder.write((String) liceDanni[12]);
                        }

                    }
                    else { // България
                        if(liceDanni[11] != null) {
                            builder.write(systemData.decodeItemDopInfo(
                                    UriregConstants.CODE_CLASSIF_EKATTE,
                                    ((Long) liceDanni[11]).intValue(),
                                    userData.getCurrentLang(),
                                    new Date())
                            );
                            builder.write(", ");
                            builder.write(systemData.decodeItem(
                                    UriregConstants.CODE_CLASSIF_EKATTE,
                                    ((Long) liceDanni[11]).intValue(),
                                    userData.getCurrentLang(),
                                    new Date())
                            );
                            builder.write(", ");
                        }
                        if(liceDanni[12] != null && isNotBlank((String) liceDanni[12])) { // адрес текст
                            builder.write((String) liceDanni[12]);
                        }
                    }
                    builder.writeln();

                    // Ментор
                    if(liceDanni[2] == null || !isNotBlank((String) liceDanni[2])) { // няма ментор
                        font.setBold(true);
                        builder.write("Професионалният стаж на стажант-юриста за срок от четири месеца");
                        font.setBold(false);
                        builder.write(
                                ", считано от датата на постъпването, да се проведе при наставник, " +
                                    "отговарящ на изискванията на чл. 297, ал. 5 ЗСВ в орган на " +
                                    "съдебната власт по чл. 7, ал. 2 от Наредба № 1 от 1 февруари 2019 г. " +
                                    "за придобиване на юридическа правоспособност по ред, определен от председателя на ");
                        builder.writeln(institution.getKey());
                    }
                    else { // има ментор
                        font.setBold(true);
                        builder.write("на професионален стаж за стажант-юрист в ");
                        font.setBold(false);
                        if(liceDanni[13] != null && isNotBlank((String) liceDanni[13])) { // място на стажа
                            builder.writeln((String) liceDanni[13]);
                        }
                        font.setBold(true);
                        builder.writeln("за срок от четири месеца, считано от датата на постъпването.");
                        builder.write("при наставник: ");
                        font.setBold(false);
                        if(liceDanni[2] != null && isNotBlank((String) liceDanni[2])) { // наставник
                            builder.write(liceDanni[2] + ", ");
                        }
                        if(liceDanni[3] != null && isNotBlank((String) liceDanni[3])) { // имейл
                            builder.write((String) liceDanni[3]);
                        }
                        builder.writeln();
                    }

                    // * * * * * * *
                    builder.writeln();
                }

                builder.writeln();
            }
        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на УД id=" + docId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }

    }

    public void buildLicePrepisStaj(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
        final Integer liceId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID);

        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildLicePrepisStajBezNast", UdostDocumentCreator.KEY_DOC_ID);
        }
        if(liceId == null) {
            throw new UdostDocExceptions.MissingDataException("buildLicePrepisStajBezNast", UdostDocumentCreator.KEY_LICE_ID);
        }

        try {
            String sql =
                    "select lice.names, lice.egn, lice.lnc, lice.universitet, staj.osn_institution,"
                        + " staj.mentor, staj.mentor_email, lice.addr_country, lice.addr_text,"
                        + " lice.addr_ekatte, staj.pro_location"
                        + " from staj"
                        + " inner join lice on lice.lice_id = staj.lice_id"
                        + " where staj.osn_institution is not null"
                        + " and lice.lice_id = :liceId"
                        + " and staj.zap_staj_id = :docId";

            Object[] data = (Object[]) JPA.getUtil().getEntityManager().createNativeQuery(sql)
                    .setParameter("docId", docId)
                    .setParameter("liceId", liceId)
                    .getSingleResult();

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            String liceIme = (String) data[0];
            String liceEgn = (String) data[1];
            String liceLnc = (String) data[2];
            String liceUni = (data[3] != null)
                    ? systemData.decodeItem(
                        UriregConstants.CODE_CLASSIF_UNIVERSITETI,
                        ((Long) data[3]).intValue(),
                        userData.getCurrentLang(),
                        new Date())
                    : null;
            String institucia = (data[4] != null)
                    ? systemData.decodeItem(
                        UriregConstants.CODE_CLASSIF_INSTITUTION,
                        ((Long) data[4]).intValue(),
                        userData.getCurrentLang(),
                        new Date())
                    : null;
            String mentor = (String) data[5];
            String mentorEmail = (String) data[6];
            String liceAdres = (String) data[8];
            String stajLocation = (String) data[10];
            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

            DocumentBuilder builder = new DocumentBuilder(document);
            builder.moveToBookmark(bookmark.getLabel());
            AsposeUtils.resetParagraphFontFormatting(builder);
            Font font = builder.getFont();

            // Име
            font.setBold(true);
            builder.write(liceIme + ", ");

            // ЕГН / ЛНЧ
            if(isNotBlank(liceEgn)) {
                builder.writeln("ЕГН " + liceEgn);
            }
            else {
                builder.writeln("ЛНЧ " + liceLnc);
            }

            // Университет
            builder.write("Завършил: ");
            font.setBold(false);
            if(liceUni != null) {
                builder.write(liceUni);
            }
            builder.writeln();

            // институция
            font.setBold(true);
            builder.write("за стажант-юрист в: ");
            font.setBold(false);
            if(isNotBlank(institucia)) {
                builder.writeln(institucia);
            }

            font.setBold(true);
            if(!isNotBlank(mentor)) { // няма ментор
                builder.writeln("за срок от шест месеца, считано от датата на постъпването.");
            }
            else { // има ментор
                builder.writeln("за срок от два месеца, считано от датата на постъпването.");
            }

            // Адрес
            builder.writeln("Постоянен адрес:");
            font.setBold(false);
            if(data[7] != null && !data[7].toString().equals(this.systemData.getSettingsValue("delo.countryBG"))) { // не е България
                builder.write(systemData.decodeItem(
                        UriregConstants.CODE_CLASSIF_COUNTRIES,
                        ((Long) data[7]).intValue(),
                        userData.getCurrentLang(),
                        new Date()));
                builder.write(", ");
                if(isNotBlank(liceAdres)) {
                    builder.write(liceAdres);
                }

            }
            else { // България
                if(data[9] != null) {
                    builder.write(systemData.decodeItemDopInfo(
                            UriregConstants.CODE_CLASSIF_EKATTE,
                            ((Long) data[9]).intValue(),
                            userData.getCurrentLang(),
                            new Date())
                    );
                    builder.write(", ");
                    builder.write(systemData.decodeItem(
                            UriregConstants.CODE_CLASSIF_EKATTE,
                            ((Long) data[9]).intValue(),
                            userData.getCurrentLang(),
                            new Date())
                    );
                    builder.write(", ");
                }
                if(isNotBlank(liceAdres)) { // адрес текст
                    builder.write(liceAdres);
                }
            }
            builder.writeln();

            // Ментор
            if(!isNotBlank(mentor)) { // няма ментор
                font.setBold(true);
                builder.write("Професионалният стаж на стажант-юриста за срок от четири месеца");
                font.setBold(false);
                builder.write(
                        ", считано от датата на постъпването, да се проведе при наставник, " +
                                "отговарящ на изискванията на чл. 297, ал. 5 ЗСВ в орган на " +
                                "съдебната власт по чл. 7, ал. 2 от Наредба № 1 от 1 февруари 2019 г. " +
                                "за придобиване на юридическа правоспособност по ред, определен от председателя на ");
                if(isNotBlank(institucia)) {
                    builder.writeln(institucia);
                }
            }
            else { // има ментор
                font.setBold(true);
                builder.write("на професионален стаж за стажант-юрист в ");
                font.setBold(false);
                if(isNotBlank(stajLocation)) { // място на стажа
                    builder.writeln(stajLocation);
                }
                font.setBold(true);
                builder.writeln("за срок от четири месеца, считано от датата на постъпването.");
                builder.write("при наставник: ");
                font.setBold(false);
                if(isNotBlank(mentor)) { // наставник
                    builder.write(mentor + ", ");
                }
                if(isNotBlank(mentorEmail)) { // имейл
                    builder.write(mentorEmail);
                }
                builder.writeln();
            }

            // Край
            font.setBold(true);
            builder.write("Препис от заповедта да се изпрати на : ");
            font.setBold(false);
            if(isNotBlank(institucia)) {
                builder.write(institucia);
            }
            if(isNotBlank(mentor)) {
                builder.write(" и на ");
                builder.write(mentor);
            }
            font.setBold(true);
            builder.writeln(" за сведение и изпълнение.");
        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на препис doc_id=" + docId + ", lice_id=" + liceId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void buildLicaZapDopStaj(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildLicaZapDopStaj", UdostDocumentCreator.KEY_DOC_ID);
        }

        try {
            String sql =
                    "select lice.names, lice.egn, lice.lnc, lice.universitet, staj.osn_institution,"
                            + " staj.mentor, staj.mentor_email, lice.addr_country, lice.addr_text,"
                            + " lice.addr_ekatte, staj.pro_location"
                            + " from staj"
                            + " inner join lice on lice.lice_id = staj.lice_id"
                            + " where staj.osn_institution is not null"
                            + " and staj.zap_staj_id = :docId"
                            + " order by lice.names asc";

            List<Object[]> data = JPA.getUtil().getEntityManager().createNativeQuery(sql)
                    .setParameter("docId", docId)
                    .getResultList();

            DocumentBuilder builder = new DocumentBuilder(document);
            builder.moveToBookmark(bookmark.getLabel());
            AsposeUtils.resetParagraphFontFormatting(builder);
            Font font = builder.getFont();

            for(int i = 0; i < data.size(); i++) {
                Object[] lice = data.get(i);
                String liceIme = (String) lice[0];
                String liceEgn = (String) lice[1];
                String liceLnc = (String) lice[2];
                String liceUni = systemData.decodeItem(
                        UriregConstants.CODE_CLASSIF_UNIVERSITETI,
                        ((Long) lice[3]).intValue(),
                        userData.getCurrentLang(),
                        new Date());
                String institucia = systemData.decodeItem(
                        UriregConstants.CODE_CLASSIF_INSTITUTION,
                        ((Long) lice[4]).intValue(),
                        userData.getCurrentLang(),
                        new Date());
                String liceAdres = (String) lice[8];


                // Име
                font.setBold(true);
                builder.write((i + 1) + ". ");
                builder.write(liceIme + ", ");

                // ЕГН / ЛНЧ
                if(isNotBlank(liceEgn)) {
                    builder.writeln("ЕГН " + liceEgn);
                }
                else {
                    builder.writeln("ЛНЧ " + liceLnc);
                }

                // Университет
                builder.write("Завършил: ");
                font.setBold(false);
                if(liceUni != null) {
                    builder.write(liceUni);
                }
                builder.writeln();

                // институция
                font.setBold(true);
                builder.write("за стажант-юрист в: ");
                font.setBold(false);
                if(isNotBlank(institucia)) {
                    builder.write(institucia);
                }

                font.setBold(true);
                builder.writeln(", считано от датата на постъпването.");

                // Адрес
                builder.writeln("Постоянен адрес:");
                font.setBold(false);
                if(lice[7] != null && !lice[7].toString().equals(this.systemData.getSettingsValue("delo.countryBG"))) { // не е България
                    builder.write(systemData.decodeItem(
                            UriregConstants.CODE_CLASSIF_COUNTRIES,
                            ((Long) lice[7]).intValue(),
                            userData.getCurrentLang(),
                            new Date()));
                    builder.write(", ");
                    if(isNotBlank(liceAdres)) {
                        builder.write(liceAdres);
                    }

                }
                else { // България
                    if(lice[9] != null) {
                        builder.write(systemData.decodeItemDopInfo(
                                UriregConstants.CODE_CLASSIF_EKATTE,
                                ((Long) lice[9]).intValue(),
                                userData.getCurrentLang(),
                                new Date())
                        );
                        builder.write(", ");
                        builder.write(systemData.decodeItem(
                                UriregConstants.CODE_CLASSIF_EKATTE,
                                ((Long) lice[9]).intValue(),
                                userData.getCurrentLang(),
                                new Date())
                        );
                        builder.write(", ");
                    }
                    if(isNotBlank(liceAdres)) { // адрес текст
                        builder.write(liceAdres);
                    }
                }
                builder.writeln();

                if(i != data.size() - 1) {
                    builder.writeln();
                }
            }


        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на препис doc_id=" + docId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void buildLicePrepisDopStaj(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
        final Integer liceId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_LICE_ID);

        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildLicePrepisStajBezNast", UdostDocumentCreator.KEY_DOC_ID);
        }
        if(liceId == null) {
            throw new UdostDocExceptions.MissingDataException("buildLicePrepisStajBezNast", UdostDocumentCreator.KEY_LICE_ID);
        }

        try {
            String sql =
                    "select lice.names, lice.egn, lice.lnc, lice.universitet, staj.osn_institution,"
                            + " staj.mentor, staj.mentor_email, lice.addr_country, lice.addr_text,"
                            + " lice.addr_ekatte, staj.pro_location"
                            + " from staj"
                            + " inner join lice on lice.lice_id = staj.lice_id"
                            + " where staj.osn_institution is not null"
                            + " and lice.lice_id = :liceId"
                            + " and staj.zap_staj_id = :docId";

            Object[] data = (Object[]) JPA.getUtil().getEntityManager().createNativeQuery(sql)
                    .setParameter("docId", docId)
                    .setParameter("liceId", liceId)
                    .getSingleResult();

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            String liceIme = (String) data[0];
            String liceEgn = (String) data[1];
            String liceLnc = (String) data[2];
            String liceUni = systemData.decodeItem(
                    UriregConstants.CODE_CLASSIF_UNIVERSITETI,
                    ((Long) data[3]).intValue(),
                    userData.getCurrentLang(),
                    new Date());
            String institucia = systemData.decodeItem(
                    UriregConstants.CODE_CLASSIF_INSTITUTION,
                    ((Long) data[4]).intValue(),
                    userData.getCurrentLang(),
                    new Date());
            String mentor = (String) data[5]; // TODO da se mahnat izli6ni select-i
            String mentorEmail = (String) data[6];
            String liceAdres = (String) data[8];
            String stajLocation = (String) data[10];
            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

            DocumentBuilder builder = new DocumentBuilder(document);
            builder.moveToBookmark(bookmark.getLabel());
            AsposeUtils.resetParagraphFontFormatting(builder);
            Font font = builder.getFont();

            // Име
            font.setBold(true);
            builder.write(liceIme + ", ");

            // ЕГН / ЛНЧ
            if(isNotBlank(liceEgn)) {
                builder.writeln("ЕГН " + liceEgn);
            }
            else {
                builder.writeln("ЛНЧ " + liceLnc);
            }

            // Университет
            builder.write("Завършил: ");
            font.setBold(false);
            if(liceUni != null) {
                builder.write(liceUni);
            }
            builder.writeln();

            // институция
            font.setBold(true);
            builder.write("за стажант-юрист в: ");
            font.setBold(false);
            if(isNotBlank(institucia)) {
                builder.write(institucia);
            }

            font.setBold(true);
            builder.writeln(", считано от датата на постъпването.");

            // Адрес
            font.setBold(true);
            builder.writeln("Постоянен адрес:");
            font.setBold(false);
            if(data[7] != null && !data[7].toString().equals(this.systemData.getSettingsValue("delo.countryBG"))) { // не е България
                builder.write(systemData.decodeItem(
                        UriregConstants.CODE_CLASSIF_COUNTRIES,
                        ((Long) data[7]).intValue(),
                        userData.getCurrentLang(),
                        new Date()));
                builder.write(", ");
                if(isNotBlank(liceAdres)) {
                    builder.write(liceAdres);
                }

            }
            else { // България
                if(data[9] != null) {
                    builder.write(systemData.decodeItemDopInfo(
                            UriregConstants.CODE_CLASSIF_EKATTE,
                            ((Long) data[9]).intValue(),
                            userData.getCurrentLang(),
                            new Date())
                    );
                    builder.write(", ");
                    builder.write(systemData.decodeItem(
                            UriregConstants.CODE_CLASSIF_EKATTE,
                            ((Long) data[9]).intValue(),
                            userData.getCurrentLang(),
                            new Date())
                    );
                    builder.write(", ");
                }
                if(isNotBlank(liceAdres)) { // адрес текст
                    builder.write(liceAdres);
                }
            }
            builder.writeln();

            // Край
            font.setBold(true);
            builder.write("Препис-извлечение от заповедта да се връчи на: ");
            font.setBold(false);
            if(isNotBlank(liceIme)) {
                builder.write(liceIme);
            }
            if(isNotBlank(institucia)) {
                builder.write(" и на ");
                builder.write(institucia);
            }
            font.setBold(true);
            builder.writeln(" - за сведение и изпълнение.");
        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на препис doc_id=" + docId + ", lice_id=" + liceId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void buildIzpitDanni(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildIzpitDanni", UdostDocumentCreator.KEY_DOC_ID);
        }

        try {
            Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
            Izpit izpit = new IzpitDAO(this.userData).findByZapIzpId(zapId);
            if(izpit != null) {
                DocumentBuilder builder = new DocumentBuilder(document);
                builder.moveToBookmark(bookmark.getLabel());

                AsposeUtils.resetParagraphFontFormatting(builder);
                Font font = builder.getFont();

                font.setBold(true);
                SimpleDateFormat dateFormat_ddMMyyyy = new SimpleDateFormat("dd.MM.yyyy");
                SimpleDateFormat dateFormat_HHmm = new SimpleDateFormat("HH:mm");
                builder.write(dateFormat_ddMMyyyy.format(izpit.getTestDate()));
                builder.write(" г. от ");
                builder.write(dateFormat_HHmm.format(izpit.getTestDate()));
                builder.write(" ч.");
                if(isNotBlank(izpit.getTestLocation())) {
                    if(izpit.getTestLocation().toLowerCase().charAt(0) == 'в' || izpit.getTestLocation().toLowerCase().charAt(0) == 'ф') {
                        builder.write(" във ");
                    }
                    else builder.write(" в ");
                    builder.write(izpit.getTestLocation());
                }
            }
        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на заповед за изпит doc_id=" + docId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void buildKazusDanni(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildIzpitDanni", UdostDocumentCreator.KEY_DOC_ID);
        }

        try {
            Integer zapId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
            Izpit izpit = new IzpitDAO(this.userData).findByZapIzpId(zapId);
            if(izpit != null) {
                DocumentBuilder builder = new DocumentBuilder(document);
                builder.moveToBookmark(bookmark.getLabel());

                AsposeUtils.resetParagraphFontFormatting(builder);
                Font font = builder.getFont();

                font.setBold(true);
                SimpleDateFormat dateFormat_ddMMyyyy = new SimpleDateFormat("dd.MM.yyyy");
                SimpleDateFormat dateFormat_HHmm = new SimpleDateFormat("HH:mm");
                builder.write(dateFormat_ddMMyyyy.format(izpit.getCaseDate()));
                builder.write(" г. от ");
                builder.write(dateFormat_HHmm.format(izpit.getCaseDate()));
                builder.write(" ч.");
                if(isNotBlank(izpit.getCaseLocation())) {
                    if(izpit.getCaseLocation().toLowerCase().charAt(0) == 'в' || izpit.getCaseLocation().toLowerCase().charAt(0) == 'ф') {
                        builder.write(" във ");
                    }
                    else builder.write(" в ");
                    builder.write(izpit.getCaseLocation());
                }
            }
        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на заповед за изпит doc_id=" + docId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void buildSignCurrentUser(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        try {
            DocumentBuilder builder = new DocumentBuilder(document);
            builder.moveToBookmark(bookmark.getLabel());
            Font font = builder.getFont();
            font.setBold(true);

            ReferentDAO referentDAO = new ReferentDAO(this.userData);
            Referent referent = referentDAO.findByCode(this.userData.getUserId(), new Date(), false);
            AsposeUtils.buildSignField(builder, this.systemData, referent, SIGN_FIELD_WIDTH, this.userData.getCurrentLang());
        }
        catch(Exception e) {
            final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
            LOGGER.error("Грешка при попълване на документ с doc_id=" + docId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void buildSignPodpisal(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildSignPodpisal", UdostDocumentCreator.KEY_DOC_ID);
        }

        try {
            DocDAO dao = new DocDAO(this.userData);
            Doc dokument = dao.findById(docId);

            DocumentBuilder builder = new DocumentBuilder(document);
            builder.moveToBookmark(bookmark.getLabel());
            Font font = builder.getFont();
            font.setBold(true);

            if (dokument.getCodePodpisal() != null) {
                ReferentDAO referentDAO = new ReferentDAO(this.userData);
                Referent referent = referentDAO.findByCode(dokument.getCodePodpisal(), new Date(), false);
                AsposeUtils.buildSignField(builder, this.systemData, referent, SIGN_FIELD_WIDTH, this.userData.getCurrentLang());
            }
            else { // по подразбиране - едно празно поле с Министър
                ReferentDAO referentDAO = new ReferentDAO(this.userData);
                List<Object[]> lica = referentDAO.selectRefDataByPosition(UriregConstants.CODE_ZNACHENIE_DLAJN_MINISTAR);
                if(!lica.isEmpty()) {
                    Object[] ministar = lica.get(0);
                    String position = systemData.decodeItem(UriregConstants.CODE_CLASSIF_POSITION, UriregConstants.CODE_ZNACHENIE_DLAJN_MINISTAR, this.userData.getCurrentLang(), new Date());
                    AsposeUtils.buildSignField(builder, SIGN_FIELD_WIDTH, (String) ministar[1], position);
                }
            }
        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на документ с doc_id=" + docId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void buildSignIzgotvil(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildSignIzgotvil", UdostDocumentCreator.KEY_DOC_ID);
        }

        try {
            DocDAO dao = new DocDAO(this.userData);
            Doc dokument = dao.findById(docId);

            DocumentBuilder builder = new DocumentBuilder(document);
            builder.moveToBookmark(bookmark.getLabel());
            Font font = builder.getFont();
            font.setBold(true);

            if (dokument.getCodeIzgotvil() != null) {
                ReferentDAO referentDAO = new ReferentDAO(this.userData);
                Referent referent = referentDAO.findByCode(dokument.getCodeIzgotvil(), new Date(), false);
                AsposeUtils.buildSignField(builder, this.systemData, referent, SIGN_FIELD_WIDTH, this.userData.getCurrentLang());
            }
            else { // по подразбиране - едно празно поле със Старши експерт
                String position = systemData.decodeItem(UriregConstants.CODE_CLASSIF_POSITION, UriregConstants.CODE_ZNACHENIE_DLAJN_STAR_EXPERT, this.userData.getCurrentLang(), new Date());
                AsposeUtils.buildSignField(builder, SIGN_FIELD_WIDTH, "", position);
            }
        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на документ с doc_id=" + docId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void buildSignSaglasuvali(Document document, ShablonBookmark bookmark) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);

        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildSignSaglasuvali", UdostDocumentCreator.KEY_DOC_ID);
        }

        try {
            DocDAO dao = new DocDAO(this.userData);
            Doc dokument = dao.findById(docId);

            DocumentBuilder builder = new DocumentBuilder(document);
            builder.moveToBookmark(bookmark.getLabel());
            Font font = builder.getFont();
            font.setBold(true);

            if (dokument.getCodeSaglList() != null && !dokument.getCodeSaglList().isEmpty()) {

                ReferentDAO referentDAO = new ReferentDAO(this.userData);

                for (Integer code : dokument.getCodeSaglList()) {
                    Referent referent = referentDAO.findByCode(code, new Date(), false);
                    AsposeUtils.buildSignField(builder, this.systemData, referent, SIGN_FIELD_WIDTH, this.userData.getCurrentLang());
                    builder.writeln();
                }
            }
            else { // по подразбиране - две празни полета със зам.-министър и директор на дирекция
                String position = systemData.decodeItem(UriregConstants.CODE_CLASSIF_POSITION, UriregConstants.CODE_ZNACHENIE_DLAJN_ZAM_MINISTAR, this.userData.getCurrentLang(), new Date());
                AsposeUtils.buildSignField(builder, SIGN_FIELD_WIDTH, "", position);
                builder.writeln();
                position = systemData.decodeItem(UriregConstants.CODE_CLASSIF_POSITION, UriregConstants.CODE_ZNACHENIE_DLAJN_DIREKTOR_VSV, this.userData.getCurrentLang(), new Date());
                AsposeUtils.buildSignField(builder, SIGN_FIELD_WIDTH, "", position);
            }

        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на документ с doc_id=" + docId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }

    /**
     * Този метод е по-специален и не се показва на екрана, а се забива в кода,
     * когато се избере даден букмарк да е от вид {@link ShablonBookmark.FillStrategies#SIGN_FIELD}
     * @param document
     * @param bookmark
     * @param admStructCode
     */
    public void buildSignField(Document document, ShablonBookmark bookmark, String admStructCode) {
        Bookmark b = document.getRange().getBookmarks().get(bookmark.getLabel());
        if(b == null) {
            LOGGER.error("Документът не съдържа bookmark с името " + bookmark.getLabel());
            return;
        }

        final Integer docId = (Integer) this.additionalData.get(UdostDocumentCreator.KEY_DOC_ID);
        if(docId == null) {
            throw new UdostDocExceptions.MissingDataException("buildSignField", UdostDocumentCreator.KEY_DOC_ID);
        }

        if(!isNotBlank(admStructCode)) {
            throw new UdostDocExceptions.MissingDataException("buildSignField", "admStructCode");
        }

        try {
            DocumentBuilder builder = new DocumentBuilder(document);
            builder.moveToBookmark(bookmark.getLabel());
            Font font = builder.getFont();
            font.setBold(true);

            int code = Integer.parseInt(admStructCode);

            ReferentDAO referentDAO = new ReferentDAO(this.userData);
            Referent referent = referentDAO.findByCode(code, new Date(), false);
            AsposeUtils.buildSignField(builder, this.systemData, referent, SIGN_FIELD_WIDTH, this.userData.getCurrentLang());
        }
        catch(NumberFormatException e) {
            throw new IllegalArgumentException("Подаден е грешен код на адм. структура '" + admStructCode + "'");
        }
        catch(Exception e) {
            LOGGER.error("Грешка при попълване на документ с doc_id=" + docId, e);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }
}
