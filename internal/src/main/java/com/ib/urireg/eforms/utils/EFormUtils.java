package com.ib.urireg.eforms.utils;


import com.ib.system.SysConstants;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.JAXBHelper;
import com.ib.system.utils.SearchUtils;
import com.ib.urireg.eforms.ServiceRequest;
import com.ib.urireg.system.SystemData;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfReader;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;






public class EFormUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EFormUtils.class);
	
	private SystemData sd = null;

	
	
	
	public EgovContainer parseEform(String fileName, byte[] bytes, SystemData systemData) throws UnexpectedResultException {
		
		LOGGER.debug("Entering parseEform");
		EgovContainer eCon = new EgovContainer();

		String xmlInfo = null;
		ServiceRequest sr;
		

				
		if (fileName != null && fileName.trim().toUpperCase().endsWith(".PDF") && bytes != null) {

			try {
				//PdfDocument pdfDoc = new PdfDocument(new PdfReader(SRC));
				PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)));

				//Метаданни за pdf-a
				PdfDocumentInfo info = pdfDoc.getDocumentInfo();

				//Къстом добавени данни :
				//jsonInfo = info.getMoreInfo("application.json_json");
				xmlInfo = info.getMoreInfo("application.json_xml");

				LOGGER.info(xmlInfo);


				if (xmlInfo != null && !xmlInfo.isEmpty()) {
					//Трансформ към клас

					sr = JAXBHelper.xmlToObject(ServiceRequest.class, xmlInfo);

					if (sr != null ) {

						eCon = processServiceRequest(sr, eCon, sd);


					}else {
						LOGGER.error("ServiceRequest sr is null !!!!!" );
					}

				}else{
					LOGGER.debug("xml property is null !!!!!" );
				}


			} catch (IOException e) {
				LOGGER.error("Проблем при изчитането на PDF файл: " + fileName);
				throw new UnexpectedResultException("Проблем при изчитането на PDF файл: " + fileName);
			} catch (JAXBException e) {

				LOGGER.error("Проблем при обработката на инфомацията от еформи в PDF файл: " + fileName);
				LOGGER.error("xmlInfo=" + xmlInfo);
				throw new UnexpectedResultException("Проблем при обработката на инфомацията от еформи в PDF файл: " + fileName);
			}
		}

		LOGGER.debug("Exiting parseEform");
		return eCon;
	}
	
	
	

	

	
	



	private EgovContainer processServiceRequest (ServiceRequest sr, EgovContainer eCon, SystemData sd) {

		String codeUsluga = null;
		String nameUsluga = null;
		String dopInfo = "";
		
		//Aplicant
		if (sr != null && sr.getApplicant() != null) {
			LOGGER.debug("APPLICANT:");
			if (sr.getApplicant().getPerson() != null && sr.getApplicant().getPerson().getPersonalData() != null && sr.getApplicant().getPerson().getPersonalData().getIdentifier() != null) {

				String imena = "";
				String egnLnch = sr.getApplicant().getPerson().getPersonalData().getIdentifier().getIdentifier();
				LOGGER.debug("APPLICANT  Идентификатор  :" + egnLnch);
				LOGGER.debug("APPLICANT  Идентификатор (тип) :" + sr.getApplicant().getPerson().getPersonalData().getIdentifier().getIdentifierType().getCode());
				LOGGER.debug("APPLICANT  First Name :" + sr.getApplicant().getPerson().getPersonalData().getFirstName());
				LOGGER.debug("APPLICANT  Middle Name :" + sr.getApplicant().getPerson().getPersonalData().getMiddleName());
				LOGGER.debug("APPLICANT  Family Name :" + sr.getApplicant().getPerson().getPersonalData().getFamilyName());

				if (! SearchUtils.isEmpty(sr.getApplicant().getPerson().getPersonalData().getFirstName())){
					imena += sr.getApplicant().getPerson().getPersonalData().getFirstName() + " ";
				}

				if (! SearchUtils.isEmpty(sr.getApplicant().getPerson().getPersonalData().getMiddleName())){
					imena += sr.getApplicant().getPerson().getPersonalData().getMiddleName() + " ";
				}

				if (! SearchUtils.isEmpty(sr.getApplicant().getPerson().getPersonalData().getFamilyName())){
					imena += sr.getApplicant().getPerson().getPersonalData().getFamilyName() + " ";
				}

				imena = imena.trim();


			}
		}

		
		
		//RequestAuthor
		if (sr != null && sr.getRequestAuthor() != null) {
			LOGGER.debug("APPLICANT:");
			if (sr.getRequestAuthor().getPerson() != null && sr.getRequestAuthor().getPerson().getPersonalData() != null && sr.getRequestAuthor().getPerson().getPersonalData().getIdentifier() != null) {

				String imena = "";
				String egnLnch = sr.getRequestAuthor().getPerson().getPersonalData().getIdentifier().getIdentifier();
				LOGGER.debug("RequestAuthor  Идентификатор  :" + egnLnch);
				LOGGER.debug("RequestAuthor  Идентификатор (тип) :" + sr.getRequestAuthor().getPerson().getPersonalData().getIdentifier().getIdentifierType().getCode());
				LOGGER.debug("RequestAuthor  First Name :" + sr.getRequestAuthor().getPerson().getPersonalData().getFirstName());
				LOGGER.debug("RequestAuthor  Middle Name :" + sr.getRequestAuthor().getPerson().getPersonalData().getMiddleName());
				LOGGER.debug("RequestAuthor  Family Name :" + sr.getRequestAuthor().getPerson().getPersonalData().getFamilyName());

				if (! SearchUtils.isEmpty(sr.getRequestAuthor().getPerson().getPersonalData().getFirstName())){
					imena += sr.getRequestAuthor().getPerson().getPersonalData().getFirstName() + " ";
				}

				if (! SearchUtils.isEmpty(sr.getRequestAuthor().getPerson().getPersonalData().getMiddleName())){
					imena += sr.getRequestAuthor().getPerson().getPersonalData().getMiddleName() + " ";
				}

				if (! SearchUtils.isEmpty(sr.getRequestAuthor().getPerson().getPersonalData().getFamilyName())){
					imena += sr.getRequestAuthor().getPerson().getPersonalData().getFamilyName() + " ";
				}

				imena = imena.trim();

				dopInfo += "Имена на завител: " + imena + "\r\n";
				dopInfo += "Идентификатор на завител: " + egnLnch + "\r\n";

				eCon.setEgnLnch(egnLnch);


			}
		}

		
		Node node = ((Node) sr.getSpecificContent());
		

		
		//Public service
		if (sr.getPublicService() != null) {
			
			if (sr.getPublicService().getIdentifier() != null && sr.getPublicService().getIdentifier().getIdentifier() != null && ! sr.getPublicService().getIdentifier().getIdentifier().trim().isEmpty()) {
				codeUsluga = sr.getPublicService().getIdentifier().getIdentifier();
				LOGGER.debug("Код на електронна административна услуга: " + codeUsluga);
				dopInfo += "Код на електронна административна услуга: " + codeUsluga + "\r\n";

			}
			
			if (sr.getPublicService().getName() != null && ! sr.getPublicService().getName().trim().isEmpty()) {
				nameUsluga  = sr.getPublicService().getName().trim();
				LOGGER.debug("Наименование на електронна административна услуга: " + nameUsluga);
				dopInfo += "Наименование на електронна административна услуга: " + nameUsluga + "\r\n";
			}

			if (sr.getRequestDateTime() != null) {
				LOGGER.debug("Дата на подаване на услугата: " + codeUsluga);
			}
		}
		
		
		String vResult = null; 
		if (sr != null && sr.getResultChannel() != null) {
			vResult = sr.getResultChannel().getIdentifier();
			LOGGER.debug("КАНАЛ (ЗА РЕЗУЛТАТ)");
			LOGGER.debug("Идентификатор :" + sr.getResultChannel().getIdentifier());
			if (sr.getResultChannel().getChannelType() != null) {
				LOGGER.debug("RESULT channelType :" + sr.getResultChannel().getChannelType().getCode());
				LOGGER.debug("RESULT channelType (Текст) :" + sr.getResultChannel().getChannelType().getName());
				dopInfo += "Начин на получаване на резултат: " + sr.getResultChannel().getChannelType().getName() + "\r\n";
			}
		}




		LOGGER.info("-------------------------------- Specific Info --------------------------------");
		eCon.setParsedInfo(dopInfo);
		proccessSpecificData(eCon, node, codeUsluga, sd);

		return eCon;
	}
	


	private EgovContainer proccessSpecificData(EgovContainer container, Node node, String codeUsluga, SystemData sd) {
		LOGGER.debug("Leaving proccessSpecificData ");

		String dopInfo = container.getParsedInfo();
		if (dopInfo == null){
			dopInfo = "";
		}

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		String sad = getSingleStringValue(node, "applicantDistrictCourtInternship", "завършил /а/ стажа в Окръжен съд", false);
		Date datIzp = getSingleDateValue(node, "applicantExaminationDay", "и положил /а/ изпит за придобиване на юридическа правоспособност на", false);
		String rnUd = getSingleStringValue(node, "sertificateNumber", "Удостоверение за юридическа правоспособност издадено от министъра на правосъдието  №", false);
		Date datUd = getSingleDateValue(node, "sertificateIssueDate", "Удостоверение за юридическа правоспособност издадено от министъра на правосъдието от дата", false);
		String prichina = getSingleStringDecodeValue(node, "originalLoss", "Оригиналът е ", false);



		if (! SearchUtils.isEmpty(sad)) {
			dopInfo += "Завършил /а/ стажа в Окръжен съд: " +  sad + "\r\n";
		}
		if (datIzp != null) {
			dopInfo += "Положил /а/ изпит за придобиване на юридическа правоспособност на " +  sdf.format(datIzp) + "\r\n";
		}

		if (! SearchUtils.isEmpty(rnUd)) {
			dopInfo += "Удостоверение за юридическа правоспособност издадено от министъра на правосъдието  №: " +  rnUd + "\r\n";
		}
		if (datUd != null) {
			dopInfo += "Удостоверение за юридическа правоспособност издадено от министъра на правосъдието Дата: " +  sdf.format(datUd) + "\r\n";
		}

		if (! SearchUtils.isEmpty(prichina)) {
			dopInfo += "Оригиналът е " +  prichina + "\r\n";
		}

		container.setParsedInfo(dopInfo);

		LOGGER.debug("Leaving proccessSpecificData ");
		return container;
	}


	private  ArrayList<Node> findNodesByName(Node node, String nodeName, boolean startWith){
		ArrayList<Node> result = new ArrayList<Node>();

		if (nodeName == null) {
			return new ArrayList<Node>();
		}

		if (startWith) {
			if (node.getNodeName().toUpperCase().startsWith(nodeName.trim().toUpperCase())) {
				result.add(node);
			}
		}else {
			if (node.getNodeName().equalsIgnoreCase(nodeName.trim())) {
				result.add(node);
			}
		}

		if (node.getChildNodes() != null) {
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				result.addAll(findNodesByName(node.getChildNodes().item(i), nodeName, startWith));
			}
		}



		return result;
	}

	private  ArrayList<Node> findNodesIncludeInName(Node node, String tekst){
		ArrayList<Node> result = new ArrayList<Node>();

		if (tekst == null) {
			return new ArrayList<Node>();
		}


		if (node.getNodeName().toUpperCase().toUpperCase().contains(tekst.trim().toUpperCase())) {
			result.add(node);
		}


		if (node.getChildNodes() != null) {
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				result.addAll(findNodesIncludeInName(node.getChildNodes().item(i), tekst));
			}
		}



		return result;
	}

	
	private String getSimpleNodeValue(Node node) {
		if (node.getNodeValue() != null) {
			return node.getNodeValue();			
		}else {
			if (node.getChildNodes() != null) {
				for (int i = 0; i < node.getChildNodes().getLength(); i++) {	
					Node child = node.getChildNodes().item(i);
					if ( child.getNodeName() != null && child.getNodeName().equalsIgnoreCase( "#text")) {						
						if (child.getNodeValue() != null && ! child.getNodeValue().trim().isEmpty())
						return child.getNodeValue();
					}
				}
				
				for (int i = 0; i < node.getChildNodes().getLength(); i++) {	
					Node child = node.getChildNodes().item(i);
					if ( child.getNodeName() != null && child.getNodeName().equalsIgnoreCase( "value")) {						
						return getSimpleNodeValue(child);
					}
				}
			}
		}
		
		return null;
	}

	private String getSimpleDecodeNodeValue(Node node) {
		if (node.getNodeValue() != null) {
			return node.getNodeValue();
		}else {
			if (node.getChildNodes() != null) {
				for (int i = 0; i < node.getChildNodes().getLength(); i++) {
					Node child = node.getChildNodes().item(i);
					if ( child.getNodeName() != null && child.getNodeName().equalsIgnoreCase( "#text")) {
						if (child.getNodeValue() != null && ! child.getNodeValue().trim().isEmpty())
							return child.getNodeValue();
					}
				}

				for (int i = 0; i < node.getChildNodes().getLength(); i++) {
					Node child = node.getChildNodes().item(i);
					if ( child.getNodeName() != null && child.getNodeName().equalsIgnoreCase( "label")) {
						return getSimpleDecodeNodeValue(child);
					}
				}
			}
		}

		return null;
	}



	private String getSimpleNodeLabel(Node node) {
		if (node.getNodeValue() != null) {
			return node.getNodeValue();
		}else {
			if (node.getChildNodes() != null) {


				for (int i = 0; i < node.getChildNodes().getLength(); i++) {
					Node child = node.getChildNodes().item(i);
					if ( child.getNodeName() != null && child.getNodeName().equalsIgnoreCase( "label")) {
						return getSimpleNodeValue(child);
					}
				}
			}
		}

		return null;
	}

	
	
	/** Метод за извличане и логване на повече от една стойност от node с име nodeName, който е дете на  parentNode
	 
	 * @param parentNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим 
	 * @param startWith дали името да е по пълно съвпадение, или да започва с него
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)
	 * @param decodeClassif - код на класификацията, от която е значението (използва се за логване, дали е декодирано правилно)
	 * @param sd2 инстанция на SystemData, с която се декодира
	 * @return Списък, без повторение (TreeSet), от намерени значения 
	 * @throws DbErrorException Грешка при опит за декодиране на значението
	 */
	private TreeSet<Integer> getMultiNomenValues(Node parentNode, String nodeName, boolean startWith, String label, Integer decodeClassif, SystemData sd2) throws DbErrorException {
		
		TreeSet<Integer> result = new TreeSet<Integer>();
		
		ArrayList<Node> childNode = findNodesByName(parentNode, nodeName, startWith);
		if (childNode.size() > 0) {
			for (Node child : childNode) {
				String val =  getSimpleNodeValue(child);
				
				
				if (val != null) {
					LOGGER.debug(label + ": " + val);
					try {
						Integer codeVal = Integer.parseInt(val);						
						String tekst = sd2.decodeItem(decodeClassif, codeVal, SysConstants.CODE_DEFAULT_LANG, new Date());
						LOGGER.debug(label + " (Декодирано): " + tekst  );
						
						result.add(codeVal);
						
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	
				}
			}
		}
		
		
		return result;
	}
	
	/** Метод за извличане и логване на повече от една стойност от node с име nodeName, който е дете на  parentNode
	 
	 * @param parentNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим 
	 * @param startWith дали името да е по пълно съвпадение, или да започва с него
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)
	 * @return Списък, без повторение (TreeSet), от намерени значения 
	 * @throws DbErrorException Грешка при опит за декодиране на значението
	 */
	private TreeSet<String> getMultiStringValues(Node parentNode, String nodeName, boolean startWith, String label) throws DbErrorException {
		
		TreeSet<String> result = new TreeSet<String>();
		
		ArrayList<Node> childNode = findNodesByName(parentNode, nodeName, startWith);
		if (childNode.size() > 0) {
			for (Node child : childNode) {
				String val =  getSimpleNodeValue(child);
				
				
				if (val != null) {
					LOGGER.debug(label + ": " + val);
					result.add(val);
				}
			}
		}
		
		
		return result;
	}
	
	
	
	/** Метод за извличане и логване на повече от една стойност от node с име nodeName, който е дете на  parentNode
	 * Заради спецификата на електронните форми, които не поддържат контрола дърво, значение от по долно ниво се избира след избора на родителите му
	 * Обикновено най високото ниво е име на node, без код в него. Например animal. избраното дете с node с име   animalКод, където кода е стойността на node animal
	 * Объркано е. Ако някой ден ви се наложи да го ползвате - Питайте Васко. Стига да не е загубил съвсем ума си до тогава ... :(   
	 * @param parentNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим 
	 * @param startWith дали името да е по пълно съвпадение, или да започва с него
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)
	 * @param decodeClassif - код на класификацията, от която е значението (използва се за логване, дали е декодирано правилно)
	 * @param sd2 инстанция на SystemData, с която се декодира
	 * @param includeAll - дали да връща и родителите или само избраните крайни значения (листа)
	 * @return Списък, без повторение (TreeSet), от намерени значения 
	 * @throws DbErrorException Грешка при опит за декодиране на значението
	 */
	private TreeSet<Integer> getMultiNomenValuesIerarh(Node parentNode, String nodeName, boolean startWith, String label, Integer decodeClassif, SystemData sd2, boolean includeAll) throws DbErrorException {
		TreeSet<Integer> values = getMultiNomenValuesIerarhRec(parentNode, nodeName, nodeName, startWith, label, decodeClassif, sd2, includeAll);
		for (Integer codeVal : values) {
			String tekst = sd2.decodeItem(decodeClassif, codeVal, SysConstants.CODE_DEFAULT_LANG, new Date());
			LOGGER.debug(label + " (Декодирано): " + tekst  );			
		}
		
		return values;
		
	}
	
	
	//Рекурсията на горната фрункция
	private TreeSet<Integer> getMultiNomenValuesIerarhRec(Node parentNode, String nodeName, String curNodeName, boolean startWith, String label, Integer decodeClassif, SystemData sd2, boolean includeAll) throws DbErrorException {
		
		TreeSet<Integer> result = new TreeSet<Integer>();
		
		ArrayList<Node> childNode = findNodesByName(parentNode, curNodeName, startWith);
		if (childNode.size() > 0) {
			for (Node child : childNode) {
				String val =  getSimpleNodeValue(child);
				
				
				if (val != null) {
					//LOGGER.debug("Passing " + label + ": " + val);
					try {
						Integer codeVal = Integer.parseInt(val);						
						//String tekst = sd2.decodeItem(decodeClassif, codeVal, BabhConstants.CODE_DEFAULT_LANG, new Date());
						//LOGGER.debug("Passing " + label + " (Декодирано): " + tekst  );
						
						if (includeAll) {
							result.add(codeVal);
							result.addAll(getMultiNomenValuesIerarhRec(parentNode, nodeName, nodeName+codeVal, false, label, decodeClassif, sd2, includeAll));
						}else {
							TreeSet<Integer> tek = getMultiNomenValuesIerarhRec(parentNode, nodeName, nodeName+codeVal, false, label, decodeClassif, sd2, includeAll);
							if (tek.size() > 0) {
								result.addAll(tek);
							}else {
								result.add(codeVal);
							}
						}
						
						
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	
				}
			}
		}
		
		
		return result;
	}
	
	
	
	/** Метод за извличане и логване на единична стойност от тип String от node с име nodeName, който е дете на  parentNode
	 *  Ако в децата има повече от един node с такова име - се взима първия намерен
	 * @param rootNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим 
	 * @param startWith дали името да е по пълно съвпадение, или да започва с него
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)	
	 * @return обект от тип String
	 */
	private String getSingleStringValue(Node rootNode, String nodeName, String label,  boolean startWith) {
		ArrayList<Node> nodes = findNodesByName(rootNode, nodeName, startWith);
		if (nodes.size() > 0) {
			for (Node child : nodes) {
				String val =  getSimpleNodeValue(child); //getSimpleDecodeNodeValue
				LOGGER.debug(label + ": " + val);
				return val;
			}
		}
		
		return null;
	}


	private String getSingleStringDecodeValue(Node rootNode, String nodeName, String label,  boolean startWith) {
		ArrayList<Node> nodes = findNodesByName(rootNode, nodeName, startWith);
		if (nodes.size() > 0) {
			for (Node child : nodes) {
				String val =  getSimpleDecodeNodeValue(child);
				LOGGER.debug(label + ": " + val);
				return val;
			}
		}

		return null;
	}

	/** Метод за извличане и логване на единична декодирана стойност на номенклатура с име nodeName, който е дете на  parentNode
	 *  Ако в децата има повече от един node с такова име - се взима първия намерен
	 * @param rootNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим
	 * @param startWith дали името да е по пълно съвпадение, или да започва с него
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)
	 * @return обект от тип String
	 */
	private String getSingleDecodeValue(Node rootNode, String nodeName, String label,  boolean startWith) {
		ArrayList<Node> nodes = findNodesByName(rootNode, nodeName, startWith);
		if (nodes.size() > 0) {
			for (Node child : nodes) {
				String val =  getSimpleNodeLabel(child);
				LOGGER.debug(label + ": " + val);
				return val;
			}
		}

		return null;
	}
	
	
	
	
	/** Метод за извличане и логване на единична стойност от тип Дата от node с име nodeName, който е дете на  parentNode
	 *  Ако в децата има повече от един node с такова име - се взима първия намерен
	 * @param rootNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим 
	 * @param startWith дали името да е по пълно съвпадение, или да започва с него
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)	
	 * @return обект от тип java.util.Date
	 */
	private Date getSingleDateValue(Node rootNode, String nodeName, String label,  boolean startWith) {
		ArrayList<Node> nodes = findNodesByName(rootNode, nodeName, startWith);
		if (nodes.size() > 0) {
			for (Node child : nodes) {
				String val =  getSimpleNodeValue(child);
				LOGGER.debug(label + ": " + val);
				if (val == null) {
					return null;
				}else {
					try {
						return DateUtils.convertXmlDateStringToDate(val);
					} catch (ParseException e) {
						LOGGER.error("Грешка при конвертиране до дата на значение " + val + " за атрибут " + label, e);
					}
				}
			}
		}
		
		return null;
	}
	
	/** Метод за извличане и логване на код (Integer) на значение от номенклатура  от node с име nodeName, който е дете на  parentNode
	 *  Ако в децата има повече от един node с такова име - се взима първия намерен
	 * @param parentNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим 
	 * @param startWith дали името да е по пълно съвпадение, или да започва с него
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)
	 * @param decodeClassif - код на класификацията, от която е значението (използва се за логване, дали е декодирано правилно)
	 * @param sd2 инстанция на SystemData, с която се декодира
	 * @return код на намерено значение
	 * @throws DbErrorException Грешка при опит за декодиране на значението
	 */
	private Integer getSingleNomenValue(Node parentNode, String nodeName, boolean startWith, String label, Integer decodeClassif, SystemData sd2) throws DbErrorException {
		
		
		
		ArrayList<Node> childNode = findNodesByName(parentNode, nodeName, startWith);
		if (childNode.size() > 0) {
			for (Node child : childNode) {
				String val =  getSimpleNodeValue(child);
				
				
				if (val != null) {
					LOGGER.debug(label + ": " + val);
					try {
						Integer codeVal = Integer.parseInt(val);						
						String tekst = sd2.decodeItem(decodeClassif, codeVal, SysConstants.CODE_DEFAULT_LANG, new Date());
						LOGGER.debug(label + " (Декодирано): " + tekst  );
						
						return codeVal;
						
					} catch (NumberFormatException e) {
						LOGGER.error("Грешка при конвертиране до число на значение " + val + " за атрибут " + label, e);
					}	
				}
			}
		}
		
		
		return null;
	}
	
	
	/** Метод за извличане и логване на стойност тип Integer от node с име nodeName, който е дете на  parentNode
	 *  Ако в децата има повече от един node с такова име - се взима първия намерен
	 * @param parentNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим 
	 * @param startWith дали името да е по пълно съвпадение, или да започва с него
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)
	 * @return намерено значение от тип Integer
	 * @throws DbErrorException Грешка при опит за декодиране на значението
	 */
	private Integer getSingleIntegerValue(Node parentNode, String nodeName, String label, boolean startWith) throws DbErrorException {
		
		
		
		ArrayList<Node> childNode = findNodesByName(parentNode, nodeName, startWith);
		if (childNode.size() > 0) {
			for (Node child : childNode) {
				String val =  getSimpleNodeValue(child);
				
				if (val != null) {
					
					try {
						if (val != null && ! val.equalsIgnoreCase("null")) {
							Integer codeVal = Integer.parseInt(val);
							LOGGER.debug(label + ": " + val);
							return codeVal;
						}
						
					} catch (NumberFormatException e) {
						LOGGER.error("Грешка при конвертиране до число на значение " + val + " за атрибут " + label, e);
					}	
				}
			}
		}
		
		
		return null;
	}
	
	private TreeSet<Integer> getLeaves(TreeSet<Integer> values, List<SystemClassif> classif) {
		TreeSet<Integer> result = new TreeSet<Integer>();
		
		for (Integer val : values) {
			boolean hasChildren = false;
			for (SystemClassif tek : classif) {
				if (val.intValue() == tek.getCodeParent()) {
					hasChildren = true;
					break;
				}
			}
			
			if (! hasChildren) {
				result.add(val);
			}
			
		}
		
		return result;
		
	}
	
	
	/** Метод за извличане и логване на единична стойност от тип String от node с име nodeName, който е дете на  parentNode
	 * 
	 *  Различното тук е че търсим само такива които се повтарят като в различни заявления към услуга и не искаме да ги объркаме с друго поле
	 *  Пример: Ако търсим number да ми намери number1 или number2, но не и numberOfFeedMaterials, numberOfLines 
	 * 
	 *  Ако в децата има повече от един node с такова име - се взима първия намерен
	 * @param rootNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим	 *
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)	
	 * @return обект от тип String
	 */
	private String getRepeatedSingleStringValue(Node rootNode, String nodeName, String label) {
		ArrayList<Node> nodes = findNodesByName(rootNode, nodeName, true);
		if (nodes.size() > 0) {
			for (Node child : nodes) {
				
				String test = child.getNodeName().replace(nodeName, "").trim();
				
				if (! SearchUtils.isEmpty(test)) {
					try {
						Integer.parseInt(test);
					} catch (NumberFormatException e) {
						continue;
					}
				}
				
				String val =  getSimpleNodeValue(child);
				
				LOGGER.debug(label + ": " + val);
				return val;
			}
		}
		
		return null;
	}
	
	
	/** Метод за извличане и логване на единична стойност от тип Дата от node с име nodeName, който е дете на  parentNode
	 *  
	 *  Различното тук е че търсим само такива които се повтарят като в различни заявления към услуга и не искаме да ги объркаме с друго поле
	 *  Пример: Ако търсим number да ми намери date1 или date2, но не и dateOfBirth, dateOfAlabala 
	 * 
	 *  Ако в децата има повече от един node с такова име - се взима първия намерен
	 * @param rootNode Родителски node, в който търсим
	 * @param nodeName име на node, който търсим	 *
	 * @param label Говоримо име на атрибута, който търсим (използва се за логване)	
	 * @return обект от тип java.util.Date
	 */
	private Date getRepeatedSingleDateValue(Node rootNode, String nodeName, String label) {
		ArrayList<Node> nodes = findNodesByName(rootNode, nodeName, true);
		if (nodes.size() > 0) {
			for (Node child : nodes) {
				
				String test = child.getNodeName().replace(nodeName, "").trim();
				
				if (! SearchUtils.isEmpty(test)) {
					try {
						Integer.parseInt(test);
					} catch (NumberFormatException e) {
						continue;
					}
				}
				
				
				String val =  getSimpleNodeValue(child);
				LOGGER.debug(label + ": " + val);
				if (val == null) {
					return null;
				}else {
					try {
						return DateUtils.convertXmlDateStringToDate(val);
					} catch (ParseException e) {
						LOGGER.error("Грешка при конвертиране до дата на значение " + val + " за атрибут " + label, e);
					}
				}
			}
		}
		
		return null;
	}

}
