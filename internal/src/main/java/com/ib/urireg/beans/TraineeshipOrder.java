package com.ib.urireg.beans;

import com.ib.indexui.system.Constants;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.ValidationUtils;
import com.ib.urireg.components.CompUdostDokument;
import com.ib.urireg.db.dao.DocDAO;
import com.ib.urireg.db.dao.LockObjectDAO;
import com.ib.urireg.db.dao.StajDAO;
import com.ib.urireg.db.dto.Doc;
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
import org.primefaces.component.export.PDFOptions;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import static com.ib.urireg.beans.UnLockObjects.BEANMSG;
import static com.ib.urireg.system.UriregConstants.*;


/**
 * ОБРАБОТКА НА ЗАПОВЕДИ ЗА СТАЖ
 *
 * @author silvia
 */

@Named("traineeshipOrder")
@ViewScoped
public class TraineeshipOrder extends IndexUIbean {


    private static final Logger LOGGER = LoggerFactory.getLogger(TraineeshipOrder.class);
    private static final String ID_OBJ = "idObj";

    private static final String FORM = "traineeshipOrderForm";

    private UserData ud;

    //основен документ
    private Doc doc;

    private transient StajDAO stajDao;
    private transient DocDAO docDao;

    private Integer vidStaj;

    //списък с избраните лица за конкретната заповед
    private transient List<Object[]> slcLicaList;

    //всички одобрени лица
    private transient List<Object[]> odobreniLicaList;

    //избраните лица в модалния прозорец
    private transient List<Object[]> checkboxLicaList;

    private transient List<SystemClassif> codeSaglClassif;

    //за работа с файлове
    private List<Files> filesList;
    private List<Files> filesListRemoved;

    private boolean haveUD;

    // Компоненти за генериране на УД
    private transient CompUdostDokument bindCompUdostDoc;
    private transient CompUdostDokument bindCompUdostDocPrepis;
    private transient Object[] licePrepis;

    private boolean haveRnDoc;

    // Префикс на документа (ако има такъв)
    private String prefix;


    @PostConstruct
    private void init() {
        LOGGER.debug("TraineeshipOrder postConstruct");

        try {

            this.ud = getUserData(UserData.class);
            this.doc = new Doc();
            this.slcLicaList = new ArrayList<>();
            this.odobreniLicaList = new ArrayList<>();
            this.checkboxLicaList = new ArrayList<>();
            this.stajDao = new StajDAO(getUserData());
            this.docDao = new DocDAO(getUserData());
            this.codeSaglClassif = new ArrayList<>();
            this.filesList = new ArrayList<>();
            this.filesListRemoved = new ArrayList<>();
            this.licePrepis = null;
            this.haveRnDoc = false;

            this.doc.setDocVid(CODE_ZNACHENIE_DOC_VID_ZAP_STAJ); //първоначален след 2019
            setPrefix(getDocPrefix()); //префикса на вида документ (ако има такъв)
            this.doc.setCodeIzgotvil(this.ud.getUserId());
            setVidStaj(UriregConstants.CODE_ZNACHENIE_STAJ_VID_INITIAL);


            if (JSFUtils.getRequestParameter(ID_OBJ) != null && !"".equals(JSFUtils.getRequestParameter(ID_OBJ))) {
                Integer idDoc = Integer.valueOf(JSFUtils.getRequestParameter(ID_OBJ));

                if (idDoc != null) {
                    boolean checkLockDoc = true;

                    checkLockDoc = checkForLock(idDoc);
                    if (checkLockDoc) {
                        lock(idDoc);
                        loadDoc(idDoc); //зареждаме съществуваща заповед за актуализация
                    }
                }
            }

            //лицата се зареждат независимо дали е за актуализация по ИД или нова заповед с всички одобрени лица
            loadLica(this.doc.getId());


        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при инициализиране на заповеди за стаж!");
        }
    }


    /**
     * Зарежда префикс(codeExt) за съответния вид документ от класификация 104
     */
    private String getDocPrefix() {
        String result = "";
        try {

           result = getSystemData().decodeItemLite(CODE_CLASSIF_DOC_VID, this.doc.getDocVid() ,getCurrentLang(),new Date(),false).getCodeExt();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при зареждане на префикс!");
        }

        return result;
    }


