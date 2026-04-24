package com.ib.urireg.utils;


import bg.egov.regix.RegixClient;
import bg.egov.regix.RegixClientException;
import bg.egov.regix.requests.grao.GraoOperation;
import bg.egov.regix.requests.grao.nbd.PersonDataRequestType;
import bg.egov.regix.requests.grao.nbd.PersonDataResponseType;
import bg.egov.regix.requests.grao.nbd.PersonNames;
import bg.egov.regix.requests.grao.pna.PermanentAddressRequestType;
import bg.egov.regix.requests.grao.pna.PermanentAddressResponseType;

import bg.egov.regix.requests.mvr.MVROperation;
import bg.egov.regix.requests.mvr.bds.PersonalIdentityInfoRequestType;
import bg.egov.regix.requests.mvr.bds.PersonalIdentityInfoResponseType;
import bg.egov.regix.signeddata.CallContext;
import com.ib.system.BaseSystemData;
import com.ib.system.BaseUserData;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.JAXBHelper;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;
import com.ib.urireg.beans.LiceDocRegix;
import com.ib.urireg.db.dto.Lice;
import com.ib.urireg.system.UriregConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Objects;

/**
 * Тука ще има методи, които от респонса на регикс сетват данни в нашите обекти фзл/нфл/адреси и т.н.
 *
 * @author belev
 */
public class RegixUtils2 {
	private static final Logger LOGGER = LoggerFactory.getLogger(RegixUtils2.class);
	private static RegixClient regixClient;

	private RegixUtils2() {
	}

	public static RegixClient getRegixClient(BaseSystemData sd) throws RegixClientException {
		try {
			if (regixClient == null) { // иницализация трябва
				LOGGER.info("START RegixClient initialization ...");

				String keystore = sd.getSettingsValue("regix.keystore.location");
				if (keystore == null) {
					throw new RegixClientException("Missing Setting regix.keystore.location!");
				}
				String password = sd.getSettingsValue("regix.keystore.password");
				if (password == null) {
					throw new RegixClientException("Missing Setting regix.keystore.password!");
				}
				String type = sd.getSettingsValue("regix.keystore.type");
				if (type == null) {
					throw new RegixClientException("Missing Setting regix.keystore.type!");
				}
				String wsdl = sd.getSettingsValue("regix.wsdl.url");
				if (wsdl == null) {
					throw new RegixClientException("Missing Setting regix.wsdl.url!");
				}
				LOGGER.info("RegixClient wsdl={}", wsdl);

				try (InputStream input = new FileInputStream(keystore)) {

					regixClient = RegixClient.create(new URL(wsdl), input, password.toCharArray(), type);

					LOGGER.info("END RegixClient initialization ... success");
				} catch (Exception e) {
					LOGGER.error("RegixClient init ERROR", e);
					throw new RegixClientException(e);
				}
			}
		} catch (Exception e) {
			LOGGER.error("RegixClient init ERROR", e);
			throw new RegixClientException(e);
		}
		return regixClient;
	}


	public static RegixClient getRegixClient(String keystore, String type, String password, String wsdl) throws RegixClientException {
		try {
			if (regixClient == null) { // иницализация трябва
				LOGGER.info("START RegixClient initialization ...");


				if (keystore == null) {
					throw new RegixClientException("Missing Setting regix.keystore.location!");
				}

				if (password == null) {
					throw new RegixClientException("Missing Setting regix.keystore.password!");
				}

				if (type == null) {
					throw new RegixClientException("Missing Setting regix.keystore.type!");
				}

				if (wsdl == null) {
					throw new RegixClientException("Missing Setting regix.wsdl.url!");
				}
				LOGGER.info("RegixClient wsdl={}", wsdl);

				try (InputStream input = new FileInputStream(keystore)) {

					regixClient = RegixClient.create(new URL(wsdl), input, password.toCharArray(), type);

					LOGGER.info("END RegixClient initialization ... success");
				} catch (Exception e) {
					LOGGER.error("RegixClient init ERROR", e);
					throw new RegixClientException(e);
				}
			}
		} catch (Exception e) {
			LOGGER.error("RegixClient init ERROR", e);
			throw new RegixClientException(e);
		}
		return regixClient;
	}

