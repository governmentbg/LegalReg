package com.ib.urireg.beans;


import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.urireg.db.dao.MessageDAO;
import com.ib.urireg.db.dto.Message;
import com.ib.urireg.db.dto.MessageLang;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.UserData;
import jakarta.annotation.PostConstruct;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;




/**
 * МОДАЛЕН ЗА СЪЗДАВАНЕ И КОРЕКЦИЯ НА СЪОБЩЕНИЯ ЗА ПУБЛИКУВАНЕ НА САЙТА НА МП
 * /позволява преизползване в различни странници с ui:include/
 *
 * @author silvia
 */

@Named("messagesDialogBean")
@ViewScoped
public class MessagesDialogBean extends IndexUIbean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesDialogBean.class);

    public static final String MOZILLA = "Mozilla";
    public static final String APPXDOWNLOAD = "application/x-download";
    public static final String CONTDISPOSITION = "Content-Disposition";


    private Integer messId;
    private Integer lang;

    private Message message;
    private MessageLang mesLang;
    private List<Files> filesList;
    private List<Files> filesToRemove;

    private UserData ud;
    private transient MessageDAO dao;

    private String dialogPrefix;


    @PostConstruct
    public void init() {
        LOGGER.info("MessagesDialogBean INIT....!");

        try {

            this.ud = getUserData(UserData.class);
            this.dao = new MessageDAO(this.ud);
            this.message = new Message();
            this.mesLang = new MessageLang();
            this.filesList = new ArrayList<>();
            this.filesToRemove = new ArrayList<>();
            setLang(this.ud.getCurrentLang()); //по подразбиране сетвам да е български


            //взима динамично формата, за да може да се преизползва и в други странници
            FacesContext ctx = FacesContext.getCurrentInstance();
            UIViewRoot root = ctx.getViewRoot();
            UIComponent dialog = findComponent(root, "msgDialog");

            if (dialog != null) {
                dialogPrefix = dialog.getClientId(ctx).replace(":msgDialog", "");
            }


        } catch (Exception e) {
            LOGGER.error("Грешка при инициализация: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при инициализиране на обект!");
        }
    }


    /**
     * ОСНОВЕН ЗАПИС
     */
    public void actionSave() {
        try {
            if (checkData()) {

                JPA.getUtil().runInTransaction(() -> {
                    this.dao.saveMessageWithLangs(this.message);

                    if (!getFilesList().isEmpty()) {
                        for (Files file : filesList) {

                            if (file.getFileInfo() == null || file.getFileInfo().isEmpty()) {
                                file.setFileInfo(file.getFilename());
                            }

                            if (file.getId() == null) {
                                new FilesDAO(this.ud).saveFileObject(file, this.message.getId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_MESSAGE);
                            }
                        }
                    }
                    if (!getFilesToRemove().isEmpty()) {
                        for (Files file:getFilesToRemove()) {
                            new FilesDAO(this.ud).deleteFileObject(file);
                        }
                    }
                });

                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
            }

        } catch (Exception e) {
            LOGGER.error("Грешка при запис: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при запис на съобщение!");
        }
    }


    /**
     * ИЗТРИВАНЕ
     */
    public void actionDelete() {
        try {

            JPA.getUtil().runInTransaction(() -> {

               this.dao.deleteMessageWithLangs(this.message.getId());

                if (!this.filesList.isEmpty()) {
                  for (Files f : this.filesList) {
                      new FilesDAO(getUserData()).deleteFileObject(f);
                    }
                }

            });

            actionClearDlg();
            PrimeFaces.current().executeScript("PF('addMessagesVar').close();");

            //remoteCommand от основния бийн след затварянето на модалния..
            // в този случай ъпдейтва таблицата и презарежда резултатите
            PrimeFaces.current().executeScript("onModalClosed();");

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при изтриване на заповед!");
        }
    }


    /**
     * При отваряне на модалния прозорец:
     * взима подадения параметър с ид-то на съобщението (ако е подадено) или създава ново
     * зарежда обекта и файловете за корекция
     */
    public void actionShowDlg() {
        try {

            if (JSFUtils.getRequestParameter("messageId") != null && !"".equals(JSFUtils.getRequestParameter("messageId"))) {
                this.messId = Integer.valueOf(JSFUtils.getRequestParameter("messageId"));
            }

            if (this.messId != null) {
                JPA.getUtil().runWithClose(() -> {
                    this.message = this.dao.loadMessageWithLang(messId, this.ud.getCurrentLang());
                    this.filesList = new FilesDAO(this.ud).selectByFileObjectDop(this.message.getId(), UriregConstants.CODE_ZNACHENIE_JOURNAL_MESSAGE);
                });

            }


        } catch (Exception e) {
            LOGGER.error("Грешка при зареждане на съобщение: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при зареждане на съобщение!");
        }
    }


    public void actionClearDlg() {
        this.message = new Message();
        this.mesLang = new MessageLang();
        this.filesList = new ArrayList<>();
        this.filesToRemove = new ArrayList<>();
        setLang(this.ud.getCurrentLang());
    }


    private boolean checkData() {
        boolean result = true;


        if (message.getMessageVid() == null) {
            JSFUtils.addMessage(dialogPrefix + ":vidDlg", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "messagesFilterList.vid")));
            result = false;
        }

        if (message.getDateFrom() == null) {
            JSFUtils.addMessage(dialogPrefix + ":dataOtDlg", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(UI_LABELS, "general.dataOt")));
            result = false;
        }

        if (message.getDateFrom() != null && message.getDateTo() != null) {
            if (message.getDateFrom().after(message.getDateTo())) {
                JSFUtils.addMessage(dialogPrefix + ":dataDoDlg", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "messagesDialog.dateBefore"));
                result = false;
            }
        }

        if (message.getStatus() == null) {
            JSFUtils.addMessage(dialogPrefix + ":statusDlg", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "messagesFilterList.status")));
            result = false;
        }



        return result;
    }




    private MessageLang getOrCreateMessageLang() {

        if (message.getMessageLangs() == null) message.setMessageLangs(new ArrayList<>());
        MessageLang ml = message.getMessageLangs().stream()
                .filter(l -> l.getLang() != null && l.getLang().equals(lang))
                .findFirst()
                .orElseGet(() -> {
                    MessageLang newMl = new MessageLang();
                    newMl.setLang(lang);
                    message.getMessageLangs().add(newMl);
                    return newMl;
                });
        mesLang = ml;
        return ml;
    }

    public void uploadFileListener(FileUploadEvent event) {
        try {
            UploadedFile upFile = event.getFile();
            Files fileObject = new Files();
            fileObject.setFilename(upFile.getFileName());
            fileObject.setContentType(upFile.getContentType());
            fileObject.setContent(upFile.getContent());
            fileObject.setFileInfo(upFile.getFileName());

            this.filesList.add(fileObject);

        } catch (Exception e) {
            LOGGER.error("Грешка при прикачване на файл: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при прикачване на файл!");
        }
    }

    public void deleteFile(Files file) {
        try {

            if (file != null) {
                if(file.getId() != null) {
                    this.filesToRemove.add(file);
                }
                this.filesList.remove(file);
            }
        } catch (Exception e) {
            LOGGER.error("Грешка при изтриване на файл! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void onRowEdit(RowEditEvent<Files> event) {
        try {

            Files file = event.getObject();
            if (file.getFileInfo() == null || file.getFileInfo().isEmpty()) {
                copyNameToInfo(file);
            }

        } catch (Exception e) {
            LOGGER.error("Грешка при потвърждение на запис! ", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    private void copyNameToInfo(Files file) {
        file.setFileInfo(file.getFilename());
    }


    private UIComponent findComponent(UIComponent base, String id) {
        if (id.equals(base.getId())) {
            return base;
        }
        for (UIComponent child : base.getChildren()) {
            UIComponent result = findComponent(child, id);
            if (result != null) {
                return result;
            }
        }
        if (base.getFacets() != null) {
            for (UIComponent facet : base.getFacets().values()) {
                UIComponent result = findComponent(facet, id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public void downloadFile(Files file) {
        try {
            if (file.getId() != null) {
                file = new FilesDAO(this.ud).findById(file.getId());
                if (file.getContent() == null) {
                    file.setContent(new byte[0]);
                }
            }

            FacesContext context = FacesContext.getCurrentInstance();
            ExternalContext ext = context.getExternalContext();
            String filename = encodeFilename(ext, file.getFilename());
            ext.setResponseHeader("Content-Type", APPXDOWNLOAD);
            ext.setResponseHeader("Content-Length", String.valueOf(file.getContent().length));
            ext.setResponseHeader(CONTDISPOSITION, "attachment; filename=\"" + filename + "\"");
            ext.getResponseOutputStream().write(file.getContent()); context.responseComplete();

        } catch (IOException e) {
            LOGGER.error("IOException при сваляне на файл: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при сваляне на файл!");
        } catch (Exception e) {
            LOGGER.error("Exception при сваляне на файл: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при обработка на файл!");
        }
    }

    private String encodeFilename(ExternalContext ext, String filename) throws Exception {
        String agent = ((HttpServletRequest) ext.getRequest()).getHeader("user-agent");
        if (agent != null && (agent.contains("MSIE") || (agent.contains(MOZILLA) && agent.contains("rv:11")) || agent.contains("Edge"))) {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8); }
        if (agent != null && agent.contains(MOZILLA)) {
            return MimeUtility.encodeText(filename, "UTF8", "B");
        }

        return URLEncoder.encode(filename, StandardCharsets.UTF_8);
    }



    public String getMessageText() {
        return getOrCreateMessageLang().getMessageText();
    }

    public void setMessageText(String text) {
        getOrCreateMessageLang().setMessageText(text);
    }

    public String getMessageTitle() {
        return getOrCreateMessageLang().getTitle();
    }

    public void setMessageTitle(String title) {
        getOrCreateMessageLang().setTitle(title);
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public MessageLang getMesLang() {
        return mesLang;
    }

    public void setMesLang(MessageLang mesLang) {
        this.mesLang = mesLang;
    }

    public List<Files> getFilesList() {
        return filesList;
    }

    public void setFilesList(List<Files> filesList) {
        this.filesList = filesList;
    }

    public List<Files> getFilesToRemove() {
        return filesToRemove;
    }

    public MessageDAO getDao() {
        return dao;
    }

    public void setDao(MessageDAO dao) {
        this.dao = dao;
    }

    public Integer getMessId() {
        return messId;
    }

    public void setMessId(Integer messId) {
        this.messId = messId;
    }

    public Integer getLang() {
        return lang;
    }

    public void setLang(Integer lang) {
        this.lang = lang;
    }

    public UserData getUd() {
        return ud;
    }

    public void setUd(UserData ud) {
        this.ud = ud;
    }

    public String getDialogPrefix() {
        return dialogPrefix;
    }

    public void setDialogPrefix(String dialogPrefix) {
        this.dialogPrefix = dialogPrefix;
    }
}