    public void actionCopyPrefix () {
        this.doc.setRnDoc(prefix);
    }


    /**
     * Зареждане на заповед за актуализация
     */
    private void loadDoc (Integer id) {
        try {

            if (id != null) {

                JPA.getUtil().runWithClose(() -> {
                    this.doc = this.docDao.findById(id);
                    this.filesList = new FilesDAO(getUserData()).selectByFileObjectDop(this.doc.getId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC);
                });


                if (this.doc != null) {

                    setHaveUD(checkForUD());
                    if (this.doc.getRnDoc() != null && !this.doc.getRnDoc().isEmpty()) {
                       setHaveRnDoc(true);
                    }

                    if (this.doc.getDocVid() != null) {
                        if (this.doc.getDocVid().equals(CODE_ZNACHENIE_DOC_VID_ZAP_STAJ)) {
                            this.vidStaj = UriregConstants.CODE_ZNACHENIE_STAJ_VID_INITIAL;
                        } else {
                            this.vidStaj = UriregConstants.CODE_ZNACHENIE_STAJ_VID_ADDITIONAL;
                        }

                        if(doc.getCodeSaglList() != null) {
                            for(Integer item : doc.getCodeSaglList()) {
                                String tekst = "";
                                SystemClassif scItem = new SystemClassif();

                                scItem.setCodeClassif(CODE_CLASSIF_ADMIN_STR);
                                scItem.setCode(item);
                                tekst = getSystemData().decodeItem(CODE_CLASSIF_ADMIN_STR, item, getCurrentLang(), new Date());
                                scItem.setTekst(tekst);
                                this.codeSaglClassif.add(scItem);
                            }
                        }

                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при зареждане на информация за заповед!");
        }
    }



    /**
     * Основен запис на целия обект
     */
    public void actionSave () {
        if (checkData()) {
            try {

                JPA.getUtil().runInTransaction(() -> {

                    this.doc = this.stajDao.saveStajList(this.doc, this.slcLicaList);

                    //ако до сега е нямало и сега са записали да не се омажат следващите действия
                    if (this.doc.getRnDoc() != null && !this.doc.getRnDoc().isEmpty()) {
                        setHaveRnDoc(true);
                    }

                    if (!this.filesList.isEmpty()) {
                        for(Files file : filesList){
                            if(file.getId()==null) {
                                new FilesDAO(getUserData()).saveFileObject(file, this.doc.getId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC);
                            }
                        }
                    }

                    if (!this.filesListRemoved.isEmpty()) {
                        for(Files file:filesListRemoved){
                            new FilesDAO(getUserData()).deleteFileObject(file);
                        }
                    }

                });


                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
                        getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

                this.filesListRemoved = new ArrayList<>();
                loadLica(this.doc.getId());


            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                        "Грешка при запис на заповед!");
            }
        }
    }


    /**
     * Проверка на въведените данни
     *
     * @return true ако данните са валидни, false в противен случай
     */
    private boolean checkData() {
        boolean result = true;

        //ако няма въведен номер на заповед минава проверка дали след префикса има номер
        if (!haveRnDoc) {
            if (this.prefix != null && this.prefix.trim().equals(this.doc.getRnDoc().trim())) {
                JSFUtils.addMessage(FORM + ":rNomer", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "traineeshipOrder.rNum"));
                result = false;
            }
        }

        if (this.vidStaj == null) {
            JSFUtils.addMessage(FORM + ":vidDoc", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "traineeshipOrder.vid")));
            result = false;
        }

