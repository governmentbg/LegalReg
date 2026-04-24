package com.ib.urireg.beans;

import com.ib.indexui.system.Constants;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.urireg.components.CompUdostDokument;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.ValidationUtils;
import com.ib.urireg.db.dao.DocDAO;
import com.ib.urireg.db.dao.IzpitDAO;
import com.ib.urireg.db.dao.LockObjectDAO;
import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.db.dto.Izpit;
import com.ib.urireg.db.dto.IzpitResult;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.DateUtils;
import com.ib.urireg.system.UserData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.PrimeFaces;
import org.primefaces.event.*;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ib.urireg.system.UriregConstants.CODE_CLASSIF_DOC_VID;

@Named("examOrderEdit")
@ViewScoped
public class ExamOrderEdit extends IndexUIbean implements Serializable {

    /**
     * Въвеждане / актуализация на заповед за изпит
     *
     */
    private static final long serialVersionUID = -5355381786269390449L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExamOrderEdit.class);

    private static final String ID_OBJ = "idObj";
    private static final String FORM_EXAM_ORDER = "formExamOrder";

    private Date decodeDate = new Date();
    private UserData ud;

    private Doc orderExam;
    private Doc protTest;
    private Doc protCase;
    private Izpit exam;

    private List<SystemClassif> codeSaglClassif;

    private List<Files> filesList;
    private List<Files> filesListRemoved;

    private boolean existResultInProtTest;
    private boolean viewPanelProtCase;
    private boolean viewPersonsInProtTest;
    private boolean viewPersonsInProtCase;
    private boolean viewBtnInsertResultTest;
    private boolean viewBtnInsertResultCase;
    private boolean viewInsertResultTest;
    private boolean viewInsertResultCase;

    private List<Object[]> licaListForProtTest;
    private List<Object[]> licaListForProtCase;

    private boolean do2019;
    private int countEnteredTestResult;
    private int countEnteredCaseResult;

    private transient IzpitDAO examDAO;
    private transient DocDAO orderExamDAO;

    private List<Object[]> listResIzpitTest;//списък с резултати от изпит-тест
    private List<Object[]> listResIzpitCase;//списък с резултати от изпит-казус

    // за групово изтриване на лица в протокол-тест
    private List<Object[]> licaSelectedTestAll;
    private List<Object[]> licaSelectedTest;

    // за групово изтриване на лица в протокол до 2019
    private List<Object[]> licaSelectedCaseAll;
    private List<Object[]> licaSelectedCase;

    // за добавяне на лице в протокол
    private String egnLnch;
    private List<Object[]> licaForAdd;
    private Object[] addLice;

    private boolean viewBtnCase;
    private boolean notViewBtnAddLica;
    private boolean notViewBtnDelProtCase;
    private boolean notViewBtnDelOrderExam;

    // Компонента за генериране на УД
    private CompUdostDokument bindCompUdostDoc;
    private CompUdostDokument bindCompUdostDocUp;
    private Object[] liceUp;
    private Integer vidUdostDok;

    private boolean showInsertCaseRes;//за да се забрани въвеждането на резултати от изпит-казус, когато има издаден УП
    private int[] countResIzpitTest;//над протоколa за тест показва брой за издържли, неиздържали, неявили се.
    private int[] countResIzpitCase;//над протоколa за казус показва брой за издържли, неиздържали, неявили се.

    private boolean exportForSiteT;
    private boolean exportForSiteC;

    public Date getTimeTest() {
        return timeTest;
    }

    private Date timeTest;
    private Date timeCase;

    // Префикс на документа (ако има такъв)
    private String prefix;

    @PostConstruct
    public void init() {

        LOGGER.debug("PostConstruct - ExamOrderEdit!!!");

        try {

            this.ud = getUserData(UserData.class);

            this.examDAO = new IzpitDAO(getUserData());
            this.orderExamDAO = new DocDAO(getUserData());

            this.orderExam = new Doc();
            this.protTest = new Doc();
            this.protCase = new Doc();
            this.codeSaglClassif = new ArrayList<>();
            this.filesList = new ArrayList<>();
            this.filesListRemoved = new ArrayList<>();
            this.existResultInProtTest = false;
            this.viewPanelProtCase = false;
            this.viewPersonsInProtTest = false;
            this.viewPersonsInProtCase = false;
            this.viewBtnInsertResultTest = false;
            this.viewBtnInsertResultCase = false;
            this.viewInsertResultTest = false;
            this.viewInsertResultCase = false;
            this.exportForSiteT= false;
            this.exportForSiteC= false;
            this.licaListForProtTest = new ArrayList<>();
            this.licaListForProtCase = new ArrayList<>();
            this.listResIzpitTest = new ArrayList<>();
            this.listResIzpitCase = new ArrayList<>();
            this.licaSelectedTestAll = new ArrayList<>();
            this.licaSelectedTest = new ArrayList<>();
            this.licaSelectedCaseAll = new ArrayList<>();
            this.licaSelectedCase = new ArrayList<>();

            this.licaForAdd = new ArrayList<>();
            this.addLice = new Object[0];
            this.viewBtnCase = false;
            this.notViewBtnAddLica = false;
            this.notViewBtnDelProtCase = false;
            this.notViewBtnDelOrderExam = false;
            this.showInsertCaseRes = true;

            if (JSFUtils.getRequestParameter(ID_OBJ) != null && !"".equals(JSFUtils.getRequestParameter(ID_OBJ))) {

                Integer idObj = Integer.valueOf(JSFUtils.getRequestParameter(ID_OBJ));

                if (idObj != null) {

                    boolean checkLockOrderExam = true;

                    checkLockOrderExam = checkForLockOrderExam(idObj);

                    if (checkLockOrderExam) {

                        lockOrderExam(idObj);

                        JPA.getUtil().runWithClose(() -> {

                            this.exam = new Izpit();

                            this.exam = this.examDAO.findByZapIzpId(idObj); // Зареждане на данни на изпит

                            this.orderExam = this.orderExamDAO.findById(this.exam.getZapIzpId()); // Зареждане на данни за заповед за изпит
                            orderExam.getCodeSaglList().size();

                            this.existResultInProtTest = this.examDAO.isTestResultEntered(this.exam.getId());
                            this.countEnteredTestResult = this.examDAO.selectCountTestResultEntered(this.exam.getId());
                            this.countEnteredCaseResult = this.examDAO.selectCountCaseResultEntered(this.exam.getId());

                            if (this.exam.getTestProtId() != null) {
                                this.protTest = this.orderExamDAO.findById(this.exam.getTestProtId()); // Зареждане на данни за протокол тест
                                actionSearchTestResults();
                                if (this.countEnteredTestResult > 0 && this.countEnteredTestResult == this.listResIzpitTest.size()) {
                                    this.viewBtnCase = true;
                                }
                            }

                            if (this.exam.getCaseProtId() != null) {
                                this.protCase = this.orderExamDAO.findById(this.exam.getCaseProtId()); // Зареждане на данни за протокол казус
                                actionSearchCaseResults();
                                this.viewPanelProtCase = true;
                            }

                            // извличане на файловете от таблица с файловете
                            this.filesList = new FilesDAO(getUserData()).selectByFileObject(this.orderExam.getId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC);

                            //зареждане на съгласувалите
                            if (orderExam.getCodeSaglList() != null) {
                                for (Integer item : orderExam.getCodeSaglList()) {
                                    String tekst = "";
                                    SystemClassif scItem = new SystemClassif();

                                    scItem.setCodeClassif(UriregConstants.CODE_CLASSIF_ADMIN_STR);
                                    scItem.setCode(item);
                                    tekst = getSystemData().decodeItem(UriregConstants.CODE_CLASSIF_ADMIN_STR, item, getCurrentLang(), this.decodeDate);
                                    scItem.setTekst(tekst);
                                    this.codeSaglClassif.add(scItem);
                                }
                            }
                        });

                        if (this.orderExam.getDocVid().equals(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019)) {
                            this.do2019 = true;
                            this.viewPanelProtCase = true;
                        } else if (this.orderExam.getDocVid().equals(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT)) {
                            this.do2019 = false;
                        }

                        actionNotViewBtnAddLica();

                        actionNotViewBtnDelProtCase();

                        actionNotViewBtnDelOrderExam();

                        actionViewBtnInsertResult();

                        // инициализиране на часовете за изпит
                        if(exam.getTestDate() == null){
                            timeTest = intitTime10(new Date());
                        }else{
                            timeTest = exam.getTestDate();
                        }
                        if(exam.getCaseDate() == null){
                            timeCase = intitTime10(new Date());
                        }else{
                            timeCase = exam.getCaseDate();
                        }
                    }
                }

            } else { // Нова заповед за изпит
                this.exam = new Izpit();
                this.orderExam.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT);
                this.orderExam.setCodeIzgotvil(this.ud.getUserId());
                timeTest = intitTime10(new Date());
                timeCase = intitTime10(new Date());
            }

        } catch (BaseException e) {
            LOGGER.error("Грешка при зареждане данните на заповед за изпит и протоколите за тест и казус! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }

    }

    public void actionViewLicaInProt(){

        if(this.orderExam.getZaiavDate() != null) {

            try {

                JPA.getUtil().runWithClose(() -> {

                    if (!this.do2019) {
                        this.licaListForProtTest = this.examDAO.selectNewIzpitLica(this.orderExam);
                    } else {
                        this.licaListForProtCase = this.examDAO.selectNewIzpitLica(this.orderExam);
                    }
                });

            } catch (BaseException e) {
                LOGGER.error("Грешка при зареждане на лицата в протоколите за тест и казус! ", e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
            }
        }

    }

    public void actionSetPrefixToRnDoc() {

        try {

            this.prefix = getSystemData().decodeItemLite(CODE_CLASSIF_DOC_VID, this.orderExam.getDocVid(), getCurrentLang(), new Date(),false).getCodeExt();
            this.orderExam.setRnDoc(this.prefix);

        } catch (DbErrorException e) {
            LOGGER.error("Грешка при зареждане на префикс! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,"Грешка при зареждане на префикс!");
        }
    }

    private boolean checkData() {

        boolean save = false;

        if (this.orderExam.getZaiavDate() == null) {
            JSFUtils.addMessage(FORM_EXAM_ORDER + ":zaiavDate", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.zaiavDate")));
            save = true;

        } else {

            if (!SearchUtils.isEmpty(this.orderExam.getRnDoc())) {
                if (this.prefix != null && this.prefix.trim().equals(this.orderExam.getRnDoc().trim())) {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":nomOrder", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "traineeshipOrder.rNum"));
                    save = true;
                }

                if (this.orderExam.getDocDate() == null) {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateOrder", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.dateOrder")));
                    save = true;
                }
            }

            if (this.orderExam.getDocDate() != null) {

                if (SearchUtils.isEmpty(this.orderExam.getRnDoc())) {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":nomOrder", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.nomOrder")));
                    save = true;
                }

                if(this.orderExam.getDocDate().before(this.orderExam.getZaiavDate())) {
                   JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateOrder", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "examOrderEdit.dateOrderBeforeDateZaiav"));
                   save = true;
               }
            }

            if (this.exam.getTestDate() != null
                    && this.exam.getTestDate().before(this.orderExam.getZaiavDate())) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateAndHourExam", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "examOrderEdit.dateExamTestBeforeDateZaiav"));
                save = true;
            }

            if (this.exam.getTestDate() != null && this.exam.getCaseDate() != null
                    && (DateUtils.startDate(this.exam.getCaseDate()).compareTo(DateUtils.startDate(this.exam.getTestDate())) <= 0)) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateAndHourCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "examOrderEdit.dateExamCaseBeforeDateExamTest"));
                save = true;
            }
        }

        if (!this.do2019 && this.exam.getTestDate() == null) {
            JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateAndHourExam", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.dateAndHourExamTest")));
            save = true;
        }

        if (!this.do2019 && SearchUtils.isEmpty(this.exam.getTestLocation())) {
            JSFUtils.addMessage(FORM_EXAM_ORDER + ":testLocation", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.placeExamTest")));
            save = true;
        }

        if (this.exam.getCaseDate() == null) {
            if(!this.do2019) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateAndHourCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.dateAndHourExamCase")));
            } else {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateAndHourCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.dateAndHourExam")));
            }
            save = true;
        }

        if (SearchUtils.isEmpty(this.exam.getCaseLocation())) {
            if(!this.do2019) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":caseLocation", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.placeExamCase")));
            } else {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":caseLocation", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.placeExam")));
            }
            save = true;
        }

        if (this.protTest.getId() != null){

            // проверка за задължителните полета в протокол-тест
            if (this.protTest.getDocDate() == null) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtIzpit", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.dateProtTest")));
                save = true;

            } else {

                if (this.orderExam.getDocDate() != null
                        && this.protTest.getDocDate().before(this.orderExam.getDocDate())) {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtIzpit", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "examOrderEdit.dateProtTestBeforeDateOrder"));
                    save = true;
                }

                if (this.orderExam.getDocDate() == null && this.orderExam.getZaiavDate() != null
                        && this.protTest.getDocDate().before(this.orderExam.getZaiavDate())) {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtIzpit", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "examOrderEdit.dateProtTestBeforeDateZaiav"));
                    save = true;
                }

                if(this.exam.getTestDate() != null
                        && (DateUtils.startDate(this.protTest.getDocDate()).compareTo(DateUtils.startDate(this.exam.getTestDate())) < 0)) {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtIzpit", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "examOrderEdit.dateTestBeforeDateOrder"));
                    save = true;
                }
            }
        }

        if (this.viewPanelProtCase || this.protCase.getId() != null){

            //проверка за задължителните полета в протокол-казус
            if (this.protCase.getDocDate() == null) {

                if (this.do2019){
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString
                            (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "zapovedList.dataProtokol")));
                }else {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString
                            (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.dateProtCase")));
                }
                save = true;

            } else {

                if (!this.do2019 && this.protTest.getDocDate() != null
                        && this.protCase.getDocDate().before(this.protTest.getDocDate())) {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "examOrderEdit.dateProtCaseBeforeDateProtTest"));
                    save = true;
                }

                if (this.do2019 && this.orderExam.getDocDate() != null
                        && this.protCase.getDocDate().before(this.orderExam.getDocDate())) {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "examOrderEdit.dateProtCaseBeforeDateOrder"));
                    save = true;
                }

                if (this.do2019 && this.orderExam.getDocDate() == null && this.orderExam.getZaiavDate() != null
                        && this.protCase.getDocDate().before(this.orderExam.getZaiavDate())) {
                    JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "examOrderEdit.dateProtCaseBeforeDateZaiav"));
                    save = true;
                }

                if(this.exam.getCaseDate() != null
                        && (DateUtils.startDate(this.protCase.getDocDate()).compareTo(DateUtils.startDate(this.exam.getCaseDate())) < 0)) {

                    if (this.do2019){
                        JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "examOrderEdit.dateCaseBeforeDateTest1"));
                    } else {
                        JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateProtCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "examOrderEdit.dateCaseBeforeDateTest"));
                    }
                    save = true;
                }
            }
        }

        return save;
    }

    public void actionSave() {

        if(checkData()) {
            return;
        }

        try {
            // да сетне часовете за изпит в дата за изпит
            exam.setTestDate(timetoDateExam(exam.getTestDate(), timeTest));
            exam.setCaseDate(timetoDateExam(exam.getCaseDate(), timeCase));

            Integer orderExamId = this.orderExam.getId();

            //тук трябва да се преценява на записа кога какво се подава.
            JPA.getUtil().runInTransaction(() -> {

                if (this.orderExam.getId() == null) { // нова заповед за изпит
                    if (this.do2019){ // ако правят нова заповед по стария ред (до 2019) - ще се прави протокол-казус, а не протокол-тест
                        this.protCase.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT);
                        if (this.exam.getCaseDate() != null) {
                            this.protCase.setDocDate(this.exam.getCaseDate());
                        }
                        this.examDAO.saveIzpitData(this.exam, this.orderExam, null, this.protCase, this.licaListForProtCase);
                    } else { // ако е по новия ред - ще се записва първо протокол-тест
                        this.protTest.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT_TEST);
                        this.examDAO.saveIzpitData(this.exam, this.orderExam, this.protTest, null, this.licaListForProtTest);
                    }

                    if(this.filesList.size() > 0) {
                        for(Files f : this.filesList){
                            if(f.getId() == null) {
                                new FilesDAO(getUserData()).saveFileObject(f, this.orderExam.getId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC);
                            }
                        }
                    }

                } else { // редакция

                    this.countEnteredTestResult = this.examDAO.selectCountTestResultEntered(this.exam.getId());

                    if (this.do2019) { // ако редактират заповед по стария ред (до 2019) - ще се редактира само протокол-казус
                        this.examDAO.saveIzpitData(this.exam, this.orderExam, null, this.protCase, null);
                    } else {
                        this.existResultInProtTest = this.examDAO.isTestResultEntered(this.exam.getId()); // Проверява дали са въведени всички резултати от протокол тест

                        if(this.viewPanelProtCase && this.existResultInProtTest){  // Ще се записва протокол-казус, ако се вижда панела за протокол казус и има въведени резултати в протокол-тест!
                            if(this.protCase.getId() == null){
                                this.protCase.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT);
                            }
                            this.examDAO.saveIzpitData(this.exam, this.orderExam, this.protTest, this.protCase, null);

                            if (this.protCase.getId() != null && this.countEnteredTestResult > 0 && this.countEnteredTestResult == this.listResIzpitTest.size()) {
                                this.viewBtnCase = false;
                            }

                        } else{ // ако не се вижда протокол казус и не са въведени всички резултати в протокол-тест ще се записва само заповедта и протокол тест
                            this.examDAO.saveIzpitData(this.exam, this.orderExam, this.protTest, null, null);
                        }
                    }

                    if(this.filesList.size() > 0) {
                        for (Files f : this.filesList) {
                            if (f.getId() == null) {
                                new FilesDAO(getUserData()).saveFileObject(f, this.orderExam.getId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC);
                            }
                        }
                    }
                    if(this.filesListRemoved.size() > 0) {
                        for (Files f : this.filesListRemoved) {
                            new FilesDAO(getUserData()).deleteFileObject(f);
                        }
                        // да се зачисти списъка след изтриване
                        this.filesListRemoved = new ArrayList<>();
                    }
                }

            });

            // Ако е нов запис Роси каза да разтварям панела за записаните лица, ако се отваря през редация - те ще натискат бутон за зареждане на лицата
            if (orderExamId == null) {
                if (this.do2019) { // ако е заповед по стария ред се зареждат лицата в протокол казус
                    actionViewPersonsInProtCase();
                } else { // ако е заповед по новия ред ще се зареждат резултатите в протокол тест
                    actionViewPersonsInProtTest();
                }
            }

            actionNotViewBtnAddLica();

            actionNotViewBtnDelProtCase();

            actionViewBtnInsertResult(); // дали да се виждат бутоните за въвеждане на резултати

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

        } catch (BaseException e) {
            LOGGER.error("Грешка при запис на заповед за изпит! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void actionDelete(){

        try {

            JPA.getUtil().runInTransaction(() -> {

                if(this.filesList.size() > 0) {
                    for (Files f : this.filesList) {
                        new FilesDAO(getUserData()).deleteFileObject(f);
                    }
                }

                this.examDAO.deleteZapovedIzpit(this.exam, this.orderExam);
            });

            this.orderExam = new Doc();
            this.orderExam.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT);
            this.exam = new Izpit();
            this.protTest = new Doc();
            this.protCase = new Doc();
            this.codeSaglClassif = new ArrayList<>();
            this.filesList = new ArrayList<>();
            this.filesListRemoved = new ArrayList<>();
            this.do2019 = false;
            this.viewPanelProtCase = false;

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "examOrderEdit.successDeleteExam"));
            PrimeFaces.current().executeScript("scrollToErrors()");

        } catch (DbErrorException e) {
            LOGGER.error("Грешка при изтриване на заповед за изпит! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

        } catch (ObjectInUseException e) {
            LOGGER.error("Грешка при изтриване на заповед за изпит! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

        } catch (BaseException e) {
            LOGGER.error("Грешка при изтриване на заповед за изпит! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }

    }

    public void actionDeleteProtCase(){

        try {

            JPA.getUtil().runInTransaction(() ->  this.examDAO.deleteCaseProtocolSled2019(this.exam, this.orderExam));

            this.protCase = new Doc();

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "examOrderEdit.successDeleteProtCase"));
            PrimeFaces.current().executeScript("scrollToErrors()");

            this.viewPanelProtCase = false;
            this.viewBtnCase = true;

        } catch (DbErrorException e) {
            LOGGER.error("Грешка при изтриване на протокол-казус! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

        } catch (ObjectInUseException e) {
            LOGGER.error("Грешка при изтриване на протокол-казус! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

        } catch (BaseException e) {
            LOGGER.error("Грешка при изтриване на протокол-казус! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void actionViewPanelCase(){

        this.viewPanelProtCase = true;

        if (this.exam.getCaseDate() != null) {
            this.protCase.setDocDate(this.exam.getCaseDate());
        }

        if(!SearchUtils.isEmpty(this.protTest.getPredsedatel())) {
            this.protCase.setPredsedatel(this.protTest.getPredsedatel());
        }

        if(!SearchUtils.isEmpty(this.protTest.getMembers())) {
            this.protCase.setMembers(this.protTest.getMembers());
        }

        actionSave();

        if (this.protCase.getId() == null){
            this.viewPanelProtCase = false;
        }
    }

    public void actionViewPersonsInProtTest(){

        this.viewPersonsInProtTest = true;

        try {

            JPA.getUtil().runWithClose(() -> this.licaListForProtTest = this.examDAO.selectLicaByProtId(this.exam.getId(), this.protTest.getId()));

        } catch (BaseException e) {
            throw new RuntimeException(e);
        }
    }

    public void actionInsertResultsInTest(){

        this.viewPersonsInProtTest = false;
        this.viewInsertResultTest = true;
        actionSearchTestResults();
    }

    public void actionViewPersonsInProtCase(){

        this.viewPersonsInProtCase = true;

        try {

            JPA.getUtil().runWithClose(() -> {
                this.licaListForProtCase = this.examDAO.selectLicaByProtId(this.exam.getId(), this.protCase.getId());
            });

        } catch (BaseException e) {
            throw new RuntimeException(e);
        }
    }

    public void actionInsertResultsInCase(){

        this.viewPersonsInProtCase = false;
        this.viewInsertResultCase = true;
        actionSearchCaseResults();
        checkCreatedUP();
    }

    public void actionViewBtnInsertResult() {

        // Проверки дали да се покаже бутона "Въвеждане на резултати" за протокол-тест и протокол-казус:
        // да има дата на протокола и днешна дата да е след датата на изпита!

        Date today = DateUtils.startDate(new Date());

        // За да се покаже бутона "Въвеждане на резултати" за протокол-тест
        if(this.protTest.getDocDate() != null){
            Date protDate = DateUtils.startDate(this.protTest.getDocDate());

            if(today.compareTo(protDate) >= 0 ) {
                this.viewBtnInsertResultTest = true;
            }
        }

        // За да се покаже бутона "Въвеждане на резултати" за протокол-казус
        if(this.protCase.getDocDate() != null) {
            Date protDate = DateUtils.startDate(this.protCase.getDocDate());

            if(today.compareTo(protDate) >= 0 ) {
                this.viewBtnInsertResultCase = true;
            }
        }

    }

    public void actionChangeDo2019() {

        Date examDate = this.exam.getCaseDate();
        String examLocation = this.exam.getCaseLocation();

        if (this.do2019){
            this.orderExam.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019);
            if(this.exam.getTestDate() != null) {
                this.exam.setTestDate(null);
            }
            if(!SearchUtils.isEmpty(this.exam.getTestLocation())) {
                this.exam.setTestLocation(null);
            }
            this.exam.setCaseDate(examDate);
            this.exam.setCaseLocation(examLocation);
        } else {
            this.orderExam.setDocVid(UriregConstants.CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT);
            this.exam.setCaseDate(null);
            this.exam.setCaseLocation(null);
        }

        actionViewLicaInProt();
    }

    public void actionNotViewBtnDelProtCase() {

        Date today = DateUtils.startDate(new Date());
        Date dateProtCase = DateUtils.startDate(this.protCase.getDocDate());

        this.notViewBtnDelProtCase = dateProtCase != null && today.compareTo(dateProtCase) >= 0;
    }

    public void actionNotViewBtnAddLica() {

        Date today = DateUtils.startDate(new Date());
        Date dateProtTest = DateUtils.startDate(this.protTest.getDocDate());

        this.notViewBtnAddLica = dateProtTest != null && today.compareTo(dateProtTest) >= 0;
    }

    public void actionNotViewBtnDelOrderExam() {

        Date today = DateUtils.startDate(new Date());
        Date dateProtTest = DateUtils.startDate(this.protTest.getDocDate());
        Date dateProtCase = DateUtils.startDate(this.protCase.getDocDate());

        if (dateProtTest != null && today.compareTo(dateProtTest) > 0) {
            this.notViewBtnDelOrderExam = true;
        }

        if (dateProtCase != null && today.compareTo(dateProtCase) > 0) {
            notViewBtnDelOrderExam = true;
        }

    }

    //запис на редактиран ред от таблицата с резултати от тест или казус
    public void actionSaveResult(RowEditEvent<Object[]> event) {

        Object[] editedRow = event.getObject();
        IzpitResult editedResult = (IzpitResult) editedRow[10];

        try {

            JPA.getUtil().runInTransaction(() -> {
                editedRow[10] = examDAO.saveResult(editedResult, orderExam);

                this.countEnteredTestResult = this.examDAO.selectCountTestResultEntered(this.exam.getId());
                this.countEnteredCaseResult = this.examDAO.selectCountCaseResultEntered(this.exam.getId());
            });

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

            if (this.protTest.getId() != null) {
                actionInsertResultsInTest();
                if (this.countEnteredTestResult > 0 && this.countEnteredTestResult == this.listResIzpitTest.size()) {
                    this.viewBtnCase = true;
                } else {
                    this.viewBtnCase = false;
                }
            }

            if (this.protCase.getId() != null) {
                actionInsertResultsInCase();
                this.viewBtnInsertResultCase = true;
            }

        } catch (DbErrorException e) {
            LOGGER.error("Грешка при запис на ред от таблицата с резултати от тест! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
            PrimeFaces.current().executeScript("scrollToErrors()");

        } catch (InvalidParameterException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, "general.invalidParameter"), e.getMessage());
            PrimeFaces.current().executeScript("scrollToErrors()");

        } catch (BaseException e) {
            LOGGER.error("Грешка при запис на ред от таблицата с резултати от тест! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
            PrimeFaces.current().executeScript("scrollToErrors()");
        }
    }

    //за да вземе актуализираните стойности от страницата
    public IzpitResult getIzpitResultFromRow(Object[] row) {

        if (row != null && row.length > 10 && row[10] instanceof IzpitResult) {
            return (IzpitResult) row[10];
        }
        return null;
    }

    //зареждане на таблицата за въвеждане на резултати от изпит-тест
    public void actionSearchTestResults() {

        try {
            JPA.getUtil().runWithClose(() -> {
                listResIzpitTest = examDAO.findIzpitResults(this.exam.getId(), this.exam.getTestProtId());
                countResIzpitTest = examDAO.countIzpitResults(listResIzpitTest,UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT_TEST);
            });

        } catch (Exception e) {
            LOGGER.error("Грешка при търсене на резултати от изпит-тест!", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при търсене на резултати от изпит-тест!");
        }
    }

    //зареждане на таблицата за въвеждане на резултати от изпит-казус
    public void actionSearchCaseResults(){

        try {
            JPA.getUtil().runWithClose(() -> {
                listResIzpitCase = examDAO.findIzpitResults(Integer.valueOf(this.exam.getId()),this.exam.getCaseProtId());
                countResIzpitCase = examDAO.countIzpitResults(listResIzpitCase,UriregConstants.CODE_ZNACHENIE_DOC_VID_PROT);
            });
        } catch (Exception e) {
            LOGGER.error("Грешка при търсене на резултати от изпит-казус!", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при търсене на резултати от изпит-казус!");
        }
    }

    public void actionRemoveLicaFromProtocol(Integer protId, List<Object[]> selected) {

        try {

            JPA.getUtil().runInTransaction(() -> this.examDAO.removeLicaFromIzpit(this.exam, protId, selected));

            List<Integer> tmpList = new ArrayList<>();

            for (Object[] tmp : selected) {
                tmpList.add(Integer.valueOf(tmp[0].toString()));
            }

            Set<Integer> idSet = new HashSet<>(tmpList);

            if(this.protTest.getId() != null) {
                this.licaSelectedTestAll.removeIf(l -> idSet.contains(Integer.valueOf(l[0].toString())));
                this.licaListForProtTest.removeIf(l -> idSet.contains(Integer.valueOf(l[0].toString())));
            } else{
                this.licaSelectedCaseAll.removeIf(l -> idSet.contains(Integer.valueOf(l[0].toString())));
                this.licaListForProtCase.removeIf(l -> idSet.contains(Integer.valueOf(l[0].toString())));
            }

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "examOrderEdit.successRemoved"));
            PrimeFaces.current().executeScript("scrollToErrors()");

        } catch (DbErrorException e) {
            LOGGER.error("Грешка при премахване на лице/лица от протокол! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());

        } catch (ObjectInUseException e) {
            LOGGER.error("Грешка при премахване на лице/лица от протокол!", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());

        } catch (BaseException e) {
            LOGGER.error("Грешка при премахване на лице/лица от протокол!", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }
    }

    public void actionSearchLicaForAddToProtocol(){

        boolean search = true;

        try {

            if (SearchUtils.isEmpty(this.egnLnch)){
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":egnLnch", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "dossier.egnLnch")));
                return;

            } else {

                if (ValidationUtils.isValidEGN(this.egnLnch)) {
                    search = true;
                } else {
                    if(ValidationUtils.isValidLNCH(this.egnLnch)){
                        search = true;
                    } else{
                        search = false;
                    }
                }
            }

            if (!search) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":egnLnch", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "examOrderEdit.msgValidEgnLnch"));
                return;

            } else {

                JPA.getUtil().runWithClose(() -> this.addLice = this.examDAO.findLiceForIzpit(this.egnLnch, this.orderExam));

                if (this.addLice != null) {
                    this.licaForAdd.add(this.addLice);
                }
            }

        } catch (DbErrorException e) {
            LOGGER.error("Грешка при търсене за добавяне на лице към протокол! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());

        } catch (InvalidParameterException e) {
            LOGGER.error("Грешка при търсене за добавяне на лице към протокол! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());

        } catch (BaseException e) {
            LOGGER.error("Грешка при търсене за добавяне на лице към протокол! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }
    }

    public void actionClearLicaForAddToProtocol(){
         this.egnLnch = null;
         this.licaForAdd = new ArrayList();
    }

    public void actionAddLiceForProtocol(){

        try {

            JPA.getUtil().runInTransaction(() -> {

                if(this.protTest.getId() != null){
                    this.examDAO.addLiceToIzpit(this.exam, this.protTest.getId(), this.addLice);
                } else {
                    this.examDAO.addLiceToIzpit(this.exam, this.protCase.getId(), this.addLice);
                }

            });

            if(this.protTest.getId() != null) {
                this.licaListForProtTest.add(this.addLice);
                actionViewPersonsInProtTest();
            } else {
                this.licaListForProtCase.add(this.addLice);
                actionViewPersonsInProtCase();
            }

            PrimeFaces.current().executeScript("PF('modalAddLice').hide();");
            actionClearLicaForAddToProtocol();

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "examOrderEdit.successAdded"));
            PrimeFaces.current().executeScript("scrollToErrors()");

        } catch (BaseException e) {
            LOGGER.error("Грешка при добавяне на лице към протокол! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }

    }

    /**
     * Избира всички редове от текущата страница за протоколо-тест
     * @param event
     */
    public void onRowSelectAllTest(ToggleSelectEvent event) {

        ArrayList<Object[]> tmpList = new ArrayList<>();
        tmpList.addAll(getLicaSelectedTestAll());
        if(event.isSelected()) {
            for (Object[] object : getLicaSelectedTest()) {
                if(object != null && object.length > 0) {
                    boolean bo = true;
                    Integer l2 = Integer.valueOf(object[0].toString());
                    for (Object[] j : tmpList) {
                        Integer l1 = Integer.valueOf(j[0].toString());
                        if(l1.equals(l2)) {
                            bo = false;
                            break;
                        }
                    }
                    if(bo) {
                        tmpList.add(object);
                    }
                }
            }
        }else {
            List<Object[]> tmpLPageClass =  getLicaSelectedTestAll();// rows from current page....
            for (Object[] object : tmpLPageClass) {
                if(object != null && object.length > 0) {
                    Integer l2 =Integer.valueOf(object[0].toString());
                    for (Object[] j : tmpList) {
                        Integer l1 = Integer.valueOf(j[0].toString());
                        if(l1.equals(l2)) {
                            tmpList.remove(j);
                            break;
                        }
                    }
                }
            }
        }
        setLicaSelectedTestAll(tmpList);
        LOGGER.debug("onToggleSelect Test >>> ");
    }

    /**
     * Select one row for Prot Test
     * @param eventS
     */
    @SuppressWarnings("rawtypes")
    public void onRowSelectTest(SelectEvent eventS) {
        if(eventS!=null  && eventS.getObject()!=null) {
            List<Object[]> tmpLst =  getLicaSelectedTestAll();

            Object[] object = (Object[]) eventS.getObject();
            if(object != null && object.length > 0) {
                boolean bo = true;
                Integer l3 = Integer.valueOf(object[0].toString());
                for (Object[] j : tmpLst) {
                    Integer l1 = Integer.valueOf(j[0].toString());
                    if(l1.equals(l3)) {
                        bo = false;
                        break;
                    }
                }
                if(bo) {
                    tmpLst.add(object);
                    setLicaSelectedTestAll(tmpLst);
                }
            }
        }
        LOGGER.debug(String.format("onRowSelect Lica in Test >>> ", getLicaSelectedTestAll().size()));
    }

    /**
     * unselect one row for Prot Test
     * @param eventU
     */
    @SuppressWarnings("rawtypes")
    public void onRowUnselectTest(UnselectEvent eventU) {
        if(eventU!=null  && eventU.getObject()!=null) {
            Object[] object = (Object[]) eventU.getObject();
            ArrayList<Object[] > tmpLst = new ArrayList<>();
            tmpLst.addAll(getLicaSelectedTestAll());
            for (Object[] j : tmpLst) {
                if(j != null && j.length > 0
                        && object != null && object.length > 0) {
                    Integer l1 = Integer.valueOf(j[0].toString());
                    Integer l2 = Integer.valueOf(object[0].toString());
                    if(l1.equals(l2)) {
                        tmpLst.remove(j);
                        setLicaSelectedTestAll(tmpLst);
                        break;
                    }
                }
            }
        }
    }

    /**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга за протокол-тест
     */
    public void onPageUpdateSelectedTest(){
        if (getLicaSelectedTestAll() != null && !getLicaSelectedTestAll().isEmpty()) {
            getLicaSelectedTest().clear();
            getLicaSelectedTest().addAll(getLicaSelectedTestAll());
        }
    }

    public void actionChangeDateTest(){

        if (this.exam.getTestDate() != null) {
            this.protTest.setDocDate(this.exam.getTestDate());
        } else {
            this.protTest.setDocDate(null);
        }
    }
    public void actionChangeDateCase(){

        if (this.protCase.getId() != null && this.exam.getCaseDate() != null) {
            this.protCase.setDocDate(this.exam.getCaseDate());
        } else {
            this.protCase.setDocDate(null);
        }
    }

    /**
     * Избира всички редове от текущата страница за протокол до 2019
     * @param event
     */
    public void onRowSelectAllCase(ToggleSelectEvent event) {

        ArrayList<Object[]> tmpList = new ArrayList<>();
        tmpList.addAll(getLicaSelectedCaseAll());
        if(event.isSelected()) {
            for (Object[] object : getLicaSelectedCase()) {
                if(object != null && object.length > 0) {
                    boolean bo = true;
                    Integer l2 = Integer.valueOf(object[0].toString());
                    for (Object[] j : tmpList) {
                        Integer l1 = Integer.valueOf(j[0].toString());
                        if(l1.equals(l2)) {
                            bo = false;
                            break;
                        }
                    }
                    if(bo) {
                        tmpList.add(object);
                    }
                }
            }
        }else {
            List<Object[]> tmpLPageClass =  getLicaSelectedCaseAll();// rows from current page....
            for (Object[] object : tmpLPageClass) {
                if(object != null && object.length > 0) {
                    Integer l2 =Integer.valueOf(object[0].toString());
                    for (Object[] j : tmpList) {
                        Integer l1 = Integer.valueOf(j[0].toString());
                        if(l1.equals(l2)) {
                            tmpList.remove(j);
                            break;
                        }
                    }
                }
            }
        }
        setLicaSelectedCaseAll(tmpList);
        LOGGER.debug("onToggleSelect Case >>> ");
    }

    /**
     * Select one row for Prot Case
     * @param eventS
     */
    @SuppressWarnings("rawtypes")
    public void onRowSelectCase(SelectEvent eventS) {
        if(eventS!=null  && eventS.getObject()!=null) {
            List<Object[]> tmpLst =  getLicaSelectedCaseAll();

            Object[] object = (Object[]) eventS.getObject();
            if(object != null && object.length > 0) {
                boolean bo = true;
                Integer l3 = Integer.valueOf(object[0].toString());
                for (Object[] j : tmpLst) {
                    Integer l1 = Integer.valueOf(j[0].toString());
                    if(l1.equals(l3)) {
                        bo = false;
                        break;
                    }
                }
                if(bo) {
                    tmpLst.add(object);
                    setLicaSelectedCaseAll(tmpLst);
                }
            }
        }
        LOGGER.debug(String.format("1 onRowSelect Lica in Case >>> ", getLicaSelectedCaseAll().size()));
    }

    /**
     * unselect one row for Prot Case
     * @param eventU
     */
    @SuppressWarnings("rawtypes")
    public void onRowUnselectCase(UnselectEvent eventU) {
        if(eventU!=null  && eventU.getObject()!=null) {
            Object[] object = (Object[]) eventU.getObject();
            ArrayList<Object[] > tmpLst = new ArrayList<>();
            tmpLst.addAll(getLicaSelectedCaseAll());
            for (Object[] j : tmpLst) {
                if(j != null && j.length > 0
                        && object != null && object.length > 0) {
                    Integer l1 = Integer.valueOf(j[0].toString());
                    Integer l2 = Integer.valueOf(object[0].toString());
                    if(l1.equals(l2)) {
                        tmpLst.remove(j);
                        setLicaSelectedCaseAll(tmpLst);
                        break;
                    }
                }
            }
        }
    }

    /**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга за протокол до 2019
     */
    public void onPageUpdateSelectedCase(){
        if (getLicaSelectedCaseAll() != null && !getLicaSelectedCaseAll().isEmpty()) {
            getLicaSelectedCase().clear();
            getLicaSelectedCase().addAll(getLicaSelectedCaseAll());
        }
    }

    /**
     * Метод за издаване на УП. Подава се целия резултат от таблицата "Резултати от изпит-казус"
     */
    public void actionCreateUp(){

        try {
            JPA.getUtil().runInTransaction(() -> {
                examDAO.genLicaUdostDoc(listResIzpitCase,decodeDate);
                listResIzpitCase = examDAO.findIzpitResults(Integer.valueOf(this.exam.getId()),this.exam.getCaseProtId());
                showInsertCaseRes = false;
            });
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "examOrderEdit.createUP"));
        } catch (InvalidParameterException e) {
            LOGGER.error("Грешка при издаване на УП! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        } catch (DbErrorException e) {
            LOGGER.error("Грешка при издаване на УП! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        } catch (ObjectInUseException e) {
            LOGGER.error("Грешка при издаване на УП!", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        } catch (BaseException e) {
            LOGGER.error("Грешка при издаване на УП!", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }
    }

    /**
     * Проверява дали има издаден УП
     */
    private void checkCreatedUP(){
        if(listResIzpitCase!=null && !listResIzpitCase.isEmpty()){
            for(Object[] item : listResIzpitCase){
                if(item[13]!=null){
                    showInsertCaseRes = false;
                    break;
                }
            }
        }
    }

    /**
     * Проверява за всяко лице дали има файл на УП
     */
    public boolean checkForFile(Integer idDoc){
        boolean noFiles = false;
        try {
            noFiles = orderExamDAO.checkDocForFile(idDoc);
        } catch (DbErrorException e) {
            throw new RuntimeException(e);
        } finally {
            JPA.getUtil().closeConnection();
        }
        return noFiles;
    }

    public void actionViewFile(){

        // извличане на файловете от таблица с файловете
        try {

            JPA.getUtil().runWithClose(() -> {
                this.filesList = new FilesDAO(getUserData()).selectByFileObject(this.orderExam.getId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC);
            });

        } catch (BaseException e) {
            LOGGER.error("Грешка при извличане на файловете към заповедта за изпит! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    /**
     * За качване на файл с резултати. Използва се за резултати от тест, казус и по стария ред
     * За тест се подава 1,за казус 2, по стария ред 3
     */
    public void uploadFileResult(FileUploadEvent event){

        String vidIzpitStr = (String) event.getComponent().getAttributes().get("vidIzpit");
        Integer vidIzpit = Integer.valueOf(vidIzpitStr);
        if(vidIzpit==2 && do2019){//по стария ред
            vidIzpit=3;
        }
        UploadedFile file = event.getFile();
        if(vidIzpit == 1) {
            uploadFileResultSave(file, vidIzpit,  (ArrayList<Object[]>)listResIzpitTest); // за тест
        } else {
            uploadFileResultSave(file, vidIzpit,  (ArrayList<Object[]>)listResIzpitCase); // за казус и до 2019!
        }
    }

    /**
     * Запис на взетите от файл резултати
     */
    private void uploadFileResultSave( UploadedFile uplFile,Integer vidIzpit, ArrayList<Object[]> listResIzpit){
        byte[] bytes = uplFile.getContent();
        String result = examDAO.uploadIzpitResults(bytes,  listResIzpit, vidIzpit, (SystemData) getSystemData());
        if(result== null || result.isEmpty()){
            try {
                JPA.getUtil().runInTransaction(() -> {
                    //запис на всеки ред
                    for(Object[] res : listResIzpit){
                        actionSaveResultUploadFile(res);
                    }
                    //Записване на качения файл към заповедта
                    Files file = new Files();
                    file.setFilename(uplFile.getFileName());
                    file.setContentType(uplFile.getContentType());
                    file.setContent(uplFile.getContent());
                    file = new FilesDAO(ud).saveFileObject(file, exam.getZapIzpId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC);

                    //обновяване на бройките
                    if(vidIzpit==1) {
                        this.countEnteredTestResult = this.examDAO.selectCountTestResultEntered(this.exam.getId());
                        this.existResultInProtTest = this.examDAO.isTestResultEntered(this.exam.getId());
                    }else if(vidIzpit==2 || vidIzpit==3){
                        this.countEnteredCaseResult = this.examDAO.selectCountCaseResultEntered(this.exam.getId());
                    }

                });
                actionViewFile(); //за да се презареди списъка с файлове на екрана
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

                if (this.protTest.getId() != null && vidIzpit==1) {
                    actionInsertResultsInTest();
                    if (this.countEnteredTestResult > 0 && this.countEnteredTestResult == listResIzpit.size()) {
                        this.viewBtnCase = true;
                    } else {
                        this.viewBtnCase = false;
                    }
                }

                if (this.protCase.getId() != null && (vidIzpit==2 || vidIzpit==3)) {
                    actionInsertResultsInCase();
                    this.viewBtnInsertResultCase = true;
                }
            } catch (DbErrorException e) {
                LOGGER.error("Грешка при запис на ред от таблицата с резултати от тест! ", e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
                PrimeFaces.current().executeScript("scrollToErrors()");

            } catch (InvalidParameterException e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, "general.invalidParameter"), e.getMessage());
                PrimeFaces.current().executeScript("scrollToErrors()");

            } catch (BaseException e) {
                LOGGER.error("Грешка при запис на ред от таблицата с резултати от тест! ", e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
                PrimeFaces.current().executeScript("scrollToErrors()");
            }
        }else{
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,result);
            PrimeFaces.current().executeScript("scrollToErrors()");
        }
    }

    //запис на един резултат
    public void actionSaveResultUploadFile(Object[] res) {

        Object[] editedRow = res;
        IzpitResult editedResult = (IzpitResult) editedRow[10];

        try {
                editedRow[10] = examDAO.saveResult(editedResult, orderExam);

        } catch (BaseException e) {
            LOGGER.error("Грешка при запис на резултат от тест! ", e);
        }
    }

    /******************************************************* FILES *******************************************************/

    public void uploadFileListener(FileUploadEvent event){
        try {

            UploadedFile upFile = event.getFile();

            Files fileObject = new Files();
            fileObject.setFilename(upFile.getFileName());
            fileObject.setContentType(upFile.getContentType());
            fileObject.setContent(upFile.getContent());

            this.filesList.add(fileObject);

        } catch (Exception e) {
            LOGGER.error("Грешка при прикачване на файл", e.getMessage());
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "dossier.errorUploadFile"));
        }
    }

    public void actionRemoveFile(Files file){

        if(file != null){
            if(file.getId() != null){
                boolean alreadyRemoved = filesListRemoved.stream().anyMatch(f -> f.getId().equals(file.getId()));
                if (!alreadyRemoved) {
                    filesListRemoved.add(file);
                }
                boolean alreadyExist = filesList.stream().anyMatch(f -> f.getId().equals(file.getId()));
                if (alreadyExist) {
                    this.filesList.remove(file);
                }
            }

        }
    }

    /**
     * Сваля избрания файл
     *
     * @param file
     */
    public void download(Files file) {

        try {
            if (file.getId() != null){

                FilesDAO dao = new FilesDAO(getUserData());

                try {
                    file = dao.findById(file.getId());
                } finally {
                    JPA.getUtil().closeConnection();
                }

                if(file.getContent() == null){
                    file.setContent(new byte[0]);
                }
            }

            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
            String agent = request.getHeader("user-agent");

            String codedfilename = "";

            if (null != agent && (-1 != agent.indexOf("MSIE") || -1 != agent.indexOf("Mozilla") && -1 != agent.indexOf("rv:11") || -1 != agent.indexOf("Edge"))) {
                codedfilename = URLEncoder.encode(file.getFilename(), "UTF8");
            } else if (null != agent && -1 != agent.indexOf("Mozilla")) {
                codedfilename = MimeUtility.encodeText(file.getFilename(), "UTF8", "B");
            } else {
                codedfilename = URLEncoder.encode(file.getFilename(), "UTF8");
            }

            externalContext.setResponseHeader("Content-Type", "application/x-download");
            externalContext.setResponseHeader("Content-Length", file.getContent().length + "");
            externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
            externalContext.getResponseOutputStream().write(file.getContent());

            facesContext.responseComplete();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /******************************************************* END FILES *******************************************************/

    /******************************************************** EXPORT *********************************************************/



    /**
     * за експорт в excel XLSX - protokol test
     */
    public void postProcessXLSXPersonsExamListTest(Object document) {
        String dateExam = exam.getTestDate() != null ? DateUtils.printDate(exam.getTestDate()) : "";
        String title = getMessageResourceString(LABELS, "examOrder.protTestTitle", dateExam);
        String zapoved = ""; // ако още не е въведена
        if(!SearchUtils.isEmpty(orderExam.getRnDoc()) && orderExam.getDocDate() != null ) {
            zapoved = " "+ orderExam.getRnDoc()+"/"+DateUtils.printDate(orderExam.getDocDate());
        }
        String dateRep = getMessageResourceString(LABELS, "examOrder.protTitle3");
        Object[] dopInfo =  new Object[]{getMessageResourceString(LABELS, "examOrder.protTitle2")+ zapoved};

        String predsedatel = getProtTest().getPredsedatel() == null ? "" : getProtTest().getPredsedatel().trim();
        String members = getProtTest().getMembers() == null ? "" : getProtTest().getMembers().trim();

        addExcelFooter((XSSFWorkbook) document, predsedatel, members);

        new CustomExpPreProcess().postProcessXLSX(document, title, dopInfo, dateRep, "LEFT");
    }

    /**
     * за експорт в excel XLSX - protokol test
     */
    public void postProcessXLSXPersonsExamListCase(Object document) {
        String title="";
        String dateExam = exam.getCaseDate() != null ? DateUtils.printDate(exam.getCaseDate()) : "";
        if(!do2019) {
            title = getMessageResourceString(LABELS, "examOrder.protCaseTitle",dateExam);
        } else{
            title = getMessageResourceString(LABELS, "examOrder.protDo2019Title",dateExam);
        }
        String zapoved = ""; // ако още не е въведена
        if(!SearchUtils.isEmpty(orderExam.getRnDoc()) && orderExam.getDocDate() != null ) {
            zapoved = " "+ orderExam.getRnDoc()+"/"+DateUtils.printDate(orderExam.getDocDate());
        }
        String dateRep = getMessageResourceString(LABELS, "examOrder.protTitle3");
        String predsedatel = getProtCase().getPredsedatel() == null ? "" : getProtCase().getPredsedatel().trim();
        String members = getProtCase().getMembers() == null ? "" : getProtCase().getMembers().trim();
        Object[] dopInfo =  new Object[]{getMessageResourceString(LABELS, "examOrder.protTitle2")+ zapoved};

        addExcelFooter((XSSFWorkbook) document, predsedatel, members );

        new CustomExpPreProcess().postProcessXLSX(document, title, dopInfo, dateRep, "LEFT");
    }


    private void addExcelFooter(Workbook wb,  String predsedatel, String members) {
        ArrayList<Object> footer = new ArrayList<>();
        footer.add("ИЗПИТНА КОМИСИЯ");
        footer.add("Председател: " + predsedatel);
        footer.add("Членове: ");
        if(members != null && !members.equals("")){
            Object [] membersList = members.split("\\r?\\n");
            int i = footer.size();
            for (Object m : membersList) {
                footer.add(m);
                i++;
            }
        }

        Sheet sheet = wb.getSheetAt(0);
        int lastRowNum = sheet.getLastRowNum();

        // Празен ред
        lastRowNum += 2;

        // Footer информация
        for (Object dop : footer) {
            Font boldFont =  wb.createFont();
            boldFont.setBold(true);
            Row footerRow = sheet.createRow(++lastRowNum);
            Cell cell = footerRow.createCell(0);
            cell.setCellValue(dop.toString());
            CellStyle cellStyleTitle =  wb.createCellStyle();
            cellStyleTitle.setFont(boldFont);
            cell.setCellStyle(cellStyleTitle);
            sheet.addMergedRegion(new CellRangeAddress(lastRowNum,lastRowNum,0,10));
        }
    }


    // Метод да ми форматира датата за име на файла в експорта
    public String getDocDateFormatted(Date dateFromPage) {
        if(dateFromPage != null) {
            return new SimpleDateFormat("yyyyMMdd").format(dateFromPage);
        } else{
            return "";
        }
    }

//    /**
//     * за експорт в pdf - добавя заглавие - за списък с лицата в протокола
//     */
//    public void preProcessPDFPersonsExamList(Object document)  {
//
//        try {
//
//            String title = getMessageResourceString(LABELS, "examOrder.personsExamList");
//            new CustomExpPreProcess().preProcessPDF(document, title, null, null, null);
//
//        } catch (UnsupportedEncodingException e) {
//            LOGGER.error(e.getMessage(),e);
//
//        } catch (IOException e) {
//            LOGGER.error(e.getMessage(),e);
//        }
//    }
//
//    /**
//     * за експорт в pdf
//     * @return
//     */
//    public PDFOptions pdfOptions() {
//        PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
//        return pdfOpt;
//    }

    /**
     * Генериране на заповед за изпит
     */
    public void actionGenerateOrder() {

        boolean generateOrder = false;
        this.vidUdostDok = this.do2019
                ? UriregConstants.CODE_ZNACHENIE_SHABLON_ZAPOVED_IZPIT_STAR
                : UriregConstants.CODE_ZNACHENIE_SHABLON_ZAPOVED_IZPIT;

        if (!this.do2019) {
            if (this.exam.getTestDate() == null) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateAndHourExam", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.dateAndHourExamTest")));
                generateOrder = true;
            }

            if (SearchUtils.isEmpty(this.exam.getTestLocation())) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":testLocation", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.placeExamTest")));
                generateOrder = true;
            }
        }

        if (this.exam.getCaseDate() == null) {
            if(!this.do2019) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateAndHourCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.dateAndHourExamCase")));
            } else {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":dateAndHourCase", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.dateAndHourExam")));
            }
            generateOrder = true;
        }

        if (SearchUtils.isEmpty(this.exam.getCaseLocation())) {
            if(!this.do2019) {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":caseLocation", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.placeExamCase")));
            } else {
                JSFUtils.addMessage(FORM_EXAM_ORDER + ":caseLocation", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "examOrder.placeExam")));
            }
            generateOrder = true;
        }

        if(!generateOrder){
            this.bindCompUdostDoc.initComponent();
        } else{
            return;
        }

    }

    public void actionGeneratePersonList() {
        this.vidUdostDok = UriregConstants.CODE_ZNACHENIE_SHABLON_PRILOJ_ZAPOVED_IZPIT;
        this.bindCompUdostDoc.initComponent();
    }

    public void actionGenerateLiceUp(Object[] liceData) {
        this.liceUp = liceData;
        this.bindCompUdostDocUp.initComponent();
    }

    public void afterGenerateLiceUp() {
        PrimeFaces.current().ajax().update("formExamOrder:enteredResultInProtCase");
        // TODO da se refre6ne tablicata s licata v protokola; da se skrie butonat?
    }


    /******************************************************* END EXPORT ******************************************************/

    /******************************************** ЗАКЛЮЧВАНЕ/ОТКЛЮЧВАНЕ НА ЗАПОВЕД ЗА ИЗПИТ ***********************************************************/

    /**
     * Заключване на заповед за изпит, като преди това отключва всички обекти, заключени от
     * потребителя
     *
     * @param idObj
     */
    public void lockOrderExam(Integer idObj) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("lockDoc! {}", ud.getPreviousPage());
        }
        LockObjectDAO daoL = new LockObjectDAO();

        try {
            JPA.getUtil().runInTransaction(() -> daoL.lock(ud.getUserId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC, idObj, null));
        } catch (BaseException e) {
            LOGGER.error("Грешка при заключване на заповед за изпит! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }
    }


    /**
     * Проверка за заключена заповед за изпит
     *
     * @param idObj
     * @return
     */
    private boolean checkForLockOrderExam(Integer idObj) {

        boolean res = true;
        LockObjectDAO daoL = new LockObjectDAO();
        try {

            Object[] obj = daoL.check(ud.getUserId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC, idObj);
            if (obj != null) {
                res = false;
                String msg = getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR,
                        Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date()) + " / "
                        + DateUtils.printDate((Date) obj[1]);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "examOrderEdit.locked"), msg);
            }

        } catch (DbErrorException e) {
            LOGGER.error("Грешка при проверка за заключена заповед за изпит! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }

        return res;
    }

    /**
     * при излизане от страницата - отключва обекта и да го освобождава за
     * актуализация от друг потребител
     */
    @PreDestroy
    public void unlockOrderExam() {

        if (!ud.isReloadPage()) {
            LockObjectDAO daoL = new LockObjectDAO();

            try {

                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(String.format("unlockData 0! %s", this.ud.getPreviousPage()));
                }

                JPA.getUtil().runInTransaction(() -> daoL.unlock(this.ud.getUserId()));

            } catch (BaseException e) {
                LOGGER.error("Грешка при отключване на заповед за изпит! ", e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
            }

            this.ud.setPreviousPage(null);
        }
        this.ud.setReloadPage(false);
    }

    /******************************************** END ЗАКЛЮЧВАНЕ/ОТКЛЮЧВАНЕ НА ЗАПОВЕД ЗА ИЗПИТ ***********************************************************/

    /**
     * по подразбиране часа да 10:00
     * @param date
     * @return
     */
    public Date intitTime10(Date date) {
        if (date == null){
            date = new Date();
        }
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        gc.set(Calendar.HOUR_OF_DAY, 10);
        gc.set(Calendar.MINUTE, 00);
        gc.set(Calendar.SECOND, 00);

        return gc.getTime();
    }

    /**
     * Слага часа в дата на изпита
     * @param dateExam
     * @param timeExam
     * @return
     */
    public Date timetoDateExam(Date dateExam, Date timeExam){
        if (dateExam == null){
            return null;
        }
        if(dateExam != null) {
            GregorianCalendar gc = new GregorianCalendar();
            Calendar time = Calendar.getInstance();
            if(timeExam == null) {
                timeExam = intitTime10(new Date());
            }
            time.setTime(timeExam);
            gc.setTime(dateExam);
            gc.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
            gc.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
            gc.set(Calendar.SECOND, 00);
            dateExam= gc.getTime();
        }
        return dateExam;
    }


    /******************************************************* GET & SET *******************************************************/

    public Date getDecodeDate() {
        return new Date(decodeDate.getTime()) ;
    }

    public void setDecodeDate(Date decodeDate) {
        this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
    }

    public UserData getUd() {
        return ud;
    }

    public void setUd(UserData ud) {
        this.ud = ud;
    }

    public Doc getOrderExam() {
        return orderExam;
    }

    public void setOrderExam(Doc orderExam) {
        this.orderExam = orderExam;
    }

    public Doc getProtTest() {
        return protTest;
    }

    public void setProtTest(Doc protTest) {
        this.protTest = protTest;
    }

    public Doc getProtCase() {
        return protCase;
    }

    public void setProtCase(Doc protCase) {
        this.protCase = protCase;
    }

    public Izpit getExam() {
        return exam;
    }

    public void setExam(Izpit exam) {
        this.exam = exam;
    }

    public List<SystemClassif> getCodeSaglClassif() {
        return codeSaglClassif;
    }

    public void setCodeSaglClassif(List<SystemClassif> codeSaglClassif) {
        this.codeSaglClassif = codeSaglClassif;
    }

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

    public boolean isExistResultInProtTest() {
        return existResultInProtTest;
    }

    public void setExistResultInProtTest(boolean existResultInProtTest) {
        this.existResultInProtTest = existResultInProtTest;
    }

    public boolean isViewPanelProtCase() {
        return viewPanelProtCase;
    }

    public void setViewPanelProtCase(boolean viewPanelProtCase) {
        this.viewPanelProtCase = viewPanelProtCase;
    }

    public boolean isViewPersonsInProtTest() {
        return viewPersonsInProtTest;
    }

    public void setViewPersonsInProtTest(boolean viewPersonsInProtTest) {
        this.viewPersonsInProtTest = viewPersonsInProtTest;
    }

    public boolean isViewPersonsInProtCase() {
        return viewPersonsInProtCase;
    }

    public void setViewPersonsInProtCase(boolean viewPersonsInProtCase) {
        this.viewPersonsInProtCase = viewPersonsInProtCase;
    }

    public boolean isViewBtnInsertResultTest() {
        return viewBtnInsertResultTest;
    }

    public void setViewBtnInsertResultTest(boolean viewBtnInsertResultTest) {
        this.viewBtnInsertResultTest = viewBtnInsertResultTest;
    }

    public boolean isViewBtnInsertResultCase() {
        return viewBtnInsertResultCase;
    }

    public void setViewBtnInsertResultCase(boolean viewBtnInsertResultCase) {
        this.viewBtnInsertResultCase = viewBtnInsertResultCase;
    }

    public boolean isViewInsertResultTest() {
        return viewInsertResultTest;
    }

    public void setViewInsertResultTest(boolean viewInsertResultTest) {
        this.viewInsertResultTest = viewInsertResultTest;
    }

    public boolean isViewInsertResultCase() {
        return viewInsertResultCase;
    }

    public void setViewInsertResultCase(boolean viewInsertResultCase) {
        this.viewInsertResultCase = viewInsertResultCase;
    }

    public List<Object[]> getLicaListForProtTest() {
        return licaListForProtTest;
    }

    public void setLicaListForProtTest(List<Object[]> licaListForProtTest) {
        this.licaListForProtTest = licaListForProtTest;
    }

    public List<Object[]> getLicaListForProtCase() {
        return licaListForProtCase;
    }

    public void setLicaListForProtCase(List<Object[]> licaListForProtCase) {
        this.licaListForProtCase = licaListForProtCase;
    }

    public boolean isDo2019() {
        return do2019;
    }

    public void setDo2019(boolean do2019) {
        this.do2019 = do2019;
    }

    public int getCountEnteredTestResult() {
        return countEnteredTestResult;
    }

    public void setCountEnteredTestResult(int countEnteredTestResult) {
        this.countEnteredTestResult = countEnteredTestResult;
    }

    public int getCountEnteredCaseResult() {
        return countEnteredCaseResult;
    }

    public void setCountEnteredCaseResult(int countEnteredCaseResult) {
        this.countEnteredCaseResult = countEnteredCaseResult;
    }

    public List<Object[]> getListResIzpitTest() {
        return listResIzpitTest;
    }

    public void setListResIzpitTest(List<Object[]> listResIzpitTest) {
        this.listResIzpitTest = listResIzpitTest;
    }

    public List<Object[]> getListResIzpitCase() {
        return listResIzpitCase;
    }

    public void setListResIzpitCase(List<Object[]> listResIzpitCase) {
        this.listResIzpitCase = listResIzpitCase;
    }

    public List<Object[]> getLicaSelectedTestAll() {
        return licaSelectedTestAll;
    }

    public void setLicaSelectedTestAll(List<Object[]> licaSelectedTestAll) {
        this.licaSelectedTestAll = licaSelectedTestAll;
    }

    public List<Object[]> getLicaSelectedTest() {
        return licaSelectedTest;
    }

    public void setLicaSelectedTest(List<Object[]> licaSelectedTest) {
        this.licaSelectedTest = licaSelectedTest;
    }

    public List<Object[]> getLicaSelectedCaseAll() {
        return licaSelectedCaseAll;
    }

    public void setLicaSelectedCaseAll(List<Object[]> licaSelectedCaseAll) {
        this.licaSelectedCaseAll = licaSelectedCaseAll;
    }

    public List<Object[]> getLicaSelectedCase() {
        return licaSelectedCase;
    }

    public void setLicaSelectedCase(List<Object[]> licaSelectedCase) {
        this.licaSelectedCase = licaSelectedCase;
    }

    public String getEgnLnch() {
        return egnLnch;
    }

    public void setEgnLnch(String egnLnch) {
        this.egnLnch = egnLnch;
    }

    public List<Object[]> getLicaForAdd() {
        return licaForAdd;
    }

    public void setLicaForAdd(List<Object[]> licaForAdd) {
        this.licaForAdd = licaForAdd;
    }

    public Object[] getAddLice() {
        return addLice;
    }

    public void setAddLice(Object[] addLice) {
        this.addLice = addLice;
    }

    public boolean isViewBtnCase() {
        return viewBtnCase;
    }

    public void setViewBtnCase(boolean viewBtnCase) {
        this.viewBtnCase = viewBtnCase;
    }

    public boolean isNotViewBtnAddLica() {
        return notViewBtnAddLica;
    }

    public void setNotViewBtnAddLica(boolean notViewBtnAddLica) {
        this.notViewBtnAddLica = notViewBtnAddLica;
    }

    public boolean isNotViewBtnDelProtCase() {
        return notViewBtnDelProtCase;
    }

    public void setNotViewBtnDelProtCase(boolean notViewBtnDelProtCase) {
        this.notViewBtnDelProtCase = notViewBtnDelProtCase;
    }

    public boolean isNotViewBtnDelOrderExam() {
        return notViewBtnDelOrderExam;
    }

    public void setNotViewBtnDelOrderExam(boolean notViewBtnDelOrderExam) {
        this.notViewBtnDelOrderExam = notViewBtnDelOrderExam;
    }

    public CompUdostDokument getBindCompUdostDoc() {
        return bindCompUdostDoc;
    }

    public void setBindCompUdostDoc(CompUdostDokument bindCompUdostDoc) {
        this.bindCompUdostDoc = bindCompUdostDoc;
    }

    public CompUdostDokument getBindCompUdostDocUp() {
        return bindCompUdostDocUp;
    }

    public void setBindCompUdostDocUp(CompUdostDokument bindCompUdostDocUp) {
        this.bindCompUdostDocUp = bindCompUdostDocUp;
    }

    public boolean isShowInsertCaseRes() {
        return showInsertCaseRes;
    }

    public void setShowInsertCaseRes(boolean showInsertCaseRes) {
        this.showInsertCaseRes = showInsertCaseRes;
    }

    public Integer getVidUdostDok() {
        return vidUdostDok;
    }

    public Object[] getLiceUp() {
        return liceUp;
    }

    public int[] getCountResIzpitTest() {
        return countResIzpitTest;
    }

    public void setCountResIzpitTest(int[] countResIzpitTest) {
        this.countResIzpitTest = countResIzpitTest;
    }

    public int[] getCountResIzpitCase() {
        return countResIzpitCase;
    }

    public void setCountResIzpitCase(int[] countResIzpitCase) {
        this.countResIzpitCase = countResIzpitCase;
    }

    public boolean isExportForSiteC() {
        return exportForSiteC;
    }

    public void setExportForSiteC(boolean exportForSiteC) {
        this.exportForSiteC = exportForSiteC;
    }
    public boolean isExportForSiteT() {
        return exportForSiteT;
    }

    public void setExportForSiteT(boolean exportForSiteT) {
        this.exportForSiteT = exportForSiteT;
    }

    public void setTimeTest(Date timeTest) {
        this.timeTest = timeTest;
    }

    public Date getTimeCase() {
        return timeCase;
    }

    public void setTimeCase(Date timeCase) {
        this.timeCase = timeCase;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /***************************************************** END GET & SET *****************************************************/
}
