package com.ib.urireg.beans;

import com.ib.urireg.db.dao.LockObjectDAO;
import com.ib.urireg.db.dao.ReferentDAO;
import com.ib.urireg.db.dto.Referent;
import com.ib.urireg.db.dto.ReferentAddress;
import com.ib.urireg.system.UriregClassifAdapter;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.UserData;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.indexui.utils.TreeUtils;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.ValidationUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.event.ActionEvent;
import jakarta.inject.Named;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named
@ViewScoped
public class AdmStruct extends IndexUIbean {


	/**
	 *
	 */
	private static final long serialVersionUID = 5870810804831852914L;

	private static final Logger LOGGER = LoggerFactory.getLogger(AdmStruct.class);

	private  TreeNode<SystemClassif>  rootNode;
	private transient ReferentDAO refDAO;

	private Referent referent;
	private Referent referentSluj;
	private LazyDataModelSQL2Array slujiteliList;
	private boolean viewTab = false;
	private TreeNode<SystemClassif>  selectedNode;
	private List<Object[]> selectedSluj = new ArrayList<>();
	private List<Object[]> selectedSlujAllM=new ArrayList<>(); //за запазване на всички маркирани
	private TabView tabView = new TabView();
	private boolean contract = false;

	//за модалния прозорец "Преместване на звено"
	private String radioPossition = "1"; //1-преди, 2-след,3- подчинен
	private boolean onlyChildren= false;
	private TreeNode<SystemClassif>  selNode; //ползвам и за преместване на служители
	private Integer registrParent;
	private String radioSaveRegistr = "1"; //1-запази, 2-прехвърли

	private boolean visibleRadioSaveRegistr = false;
	private Integer codePrev=null;
	private Integer codeParent=null;
	private Date decodeDate;

	//за модалния прозорец "Преместване на служители"
	//private Integer selectedZvenoEmpl;
	private String emplMoveNote = ""; // ползвам и за напускане на служители

	//за историята на звено/служител
	private LazyDataModelSQL2Array admHistoryList;
	private boolean isZveno = true;

	//за таб търсене
	private LazyDataModelSQL2Array searchList;
	private String searchName;

	private boolean fLockOk;
	private UserData ud;

	private int countryBG; // ще се инициализира в инита през системна настройка: delo.countryBG

	private boolean emplDeleteAllowed; // ще се допуска изтриване на служител само ако флага се вдигне

	//за избор от напуснали
	private List<SystemClassif> leftEmpl = new ArrayList<>();
	private Integer selectedEmpl;

	//за модалните прозорци за преместване и закриване на звено
	private String refInfoMoveZveno;
	private String refInfoCloseZveno;

	@PostConstruct
	void initData() {

		LOGGER.debug("PostConstruct!!!");

		try {
			this.countryBG = Integer.parseInt(getSystemData().getSettingsValue("delo.countryBG"));
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на код на държава България от настройка: delo.countryBG", e);
		}

		try {
			ud = getUserData(UserData.class);
			fLockOk = true;
			// проверка за заключен обект "Администртивна структура"
			// целта е да се осигури в даден момент само един потребител да може да отвори тази дейност
			fLockOk = checkForLock();
			if (fLockOk) {

				lockAdmStr(); //заключване на дейност "Адм. стр."

				decodeDate = new Date();
				refDAO = new ReferentDAO(getUserData());
				loadTree();
				referent = new Referent();
				referentSluj = new Referent();
				referent.setAddress(new ReferentAddress());

			}
			//	setSpecifics(Collections.singletonMap(SysClassifAdapter.EKATTE_INDEX_TIP,1));
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при работа!");
		}
	}

