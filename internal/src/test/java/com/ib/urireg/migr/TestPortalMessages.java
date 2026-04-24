package com.ib.urireg.migr;

import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.FileUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.urireg.db.dao.MessageDAO;
import com.ib.urireg.db.dto.Message;
import com.ib.urireg.db.dto.MessageLang;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TestPortalMessages {

    private static String dat = null;
    private static String subject = "";
    private static String body = "";
    private static ArrayList<Object[]> files = new ArrayList<Object[]>();
    private static MessageDAO mdao = new MessageDAO(ActiveUser.DEFAULT);
    private static FilesDAO fdao = new FilesDAO(ActiveUser.DEFAULT);
    private static SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

    public static void main(String[] args) {




        try {
            byte[] bytes = FileUtils.getBytesFromFile(new File("d:\\novini.html"));
            String pageHtml = new String(bytes);

            Document doc = Jsoup.parse(pageHtml);

            Elements paragraphs = doc.select("p");



            for (Element p : paragraphs) {
                String text = p.text();
                String html = p.html();

                if (text.contains("л. 296, т. 1 от Закона за съдебната власт. Моля в 7 дневен срок да отстраните нередовност")){
                    System.out.println("qqqq");
                }


                if (text.length() > 45000) {
                   continue;
                }
                if (text != null && text.contains("г.") && text.length() < 15) {
                    flushMessage();
                    dat = text;
                    continue;
                }

                if (text != null) {

                    text = text.trim();


                    Date tmpDat = null;
                    try {
                        tmpDat = sdf.parse(text);
                    } catch (ParseException e) {
                    }

                    if (tmpDat != null) {
                        flushMessage();
                        dat = text;
                        continue;
                    }

                }





                if (html.contains("GetBlob")) {
                    Document tag = Jsoup.parse(html);
                    Element a = tag.selectFirst("a");

                    if (a != null) {
                        String href = "https://mjs.bg" + a.attr("href").replace("\\\"", "");
                        String name = a.text();

                        //System.out.println("Link: " + href + ", Text:  " + name);
                        System.out.println(href);
                        files.add(downloadFileAsBytes(href, name));
                    }

                }else{

                    if (SearchUtils.isEmpty(subject) && SearchUtils.isEmpty(body) &&  p.html().startsWith("<strong>")){
                        subject = text;
                    }else {

//                        html = html.replace("\r\n", "</br>");
//                        html = html.replace("\r", "</br>");
                          html = html.replace("\\\"", "\"");

                        body += html + "<BR>";
                    }
                }



            }
            flushMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void flushMessage() {

        Date datBeg = null;
        if (dat == null) {
            System.out.println("************************* Грешкаааааааааааааааааааа");
            return;
        }else{
            dat = dat.toLowerCase().replace("г .", "").replace("г.", "").trim();
            try {
                datBeg =sdf.parse(dat);
            } catch (ParseException e) {
                e.printStackTrace();
                return;
            }
        }

        try {

            JPA.getUtil().begin();

            Message mess = new Message();
            mess.setStatus(2);
            mess.setMessageVid(4);

            mess.setMessageLangs(new ArrayList<MessageLang>());
            MessageLang lang = new MessageLang();
            lang.setMessageText(body);
            lang.setTitle(subject);
            mess.getMessageLangs().add(lang);
            mess.setDateFrom(datBeg);

            mdao.saveMessageWithLangs(mess);

            for (Object [] tek : files){
                Files f = new Files();
                f.setContent((byte[])tek[1]);
                f.setFilename((String)tek[0]);
                f.setFileInfo((String)tek[2]);
                if (f.getFilename().toUpperCase().endsWith(".PDF")){
                    f.setContentType("application/pdf");
                }else if (f.getFilename().toUpperCase().endsWith(".XLSX")){
                    f.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                }else{
                    f.setContentType("application/octet-stream");
                }

                fdao.saveFileObject(f,  mess.getId(), mess.getCodeMainObject());

            }




            JPA.getUtil().commit();
        } catch (DbErrorException e) {
            e.printStackTrace();
            JPA.getUtil().rollback();
        }finally {
            JPA.getUtil().closeConnection();
        }

        //System.out.println("*******************************************************************  flushing " + dat);
        //System.out.println(body.length());
        dat = "";
        body = "";
        subject = null;
        files.clear();


    }

    public static Object[] downloadFileAsBytes(String fileURL, String info) throws IOException {

        URL url = new URL(fileURL);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("GET");

        String disposition = http.getHeaderField("Content-Disposition");
        String fileName = null;

        if (disposition != null) {

            // 1. filename*=
            if (disposition.contains("filename*=")) {
                String encoded = disposition.split("filename\\*=")[1];

                if (encoded.contains("''")) {
                    encoded = encoded.substring(encoded.indexOf("''") + 2);
                }

                fileName = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            }

            // 2. filename=
            if (fileName == null && disposition.contains("filename=")) {
                fileName = disposition.split("filename=")[1]
                        .replace("\"", "")
                        .trim();
            }
        }

        // 3. Ако няма име → от URL
        if (fileName == null) {
            fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
        }

        // 4. Чистене на Windows забранени символи
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 5. Сваляне на байтовете
        byte[] bytes;
        try (InputStream in = http.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            bytes = baos.toByteArray();
        }

        return new Object[] { fileName, bytes, info };
    }

}