	/**
	 * Зареждане на данни за физическо лице от REGIX по подадено ЕГН.
	 *
	 * @param lice
	 * @param egn
	 * @param loadLiceData         - дали искаме да се зареждат основни данни за лицето
	 * @param loadPermanentAddress - дали искаме да се зарежда постоянния адрес
	 * @return <code>true</code> ако има разлики от RegIX спрямо обекта, иначе <code>false</code>
	 * @throws RegixClientException
	 * @throws DatatypeConfigurationException
	 */
	public static boolean loadLiceByEgn(Lice lice, String egn //
			, boolean loadLiceData //
			, boolean loadPermanentAddress //
			, BaseSystemData sd) throws RegixClientException, DatatypeConfigurationException {
		boolean changed = false;

		RegixClient client = getRegixClient(sd);

		// зареждаме основните данни
		if (loadLiceData) {
			PersonDataRequestType requestMainData = new PersonDataRequestType();
			requestMainData.setEGN(egn);
			PersonDataResponseType responseMainData = (PersonDataResponseType) client.executeOperation(GraoOperation.PERSON_DATA_SEARCH, requestMainData);

			if (responseMainData != null) {
				responseMainData.setEGN(egn); // заради тестовият регикс, защото го подменя
				changed |= setLiceData(lice, responseMainData);
			}
		}

		// зареждане на постоянен адрес
		if (loadPermanentAddress) {
			PermanentAddressRequestType requestPermAddress = new PermanentAddressRequestType();
			requestPermAddress.setEGN(egn);
			requestPermAddress.setSearchDate(DateUtils.toGregorianCalendar(new Date()));

			PermanentAddressResponseType responsePermAddress = (PermanentAddressResponseType) client.executeOperation(GraoOperation.PERMANENT_ADDRESS_SEARCH, requestPermAddress);
			if (responsePermAddress != null) {
				changed |= setLiceAddress(lice, responsePermAddress);
			}
		}

		return changed;
	}


	/**
	 * Сетва в обекта Lice данните от RegIX. В данните от RegIX няма адреси и за това трябват да се ползва отделните методи за
	 * адреси
	 *
	 * @param lice
	 * @param response
	 * @return <code>true</code> ако има разлики от RegIX спрямо обекта, иначе <code>false</code>
	 */
	static boolean setLiceData(Lice lice, PersonDataResponseType response) {
		if (response == null || response.getPersonNames() == null) {
			return false;
		}
		lice.setEgn(response.getEGN()); // това е безусловно защото за нов запис трябва, а за корекция е еднакво

		boolean changed = false;

		//
		if (setPersonNames(lice, response.getPersonNames())) {
			changed = true;
		}

		//
		Date fzlBirthDate = DateUtils.toDate(response.getBirthDate());
		if (fzlBirthDate != null && !Objects.equals(lice.getBirthDate(), fzlBirthDate)) {
			changed = true;
			lice.setBirthDate(fzlBirthDate);
		}

		//
		String tmp = convertToTitleCase(response.getPlaceBirth(), true);
		if (!isStringEq(lice.getBirthPlace(), tmp)) {
			changed = true;
			lice.setBirthPlace(tmp);
		}

		return changed;
	}

	/**
	 * Сетва в обекта Lice данните от RegIX за постоянен адрес.
	 *
	 * @param lice
	 * @param response
	 * @return <code>true</code> ако има разлики от RegIX спрямо обекта, иначе <code>false</code>
	 */
	static boolean setLiceAddress(Lice lice, PermanentAddressResponseType response) {
		if (response == null) {
			return false;
		}
		lice.setAddrCountry(UriregConstants.CODE_ZNACHENIE_BG); // остава БГ

		boolean changed = false;

		//
		StringBuilder addrText = new StringBuilder();
		String t = SearchUtils.trimToNULL(response.getCityArea());
		if (t != null) {

			String areaCode = SearchUtils.trimToNULL(response.getCityAreaCode());
			if (areaCode != null && t.startsWith(areaCode + "_")) {
				t = t.substring(areaCode.length() + 1);
			}

			if (addrText.length() > 0) {
				addrText.append(", ");
			}
			addrText.append(t);
		}
		t = SearchUtils.trimToNULL(response.getLocationName());
		if (t != null) {
			if (addrText.length() > 0) {
				addrText.append(", ");
			}
			String tUp = t.toUpperCase();
			if (tUp.indexOf("БУЛ") == -1 && tUp.indexOf("УЛ") == -1 && tUp.indexOf("Ж.К") == -1 && tUp.indexOf("ЖК") == -1 && tUp.indexOf("ПЛ") == -1) {
				addrText.append("ул. "); // ако няма изрично булевар или улица добавям улица
			}
			addrText.append(t);
		}
		t = SearchUtils.trimToNULL(response.getBuildingNumber());
		if (t != null) {
			if (addrText.length() > 0) {
				addrText.append(" ");
			}
			addrText.append("№ " + t);
		}
		t = SearchUtils.trimToNULL(response.getEntrance());
		if (t != null) {
			if (addrText.length() > 0) {
				addrText.append(", ");
			}
			addrText.append("вх. " + t);
		}
		t = SearchUtils.trimToNULL(response.getFloor());
		if (t != null) {
			if (addrText.length() > 0) {
				addrText.append(", ");
			}
			addrText.append("ет. " + t);
		}
		t = SearchUtils.trimToNULL(response.getApartment());
		if (t != null) {
			if (addrText.length() > 0) {
				addrText.append(", ");
			}
			addrText.append("ап. " + t);
		}
		if (!isStringEq(lice.getAddrText(), addrText.toString())) {
			changed = true;
			lice.setAddrText(addrText.toString());
		}

		//
		Integer ekatte = null;
		String ekatteCode = SearchUtils.trimToNULL(response.getSettlementCode());
		if (ekatteCode != null) {
			try {
				ekatte = Integer.parseInt(ekatteCode);
			} catch (Exception e) { // при нас е число
			}
		}
		if (ekatte != null && !Objects.equals(lice.getAddrEkatte(), ekatte)) {
			changed = true;
			lice.setAddrEkatte(ekatte);
		}

		return changed;
	}