	public void actionSaveZveno() {
		if(checkZvenoData()) {
			return;
		}
		try {
			if(referent.getId()==null) {
				referent.getAddress().setAddrCountry(this.countryBG);
			}

			if (this.referent.getAddress() != null) {
				referent.getAddress().setAddrCountry(this.countryBG);
			}

			referent.setRefType(UriregConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO);
			JPA.getUtil().runInTransaction(() -> this.referent = 	refDAO.saveAdmReferent(referent, decodeDate, getSystemData()));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
			getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
			getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
			getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_USERS, false, false);
			loadTree();
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при запис на звено!");
		}
	}

	private void loadTree() {
		try {
			List<SystemClassif> listItems = refDAO.findZvenoList(decodeDate,getSystemData());
			this.rootNode = new TreeUtils().loadTreeData3(listItems, null, false, true, null, null);
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}

	}

	public void actionSaveSluj() {
		if(checkSlujData()) {
			return;
		}
		try {
			if(contract) {
				referentSluj.setEmplContract(SysConstants.CODE_ZNACHENIE_DA);
			}else {
				referentSluj.setEmplContract(SysConstants.CODE_ZNACHENIE_NE);
			}
			referentSluj.setCodeParent(referent.getCode());
			referentSluj.setRefGrj(this.countryBG);
			referentSluj.setRefType(UriregConstants.CODE_ZNACHENIE_REF_TYPE_EMPL);
			JPA.getUtil().runInTransaction(() -> this.referentSluj = 	refDAO.saveAdmReferent(referentSluj, decodeDate, getSystemData()));
			getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
			getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
			getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_USERS, false, false);

			String msg = this.refDAO.checkEmplDeleteAllowed(this.referentSluj, this.decodeDate, getSystemData());
			this.emplDeleteAllowed = msg == null;

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при запис на служител!");
		}
		searchEmplList();
	}

	@SuppressWarnings("unchecked")
	public void processSelection(NodeSelectEvent event) {

		try {
			referentSluj = new Referent();
			viewTab = true;
			selectedNode = event.getTreeNode();
			referent = refDAO.findByCode((selectedNode.getData()).getCode(),decodeDate,true);

			if(referent.getAddress()==null) {
				referent.setAddress(new ReferentAddress());
			}
			searchEmplList();
			selectedSluj = new ArrayList<>();
			selectedSlujAllM = new ArrayList<>();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.invalidParameter"));
		}finally {
			JPA.getUtil().closeConnection();
		}
		tabView.setActiveIndex(1);
	}

	@SuppressWarnings("unchecked")
	public void onContextMenu(NodeSelectEvent event) {

		selectedNode = event.getTreeNode();
	}

	private void searchEmplList() {
		SelectMetadata smd;
		try {
			smd = refDAO.createSelectEmployeeList(referent.getCode(), decodeDate, getSystemData());
			if (smd != null) {
				String defaultSortColumn = "A1,A2";
				this.slujiteliList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
			} else {
				this.slujiteliList = null;
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void actionSelectNew(ActionEvent event) {

		if(event!=null && selectedNode!=null) {

			referent = new Referent();
			referent.setAddress(new ReferentAddress());
			referentSluj = new Referent();
			slujiteliList = null;
			viewTab= true;
			tabView.setActiveIndex(0);

			String valChoiseNew =  event.getComponent().getId();
			SystemClassif nodeClassif = (SystemClassif)selectedNode.getData();
			Integer codeForRegistr=0; // за да сетнем по подразбиране регистратурата на родителя
			if("addBefore".equals(valChoiseNew)){
				referent.setCodeParent(nodeClassif.getCodeParent());
				referent.setCodePrev(nodeClassif.getCodePrev());
				codeForRegistr = nodeClassif.getCodeParent();
			} else if("addAfter".equals(valChoiseNew)){
				referent.setCodeParent(nodeClassif.getCodeParent());
				referent.setCodePrev(nodeClassif.getCode());
				codeForRegistr = nodeClassif.getCodeParent();
			} else if("addChild".equals(valChoiseNew)){
				referent.setCodeParent(nodeClassif.getCode());
				referent.setCodePrev(0);
				codeForRegistr = nodeClassif.getCode();
			}
			if(codeForRegistr!=null && !codeForRegistr.equals(0)) {

				try {
						SystemClassif	selectedZvenoClassif = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, codeForRegistr, getCurrentLang(), decodeDate, true);
						if(selectedZvenoClassif!=null) {
							referent.setRefRegistratura( (Integer) selectedZvenoClassif.getSpecifics()[UriregClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA]);
						}
					} catch (DbErrorException e) {
					LOGGER.error(e.getMessage(), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
				}
			}else	{ //ако звеното няма родител, слагаме текущата регисттратура
				referent.setRefRegistratura(getUserData(UserData.class).getRegistratura());
			}
		}
	}

	public void actionSearch() {
		if(searchName!=null && !"".equals(searchName)) {
			SelectMetadata smd;
			smd = refDAO.createSelectReferentSearch(searchName, decodeDate);
			String defaultSortColumn = "A1";
			this.searchList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
		}else {
			JSFUtils.addMessage("admStructForm:tabsAdmStruct:searchTxt", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "general.criteriiMs"));
		}
	}

	//при избор на резултат от търсенето
	public void selectForEdit(Object[] item) {
		viewTab = true;
		if(SearchUtils.asInteger(item[2]).equals(UriregConstants.CODE_ZNACHENIE_REF_TYPE_EMPL)) {
			actionEditSluj(SearchUtils.asInteger(item[0]));
			try {
				referent = refDAO.findByCode(SearchUtils.asInteger(item[3]),decodeDate,true);
				if(referent.getAddress()==null) {
					referent.setAddress(new ReferentAddress());
				}

			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} finally {
				JPA.getUtil().closeConnection();
			}
			tabView.setActiveIndex(1);
		}else {
			try {
				referent = refDAO.findByCode(SearchUtils.asInteger(item[0]),decodeDate,true);
				if(referent.getAddress()==null) {
					referent.setAddress(new ReferentAddress());
				}
				referentSluj = new Referent();
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} finally {
				JPA.getUtil().closeConnection();
			}
			tabView.setActiveIndex(0);
		}

		searchEmplList();
	}

	public void actionNewEmpl() {
		referentSluj =  new Referent();
		contract = false;
		this.emplDeleteAllowed = false;
	}

	private boolean checkZvenoData() {
		boolean save = false;

		if( referent.getRefName()==null || "".equals(referent.getRefName())) {
			JSFUtils.addMessage("admStructForm:tabsEditZvenaSluj:nameZveno", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "admStruct.nameZveno")));
			save = true;
		}

//		if( referent.getNflEik()==null || "".equals(referent.getNflEik())) {
//			JSFUtils.addMessage("admStructForm:tabsEditZvenaSluj:eik", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "admStruct.eik")));
//			save = true;
//		}

		if(referent.getNflEik()!=null && !"".equals(referent.getNflEik())&& !ValidationUtils.isValidBULSTAT(referent.getNflEik())) {
			JSFUtils.addMessage("admStructForm:tabsEditZvenaSluj:eik",FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "refCorr.msgValidEik"));
			save = true;
		}

		if( referent.getRefRegistratura()==null ) {
			JSFUtils.addMessage("admStructForm:tabsEditZvenaSluj:registratura", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "users.registratura")));
			save = true;
		}

		if(referent.getContactEmail()!=null && !"".equals(referent.getContactEmail())
				&& !ValidationUtils.isEmailValid(referent.getContactEmail())) {
			JSFUtils.addMessage("admStructForm:tabsEditZvenaSluj:emailZveno",FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, "general.validE-mail"));

			save = true;
		}
		return save;
	}

	private boolean checkSlujData() {
		boolean save = false;

		if( referentSluj.getRefName()==null || "".equals(referentSluj.getRefName())) {
			JSFUtils.addMessage("admStructForm:tabsEditZvenaSluj:nameSluj", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "admStruct.names")));
			save = true;
		}

		if(referentSluj.getFzlEgn()!=null && !"".equals(referentSluj.getFzlEgn())&& !ValidationUtils.isValidEGN(referentSluj.getFzlEgn())) {
			JSFUtils.addMessage("admStructForm:tabsEditZvenaSluj:egn",FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "refCorr.msgValidEgn"));
			save = true;
		}

		if(referentSluj.getContactEmail()!=null && !"".equals(referentSluj.getContactEmail())
				&& !ValidationUtils.isEmailValid(referentSluj.getContactEmail())) {
			JSFUtils.addMessage("admStructForm:tabsEditZvenaSluj:email",FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, "general.validE-mail"));

			save = true;
		}
		return save;
	}

	public void actionEditSluj(Integer code) {
		try {
			referentSluj = refDAO.findByCode(code ,decodeDate,true);
			if(referentSluj!=null) {
				if(referentSluj.getEmplContract()!=null && referentSluj.getEmplContract().intValue()==SysConstants.CODE_ZNACHENIE_DA) {
					contract=true;
				}else {
					contract = false;
				}
			}

			String msg = this.refDAO.checkEmplDeleteAllowed(this.referentSluj, this.decodeDate, getSystemData());
			this.emplDeleteAllowed = msg == null;

		} catch (BaseException e) {
			LOGGER.error("Грешка при търсене на служител! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void actionButtMoveZveno() {

		radioPossition = "1";
		//selectedZveno = null;
		onlyChildren= false;
	    radioSaveRegistr = "1"; //1-запази, 2-прехвърли
	    visibleRadioSaveRegistr = false;
	    selNode = null;
	    refInfoMoveZveno = referent.getRefInfo();
	}

	public void actionButtCloseZveno() {
	    refInfoCloseZveno = referent.getRefInfo();
	}

	//при смяна на ниво от радиобутона за местене на звена
	public void changeMoveZvenoRadio() {
		checkMoveZveno();
	}

	//при избор или смяна на звено,спрямо което ще се мести
	@SuppressWarnings("unchecked")
	public void changeRegMove(NodeSelectEvent event) {
			if(event!=null) {
				selNode = event.getTreeNode();
			}
		checkMoveZveno();
	}

	@SuppressWarnings("unchecked")
	public void changeEmplMove(NodeSelectEvent event) {
		if(event!=null) {
			selNode = event.getTreeNode();
		}
	}

	private void checkMoveZveno() {
		if(selNode!=null) {
			try {
				SystemClassif selectedZvenoClassif = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR,((SystemClassif) selNode.getData()).getCode(), getCurrentLang(), decodeDate, true);

//				Integer zvenoType = (Integer) selectedZvenoClassif.getSpecifics()[UriregClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE];
//				if(zvenoType != null && zvenoType.intValue() == UriregConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO) {
					if("1".equals(radioPossition)){
						codeParent = selectedZvenoClassif.getCodeParent();
						codePrev = selectedZvenoClassif.getCodePrev();
					} else if("2".equals(radioPossition)){
						codeParent = selectedZvenoClassif.getCodeParent();
						codePrev = selectedZvenoClassif.getCode();
					} else if("3".equals(radioPossition)){
						codeParent = selectedZvenoClassif.getCode();
						codePrev = 0;
					}
					if(selectedZvenoClassif!=null && selectedZvenoClassif.getCodePrev()!=0) {

				    	if("1".equals(radioPossition)|| "2".equals(radioPossition)){//при избрано "Преди" или "След" се проверява регистратурата на родителя на избраното звено
				    		selectedZvenoClassif = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, selectedZvenoClassif.getCodeParent(), getCurrentLang(), decodeDate, true);
				    	}
				    	registrParent = (Integer) selectedZvenoClassif.getSpecifics()[UriregClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA];

				    	if(!referent.getRefRegistratura().equals(registrParent)) {
				    		visibleRadioSaveRegistr = true;
				    	}
				    }

//				else {
//					JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "admStruct.selectZveno"));
//					selectedZveno = null;
//				}

			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
	}

	public void actionMoveZveno() {

		if(selNode==null) {
			JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "users.zveno")));
		    return;
		}
		try {
			referent.setRefInfo(refInfoMoveZveno);
			JPA.getUtil().runInTransaction(() -> this.referent = refDAO.moveAdmZveno(referent, codePrev, codeParent, decodeDate, !"1".equals(radioSaveRegistr), onlyChildren , getSystemData()));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "admStruct.successMove"));
			getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
			getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
			getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_USERS, false, false);
			loadTree();
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		PrimeFaces.current().executeScript("PF('dlgMoveZveno').hide();");

	}

	public void actionButtMoveEmpl() {
		selNode = null;
		//selectedZvenoEmpl = null;
		emplMoveNote = "";
		if(selectedSlujAllM==null || selectedSlujAllM.isEmpty()) {
			JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "admStruct.selectEmpl"));
			return;
		}

		PrimeFaces.current().executeScript("PF('dlgMoveEmpl').show();");
	}

	public void actionMoveSingleEmpl() {
		selNode = null;
		emplMoveNote = "";
		selectedSlujAllM = new ArrayList<>();
		Object[] id = new Object [1];
		id[0] = referentSluj.getCode();
		selectedSlujAllM.add(id);
	}

	public void actionMoveEmpl() {

		if(selNode==null) {
			JSFUtils.addMessage("admStructForm:tabsEditZvenaSluj:positionEmpl", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "users.zveno")));
		    return;
		}
		try {

			JPA.getUtil().runInTransaction(() -> refDAO.moveAdmEmployeeList(selectedSlujAllM, ((SystemClassif) selNode.getData()).getCode(), decodeDate,emplMoveNote, getSystemData()));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "admStruct.successMove"));
			getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
			getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_USERS, false, false);
			getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
			searchEmplList();
			loadTree();
		}
		catch (ObjectInUseException | InvalidParameterException e) {
			LOGGER.error(e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		selectedSluj = new ArrayList<>();
		selectedSlujAllM = new ArrayList<>();
		referentSluj = new Referent();
		PrimeFaces.current().executeScript("PF('dlgMoveEmpl').hide();");

	}

	public void actionCloseZveno() {
		try {
			referent.setRefInfo(refInfoCloseZveno);
			JPA.getUtil().runInTransaction(() -> refDAO.closeAdmReferent(referent, decodeDate, getSystemData(), false));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "admStruct.successCloseZveno"));
			getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
		    getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_USERS, false, false);
		    getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
			loadTree();
		    referent = new Referent();
			referentSluj = new Referent();
			referent.setAddress(new ReferentAddress());
			viewTab = false;
		} catch (ObjectInUseException e) {
			LOGGER.error(e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		PrimeFaces.current().executeScript("PF('dlgCloseZveno').hide();");

	}

	private boolean actionDelete (Referent ref) {
		boolean success = false;
		try {

			JPA.getUtil().runInTransaction(() -> this.refDAO.deleteAdmReferent(ref,decodeDate, getSystemData()));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			success = true;
		} catch (ObjectInUseException e) {
			LOGGER.error(e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

		} catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на служител или звено! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

		}
		return success;
	}

	public void actionDeleteZveno() {
		boolean success = actionDelete(referent);
		if(success) {
			referent = new Referent();
			referentSluj = new Referent();
			referent.setAddress(new ReferentAddress());
			viewTab = false;
			try {
				getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
				getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_USERS, false, false);
				getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
				loadTree();
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
	}

	public void actionDeleteEmpl() {
		boolean success = actionDelete(referentSluj);
		if(success) {
			referentSluj = new Referent();
			try {
				getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
			    getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_USERS, false, false);
			    getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
				searchEmplList();
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}


		}
	}

	public void actionButtLeaveEmpl() {

		emplMoveNote = "";
		if(selectedSlujAllM==null || selectedSlujAllM.isEmpty()) {
			JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "admStruct.selectEmplLeave"));
			return;
		}

		PrimeFaces.current().executeScript("PF('dlgLeaveEmpl').show();");
	}

	public void actionLeaveSingleEmpl() {

		Object [] id = new Object [1];
		id[0] = referentSluj.getCode();
		selectedSlujAllM = new ArrayList<>();
		selectedSlujAllM.add(id);
	}

	public void actionLeaveEmpl() {
		try {
			JPA.getUtil().runInTransaction(() -> refDAO.closeAdmEmployeeList(selectedSlujAllM, decodeDate,emplMoveNote, getSystemData()));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "admStruct.leaveEmpl"));
			getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
			getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_USERS, false, false);
			getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
			loadTree();
		} catch (ObjectInUseException e) {
			LOGGER.error(e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		searchEmplList();
		selectedSlujAllM = new ArrayList<>();
		referentSluj = new Referent();
		PrimeFaces.current().executeScript("PF('dlgLeaveEmpl').hide();");

	}

	private void searchHistory(Integer code,Integer type) {
		SelectMetadata smd = refDAO.createSelectAdmHistory(code, type);
		String defaultSortColumn = "A6";
		this.admHistoryList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
	}

	public void searchHistoryZveno() {
		searchHistory(referent.getCode(), UriregConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO);
		isZveno = true;
	}

	public void searchHistoryEmpl() {
		searchHistory(referentSluj.getCode(), UriregConstants.CODE_ZNACHENIE_REF_TYPE_EMPL);
		isZveno = false;
	}



	/**
	 * Проверка за заключена дейност "Актуализация на ад. стр."
	 */
	private boolean checkForLock() {
		boolean res = true;
		LockObjectDAO daoL = new LockObjectDAO();
		try {
			Object[] obj = daoL.check(ud.getUserId(), UriregConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT, UriregConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT);
			if (obj != null) {
				 res = false;

				 String msg = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_ADMIN_STR, Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date())
						       + " / " + DateUtils.printDate((Date)obj[1]);
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(LABELS, "docu.admStrLocked"), msg);
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при проверка за заключена адм. стр.! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		return res;
	}

	/**
	 * Заключване на дейност "актуализация на адм. структура", като преди това отключва всички обекти, заключени от потребителя
	 */
	public void lockAdmStr() {
		LOGGER.info("lockAdmStr!"  + ud.getPreviousPage() );
		LockObjectDAO daoL = new LockObjectDAO();
		try {
			JPA.getUtil().runInTransaction(() -> {
				daoL.lock(ud.getUserId(), UriregConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT, UriregConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT, null);
			});
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при заключване на адм. стр.! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} catch (BaseException e) {
			LOGGER.error("Грешка при заключване на  адм. стр.! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}


	/**
	 * при излизане от страницата - отключва обекта и да го освобождава за актуализация от друг потребител
	 */
	@PreDestroy
	public void unlockDoc(){
        if (!ud.isReloadPage()) {
            LOGGER.info("unlockData 0!{}", ud.getPreviousPage());
    		LockObjectDAO daoL = new LockObjectDAO();
    		try {

				JPA.getUtil().runInTransaction(() -> {
					daoL.unlock(ud.getUserId());
				});

    		} catch (DbErrorException e) {
    			LOGGER.error("Грешка при отключване на адм. стр.! ", e);
    			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
    		} catch (BaseException e) {
    			LOGGER.error("Грешка при отключване на адм. стр.! ", e);
    			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
    		}
        	ud.setPreviousPage(null);
        }
        ud.setReloadPage(false);
	}

	/**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
     */
    public void   onPageUpdateSelected(){
    	if (selectedSlujAllM != null && !selectedSlujAllM.isEmpty()) {
    		selectedSluj.clear();
    		selectedSluj.addAll(selectedSlujAllM);
    	}
        LOGGER.debug(" onPageUpdateSelected->>{}", selectedSluj.size());
    }

    /*
	 * Множествен избор на служители
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	  public void onRowSelectAll(ToggleSelectEvent event) {
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(selectedSlujAllM);
    	if(event.isSelected()) {
    		for (Object[] obj : selectedSluj) {
    			if(obj != null && obj.length > 0) {
	    			boolean bb = true;
	    			Long l2 = Long.valueOf(obj[0].toString());
	    			for (Object[] j : tmpL) {
	        			Long l1 = Long.valueOf(j[0].toString());
	    	    		if(l1.equals(l2)) {
	    	    			bb = false;
	    	    			break;
	    	    		}
	        		}
	    			if(bb) {
	    				tmpL.add(obj);
	    			}
    			}
    		}
    	}else {
	    	List<Object[]> tmpLPageC =  getSlujiteliList().getResult();// rows from current page....
			for (Object[] obj : tmpLPageC) {
				if(obj != null && obj.length > 0) {
					Long l2 = Long.valueOf(obj[0].toString());
					for (Object[] j : tmpL) {
		    			Long l1 = Long.valueOf(j[0].toString());
			    		if(l1.equals(l2)) {
			    			tmpL.remove(j);
			    			break;
			    		}
		    		}
				}
			}
		}
		setSelectedSlujAllM(tmpL);
		LOGGER.debug("onToggleSelect->>");
	}

	  /**
	     * Select one row
	     * @param event
	     */
	    @SuppressWarnings("rawtypes")
		public void onRowSelect(SelectEvent event) {
	    	if(event!=null  && event.getObject()!=null) {
	    		List<Object[]> tmpList =  selectedSlujAllM;

	    		Object[] obj = (Object[]) event.getObject();
	    		if(obj != null && obj.length > 0) {
		    		boolean bb = true;
		    		Integer l2 = Integer.valueOf(obj[0].toString());
					for (Object[] j : tmpList) {
						Integer l1 = Integer.valueOf(j[0].toString());
			    		if(l1.equals(l2)) {
			    			bb = false;
			    			break;
			    		}
			   		}
					if(bb) {
						tmpList.add(obj);
						setSelectedSlujAllM(tmpList);
					}
	    		}
	    	}
            LOGGER.debug("1 onRowSelectIil->>{}", selectedSlujAllM.size());
	}

	    /**
	     * unselect one row
	     * @param event
	     */
	    @SuppressWarnings("rawtypes")
		public void onRowUnselect(UnselectEvent event) {
	    	if(event!=null  && event.getObject()!=null) {
	    		Object[] obj = (Object[]) event.getObject();
	    		List<Object[] > tmpL = new ArrayList<>();
	    		tmpL.addAll(selectedSlujAllM);
	    		for (Object[] j : tmpL) {
	    			if(j != null && j.length > 0 && obj != null && obj.length > 0) {
		    			Integer l1 = Integer.valueOf(j[0].toString());
		    			Integer l2 = Integer.valueOf(obj[0].toString());
			    		if(l1.equals(l2)) {
			    			tmpL.remove(j);
			    			setSelectedSlujAllM(tmpL);
			    			break;
			    		}
	    			}
	    		}
                LOGGER.debug("onRowUnselectIil->>{}", selectedSlujAllM.size());
	    	}
	 }

	public void actionLoadLeft() {
		try {
			leftEmpl = getSystemData().getChildrenOnNextLevel(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, -1000, decodeDate, getCurrentLang());
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на напуснали служители! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}

	public void actionSaveLeftEmpl() {

			if(selectedEmpl==null) {
				JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "refDeleg.employee")));
			    return;
			}

			try {

				SystemClassif classifEmpl = getSystemData().decodeItemLite(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, selectedEmpl, getCurrentLang(), decodeDate, false);

				JPA.getUtil().runInTransaction(() -> this.referentSluj = 	refDAO.returnLeftEmployee(classifEmpl, referent.getCode(), decodeDate, getSystemData(), false));
				getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
				getSystemData().reloadClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
				getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_USERS, false, false);

				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
			} catch (InvalidParameterException | ObjectInUseException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
			catch (BaseException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при запис на служител!");
			}
			searchEmplList();
			selectedEmpl = null;
			PrimeFaces.current().executeScript("PF('dlgSelectLeftEmpl').hide();");

	}



	public TreeNode<SystemClassif> getRootNode() {
		return rootNode;
	}

	public void setRootNode(TreeNode<SystemClassif> rootNode) {
		this.rootNode = rootNode;
	}

	public Referent getReferent() {
		return referent;
	}

	public void setReferent(Referent referent) {
		this.referent = referent;
	}

	public boolean isViewTab() {
		return viewTab;
	}

	public void setViewTab(boolean viewTab) {
		this.viewTab = viewTab;
	}

	public Referent getReferentSluj() {
		return referentSluj;
	}

	public void setReferentSluj(Referent referentSluj) {
		this.referentSluj = referentSluj;
	}

	public LazyDataModelSQL2Array getSlujiteliList() {
		return slujiteliList;
	}

	public List<Object[]> getSelectedSluj() {
		return selectedSluj;
	}

	public void setSelectedSluj(List<Object[]> selectedSluj) {
		this.selectedSluj = selectedSluj;
	}

	public String getRadioPossition() {
		return radioPossition;
	}

	public void setRadioPossition(String radioPossition) {
		this.radioPossition = radioPossition;
	}

	public boolean isOnlyChildren() {
		return onlyChildren;
	}

	public void setOnlyChildren(boolean onlyChildren) {
		this.onlyChildren = onlyChildren;
	}

	public String getRadioSaveRegistr() {
		return radioSaveRegistr;
	}

	public void setRadioSaveRegistr(String radioSaveRegistr) {
		this.radioSaveRegistr = radioSaveRegistr;
	}


	public boolean isVisibleRadioSaveRegistr() {
		return visibleRadioSaveRegistr;
	}

	public void setVisibleRadioSaveRegistr(boolean visibleRadioSaveRegistr) {
		this.visibleRadioSaveRegistr = visibleRadioSaveRegistr;
	}

	public Integer getRegistrParent() {
		return registrParent;
	}

	public void setRegistrParent(Integer registrParent) {
		this.registrParent = registrParent;
	}

	public TabView getTabView() {
		return tabView;
	}

	public void setTabView(TabView tabView) {
		this.tabView = tabView;
	}

	public String getEmplMoveNote() {
		return emplMoveNote;
	}

	public void setEmplMoveNote(String emplMoveNote) {
		this.emplMoveNote = emplMoveNote;
	}

	public LazyDataModelSQL2Array getAdmHistoryList() {
		return admHistoryList;
	}

	public boolean isZveno() {
		return isZveno;
	}

	public void setZveno(boolean isZveno) {
		this.isZveno = isZveno;
	}

	public boolean isContract() {
		return contract;
	}

	public void setContract(boolean contract) {
		this.contract = contract;
	}

	public TreeNode<SystemClassif> getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TreeNode<SystemClassif> selectedNode) {
		this.selectedNode = selectedNode;
	}

	public Date getDecodeDate() {
		return decodeDate;
	}

	public LazyDataModelSQL2Array getSearchList() {
		return searchList;
	}

	public String getSearchName() {
		return searchName;
	}

	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}


	public boolean isfLockOk() {
		return fLockOk;
	}

	public void setfLockOk(boolean fLockOk) {
		this.fLockOk = fLockOk;
	}


	public UserData getUd() {
		return ud;
	}


	public void setUd(UserData ud) {
		this.ud = ud;
	}

	public List<Object[]> getSelectedSlujAllM() {
		return selectedSlujAllM;
	}

	public void setSelectedSlujAllM(List<Object[]> selectedSlujAllM) {
		this.selectedSlujAllM = selectedSlujAllM;
	}

	public boolean isEmplDeleteAllowed() {
		return this.emplDeleteAllowed;
	}

	public List<SystemClassif> getLeftEmpl() {
		return leftEmpl;
	}

	public void setLeftEmpl(List<SystemClassif> leftEmpl) {
		this.leftEmpl = leftEmpl;
	}

	public Integer getSelectedEmpl() {
		return selectedEmpl;
	}

	public void setSelectedEmpl(Integer selectedEmpl) {
		this.selectedEmpl = selectedEmpl;
	}

	public String getRefInfoMoveZveno() {
		return refInfoMoveZveno;
	}

	public void setRefInfoMoveZveno(String refInfoMoveZveno) {
		this.refInfoMoveZveno = refInfoMoveZveno;
	}

	public String getRefInfoCloseZveno() {
		return refInfoCloseZveno;
	}

	public void setRefInfoCloseZveno(String refInfoCloseZveno) {
		this.refInfoCloseZveno = refInfoCloseZveno;
	}
}