        if (this.slcLicaList == null || this.slcLicaList.isEmpty()) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "traineeshipOrder.insLica"));
            result = false;
        }

        if (haveRnDoc) {
            if (SearchUtils.isEmpty(this.doc.getRnDoc())) {
                JSFUtils.addMessage(FORM + ":rNomer", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "traineeshipOrder.zapovedNo")));
                result = false;
            }

            if (this.doc.getDocDate() == null) {
                JSFUtils.addMessage(FORM + ":zapovedDataOt", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(UI_LABELS, "general.date")));
                result = false;
            }
        } else {

            //29.01.26г. по искане на Дани Василева
            //да не може да се въвежда само номер или само дата.. ако едното се въведе трябва и другото да е въведено
            if (!SearchUtils.isEmpty(this.doc.getRnDoc())) {
                if (this.doc.getDocDate() == null) {
                    JSFUtils.addMessage(FORM + ":zapovedDataOt", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(UI_LABELS, "general.date")));
                    result = false;
                }
            }

            if (this.doc.getDocDate() != null) {
                if (SearchUtils.isEmpty(this.doc.getRnDoc())) {
                    JSFUtils.addMessage(FORM + ":rNomer", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "traineeshipOrder.zapovedNo")));
                    result = false;
                }
            }
        }


        return result;
    }


    /**
     * Използва се от екрана
     * извиква извличането на одобрени лица
     */
    public void actionAddLica() {
        try {

            //имаме актуализация на заповед, която все още няма въведен номер и все още могат да бъдат добавяни/премахвани лица
            if (this.doc.getId() != null &&
                    (this.doc.getRnDoc() == null || this.doc.getRnDoc().trim().isEmpty())) {

                //извличаме още един път списъка с ВСИЧКИ одобрени лица, а не само към конкретната молба
                JPA.getUtil().runInTransaction(() -> this.odobreniLicaList = this.stajDao.findStajList(null, this.doc.getDocVid()));
            }


            //ако има някакви вече въведени правим проверка за дублиране
            if (this.slcLicaList != null && !this.slcLicaList.isEmpty() && this.odobreniLicaList != null) {
                Set<Object> addedIds = new HashSet<>();
                for (Object[] osoba : slcLicaList) {
                    addedIds.add(osoba[0]);
                }
                this.odobreniLicaList.removeIf(osoba -> addedIds.contains(osoba[0]));
            }


        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при зареждане на данни за одобрени лица!!");
        }
    }

    public void actionChangeVid() {
        try {

            this.doc.setDocVid(CODE_ZNACHENIE_DOC_VID_ZAP_STAJ);
            if (this.vidStaj.equals(CODE_ZNACHENIE_STAJ_VID_ADDITIONAL)) {
                this.doc.setDocVid(CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ);
            }

            this.prefix = getDocPrefix(); // ако префикса за първоначален и допълнителен стаж е различен
            loadLica(this.doc.getId());

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при извличане на данни за одобрени лица!!");
        }
    }


    /**
     * Зарежда одобрените лица за стаж към конкретна заповед
     * ИЛИ
     * ако idDoc е null зарежда всички одобрени, които не участват в друга заповед
     */
    private void loadLica(Integer idDoc) {
        try {

            //имаме нов документ и зареждаме по подразбиране всички одобрени лица, които не са част от друга заповед
            if (this.doc != null) {
                JPA.getUtil().runInTransaction(() -> this.slcLicaList = this.stajDao.findStajList(idDoc, this.doc.getDocVid()));
            }


        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при зареждане на списък с одобрени лица!!");
        }
    }


    public void onRowCheckboxSelect(SelectEvent<Object[]> event) {
        try {

            Object[] row = event.getObject();
            Object id = row[0]; //id на лицето

            boolean exists = this.checkboxLicaList.stream().anyMatch(r -> r[0].equals(id));
            if (!exists) {
                this.checkboxLicaList.add(Arrays.copyOf(row, row.length)); // добавям копие
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при добавяне на лице в списъка!");
        }
    }

    public void selectAllRows() {
        try {

            for (Object[] row : this.odobreniLicaList) {
                boolean added = false;

                for (Object[] selectedRow : this.slcLicaList) {
                    if (Arrays.equals(row, selectedRow)) {
                        added = true;
                        break;
                    }
                }

                if (!added) {
                    this.slcLicaList.add(Arrays.copyOf(row, row.length));
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при добавяне на лица в списъка!");
        }
    }


    public void onRowCheckboxUnselect(UnselectEvent<Object[]> event) {
        try {

            Object[] row = event.getObject();
            Object id = row[0]; //id на лицето

            this.checkboxLicaList.removeIf(r -> r[0].equals(id));

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при премахване на лице от списъка!");
        }
    }


    public void actionConfirmDlg() {
        try {

            if (this.checkboxLicaList != null && !this.checkboxLicaList.isEmpty()) {

                boolean listChanged = false; // дали са коригирали лицата
                for (Object[] row : this.checkboxLicaList) {
                    boolean alreadyAdded = false;

                    for (Object[] selectedRow : this.slcLicaList) {
                        if (Arrays.equals(row, selectedRow)) {
                            alreadyAdded = true;
                            break;
                        }
                    }

                    if (!alreadyAdded) {
                        this.slcLicaList.add(Arrays.copyOf(row, row.length));
                        listChanged = true; //отбелязваме, че са коригирали лицата
                    }
                }

                if (listChanged) {
                    actionSave(); // извикваме само ако има промяна
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при добавяне на лица в списъка!");
        }
    }



    /**
     * Използва се от екрана за премахване на лице от списъка..
     * позволява се премахване само ако НЯМА въведени номер на заповед и дата
     * ако заповедта няма id само го премахва от локалния списък
     * ако вече е записана премахва връзката в базата
     */
    public void actionDeleteLice(Object[] selectedRow) {
        try {

            if (this.doc.getRnDoc() != null && !this.doc.getRnDoc().isEmpty()) {
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "traineeshipOrder.msgLica"));
                return;
            }

            if (selectedRow != null && selectedRow.length > 0)  {
                if(this.doc.getId() == null) {
                    this.slcLicaList.removeIf(r -> Arrays.equals(r, selectedRow));
                } else {
                    removeLice(selectedRow);
                }

                loadLica(this.doc.getId());
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
                        getMessageResourceString(beanMessages, "traineeshipOrder.removed"));
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при премахване на избрано лице от списъка!");
        }
    }


    /**
     * ПРЕМАХВАНЕ НА ЛИЦЕ ОТ БАЗАТА
     */
    private void removeLice(Object[] row) {
        try {

            JPA.getUtil().runInTransaction(() -> this.stajDao.removeStajList(row));

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при изтриване на избрано лице от базата!");
        }
    }


    /**
     * ОСНОВНО ИЗТРИВАНЕ НА ЦЕЛИЯ ОБЕКТ
     */
    public void actionDelete() {
        try {

            JPA.getUtil().runInTransaction(() -> {

                this.docDao.deleteById(this.doc.getId());

                if (!this.filesList.isEmpty()) {
                    for (Files f : this.filesList) {
                        new FilesDAO(getUserData()).deleteFileObject(f);
                    }
                }

                if (!this.slcLicaList.isEmpty()) {
                    for (Object[] lice : this.slcLicaList) {
                        this.stajDao.removeStajList(lice);
                    }
                }
            });

            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
                    getMessageResourceString(beanMessages, "traineeshipOrder.deleted"));

            this.doc = new Doc();
            this.slcLicaList = new ArrayList<>();
            this.odobreniLicaList = new ArrayList<>();
            this.checkboxLicaList = new ArrayList<>();
            this.codeSaglClassif = new ArrayList<>();
            this.filesList = new ArrayList<>();
            this.filesListRemoved = new ArrayList<>();


        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при изтриване на заповед!");
        }
    }


    /**
     * ГЕНЕРИРАНЕ НА ОБЩА ЗАПОВЕД
     * Отваря компонентата compUdostDoc
     */
    public void actionGenerateOrder() {
        this.bindCompUdostDoc.initComponent();
    }

    public void actionGeneratePrepis(Object[] liceData) {
        this.licePrepis = liceData;
        this.bindCompUdostDocPrepis.initComponent();
    }


    public void afterGeneratePrepis() {
        LOGGER.info("afterGeneratePrepis");

        try {

            JPA.getUtil().runWithClose(() -> this.filesList = new FilesDAO(getUserData()).selectByFileObjectDop(this.doc.getId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC));

            loadLica(this.doc.getId());
            setHaveUD(checkForUD());

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при актуализиране на данни!");
        }
    }

    /**
     * Проверка за заключен документ
     *
     * @param idObj
     * @return
     */
    private boolean checkForLock(Integer idObj) {

        boolean res = true;
        LockObjectDAO daoL = new LockObjectDAO();
        try {

            Object[] obj = daoL.check(this.ud.getUserId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC, idObj);
            if (obj != null) {
                res = false;
                String msg = getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR,
                        Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date()) + " / "
                        + DateUtils.printDate((Date) obj[1]);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,
                        getMessageResourceString(BEANMSG, "traineeshipOrder.locked"), msg);
            }

        } catch (DbErrorException e) {
            LOGGER.error("Грешка при проверка за заключена преписка! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }

        return res;
    }

    /**
     * Заключване на заповед, като преди това отключва всички обекти, заключени от потребителя
     *
     * @param idObj
     */
    public void lock(Integer idObj) {

        LockObjectDAO daoL = new LockObjectDAO();
        try {

            JPA.getUtil().runInTransaction(() -> daoL.lock(this.ud.getUserId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC, idObj, null));
        } catch (BaseException e) {
            LOGGER.error("Грешка при заключване на документ! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
        }
    }

    /**
     * при излизане от страницата - отключва обекта и да го освобождава за
     * актуализация от друг потребител
     */
    @PreDestroy
    public void unlockDelo() {

        if (!this.ud.isReloadPage()) {
            LockObjectDAO daoL = new LockObjectDAO();

            try {

                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(String.format("unlockData 0! %s", this.ud.getPreviousPage()));
                }

                JPA.getUtil().runInTransaction(() -> daoL.unlock(this.ud.getUserId()));
            } catch (BaseException e) {
                LOGGER.error("Грешка при отключване на документ! ", e);
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                        getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
            }
            this.ud.setPreviousPage(null);
        }
        this.ud.setReloadPage(false);
    }



    /******************************************************* FILES *******************************************************/

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
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при прикачване на файл!");
        }
    }

    public void actionDownloadPrepis(Integer fileId) {
       try {

           if (fileId != null) {

               Files tmpFile;
               tmpFile = new FilesDAO(ud).findById(fileId);

              if (tmpFile != null) {
                  LOGGER.error("starting download file");
                  downloadFile(tmpFile);
              }
           }

       } catch (Exception e) {
           LOGGER.error("Грешка при сваляне на препис! ", e);
           JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                   "Грешка при сваляне на препис!");
       } finally {
           JPA.getUtil().closeConnection();
       }
    }


    public void downloadFile(Files files) {
        try {

            if (files.getId() != null){
                FilesDAO dao = new FilesDAO(getUserData());
                try {
                    files = dao.findById(files.getId());
                } finally {
                    JPA.getUtil().closeConnection();
                }
                if (files.getContent() == null){
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

        } catch (IOException e) {
            LOGGER.error("IOException: " + e.getMessage());
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,"general.unexpectedResult"));
            LOGGER.error(e.getMessage(), e);

        } catch (Exception e) {
            LOGGER.error("Exception: " + e.getMessage());
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, "general.exception"));
        }
    }


    public void deleteFile(Files file) {
        try {

            if (file != null){
                if(file.getId()!=null){
                    filesListRemoved.add(file);
                }

                filesList.remove(file);
            }

        } catch (Exception e) {
            LOGGER.error("Грешка при изтриване на файл! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }


    private boolean checkForUD() {
        try {
            if (filesList == null || filesList.isEmpty()) {
                return false;
            }

            return filesList.stream()
                    .map(Files::getFilePurpose)
                    .filter(Objects::nonNull)
                    .anyMatch(purpose ->
                            purpose.equals(CODE_ZNACHENIE_FILE_PURPOSE_UD_ORIGINAL) |
                                    purpose.equals(CODE_ZNACHENIE_FILE_PURPOSE_UD_DUPLICATE)
                    );

        } catch (Exception e) {
            LOGGER.error("Грешка при проверка за УД!", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
            return false;
        }
    }


    /******************************************************** EXPORT *********************************************************/
    public void postProcessXLSPersonsTrainList(Object document) {
        String title = getMessageResourceString(LABELS, "traineeshipOrder.licaListExport");
        new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);
    }


    public void preProcessPDFPersonsTrainList(Object document)  {
        try {

            String title = getMessageResourceString(LABELS, "traineeshipOrder.licaListExport");
            new CustomExpPreProcess().preProcessPDF(document, title, null, null, null);

        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(),e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(),e);
        }
    }


    public PDFOptions pdfOptions() {
        PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
        return pdfOpt;
    }
    /*****************************************************************************************************************/



    public Doc getDoc() {
        return doc;
    }

    public void setDoc(Doc doc) {
        this.doc = doc;
    }

    public List<Object[]> getOdobreniLicaList() {
        return odobreniLicaList;
    }

    public void setOdobreniLicaList(List<Object[]> odobreniLicaList) {
        this.odobreniLicaList = odobreniLicaList;
    }

    public List<Object[]> getSlcLicaList() {
        return slcLicaList;
    }

    public void setSlcLicaList(List<Object[]> slcLicaList) {
        this.slcLicaList = slcLicaList;
    }

    public Integer getVidStaj() {
        return vidStaj;
    }

    public void setVidStaj(Integer vidStaj) {
        this.vidStaj = vidStaj;
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

    public List<Object[]> getCheckboxLicaList() {
        return checkboxLicaList;
    }

    public void setCheckboxLicaList(List<Object[]> checkboxLicaList) {
        this.checkboxLicaList = checkboxLicaList;
    }

    public List<Files> getFilesListRemoved() {
        return filesListRemoved;
    }

    public void setFilesListRemoved(List<Files> filesListRemoved) {
        this.filesListRemoved = filesListRemoved;
    }

    public CompUdostDokument getBindCompUdostDoc() {
        return bindCompUdostDoc;
    }

    public void setBindCompUdostDoc(CompUdostDokument bindCompUdostDoc) {
        this.bindCompUdostDoc = bindCompUdostDoc;
    }

    public CompUdostDokument getBindCompUdostDocPrepis() {
        return bindCompUdostDocPrepis;
    }

    public void setBindCompUdostDocPrepis(CompUdostDokument bindCompUdostDocPrepis) {
        this.bindCompUdostDocPrepis = bindCompUdostDocPrepis;
    }

    public Integer getUdostDokVid() {
        if(this.doc.getDocVid() == CODE_ZNACHENIE_DOC_VID_ZAP_STAJ) {
            return CODE_ZNACHENIE_SHABLON_ZAPOVED_STAJ;
        }
        else if(this.doc.getDocVid() == CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ) {
            return CODE_ZNACHENIE_SHABLON_ZAPOVED_DOP_STAJ;
        }
        return null;
    }

    public Integer getPrepisVid(Object mentorObj) {
        // при заповед за стаж връщаме шаблона за препис с/без наставник
        String mentor = (String) mentorObj;
        if(this.doc.getDocVid() == CODE_ZNACHENIE_DOC_VID_ZAP_STAJ) {
            if(ValidationUtils.isNotBlank(mentor)) {
                return UriregConstants.CODE_ZNACHENIE_SHABLON_ZAPOVED_STAJ_NAST;
            }
            else {
                return UriregConstants.CODE_ZNACHENIE_SHABLON_ZAPOVED_STAJ_BEZ_NAST;
            }
        }
        // при заповед за ДОП стаж връщаме шаблона за препис от допълнителен стаж
        else if(this.doc.getDocVid() == CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ) {
            return UriregConstants.CODE_ZNACHENIE_SHABLON_IZVLECH_ZAPOVED_DOP_STAJ;
        }

        return null;
    }

    public Object[] getLicePrepis() {
        return licePrepis;
    }

    public boolean isHaveUD() {
        return haveUD;
    }

    public void setHaveUD(boolean haveUD) {
        this.haveUD = haveUD;
    }

    public boolean isHaveRnDoc() {
        return haveRnDoc;
    }

    public void setHaveRnDoc(boolean haveRnDoc) {
        this.haveRnDoc = haveRnDoc;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