	/**
	 * сглобява ги с разделител " ". Разпределя ги по новите колони
	 */
	private static boolean setPersonNames(Lice lice, PersonNames personNames) {
		if (personNames == null) {
			personNames = new PersonNames(); // за да не се бърка в логиката ако няма данни ще си сработи правилно
		}

		boolean changed = false;

		String firstname = null;
		String surname = null;
		String lastname = null;

		StringBuilder names = new StringBuilder();
		if (personNames.getFirstName() != null) {
			if (names.length() > 0) {
				names.append(" ");
			}
			names.append(personNames.getFirstName());
			firstname = String.valueOf(personNames.getFirstName());
		}
		if (personNames.getSurName() != null) {
			if (names.length() > 0) {
				names.append(" ");
			}
			names.append(personNames.getSurName());
			surname = String.valueOf(personNames.getSurName());
		}
		if (personNames.getFamilyName() != null) {
			if (names.length() > 0) {
				names.append(" ");
			}
			names.append(personNames.getFamilyName());
			lastname = String.valueOf(personNames.getFamilyName());
		}

		firstname = convertToTitleCase(firstname, false);
		surname = convertToTitleCase(surname, false);
		lastname = convertToTitleCase(lastname, false);

		String tmp = convertToTitleCase(names.toString(), false);
		names.setLength(0);
		names.append(tmp);

		if (!isStringEq(lice.getNames(), names.toString())) {
			changed = true;
			lice.setNames(names.toString());
		}

		if (!isStringEq(lice.getFirstname(), firstname)) {
			changed = true;
			lice.setFirstname(firstname);
		}
		if (!isStringEq(lice.getSurname(), surname)) {
			changed = true;
			lice.setSurname(surname);
		}
		if (!isStringEq(lice.getLastname(), lastname)) {
			changed = true;
			lice.setLastname(lastname);
		}

		return changed;
	}

	/**
	 * Иска се ако от RegIX дойде празно да се смята, че няма разлика и не се пипа текста при нас !!!
	 *
	 * @return true ако са еднакви, като за еднакви се смятат "" и " " еднаково на НУЛЛ !!!
	 */
	private static boolean isStringEq(String babh, String regix) {
		babh = SearchUtils.trimToNULL(babh);
		regix = SearchUtils.trimToNULL(regix);
		return regix == null || "-".equals(regix) || Objects.equals(babh, regix);
	}


