package com.ib.urireg.system.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**  За да логваме REST Цлиент.
 * За да се ползва трябва:
 --------------------------------------------
  		Client client = ClientBuilder.newClient();
		client.register(new LogClientRequestFilter());
------------------------------------------------

 * @author krasig
 *
 */
public class LogClientRequestFilter implements ClientRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogClientRequestFilter.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		LOGGER.info("Rest client invoke:------------------------------");

		// log method and URL
		LOGGER.info("Method={} Url={}", requestContext.getMethod(), requestContext.getUri());

		// log JSON body only for POST (and optionally PUT)
		if (requestContext.getEntity() != null
				&& ("POST".equalsIgnoreCase(requestContext.getMethod())
					|| "PUT".equalsIgnoreCase(requestContext.getMethod()))) {

			MediaType mediaType = requestContext.getMediaType();
			boolean isJson = mediaType != null
					&& MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType);

			if (isJson) {
				try {
					String json = OBJECT_MAPPER.writeValueAsString(requestContext.getEntity());
					LOGGER.info("Request JSON body: {}", json);
				} catch (JsonProcessingException e) {
					// fallback to toString() if serialization fails
					LOGGER.warn("Failed to serialize request entity as JSON, falling back to toString()", e);
					LOGGER.info("Request body (toString): {}", requestContext.getEntity().toString());
				}
			} else {
				LOGGER.info("Request body (non-JSON): {}", requestContext.getEntity().toString());
			}
		}
	}
}
