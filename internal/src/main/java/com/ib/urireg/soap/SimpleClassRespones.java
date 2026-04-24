/*
 * Copyright (c) 2025. Index - Bulgaria Ltd. All rights reserved.
 *
 */

package com.ib.urireg.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "SimpleClassRespones")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SimpleClassRespones", propOrder = {"firstName", "lastName"})
public class SimpleClassRespones {

    @XmlElement
    private String firstName;

    @XmlElement
    private String lastName;

    public SimpleClassRespones() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
