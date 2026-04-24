package com.ib.urireg.system.filters;

import com.ib.urireg.rest.common.Logged;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/*
Този филтър се използва за да се логват всички постъпващи РЕСТ RequestResponse.
За да се случи това е необходимо да интересния сървис да бъде анотиран с @Logged
!!!!! Важно !!!!!
Това е логване само на сървисите. Не на клиентите. За тях - @see LogClientRequestFilter, LogClientResponseFilter

   тук Еклипса може да се олаче с компилатион еррор, но то всъщност не е.
 * За да се оправи, трябва :
 * go to Window > Preferences > Jboss Tools > JAX-RS > JAX-RS Validator > JAX-RS Name Bindings and set Missing @Retention annotation to something other than "Error".*/
@Logged
@Provider
public class RestLoggingFilter implements ContainerRequestFilter,ContainerResponseFilter {
	@Context
	private ResourceInfo resourceInfo;
//	@Context
//    private HttpServletRequest servletRequest;
	private static final Logger LOGGER = LoggerFactory.getLogger(RestLoggingFilter.class);
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		LOGGER.debug("------------- REQUEST START---------");
		LOGGER.debug("		    start-time:{}", System.currentTimeMillis());
		LOGGER.debug("Entering in Resource :{} ", requestContext.getUriInfo().getPath());
		LOGGER.debug("		  Method Name :{} ", resourceInfo.getResourceMethod().getName());
		LOGGER.debug("				Class :{} ", resourceInfo.getResourceClass().getCanonicalName());
		LOGGER.debug("   		 MediaType:{}",requestContext.getMediaType());
		logQueryParameters(requestContext);
		logMethodAnnotations();
		logRequestHeader(requestContext);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(requestContext.getEntityStream(), baos);
        byte[] bytes = baos.toByteArray();
        LOGGER.debug("Posted: " + new String(bytes, StandardCharsets.UTF_8));
        requestContext.setEntityStream(new ByteArrayInputStream(bytes));

//        String entity = readEntityStream(requestContext);
//        if(null != entity && entity.trim().length() > 0) {
//            LOGGER.debug("	Entity Stream : {}",entity);
//        }


//		LOGGER.debug("  	  Headers:{}",request.getHeaders());
//		BufferedInputStream stream = new BufferedInputStream(request.getEntityStream());
//		String payload = IOUtils.toString(stream, "UTF-8");
//		LOGGER.debug("EntityStream:"+payload);
		LOGGER.debug("------------- REQUEST END---------");
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		// TODO Auto-generated method stub
		LOGGER.debug("----------- RESPONSE ------------");
		//LOGGER.debug("		Date:{}",responseContext.getDate());
		LOGGER.debug("  Status:{}", responseContext.getStatus());
		LOGGER.debug("  {}",responseContext.getEntity());

	}

	private void logQueryParameters(ContainerRequestContext requestContext) {
		LOGGER.debug("----Start Query Parameters of resource ----");
		Iterator iterator = requestContext.getUriInfo().getQueryParameters().keySet().iterator();
        while (iterator.hasNext()) {
            String name = (String) iterator.next();
            List obj = requestContext.getUriInfo().getQueryParameters().get(name);
            String value = null;
            if(null != obj && !obj.isEmpty()) {
                value = (String) obj.get(0);
            }
            LOGGER.debug("Query Parameter Name:{}, Value :{}", name, value);
        }
    }

	   private void logMethodAnnotations() {
	        Annotation[] annotations = resourceInfo.getResourceMethod().getDeclaredAnnotations();
	        if (annotations != null && annotations.length > 0) {
	            LOGGER.debug("----Start Annotations of resource ----");
	            for (Annotation annotation : annotations) {
	            	LOGGER.debug(annotation.toString());
	            }
	            LOGGER.debug("----End Annotations of resource----");
	        }
	    }

	   private void logRequestHeader(ContainerRequestContext requestContext) {
	        Iterator iterator;
	        LOGGER.debug("----Start Header Section of request ----");
	        LOGGER.debug("Method Type : {}", requestContext.getMethod());
	        iterator = requestContext.getHeaders().keySet().iterator();
	        while (iterator.hasNext()) {
	            String headerName = (String) iterator.next();
	            String headerValue = requestContext.getHeaderString(headerName);
	            LOGGER.debug("Header Name: {}, Header Value :{} ",headerName, headerValue);
	        }
	        LOGGER.debug("----End Header Section of request ----");
	    }

	    private String readEntityStream(ContainerRequestContext requestContext)
	    {
	    	String payload="";
			try {
				BufferedInputStream stream = new BufferedInputStream(requestContext.getEntityStream());
				payload = IOUtils.toString(stream, StandardCharsets.UTF_8);
//				requestContext.setEntityStream(IOUtils.toInputStream(payload,StandardCharsets.UTF_8));
				requestContext.setEntityStream(new ByteArrayInputStream("firstParam=111&secondParam=222".getBytes()));

			} catch (IOException e) {
				LOGGER.error("",e);
				payload="Error reading requestContext.getEntityStream()!!!!!";
			}
			return payload;
	    }


}
