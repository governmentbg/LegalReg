package com.ib.urireg.components;

import com.ib.system.db.JPA;
import com.ib.urireg.db.dto.*;
import com.ib.urireg.experimental.ObjectComparator;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.SystemData;
import com.ib.indexui.CompObjAuditSys;
import com.ib.indexui.db.dto.AdmGroup;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.db.dto.StatTable;
import com.ib.indexui.db.dto.UniversalReport;
import com.ib.indexui.report.uni.SprObject;
import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.ObjectsDifference;
import com.ib.system.SysConstants;
import com.ib.system.db.dto.*;
import com.ib.system.utils.JAXBHelper;
import jakarta.enterprise.context.Dependent;
import jakarta.faces.component.FacesComponent;
import jakarta.inject.Named;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** */
@FacesComponent(value = "compObjAudit", createTag = true)
@Named("auditComparator")
@Dependent
public class CompObjAudit extends CompObjAuditSys {

	private static final Logger LOGGER = LoggerFactory.getLogger(CompObjAudit.class);


	@Override
	/**
	 * Тука се правят едни врътки за да може в основният екран на досието да се покажат през преглед на журнала
	 * прикачените файлове за заявленията и уд на лицето. Може да се развива и за други подобни неща ако се иска.
	 */
	public void initObjAudit() {
		super.initObjAudit();

		Integer codeObj = (Integer) getAttributes().get("codeObj");
		if (!Objects.equals(codeObj, UriregConstants.CODE_ZNACHENIE_JOURNAL_LICE)) {
			return; // само за лице има такива дивотии
		}
		Integer idObj = (Integer) getAttributes().get("idObj");

		Set<Map.Entry<Integer, Integer>> liceDocIdents = new HashSet<>(); // <ид, код> на тези документи

		try {
			for (SystemJournal j : getDocHistory()) {
				if (Objects.equals(j.getJoinedIdObject1(), idObj) && Objects.equals(j.getJoinedCodeObject1(), codeObj)
						&& Objects.equals(j.getCodeObject(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC)) {

					// това е идента на документа
					liceDocIdents.add(new AbstractMap.SimpleEntry<>(j.getIdObject(), j.getCodeObject()));
				}
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на свързани данни от журнала", e);
		} // горе няма работа с БД и няма клосе

		if (liceDocIdents.isEmpty()) {
			return;
		}
		try {
			getDocHistory().addAll(findFilesRows(liceDocIdents));

			getDocHistory().sort(Comparator.comparing(SystemJournal::getId));
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на данни от журнала", e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}


	@SuppressWarnings("unchecked")
	private List<SystemJournal> findFilesRows(Set<Map.Entry<Integer, Integer>> othersIdent) {
		Map<String, Integer> params = new HashMap<>();

		StringBuilder sql = new StringBuilder();
		sql.append(" select x from SystemJournal x where 1=1 ");

		int index = 0;
		for (Map.Entry<Integer, Integer> entry : othersIdent) {
			if (index == 0) {
				sql.append(" and ( ");
			} else {
				sql.append(" or ");
			}

			String idArg = "idArg_" + index;
			String codeArg = "codeArg_" + index;

			sql.append(" (x.codeObject = :codeFile and x.joinedIdObject1 = :"+idArg+" and x.joinedCodeObject1 = :"+codeArg+") ");

			params.put(idArg, entry.getKey());
			params.put(codeArg, entry.getValue());

			index++;
		}
		sql.append(" ) order by x.id asc ");

		Query query = JPA.getUtil().getEntityManager().createQuery(sql.toString());

		query.setParameter("codeFile", SysConstants.CODE_ZNACHENIE_JOURNAL_FILE);

		for (Map.Entry<String, Integer> entry : params.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
		}
		return query.getResultList();
	}

	/**
	 * Зарежда текущите разликите - сегашно и предишно състояние
	 * @param currentEventTmp
	 * @param previousEventTmp
	 */
	@Override
	public List<ObjectsDifference> loadCurrentDiff(SystemJournal currentEventTmp,SystemJournal previousEventTmp) {
		List<ObjectsDifference> compareResult=new ArrayList<>();


		LOGGER.debug("LoadCurrentDiff between {} and {}",currentEventTmp!=null?currentEventTmp.getId():null,previousEventTmp!=null?previousEventTmp.getId():null);

		try {

//			Object xmlToObject2 = JAXBHelper.xmlToObject2(getSelectedEvent().getObjectXml());
//			System.out.println("==========================="+xmlToObject2.getClass());

			Object currentObj=null,prevObj=null;
			Integer codeObject=currentEventTmp!=null?currentEventTmp.getCodeObject():previousEventTmp.getCodeObject();
			Integer codeAction=currentEventTmp!=null?currentEventTmp.getCodeAction():previousEventTmp.getCodeAction();

			if (codeAction != null && codeAction.equals(UriregConstants.CODE_DEIN_UNISEARCH)) {
				if (previousEventTmp.getObjectXml() != null) {
					SprObject spr = JAXBHelper.xmlToObject(SprObject.class, previousEventTmp.getObjectXml());

					return convertSprObjectToDifferences(spr, getSystemData(), previousEventTmp.getDateAction()) ;
				}
			}


			switch (codeObject) {


				case UriregConstants.CODE_ZNACHENIE_JOURNAL_FILE:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Files.class, currentEventTmp.getObjectXml()):new Files();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Files.class, previousEventTmp.getObjectXml()):new Files();
					break;

				case UriregConstants.CODE_ZNACHENIE_JOURNAL_USER:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(AdmUser.class, currentEventTmp.getObjectXml()):new AdmUser();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(AdmUser.class, previousEventTmp.getObjectXml()):new AdmUser();
					break;



				case UriregConstants.CODE_ZNACHENIE_JOURNAL_REFERENT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Referent.class, currentEventTmp.getObjectXml()):new Referent();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Referent.class, previousEventTmp.getObjectXml()):new Referent();
					break;

				case SysConstants.CODE_ZNACHENIE_JOURNAL_OPTION:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SystemOption.class, currentEventTmp.getObjectXml()):new SystemOption();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SystemOption.class, previousEventTmp.getObjectXml()):new SystemOption();
					break;


				case Constants.CODE_ZNACHENIE_JOURNAL_STAT_TABLE:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(StatTable.class, currentEventTmp.getObjectXml()):new StatTable();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(StatTable.class, previousEventTmp.getObjectXml()):new StatTable();
					break;
				case Constants.CODE_ZNACHENIE_JOURNAL_UNI_REPORT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(UniversalReport.class, currentEventTmp.getObjectXml()):new UniversalReport();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(UniversalReport.class, previousEventTmp.getObjectXml()):new UniversalReport();
					break;

				case SysConstants.CODE_ZNACHENIE_JOURNAL_OPIS:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SystemClassifOpis.class, currentEventTmp.getObjectXml()):new SystemClassifOpis();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SystemClassifOpis.class, previousEventTmp.getObjectXml()):new SystemClassifOpis();
					break;
				case SysConstants.CODE_ZNACHENIE_JOURNAL_CLASSIF:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SystemClassif.class, currentEventTmp.getObjectXml()):new SystemClassif();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SystemClassif.class, previousEventTmp.getObjectXml()):new SystemClassif();
					break;
				case SysConstants.CODE_ZNACHENIE_JOURNAL_LIST:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SyslogicListOpisEntity.class, currentEventTmp.getObjectXml()):new SyslogicListOpisEntity();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SyslogicListOpisEntity.class, previousEventTmp.getObjectXml()):new SyslogicListOpisEntity();
					break;
				case SysConstants.CODE_ZNACHENIE_JOURNAL_LISTROW:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(SyslogicListEntity.class, currentEventTmp.getObjectXml()):new SyslogicListEntity();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(SyslogicListEntity.class, previousEventTmp.getObjectXml()):new SyslogicListEntity();
					break;


				case UriregConstants.CODE_ZNACHENIE_JOURNAL_GROUPUSER:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(AdmGroup.class, currentEventTmp.getObjectXml()):new AdmGroup();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(AdmGroup.class, previousEventTmp.getObjectXml()):new AdmGroup();
					break;

				case UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Doc.class, currentEventTmp.getObjectXml()):new Doc();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Doc.class, previousEventTmp.getObjectXml()):new Doc();
					break;

				case UriregConstants.CODE_ZNACHENIE_JOURNAL_LICE:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Lice.class, currentEventTmp.getObjectXml()):new Lice();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Lice.class, previousEventTmp.getObjectXml()):new Lice();
					break;

				case UriregConstants.CODE_ZNACHENIE_JOURNAL_STAJ:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Staj.class, currentEventTmp.getObjectXml()):new Staj();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Staj.class, previousEventTmp.getObjectXml()):new Staj();
					break;

				case UriregConstants.CODE_ZNACHENIE_JOURNAL_IZPIT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(Izpit.class, currentEventTmp.getObjectXml()):new Izpit();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(Izpit.class, previousEventTmp.getObjectXml()):new Izpit();
					break;

				case UriregConstants.CODE_ZNACHENIE_JOURNAL_IZPIT_RESULT:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(IzpitResult.class, currentEventTmp.getObjectXml()):new IzpitResult();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(IzpitResult.class, previousEventTmp.getObjectXml()):new IzpitResult();
					break;

				case UriregConstants.CODE_ZNACHENIE_JOURNAL_LICE_DOC:
					currentObj=currentEventTmp!=null?JAXBHelper.xmlToObject(LiceDoc.class, currentEventTmp.getObjectXml()):new LiceDoc();
					prevObj=previousEventTmp!=null?JAXBHelper.xmlToObject(LiceDoc.class, previousEventTmp.getObjectXml()):new LiceDoc();
					break;

				default:
                    LOGGER.error("Object code={} not implemented", currentEventTmp.getCodeObject());
					break;
			}

			 compareResult = new ObjectComparator(
					previousEventTmp!=null?previousEventTmp.getDateAction():new Date(),
					currentEventTmp!=null?currentEventTmp.getDateAction():new Date(),
							(SystemData) JSFUtils.getManagedBean("systemData"),
							null).compare( prevObj,currentObj);





		} catch (Exception e1) {
			LOGGER.error("",e1);

		}
		return compareResult;

	}





}
