/*
 * Copyright (c) 2025. Index - Bulgaria Ltd. All rights reserved.
 *
 */

package com.ib.urireg.soap;


import jakarta.xml.ws.WebFault;

/**
 * Тук слагаме грешката, която ще ни е полезна
 */
@WebFault(name = "ServiceFault", targetNamespace = "http://mjs.com/soap/fault")
public class ServiceFaultException extends Exception {

    private final ServiceFault faultInfo;

    public ServiceFaultException(String message, ServiceFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public ServiceFaultException(String message, ServiceFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public ServiceFault getFaultInfo() {
        return faultInfo;
    }
}
