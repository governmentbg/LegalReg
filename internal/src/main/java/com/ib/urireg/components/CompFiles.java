package com.ib.urireg.components;

import com.ib.indexui.utils.JSFUtils;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.exceptions.DbErrorException;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UserData;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.primefaces.PrimeFaces;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.DefaultStreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/** */
@FacesComponent(value = "compFiles", createTag = true)
public class CompFiles extends UINamingContainer {

	private enum PropertyKeys {
		filesList, showDialog
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CompFiles.class);
	private UserData userData = null;
	private SystemData systemData = null;
	private StreamedContent fileToDownload;

	public  static final  String MOZILLA  		 = "Mozilla";
	public  static final  String APPXDOWNLOAD	 = "application/x-download";
	public  static final  String CONTDISPOSITION = "Content-Disposition";

	public void initCmp() {
		setFilesList(new ArrayList<>());
	}

	public void actionLoadFiles(){
		Integer codeObj = (Integer) getAttributes().get("codeObj");
		Integer idObj = (Integer) getAttributes().get("idObj");
		//допълнителни параметри,в случай че се налага обединяване на файлове от два обекта
		Integer codeObj1 = (Integer) getAttributes().get("codeObj1");
		Integer idObj1 = (Integer) getAttributes().get("idObj1");

		boolean singleFile = (boolean) getAttributes().get("singleFile");
		try {
			setFilesList(new ArrayList<>());
			addFilesToList(idObj,codeObj);

			if(codeObj1!=null && idObj1!=null){
				addFilesToList(idObj1,codeObj1);
			}
			if(singleFile){
				if (getFilesList() == null ||getFilesList().isEmpty() || getFilesList().size()>1) {// При повече от един файл или при липса на файлове се показва модалния
					actionMultipleFiles();
				} else if (getFilesList().size() == 1) { // ако е само един файл,директно се сваля
					downloadSingleFile(getFilesList().get(0).getId());
					PrimeFaces.current().executeScript("PrimeFaces.monitorDownload();");
					setShowDialog(Boolean.FALSE);
				}
			}else{
				actionMultipleFiles();
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private void actionMultipleFiles(){
		setShowDialog(Boolean.TRUE);
		PrimeFaces.current().ajax().update(getClientId() + ":fileDialogGroup");
		PrimeFaces.current().executeScript("PF('" + getClientId().replace(':', '_') + "_fileDialogWV').show()");
	}

	public void addFilesToList(Integer idObj, Integer codeObj) {
		try {
			FilesDAO dao = new FilesDAO(getUserData());
			List<Files> newFiles = dao.selectByFileObjectDop(idObj, codeObj);

			List<Files> currentFiles = getFilesList();
			if (currentFiles == null) {
				currentFiles = new ArrayList<>();
			}
			currentFiles.addAll(newFiles);
			setFilesList(currentFiles);

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**
	 * Download selected file
	 *
	 * @param idFile
	 */
	public void download(Integer idFile)  throws IOException {

        try {
			Files file = new FilesDAO(userData).findById(idFile);
			if(file!=null && file.getContent()!=null) {
				FacesContext facesContext = FacesContext.getCurrentInstance();
				ExternalContext externalContext = facesContext.getExternalContext();

				HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
				String agent = request.getHeader("user-agent");

				String codedfilename = "";

				if (null != agent && (agent.contains("MSIE") || agent.contains(MOZILLA) && agent.contains("rv:11") || agent.contains("Edge"))) {
					codedfilename = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8);
				} else if (null != agent && agent.contains(MOZILLA)) {
					codedfilename = MimeUtility.encodeText(file.getFilename(), "UTF8", "B");
				} else {
					codedfilename = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8);
				}

				externalContext.setResponseHeader("Content-Type", APPXDOWNLOAD);
				externalContext.setResponseHeader("Content-Length", file.getContent().length + "");
				externalContext.setResponseHeader(CONTDISPOSITION, "attachment;filename=\"" + codedfilename + "\"");
				externalContext.getResponseOutputStream().write(file.getContent());

				facesContext.responseComplete();
			}
        } catch (DbErrorException e) {
            throw new RuntimeException(e);
        }

	}

	public void downloadSingleFile(Integer idFile) {
		try {
			Files file = new FilesDAO(userData).findById(idFile);

			if (file != null && file.getContent() != null) {
				fileToDownload = DefaultStreamedContent.builder().name(file.getFilename()).contentType("application/octet-stream")
						.stream(() -> new ByteArrayInputStream(file.getContent())).build();
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	public StreamedContent getFileToDownload() {
		return fileToDownload;
	}

	/** @return */
	@SuppressWarnings("unchecked")
	public List<Files> getFilesList() {
		List<Files> eval = (List<Files>) getStateHelper().eval(PropertyKeys.filesList, null);
		return eval != null ? eval : new ArrayList<>();
	}

	/** * @param filesList */
	public void setFilesList(List<Files> filesList) {
		getStateHelper().put(PropertyKeys.filesList, filesList);
	}

	public Boolean getShowDialogl() {
		return (Boolean) getStateHelper().eval(PropertyKeys.showDialog, null);
	}

	public void setShowDialog(Boolean showDialog) {
		getStateHelper().put(PropertyKeys.showDialog, showDialog);
	}

	private UserData getUserData() {
		if (this.userData == null) {
			this.userData = (UserData) JSFUtils.getManagedBean("userData");
		}
		return this.userData;
	}

	private SystemData getSystemData() {
		if (this.systemData == null) {
			this.systemData =  (SystemData) JSFUtils.getManagedBean("systemData");
		}
		return this.systemData;
	}

	/** @return */
	public Integer getCurrentLang() {
		return getUserData().getCurrentLang();
	}
}
