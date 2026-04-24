package com.ib.urireg.beans;

import com.aspose.words.Document;
import com.ib.urireg.db.dao.ReferentDAO;
import com.ib.urireg.db.dao.ShablonLogicDAO;
import com.ib.urireg.db.dto.ShablonBookmark;
import com.ib.urireg.db.dto.ShablonFile;
import com.ib.urireg.db.dto.ShablonLogic;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.udostDocs.AdvancedUdostMethods;
import com.ib.urireg.udostDocs.AsposeUtils;
import com.ib.urireg.udostDocs.ContainerMethods;
import com.ib.urireg.udostDocs.CustomFillMethods;
import com.ib.urireg.udostDocs.DefaultValueMethods;
import com.ib.urireg.udostDocs.OnCompleteMethods;
import com.ib.urireg.udostDocs.UdostDokumentMethods;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.ValidationUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Страницата за настройване на шаблони и методи за попълване.
 * Взета е от BabhRegs и е модифицирана, за да може към документ
 * да се прикачват повече от един шаблон и логика за попълване.
 * Същото е направено и в компонентата за генериране на УД.
 *
 * @author n.kanev
 */
@Named("shabloniFill")
@ViewScoped
public class ShabloniFillBean extends IndexUIbean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ShabloniFillBean.class);

	private transient ShablonLogicDAO shabloniDao;
	private ShablonLogic shablonLogic;
	private ShablonFile shablonFile;

	// Всички документи, за които ще има УД от класификация 688
	private List<SelectItem> vidoveDokumenti;
	// Избраният документ от падащото меню;
	private Object[] selectedVidDokument;
	private List<ShablonLogic> shabloniForVidDokument;

	// bookmarks, които са записани в настоящия shablonLogic
	private List<ShablonBookmark> bookmarksInShablonLogic;
	// имена на bookmark-овете, които ги има в заредения doc файл
	private List<String> bookmarkLabelsInFile;
	private Map<String, String> oldIncorrectNames;
	// основният мап, в който се зареждат данните за настоящите букмаркове;
	// при избор на вид документ се проверява дали има разминаване между
	// букмарковете в шаблона и записаните вече в базата;
	private Map<String, ShablonBookmark> bookmarkMap;
	private List<String> passiveBookmarks;

	// Всички методи от UdostDokumentMethods
	private List<String> methods;
	// Неизползвани методи (UdostDokumentMethods)
	private List<String> unusedMethodNames;
	// Използвани методи (UdostDokumentMethods)
	private List<String> usedMethodNames;
	// Къде се използва методът getEmpty
	private List<Object[]> emptyMethodUsages;
	// за панела с търсене по име на метод
	private String selectedMethodToCheckUsage;
	// резултатите за търсене по име на метод
	private List<Object[]> selectedMethodUsages;
	// Методите от класа AdvancedUdostMethods
	private List<String> advMetodi;
	// Методите от класа OnCompleteMethods
	private List<String> onCompleteMetodi;
	// Методите от класа DefaultValueMethods
	private List<String> defaultValueMetodi;
	// Методите от класа CustomFillMethods
	private List<String> customMetodi;
	// Методите от класа ContainerMethods
	private List<String> containerMetodi;

	// Misc.
	private Set<String> componentsWithErrors;
	private boolean saveClickedOnce;
	// когато избираме падащото меню за подмяна на файл, тук записваме ИД, по което да подменим
	private Integer idOfFileToUpdate;


	@PostConstruct
	public void init() throws DbErrorException {

		this.shabloniDao = new ShablonLogicDAO(getUserData());

		try {
			this.vidoveDokumenti = new ArrayList<>();

			List<Object[]> templates = this.shabloniDao.findTemplates(getSystemData());

			if(templates != null) {
				templates.stream()
						.sorted(Comparator.comparingLong(t -> (Long) t[0]))
						.forEach(t -> {
							SelectItem item = new SelectItem(t, (String) t[1]);
							this.vidoveDokumenti.add(item);
						});
			}
		}
		catch (DbErrorException e) {
			LOGGER.error("Грешка при търсене на файлове-шаблони в базата.", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}
		finally {
			JPA.getUtil().closeConnection();
		}

		this.methods = Stream.of(UdostDokumentMethods.class.getDeclaredMethods())
				.map(Method::getName)
				.filter(m -> !m.contains("lambda$"))
				.collect(Collectors.toList());

		this.advMetodi = Stream.of(AdvancedUdostMethods.class.getDeclaredMethods())
				.map(Method::getName)
				.filter(m -> !m.contains("lambda$"))
				.collect(Collectors.toList());

		this.onCompleteMetodi = Stream.of(OnCompleteMethods.class.getDeclaredMethods())
				.map(Method::getName)
				.filter(m -> !m.contains("lambda$"))
				.collect(Collectors.toList());

		this.defaultValueMetodi = Stream.of(DefaultValueMethods.class.getDeclaredMethods())
				.map(Method::getName)
				.filter(m -> !m.contains("lambda$"))
				.collect(Collectors.toList());

		this.customMetodi = Stream.of(CustomFillMethods.class.getDeclaredMethods())
				.map(Method::getName)
				.filter(m -> !m.contains("lambda$") && !m.equals(CustomFillMethods.SIGN_METHOD_NAME))
				.collect(Collectors.toList());

		this.containerMetodi = Stream.of(ContainerMethods.class.getDeclaredMethods())
				.map(Method::getName)
				.filter(m -> !m.contains("lambda$"))
				.collect(Collectors.toList());

		refreshEmptyMethodUsages();
		refreshUnusedMethodNames();
	}

	/**
	 * Зарежда списъка с this.shabloniForVidDokument за избрания вид документ от падащото меню.
	 * Вика се при избор от падащото меню.
	 */
	public void loadShabloniForVidDokument() {
		try {
			this.shabloniForVidDokument = this.shabloniDao.findByDocVid((((Long) this.selectedVidDokument[0])).intValue());
			if(this.shabloniForVidDokument != null) {
				this.shabloniForVidDokument.sort(Comparator.comparingInt(ShablonLogic::getId));
			}
			if(this.shabloniForVidDokument != null) {
				loadShablonFile(this.shabloniForVidDokument.get(0));
			}
			else {
				loadShablonFile(null);
			}
		}
		catch(DbErrorException e) {
			LOGGER.error("Грешка при работа с базата", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}
	}

	/**
	 * При кликване върху файл, зарежда шаблона и букмарковете.
	 * @param shablonLogic обектът, върху който сме кликнали
	 */
	public void actionChooseShablon(ShablonLogic shablonLogic) {
		if(shablonLogic == null) {
			loadShablonFile(null);
			return;
		}

		// вече е кликнато върху файл
		if(this.shablonLogic != null) {
			// кликнато е повторно върху сегашния линк. не правим нищо.
			if(Objects.equals(this.shablonLogic.getId(), shablonLogic.getId())) {
				return;
			}
			else { // кликнат е друг линк; изкарвам предупреждение, за да не се загубят промените.
				PrimeFaces.current().executeScript("PF('cd-" + shablonLogic.getId() + "').show();");
				return;
			}
		}
		else {
			loadShablonFile(shablonLogic);
		}
	}

	public void loadShablonFile(ShablonLogic shablonLogic) {

		if(shablonLogic == null) {
			this.shablonLogic = null;
			this.shablonFile = null;
			this.bookmarksInShablonLogic = null;
			this.bookmarkLabelsInFile = null;
			return;
		}

		System.out.println("Loading: " +  shablonLogic.getId());
		try {
			this.saveClickedOnce = false;
			this.shablonLogic = shablonLogic;
			this.shablonFile = this.shabloniDao.loadFile(this.shablonLogic.getId());
			this.bookmarksInShablonLogic = this.shablonLogic.getBookmarks();

			this.bookmarkLabelsInFile = getBookmarkLabelsFromWordFile();
			makeBookmarksMap();
			highlightErrors();
		}
		catch(DbErrorException e) {
			LOGGER.error("Грешка при работа с базата", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}
		catch(Exception e) {
			LOGGER.error("Грешка при инициализиране на Aspose лиценз", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}

	private List<String> getBookmarkLabelsFromWordFile() throws Exception {
		Document wordFile = AsposeUtils.getWordFileFromBytes(this.shablonFile.getContent());
		return AsposeUtils.getBookmarkLabelsFromWordFile(wordFile);
	}

	/**
	 * Този метод е много важен
	 */
	private void makeBookmarksMap() {

		if(this.bookmarkMap == null) {
			this.bookmarkMap = new HashMap<>();
		}
		else {
			this.bookmarkMap.clear();
		}

		if(this.passiveBookmarks == null) {
			this.passiveBookmarks = new ArrayList<>();
		}
		else {
			this.passiveBookmarks.clear();
		}

		for(String label : this.bookmarkLabelsInFile) {
			ShablonBookmark bookmark = this.bookmarksInShablonLogic
					.stream()
					.filter(b -> b.getLabel().equals(label))
					.findFirst()
					.orElse(null);
			// във файла има bm, който липсва в досегашната логика (добавен е впоследствие във файла)
			if(bookmark == null) {
				bookmark = new ShablonBookmark();
				bookmark.setLabel(label);
			}

			if(bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.PASSIVE) {
				this.passiveBookmarks.add(label);
			}

			this.bookmarkMap.put(label, bookmark);
		}

		// тук проверявам обратното - ако в базата има вече попълнен bm, който после е изтрит от уърдовския файл
		boolean shouldDeleteBookmarks = false;
		for(int i = 0; i < this.bookmarksInShablonLogic.size(); i++) {
			ShablonBookmark bookmark = this.bookmarksInShablonLogic.get(i);
			String label = this.bookmarkLabelsInFile
					.stream()
					.filter(b -> b.equals(bookmark.getLabel()))
					.findFirst()
					.orElse(null);

			if(label == null) {
				// bookmark е изтрит от файла; изтривам го и от логиката
				this.bookmarksInShablonLogic.set(i, null);
				shouldDeleteBookmarks = true;
			}
		}
		if(shouldDeleteBookmarks) {
			try {
				this.bookmarksInShablonLogic.removeIf(b -> b == null);
				JPA.getUtil().runInTransaction(() -> {
					this.shabloniDao.save(this.shablonLogic);
				});
			}
			catch (BaseException e) {
				LOGGER.error("Грешка при работа с базата", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
				scrollToMessages();
			}
			finally {
				JPA.getUtil().closeConnection();
			}
		}
	}

	/**
	 * При зареждане на шаблон, проверява дали записаните имена на методи са още валидни.
	 * Ако в букмарк е бил записано име на метод, но после методът е бил преименуван в UdostDokumentMethods,
	 * ще огради името в червено, за да се избере новото име.
	 */
	private void highlightErrors() {
		if(this.oldIncorrectNames == null) {
			this.oldIncorrectNames = new HashMap<>();
		}
		else {
			this.oldIncorrectNames.clear();
		}

		for(int i = 0; i < this.bookmarkLabelsInFile.size(); i++) {
			String label = this.bookmarkLabelsInFile.get(i);
			ShablonBookmark bookmark = this.bookmarkMap.get(label);

			if(bookmark.getId() != null
					&& bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.NORMAL
					&& (!this.methods.contains(bookmark.getMethodName()))) {
				this.oldIncorrectNames.put(label, bookmark.getMethodName());
				bookmark.setMethodName(null);
				String script = "$(document.getElementById('formShabloniFill:logicTable:" + i + ":fillMethod')).addClass('ui-state-error')"; // TODO check
				PrimeFaces.current().executeScript(script);
			}
		}
	}

	public boolean showWarningIcon() {

		if(this.shablonLogic.getId() == null || this.bookmarkMap == null) {
			return true;
		}

		for(Map.Entry<String, ShablonBookmark> entry : this.bookmarkMap.entrySet()) {
			ShablonBookmark currentBookmark = entry.getValue();
			if(currentBookmark.getMethodName() != null && currentBookmark.getMethodName().equals(UdostDokumentMethods.EMPTY_METHOD_NAME)) {
				return true;
			}
			if(currentBookmark.getId() == null) {
				return true;
			}
		}

		return false;
	}

	public boolean showErrorIcon() {
		return (this.oldIncorrectNames) != null && (!this.oldIncorrectNames.isEmpty());
	}

	public String getIconClassForBmType(ShablonBookmark.BookmarkTypes bmType) {
		if(bmType == null) {
			return "";
		}

		switch (bmType) {
			case STRING : return "fas fa-font";
			case LIST : return "fas fa-list";
			case TABLE : return "fas fa-table";
			case TREE : return "fas fa-tree";
			default : return "";
		}
	}

	public boolean expandedRow(int index) {
		return this.componentsWithErrors != null
				&& this.componentsWithErrors.stream()
				.filter(s -> Integer.parseInt(s.substring(0, s.indexOf(":"))) == index)
				.anyMatch(s ->
						s.contains("description")
							|| s.contains("fillComponent")
							|| s.contains("fillMethodCustom")
							|| s.contains("fillMethodAdv")
							|| s.contains("selectManyChildBookmarks")
							|| s.contains("fillMethodContainer")
							|| s.contains("selectSignDlajnost"));
	}

	public boolean componentHasError(String compName) {
		return this.componentsWithErrors != null
				&& this.componentsWithErrors.contains(compName);
	}

	public boolean isInContainer(String label) {
		for(String key : this.bookmarkMap.keySet()) {
			ShablonBookmark bookmark = this.bookmarkMap.get(key);
			if(bookmark.getFillStrategy().equals(ShablonBookmark.FillStrategies.CONTAINER)
					&& bookmark.getFillsAlso() != null
					&& bookmark.getFillsAlso().contains(label)) {
				return true;
			}
		}

		return false;
	}

	public void setTheEmptyMethod(String bookmarkLabel, int tableIndex) {
		this.bookmarkMap.get(bookmarkLabel).setMethodName(UdostDokumentMethods.EMPTY_METHOD_NAME);
		this.bookmarkMap.get(bookmarkLabel).setBookmarkType(ShablonBookmark.BookmarkTypes.STRING);
		if(this.componentsWithErrors != null) {
			this.componentsWithErrors.remove(tableIndex + ":fillMethod");
			this.componentsWithErrors.remove(tableIndex + ":fillType");
		}
	}

	public void changeFillStrategy(String label) {
		ShablonBookmark bookmark = this.bookmarkMap.get(label);
		switch(bookmark.getFillStrategy()) {
			case NORMAL : {
				bookmark.setDescription(null);
				this.passiveBookmarks.remove(label);
				removeBookmarkNameFromFillOther(label);
				break;
			}
			case ADVANCED : {
				bookmark.setMethodName(null);
				bookmark.setBookmarkType(null);
				this.passiveBookmarks.remove(label);
				removeBookmarkNameFromFillOther(label);
				break;
			}
			case PASSIVE : {
				bookmark.setDescription(null);
				bookmark.setMethodName(null);
				bookmark.setBookmarkType(null);
				this.passiveBookmarks.add(label);
				break;
			}
			case SIGN_FIELD : {
				bookmark.setMethodName(CustomFillMethods.SIGN_METHOD_NAME);
				bookmark.setBookmarkType(null);
				bookmark.setDescription(null);
				this.passiveBookmarks.remove(label);
				removeBookmarkNameFromFillOther(label);
				break;
			}
		}
	}

	private void removeBookmarkNameFromFillOther(String label) {
		this.bookmarkMap.forEach((k, v) -> {
			if(v.getFillsAlso() != null && v.getFillsAlso().contains(label)) {
				v.getFillsAlso().remove(label);
			}
		});
	}

	public void actionSave() {
		for(String label : this.bookmarkMap.keySet()) {
			LOGGER.info(this.bookmarkMap.get(label).toString());
		}

		boolean valid = true;
		if(this.componentsWithErrors == null) {
			this.componentsWithErrors = new HashSet<>();
		}
		else {
			this.componentsWithErrors.clear();
		}

		for(int i = 0; i < this.bookmarkLabelsInFile.size(); i++) {
			String label = this.bookmarkLabelsInFile.get(i);
			ShablonBookmark bookmark = this.bookmarkMap.get(label);

			valid &= validateBookmark(bookmark, i);
		}

		if(!valid) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Таблицата не е попълнена изцяло!");
			scrollToMessages();
			return;
		}

		saveConfirmed();
	}

	private boolean validateBookmark(ShablonBookmark bookmark, int tableIndex) {
		boolean valid = true;
		this.saveClickedOnce = true;

		if(bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.NORMAL) {
			if (!ValidationUtils.isNotBlank(bookmark.getMethodName())) {
				valid = false;
				this.componentsWithErrors.add(tableIndex + ":fillMethod");
			}
			if (bookmark.getBookmarkType() == null) {
				valid = false;
				this.componentsWithErrors.add(tableIndex + ":fillType");
			}
		}
		else if(bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.ADVANCED) {
			if (!ValidationUtils.isNotBlank(bookmark.getMethodName())) {
				valid = false;
				this.componentsWithErrors.add(tableIndex + ":fillMethodAdv");
			}
			if(!ValidationUtils.isNotBlank(bookmark.getDescription())) {
				valid = false;
				this.componentsWithErrors.add(tableIndex + ":description");
			}
			if(bookmark.getFillComponent() == null) {
				valid = false;
				this.componentsWithErrors.add(tableIndex + ":fillComponent");
			}
		}
		else if(bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.CUSTOM_IMPL) {
			if(!ValidationUtils.isNotBlank(bookmark.getMethodName())) {
				valid = false;
				this.componentsWithErrors.add(tableIndex + ":fillMethodCustom");
			}
		}
		else if(bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.CONTAINER) {
			if(bookmark.getFillsAlso() == null || bookmark.getFillsAlso().isEmpty()) {
				valid = false;
				this.componentsWithErrors.add(tableIndex + ":selectManyChildBookmarks");
			}
			if(!ValidationUtils.isNotBlank(bookmark.getMethodName())) {
				valid = false;
				this.componentsWithErrors.add(tableIndex + ":fillMethodContainer");
			}
		}
		else if(bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.SIGN_FIELD) {
			if(!ValidationUtils.isNotBlank(bookmark.getDefaultValueMethodName())) {
				valid = false;
				this.componentsWithErrors.add(tableIndex + ":selectSignDlajnost");
			}
		}

		return valid;
	}

	public void saveConfirmed() {
		try {
			this.shablonLogic.getBookmarks().clear();
			this.shablonLogic.getBookmarks().addAll(new ArrayList<>(this.bookmarkMap.values()));
			JPA.getUtil().runInTransaction(() -> {
				this.shablonLogic = shabloniDao.save(this.shablonLogic);
			});
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
			this.bookmarksInShablonLogic = this.shablonLogic.getBookmarks();
			makeBookmarksMap(); // рефреш на обектите, ако има промени по файла и новозаписани
			this.oldIncorrectNames.clear();
		}
		catch (BaseException e) {
			LOGGER.error("Грешка при запис на ShablonLogic", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при запис в базата");
			scrollToMessages();
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void downloadFile(Integer id) {
		try {
			ShablonFile file = shabloniDao.loadFile(id);
			downloadFile(file.getContent(), file.getFilename());
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Качваме нов шаблон към избрания вид документ от падащото меню.
	 * За ъпдейтване на шаблона има отделен метод {@link ShabloniFillBean#updateFile}
	 * @param event В event.getFile() се съдържа каченият файл.
	 */
	public void uploadFile(final FileUploadEvent event) {

		try {
			// това не би трябвало да се случи; за всеки случай
			if(this.selectedVidDokument == null) {
				return;
			}

			// качва се изцяло нов шаблон за избрания вид документ
			JPA.getUtil().runInTransaction(() -> {
				UploadedFile file = event.getFile();
				ShablonLogic newLogic = new ShablonLogic();
				ShablonFile newFile = new ShablonFile();

				newLogic.setDocVid(((Long) this.selectedVidDokument[0]).intValue());
				newFile.setFilename(file.getFileName());
				newFile.setContentType(file.getContentType());
				newFile.setContent(file.getContent());

				newLogic = this.shabloniDao.save(newLogic);
				newFile.setId(newLogic.getId());
				newFile.setUserReg(newLogic.getUserReg());
				newFile.setDateReg(newLogic.getDateReg());
				this.shabloniDao.saveFile(newFile);
			});

			loadShabloniForVidDokument();
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Файлът е записан");
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при работа с базата");
			this.shablonFile = null;
			scrollToMessages();
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**
	 * Ъпдейтва се файлът на шаблона към избрания вид документ от падащото меню.
	 * @param event В event.getFile() се съдържа каченият файл.
	 */
	public void updateFile(final FileUploadEvent event) {

		try {
			// това не би трябвало да се случи; за всеки случай
			if(this.selectedVidDokument == null) {
				return;
			}

			// качва се изцяло нов шаблон за избрания вид документ
			JPA.getUtil().runInTransaction(() -> {
				UploadedFile file = event.getFile();
				ShablonFile fileToUpdate = this.shabloniDao.loadFile(this.idOfFileToUpdate);

				fileToUpdate.setFilename(file.getFileName());
				fileToUpdate.setContent(file.getContent());
				fileToUpdate.setContentType(file.getContentType());
				this.shabloniDao.saveFile(fileToUpdate);
			});
			loadShabloniForVidDokument();
			loadShablonFile(this.shablonLogic);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Файлът е записан");
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при работа с базата");
			this.shablonFile = null;
			scrollToMessages();
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void deleteFile(Integer id) {
		try {
			JPA.getUtil().runInTransaction(() -> {
				this.shabloniDao.deleteById(id);
			});

			if(this.shablonLogic != null && this.shablonLogic.getId().equals(id)) {
				this.shablonLogic = null;
				this.shablonFile = null;
				this.bookmarksInShablonLogic = null;
				this.bookmarkMap = null;
				this.selectedMethodToCheckUsage = null;
			}

			loadShabloniForVidDokument();

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Изтриването е успешно");
		}
		catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на ShablonLogic", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при изтриване от базата");
			scrollToMessages();
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void refreshUsedMethodNames() {
		try {
			this.usedMethodNames = this.shabloniDao.findCurrentlyUsedMethodNames()
					.stream()
					.filter(Objects::nonNull)
					.map(n -> (String) n)
					.collect(Collectors.toCollection(ArrayList::new));
		}
		catch (DbErrorException e) {
			LOGGER.error("Грешка при търсене на имена на методи в базата", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при търсене на имена на методи в базата");
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void refreshUnusedMethodNames() {
		refreshUsedMethodNames();

		this.unusedMethodNames = this.methods
				.stream()
				.filter(m -> !usedMethodNames.contains(m))
				.filter(m -> !m.equals(UdostDokumentMethods.EMPTY_METHOD_NAME))
				.collect(Collectors.toList());
	}

	public void refreshSelectedMethodUsages() {
		try {
			this.selectedMethodUsages = this.shabloniDao.findMethodUsage(this.selectedMethodToCheckUsage);
		}
		catch (DbErrorException e) {
			LOGGER.error("Грешка при търсене на имена на методи в базата", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при търсене на имена на методи в базата");
			scrollToMessages();
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void refreshEmptyMethodUsages() {
		try {
			this.emptyMethodUsages = this.shabloniDao.findMethodUsage(UdostDokumentMethods.EMPTY_METHOD_NAME);
		}
		catch (DbErrorException e) {
			LOGGER.error("Грешка при търсене на имена на методи в базата", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при търсене на имена на методи в базата");
			scrollToMessages();
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**
	 * Кликнат е линк в екрана в секциите с неизползвани методи и getEmpty
	 * @param docVid
	 * @param logicId
	 */
	public void shablonLinkClicked(Object docVid, Object logicId) {

		this.selectedVidDokument = this.vidoveDokumenti
				.stream()
				.map(selectItem -> (Object[]) selectItem.getValue())
				.filter(obj -> obj[0].equals(docVid))
				.findFirst()
				.orElse(null);

		loadShabloniForVidDokument();

		ShablonLogic logic = this.shabloniForVidDokument.stream()
				.filter(s -> s.getId().equals(((Long) logicId).intValue()))
				.findFirst()
				.orElse(null);

		actionChooseShablon(logic);
	}

	private void downloadFile(byte[] content, String filename) throws IOException {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();

		HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
		String agent = request.getHeader("user-agent");

		String codedfilename = "";

		if (null != agent && (agent.contains("MSIE") || agent.contains("Mozilla") && agent.contains("rv:11") || agent.contains("Edge"))) {
			codedfilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
		}
		else if (null != agent && agent.contains("Mozilla")) {
			codedfilename = MimeUtility.encodeText(filename, "UTF8", "B");
		}
		else {
			codedfilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
		}

		externalContext.setResponseHeader("Content-Type", "application/x-download");
		externalContext.setResponseHeader("Content-Length", content.length + "");
		externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
		externalContext.getResponseOutputStream().write(content);

		facesContext.responseComplete();
	}

	private void scrollToMessages() {
		PrimeFaces.current().executeScript("scrollToErrors()");
	}


	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


	public List<SelectItem> getVidoveDokumenti() {
		return vidoveDokumenti;
	}

	public Object[] getSelectedVidDokument() {
		return selectedVidDokument;
	}

	public void setSelectedVidDokument(Object[] vidDokument) {
		this.selectedVidDokument = vidDokument;
	}

	public ShablonLogic getShablonLogic() {
		return shablonLogic;
	}

	public ShablonFile getShablonFile() {
		return shablonFile;
	}

	public List<String> getUsedMethodNames() {
		return usedMethodNames;
	}

	public List<String> getUnusedMethodNames() {
		return unusedMethodNames;
	}

	public List<Object[]> getEmptyMethodUsages() {
		return emptyMethodUsages;
	}

	public String getSelectedMethodToCheckUsage() {
		return selectedMethodToCheckUsage;
	}

	public void setSelectedMethodToCheckUsage(String selectedMethodToCheckUsage) {
		this.selectedMethodToCheckUsage = selectedMethodToCheckUsage;
	}

	public List<String> getMethods() {
		return methods;
	}

	public List<Object[]> getSelectedMethodUsages() {
		return selectedMethodUsages;
	}

	public Map<String, String> getOldIncorrectNames() {
		return oldIncorrectNames;
	}

	public List<String> getBookmarkLabelsInFile() {
		return bookmarkLabelsInFile;
	}

	public List<String> getPassiveBookmarks() {
		return passiveBookmarks;
	}

	public Map<String, ShablonBookmark> getBookmarkMap() {
		return bookmarkMap;
	}

	public boolean isSaveClickedOnce() {
		return saveClickedOnce;
	}

	public List<String> getAdvMetodi() {
		return advMetodi;
	}

	public List<String> getOnCompleteMetodi() {
		return onCompleteMetodi;
	}

	public List<String> getDefaultValueMetodi() {
		return defaultValueMetodi;
	}

	public List<String> getCustomMetodi() {
		return customMetodi;
	}

	public List<String> getContainerMetodi() {
		return containerMetodi;
	}

	public ShablonBookmark.BookmarkTypes[] getBookmarkTypes() {
		return ShablonBookmark.BookmarkTypes.values();
	}

	public ShablonBookmark.FillComponents[] getFillComponents() {
		return ShablonBookmark.FillComponents.values();
	}

	public List<ShablonLogic> getShabloniForVidDokument() {
		return shabloniForVidDokument;
	}

	public void setShabloniForVidDokument(List<ShablonLogic> shabloniForVidDokument) {
		this.shabloniForVidDokument = shabloniForVidDokument;
	}

	public void setIdOfFileToUpdate(Integer idOfFileToUpdate) {
		this.idOfFileToUpdate = idOfFileToUpdate;
	}

}
