package com.ib.urireg.migr;


import com.ib.system.ActiveUser;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.*;
import com.ib.urireg.db.dao.IzpitDAO;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.utils.MigrUtils;
import jakarta.persistence.Query;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class TestBefore2011 {


    public static void main(String[] args){
        try {
            String fileName = "D:\\_mp\\Миграция на УП преди 2011_В.xlsx";
            File xlsFile = new File(fileName);
            byte[] bytes = FileUtils.getBytesFromFile(xlsFile);
            String nomPar = "234";

            String warnings = new IzpitDAO(ActiveUser.DEFAULT).proccessFileBefore2011(xlsFile.getName(), bytes, nomPar, true);

            System.out.println("--------------------------------------");
            System.out.println(warnings);
            System.out.println("--------------------------------------");


        } catch (IOException e) {
            e.printStackTrace();
        }

    }




}
