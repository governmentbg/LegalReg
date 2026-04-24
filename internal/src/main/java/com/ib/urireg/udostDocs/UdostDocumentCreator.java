package com.ib.urireg.udostDocs;

import com.aspose.words.Bookmark;
import com.aspose.words.Cell;
import com.aspose.words.CompositeNode;
import com.aspose.words.ConvertUtil;
import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.LineSpacingRule;
import com.aspose.words.ListTemplate;
import com.aspose.words.Paragraph;
import com.aspose.words.Row;
import com.aspose.words.SaveFormat;
import com.aspose.words.Table;
import com.ib.urireg.db.dao.ShablonLogicDAO;
import com.ib.urireg.db.dto.ShablonBookmark;
import com.ib.urireg.db.dto.ShablonBookmark.FillStrategies;
import com.ib.urireg.db.dto.ShablonFile;
import com.ib.urireg.db.dto.ShablonLogic;
import com.ib.urireg.system.UriregConstants;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Клас, който служи за попълване на уърдовски шаблони на удостоверителни документи.
 * <br/>
 * След като се създаде обектът, автоматично зарежда наличните шаблони. Нужно е да
 * се провери предварително дали има повече от един ({@link #needsToChooseShablon()})
 * и да се сетне ({@link #setChosenShablon(ShablonFile)}).
 * </br>
 * След това се вика {@link #gatherData()}, за да се заредят текстовите стойности.
 * Всички стойности се запазват в мапа {@link #getValues()}, а празните са в
 * {@link #getNullValues()}.
 * </br>
 * За да се създаде попълнен шаблон, се вика {@link #fillBookmarksInShablon()}.
 * </br>
 * Шаблонът може да се върне като html стринг с {@link #getDocumentAsHtmlString()}
 * след като е попълнен.
 * </br>
 * Записът на попълнения шаблон като файл в базата се прави от компонентата CompUdostDokument като вика подаден външен метод.
 *
 * @author n.kanev
 */
public class UdostDocumentCreator {

	private static final Logger LOGGER = LoggerFactory.getLogger(UdostDocumentCreator.class);
	private static final String EMPTY_VALUE = "";
	private static final String DUBLIKAT_TEKST = " - Дубликат";

	public static final String LIST_DELIMITER = "<._.>";
	public static final String TABLE_ROW_DELIMITER = "<o_O>";
	public static final String TREE_ATTRIBUTES_DELIMITER = "<*_*>";
	public static final String HIDE_BOOKMARK_FLAG = "<x_x>";

	public static final String DUBLIKAT_BM_LABEL = "dublikat";

	public static final String KEY_DOC_ID = "docId";
	public static final String KEY_LICE_ID = "liceId";
	public static final String KEY_DUBLIKAT = "dublikat";
	public static final String KEY_DOK_VID = "vidDok";

	private static final String WHITE_BULLET = "\u25e6"; // ◦
	private static final String BLACK_BULLET = "\u2022"; // •

	// Данни, които се подават отвън при инициализиране
	private final UserData ud;
	private final Map<String, Object> additionalData;
	private final Integer docVid;

	// private данни, които се създават тук
	private List<ShablonLogic> shabloni;
	private ShablonFile chosenShablon;
	private ShablonLogic shablonLogic;
	private List<ShablonBookmark> bookmarks;
	private Document shablonDocument;
	private Map<String, String> values;
	private Set<String> nullValues;
	private Set<String> killedBookmarks;
	private final UdostDokumentMethods methodsObject;
	private final AdvancedUdostMethods advMethodsObject;
	private final DefaultValueMethods defaultMethodsObject;
	private final CustomFillMethods customMethodsObject;
	private final ContainerMethods containerMethodsObject;
	private Map<String, Object> advancedBookmarkValues;

	public UdostDocumentCreator(SystemData sd, UserData ud, Integer docVid, Map<String, Object> additionalData)
			throws MissingSystemSettingException, DbErrorException, NoShablonException {
		this.ud = ud;
		this.additionalData = additionalData;
		this.docVid = docVid;
		this.methodsObject = new UdostDokumentMethods(ud, sd, additionalData);
		this.advMethodsObject = new AdvancedUdostMethods(ud, sd, additionalData);
		this.defaultMethodsObject = new DefaultValueMethods(ud, sd, additionalData);
		this.customMethodsObject = new CustomFillMethods(ud, sd, additionalData);
		this.containerMethodsObject = new ContainerMethods(ud, sd, additionalData);
		initialize();
	}

	/**
	 * Намира шаблоните към дадения документ.
	 * Ако е един, го зарежда в {@link #chosenShablon}.
	 * Ако са повече от един, {@link #needsToChooseShablon()} връща true
	 * и трябва да се извика методът {@link #setChosenShablon(ShablonFile)}.
	 * Наличните шаблони могат да се вземат с {@link #getShabloni()}.
	 *
	 * @throws MissingSystemSettingException
	 * @throws DbErrorException
	 * @throws NoShablonException
	 */
	private void initialize() throws MissingSystemSettingException, DbErrorException, NoShablonException {
		try {
			ShablonLogicDAO logicDao = new ShablonLogicDAO(this.ud);
			List<ShablonLogic> list = logicDao.findByDocVid(this.docVid);

			if (list == null || list.isEmpty()) {
				LOGGER.error(String.format("Към документ %d няма въведени шаблони", this.docVid));
				throw new NoShablonException(String.format("Към документ %d няма въведени шаблони", this.docVid));
			}

			this.shabloni = list;

			if(list.size() == 1) { // само един прикачен шаблон за вида документ
				this.shablonLogic = list.get(0);
				this.chosenShablon = logicDao.loadFile(shablonLogic.getId());
			}
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**
	 * Събира данните за шаблона и ги запазва във {@link #values}.
	 * Ако стойност липсва, името на съответния букмарк отива в {@link #nullValues}.
	 *
	 * @throws NoSuchMethodException в шаблона е зададен метод, който липсва в класа {@link UdostDokumentMethods}
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException грешка при извикване на метод от {@link UdostDokumentMethods}
	 * @throws InvocationTargetException
	 * @throws DbErrorException
	 */
	public void gatherData()
			throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, DbErrorException {

		if(this.shablonLogic == null) {
			throw new MissingBookmarkLogicException("Не е настроена логика на попълване на шаблона");
		}

		ShablonLogicDAO logicDao = new ShablonLogicDAO(this.ud);
		this.chosenShablon = logicDao.loadFile(shablonLogic.getId());

		if(this.chosenShablon == null) {
			LOGGER.error("Документът има повече от един налични шаблона и не е извикан методът setChosenShablon, за да се избере кой да се попълни");
			throw new NoChosenShablonException("Документът има повече от един налични шаблона и не е извикан методът setChosenShablon, за да се избере кой да се попълни");
		}

		this.bookmarks = this.shablonLogic.getBookmarks();

		this.values = new HashMap<>();
		this.nullValues = new HashSet<>();
		this.advancedBookmarkValues = new HashMap<>();
		this.killedBookmarks = new HashSet<>();

		// Минавам първо веднъж само през тези букмаркове, за да определя има ли такива, които да се игнорират
		for (ShablonBookmark bookmark : this.bookmarks) {
			try {
				if (bookmark.getFillStrategy() == FillStrategies.CONTAINER) {
					Method fillerMethod = ContainerMethods.class.getMethod(bookmark.getMethodName());
					Object returnValue = fillerMethod.invoke(this.containerMethodsObject);
					if (returnValue != null) {
						String result = (String) returnValue;
						if (result.equals(UdostDocumentCreator.HIDE_BOOKMARK_FLAG)) {
							this.killedBookmarks.addAll(bookmark.getFillsAlso());
							this.values.put(bookmark.getLabel(), UdostDocumentCreator.HIDE_BOOKMARK_FLAG);
						}
						else {
							this.killedBookmarks.addAll(bookmark.getFillsAlso());
							this.values.put(bookmark.getLabel(), result);
						}
					}
				}
			}
			catch (NoSuchMethodException e) {
				LOGGER.error("В класа UdostDokumentMethods не съществува метод " + bookmark.getMethodName());
				throw e;
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				LOGGER.error("Грешка при извикване на метод UdostDokumentMethods." + bookmark.getMethodName());
				throw e;
			}
		}

		// Зареждане на данните за документа
		// Нормалните букмаркове се попълват директно, но ADVANCED се слагат в мапа advancedBookmarksValues
		// и ще се покажат на екрана, за да им се прикачат компоненти за попълване.
		for(ShablonBookmark bookmark : this.bookmarks) {
			if(this.killedBookmarks.contains(bookmark.getLabel())) {
				continue;
			}

			try {
				if(bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.NORMAL) {
					Method fillerMethod = UdostDokumentMethods.class.getMethod(bookmark.getMethodName());
					Object returnValue = fillerMethod.invoke(this.methodsObject);
					if (returnValue != null) {
						String result = (String) returnValue;
						this.values.put(bookmark.getLabel(), result);
					}
					else {
						this.nullValues.add(bookmark.getLabel());
					}
				}

				else if(bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.ADVANCED) {
					if(bookmark.getDefaultValueMethodName() != null) {
						Method fillerMethod = DefaultValueMethods.class.getMethod(bookmark.getDefaultValueMethodName());
						Object returnValue = fillerMethod.invoke(this.defaultMethodsObject);
						this.advancedBookmarkValues.put(bookmark.getLabel(), returnValue);
						this.fillAdvancedBookmark(bookmark, returnValue);
					}
					else {
						this.advancedBookmarkValues.put(bookmark.getLabel(), null);
					}
				}
				else if(bookmark.getFillStrategy() == ShablonBookmark.FillStrategies.PASSIVE) {
					// TODO ?
				}
				else if (bookmark.getFillStrategy() == FillStrategies.CONTAINER) {
					// Веднъж вече съм минал през тези и няма нужда да се гледат отново
					continue;
				}
			}
			catch (NoSuchMethodException e) {
				LOGGER.error("В класа UdostDokumentMethods не съществува метод " + bookmark.getMethodName());
				throw e;
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				LOGGER.error("Грешка при извикване на метод UdostDokumentMethods." + bookmark.getMethodName());
				throw e;
			}
		}
	}

	// TODO tova da se vika kato e gotova redakciata i se klikne butona
	public void gatherAdvancedBookmarksData() {
		this.advancedBookmarkValues.forEach((key, value) -> {
			ShablonBookmark bookmark = this.bookmarks.stream().filter(b -> b.getLabel().equals(key)).findFirst().orElse(null);
			fillAdvancedBookmark(bookmark, value);
		});
	}

	private void fillAdvancedBookmark(ShablonBookmark bookmark, Object value) {
		try {
			Method fillerMethod = AdvancedUdostMethods.class.getMethod(bookmark.getMethodName(), Object.class, ShablonBookmark.class);
			Object returnValues = fillerMethod.invoke(this.advMethodsObject, value, bookmark);
			if (returnValues != null) {
				Map<String, String> map = (Map<String, String>) returnValues;
				map.forEach((key2, value2) -> {
					if(value2 == null) {
						this.nullValues.add(key2);
						this.values.remove(key2);
					}
					else {
						this.values.put(key2, value2);
						this.nullValues.remove(key2);
					}
				});
			}
		}
		catch (NoSuchMethodException e) {
			LOGGER.error("В класа AdvancedUdostMethods не съществува метод " + bookmark.getMethodName(), e);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOGGER.error("Грешка при извикване на метод AdvancedUdostMethods." + bookmark.getMethodName(), e);
		}
	}

	/**
	 * Записва данните от {@link #values} в уърдовския файл.
	 * Преди да се извика методът, трябва да е извикан {@link #gatherData()}.
	 * @throws Exception ако има грешка с Aspose
	 */
	public void fillBookmarksInShablon() throws Exception {

		// за да хвърли грешка, ако не е бил викнат преди това gatherData()
		getValues();

		this.shablonDocument = AsposeUtils.getWordFileFromBytes(this.chosenShablon.getContent());

		for(ShablonBookmark bookmark : this.bookmarks) {
			String bookmarkLabel = bookmark.getLabel();

			if(this.killedBookmarks.contains(bookmarkLabel)) {
				continue;
			}

			Bookmark b = this.shablonDocument.getRange().getBookmarks().get(bookmarkLabel);
			if(b != null) {
				if(bookmark.getFillStrategy() == FillStrategies.CONTAINER) {
					if(this.values.get(bookmarkLabel) != null) {
						if(this.values.get(bookmarkLabel).equals(UdostDocumentCreator.HIDE_BOOKMARK_FLAG)) {
							b.setText("");
						}
						else {
							b.setText(this.values.get(bookmarkLabel));
						}
					}
				}
				else if(bookmark.getFillStrategy() == FillStrategies.CUSTOM_IMPL) {
					Method fillerMethod = CustomFillMethods.class.getMethod(bookmark.getMethodName(), Document.class, ShablonBookmark.class);
					fillerMethod.invoke(this.customMethodsObject, this.shablonDocument, bookmark);
				}
				else if(bookmark.getFillStrategy() == FillStrategies.SIGN_FIELD) {
					if(bookmark.getMethodName() != null // тук е името на метода от CustomFillMethods
							&& bookmark.getDefaultValueMethodName() != null) { // тук е кодът на лицето от адм. структура
						Method fillerMethod = CustomFillMethods.class.getMethod(bookmark.getMethodName(), Document.class, ShablonBookmark.class, String.class);
						fillerMethod.invoke(this.customMethodsObject, this.shablonDocument, bookmark, bookmark.getDefaultValueMethodName());
					}
				}
				else if(!this.values.containsKey(bookmarkLabel)) {
					b.setText(EMPTY_VALUE);
				}
				else {
					if (bookmark.getBookmarkType() != null) {
						switch (bookmark.getBookmarkType()) {
							case STRING: {
								this.fillStringBookmark(b, bookmarkLabel);
								break;
							}
							case LIST: {
								this.fillListBookmark(bookmarkLabel);
								break;
							}
							case TABLE: {
								this.fillTableBookmark(b, bookmarkLabel);
								break;
							}
							case TREE: {
								this.fillTreeBookmark(bookmarkLabel);
								break;
							}
							default:
								break;
						}
					}
					else {
						this.fillStringBookmark(b, bookmarkLabel);
					}
				}
			}
			else {
				LOGGER.error("Документът не съдържа bookmark с името " + bookmarkLabel);
				throw new WrongBookmarkNameException("Документът не съдържа bookmark с името " + bookmarkLabel);
			}
		}

		if(this.additionalData.get(KEY_DUBLIKAT) != null && (boolean) this.additionalData.get(KEY_DUBLIKAT)) {
			Bookmark b = this.shablonDocument.getRange().getBookmarks().get(DUBLIKAT_BM_LABEL);
			if(b != null) {
				b.setText(DUBLIKAT_TEKST);
			}
		}
	}

	/**
	 * Връща съдържанието на шаблона като html, за да се покаже на екрана.
	 * Трябва да се извика след като се попълнят данните.
	 * @return html String
	 * @throws Exception
	 */
	public String getDocumentAsHtmlString() throws Exception {
		try {
			return AsposeUtils.getDocumentAsHtml(this.shablonDocument);
		}
		catch(NullPointerException e) {
			throw new ValuesNotGeneratedException("Не са извикани методите gatherData и fillBookmarksInShablon");
		}
		catch (Exception e) {
			LOGGER.error("Грешка при запис на HTML");
			throw e;
		}
	}

	private void fillStringBookmark(Bookmark b, String bookmarkLabel) throws Exception {
		b.setText(this.values.get(bookmarkLabel));
	}

	private void fillListBookmark(String bookmarkLabel) throws Exception {
		String[] values = this.values.get(bookmarkLabel).split(LIST_DELIMITER);
		DocumentBuilder builder = new DocumentBuilder(this.shablonDocument);
		builder.moveToBookmark(bookmarkLabel);
		builder.getListFormat().setList(this.shablonDocument.getLists().add(ListTemplate.NUMBER_ARABIC_DOT));

		for(int i = 0; i < values.length; i++) {
			String string = values[i];
			if(string != null) {
				builder.write(string.toString().trim());
				if(i != values.length - 1) {
					builder.writeln();
				}
			}
		}
	}

	private void fillTableBookmark(Bookmark b, String bookmarkLabel) {
		CompositeNode node = b.getBookmarkStart().getParentNode();
		Table targetTable = null;
		while(node != null) {
        	if(node instanceof Table) {
        		targetTable = (Table) node;
        		break;
        	}
        	node = node.getParentNode();
        }
		if(targetTable == null) {
			// TODO error message
			return;
		}

		// изтриват се всички празни редове след антетката
		int initialTableRows = targetTable.getRows().getCount();
		for(int i = 1; i < initialTableRows; i++) {
    		Row row = targetTable.getRows().get(1);
    		targetTable.getRows().remove(row);
    	}

		DocumentBuilder builder = new DocumentBuilder(this.shablonDocument);
		String dataString = this.values.get(bookmarkLabel);
		List<String> rowsList = Arrays.asList(dataString.split(Pattern.quote(TABLE_ROW_DELIMITER), -1));

		for(String dataRowString : rowsList) {
    		Row row = new Row(this.shablonDocument);
    		targetTable.appendChild(row);
    		List<String> cellsList = Arrays.asList(dataRowString.split(Pattern.quote(LIST_DELIMITER), -1));

    		for(String dataCell : cellsList) {
    			Cell cell = new Cell(this.shablonDocument);
    			row.appendChild(cell);
    			cell.appendChild(new Paragraph(this.shablonDocument));
    			builder.moveTo(cell.getFirstParagraph());
    			builder.write(dataCell);
    		}
    	}
	}

	private void fillTreeBookmark(String bookmarkLabel) throws Exception {
		DocumentBuilder builder = new DocumentBuilder(this.shablonDocument);
		builder.moveToBookmark(bookmarkLabel);

		String dataString = this.values.get(bookmarkLabel);
		String[] treeRows = dataString.split(Pattern.quote(LIST_DELIMITER));

		builder.getCurrentParagraph().getParagraphFormat().setLineSpacingRule(LineSpacingRule.MULTIPLE);
		builder.getCurrentParagraph().getParagraphFormat().setLineSpacing(12);
		builder.writeln();

		for(String rowString : treeRows) {
			String[] rowData = rowString.split(Pattern.quote(TREE_ATTRIBUTES_DELIMITER));
			String text = rowData[0];
			int indentLevel = Integer.parseInt(rowData[1]);
			int isMain = Integer.parseInt(rowData[2]);

			if(isMain == UriregConstants.CODE_ZNACHENIE_DA) {
				builder.getFont().setBold(true);
			}
			else {
				builder.getFont().setItalic(true);
			}
			// ляво подравняване с по 1 см за всяко следващо ниво
			builder.getCurrentParagraph().getParagraphFormat().setLeftIndent(indentLevel * ConvertUtil.millimeterToPoint(10));
			if(isMain == UriregConstants.CODE_ZNACHENIE_DA) {
				builder.write(BLACK_BULLET + " ");
			}
			else {
				builder.write(WHITE_BULLET + " ");
			}
			builder.write(text);

			builder.getFont().setBold(false);
			builder.getFont().setItalic(false);
			builder.writeln();

		}
	}


	/* * * * * * * * * * * * EXCEPTIONS * * * * * * * * * * * * * * * * * * * */

	public static class NoShablonException extends RuntimeException {

		private static final long serialVersionUID = 2132541975496907997L;

		public NoShablonException(String message) {
			super(message);
		}
	}

	public static class MissingSystemSettingException extends RuntimeException {

		private static final long serialVersionUID = -5121616202710447595L;

		public MissingSystemSettingException(String message) {
			super(message);
		}
	}

	public static class NoChosenShablonException extends RuntimeException {

		private static final long serialVersionUID = -6946524623808084727L;

		public NoChosenShablonException(String message) {
			super(message);
		}
	}

	public static class ValuesNotGeneratedException extends RuntimeException {

		private static final long serialVersionUID = -4186991690642212462L;

		public ValuesNotGeneratedException(String message) {
			super(message);
		}
	}

	public static class MissingBookmarkLogicException extends RuntimeException {

		private static final long serialVersionUID = -1252968140800725865L;

		public MissingBookmarkLogicException(String message) {
			super(message);
		}
	}

	public static class WrongBookmarkNameException extends RuntimeException {

		private static final long serialVersionUID = -1276078540644875185L;

		public WrongBookmarkNameException(String message) {
			super(message);
		}
	}

	public static class UdostDocAlreadySavedException extends RuntimeException {

		private static final long serialVersionUID = -5555961037426741406L;

		public UdostDocAlreadySavedException(String message) {
			super(message);
		}
	}

	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	/**
	 * @return true, ако към документа има повече от един наличен шаблон,
	 * и не е избран нито един, който да се използва.
	 */
	public boolean needsToChooseShablon() {
		return this.shabloni.size() > 1
				&& this.chosenShablon == null;
	}

	/**
	 * @return наличните за документа шаблони.
	 */
	public List<ShablonLogic> getShabloni() {
		return this.shabloni;
	}

	/**
	 * Ако {@link #needsToChooseShablon()} върне true, трябва с този метод
	 * да се избере кой наличен шаблон да се използва. Иначе методите за
	 * попълване ще хвърлят {@link NoChosenShablonException}.
	 * @param chosenShablon
	 */
	public void setChosenShablon(ShablonFile chosenShablon) {
		this.chosenShablon = chosenShablon;
	}

	public ShablonFile getChosenShablon() {
		return this.chosenShablon;
	}

	public void setShablonLogic(ShablonLogic shablonLogic) {
		this.shablonLogic = shablonLogic;
	}

	/**
	 * @return Името на букмарковете във файла, за които
	 * намерената стойност от метода в {@link UdostDokumentMethods} е null.
	 */
	public Set<String> getNullValues() {
		if(this.nullValues == null) {
			throw new ValuesNotGeneratedException("Не е извикан методът gatherData");
		}
		return this.nullValues;
	}

	/**
	 * @return Мап, в който са стойностите, които ще се попълнят в шаблона.
	 * Ключът е името на букмарка в уърдовския файл.
	 */
	public Map<String, String> getValues() {
		if(this.values == null) {
			throw new ValuesNotGeneratedException("Не е извикан методът gatherData");
		}
		return this.values;
	}

	/**
	 * @return списък с букмарковете в избрания шаблон
	 */
	public List<ShablonBookmark> getBookmarks() {
		if(this.bookmarks == null) {
			throw new ValuesNotGeneratedException("Не е извикан методът gatherData");
		}
		return this.bookmarks;
	}

	public byte[] getDocumentAsBytes() throws Exception {
		try(ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			this.shablonDocument.save(stream, SaveFormat.DOCX);
			return stream.toByteArray();
		}
	}

	public Map<String, Object> getAdvancedBookmarkValues() {
		return advancedBookmarkValues;
	}

	public Integer getDocVid() {
		return docVid;
	}

}
