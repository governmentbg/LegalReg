package com.ib.urireg.quartz;

import com.ib.urireg.system.SystemData;
import junit.framework.TestCase;
import org.junit.Test;
import org.quartz.JobExecutionException;

public class IndexFileContentJobTest extends TestCase {

    @Test
    public void test() {
        try {
            new IndexFileContentJob().proccessFiles(new SystemData());
        } catch (JobExecutionException e) {
            e.printStackTrace();
        }
    }

}
