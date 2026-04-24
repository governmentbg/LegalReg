package com.ib.urireg.experimental;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.omnifaces.cdi.Eager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Държи нещата необходими за частта Деловодство от работния плот.
 * Дреме в ApplicationScope-to дефиниран в faces-config
 *
 * @author vassil
 *
 */

@Named("globalDocHolder")
@ApplicationScoped
@Eager
public class GlobalHolder implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalHolder.class);



}
