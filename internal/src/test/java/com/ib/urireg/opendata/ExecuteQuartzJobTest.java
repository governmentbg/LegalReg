package com.ib.urireg.opendata;

import com.ib.system.quartz.BaseJobResult;
import com.ib.urireg.quartz.Sent2OpenDataJob;
import com.ib.urireg.system.SystemData;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.impl.JobExecutionContextImpl;

public class ExecuteQuartzJobTest {

    public static void main(String[] args) throws Exception {

        try {
            BaseJobResult result = new Sent2OpenDataJob().proccessPublicRegister(new SystemData());
            System.out.println(result.getComment());

        } catch (JobExecutionException e) {
            e.printStackTrace();
        }

    }

}
