/*
 * Copyright (c) 2025. Index - Bulgaria Ltd. All rights reserved.
 *
 */

package com.ib.urireg.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import java.util.List;

/**
 * Mnogo prost primer za soap syrwis . Wiv syotwetnia лас със същото име но суфиь ...Impl
 */
@WebService(
        name = "HelloSoap",
        targetNamespace = "http://mjs.com/soap")
public interface HelloSoap {
    @WebMethod
    String echo(String input);

    @WebMethod
    SimpleClassRespones test1(String input);

    @WebMethod
    List<SimpleClassRespones> test2(String input);

    /**Този метод че изхвърли грешка ако не е попълнен параметъра
     * @param input
     * @return
     */
    @WebMethod
    String testError(String input)  throws ServiceFaultException;

}
