package com.ib.urireg.beans;

import bg.egov.regix.RegixClientException;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;
import com.ib.system.utils.ValidationUtils;
import com.ib.urireg.components.CompUdostDokument;
import com.ib.urireg.db.dao.*;
import com.ib.urireg.db.dto.*;
import com.ib.urireg.eforms.utils.EFormUtils;
import com.ib.urireg.eforms.utils.EgovContainer;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.SysClassifAdapter;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.urireg.system.UserData;
import com.ib.urireg.utils.RegixUtils2;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.omnifaces.cdi.ViewScoped;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.inject.Named;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.urireg.system.UriregConstants.*;

@Named("dossierPerson")
@ViewScoped
public class DossierPerson extends IndexUIbean implements Serializable {

    private static final long serialVersionUID = -5644709981503440640L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DossierPerson.class);

    private Integer ekatte;

    private Doc zaiavStaj;

    private Doc zaiavIzpit;

    private Doc zaiavDublikat;

    private Staj staj;

    private Lice lice;

    private List<Object[]> stajoveList;

    private List<Object[]> izpitiList;

    private LiceDAO liceDAO;
    private StajDAO stajDAO;
    private IzpitDAO izpitDAO;
    private DocDAO docDAO;
    private  Integer liceId;

    private List<Files> filesList;
    private List<Files> filesListRemoved;

    private String udDoc;

    private boolean udDocFileCreateOrig;
    private boolean udDocFileCreateDubl;

    // Компонента за генериране на УД
    private CompUdostDokument bindCompUdostDoc;
    private boolean udDublikat;

    private List<SystemClassif> statusList;

    private boolean showBntStaj;
    private boolean showBntEditStaj;
    private boolean showBntIzpit;
    private boolean showBntEditIzpit;

    private boolean showBntVeiwDublikat;

    private Lice liceRegix;

    private LiceDocRegix  liceDocRegix;

    private String lcNum;

    private UserData ud;

    private Integer isView;

    private Integer statusFromStaj;
    private Integer statusFromIzpit;

    private List<Object[]> dublikatiList;
    private Integer lastDublikatiId;
    //private boolean visibleModalDublikati = false;

    private Object[] selectedDocDubl;

    private String messageText;
    private String shablonText;
    private List<SystemClassif> shabloniList;


    private String rnDocZapoved;
    private Date docDateZapoved;
    private boolean showBntAttachZapStaj;

    private Integer statusLice;

    @PostConstruct
    public void init() {
        //System.out.println("DossierPerson инит ");
        ud = getUserData(UserData.class);
        liceDAO = new LiceDAO(ud);
        stajDAO = new StajDAO(ud);
        izpitDAO = new IzpitDAO(ud);
        docDAO = new DocDAO(ud);

        liceRegix = new Lice();
        liceDocRegix = new LiceDocRegix();

        zaiavDublikat = new Doc();
        zaiavIzpit = new Doc();
        filesList = new ArrayList<>();
        filesListRemoved = new ArrayList<>();

        statusList = new ArrayList<>();
        shabloniList = new ArrayList<>();

        udDoc ="";
        udDocFileCreateOrig = false;
        udDocFileCreateDubl = false;
        showBntVeiwDublikat = false;

        try {
            String liceStr = JSFUtils.getRequestParameter("idLice");
            if(liceStr!=null && !liceStr.trim().isEmpty()){
                liceId = Integer.valueOf(liceStr);
            }
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage(), e);
        }


        showBntStaj  = true;
        showBntEditStaj = true;
        showBntIzpit = true;
        showBntEditIzpit = true;

        FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
        String isView = (String) faceletContext.getAttribute("isView");

       // LOGGER.debug("isView----->  "+isView);
        this.isView=0;
        if(!SearchUtils.isEmpty(isView)) {
            this.isView = Integer.valueOf(isView);
        }

        if(liceId==null){
            actionNewDossierPerson();
        } else {
            if(this.isView.intValue()==1){
                //load dossier view
                actionLoadDossierPerson();
            } else if(checkForLock(liceId)){
                lockDossier(liceId);
                //load dossier
                actionLoadDossierPerson();
            }


        }



    }

    private void removeStatus(Integer code){
        for(int i=0; i<statusList.size(); i++){
            SystemClassif s = statusList.get(i);
            if(s.getCode()==code){
                statusList.remove(i);
                break;
            }
        }
    }

    public void actionNewDossierPerson() {
        zaiavStaj = new Doc();
        zaiavStaj.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAIAV_STAJ);

        try {
            zaiavStaj.setRnDoc(getSystemData().decodeItemLite(CODE_CLASSIF_DOC_VID, CODE_ZNACHENIE_DOC_VID_ZAIAV_STAJ,getCurrentLang(),new Date(),false).getCodeExt());
        } catch (DbErrorException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,e.getMessage());
        }

        staj = new Staj();
        staj.setStajVid(UriregConstants.CODE_ZNACHENIE_STAJ_VID_INITIAL);
        lice = new Lice();

        filesList = new ArrayList<>();
        filesListRemoved = new ArrayList<>();

        liceId = null;
        ekatte = null;

        loadStatusLiceStaj(false);

    }

    private void actionLoadDossierPerson() {
        zaiavStaj = new Doc();
        try {
            lice = liceDAO.findById(liceId);

            stajoveList = stajDAO.findStajByLiceList(liceId);
            izpitiList  = izpitDAO.loadIzpitiDosie(liceId);

            udDocFileCreateOrig = false;
            udDocFileCreateDubl = false;

            showBntAttachZapStaj = false;

            if(lice.getUdostId()!=null) {

                udDoc = docDAO.decodeRnDateDoc(lice.getUdostId());

               udDocFileCreateOrig = !docDAO.checkDocForFile(lice.getUdostId());

               //ще проверяваме дали може да се показва бутона за генериране на дубликат уд
               if(!udDocFileCreateOrig){
                   //брой на файлове дубликати
                   Integer brDocFile = docDAO.countDocForFile(lice.getUdostId(),CODE_ZNACHENIE_FILE_PURPOSE_UD_DUPLICATE);
                   //брой подадени заявления за дубликати
                   Integer brDoc = docDAO.countDocByVidAndLice(CODE_ZNACHENIE_DOC_VID_ZAIAV_DUBL_UDOST,liceId);

                   if(brDoc>0){
                       showBntVeiwDublikat = true;
                   }

                   if (brDoc.intValue() > brDocFile.intValue()) {
                       udDocFileCreateDubl = true;
                   }

               }

                showBntStaj  = false;
                showBntEditStaj = false;
                showBntIzpit = false;
                showBntEditIzpit = false;
            } else {
                // butoni
                if( (lice.getDo2019()==null || lice.getDo2019().intValue() != CODE_ZNACHENIE_DA) && lice.getBroiIzpit()!=null && lice.getBroiIzpit().intValue() >= 3) {
                    showBntStaj  = false;
                    showBntEditStaj = false;
                    showBntIzpit = false;
                    showBntEditIzpit = false;
                } else {
                    //само ако е неиздържал да може да подава заявление за допълнителен стаж
                    showBntStaj = false;
                    if(lice.getStatus().intValue() ==CODE_ZNACHENIE_LICE_STATUS_EXAM_FAILED){
                        showBntStaj = true;
                    }

                    showBntIzpit = false;
                    //бутона за ново заявление да се показва когато имаме стаж без ид на заявление за изпит
                    if(stajoveList!=null && !stajoveList.isEmpty()){
                        Object[] stajData = stajoveList.get(0);
                        Integer idZayqv =   SearchUtils.asInteger(stajData[19]);
                        Integer idZapovedStaj =   SearchUtils.asInteger(stajData[16]);
                        if(idZayqv==null && idZapovedStaj!=null && lice.getStatus().intValue() == CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED){
                            showBntIzpit = true;
                        }
                    }
                    // или последният изпит е "неявил се" също позволяваме да може да се подава заявление за изпит
                    if(!showBntIzpit) {
                        if(izpitiList!=null && !izpitiList.isEmpty()) {
                            Object[] izpData = izpitiList.get(0);
                            Integer protTestStatus = SearchUtils.asInteger(izpData[8]);
                            Integer protCaseStatus = SearchUtils.asInteger(izpData[13]);

                            if ((protTestStatus != null && protTestStatus.intValue() == CODE_ZNACHENIE_IZPIT_RESULT_MISSED)
                                    || (protCaseStatus != null && protCaseStatus.intValue() == CODE_ZNACHENIE_IZPIT_RESULT_MISSED)) {
                                showBntIzpit = true;
                            }
                        }
                    }

                }

                loadDatePoint();
            }

         } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadDosie"));
        } finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void actionSaveNew(){

       boolean b1 = checkDataLice("New");
       boolean b2 =  checkDataStaj("New");
        if (b1 && b2) {
            try {

                JPA.getUtil().runInTransaction(() -> {

                    if(lice.getId() == null) {
                        liceDAO.createNewDossier(zaiavStaj, lice, staj, getSystemData());
                    } else {

                        //lice.setAddrEkatte(ekatte);
                        liceDAO.save(lice, getSystemData());
                        stajDAO.save(staj);
                        docDAO.save(zaiavStaj, lice.getId());
                    }

                    for(Files f:filesList){
                        if(f.getId()==null) {
                            new FilesDAO(ud).saveFileObject(f, zaiavStaj.getId(), CODE_ZNACHENIE_JOURNAL_DOC);
                        }
                    }

                    for(Files f:filesListRemoved){
                        new FilesDAO(ud).deleteFileObject(f);
                    }
                });

                JPA.getUtil().runWithClose(() ->{
                    lice = liceDAO.findById(lice.getId());
                    zaiavStaj = docDAO.findById(zaiavStaj.getId());
                    if (zaiavStaj.getPrilVidList()!=null) {
                        zaiavStaj.getPrilVidList().size();
                    }
                });

                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

                //actionNewDossierPerson();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorSaveDosie"));
            }
        }
    }

    public void actionNewZaqvIzpit(){
        zaiavIzpit = new Doc();
        zaiavIzpit.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAIAV_IZPIT);

        try {
            zaiavIzpit.setRnDoc(getSystemData().decodeItemLite(CODE_CLASSIF_DOC_VID, CODE_ZNACHENIE_DOC_VID_ZAIAV_IZPIT,getCurrentLang(),new Date(),false).getCodeExt());
        } catch (DbErrorException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,e.getMessage());
        }

        zaiavIzpit.setDocDate(new Date());
        zaiavIzpit.setZaiavDate(new Date());
        filesListRemoved = new ArrayList<>();
        filesList = new ArrayList<>();

        try {
            JPA.getUtil().runWithClose(()-> staj = stajDAO.findById(asInteger(stajoveList.get(0)[0])) );
        } catch (BaseException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadStaj"));
        }
        statusFromIzpit = Integer.valueOf(lice.getDbStatus());

        loadStatusLiceIzpit();
    }

    public void actionEditZaqvIzpit(){
        zaiavIzpit = new Doc();
        String idZaqvIzpit = JSFUtils.getRequestParameter("idZaqvIzpit");
        filesListRemoved = new ArrayList<>();
        try {
            zaiavIzpit = docDAO.findById(Integer.valueOf(idZaqvIzpit));
            filesList = new FilesDAO(ud).selectByFileObject(zaiavIzpit.getId(), CODE_ZNACHENIE_JOURNAL_DOC);


            //от връзката ако има ако не от последният
            staj = stajDAO.findStajIzpitId(zaiavIzpit.getId());
            if(staj==null) {
                staj = stajDAO.findById(asInteger(stajoveList.get(0)[0]));
            }
        } catch (DbErrorException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadIzpit"));
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadIzpit"));
        } finally {
            JPA.getUtil().closeConnection();
        }

        statusFromIzpit = Integer.valueOf(lice.getDbStatus());

        loadStatusLiceIzpit();
    }

    public void actionSaveIzpit(){
        boolean isNew = true;
        boolean glD = true;
        if(zaiavIzpit.getId()!=null){
            isNew = false;
        } else {
            glD =checkDataEkate();
        }

        if (glD && checkDataZaqvIzpit(isNew)) {
            try {

                JPA.getUtil().runInTransaction(() -> {
                        izpitDAO.saveZayavIzpit(liceId, staj, zaiavIzpit);
                        lice.setStatus(statusFromIzpit.intValue());
                        liceDAO.save(lice); //да запише статуса
                        for(Files f:filesList){
                            if(f.getId()==null) {
                                new FilesDAO(ud).saveFileObject(f, zaiavIzpit.getId(), CODE_ZNACHENIE_JOURNAL_DOC);
                            }
                        }

                        for(Files f:filesListRemoved){
                            new FilesDAO(ud).deleteFileObject(f);
                        }

                });

                JPA.getUtil().runWithClose(() -> {
                    izpitiList  = izpitDAO.loadIzpitiDosie(liceId);
                    stajoveList = stajDAO.findStajByLiceList(liceId);
                });

                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

                if(isNew) {
                    showBntIzpit=false;
                }

                String dialog = "PF('zayavIzpitRezolMP').hide();";
                PrimeFaces.current().executeScript(dialog);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorSaveIzpit"));
            }
        }

    }

    public void actionRemoveZaqvlenieIzpit() {
        try {
            Object[] izpit = izpitiList.get(0);

            deleteDoc(SearchUtils.asInteger(izpit[15]));

            izpitiList.remove(0);

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dossier.deleteIzpit"));
        } catch (ObjectInUseException  e) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorDeleteIzpit"),e.getMessage());
        }
    }

    public List<Integer> loadDocPrilList(Integer docId) {
        try {

            return stajDAO.selectDocPrilByDocId(docId);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadDocs"));
        } finally {
            JPA.getUtil().closeConnection();
        }

        return new ArrayList<>();
    }

    public void actionEditLice(){

        try {
            lice = liceDAO.findById(liceId);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadLice"));
        } finally {
            JPA.getUtil().closeConnection();
        }

        ekatte = lice.getAddrEkatte();
        //starRed =(lice.getDo2019()!=null && lice.getDo2019().intValue() == CODE_ZNACHENIE_DA?true:false);

        loadStatusListLice();
    }

    public void  actionSaveLice(){

        if (checkDataLice("")) {

            try {
                lice.setAddrEkatte(ekatte);

//                if (starRed) {
//                    lice.setDo2019(CODE_ZNACHENIE_DA);
//                } else {
//                    lice.setDo2019(CODE_ZNACHENIE_NE);
//                }
                JPA.getUtil().runInTransaction(() -> liceDAO.save(lice, getSystemData()));

                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

                String dialog = "PF('editLiceMP').hide();";
                PrimeFaces.current().executeScript(dialog);

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorSaveLice"));
            }
        }
    }

    public void actionGetBirthdayFromEGN(){
        LOGGER.debug("-----> actionGetBirthdayFromEGN");
        lice.setBirthDate(null);
        if (lice.getEgn()!=null && !lice.getEgn().trim().isEmpty()) {

            if(!ValidationUtils.isValidEGN(lice.getEgn())){
                JSFUtils.addMessage("dossierForm:egnLiceNew", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages,"refCorr.msgValidEgn"));
                return;
            }

            try {
                Date bd =StringUtils.birthdayFromEGN(lice.getEgn());
                lice.setBirthDate(bd);
            } catch (Exception e) {
                LOGGER.debug(e.getMessage(), e);
                lice.setBirthDate(null);
            }
        }

        if ((lice.getEgn()!=null && !lice.getEgn().trim().isEmpty()) ||
                (lice.getLnc()!=null && !lice.getLnc().trim().isEmpty()) ) {
            try {
                if (liceDAO.chckByEgnLnc(lice.getEgn(), lice.getLnc() ,lice.getId())) {
                    JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.liceExistEGN")+ "("+lice.getEgn()+")");
                    lice.setEgn(null);
                    lice.setLnc(null);
                    lice.setBirthDate(null);

                } else if (lice.getId() == null){
                    //
                    String perm = getSystemData().getSettingsValue("regix.GRAO.PersonDataSearch");

                    if (perm!=null && perm.trim().equals("1") && lice.getEgn()!=null && !lice.getEgn().trim().isEmpty()) {
                        String permAddres = getSystemData().getSettingsValue("regix.GRAO.PermanentAddressSearch");

                        RegixUtils2.loadLiceByEgn(lice, lice.getEgn(), true, (permAddres!=null && permAddres.equals("1")?true:false), getSystemData());

                        ekatte = lice.getAddrEkatte();
                    }
                }
            } catch ( RegixClientException e){
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "general.regixClientError"));
            } catch ( DatatypeConfigurationException e){
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "general.regixClientError"));
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorCheckLice"));
            } finally {
                JPA.getUtil().closeConnection();
            }
        }

    }

    public void actionCheckLnch(){
        if (lice.getLnc()!=null && !lice.getLnc().trim().isEmpty()) {
            if(!ValidationUtils.isValidLNCH(lice.getLnc())){
                JSFUtils.addMessage("dossierForm:lnchLiceNew", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages,"refCorr.msgValidLnch"));
            } else {
                try {

                    if (liceDAO.chckByEgnLnc(null, lice.getLnc(), lice.getId())) {
                        JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.liceExistLnch")+ "("+lice.getLnc()+")");
                        lice.setEgn(null);
                        lice.setLnc(null);
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorCheckLice"));
                } finally {
                    JPA.getUtil().closeConnection();
                }
            }
        }
    }

    private boolean checkDataLice(String newLice) {

        boolean save = true;

        boolean isegn = false;
        //boolean islnch = false;

        if( (lice.getEgn()==null || lice.getEgn().trim().length()==0)
                && (lice.getLnc()==null || lice.getLnc().trim().length()==0)){
            JSFUtils.addMessage("dossierForm:egnLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,"ЕГН или ЛНЧ"));
            save = false;
        } else {
            if(lice.getEgn()!=null && lice.getEgn().trim().length()>0){
                if(!ValidationUtils.isValidEGN(lice.getEgn())){
                    JSFUtils.addMessage("dossierForm:egnLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages,"refCorr.msgValidEgn"));
                    save = false;
                }
                if(save) {isegn =true;}
            } else if(lice.getLnc()!=null && lice.getLnc().trim().length()>0){
                if(!ValidationUtils.isValidLNCH(lice.getLnc())){
                    JSFUtils.addMessage("dossierForm:lnchLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages,"refCorr.msgValidLnch"));
                    save = false;
                }
               // if(save) {islnch =true;}
            }

        }

        if(save){
            try {
                if(liceDAO.chckByEgnLnc(lice.getEgn() ,lice.getLnc() ,lice.getId())){
                    save = false;
                    if(isegn) {
                        JSFUtils.addMessage("dossierForm:egnLice" + newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.liceExist"));
                    } else {
                        JSFUtils.addMessage("dossierForm:lnchLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages,"dossier.liceExist"));
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при проверка в базата за лице!");
                save = false;
            } finally {
                JPA.getUtil().closeConnection();
            }

        }


        if( lice.getBirthDate()==null) {
            JSFUtils.addMessage("dossierForm:birthDateLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "dossier.dateOfBirth")));
            save = false;
        }

        if( lice.getFirstname()==null || lice.getFirstname().trim().length()==0) {
            JSFUtils.addMessage("dossierForm:firstnameLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "dossier.ime")));
            save = false;
        }

        if( lice.getSurname()==null || lice.getSurname().trim().length()==0) {
            JSFUtils.addMessage("dossierForm:surnameLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "dossier.prezime")));
            save = false;
        }

        if( lice.getLastname()==null || lice.getLastname().trim().length()==0) {
            JSFUtils.addMessage("dossierForm:lastnameLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "dossier.familiya")));
            save = false;
        }

        if( lice.getBirthPlace()==null || lice.getBirthPlace().trim().length()==0) {
            JSFUtils.addMessage("dossierForm:birthPlaceLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "dossier.placeOfBirth")));
            save = false;
        }

        if( lice.getUniversitet()==null) {
            JSFUtils.addMessage("dossierForm:unibitL"+newLice+":аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "dossier.universitet")));
            save = false;
        }

        if( ekatte==null) {
            JSFUtils.addMessage("dossierForm:mestoL"+newLice+":аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(UI_LABELS, "general.ekatte")));
            save = false;
        }

        if( lice.getAddrText()==null || lice.getAddrText().trim().length()==0) {
            JSFUtils.addMessage("dossierForm:adresLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(UI_LABELS, "general.adres")));
            save = false;
        }
        if (newLice.isEmpty()) {

            if(lice.getStatus()!=null && lice.getStatus() == CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN && lice.getStatusPri()==null) {
                JSFUtils.addMessage("dossierForm:statusPriLice"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "dossier.statusPri")));
                save = false;
            }

            if (!lice.getStatus().equals(lice.getDbStatus())) {

                try {
                    String err = liceDAO.checkStatusChangeAllowed(lice, getSystemData());
                    if (err != null) {
                        save = false;
                        JSFUtils.addMessage("dossierForm:statusLice" + newLice, FacesMessage.SEVERITY_ERROR, err);
                    }
                } catch (DbErrorException e) {
                    LOGGER.error(e.getMessage(), e);
                    JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при проверка в базата на статус на лице!");
                    save = false;
                } catch (InvalidParameterException e) {
                    LOGGER.error(e.getMessage(), e);
                    JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешни параметри за проверка статус на лице!", e.getMessage());
                    save = false;
                } finally {
                    JPA.getUtil().closeConnection();
                }

            }
        }


        return save;
    }


    private boolean checkDataZaqvIzpit(boolean isNew) {

        boolean save = true;

        if (zaiavIzpit.getRnDoc() == null || zaiavIzpit.getRnDoc().trim().length() == 0) {
            JSFUtils.addMessage("dossierForm:rnDocZIzpit" , FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.regNom")));
            save = false;
        } else {
            try {
                String pref = getSystemData().decodeItemLite(CODE_CLASSIF_DOC_VID, zaiavIzpit.getDocVid() ,getCurrentLang(),new Date(),false).getCodeExt();
                if( pref!=null &&  pref.trim().equals(zaiavIzpit.getRnDoc().trim()) ) {
                    JSFUtils.addMessage("dossierForm:rnDocZIzpit" , FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.regNom")));
                    save = false;
                }
            } catch (DbErrorException e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,e.getMessage());
                save = false;
            }
        }


        if (zaiavIzpit.getDocDate() == null) {
            JSFUtils.addMessage("dossierForm:docDateZIzpit", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.docDate")));
            save = false;
        }

        if (zaiavIzpit.getZaiavDate() == null) {
            JSFUtils.addMessage("dossierForm:zaiavDateZIzpit", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "dossier.dataPodavaneZayav")));
            save = false;
        }

        if(save && zaiavIzpit.getZaiavDate().after(zaiavIzpit.getDocDate())) {
            JSFUtils.addMessage("dossierForm:docDateZIzpit", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.dataRegNoBeforeDateZayav"));
            save = false;
        }

        if(zaiavIzpit.getDocDate() != null && !isNew){
            Object[] curIzpit = izpitiList.get(0);
            Date dateZap =   SearchUtils.asDate(curIzpit[6]);
            if(dateZap!=null && zaiavIzpit.getDocDate().after(dateZap)) {
                JSFUtils.addMessage("dossierForm:docDateZIzpit", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.dateZajavAfterDateZapIzpit"));
                save = false;
            }
        }

//        if (!lice.getStatus().equals(lice.getDbStatus())) {
//
//            try {
//                String err = liceDAO.checkStatusChangeAllowed(lice, getSystemData());
//                if (err != null) {
//                    save = false;
//                    JSFUtils.addMessage("dossierForm:statusLiceIzpit", FacesMessage.SEVERITY_ERROR, err);
//                }
//            } catch (DbErrorException e) {
//                LOGGER.error(e.getMessage(), e);
//                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при проверка в базата на статус на лице!");
//                save = false;
//            } catch (InvalidParameterException e) {
//                LOGGER.error(e.getMessage(), e);
//                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешни параметри за проверка статус на лице!", e.getMessage());
//                save = false;
//            } finally {
//                JPA.getUtil().closeConnection();
//            }
//
//        }

        return save;
    }


    public void actionNewDopStaj(){
        loadDataZajavStaj(false);
    }

    public void actionEditZaqvlenieStaj(){
        loadDataZajavStaj(true);
    }

    private void loadDataZajavStaj(boolean edit) {
        zaiavStaj = new Doc();
        zaiavStaj.setCodeSaglList(new ArrayList<>());
        staj = new Staj();

        Object[] curStaj = stajoveList.get(0);

        filesListRemoved = new ArrayList<>();
        filesList = new ArrayList<>();
        if(edit) {
            try {
                staj = stajDAO.findById(SearchUtils.asInteger(curStaj[0]));

                zaiavStaj = docDAO.findById(SearchUtils.asInteger(curStaj[15]));

                filesList = new FilesDAO(ud).selectByFileObject(zaiavStaj.getId(),CODE_ZNACHENIE_JOURNAL_DOC);


            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadStaj"));
            } finally {
                JPA.getUtil().closeConnection();
            }

        } else {

            staj.setStajVid(UriregConstants.CODE_ZNACHENIE_STAJ_VID_ADDITIONAL);
            staj.setOsnInstitution(SearchUtils.asInteger(curStaj[6]));

            zaiavStaj.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAIAV_DOP_STAJ);
            try {
                zaiavStaj.setRnDoc(getSystemData().decodeItemLite(CODE_CLASSIF_DOC_VID, CODE_ZNACHENIE_DOC_VID_ZAIAV_DOP_STAJ,getCurrentLang(),new Date(),false).getCodeExt());
            } catch (DbErrorException e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,e.getMessage());
            }
        }
        statusFromStaj = Integer.valueOf(lice.getDbStatus());

        loadStatusLiceStaj(!edit);

    }

    public void actionSaveZajavStaj(){

        boolean glD = true;
        if(staj.getId() == null){
            glD =checkDataEkate();
        }

        if(glD && checkDataStaj("")) {

            try {
                lice.setStatus(statusFromStaj.intValue());

                if (staj.getId() == null) {
                    //dopylnitelen
                    JPA.getUtil().runInTransaction(() -> {

                        stajDAO.createDopZayavStaj(zaiavStaj, lice, staj);

                        for (Files f : filesList) {
                            if (f.getId() == null) {
                                new FilesDAO(ud).saveFileObject(f, zaiavStaj.getId(), CODE_ZNACHENIE_JOURNAL_DOC);
                            }
                        }
                    });

                    JPA.getUtil().runWithClose(() -> {
                        zaiavStaj = docDAO.findById(zaiavStaj.getId());
                        if (zaiavStaj.getPrilVidList() != null) {
                            zaiavStaj.getPrilVidList().size();
                        }
                    });
                } else {
                    //redakciq
                    JPA.getUtil().runInTransaction(() -> {
                        stajDAO.save(staj);
                        docDAO.save(zaiavStaj, liceId);

                        for (Files f : filesList) {
                            if (f.getId() == null) {
                                new FilesDAO(ud).saveFileObject(f, zaiavStaj.getId(), CODE_ZNACHENIE_JOURNAL_DOC);
                            }
                        }

                        for (Files f : filesListRemoved) {
                            new FilesDAO(ud).deleteFileObject(f);
                        }

                        liceDAO.save(lice); //да запише статуса
                    });
                }

                JPA.getUtil().runWithClose(() -> stajoveList = stajDAO.findStajByLiceList(liceId));

                loadDatePoint();

                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

                String dialog = "PF('zayavStajMP').hide();";
                PrimeFaces.current().executeScript(dialog);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorSaveStaj"));
            }

        }
    }


    public void actionRemoveZaqvlenieStaj() {

        try {
            Object[] curStaj = stajoveList.get(0);

            deleteDoc(SearchUtils.asInteger(curStaj[15]));

            stajoveList.remove(0);

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dossier.deleteDopStaj"));

            JPA.getUtil().runWithClose(() ->{lice = liceDAO.findById(lice.getId());});

        }  catch (ObjectInUseException  e) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }  catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorDeleteStaj") ,e.getMessage());
        }


    }

    private void deleteDoc(Integer docId) throws BaseException {
        JPA.getUtil().runInTransaction(() -> {docDAO.deleteById(docId);});
    }

    private boolean checkDataStaj(String newLice) {

        boolean save = true;

        if (zaiavStaj.getRnDoc() == null || zaiavStaj.getRnDoc().trim().length() == 0) {
            JSFUtils.addMessage("dossierForm:rnDocStaj"+newLice , FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.regNom")));
            save = false;
        } else {
            try {
                String pref = getSystemData().decodeItemLite(CODE_CLASSIF_DOC_VID, zaiavStaj.getDocVid() ,getCurrentLang(),new Date(),false).getCodeExt();
                if(pref!=null && pref.trim().equals(zaiavStaj.getRnDoc().trim())) {
                    JSFUtils.addMessage("dossierForm:rnDocStaj"+newLice , FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.regNom")));
                    save = false;
                }
            } catch (DbErrorException e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,e.getMessage());
                save = false;
            }
        }

        if (zaiavStaj.getDocDate() == null) {
            JSFUtils.addMessage("dossierForm:docDateStaj"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.docDate")));
            save = false;
        } else if(staj.getId() != null){
            Object[] curStaj = stajoveList.get(0);
            Date dateZap =   SearchUtils.asDate(curStaj[5]);
            if(dateZap!=null && zaiavStaj.getDocDate().after(dateZap)) {
                JSFUtils.addMessage("dossierForm:docDateStaj", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.dateZajavAfterDateZapStaj"));
                save = false;
            }
        }

        if(staj.getOsnInstitution()==null) {
            if(staj.getStajVid().intValue() == CODE_ZNACHENIE_STAJ_VID_INITIAL) {
                JSFUtils.addMessage("dossierForm:sud" + newLice + ":аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "dossier.razprSudstaj")));
            } else {
                JSFUtils.addMessage("dossierForm:sud" + newLice + ":аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "dossier.razprSudstaj1")));
            }
            save = false;
        }

        if(staj.getStajVid().intValue() == CODE_ZNACHENIE_STAJ_VID_INITIAL) {
            if(staj.getMentor()!=null && !staj.getMentor().trim().isEmpty()){
                if(staj.getMentorEmail()== null || staj.getMentorEmail().trim().length()==0){
                    JSFUtils.addMessage("dossierForm:mentorEmail"+newLice, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "dossier.nastavnikEmail")));
                    save = false;
                }
            }

        }



        return save;
    }

    public void uploadFileListener(FileUploadEvent event){
        try {

            UploadedFile upFile = event.getFile();

            Files fileObject = new Files();
            fileObject.setFilename(upFile.getFileName());
            fileObject.setContentType(upFile.getContentType());
            fileObject.setContent(upFile.getContent());

            filesList.add(fileObject);

        } catch (Exception e) {
            LOGGER.error("Грешка при прикачване на файл", e.getMessage());
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "dossier.errorUploadFile"));
        }
    }

    public void uploadFileListenerDublikat(FileUploadEvent event){
        try {

            UploadedFile upFile = event.getFile();

            Files fileObject = new Files();
            fileObject.setFilename(upFile.getFileName());
            fileObject.setContentType(upFile.getContentType());
            fileObject.setContent(upFile.getContent());

            //-----------------------------------------------------------
            EgovContainer con = new EFormUtils().parseEform(upFile.getFileName(), upFile.getContent(), (SystemData)getSystemData());
            if(con!=null && con.getEgnLnch()!=null){
                //
                if(con.getEgnLnch().equals(lice.getEgn()) || con.getEgnLnch().equals(lice.getLnc())) {
                    zaiavDublikat.setDocInfo(con.getParsedInfo());
                    filesList.add(fileObject);
                } else {
                    //mess
                    JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,"ЕГН/ЛНЧ на лицето ("+(lice.getEgn()!=null && !lice.getEgn().isEmpty()?lice.getEgn():lice.getLnc())+") не съвпада с подаденото от ел. форми ("+con.getEgnLnch()+") !");
                }
            } else {
                filesList.add(fileObject);
            }
        } catch (Exception e) {
            LOGGER.error("Грешка при прикачване на файл", e.getMessage());
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "dossier.errorUploadFile"));
        }
    }

    public void actionRemoveFile(Files file){

        if(file!=null){
            if(file.getId()!=null){
                filesListRemoved.add(file);
            }
            filesList.remove(file);
        }

    }


    public Integer getEkatte() {
       // System.out.println("getEkatte ");
        return ekatte;
    }


    public void setEkatte(Integer ekatte) {
        //System.out.println("mqsto "+ekatte);
        lice.setAddrCountry(UriregConstants.CODE_ZNACHENIE_BG);
        lice.setAddrOblast(null);
        lice.setAddrObstina(null);

        if(ekatte!=null){
            lice.setAddrEkatte(ekatte);
        } else {
            lice.setAddrEkatte(null);
        }
        this.ekatte = ekatte;
    }

    public Map<Integer, Object> getSpecificsEKATTE() {
       return Collections.singletonMap(SysClassifAdapter.EKATTE_INDEX_TIP, 3);
    }

    public void actionRemoveDossier() {

        try {
            Object[] curStaj = stajoveList.get(stajoveList.size()-1);

            deleteDoc(SearchUtils.asInteger(curStaj[15]));

            actionNewDossierPerson();

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dossier.deleteDossier"));
        } catch (ObjectInUseException  e) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorDeleteDosie"),e.getMessage());
        }
    }


    public void download(Files files) {
        try {
            if (files.getId() != null){


                FilesDAO dao = new FilesDAO(ud);


                try {
                    files = dao.findById(files.getId());
                } finally {
                    JPA.getUtil().closeConnection();
                }

                if(files.getContent() == null){
                    files.setContent(new byte[0]);
                }
            }

            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
            String agent = request.getHeader("user-agent");

            String codedfilename = "";

            if (null != agent && (-1 != agent.indexOf("MSIE") || -1 != agent.indexOf("Mozilla") && -1 != agent.indexOf("rv:11") || -1 != agent.indexOf("Edge"))) {
                codedfilename = URLEncoder.encode(files.getFilename(), "UTF8");
            } else if (null != agent && -1 != agent.indexOf("Mozilla")) {
                codedfilename = MimeUtility.encodeText(files.getFilename(), "UTF8", "B");
            } else {
                codedfilename = URLEncoder.encode(files.getFilename(), "UTF8");
            }

            externalContext.setResponseHeader("Content-Type", "application/x-download");
            externalContext.setResponseHeader("Content-Length", files.getContent().length + "");
            externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
            externalContext.getResponseOutputStream().write(files.getContent());

            facesContext.responseComplete();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Отваря компонентата за генериране на УД
     * @param dublikat дали да генерира документа като дубликат
     */
    public void actionGenerateOrder(boolean dublikat) {
        setUdDublikat(dublikat);
        lastDublikatiId = null;
        if(dublikat){
            actionShowSprDub();
            if(dublikatiList!=null &&  dublikatiList.size()>0){
                lastDublikatiId = SearchUtils.asInteger(dublikatiList.get(0)[0]);
            }

        }
        this.bindCompUdostDoc.initComponent();
    }


    public void actionNewZaqvDublikat() {
        zaiavDublikat = new Doc();
        zaiavDublikat.setDocVid(CODE_ZNACHENIE_DOC_VID_ZAIAV_DUBL_UDOST);
        zaiavDublikat.setDocDate(new Date());

        try {
            zaiavDublikat.setRnDoc(getSystemData().decodeItemLite(CODE_CLASSIF_DOC_VID, CODE_ZNACHENIE_DOC_VID_ZAIAV_DUBL_UDOST,getCurrentLang(),new Date(),false).getCodeExt());
        } catch (DbErrorException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,e.getMessage());
        }

        filesListRemoved = new ArrayList<>();
        filesList = new ArrayList<>();
    }

    public void actionSaveDublikat(){

        if(checkDataZaqvDublikat()) {
            try {

                JPA.getUtil().runInTransaction(() -> {

                    boolean isNew = (zaiavDublikat.getId() == null ? true : false);
                    docDAO.save(zaiavDublikat, liceId);

                    if (isNew) {
                        LiceDocDAO liceDocDao = new LiceDocDAO(ud);
                        LiceDoc liceDocZyav = new LiceDoc(liceId, zaiavDublikat.getId());
                        liceDocDao.save(liceDocZyav);

                        //----- слагаме флаг в УД че ще се прави дубликат
                        Doc udost = docDAO.findById(lice.getUdostId());
                        udost.setOriginal(CODE_ZNACHENIE_NE);
                        docDAO.saveUdost(lice,udost);
                    }

                    for(Files f:filesList){
                        if(f.getId()==null) {
                            new FilesDAO(ud).saveFileObject(f, zaiavDublikat.getId(), CODE_ZNACHENIE_JOURNAL_DOC);
                        }
                    }

                    udDocFileCreateDubl = true; //да се покаже бутона за генериране да файл
                    showBntVeiwDublikat = true; //да се покаже бутона за преглед на дубликати
                });

                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

                String dialog = "PF('zayavDublikatMP').hide();";
                PrimeFaces.current().executeScript(dialog);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorSaveIzpit"));
            }
        }

    }

    private boolean checkDataZaqvDublikat() {

        boolean save = true;

        if (zaiavDublikat.getRnDoc() == null || zaiavDublikat.getRnDoc().trim().length() == 0) {
            JSFUtils.addMessage("dossierForm:rnDocZDublikat" , FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.regNom")));
            save = false;
        } else {
            try {
                String pref = getSystemData().decodeItemLite(CODE_CLASSIF_DOC_VID, zaiavDublikat.getDocVid() ,getCurrentLang(),new Date(),false).getCodeExt();
                if(pref!=null && pref.trim().equals(zaiavDublikat.getRnDoc().trim())) {
                    JSFUtils.addMessage("dossierForm:rnDocZDublikat" , FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.regNom")));
                    save = false;
                }
            } catch (DbErrorException e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,e.getMessage());
                save = false;
            }
        }


        if (zaiavDublikat.getDocDate() == null) {
            JSFUtils.addMessage("dossierForm:docDateZDublikat", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.docDate")));
            save = false;
        }

        return save;
    }

    public void  actionShowSprDub(){
        try {
            //visibleModalDublikati=true;
            JPA.getUtil().runWithClose(()-> dublikatiList = docDAO.listDublikatByLice(liceId));
        } catch (BaseException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadDublikati"));
        }
    }

//    public void actionHideSprDub(){
//        visibleModalDublikati=false;
//    }

    public void  actionDeleteDub(Object[] doc){

        boolean delete = false;
        try {
            deleteDoc(SearchUtils.asInteger(doc[0]));
            dublikatiList.remove(doc);
            delete = true;
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dossier.окDeleteDublikat"));
        } catch (ObjectInUseException  e) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }  catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorDeleteDublikat") ,e.getMessage());
        }

        if(delete) {  //като мине изтриването ще презаредиме данните
            try {
                showBntVeiwDublikat =false;
                udDocFileCreateDubl = false;

                //брой на файлове дубликати
                Integer brDocFile = docDAO.countDocForFile(lice.getUdostId(), CODE_ZNACHENIE_FILE_PURPOSE_UD_DUPLICATE);
                //брой подадени заявления за дубликати
                Integer brDoc = docDAO.countDocByVidAndLice(CODE_ZNACHENIE_DOC_VID_ZAIAV_DUBL_UDOST, liceId);

                if (brDoc > 0) {
                    showBntVeiwDublikat = true;
                }

                if (brDoc.intValue() > brDocFile.intValue()) {
                    udDocFileCreateDubl = true;
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorDeleteDublikat"), e.getMessage());
            } finally {
                JPA.getUtil().closeConnection();
            }
        }
    }

    public void afterGenerateUP() {
        udDocFileCreateOrig = false;
        udDocFileCreateDubl = false;

        if (udDublikat) {
            Files file = bindCompUdostDoc.getSavedFile();

            try {
                JPA.getUtil().runInTransaction(() -> {
                    new FilesDAO(ud).saveFileObject(file, lastDublikatiId, CODE_ZNACHENIE_JOURNAL_DOC);
                });
            } catch (BaseException e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
            }
        }
        PrimeFaces.current().ajax().update("dossierForm:liceDataPanel");
    }

    public void actionGenerateUDTest(){
        try {
            Doc ud = new Doc();
            ud.setDocDate(new Date());
            ud.setDocVid(CODE_ZNACHENIE_DOC_VID_UDOST_PRAVO);

            JPA.getUtil().runInTransaction(() -> {


                docDAO.saveUdost(lice ,ud);

            });

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));


        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorSaveIzpit"));
        }


    }

    private void loadStatusListLice(){
        try {
            statusList = getSystemData().getSysClassification(UriregConstants.CODE_CLASSIF_LICE_STATUS, new Date(), getCurrentLang());

            List <Integer> codes = new ArrayList<>();
            codes.add(CODE_ZNACHENIE_LICE_STATUS_CANDIDATE);
            codes.add(CODE_ZNACHENIE_LICE_STATUS_CASE_APPROVED);
            codes.add(CODE_ZNACHENIE_LICE_STATUS_EXAM_PASSED);
            codes.add(CODE_ZNACHENIE_LICE_STATUS_EXAM_FAILED);
            codes.add(CODE_ZNACHENIE_LICE_STATUS_PRAVO);

            if(lice.getStatus()!=null){
                codes.remove(lice.getStatus());
            }
            for(Integer code:codes){
                if( !(code.intValue()==CODE_ZNACHENIE_LICE_STATUS_PRAVO && lice.getStatus()!=null && lice.getStatus().intValue() == CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN)){
                    removeStatus(code);
                }

            }
        } catch (DbErrorException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void loadStatusLiceStaj(boolean newDopStaj){
        try {
            statusList = new ArrayList<>();

            List <SystemClassif> items  = getSystemData().getSysClassification(UriregConstants.CODE_CLASSIF_LICE_STATUS, new Date(), getCurrentLang());
            if(lice!=null && lice.getDbStatus()!=null) {
                if(newDopStaj){
                    statusFromStaj = CODE_ZNACHENIE_LICE_STATUS_CANDIDATE; //поставяме по подразбиране кандидат
                    for (SystemClassif item : items) {
                        if (item.getCode() == CODE_ZNACHENIE_LICE_STATUS_CANDIDATE || item.getCode() == CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED) {
                            statusList.add(item);
                        }
                    }
                } else {
                    for (SystemClassif item : items) {
                        if (item.getCode() == lice.getDbStatus().intValue() || item.getCode() == CODE_ZNACHENIE_LICE_STATUS_CANDIDATE || item.getCode() == CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED) {
                            statusList.add(item);
                        }
                    }
                }
            } else {
                for (SystemClassif item : items) {
                    if (item.getCode() == CODE_ZNACHENIE_LICE_STATUS_CANDIDATE || item.getCode() == CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED) {
                        statusList.add(item);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void loadStatusLiceIzpit(){
        try {
            statusList = new ArrayList<>();

            if(lice.getDbStatus()!=null) {
                List <SystemClassif> items  = getSystemData().getSysClassification(UriregConstants.CODE_CLASSIF_LICE_STATUS, new Date(), getCurrentLang());
                for (SystemClassif item : items) {
                    if (item.getCode() == lice.getDbStatus().intValue() || item.getCode() == CODE_ZNACHENIE_LICE_STATUS_TEST_APPROVED) {
                        statusList.add(item);
                    }
                }
            } else {
                LOGGER.error("loadStatusLiceIzpit() -> Lice status is null");
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void actionSprRegix(){

        try {
            liceRegix = new Lice();
            //
            String perm = getSystemData().getSettingsValue("regix.GRAO.PersonDataSearch");

            if (perm!=null && perm.trim().equals("1") && lice.getEgn()!=null && !lice.getEgn().trim().isEmpty()) {

                String permAddres = getSystemData().getSettingsValue("regix.GRAO.PermanentAddressSearch");

                RegixUtils2.loadLiceByEgn(liceRegix, lice.getEgn(), true, (permAddres!=null && permAddres.equals("1")?true:false), getSystemData());
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadRegixLice"));
        }

    }

    public void actionSprRegixDoc(){
        try {
            liceDocRegix  = new LiceDocRegix(lice.getId());
            RegixUtils2.personCard(lice.getEgn() ,lcNum ,liceDocRegix,getSystemData(), getUserData(), lice.getId());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,e.getMessage());
        }
    }

    public Date getDateNow(){
        return new Date();
    }

    public Doc getZaiavStaj() {
        return zaiavStaj;
    }

    public void setZaiavStaj(Doc zaiavStaj) {
        this.zaiavStaj = zaiavStaj;
    }

    public Staj getStaj() {
        return staj;
    }

    public void setStaj(Staj staj) {
        this.staj = staj;
    }

    public Lice getLice() {
        return lice;
    }

    public void setLice(Lice lice) {
        this.lice = lice;
    }

    public Integer getLiceId() {
        return liceId;
    }

    public void setLiceId(Integer liceId) {
        this.liceId = liceId;
    }

    public List<Object[]> getStajoveList() {
        return stajoveList;
    }

    public void setStajoveList(List<Object[]> stajoveList) {
        this.stajoveList = stajoveList;
    }

    public Doc getZaiavIzpit() {
        return zaiavIzpit;
    }

    public void setZaiavIzpit(Doc zaiavIzpit) {
        this.zaiavIzpit = zaiavIzpit;
    }

    public List<Object[]> getIzpitiList() {
        return izpitiList;
    }

    public void setIzpitiList(List<Object[]> izpitiList) {
        this.izpitiList = izpitiList;
    }

//    public boolean isStarRed() {
//        return starRed;
//    }
//
//    public void setStarRed(boolean starRed) {
//        this.starRed = starRed;
//    }

    public List<Files> getFilesList() {
        return filesList;
    }

    public void setFilesList(List<Files> filesList) {
        this.filesList = filesList;
    }

    public List<Files> getFilesListRemoved() {
        return filesListRemoved;
    }

    public void setFilesListRemoved(List<Files> filesListRemoved) {
        this.filesListRemoved = filesListRemoved;
    }

    public String getUdDoc() {
        return udDoc;
    }

    public void setUdDoc(String udDoc) {
        this.udDoc = udDoc;
    }

    public List<SystemClassif> getStatusList() {
        return statusList;
    }

    public void setStatusList(List<SystemClassif> statusList) {
        this.statusList = statusList;
    }

    public CompUdostDokument getBindCompUdostDoc() {
        return bindCompUdostDoc;
    }

    public void setBindCompUdostDoc(CompUdostDokument bindCompUdostDoc) {
        this.bindCompUdostDoc = bindCompUdostDoc;
    }

    public boolean isUdDublikat() {
        return udDublikat;
    }

    public boolean isUdDocFileCreateOrig() {
        return udDocFileCreateOrig;
    }

    public void setUdDocFileCreateOrig(boolean udDocFileCreateOrig) {
        this.udDocFileCreateOrig = udDocFileCreateOrig;
    }

    public boolean isUdDocFileCreateDubl() {
        return udDocFileCreateDubl;
    }

    public void setUdDocFileCreateDubl(boolean udDocFileCreateDubl) {
        this.udDocFileCreateDubl = udDocFileCreateDubl;
    }

    public void setUdDublikat(boolean udDublikat) {
        this.udDublikat = udDublikat;
    }

    public Doc getZaiavDublikat() {
        return zaiavDublikat;
    }

    public void setZaiavDublikat(Doc zaiavDublikat) {
        this.zaiavDublikat = zaiavDublikat;
    }

    public boolean isShowBntStaj() {
        return showBntStaj;
    }

    public void setShowBntStaj(boolean showBntStaj) {
        this.showBntStaj = showBntStaj;
    }

    public boolean isShowBntEditStaj() {
        return showBntEditStaj;
    }

    public void setShowBntEditStaj(boolean showBntEditStaj) {
        this.showBntEditStaj = showBntEditStaj;
    }

    public boolean isShowBntIzpit() {
        return showBntIzpit;
    }

    public void setShowBntIzpit(boolean showBntIzpit) {
        this.showBntIzpit = showBntIzpit;
    }

    public boolean isShowBntEditIzpit() {
        return showBntEditIzpit;
    }

    public void setShowBntEditIzpit(boolean showBntEditIzpit) {
        this.showBntEditIzpit = showBntEditIzpit;
    }

    public Lice getLiceRegix() {
        return liceRegix;
    }

    public void setLiceRegix(Lice liceRegix) {
        this.liceRegix = liceRegix;
    }

    public String getLcNum() {
        return lcNum;
    }

    public void setLcNum(String lcNum) {
        this.lcNum = lcNum;
    }

    public LiceDocRegix getLiceDocRegix() {
        return liceDocRegix;
    }

    public void setLiceDocRegix(LiceDocRegix liceDocRegix) {
        this.liceDocRegix = liceDocRegix;
    }

    /**
     * Проверка за заключено досие
     * @param idObj
     * @return
     */
    private boolean checkForLock(Integer idObj) {
        boolean res = true;
        LockObjectDAO daoL = new LockObjectDAO();
        try {
            Object[] obj = daoL.check(ud.getUserId(), CODE_ZNACHENIE_JOURNAL_LICE, idObj);
            if (obj != null) {
                res = false;
                String msg = getSystemData().decodeItem(CODE_CLASSIF_ADMIN_STR, Integer.valueOf(obj[0].toString()), ud.getCurrentLang(), new Date())
                        + " / " + DateUtils.printDate((Date)obj[1]);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(beanMessages, "dossier.locked"), msg);
            }
        } catch (DbErrorException e) {
            LOGGER.error("Грешка при проверка за заключена преписка! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }
        return res;
    }

    /**
     * Заключване на досие, като преди това отключва всички обекти, заключени от потребителя
     * @param idObj
     */
    public void lockDossier(Integer idObj) {

        LockObjectDAO daoL = new LockObjectDAO();
        try {
            JPA.getUtil().runInTransaction(() ->
                    daoL.lock(ud.getUserId(), CODE_ZNACHENIE_JOURNAL_LICE, idObj, null)
            );
        } catch (BaseException e) {
            LOGGER.error("Грешка при заключване на досие! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }
    }


    /**
     * при излизане от страницата - отключва обекта и да го освобождава за актуализация от друг потребител
     */
    @PreDestroy
    public void unlockDossier(){
        if (!ud.isReloadPage() && isView != 1) {
            LockObjectDAO daoL = new LockObjectDAO();
            try {

                JPA.getUtil().runInTransaction(() ->
                        daoL.unlock(ud.getUserId())
                );
            } catch (BaseException e) {
                LOGGER.error("Грешка при отключване на досие! ", e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
            }
            ud.setPreviousPage(null);

        }
        ud.setReloadPage(false);
    }

    public Integer getIsView() {
        return isView;
    }

    public void setIsView(Integer isView) {
        this.isView = isView;
    }

    public Integer getStatusFromStaj() {
        return statusFromStaj;
    }

    public void setStatusFromStaj(Integer statusFromStaj) {
        this.statusFromStaj = statusFromStaj;
    }

    public Integer getStatusFromIzpit() {
        return statusFromIzpit;
    }

    public void setStatusFromIzpit(Integer statusFromIzpit) {
        this.statusFromIzpit = statusFromIzpit;
    }

    public boolean isShowBntVeiwDublikat() {
        return showBntVeiwDublikat;
    }

    public void setShowBntVeiwDublikat(boolean showBntVeiwDublikat) {
        this.showBntVeiwDublikat = showBntVeiwDublikat;
    }

    public List<Object[]> getDublikatiList() {
        return dublikatiList;
    }

    public void setDublikatiList(List<Object[]> dublikatiList) {
        this.dublikatiList = dublikatiList;
    }

    public Integer getLastDublikatiId() {
        return lastDublikatiId;
    }

    public void setLastDublikatiId(Integer lastDublikatiId) {
        this.lastDublikatiId = lastDublikatiId;
    }

//    public boolean isVisibleModalDublikati() {
//        return visibleModalDublikati;
//    }
//
//    public void setVisibleModalDublikati(boolean visibleModalDublikati) {
//        this.visibleModalDublikati = visibleModalDublikati;
//    }

    public Object[] getSelectedDocDubl() {
        return selectedDocDubl;
    }

    public void setSelectedDocDubl(Object[] selectedDocDubl) {
        this.selectedDocDubl = selectedDocDubl;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void actionNewMessageStaj(){
        brbr =1;

        String imenaLice = lice.getFirstname();

        if(lice.getLastname()!=null){
            imenaLice += " "+lice.getLastname();
        }

        messageText="С настоящото, на основание чл. 4, ал. 3 от Наредба № 1 от 1 февруари 2019 г. за придобиване на юридическа правоспособност, се уведомява "+imenaLice;
        messageText +=", че към представените в Министерство на правосъдието документи рег.N: ";

        try {
            Object[] curStaj = stajoveList.get(0);
            String nomer =  SearchUtils.asString(curStaj[2]);
            Date dateS = SearchUtils.asDate(curStaj[3]);
            if(dateS!=null){
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                nomer +=" / "+sdf.format(dateS);
            }
            messageText +=nomer;

            shabloniList = getSystemData().getSysClassification(UriregConstants.CODE_CLASSIF_MESSAGE_SHABLONI_STAJ, new Date(), getCurrentLang());
        } catch (Exception e) {
            LOGGER.error("Грешка при actionNewMessageIzpit: {}", e.getMessage(), e);
        }
    }

    public void actionNewMessageIzpit(){

        brbr =1;

        String imenaLice = lice.getFirstname();

        if(lice.getLastname()!=null){
            imenaLice += " "+lice.getLastname();
        }

        messageText="С настоящото, на основание чл. 23, ал. 5 от Наредба № 1 от 1 февруари 2019 г. за придобиване на юридическа правоспособност, се уведомява  "+imenaLice;
        messageText +=", че към представените в Министерство на правосъдието документи рег. N: ";

        try {
            Object[] curIzpit = izpitiList.get(0);
            String nomer =  SearchUtils.asString(curIzpit[1]);
            Date dateS = SearchUtils.asDate(curIzpit[2]);
            if(dateS!=null){
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                nomer +=" / "+sdf.format(dateS);
            }
            messageText +=nomer;

            shabloniList = getSystemData().getSysClassification(CODE_CLASSIF_MESSAGE_SHABLONI_IZPIT, new Date(), getCurrentLang());
        } catch (Exception e) {
            LOGGER.error("Грешка при actionNewMessageIzpit: {}", e.getMessage(), e);
        }
    }

    public void actionPublishMessage(){

        if(messageText==null || messageText.isEmpty()){
            //messs
            JSFUtils.addMessage("dossierForm:msgMsg" , FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "messagesFilterList.text")));
        } else {
            Message message = new Message();

            message.setMessageVid(CODE_ZNACHENIE_MESSAGE_VID_UVED);
            message.setStatus(CODE_ZNACHENIE_MESSAGE_ACTIVE);
            message.setDateFrom(new Date());

            MessageLang mesLang  = new MessageLang();

            mesLang.setTitle("Уведомление");
            mesLang.setMessageText(messageText);
            mesLang.setLang(ud.getCurrentLang());

            ArrayList<MessageLang> mesLangs = new ArrayList<>();
            mesLangs.add(mesLang);
            message.setMessageLangs(mesLangs);


            try {
                JPA.getUtil().runInTransaction(() -> {new MessageDAO(ud).saveMessageWithLangs(message);});

                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "general.publok"));


                String dialog = "PF('messagesMP').hide();";
                PrimeFaces.current().executeScript(dialog);
            } catch (Exception e) {
                LOGGER.error("Грешка при запис: {}", e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при запис на съобщение!");
            }

        }

    }
    private int brbr=1;
    public void actionAddShablonText () {
        if(shablonText!=null && !shablonText.isEmpty()){
            messageText +="<p>"+brbr+". "+shablonText+"</p>";

            brbr =++brbr;
        }
    }

    public List<SystemClassif> getShabloniList() {
        return shabloniList;
    }

    public void setShabloniList(List<SystemClassif> shabloniList) {
        this.shabloniList = shabloniList;
    }

    public String getShablonText() {
        return shablonText;
    }

    public void setShablonText(String shablonText) {
        this.shablonText = shablonText;
    }

    public String getRnDocZapoved() {
        return rnDocZapoved;
    }

    public void setRnDocZapoved(String rnDocZapoved) {
        this.rnDocZapoved = rnDocZapoved;
    }

    public Date getDocDateZapoved() {
        return docDateZapoved;
    }

    public void setDocDateZapoved(Date docDateZapoved) {
        this.docDateZapoved = docDateZapoved;
    }

    public void actionZapovedStaj(){


        if(checkDataZapovedStaj()) {

            Object[] curStaj = stajoveList.get(0);
            Integer vidStaj = SearchUtils.asInteger(curStaj[1]);

            if (vidStaj != null) {
                boolean ok = true;
                try {

                    JPA.getUtil().begin();

                    Doc zapStaj = docDAO.findByNomDateVid(rnDocZapoved, docDateZapoved, vidStaj.intValue() == CODE_ZNACHENIE_STAJ_VID_INITIAL ? CODE_ZNACHENIE_DOC_VID_ZAP_STAJ : CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ);


                    if (zapStaj == null) {
                        //трябва да се създаде нова зеповед
                        zapStaj = new Doc();
                        zapStaj.setRnDoc(rnDocZapoved);
                        zapStaj.setDocDate(docDateZapoved);
                        zapStaj.setDocVid(vidStaj.intValue() == CODE_ZNACHENIE_STAJ_VID_INITIAL ? CODE_ZNACHENIE_DOC_VID_ZAP_STAJ : CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ);

                        zapStaj = docDAO.save(zapStaj);

                    }

                    Staj staj = stajDAO.findById(SearchUtils.asInteger(curStaj[0]));
                    staj.setZapStajId(zapStaj.getId());
                    stajDAO.save(staj);

                    LiceDoc liceDoc = new LiceDoc(liceId, zapStaj.getId());
                    new LiceDocDAO(ud).save(liceDoc);

                    lice.setStatus(CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED);
                    liceDAO.save(lice); //да запише статуса

                    JPA.getUtil().commit();
                    //-------------------------
                    curStaj[4] = rnDocZapoved;
                    curStaj[5] = docDateZapoved;

                    JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

                    loadDatePoint();

                    String dialog = "PF('zapovedStajMP').hide();";
                    PrimeFaces.current().executeScript(dialog);

                } catch (Exception e) {
                    ok = false;
                    JPA.getUtil().rollback();
                    LOGGER.error(e.getMessage(), e);
                    JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorLoadZapStaj"));
                } finally {
                    JPA.getUtil().closeConnection();
                }


                if(ok && lice.getUdostId()==null){
                   if( !((lice.getDo2019()==null || lice.getDo2019().intValue() != CODE_ZNACHENIE_DA) && lice.getBroiIzpit()!=null && lice.getBroiIzpit().intValue() >= 3)){
                       showBntIzpit = true;
                   }

                }
            }
        }
    }


    private boolean checkDataZapovedStaj() {

        boolean save = true;

        if (rnDocZapoved == null || rnDocZapoved.trim().length() == 0) {
            JSFUtils.addMessage("dossierForm:rnDocZapStaj" , FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.regNom")));
            save = false;
        }

        if (docDateZapoved == null) {
            JSFUtils.addMessage("dossierForm:docDateZapStaj", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.docDate")));
            save = false;
        } else {

            Object[] curStaj = stajoveList.get(0);
            Date dateZajav =   SearchUtils.asDate(curStaj[3]);

            if(docDateZapoved.before(dateZajav)) {
                JSFUtils.addMessage("dossierForm:docDateZapStaj", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.dateZapBeforeDateZajavStaj"));
                save = false;
            }

        }

        return save;
    }

    private void loadDatePoint(){
        if(stajoveList!=null && !stajoveList.isEmpty()){
            try {

                Object[] s=  stajoveList.get(0);
                String nomZap  = SearchUtils.asString(s[4]);
                Date dateZajav =   SearchUtils.asDate(s[3]);

                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                Date datePoit =  sdf.parse("16.01.2026");

                if( (nomZap== null || nomZap.trim().isEmpty()) && dateZajav!=null && dateZajav.before(datePoit)){
                    showBntAttachZapStaj = true;
                } else {
                    showBntAttachZapStaj = false;
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isShowBntAttachZapStaj() {
        return showBntAttachZapStaj;
    }

    public void setShowBntAttachZapStaj(boolean showBntAttachZapStaj) {
        this.showBntAttachZapStaj = showBntAttachZapStaj;
    }

    public Integer getStatusLice() {
        return statusLice;
    }

    public void setStatusLice(Integer statusLice) {
        this.statusLice = statusLice;
    }

    public void actionOpenPMStat(){
        statusLice = lice.getStatus();
    }

    public void actionSaveStatusLice(){

        if(statusLice!=null) {

            try {

                lice.setStatus(statusLice);

                JPA.getUtil().runInTransaction(() -> {
                    liceDAO.save(lice); //да запише статуса
                });

                showBntStaj = false;
                if(lice.getStatus().intValue() == CODE_ZNACHENIE_LICE_STATUS_EXAM_FAILED){
                    showBntStaj = true;
                }

                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

                String dialog = "PF('statusLiceMP').hide();";
                PrimeFaces.current().executeScript(dialog);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorSaveIzpit"));
            }

        }
    }

    public void actionLoadAddresDataRegix(){
        try {
            Lice liceTmp = new Lice();
            //
            String perm = getSystemData().getSettingsValue("regix.GRAO.PersonDataSearch");
            String permAddres = getSystemData().getSettingsValue("regix.GRAO.PermanentAddressSearch");

            if (perm!=null && perm.trim().equals("1") && lice.getEgn()!=null && !lice.getEgn().trim().isEmpty()) {


                RegixUtils2.loadLiceByEgn(liceTmp, lice.getEgn(), true, (permAddres!=null && permAddres.equals("1")?true:false), getSystemData());


                lice.setBirthPlace(liceTmp.getBirthPlace());

                lice.setAddrCountry(liceTmp.getAddrCountry());
                lice.setAddrOblast(liceTmp.getAddrOblast());
                lice.setAddrObstina(liceTmp.getAddrObstina());
                lice.setAddrText(liceTmp.getAddrText());
                lice.setAddrEkatte(liceTmp.getAddrEkatte());

                 ekatte = lice.getAddrEkatte();


            }

        } catch ( RegixClientException e){
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "general.regixClientError"));
        } catch ( DatatypeConfigurationException e){
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "general.regixClientError"));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dossier.errorCheckLice"));
        } finally {
            JPA.getUtil().closeConnection();
        }
    }

    private boolean checkDataEkate(){
        boolean save =true;
        if( lice.getAddrEkatte()==null || lice.getAddrText()==null || lice.getAddrText().trim().length()==0) {
            JSFUtils.addGlobalMessage( FacesMessage.SEVERITY_ERROR, "В личните данни на лицето липсва: "+getMessageResourceString(LABELS, "dossier.postAdres"));
            save = false;
        }

        if( lice.getBirthPlace()==null || lice.getBirthPlace().trim().length()==0) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "В личните данни на лицето липсва: "+getMessageResourceString(LABELS, "dossier.placeOfBirth"));
            save = false;
        }

        if( lice.getUniversitet()==null) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "В личните данни на лицето липсва: "+getMessageResourceString(LABELS, "dossier.universitet"));
            save = false;
        }

        return save;
    }

}
