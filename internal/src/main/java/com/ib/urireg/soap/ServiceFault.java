/*
 * Copyright (c) 2025. Index - Bulgaria Ltd. All rights reserved.
 *
 */

package com.ib.urireg.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Class  който ще зареждаме грешка , ще логваме и ще предаваме към client-а
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceFault", propOrder = {"code", "message", "correlationId"})
public class ServiceFault {
    private String code;
    private String message;
    private String correlationId;

    public ServiceFault() {
    }

    public ServiceFault(String code, String message, String correlationId) {
        this.code = code;
        this.message = message;
        this.correlationId = correlationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ServiceFault{");
        sb.append("code='").append(code).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", correlationId='").append(correlationId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
