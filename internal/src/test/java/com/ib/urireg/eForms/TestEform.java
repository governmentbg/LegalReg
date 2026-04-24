package com.ib.urireg.eForms;

import com.ib.system.utils.FileUtils;
import com.ib.urireg.eforms.utils.EFormUtils;
import com.ib.urireg.eforms.utils.EgovContainer;
import com.ib.urireg.system.SystemData;

import java.io.File;
import java.io.IOException;

public class TestEform {

    public static void main(String[] args) {

        try {
            File f = new File("d:\\_tests\\3025-56a4c77b-8d4c-4203-99d8-d5a206df7574-ZVLN.pdf");
            byte[] bytes = FileUtils.getBytesFromFile(f);

            EgovContainer con = new EFormUtils().parseEform(f.getName(), bytes, new SystemData());
            System.out.println("---------------------------------------------");
            System.out.println(con.getParsedInfo());
            System.out.println("---------------------------------------------");
            System.out.println(con.getEgnLnch());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