	public static void personCard(String egn, String lcNumber , LiceDocRegix liceDocRegix, BaseSystemData sd, BaseUserData userData, Integer liceId)  throws RegixClientException, DatatypeConfigurationException {

		try {

			CallContext callContext = new CallContext();
			bg.egov.regix.signeddata.ObjectFactory factory = new bg.egov.regix.signeddata.ObjectFactory();

			String serviceType = sd.getSettingsValue("regix.ctx.ServiceType");
			if (serviceType == null) {
				throw new RegixClientException("Missing Setting regix.ctx.ServiceType!");
			}

			String employeeIdentifier = ""+userData.getUserId();  // sd.getSettingsValue("regix.ctx.EmployeeIdentifier");
//			if (employeeIdentifier == null) {
//				throw new RegixClientException("Missing Setting regix.ctx.EmployeeIdentifier!");
//			}

			String employeeNames = userData.getLiceNames(); // sd.getSettingsValue("regix.ctx.EmployeeNames");
//			if (employeeNames == null) {
//				throw new RegixClientException("Missing Setting regix.ctx.EmployeeNames!");
//			}

			String employeeAditionalIdentifier = userData.getLoginName(); // sd.getSettingsValue("regix.ctx.EmployeeAditionalIdentifier");
//			if (employeeAditionalIdentifier == null) {
//				throw new RegixClientException("Missing Setting regix.ctx.EmployeeAditionalIdentifier!");
//			}

			String employeePosition = sd.getSettingsValue("regix.ctx.EmployeePosition");
			if (employeePosition == null) {
				throw new RegixClientException("Missing Setting regix.ctx.EmployeePosition!");
			}

			String administrationOId = sd.getSettingsValue("regix.ctx.AdministrationOId");
			if (administrationOId == null) {
				throw new RegixClientException("Missing Setting regix.ctx.AdministrationOId!");
			}

			String administrationName = sd.getSettingsValue("regix.ctx.AdministrationName");
			if (administrationName == null) {
				throw new RegixClientException("Missing Setting regix.ctx.AdministrationName!");
			}

			String lawReason = sd.getSettingsValue("regix.ctx.LawReason");
			if (lawReason == null) {
				throw new RegixClientException("Missing Setting regix.ctx.LawReason!");
			}

	//        Идентификатор на инстанцията на административната услуга или процедура в администрацията (например: номер на преписка)
			String serviceUri = "ServiceURI";
			if (liceId != null) {
				serviceUri = "" +  liceId;
			}
			callContext.setServiceURI("ServiceURI");
			callContext.setServiceType(serviceType); // regix.ctx.ServiceType
			callContext.setEmployeeIdentifier(factory.createCallContextEmployeeIdentifier(employeeIdentifier)); //
			callContext.setEmployeeNames(factory.createCallContextEmployeeNames(employeeNames)); // regix.ctx.EmployeeNames
			callContext.setEmployeeAditionalIdentifier(factory.createCallContextEmployeeAditionalIdentifier(employeeAditionalIdentifier)); // regix.ctx.EmployeeAditionalIdentifier
			callContext.setEmployeePosition(factory.createCallContextEmployeePosition(employeePosition)); // regix.ctx.EmployeePosition
			callContext.setAdministrationOId(factory.createCallContextAdministrationOId(administrationOId)); // regix.ctx.AdministrationOId
			callContext.setAdministrationName(factory.createCallContextAdministrationName(administrationName)); // regix.ctx.AdministrationName
			callContext.setLawReason(lawReason); // regix.ctx.LawReason

			RegixClient client = getRegixClient(sd);

			PersonalIdentityInfoRequestType request = new PersonalIdentityInfoRequestType();
			request.setEGN(egn);
			request.setIdentityDocumentNumber(lcNumber);

			PersonalIdentityInfoResponseType responseMainData = (PersonalIdentityInfoResponseType) client.executeOperation(MVROperation.BDS_PERSON_IDENTITY, request,callContext);

			if(responseMainData != null) {
				if (responseMainData.getReturnInformations() != null) {
					LOGGER.warn(JAXBHelper.objectToXml(responseMainData.getReturnInformations(), false));
				}
				
				liceDocRegix.setEgn(responseMainData.getEGN());
				liceDocRegix.setNames(responseMainData.getPersonNames().getFirstName() +" "+responseMainData.getPersonNames().getSurname() +" "+responseMainData.getPersonNames().getFamilyName() );
				liceDocRegix.setIssuerPlace(responseMainData.getIssuerPlace());
				liceDocRegix.setIssueDate(DateUtils.toDate(responseMainData.getIssueDate()));
				liceDocRegix.setValidDate(DateUtils.toDate(responseMainData.getValidDate()));
				liceDocRegix.setIdentityDocumentNumber(responseMainData.getIdentityDocumentNumber());
			}

		} catch (Exception e) {
			LOGGER.error("RegixClient - personCard init ERROR ", e);
			throw new RegixClientException(e);
		}

	}

	public static String convertToTitleCase(String s, boolean mrd) {
		if (s == null) {
			return null;
		}
		String titleCase = StringUtils.toTitleCase(s);

		if (mrd) {
			if (titleCase.contains("Гр.")) {
				titleCase = titleCase.replace("Гр.", "гр.");
			}
			if (titleCase.contains("С.")) {
				titleCase = titleCase.replace("С.", "с.");
			}
		}
		return titleCase;
	}

}
