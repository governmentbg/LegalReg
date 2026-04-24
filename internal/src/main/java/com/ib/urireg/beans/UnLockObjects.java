package com.ib.urireg.beans;

import com.ib.urireg.db.dao.ReferentDAO;
import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;
import jakarta.persistence.Query;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * За отключване на обекти.
 */

@Named
@ViewScoped
public class UnLockObjects extends IndexUIbean {

    private static final long serialVersionUID = 507326780295416575L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UnLockObjects.class);

    private List<Integer> usersList = new ArrayList<>();
    private List<SystemClassif> usersListClassifs = new ArrayList<>();
    private List<String> usersImena = new ArrayList<>();
    private transient AdmUserDAO userDao;
    /**
     * код на обект
     */
    private Integer refType = getCodeDocument();
    private List<SelectItem> obectList;
    private transient List<Object[]> lockList;
    private transient List<Object[]> rowSelectedTmp = new ArrayList<>();
    private List<LockObjects> lockObjectsList;
    private List<LockObjects> lockObjectsListAll = new ArrayList<>();
    private transient List<Object[]> rowSelected = new ArrayList<>();
    private List<LockObjects> rowSelectedN = new ArrayList<>();
    private String msgBox;
    private String msgBox1;
    private Integer zapDocVid;
    private List<SelectItem> vidZapovedList;//за вид протокол
    private Integer period;
    private Date dateFrom;
    private Date dateTo;

    public static final String BEANMSG = "beanMessages";
    public static final String FROMLOCKOBJLO = " FROM lock_objects lo ";
    public static final String TIPOBJIDLOCKINF = " SELECT lo.lock_date dataZakl, lo.user_id userZakl, lo.object_tip objectTip, lo.object_id objectId, lo.lock_info lockInfo ";
    public static final String OBJTYPE = "objType";


    public Integer getCodeClassifUser() {
        return SysConstants.CODE_CLASSIF_USERS;
    }

    /**
     * Код на класификация "Тип документ"
     */
    public Integer getCodeDocument() {
        return UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC;
    }

    /**
     * Код на класификация "Административна структура"
     */
    public Integer getCodeAdmStructura() {
        return UriregConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT;
    }

    public Integer getCodeLiceDossier() {
        return UriregConstants.CODE_ZNACHENIE_JOURNAL_LICE;
    }

    /**
     *
     */
    @PostConstruct
    void initData() {

        obectList = new ArrayList<>();
        obectList.add(new SelectItem(getCodeDocument(), getMessageResourceString(LABELS, "unLockObj.order")));
        obectList.add(new SelectItem(getCodeAdmStructura(), getMessageResourceString(LABELS, "admStruct.admStruct")));
        obectList.add(new SelectItem(getCodeLiceDossier(), getMessageResourceString(LABELS, "dossier.dossier")));

        vidZapovedList = new ArrayList<>();

        try {

            String nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT, getCurrentLang(), new Date());
            vidZapovedList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT, nameItem));

            nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019, getCurrentLang(), new Date());
            vidZapovedList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT, nameItem));

            nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_STAJ, getCurrentLang(), new Date());
            vidZapovedList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_STAJ, nameItem));

            nameItem = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_DOC_VID, UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ, getCurrentLang(), new Date());
            vidZapovedList.add(new SelectItem(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_STAJ, nameItem));

        } catch (DbErrorException e) {
            throw new RuntimeException(e);
        }


        this.userDao = new AdmUserDAO(getUserData());
        String qq = "select  count( * )  FROM LOCK_OBJECTS ";
        Query query = JPA.getUtil().getEntityManager().createNativeQuery(qq);
        Integer cc = SearchUtils.asInteger(query.getSingleResult());
        if (cc == 0) {
            setMsgBox(getMessageResourceString(BEANMSG, "unLockObject.noLock"));
            PrimeFaces current = PrimeFaces.current();
            current.executeScript("PF('dlg2').show();");
        }
    }

    /**
     * премахва избраните критерии за търсене
     */
    public void actionUnlock() {

        if (lockObjectsListAll.isEmpty()) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, "unLockObject.noSelected"));
            return;
        }
        Integer objType = null;
        Integer obectId = null;
        try {
            JPA.getUtil().begin();
            for (LockObjects selectedRow : lockObjectsListAll) {
                obectId = Integer.valueOf(selectedRow.getObjectId().toString());
                objType = Integer.valueOf(selectedRow.getObjectTip().toString());
                lockList = null;
                String qq = "DELETE "
                        + FROMLOCKOBJLO
                        + " WHERE lo.object_id=:obectId AND lo.OBJECT_TIP=:objType ";

                Query query = JPA.getUtil().getEntityManager().createNativeQuery(qq);
                query.setParameter(OBJTYPE, objType).setParameter("obectId", obectId);
                query.executeUpdate();
				// запис в журнала, че обекта е отключен
				String comment = "";
				if (objType.equals(getCodeAdmStructura())) {
					comment = (getMessageResourceString(LABELS, "unLockObj.order"));
				} else if (objType.equals(getCodeDocument())) {
					comment = (getMessageResourceString(LABELS, "admStruct.admStruct"));
				} else if (objType.equals(getCodeLiceDossier())) {
					comment = (getMessageResourceString(LABELS, "dossier.dossier"));
				}
				SystemJournal journal = new SystemJournal(refType, Integer.parseInt(obectId.toString()), "отключване на " + comment);
                journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
                journal.setDateAction(new Date());
                journal.setIdUser(getUserData().getUserId());
                switch (refType.intValue()) {
                    case UriregConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT:
                        journal.setCodeObject(UriregConstants.CODE_ZNACHENIE_JOURNAL_REFERENT);
                        journal.setIdentObject("отключване - адм. структура");
                        new ReferentDAO(getUserData()).saveAudit(journal);
                        break;

                }
            }
            JPA.getUtil().commit();
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, "unLockObject.unLocked"));


        } catch (DbErrorException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
            JPA.getUtil().rollback();
        } finally {
            JPA.getUtil().closeConnection();
        }
        actionSearch();
    }


    /**
     * премахва избраните критерии за търсене
     */
    public void actionClear() {

        clearFilter();
        changePeriod();

        lockObjectsListAll = new ArrayList<>();
        rowSelectedTmp = new ArrayList<>();
    }

    public void clearFilter() {
        refType = getCodeDocument();
        lockObjectsList = null;
        usersList = new ArrayList<>();
        zapDocVid = null;
        period = null;
        usersList = new ArrayList<>();
        usersListClassifs = new ArrayList<>();
    }

    /**
     * Метод за смяна на датите при избор на период за търсене.
     */
    public void changePeriod() {

        if (this.period != null) {
            Date[] di;
            di = DateUtils.calculatePeriod(this.period);
            setDateFrom(di[0]);
            setDateTo(di[1]);
        } else {
            setDateFrom(null);
            setDateTo(null);
        }
    }


    /**
     * Метод за смяна на датите при избор на период за търсене.
     */

    public void changeDate() {
        this.setPeriod(null);
    }


    /**
     * Списък с документи по зададени критерии
     */
    @SuppressWarnings("unchecked")
    public void actionSearch() {
        String qq = "";
        lockObjectsList = new ArrayList<>();
        lockObjectsListAll = new ArrayList<>();
        rowSelectedTmp = new ArrayList<>();

        lockList = null;

        Query query;

		if (refType == UriregConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT) {
            qq = TIPOBJIDLOCKINF
                    + FROMLOCKOBJLO
                    + " WHERE OBJECT_TIP=:objType ";

        } else if (refType == UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC) {
            qq = TIPOBJIDLOCKINF
                    + " , d.doc_vid, d.rn_doc, d.doc_date "
                    + FROMLOCKOBJLO
                    + " , doc d "
                    + " WHERE OBJECT_TIP=:objType AND lo.object_id = d.doc_id";
            if (zapDocVid != null) {
                qq += " AND d.doc_vid = " + zapDocVid;
            }

        } else if (refType == UriregConstants.CODE_ZNACHENIE_JOURNAL_LICE) {
            qq = TIPOBJIDLOCKINF
                    + " , l.names || '('|| l.egn || ')' "
                    + FROMLOCKOBJLO
                    + " , lice l "
                    + " WHERE OBJECT_TIP=:objType AND lo.object_id = l.lice_id ";
        }

        String uId = "";

        if (!usersList.isEmpty()) {
            for (Integer tt : usersList) {
                uId += tt + ",";
            }
            uId = uId.substring(0, uId.length() - 1);
            qq += " and lo.USER_ID in (" + uId + ")";
        }

        if (dateFrom != null) {
           qq += " and lo.LOCK_DATE >= :dateFrom ";
        }

        if (dateTo != null) {
			qq += " and lo.LOCK_DATE <= :dateTo ";
        }

		query = JPA.getUtil().getEntityManager().createNativeQuery(qq).setParameter(OBJTYPE, refType);

		if (dateFrom != null) {
			query.setParameter("dateFrom", DateUtils.startDate(dateFrom));
		}

		if (dateTo != null) {
			query.setParameter("dateTo", DateUtils.endDate(dateTo));
		}

        lockList = query.getResultList();

        lockObjectsList = new ArrayList<>();
        if (lockList.isEmpty()) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, "unLockObject.noLocked"));
            return;
        }

        for (int i = 0; i < lockList.size(); i++) {

            Integer objectId = null;
            Integer objectTip = null;
            Integer userId = null;
            Date lockDate = null;
            String lockInfo = null;
            Integer zapDocVid = null;
            String nameLice = null;
			String rnDoc = null;
			Date docDate = null;
            LockObjects tmp = new LockObjects(objectId, objectTip, userId, lockDate, lockInfo, zapDocVid, nameLice, rnDoc, docDate);
            Object[] op = lockList.get(i);

            if (refType == UriregConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT) {
				if (op[0] instanceof java.util.Date) {
					tmp.setLockDate((java.util.Date) op[0]);
				}
				tmp.setUserId(Integer.valueOf(op[1].toString()));
				tmp.setObjectTip(Integer.valueOf(op[2].toString()));
                tmp.setObjectId(Integer.valueOf(op[3].toString()));
                tmp.setLockInfo((String) op[4]);
            }
            if (refType == UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC) {
				if (op[0] instanceof java.util.Date) {
					tmp.setLockDate((java.util.Date) op[0]);
				}
                tmp.setUserId(Integer.valueOf(op[1].toString()));
                tmp.setObjectTip(Integer.valueOf(op[2].toString()));
                tmp.setObjectId(Integer.valueOf(op[3].toString()));
				tmp.setLockInfo((String) op[4]);
                tmp.setZapDocVid(Integer.valueOf(op[5].toString()));
				tmp.setRnDoc((String) op[6]);
				if (op[7] instanceof java.util.Date) {
                    tmp.setDocDate((java.util.Date) op[7]);
                }
            }

            if (refType == UriregConstants.CODE_ZNACHENIE_JOURNAL_LICE) {
				if (op[0] instanceof java.util.Date) {
					tmp.setLockDate((java.util.Date) op[0]);
				}
				tmp.setUserId(Integer.valueOf(op[1].toString()));
				tmp.setObjectTip(Integer.valueOf(op[2].toString()));
                tmp.setObjectId(Integer.valueOf(op[3].toString()));
                tmp.setLockInfo((String) op[4]);
                tmp.setNameLice((String) op[5]);
            }

            lockObjectsList.add(tmp);
        }
    }

    public List<Integer> getUsersList() {
        return usersList;
    }

    public void setUsersList(List<Integer> usersList) {

        this.usersImena = new ArrayList<>();

        if (!usersList.isEmpty()) {

            try {

                for (Integer user : usersList) {

                    AdmUser userTmp = this.userDao.findById(user);
                    usersImena.add(userTmp.getNames());
                }

            } catch (DbErrorException e) {
                LOGGER.error("Грешка при търсене на участник в групата! ", e);
                JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"));
            }
        }

        this.usersList = usersList;
    }

    public Integer getRefType() {
        return refType;
    }

    public void setRefType(Integer refType) {
        this.refType = refType;
    }

    public List<SelectItem> getObectList() {
        return obectList;
    }

    public void setObectList(List<SelectItem> obectList) {
        this.obectList = obectList;
    }

    /*
     * Множествен избор на задачи
     */

    /**
     * Избира всички редове от текущата страница
     *
     * @param event
     */
    public void onRowSelectAll(ToggleSelectEvent event) {
        List<LockObjects> tmpL = new ArrayList<>();
        tmpL.addAll(getLockObjectsListAll());
        if (event.isSelected()) {
            for (LockObjects obj : rowSelectedN) {
                boolean bb = true;
                Integer l1 = Integer.valueOf(obj.getObjectId().toString());
                Integer l3 = Integer.valueOf(obj.getObjectTip().toString());
                for (LockObjects j : tmpL) {
                    Integer l2 = Integer.valueOf(j.getObjectId().toString());
                    Integer l4 = Integer.valueOf(j.getObjectTip().toString());
                    if (l1.equals(l2) && l3.equals(l4)) {
                        bb = false;
                        break;
                    }
                }
                if (bb) {
                    tmpL.add(obj);
                }
            }
        } else {
        }
        setLockObjectsListAll(tmpL);
        LOGGER.debug("onToggleSelect->>");
    }

    @SuppressWarnings("rawtypes")
    public void rowSelectCheckbox(SelectEvent event) {
        //	LockObjects u = (LockObjects) event.getObject();
        if (event != null && event.getObject() != null) {
            List<LockObjects> tmpList = getLockObjectsListAll();

            LockObjects obj = (LockObjects) event.getObject();
            boolean bb = true;
            Integer l2 = Integer.valueOf(obj.getObjectId().toString());
            Integer l4 = Integer.valueOf(obj.getObjectTip().toString());
            for (LockObjects j : tmpList) {
                Integer l1 = Integer.valueOf(j.getObjectId().toString());
                Integer l3 = Integer.valueOf(j.getObjectTip().toString());
                if (l1.equals(l2) && l3.equals(l4)) {
                    bb = false;
                    break;
                }
            }
            if (bb) {
                tmpList.add(obj);
                setLockObjectsListAll(tmpList);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void rowUnselectCheckbox(UnselectEvent event) {
        List<LockObjects> tmpList = getLockObjectsListAll();

        LockObjects obj = (LockObjects) event.getObject();
        boolean bb = false;
        Integer l2 = Integer.valueOf(obj.getObjectId().toString());
        Integer l4 = Integer.valueOf(obj.getObjectTip().toString());
        for (LockObjects j : tmpList) {
            Integer l1 = Integer.valueOf(j.getObjectId().toString());
            Integer l3 = Integer.valueOf(j.getObjectTip().toString());
            if (l1.equals(l2) && l3.equals(l4)) {
                bb = true;
                break;
            }
        }
        if (bb) {
            tmpList.remove(obj);
            setLockObjectsListAll(tmpList);
        }

    }

    /**
     * Select one row
     *
     * @param event
     */
    @SuppressWarnings("rawtypes")
    public void onRowSelect(SelectEvent event) {
        if (event != null && event.getObject() != null) {
            List<Object[]> tmpList = getRowSelectedTmp();

            Object[] obj = (Object[]) event.getObject();
            boolean bb = true;
            Integer l2 = Integer.valueOf(obj[0].toString());
            for (Object[] j : tmpList) {
                Integer l1 = Integer.valueOf(j[0].toString());
                if (l1.equals(l2)) {
                    bb = false;
                    break;
                }
            }
            if (bb) {
                tmpList.add(obj);
                setRowSelectedTmp(tmpList);
            }
        }
    }

    public List<LockObjects> getLockObjectsList() {
        return lockObjectsList;
    }

    public void setLockObjectsList(List<LockObjects> lockObjectsList) {
        this.lockObjectsList = lockObjectsList;
    }

    public List<Object[]> getRowSelected() {
        return rowSelected;
    }

    public void setRowSelected(List<Object[]> rowSelected) {
        this.rowSelected = rowSelected;
    }

    public String getMsgBox() {
        return msgBox;
    }

    public void setMsgBox(String msgBox) {
        this.msgBox = msgBox;
    }

    public class LockObjects implements Serializable {
        /**
         *
         */

        public Integer objectTip;
        public Integer userId;
        public Date lockDate;
        public String lockInfo;
        public Integer zapDocVid;
        public String nameLice;
		public String rnDoc;
		public Date docDate;


        public LockObjects(Integer objectId, Integer objectTip, Integer userId, Date lockDate, String lockInfo,
                           Integer zapDocVid, String nameLice, String rnDoc, Date docDate) {
            this.objectId = objectId;
            this.objectTip = objectTip;
            this.userId = userId;
            this.lockDate = lockDate;
            this.zapDocVid = zapDocVid;
            this.nameLice = nameLice;
			this.rnDoc = rnDoc;
			this.docDate = docDate;

        }

        public Integer objectId;

        public Integer getObjectId() {
            return objectId;
        }

        public void setObjectId(Integer objectId) {
            this.objectId = objectId;
        }

        public Integer getObjectTip() {
            return objectTip;
        }

        public void setObjectTip(Integer objectTip) {
            this.objectTip = objectTip;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public Date getLockDate() {
            return lockDate;
        }

        public void setLockDate(Date lockDate) {
            this.lockDate = lockDate;
        }

        public String getLockInfo() {
            return lockInfo;
        }

        public void setLockInfo(String lockInfo) {
            this.lockInfo = lockInfo;
        }

		public Integer getZapDocVid() {
			return zapDocVid;
		}

		public void setZapDocVid(Integer zapDocVid) {
			this.zapDocVid = zapDocVid;
		}

		public String getNameLice() {
			return nameLice;
		}

		public void setNameLice(String nameLice) {
			this.nameLice = nameLice;
		}

		public String getRnDoc() {
			return rnDoc;
		}

		public void setRnDoc(String rnDoc) {
			this.rnDoc = rnDoc;
		}

		public Date getDocDate() {
			return docDate;
		}

		public void setDocDate(Date docDate) {
			this.docDate = docDate;
		}
	}

    public Integer getZapDocVid() {
        return zapDocVid;
    }

    public void setZapDocVid(Integer zapDocVid) {
        this.zapDocVid = zapDocVid;
    }

    public List<SelectItem> getVidZapovedList() {
        return vidZapovedList;
    }

    public void setVidZapovedList(List<SelectItem> vidZapovedList) {
        this.vidZapovedList = vidZapovedList;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public List<Object[]> getRowSelectedTmp() {
        return rowSelectedTmp;
    }

    public void setRowSelectedTmp(List<Object[]> rowSelectedTmp) {
        this.rowSelectedTmp = rowSelectedTmp;
    }

    public List<LockObjects> getLockObjectsListAll() {
        return lockObjectsListAll;
    }

    public void setLockObjectsListAll(List<LockObjects> lockObjectsListAll) {
        this.lockObjectsListAll = lockObjectsListAll;
    }

    public List<LockObjects> getRowSelectedN() {
        return rowSelectedN;
    }

    public void setRowSelectedN(List<LockObjects> rowSelectedN) {
        this.rowSelectedN = rowSelectedN;
    }

    public String getMsgBox1() {
        return msgBox1;
    }

    public void setMsgBox1(String msgBox1) {
        this.msgBox1 = msgBox1;
    }

    public List<SystemClassif> getUsersListClassifs() {
        return usersListClassifs;
    }

    public void setUsersListClassifs(List<SystemClassif> usersListClassifs) {
        this.usersListClassifs = usersListClassifs;
    }


}
