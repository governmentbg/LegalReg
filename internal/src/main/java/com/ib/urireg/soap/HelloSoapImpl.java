/*
 * Copyright (c) 2025. Index - Bulgaria Ltd. All rights reserved.
 *
 */

package com.ib.urireg.soap;

import jakarta.jws.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Това е много прост пример за СОАП сървис.
 */
@WebService(
        endpointInterface = "com.ib.urireg.soap.HelloSoap",
        serviceName = "HelloSoap",
        portName = "HelloServicePort",
        targetNamespace = "http://mjs.com/soap")
public class HelloSoapImpl implements HelloSoap {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloSoapImpl.class);
    @Override
    public String echo(String input) {
        return "echo:"+input;
    }

    @Override
    public SimpleClassRespones test1(String input) {
        SimpleClassRespones sc=new SimpleClassRespones();
        sc.setFirstName("Ali"+"-"+input);
        sc.setLastName("Baba"+"-"+input);
        return sc;
    }

    @Override
    public List<SimpleClassRespones> test2(String input) {
        List<SimpleClassRespones> list=new ArrayList<SimpleClassRespones>();
        list.add(test1(input+"1"));
        list.add(test1(input+"2"));
        return list;
    }

    @Override
    public String testError(String input) throws ServiceFaultException {
        if (input==null || input.isEmpty()){
            ServiceFault fault = new ServiceFault(
                    "VALIDATION_ERROR",
                    "Input must not be blank",
                    "CID-" + System.currentTimeMillis()
            );
            LOGGER.error(fault.toString());
            throw new ServiceFaultException("Invalid request", fault);
        }
        return "Correct input:"+input;
    }
}
