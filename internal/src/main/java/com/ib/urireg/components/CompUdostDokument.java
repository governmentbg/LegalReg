package com.ib.urireg.components;

import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.urireg.db.dto.ShablonBookmark;
import com.ib.urireg.db.dto.ShablonLogic;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.UserData;
import com.ib.urireg.udostDocs.OnCompleteMethods;
import com.ib.urireg.udostDocs.UdostDocumentCreator;
import jakarta.el.ELContext;
import jakarta.el.MethodExpression;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@FacesComponent(value = "compUdostDokument", createTag = true)
public class CompUdostDokument extends UINamingContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(CompUdostDokument.class);

	private static final String ERR_ZAPIS = "Грешка при запис на удостоверителен документ";
	
	private enum PropertyKeys {
		PARENT_OBJECT_ID, HTML, EDITING, CHOSEN_SHABLON, DUBLIKAT, VID_DOK, DELETE_PREV_FILE, SAVED_FILE
	}
	
	private UserData userData = null;
	private SystemData systemData = null;
	private UdostDocumentCreator creator;
	
	public void initComponent() {
		Integer vidDok = ((Number) this.getValueExpression("vidDok").getValue(FacesContext.getCurrentInstance().getELContext())).intValue();
		setVidDok(vidDok);
		Integer objectId = ((Number) this.getValueExpression("parentObjectId").getValue(FacesContext.getCurrentInstance().getELContext())).intValue();
		setParentObjectId(objectId);
		
		if(this.getValueExpression("isDublikat") != null) {
			boolean dublikat = this.getValueExpression("isDublikat").getValue(FacesContext.getCurrentInstance().getELContext());
			setDublikat(dublikat);
		}
		else if(this.getAttributes().get("isDublikat") != null) {
			Boolean value = (Boolean) this.getAttributes().get("isDublikat");
			setDeletePreviousFile(value);
		}
		else {
			setDublikat(false);
		}

		if(this.getValueExpression("deletePrevFile") != null) {
			boolean deletePrevFile = this.getValueExpression("deletePrevFile").getValue(FacesContext.getCurrentInstance().getELContext());
			setDeletePreviousFile(deletePrevFile);
		}
		else if(this.getAttributes().get("deletePrevFile") != null) {
			Boolean value = (Boolean) this.getAttributes().get("deletePrevFile");
			setDeletePreviousFile(value);
		}
		else {
			setDeletePreviousFile(false);
		}

		Map<String, Object> dataMap = new HashMap<>();
		Integer docId = (Integer) getAttributes().get("docId");
		dataMap.put(UdostDocumentCreator.KEY_DOC_ID, docId);
		Integer liceId = (Integer) getAttributes().get("liceId");
		dataMap.put(UdostDocumentCreator.KEY_LICE_ID, liceId);
		dataMap.put(UdostDocumentCreator.KEY_DUBLIKAT, isDublikat());
		dataMap.put(UdostDocumentCreator.KEY_DOK_VID, getVidDok());
		// TODO more keys

		setEditing(false);
		setChosenShablon(null);
		setHtml(null);
		this.setSavedFile(null);


		try {
			this.creator = new UdostDocumentCreator(getSystemData(), getUserData(), getVidDok(), dataMap);
			if(!this.creator.needsToChooseShablon()) {
				generateDanni();
			}
			PrimeFaces.current().executeScript("PF('" + getClientId() + ":udost').show()");
			PrimeFaces.current().ajax().update(getClientId());
		}
		catch(UdostDocumentCreator.MissingSystemSettingException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Към документа няма въведени шаблони");
			LOGGER.error(e.getMessage(), e);
		}
		catch (Exception e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	public void onChooseShablon() {
		ShablonLogic shablon = this.creator.getShabloni()
				.stream()
				.filter(f -> f.getId().equals(getChosenShablon()))
				.findFirst()
				.orElse(null);
		this.creator.setShablonLogic(shablon);
		generateDanni();
	}

	public void goBack() {
		setChosenShablon(null);
		this.creator.setShablonLogic(null);
		this.creator.setChosenShablon(null);
	}

	public void generateDanni() {
		try {
			this.creator.gatherData();
			this.creator.fillBookmarksInShablon();
			setHtml(creator.getDocumentAsHtmlString());
		}
		catch (Exception e) {
			if(e instanceof InvocationTargetException) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, ((InvocationTargetException) e).getTargetException().getMessage());
			}
			else {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	public void finishEditing() {
		try {
			this.creator.gatherAdvancedBookmarksData();
			this.creator.fillBookmarksInShablon();
			setHtml(creator.getDocumentAsHtmlString());
		}
		catch (Exception e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
		finally {
			setEditing(false);
		}
	}
	
	public void cancelEditing() {
		setEditing(false);
	}
	
	public void generateDoc(boolean checkForPrevfile) {
		try {
			if(isDeletePreviousFile() && checkForPrevfile) {

				List<Files> currentFiles = new FilesDAO(this.userData).selectByFileObjectDop(getParentObjectId(), getCodeObj());
				int purpose = (isDublikat()
						? UriregConstants.CODE_ZNACHENIE_FILE_PURPOSE_UD_DUPLICATE
						: UriregConstants.CODE_ZNACHENIE_FILE_PURPOSE_UD_ORIGINAL);
				for (Files oldFile : currentFiles) {
					if (Objects.equals(oldFile.getFileType(), getVidDok())
							&& Objects.equals(oldFile.getFilePurpose(), purpose)) { // проверявам и по purpose, там пише дали файлът е оригинал или дубликат
						PrimeFaces.current().executeScript("PF('" + getClientId() + ":confirm1').show()");
						return;
					}
				}
			}

			saveDocument();
			callActionAfterGenerate();
		}
		catch (Exception e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, ERR_ZAPIS);
			LOGGER.error(ERR_ZAPIS, e);
		}
	}
	
	public void downloadDoc() {

		if(this.getSavedFile() == null) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Документът не е записан в базата");
			return;
		}
		
		try {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			ExternalContext externalContext = facesContext.getExternalContext();

			HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
			String agent = request.getHeader("user-agent");

			String codedfilename = "";

			if (null != agent && (agent.contains("MSIE") || agent.contains("Mozilla") && agent.contains("rv:11") || agent.contains("Edge"))) {
				codedfilename = URLEncoder.encode(this.getSavedFile().getFilename(), StandardCharsets.UTF_8);
			}
			else if (null != agent && agent.contains("Mozilla")) {
				codedfilename = MimeUtility.encodeText(this.getSavedFile().getFilename(), "UTF8", "B");
			}
			else {
				codedfilename = URLEncoder.encode(this.getSavedFile().getFilename(), StandardCharsets.UTF_8);
			}

			externalContext.setResponseHeader("Content-Type", "application/x-download");
			externalContext.setResponseHeader("Content-Length", this.getSavedFile().getContent().length + "");
			externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
			externalContext.getResponseOutputStream().write(this.getSavedFile().getContent());

			facesContext.responseComplete();
		}
		catch(Exception e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Грешка при сваляне на удостоверителен документ");
			LOGGER.error(e.getMessage(), e);
		}
	}

	private void saveDocument() {

		try {
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss");
			byte[] contents = this.creator.getDocumentAsBytes();
			final FilesDAO filesDao = new FilesDAO(this.userData);

			String docType = getSystemData().decodeItem(
					UriregConstants.CODE_CLASSIF_SHABLONI,
					this.getVidDok(),
					this.userData.getCurrentLang(),
					date);

			Files file = new Files();
			file.setContentType(this.creator.getChosenShablon().getContentType());
			file.setContent(contents);
			file.setFilename(String.format("%s_%s.docx", docType, sdf.format(date)));
			file.setOfficial(SysConstants.CODE_ZNACHENIE_DA);
			file.setFileInfo(docType);
			file.setFilePurpose(isDublikat()
					? UriregConstants.CODE_ZNACHENIE_FILE_PURPOSE_UD_DUPLICATE
					: UriregConstants.CODE_ZNACHENIE_FILE_PURPOSE_UD_ORIGINAL);

			// по този код проверявам дали вече има такъв записан файл и го презаписвам / правя нов
			file.setFileType(getVidDok());

			JPA.getUtil().runInTransaction(() -> {
				if(isDeletePreviousFile()) { // Изтривам предишен такъв документ
					List<Files> currentFiles = filesDao.selectByFileObjectDop(getParentObjectId(), getCodeObj());

					for (Files oldFile : currentFiles) {
						if (Objects.equals(oldFile.getFileType(), getVidDok())
								&& Objects.equals(oldFile.getFilePurpose(), file.getFilePurpose())) { // проверявам и по purpose, там пише дали файлът е оригинал или дубликат
							filesDao.deleteFileObject(oldFile);
						}
					}
				}
				Files f = filesDao.saveFileObject(file, getParentObjectId(), getCodeObj());
				this.setSavedFile(f);

				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Документът е записан!");
			});
		}
		catch (Exception e) {
			LOGGER.error(ERR_ZAPIS, e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, ERR_ZAPIS, e.getMessage());
		}
		finally {
			JPA.getUtil().closeConnection();
		}

    }

	private void callActionAfterGenerate() {
		final OnCompleteMethods onCompleteMethods = new OnCompleteMethods(this.userData, this.systemData);
		this.creator.getBookmarks().forEach(b -> {
			String methodName = b.getOnCompleteMethodName();
			if(methodName != null) {				
				try {
					Method fillerMethod = OnCompleteMethods.class.getMethod(methodName, Object.class);
					// Букмарковете с fillComponent=ADM_STRUCT ще подават на метода си кода на лицето от структурата
					fillerMethod.invoke(onCompleteMethods, this.creator.getAdvancedBookmarkValues().get(b.getLabel()));
				}
				catch (NoSuchMethodException e) {
					LOGGER.error("В класа UdostDokumentOnCompleteMethods не съществува метод " + methodName, e);
				}
				catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					LOGGER.error("Грешка при извикване на метод UdostDokumentOnCompleteMethods." + methodName, e);
				}
			}
		});
		
		ELContext elContext = FacesContext.getCurrentInstance().getELContext();
		MethodExpression o = (MethodExpression) getAttributes().get("actionAfterGenerate");
		if(o != null) {
			o.invoke(elContext, null);
		}
	}
	
	public boolean showApplyEditButton() {
		if(this.creator != null) {
			return this.creator.getBookmarks().stream().anyMatch(b -> b.getFillStrategy().equals(ShablonBookmark.FillStrategies.ADVANCED));
		}
		else return false;
	}
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	

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
	
	public UdostDocumentCreator getCreator() {
		return creator;
	}

	public Integer getChosenShablon() {
		return (Integer) getStateHelper().eval(PropertyKeys.CHOSEN_SHABLON, null);
	}

	public void setChosenShablon(Integer chosenShablon) {
		getStateHelper().put(PropertyKeys.CHOSEN_SHABLON, chosenShablon);
	}

	public boolean isEditing() {
		return (boolean) getStateHelper().eval(PropertyKeys.EDITING, false);
	}

	public void setEditing(boolean editing) {
		getStateHelper().put(PropertyKeys.EDITING, editing);
	}

	public boolean isDublikat() {
		return (boolean) getStateHelper().eval(PropertyKeys.DUBLIKAT, false);
	}

	public void setDublikat(boolean dublikat) {
		getStateHelper().put(PropertyKeys.DUBLIKAT, dublikat);
	}

	public boolean isDeletePreviousFile() {
		return (boolean) getStateHelper().eval(PropertyKeys.DELETE_PREV_FILE, false);
	}

	public void setDeletePreviousFile(boolean deletePreviousFile) {
		getStateHelper().put(PropertyKeys.DELETE_PREV_FILE, deletePreviousFile);
	}

	public String getHtml() {
		return (String) getStateHelper().eval(PropertyKeys.HTML, null);
	}

	public void setHtml(String html) {
		getStateHelper().put(PropertyKeys.HTML, html);
	}

	public Integer getParentObjectId() {
		return (Integer) getStateHelper().eval(PropertyKeys.PARENT_OBJECT_ID, null);
	}

	public void setParentObjectId(Integer parentObjectId) {
		getStateHelper().put(PropertyKeys.PARENT_OBJECT_ID, parentObjectId);
	}

	public Integer getVidDok() {
		return (Integer) getStateHelper().eval(PropertyKeys.VID_DOK, null);
	}

	public void setVidDok(Integer vidDoc) {
		getStateHelper().put(PropertyKeys.VID_DOK, vidDoc);
	}

	public Files getSavedFile() {
		return (Files) getStateHelper().eval(PropertyKeys.SAVED_FILE, null);
	}

	public void setSavedFile(Files file) {
		getStateHelper().put(PropertyKeys.SAVED_FILE, file);
	}

	private Integer getCodeObj() {
		// Това се прави специално при генерирането на преписи от заповедта за стаж.
		// При тях файлът се записва с код на обект "стаж", а в останалите случаи с код за "документ".
		// Много е специфично!

		// 23.02.26 - третото условие е добавено допълнително, понежебез него преписи от доп. стаж се записваха на грешно място
		return (this.getVidDok() == UriregConstants.CODE_ZNACHENIE_SHABLON_ZAPOVED_STAJ_NAST
				|| this.getVidDok() == UriregConstants.CODE_ZNACHENIE_SHABLON_ZAPOVED_STAJ_BEZ_NAST
				|| this.getVidDok() == UriregConstants.CODE_ZNACHENIE_SHABLON_IZVLECH_ZAPOVED_DOP_STAJ)
				? UriregConstants.CODE_ZNACHENIE_JOURNAL_STAJ
				: UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC;
	}
}
