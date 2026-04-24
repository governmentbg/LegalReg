package com.ib.urireg.db.dao;

import com.ib.system.ActiveUser;
import com.ib.system.utils.FileUtils;
import com.ib.urireg.system.SystemData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestLoadFileIzpit {

    public static  void main(String[] args) {


        try {

            IzpitDAO dao = new IzpitDAO(ActiveUser.DEFAULT);
            SystemData sd = new SystemData();
            
            byte[] bytes = FileUtils.getBytesFromFile(new File("d:\\_mp\\personsTestListXls-02112025 .xlsx"));

            ArrayList<Object[]> listResIzpitTest = (ArrayList<Object[]>) dao.findIzpitResults(29, 272);
            System.out.println(listResIzpitTest.size());

            String result = dao.uploadIzpitResults(bytes, listResIzpitTest, 1,  sd);
            System.out.println(result);

            System.out.println("----------------------------------");
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        
    }
}
