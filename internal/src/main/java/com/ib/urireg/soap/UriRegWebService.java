/*
 * Copyright (c) 2025. Index - Bulgaria Ltd. All rights reserved.
 *
 */

package com.ib.urireg.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

import java.util.ArrayList;
import java.util.List;

@WebService( name = "UriRegWebService", targetNamespace = "http://mjs.com/soap")
public interface UriRegWebService {

    @WebMethod(operationName = "version", action = "urn:Version")
    public String version();


    @WebMethod()
    public PersonWS findUriPerson(@WebParam(name="egnLnch") String egnLnch) throws ServiceFaultException;
}
