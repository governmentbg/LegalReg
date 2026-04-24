package com.ib.urireg.system;

import com.ib.system.BaseSystemData;
import com.ib.system.exceptions.DbErrorException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.omnifaces.cdi.Eager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Конкретната за системата
 */
@Named
@ApplicationScoped
@Eager
public class SystemData extends BaseSystemData {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemData.class);

	/** */
	private static final long serialVersionUID = 5432305666789502131L;

	private String srcVersion	= "";

	/**  */
	public SystemData() {
		super();
		try {
			this.srcVersion = getSettingsValue("urireg.src.version");
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на 'urireg.src.version'", e);
		}
	}

	/**
	 * Разкодира значения (кодове с разделител ',') по език към конкретна дата като в разкодирания резултата пак са с разделител
	 * запетая
	 *
	 * @param codeClassif
	 * @param codes
	 * @param lang
	 * @param date
	 * @return
	 * @throws DbErrorException
	 */
	public String decodeItems(Integer codeClassif, String codes, Integer lang, Date date) throws DbErrorException {
		if (codes == null || codes.isEmpty()) {
			return null;
		}
		if (codes.indexOf(',') == -1) { // само един е
			return decodeItem(codeClassif, Integer.valueOf(codes), lang, date);
		}

		String[] arr = codes.split(",");

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			String code = arr[i];

			sb.append(decodeItem(codeClassif, Integer.valueOf(code), lang, date));
			if (i < arr.length - 1) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	/** @see BaseSystemData#createDynamicAdapterInstance() */
	@Override
	protected Object createDynamicAdapterInstance() {
		return new UriregClassifAdapter(this);
	}

	/** @return the srcVersion */
	public String getSrcVersion() {
		return this.srcVersion;
	}
}
