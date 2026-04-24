package com.ib.urireg;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestLogger.class);

    public static void main(String[] args) {
        LOGGER.info("aaaaa");
        LOGGER.warn("bbb");
        LOGGER.error("cccc");
        LOGGER.debug("ddd");

    }

    @Test
    public void testAaaaa(){
        LOGGER.info("aaaaa");
        LOGGER.warn("bbb");
        LOGGER.error("cccc");
        LOGGER.debug("ddd");
        System.out.println("bbbb");
    }
}
