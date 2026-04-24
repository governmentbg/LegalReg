package com.ib.urireg.components;

import com.ib.urireg.system.UriregClassifAdapter;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.UserData;
import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.JSFUtils;
import com.ib.indexui.utils.TreeUtils;
import com.ib.system.BaseSystemData;
import com.ib.system.BaseUserData;
import com.ib.system.SysConstants;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SysClassifUtils;
import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.context.FacesContext;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ITEM_CHOICE;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;

/** */
@FacesComponent(value = "compAccess", createTag = true)
public class CompAccess extends UINamingContainer {

	private enum PropertyKeys {
		root, rootAdm, rootGroup, selectedNode, tempClassifs, searchWord, showMe, selectedType,
		mapAdmSelected, mapGroupSelected, mapAdmRemoved, mapGroupRemoved, listDocAccess, listDeloAccesss, typeDocDelo, treeType,
		onlyZvenaRegistratura, zvenaMap
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CompAccess.class);

	private static String[] lblRadioBtnDefault = { "избрано", "избрано и подчинени", "подчинени" };
	static { // зареждам ги от класификацията. ако нещо избие ще останат горните имена по дефолт
		try {
			BaseSystemData sd = (BaseSystemData) JSFUtils.getManagedBean("systemData");
			List<SystemClassif> list = sd.getSysClassification(CODE_CLASSIF_ITEM_CHOICE, new Date(), CODE_DEFAULT_LANG);
			SysClassifUtils.doSortClassifPrev(list); // важна е сортировката

			lblRadioBtnDefault = new String[] { list.get(0).getTekst(), list.get(1).getTekst(), list.get(2).getTekst() };
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private BaseSystemData	systemData	= null;
	private BaseUserData	userData	= null;
	private Date			dateClassif	= null;

	public  static final  String READONLYCODES = "readOnlyCodes";
	/**
	 * preRenderComponent
	 */
	public void initAutoCompl() throws DbErrorException {

		setTempClassifs(new ArrayList<>());
		setMapAdmSelected(new HashMap<>());
		setMapGroupSelected(new HashMap<>());
		setMapAdmRemoved(new HashMap<>());
		setMapGroupRemoved(new HashMap<>());

		Map<Integer, Object> specs = new HashMap<>();
        specs.put(UriregClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, UriregConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO);
        specs.put(UriregClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA, ((UserData)getUserData()).getRegistratura()); // текущата регистратура на логнатия

        //Проверката за звената не се използва! Чекбокса в xhtml- a е коментиран!! Засега тук няма да пипам кода....
        List<SystemClassif> zvena = getSystemData().queryClassification(UriregConstants.CODE_CLASSIF_ADMIN_STR, null, dateClassif, getUserData().getCurrentLang(), specs);

        Map<String, Boolean> zvMap=new HashMap<>();

        for (int i = 0; i < zvena.size(); i++) {
        	zvMap.put(String.valueOf(zvena.get(i).getCode()), true);
		}
        setZvenaMap(zvMap);
        //----------------------

//        if (getTypeDocDelo()!=null) {
//
//
//			// po nqkva pri4ina kogato se polzva tuk  tempClassifsAdd(sc); - dublira zapisa sc za tova slagam cql spisak v metoda onNodeSelect raboti normalno????
//			List<SystemClassif> tmp=new ArrayList<>();
//			if (getTypeDocDelo()==1) {
//				//delo
//				for (int i = 0; i < getListDeloAccess().size(); i++) {
//					if (getListDeloAccess().get(i).getFlag()!=null && getListDeloAccess().get(i).getFlag()==SysConstants.CODE_DEIN_IZTRIVANE) {
////						if (getListDeloAccess().get(i).getCodeClassif().equals(UriregConstants.CODE_CLASSIF_GROUP_EMPL)) {
////							mapGroupRemovedPut(getListDeloAccess().get(i).getCodeRef().toString(), getListDeloAccess().get(i).getId());
////						}else {
//							mapAdmRemovedPut(getListDeloAccess().get(i).getCodeRef().toString(), getListDeloAccess().get(i).getId());
////						}
//					}else {
//						SystemClassif sc=new SystemClassif();
//						sc.setCode(getListDeloAccess().get(i).getCodeRef());
////						sc.setCodeClassif(getListDeloAccess().get(i).getCodeClassif());
////						sc.setTekst(getSystemData().decodeItem(getListDeloAccess().get(i).getCodeClassif(), getListDeloAccess().get(i).getCodeRef(), getLang(), getDateClassif()));
//						sc.setTekst(getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, getListDeloAccess().get(i).getCodeRef(), getLang(), getDateClassif()));
//						sc.setId(getListDeloAccess().get(i).getId());
//						sc.setPored(getListDeloAccess().get(i).getExclude()); // ако има стойност 1 - значи достъпа е изрично забранен - да не се позволява изтриване!
//						tmp.add(sc);
//
////						if (getListDeloAccess().get(i).getCodeClassif().equals(UriregConstants.CODE_CLASSIF_GROUP_EMPL)) {
////							mapGroupSelectedPut(getListDeloAccess().get(i).getCodeRef().toString(), getListDeloAccess().get(i).getId());
////						}else {
//							mapAdmSelectedPut(getListDeloAccess().get(i).getCodeRef().toString(), getListDeloAccess().get(i).getId());
////						}
//					}
//				}
//			}else {
//				//doc
//				for (int i = 0; i < getListDocAccess().size(); i++) {
//					if (getListDocAccess().get(i).getFlag()!=null && getListDocAccess().get(i).getFlag()==SysConstants.CODE_DEIN_IZTRIVANE) {
////						if (getListDocAccess().get(i).getCodeClassif().equals(UriregConstants.CODE_CLASSIF_GROUP_EMPL)) {
////							mapGroupRemovedPut(getListDocAccess().get(i).getCodeRef().toString(), getListDocAccess().get(i).getId());
////						}else {
//							mapAdmRemovedPut(getListDocAccess().get(i).getCodeRef().toString(), getListDocAccess().get(i).getId());
////						}
//					}else {
//						SystemClassif sc=new SystemClassif();
//						sc.setCode(getListDocAccess().get(i).getCodeRef());
////						sc.setCodeClassif(getListDocAccess().get(i).getCodeClassif());
////						sc.setTekst(getSystemData().decodeItem(getListDocAccess().get(i).getCodeClassif(), getListDocAccess().get(i).getCodeRef(), getLang(), getDateClassif()));
//						sc.setTekst(getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, getListDocAccess().get(i).getCodeRef(), getLang(), getDateClassif()));
//						sc.setId(getListDocAccess().get(i).getId());
//						sc.setPored(getListDocAccess().get(i).getExclude()); // ако има стойност 1 - значи достъпа е изрично забранен - да не се позволява изтриване!
//						tmp.add(sc);
//
////						if (getListDocAccess().get(i).getCodeClassif().equals(UriregConstants.CODE_CLASSIF_GROUP_EMPL)) {
////							mapGroupSelectedPut(getListDocAccess().get(i).getCodeRef().toString(), getListDocAccess().get(i).getId());
////						}else {
//							mapAdmSelectedPut(getListDocAccess().get(i).getCodeRef().toString(), getListDocAccess().get(i).getId());
////						}
//					}
//				}
//			}
//
//
//			setTempClassifs(tmp);
//
//
//			loadRoot();
//		}
	}



	/**
	 * При натискане на бутон - "Потвърждение" - модалния за избор от дървото
	 *
	 */
	public void actionConfirm() {


		ELContext ctx = getFacesContext().getELContext();

//		if(isReorder()) {
//		  List<SystemClassif> notOrdLst = getTempClassifs();
//		  List<SystemClassif> ordLst = new ArrayList<SystemClassif>();
//			for (Integer code : tmpArr) {
//				for (SystemClassif sc : notOrdLst) {
//					if(code.equals(Integer.valueOf(sc.getCode()))) {
//						ordLst.add(sc);
//						notOrdLst.remove(sc);
//						break;
//					}
//				}
//			}
//			setTempClassifs(ordLst);
//			setReorder(false);
//		}

//		if (getTypeDocDelo()==1) {
//			//Delo
//			// връща списък с значения като DeloAccess
//			ValueExpression expr = getValueExpression("listDeloAccesss");
//			List<DeloAccess> da=new ArrayList<>();
//
//			for (int i = 0; i < getTempClassifs().size(); i++) {
//				DeloAccess d=new DeloAccess();
////				d.setCodeClassif(getTempClassifs().get(i).getCodeClassif());
//				d.setCodeRef(getTempClassifs().get(i).getCode());
//				d.setId(getTempClassifs().get(i).getId());
//				d.setFlag(null);
//				if (getTempClassifs().get(i).getPored()!=null) {
//					d.setExclude(getTempClassifs().get(i).getPored());
//				}
//
//				da.add(d);
//			}
//			for (String key : getMapAdmRemoved().keySet()) {
//				DeloAccess d=new DeloAccess();
////				d.setCodeClassif(Constants.CODE_CLASSIF_ADMIN_STR);
//				d.setCodeRef(Integer.valueOf(key));
//				d.setId(getMapAdmRemoved().get(key));
//				d.setFlag(SysConstants.CODE_DEIN_IZTRIVANE);
//				da.add(d);
//			}
////			for (String key : getMapGroupRemoved().keySet()) {
////				DeloAccess d=new DeloAccess();
//////				d.setCodeClassif(UriregConstants.CODE_CLASSIF_GROUP_EMPL);
////				d.setCodeRef(Integer.valueOf(key));
////				d.setId(getMapGroupRemoved().get(key));
////				d.setFlag(UriregConstants.CODE_DEIN_IZTRIVANE);
////				da.add(d);
////			}
//
//
//			if (expr != null) {
//				expr.setValue(ctx, da);
//			}
//
//		}else{
//			//doc
//			// връща списък с значения като DeloAccess
//			ValueExpression expr = getValueExpression("listDocAccess");
//			List<DocAccess> da=new ArrayList<>();
//
//			for (int i = 0; i < getTempClassifs().size(); i++) {
//				DocAccess d=new DocAccess();
////				d.setCodeClassif(getTempClassifs().get(i).getCodeClassif());
//				d.setCodeRef(getTempClassifs().get(i).getCode());
//				d.setId(getTempClassifs().get(i).getId());
//				d.setFlag(null);
//				if (getTempClassifs().get(i).getPored()!=null) {
//					d.setExclude(getTempClassifs().get(i).getPored());
//				}
//				da.add(d);
//			}
//			for (String key : getMapAdmRemoved().keySet()) {
//				DocAccess d=new DocAccess();
////				d.setCodeClassif(Constants.CODE_CLASSIF_ADMIN_STR);
//				d.setCodeRef(Integer.valueOf(key));
//				d.setId(getMapAdmRemoved().get(key));
//				d.setFlag(SysConstants.CODE_DEIN_IZTRIVANE);
//				da.add(d);
//			}
////			for (String key : getMapGroupRemoved().keySet()) {
////				DocAccess d=new DocAccess();
////				d.setCodeClassif(UriregConstants.CODE_CLASSIF_GROUP_EMPL);
////				d.setCodeRef(Integer.valueOf(key));
////				d.setId(getMapGroupRemoved().get(key));
////				d.setFlag(UriregConstants.CODE_DEIN_IZTRIVANE);
////				da.add(d);
////			}
//
//
//			if (expr != null) {
//				expr.setValue(ctx, da);
//			}
//
//		}
//
//		// извиква remoteCommnad - ако има такава....
//		String remoteCommnad = (String) getAttributes().get("onComplete");
//		if (remoteCommnad != null && !remoteCommnad.equals("")) {
//			PrimeFaces.current().executeScript(remoteCommnad);
//		}
	 }







	/**
	 * Зачиства дървото
	 *
	 * @throws DbErrorException
	 */
	public void clear() throws DbErrorException {
		setSearchWord("");
		setRoot(null);
		loadRoot();
	}

	/**
	 * Зачиства таблицата с избрани значения
	 */
	public void clearCodesTable() {
		setMapAdmSelected(new HashMap<>());
		setMapGroupSelected(new HashMap<>());
		SystemClassif sc;
		List<SystemClassif> tmpList = new ArrayList<>();
		boolean excludeA = false;
		for (int i = 0; i < getTempClassifs().size(); i++) {
            // да НЕ изтрива лицата с "Отнет достъп"!
		    sc = getTempClassifs().get(i);
		    excludeA = Integer.valueOf(Constants.CODE_ZNACHENIE_DA).equals(sc.getPored());
			if (sc.getId()!=null && !excludeA) {
				mapAdmRemovedPut(String.valueOf(getTempClassifs().get(i).getCode()), getTempClassifs().get(i).getId());
			}else if(sc.getId()!=null && excludeA) {
				tmpList.add(sc);//да останат само лица с изрично отнет достъп
			}
		}
		//setTempClassifs(new ArrayList<>());
		setTempClassifs(tmpList);
	}

	/**
	 * Зачиства полетата, в които се връща резултата
	 */
	public void clearInput() {
		ValueExpression expr = getValueExpression("selectedCodes");
		ValueExpression expr2 = getValueExpression("selectedText");
		ValueExpression expr3 = getValueExpression("selectedClassifs");

		ELContext ctx = getFacesContext().getELContext();
		if (expr != null) {
			expr.setValue(ctx, new ArrayList<Integer>());
		}
		if (expr2 != null) {
			expr2.setValue(ctx, null);
		}
		if (expr3 != null) {
			expr3.setValue(ctx, new ArrayList<>());
		}
		clearCodesTable();
	}



	/** @return */
	public Date getCurrentDate() {
		return getDateClassif();
	}

	/** @return */
	private Boolean getIsSelectNode() {
		Boolean bb = true;
		if( getAttributes().get("isSelectNode") != null) {
			bb = (Boolean) getAttributes().get("isSelectNode");
		}
		return bb; // true - да позволи избор на node от дървото; false - избор само на листа...
	}

	/** @return */
	public Integer getLang() {
		return getUserData().getCurrentLang();
	}

	/** @return */
	public String[] getLblRadioBtnDefault() {
		return lblRadioBtnDefault;
	}

	/** @return */
	public TreeNode getRoot() {
		return (TreeNode) getStateHelper().eval(PropertyKeys.root, null);
	}


	/** @return */
	public String getSearchWord() {
		return (String) getStateHelper().eval(PropertyKeys.searchWord, "");
	}


	/** @return */
	public TreeNode getSelectedNode() {
		return (TreeNode) getStateHelper().eval(PropertyKeys.selectedNode, null);
	}

	/** @return */
	public String getSelectedType() {
		return (String) getStateHelper().eval(PropertyKeys.selectedType, "0");
	}

	/** @return */
	@SuppressWarnings("unchecked")
	public List<SystemClassif> getTempClassifs() {
		List<SystemClassif> eval = (List<SystemClassif>) getStateHelper().eval(PropertyKeys.tempClassifs, null);
		return eval != null ? eval : new ArrayList<>();
	}



	/** @return */
	public boolean isShowMe() {
		return (Boolean) getStateHelper().eval(PropertyKeys.showMe, false);
	}

	/**
	 * Зарежда дървото
	 *
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public void loadRoot() throws DbErrorException {




		setSelectedType("0");
		setSearchWord("");



		Boolean saveStateTree = false;
		if( getAttributes().get("saveStateTree") != null) {
			saveStateTree = (Boolean) getAttributes().get("saveStateTree");
		}

		setShowMe(true);

		if (getTreeType()==1) {
			//adm struct
			if (getRootAdm() == null || !saveStateTree) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Loading tree with classifCode:{}", Constants.CODE_CLASSIF_ADMIN_STR);
				}
				try {

					Boolean sortByName = false;
					if( getAttributes().get("sortByName") != null) {
						sortByName = (Boolean) getAttributes().get("sortByName");
					}

					List<SystemClassif> classifList = loadClassifList(Constants.CODE_CLASSIF_ADMIN_STR, Boolean.FALSE.equals(sortByName), null);

					Boolean expanded = false;
					if(getAttributes().get("expanded") != null) {
						expanded = (Boolean) getAttributes().get("expanded");
					}
					List<Integer> readOnlyCodes = (List<Integer>) getAttributes().get(READONLYCODES);

					TreeNode rootNode = new TreeUtils().loadTreeData3(classifList, "", sortByName, expanded, readOnlyCodes, null);
					setRootAdm(rootNode);


				} catch (Exception e) {
					throw new DbErrorException(e);
				}

			}
			setRoot(getRootAdm());
		}else {
			if (getTreeType()==2) {
				if (getRootGroup() == null || !saveStateTree) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Loading tree with classifCode:{}", UriregConstants.CODE_CLASSIF_GROUP_EMPL);
					}
					try {

						// групите да са филтрирани в рамките на регистртурата
						// Дървото задължително трябва да е по азбучен ред, иначе не може да се изгради!!!
						Map<Integer, Object> specRegistratura = Collections.singletonMap(UriregClassifAdapter.REG_GROUP_INDEX_REGISTRATURA, ((UserData) getUserData()).getRegistratura());
						//List<SystemClassif> classifList = loadClassifList(UriregConstants.CODE_CLASSIF_GROUP_EMPL, Boolean.FALSE.equals(sortByName), null);
						List<SystemClassif> classifList = getSystemData().queryClassification(UriregConstants.CODE_CLASSIF_GROUP_EMPL, null, getDateClassif(), getLang(), specRegistratura);
						TreeNode rootNode = new TreeUtils().loadTreeData3(classifList, "", true, true, null, null);

						setRootGroup(rootNode);

					} catch (Exception e) {
						throw new DbErrorException(e);
					}

				}
				setRoot(getRootGroup());
			}
		}

	}


	/**
	 * Зарежда избраната класификация и я предава към value на bean Избор от дървото
	 *
	 * @param event
	 * @throws DbErrorException
	 */
	public void onNodeSelect(NodeSelectEvent event) throws DbErrorException {
		try {
		Boolean showRadioBtn = false;
		if(getAttributes().get("showRadioBtn") != null) {
			showRadioBtn = (Boolean) getAttributes().get("showRadioBtn");
		}

		int selV = Integer.parseInt(getSelectedType());

		boolean bb = true;
		if (!(event.getTreeNode().isLeaf() || getIsSelectNode())) { // позволено е да се избира всичко
			if (!(showRadioBtn && selV == 2)) { // ако е забранено избирането на node, но е избран радиобутон "Подчинените"
				bb = false;
			}
			// не позволява да се избере node от дървото
			event.getTreeNode().setExpanded(!event.getTreeNode().isExpanded());
			event.getTreeNode().setSelected(false);
		}

		if (bb) {

			SystemClassif selectedItem = (SystemClassif) event.getTreeNode().getData();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Selected node from tree: {}", selectedItem.getTekst());
			}

			if (selV == 0) { // само избраното - by default

				if (selectedItem.getCodeClassif()==Constants.CODE_CLASSIF_ADMIN_STR) {
					//admStuct
					if (!getMapAdmSelected().containsKey(String.valueOf(selectedItem.getCode()))) {
						boolean go=true;
						boolean b = getOnlyZvenaRegistratura();
						if (b && !getZvenaMap().containsKey(String.valueOf(selectedItem.getCode()))) {
							go=false;
						}
						if (go) {
							//ako e izbrano samo zvenata gledame dali e tam i posle si minava po stariq na4in


							if (getMapAdmRemoved().containsKey(String.valueOf(selectedItem.getCode()))) {
								mapAdmSelectedPut(String.valueOf(selectedItem.getCode()), getMapAdmRemoved().get(String.valueOf(selectedItem.getCode())));

								selectedItem.setId(getMapAdmRemoved().get(String.valueOf(selectedItem.getCode())));
								mapAdmRemovedRemove(String.valueOf(selectedItem.getCode()));
							}else {
								mapAdmSelectedPut(String.valueOf(selectedItem.getCode()), null);
							}
							tempClassifsAdd(selectedItem);
						}
					}
				}else {
					//groupSluj - trqbva da razviq...


						String emplCodes = (String) getSystemData().getItemSpecific(UriregConstants.CODE_CLASSIF_GROUP_EMPL, selectedItem.getCode(), SysConstants.CODE_DEFAULT_LANG, dateClassif, UriregClassifAdapter.REG_GROUP_INDEX_MEMBERS);

						String[] s=emplCodes.split(",");
						for (int i = 0; i < s.length; i++) {
							SystemClassif sc= getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, Integer.valueOf(s[i]), getLang(), dateClassif, false);
							// служителя трябва да е активен в админ.стр. към датата която се иска
							if (sc != null && !getMapAdmSelected().containsKey(s[i])) {
								if (getMapAdmRemoved().containsKey(s[i])) {
									mapAdmSelectedPut(s[i], getMapAdmRemoved().get(s[i]));
									sc.setId(getMapAdmRemoved().get(s[i]));
									mapAdmRemovedRemove(s[i]);
								}else {
									mapAdmSelectedPut(s[i], null);
								}
								tempClassifsAdd(sc);
							}
						}

//					if (!getMapGroupSelected().containsKey(String.valueOf(selectedItem.getCode()))) {
//
//						if (getMapGroupRemoved().containsKey(String.valueOf(selectedItem.getCode()))) {
//							mapGroupSelectedPut(String.valueOf(selectedItem.getCode()), getMapGroupRemoved().get(String.valueOf(selectedItem.getCode())));
//
//							selectedItem.setId(getMapGroupRemoved().get(String.valueOf(selectedItem.getCode())));
//							mapGroupRemovedRemove(String.valueOf(selectedItem.getCode()));
//						}else {
//							mapGroupSelectedPut(String.valueOf(selectedItem.getCode()), null);
//						}
//						tempClassifsAdd(selectedItem);
//					}
				}
				// само при избор на конкретно значение!!!

			} else {
				if (selV != 0){ // с подчинени

						List<SystemClassif> classifList;
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("подчинени ->{}", selV);
						}

						classifList = loadClassifList(selectedItem.getCodeClassif(), false, null);

						List<SystemClassif> sc11 = null;
						sc11 = SysClassifUtils.getChildren(classifList, selectedItem.getCode(), selectedItem);
						boolean bb1 = true;
						List<Integer> tempCodesParent = new ArrayList<>();

						for (SystemClassif item : sc11) {
							if (selectedItem.getCodeClassif()==Constants.CODE_CLASSIF_ADMIN_STR) {
								if (!getMapAdmSelected().containsKey(String.valueOf(item.getCode())) && !(selV == 2 && item.getCode() == selectedItem.getCode())) {


									bb1 = true;
									if (LOGGER.isDebugEnabled()) {
										LOGGER.debug("item.code>>{}; {}; parent:{}", item.getCode(), item.getTekst(), item.getCodeParent());
									}
									boolean bs = selV == 2 && Boolean.FALSE.equals(getIsSelectNode()) && tempCodesParent.contains(item.getCode());
									if (bs) { // не трябва да се включват nodes!!!
										bb1 = false;
										if (LOGGER.isDebugEnabled()) {
											LOGGER.debug(" bez item.code>>{}", item.getCode());
										}
									}
									if (bb1) {
										boolean go=true;
										boolean b =  getOnlyZvenaRegistratura();
										if (b && !getZvenaMap().containsKey(String.valueOf(item.getCode()))) {
											go=false;
										}
										if (go) {
											//ako e izbrano samo zvenata gledame dali e tam i posle si minava po stariq na4in
											if (getMapAdmRemoved().containsKey(String.valueOf(item.getCode()))) {
												mapAdmSelectedPut(String.valueOf(item.getCode()), getMapAdmRemoved().get(String.valueOf(item.getCode())));

												item.setId(getMapAdmRemoved().get(String.valueOf(item.getCode())));
												mapAdmRemovedRemove(String.valueOf(item.getCode()));
											}else {
												mapAdmSelectedPut(String.valueOf(item.getCode()), null);
											}
											tempClassifsAdd(item);
											if (!tempCodesParent.contains(item.getCodeParent())) {
												tempCodesParent.add(item.getCodeParent());
											}
										}
									}
								}
							}else {
								//groupSluj
								if (!getMapGroupSelected().containsKey(String.valueOf(item.getCode())) && !(selV == 2 && item.getCode() == selectedItem.getCode())) {

									bb1 = true;
									if (LOGGER.isDebugEnabled()) {
										LOGGER.debug("item.code>>{}; {}; parent:{}", item.getCode(), item.getTekst(), item.getCodeParent());
									}
									boolean bg = selV == 2 && Boolean.FALSE.equals(getIsSelectNode()) && tempCodesParent.contains(item.getCode());
									if (bg) { // не трябва да се включват  nodes!!!
										bb1 = false;
										if (LOGGER.isDebugEnabled()) {
											LOGGER.debug(" bez item.code>>{}", item.getCode());
										}
									}
									if (bb1) {
										if (!getMapGroupSelected().containsKey(String.valueOf(item.getCode()))) {

											if (getMapGroupRemoved().containsKey(String.valueOf(item.getCode()))) {
												mapGroupSelectedPut(String.valueOf(item.getCode()), getMapGroupRemoved().get(String.valueOf(item.getCode())));
												item.setId(getMapGroupRemoved().get(String.valueOf(item.getCode())));
												mapGroupRemovedRemove(String.valueOf(item.getCode()));

											}else {
												mapGroupSelectedPut(String.valueOf(item.getCode()), null);
											}
											tempClassifsAdd(item);
										}
										if (!tempCodesParent.contains(item.getCodeParent())) {
											tempCodesParent.add(item.getCodeParent());
										}
									}
								}
							}
						}
					}
				}
			}
		}catch (Exception e) {
			LOGGER.error("Грешка при избор", e);
		}
	}

	/**
	 * @param row
	 */
	public void removeFromCodes() {

		Integer code = Integer.valueOf(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("rItem"));
//		Integer classif=null;
		for (int i = 0; i < getTempClassifs().size(); i++) {
			if (getTempClassifs().get(i).getCode()==code) {
//				classif=getTempClassifs().get(i).getCodeClassif();
				tempClassifsRemove(getTempClassifs().get(i));
				break;
			}
		}

//		if (classif==Constants.CODE_CLASSIF_ADMIN_STR) {
			if (getMapAdmSelected().get(code.toString())!=null) {
				mapAdmRemovedPut(code.toString(), getMapAdmSelected().get(code.toString()));
			}
			mapAdmSelectedRemove(code.toString());
//		}else {
//			if (getMapGroupSelected().get(code.toString())!=null) {
//				mapGroupRemovedPut(code.toString(), getMapGroupSelected().get(code.toString()));
//			}
//			mapGroupSelectedRemove(code.toString());
//		}


	}

	/**
	 * формира текста - избраните значения, разделени със запетаи
	 *
	 * @param tmpArr
	 * @return
	 * @throws DbErrorException
	 */
	public String sbSelectedText(List<Integer> tmpArr) throws DbErrorException {
		StringBuilder sb = new StringBuilder();
		if(tmpArr != null) {
			try {
				for (Integer code : tmpArr) {
					sb.append(getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, code, getLang(), getDateClassif()).trim()).append(", ");
				}
				int idx = sb.lastIndexOf(",");
				if (idx != -1) {
					sb.replace(idx, idx + 1, " ");
				}
			} catch (Exception e) {
				throw new DbErrorException(e);
			}
		}
		return sb.toString();
	}

	/**
	 * Търсене на класификация по текст
	 *
	 * @throws DbErrorException
	 */
	public void search() throws DbErrorException {
		Integer codeClassif = Constants.CODE_CLASSIF_ADMIN_STR;
		try {
			List<SystemClassif> classifList = null;
			if(getTreeType() == 2) { // grupi
				codeClassif = UriregConstants.CODE_CLASSIF_GROUP_EMPL;
				// групите да са филтрирани в рамките на регистртурата
				// Задължително трябва да е по азбучен ред, иначе неможе да се изгради дървото!!!
				Map<Integer, Object> specRegistratura = Collections.singletonMap(UriregClassifAdapter.REG_GROUP_INDEX_REGISTRATURA, ((UserData) getUserData()).getRegistratura());
				classifList = getSystemData().queryClassification(codeClassif, null, getDateClassif(), getLang(), specRegistratura);
			}else {
				classifList = loadClassifList(codeClassif, false, null);
			}
			TreeNode rootNode = new TreeUtils().fTree(classifList, getSearchWord(), true, true, null, null);
			setRoot(rootNode);

		} catch (Exception e) {
			throw new DbErrorException(e);
		}
	}

	/**
	 * Зарежда списъка за търсене
	 *
	 * @param codeClassif
	 * @param fixPrev     подава се true ако се иска да се оправят код на предходен при филтриране на класификация по правата на
	 *                    потребителя
	 * @param query
	 * @return
	 * @throws DbErrorException
	 */
	List<SystemClassif> loadClassifList(Integer codeClassif, boolean fixPrev, String query) throws DbErrorException {
		@SuppressWarnings("unchecked")
		Map<Integer, Object> specifics = (Map<Integer, Object>) getAttributes().get("specifics");

		Boolean filtered = false;
		if(getAttributes().get("filtered") != null) {
			filtered = (Boolean) getAttributes().get("filtered");
		}
		int lang = getUserData().getCurrentLang();

		List<SystemClassif> classifList;
		if (Boolean.TRUE.equals(filtered)) { // иска се филтрирана - по права на потребителя
			if (query != null || (specifics != null && !specifics.isEmpty())) {
				classifList = getSystemData().queryClassification(getUserData(), codeClassif, query, getDateClassif(), lang, specifics);
			} else {
				classifList = getSystemData().getSysClassification(getUserData(), codeClassif, getDateClassif(), lang, fixPrev);
			}

		} else if (specifics != null && !specifics.isEmpty()) { // ще се пуска търсене на специфики
			classifList = getSystemData().queryClassification(codeClassif, query, getDateClassif(), lang, specifics);

		} else { // иначе както си е
			if (query == null) {
				classifList = getSystemData().getSysClassification(codeClassif, getDateClassif(), lang);
			} else {
				@SuppressWarnings("unchecked")
				List<Integer> notInCodes = (List<Integer>) getAttributes().get(READONLYCODES);
				classifList = getSystemData().queryClassification(codeClassif, query, getDateClassif(), lang, notInCodes);
			}
		}

		return classifList;
	}


	public void actionChangeTree() throws DbErrorException {
		loadRoot();
	}



	/** @param root */
	public void setRoot(TreeNode root) {
		getStateHelper().put(PropertyKeys.root, root);
	}

	/** @param searchWord */
	public void setSearchWord(String searchWord) {
		getStateHelper().put(PropertyKeys.searchWord, searchWord.trim());
	}

	/** @param selectedNode */
	public void setSelectedNode(TreeNode selectedNode) {
		getStateHelper().put(PropertyKeys.selectedNode, selectedNode);
	}

	/** @param selectedType */
	public void setSelectedType(String selectedType) {
		getStateHelper().put(PropertyKeys.selectedType, selectedType);
	}

	/** @param showMe */
	public void setShowMe(boolean showMe) {
		getStateHelper().put(PropertyKeys.showMe, showMe);
	}

	/** * @param tempClassifs */
	public void setTempClassifs(List<SystemClassif> tempClassifs) {
		getStateHelper().put(PropertyKeys.tempClassifs, tempClassifs);
	}
	/** * @param tempClassifs */
	public void tempClassifsAdd(SystemClassif tempClassif) {
		getStateHelper().add(PropertyKeys.tempClassifs, tempClassif);
	}
	/** * @param tempClassifs */
	public void tempClassifsRemove(SystemClassif tempClassif) {
		getStateHelper().remove(PropertyKeys.tempClassifs, tempClassif);
	}

	/** @return */
	public TreeNode getRootGroup() {
		return (TreeNode) getStateHelper().eval(PropertyKeys.rootGroup, null);
	}
	/** @param root */
	public void setRootGroup(TreeNode rootGroup) {
		getStateHelper().put(PropertyKeys.rootGroup, rootGroup);
	}
	/** @return */
	public TreeNode getRootAdm() {
		return (TreeNode) getStateHelper().eval(PropertyKeys.rootAdm, null);
	}
	/** @param root */
	public void setRootAdm(TreeNode rootGroup) {
		getStateHelper().put(PropertyKeys.rootAdm, rootGroup);
	}

	/** @return */
	public Map<String, Integer> getMapAdmSelected() {
		@SuppressWarnings("unchecked")
		Map<String, Integer> eval = (Map<String, Integer>) getStateHelper().eval(PropertyKeys.mapAdmSelected, null);
		return eval != null ? eval : new HashMap<>();
	}
	/** @param mapAdmSelected */
	public void setMapAdmSelected(Map<String, Integer> mapAdmSelected) {
		getStateHelper().put(PropertyKeys.mapAdmSelected, mapAdmSelected);
	}
	/** @param mapGroupRemoved */
	public void mapAdmSelectedPut(String key, Integer value) {
		getStateHelper().put(PropertyKeys.mapAdmSelected, key,value);
	}
	/** @param mapGroupRemoved */
	public void mapAdmSelectedRemove(String key) {
		getStateHelper().remove(PropertyKeys.mapAdmSelected, key);
	}



	/** @return */
	public Map<String, Integer> getMapGroupSelected() {
		@SuppressWarnings("unchecked")
		Map<String, Integer> eval = (Map<String, Integer>) getStateHelper().eval(PropertyKeys.mapGroupSelected, null);
		return eval != null ? eval : new HashMap<>();
	}
	/** @param mapGroupSelected */
	public void setMapGroupSelected(Map<String, Integer> mapGroupSelected) {
		getStateHelper().put(PropertyKeys.mapGroupSelected, mapGroupSelected);
	}
	/** @param mapGroupRemoved */
	public void mapGroupSelectedPut(String key, Integer value) {
		getStateHelper().put(PropertyKeys.mapGroupSelected, key,value);
	}
	/** @param mapGroupRemoved */
	public void mapGroupSelectedRemove(String key) {
		getStateHelper().remove(PropertyKeys.mapGroupSelected, key);
	}

	/** @return */
	@SuppressWarnings("unchecked")
	public Map<String, Boolean> getZvenaMap() {
		return (Map<String, Boolean>) getStateHelper().eval(PropertyKeys.zvenaMap, null);
	}
	/** @param mapGroupSelected */
	public void setZvenaMap(Map<String, Boolean> mapGroupSelected) {
		getStateHelper().put(PropertyKeys.zvenaMap, mapGroupSelected);
	}

	/** @return */
	public Map<String, Integer> getMapAdmRemoved() {
		@SuppressWarnings("unchecked")
		Map<String, Integer> eval = (Map<String, Integer>) getStateHelper().eval(PropertyKeys.mapAdmRemoved, null);
		return eval != null ? eval : new HashMap<>();
	}
	/** @param mapAdmRemoved */
	public void setMapAdmRemoved(Map<String, Integer> mapAdmRemoved) {
		getStateHelper().put(PropertyKeys.mapAdmRemoved, mapAdmRemoved);
	}
	/** @param mapGroupRemoved */
	public void mapAdmRemovedPut(String key, Integer value) {
		getStateHelper().put(PropertyKeys.mapAdmRemoved, key,value);
	}
	/** @param mapGroupRemoved */
	public void mapAdmRemovedRemove(String key) {
		getStateHelper().remove(PropertyKeys.mapAdmRemoved, key);
	}



	/** @return */
	public Map<String, Integer> getMapGroupRemoved() {
		@SuppressWarnings("unchecked")
		Map<String, Integer> eval = (Map<String, Integer>) getStateHelper().eval(PropertyKeys.mapGroupRemoved, null);
		return eval != null ? eval : new HashMap<>();
	}
	/** @param mapGroupRemoved */
	public void setMapGroupRemoved(Map<String, Integer> mapGroupRemoved) {
		getStateHelper().put(PropertyKeys.mapGroupRemoved, mapGroupRemoved);
	}
	/** @param mapGroupRemoved */
	public void mapGroupRemovedPut(String key, Integer value) {
		getStateHelper().put(PropertyKeys.mapGroupRemoved, key,value);
	}
	/** @param mapGroupRemoved */
	public void mapGroupRemovedRemove(String key) {
		getStateHelper().remove(PropertyKeys.mapGroupRemoved, key);
	}




	/** @return */
	public Integer getTypeDocDelo() {
		return (Integer) getStateHelper().eval(PropertyKeys.typeDocDelo, null);
	}

	/** @param typeDocDelo */
	public void setTypeDocDelo(Integer typeDocDelo) {
		getStateHelper().put(PropertyKeys.typeDocDelo, typeDocDelo);
	}

	/** @return */
	public Integer getTreeType() {
		return (Integer) getStateHelper().eval(PropertyKeys.treeType, 1);
	}

	/** @param treeType */
	public void setTreeType(Integer treeType) {
		getStateHelper().put(PropertyKeys.treeType, treeType);
	}

	/** @return */
	public Boolean getOnlyZvenaRegistratura() {
		return (Boolean) getStateHelper().eval(PropertyKeys.onlyZvenaRegistratura, false);
	}

	/** @param treeType */
	// НЕ се използва !!!
	public void setOnlyZvenaRegistratura(Boolean onlyZvenaRegistratura) {
		getStateHelper().put(PropertyKeys.onlyZvenaRegistratura, onlyZvenaRegistratura);
	}




	/** @return the dateClassif */
	private Date getDateClassif() {
		if (this.dateClassif == null) {
			this.dateClassif = (Date) getAttributes().get("dateClassif");
			if (this.dateClassif == null) {
				this.dateClassif = new Date();
			}
		}
		return this.dateClassif;
	}

	/** @return the systemData */
	private BaseSystemData getSystemData() {
		if (this.systemData == null) {
			this.systemData = (BaseSystemData) JSFUtils.getManagedBean("systemData");
		}
		return this.systemData;
	}

	/** @return the userData */
	private BaseUserData getUserData() {
		if (this.userData == null) {
			this.userData = (BaseUserData) JSFUtils.getManagedBean("userData");
		}
		return this.userData;
	}












}
