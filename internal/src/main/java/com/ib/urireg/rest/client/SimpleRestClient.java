package com.ib.urireg.rest.client;

import com.ib.urireg.system.filters.LogClientRequestFilter;
import com.ib.urireg.system.filters.LogClientResponseFilter;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;


/**
 * Възможно най-простия цлиент, който да се ползва.
 * Целта му е да предостави достъп до CLIENT , което е heavy операцията.
 * НЯМА ДА ИМА НИКАКВИ СПЕЦИАЛНИ МЕТОДИ!!!!
 * Освен getMycontext, който в момента е направен да вярва на всичко.
 * Ако има нужда, трябва да се екстендне класа и да се овърридне този метод
 * Целта е да дава клиент и после всеки да си прави таргет-а, да подава параметри и т.н.
 * Използване:<pre>
 * try {
 *	SimpleRestClient instance = SimpleRestClient.getInstance();
 *	WebTarget webTarget = instance.getClient().target(SRV_TARGET).path("/someservice");
 *	...
 *	}
 *	</pre>
 * Аман от сложни универсални дивотии.
 * @author krasig
 *
 */
public class SimpleRestClient {
	static final Logger LOGGER = LoggerFactory.getLogger(SimpleRestClient.class);
	private static ResteasyClient client;
	private static SimpleRestClient instance;

	public static SimpleRestClient getInstance() {
		LOGGER.debug("getInstance");
		if (instance == null) {
			instance = new SimpleRestClient();
		}
		return instance;
	}

	public SimpleRestClient() {
		super();
		//String SRV_TARGET="http://localhost:8080/RestTests/rest/sample";
		try {

			int timeout = 15;


//			RequestConfig config = RequestConfig.custom()
//					.setConnectionRequestTimeout(timeout * 1000)
//					.setConnectTimeout(timeout * 1000)
//					.setSocketTimeout(timeout * 1000)
//					.setRedirectsEnabled(true)
//					.setRelativeRedirectsAllowed(true)
//					.build();
//
//			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
//					.<ConnectionSocketFactory>create()
//					.register("http", new PlainConnectionSocketFactory())
//					.register("https", new SSLConnectionSocketFactory(/*sslContext*/getMyContext(), NoopHostnameVerifier.INSTANCE))
//
//					.build();
//			//=====  HTTP+HTTPS End ======
//			PoolingHttpClientConnectionManager cmPool = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
//			cmPool.setMaxTotal(100); // Increase max total connection to 200
//			cmPool.setDefaultMaxPerRoute(100); // Increase default max connection per route to 20
//
//			CloseableHttpClient httpClient = HttpClientBuilder.create()
//					.setDefaultRequestConfig(config)
//					.setConnectionManager(cmPool)
//					.build();
//
//			org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClientEngine engine = new ApacheHttpClient43Engine(httpClient);
			ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder().sslContext(getMyContext());
			resteasyClientBuilder.property("resteasy.client.connection.pooling", true);
			resteasyClientBuilder.property("resteasy.client.connection.pooling.max.connections", 100);
			resteasyClientBuilder.property("resteasy.client.connection.timeout", 5000); // Connection timeout in ms
			resteasyClientBuilder.property("resteasy.client.receive.timeout", 100000);   // Read timeout in ms
			client = resteasyClientBuilder.hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY).build();

			//Това е за да логваме всяко извикване
			client.register(new LogClientRequestFilter());
			client.register(new LogClientResponseFilter());

			client.httpEngine();
		} catch (Exception e) {

			LOGGER.error("Error in SimpleRestClient", e);
		}
	}


	public Client getClient() {
		return client;
	}

	private SSLContext getMyContext() {
		try {
			// Create an all-trusting TrustManager
			TrustManager[] trustAllCertificates = new TrustManager[]{
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
							// No validation for client certificates
						}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
							// No validation for server certificates
						}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[0];
						}
					}
			};

			// Initialize the SSLContext with the all-trusting TrustManager
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
			return sslContext;
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException("Failed to create SSL context that accepts any certificates", e);
		}

	}
}
