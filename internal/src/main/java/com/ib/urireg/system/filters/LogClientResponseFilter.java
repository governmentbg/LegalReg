package com.ib.urireg.system.filters;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**  За да логваме REST Цлиент.
 * За да се ползва трябва:
 --------------------------------------------
 Client client = ClientBuilder.newClient();
 client.register(new LogClientResponseFilter());
 ------------------------------------------------

 * @author krasig
 *
 */
public class LogClientResponseFilter implements ClientResponseFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogClientResponseFilter.class.getName());

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
    	LOGGER.info("Rest response:------------------------------");
    	LOGGER.info("Date: " + responseContext.getDate() + "- Status: " + responseContext.getStatus());
    }
}
