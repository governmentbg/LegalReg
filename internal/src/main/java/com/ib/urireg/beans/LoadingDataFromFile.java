package com.ib.urireg.beans;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;
import com.ib.urireg.db.dao.IzpitDAO;
import com.ib.urireg.db.dao.ShablonLogicDAO;
import com.ib.urireg.db.dto.ShablonFile;
import com.ib.urireg.db.dto.ShablonLogic;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Named("loadingDataFromFile")
@ViewScoped
public class LoadingDataFromFile extends IndexUIbean implements Serializable {

    /**
     * Зареждане на данни от файл преди 2011г.
     *
     */
    private static final long serialVersionUID = -499252651930262387L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadingDataFromFile.class);

    private String nomFile;
    private String error;

    /**
     * За качване на файл с резултати.
     *
     */
    public void actionUploadFile(FileUploadEvent event){

        UploadedFile file = event.getFile();

        if(SearchUtils.isEmpty(this.nomFile)){
            JSFUtils.addMessage("formLoadingData:nomFile", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "loadingDataFromFile.nomFile")));
            return;

        } else {
            uploadFile(file, this.nomFile);
        }
    }

    public void uploadFile(UploadedFile uplFile, String nomFile){

        this.error = new IzpitDAO(getUserData()).proccessFileBefore2011(uplFile.getFileName(), uplFile.getContent(), nomFile, false);

        if (SearchUtils.isEmpty(this.error)){
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
        } else {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "loadingDataFromFile.errorLoadData"));
            PrimeFaces.current().executeScript("scrollToErrors()");
        }
    }

    public void actionDownloadShablon() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
            String agent = request.getHeader("user-agent");
            ShablonLogicDAO dao = new ShablonLogicDAO(getUserData());
            List<ShablonLogic> logic = dao.findByDocVid(2011); // забито е да намери реда с код 2011

            if(logic.isEmpty()) {
                return;
            }

            ShablonFile file = dao.loadFile(logic.get(0).getId());

            String codedfilename = "";

            if (null != agent && (agent.contains("MSIE") || agent.contains("Mozilla") && agent.contains("rv:11") || agent.contains("Edge"))) {
                codedfilename = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8);
            } else if (null != agent && agent.contains("Mozilla")) {
                codedfilename = MimeUtility.encodeText(file.getFilename(), "UTF8", "B");
            } else {
                codedfilename = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8);
            }

            externalContext.setResponseHeader("Content-Type", "application/x-download");
            externalContext.setResponseHeader("Content-Length", file.getContent().length + "");
            externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
            externalContext.getResponseOutputStream().write(file.getContent());

            facesContext.responseComplete();
        }
        catch(IOException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при четене на файл");
        }
        catch(DbErrorException e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при работа с базата");
        }
    }

    public String getNomFile() {
        return nomFile;
    }

    public void setNomFile(String nomFile) {
        this.nomFile = nomFile;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
