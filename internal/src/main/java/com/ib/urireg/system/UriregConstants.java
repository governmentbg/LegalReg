package com.ib.urireg.system;

import com.ib.indexui.system.Constants;

/**
 * Константи за проекта DocuWork
 *
 * @author belev
 */
public class UriregConstants extends Constants {

	/** тил на логване в състемта */
	public static final String	LOGIN_TYPE			= "DOCU_WORK_LOGIN_TYPE";
	/** тип на логване използвайки LDAP */
	public static final String	LOGIN_TYPE_LDAP		= "DOCU_WORK_LOGIN_TYPE_LDAP";
	/** тип на логване използвайки база данни */
	public static final String	LOGIN_TYPE_DATABASE	= "DOCU_WORK_TYPE_DATABASE";
	/** дефолтен домайн на потребителите, които влизат чрез LDAP протокол */
	public static final String	DEFAULT_LDAP_DOMAIN	= "DOCU_WORK_DEFAULT_LDAP_DOMAIN";


	/** Код на класификация "Тип участник" */
	public static final int CODE_CLASSIF_REF_TYPE = 101;

	/** Код на класификация "Вид документ" */
	public static final int CODE_CLASSIF_DOC_VID = 104;

	/** Код на класификация "Административна структура за справки (+напуснали)" */
	public static final int CODE_CLASSIF_ADMIN_STR_REPORTS = 114;

	/** Код на класификация "Предназначение на файл" */
	public static final int CODE_CLASSIF_FILE_PURPOSE = 115;

	/** Код на класификация "Роля на референт в документ" */
	public static final int CODE_CLASSIF_DOC_REF_ROLE = 118;

	/** Код на класификация "Настройки на потребител" */
	public static final int CODE_CLASSIF_USER_SETTINGS = 137;

	/** Код на класификация "Дефинитивни права" */
	public static final int CODE_CLASSIF_DEF_PRAVA = 139;

	/** Код на класификация "Вид адрес" */
	public static final int CODE_CLASSIF_ADDR_TYPE = 140;

	/** Код на класификация "Вид документ в прикачен файл" */
	public static final int CODE_CLASSIF_DOC_VID_ATTACH = 141;

	/** Код на класификация "Регистратури" */
	public static final int CODE_CLASSIF_REGISTRATURI = 146;

	/** Код на класификация "Групи служители" */
	public static final int CODE_CLASSIF_GROUP_EMPL = 149;


	/** Код на класификация "Статус на лице" */
	public static final int CODE_CLASSIF_LICE_STATUS = 201;

	/** Код на класификация "Причина за статус на лице" */
	public static final int CODE_CLASSIF_LICE_PRI = 202;

	/** Код на класификация "Учебни заведения" */
	public static final int CODE_CLASSIF_UNIVERSITETI = 203;

	/** Код на класификация "Видове стаж" */
	public static final int CODE_CLASSIF_STAJ_VID = 205;

	/** Код на класификация "Институции, в която се провежда стаж" */
	public static final int CODE_CLASSIF_INSTITUTION = 206;

	/** Код на класификация "Резултат от изпит" */
	public static final int CODE_CLASSIF_IZPIT_RESULT = 208;

	/** Код на класификация "Предоставени документи" */
	public static final int CODE_CLASSIF_DADENI_DOC = 209;

	/** Код на класификация "Статус на документ" */
	public static final int CODE_CLASSIF_DOC_STATUS = 210;

	/** Код на класификация "Причина за статус на документ" */
	public static final int CODE_CLASSIF_DOC_STATUS_PRI = 211;

	/** Код на класификация "Шаблони за попълване" */
	public static final int CODE_CLASSIF_SHABLONI = 212;

	/** Код на класификация "ВИД НА СЪОБЩЕНИЕ" */
	public static final int CODE_CLASSIF_MESSAGE_VID = 213;

	/** Код на класификация "СТАТУС НА СЪОБЩЕНИЕ" */
	public static final int CODE_CLASSIF_MESSAGE_STATUS = 214;

	/** Код на класификация "Причини за нередност по заявление за стаж" */
	public static final int CODE_CLASSIF_MESSAGE_SHABLONI_STAJ = 216;

	/** Код на класификация "Причини за нередности по заявление за изпит" */
	public static final int CODE_CLASSIF_MESSAGE_SHABLONI_IZPIT = 215;

	// Значения от системни класификации

	// Системна класификация 2 - Информационни обекти (за журналиране)
	/** Документ */
	public static final int	CODE_ZNACHENIE_JOURNAL_DOC					= 51;
	/** Участник в процеса */
	public static final int	CODE_ZNACHENIE_JOURNAL_REFERENT				= 52;
	/** Лице */
	public static final int	CODE_ZNACHENIE_JOURNAL_LICE					= 101;
	/** Провеждане на стаж */
	public static final int	CODE_ZNACHENIE_JOURNAL_STAJ					= 102;
	/** Провеждане на изпит */
	public static final int CODE_ZNACHENIE_JOURNAL_IZPIT				= 103;
	/** Резултат от изпит */
	public static final int	CODE_ZNACHENIE_JOURNAL_IZPIT_RESULT			= 105;
	/** Връзка Лице - Документ */
	public static final int	CODE_ZNACHENIE_JOURNAL_LICE_DOC				= 106;
	/** Логика за попълване на шаблон */
	public static final int	CODE_ZNACHENIE_JOURNAL_SHABLON_LOGIC		= 108;
	/** Съобщение /материал */
	public static final int	CODE_ZNACHENIE_JOURNAL_MESSAGE				= 109;


	/** Код на значение "Деловодител" класификация "Бизнес роля" 4 */
	public static final int CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL = 1;

	/** Код на значение  "Административна структура"  класификация "Меню" 7*/
	public static final int	CODE_ZNACHENIE_MENU_ADM_STRUCT = 32;

	/** Код на значение "Министър"  класификация "Длъжности" 25 */
	public static final int CODE_ZNACHENIE_DLAJN_MINISTAR = 1;
	/** Код на значение "Заместник-министър"  класификация "Длъжности" 25 */
	public static final int CODE_ZNACHENIE_DLAJN_ZAM_MINISTAR = 2;
	/** Код на значение "Директор на дирекция 'ВСВ'"  класификация "Длъжности" 25 */
	public static final int CODE_ZNACHENIE_DLAJN_DIREKTOR_VSV = 3;
	/** Код на значение "Старши експерт в дирекция 'ВСВ'"  класификация "Длъжности" 25 */
	public static final int CODE_ZNACHENIE_DLAJN_STAR_EXPERT = 4;

	/** Код на значение "звено" класификация "Тип участник" 101 */
	public static final int	CODE_ZNACHENIE_REF_TYPE_ZVENO	= 1;
	/** Код на значение "служител" класификация "Тип участник" 101 */
	public static final int	CODE_ZNACHENIE_REF_TYPE_EMPL	= 2;
	/** Код на значение "организация кореспондент" класификация "Тип участник" 101 */
	public static final int	CODE_ZNACHENIE_REF_TYPE_NFL		= 3;
	/** Код на значение "лице кореспондент" класификация "Тип участник" 101 */
	public static final int	CODE_ZNACHENIE_REF_TYPE_FZL		= 4;
	/** Код на значение "мигриран" класификация "Тип участник" 101 */
	public static final int	CODE_ZNACHENIE_REF_TYPE_MIG		= 5;

	/** Код на значение "България" класификация "Държави" 22 */
	public static final int	CODE_ZNACHENIE_BG= 37;

	/** Код на значение "основен документ" класификация "Предназначение на файл" 115 */
	public static final int	CODE_ZNACHENIE_FILE_PURPOSE_MAIN_DOC = 1;
	/** Код на значение "приложение" класификация "Предназначение на файл" 115 */
	public static final int	CODE_ZNACHENIE_FILE_PURPOSE_APPLICATION	= 2;
	/** Код на значение "помощен файл" класификация "Предназначение на файл" 115 */
	public static final int	CODE_ZNACHENIE_FILE_PURPOSE_HELP_FILE = 3;
	/** Код на значение "помощен файл" класификация "Предназначение на файл" 115 */
	public static final int	CODE_ZNACHENIE_FILE_PURPOSE_UD_ORIGINAL = 4;
	/** Код на значение "помощен файл" класификация "Предназначение на файл" 115 */
	public static final int	CODE_ZNACHENIE_FILE_PURPOSE_UD_DUPLICATE = 5;

	/** Код на значение "автор" класификация "Роля на референт в документ" 118 */
	public static final int	CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR	= 1;
	/** Код на значение "съгласувал" класификация "Роля на референт в документ" 118 */
	public static final int	CODE_ZNACHENIE_DOC_REF_ROLE_AGREED	= 2;
	/** Код на значение "подписал" класификация "Роля на референт в документ" 118 */
	public static final int	CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED	= 3;

	/** Код на значение "адрес за кореспонденция" класификация "Вид адрес" 140 */
	public static final int CODE_ZNACHENIE_ADDR_TYPE_CORRESP = 1;

	/** Код на значение "Заявление за стаж" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_ZAIAV_STAJ = 1;
	/** Код на значение "Заявление за допълнителен стаж" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_ZAIAV_DOP_STAJ = 2;
	/** Код на значение "Заповед за стаж" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_ZAP_STAJ = 3;
	/** Код на значение "Заповед за допълнителен стаж" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_ZAP_DOP_STAJ = 4;
	/** Код на значение "Заявление за изпит" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_ZAIAV_IZPIT = 5;
	/** Код на значение "Заповед за провеждане на изпит" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT = 6;
	/** Код на значение "Протокол от изпит – тест" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_PROT_TEST = 7;
	/** Код на значение "Протокол от изпит" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_PROT = 8;
	/** Код на значение "Удостоверение за юридическа правоспособност" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_UDOST_PRAVO = 9;
	/** Код на значение "Заявление за дубликат на удостоверение за юридическа правоспособност" класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_ZAIAV_DUBL_UDOST = 10;
	/** Код на значение "Заповед за провеждане на изпит - законод. до 2019г." класификация 104 - Вид документ */
	public static final int CODE_ZNACHENIE_DOC_VID_ZAP_IZPIT_DO_2019 = 11;

	/** Код на значение "Кандидат" класификация 201 - Статус на лице */
	public static final int CODE_ZNACHENIE_LICE_STATUS_CANDIDATE = 1;
	/** Код на значение "Одобрен за стаж" класификация 201 - Статус на лице */
	public static final int CODE_ZNACHENIE_LICE_STATUS_STAJ_APPROVED = 2;
	/** Код на значение "Одобрен за тест" класификация 201 - Статус на лице */
	public static final int CODE_ZNACHENIE_LICE_STATUS_TEST_APPROVED = 3;
	/** Код на значение "Одобрен за казус" класификация 201 - Статус на лице */
	public static final int CODE_ZNACHENIE_LICE_STATUS_CASE_APPROVED = 4;
	/** Код на значение "Издържал" класификация 201 - Статус на лице */
	public static final int CODE_ZNACHENIE_LICE_STATUS_EXAM_PASSED = 5;
	/** Код на значение "Неиздъжал" класификация 201 - Статус на лице */
	public static final int CODE_ZNACHENIE_LICE_STATUS_EXAM_FAILED = 6;
	/** Код на значение "Правоспособен" класификация 201 - Статус на лице */
	public static final int CODE_ZNACHENIE_LICE_STATUS_PRAVO = 7;
	/** Код на значение "Лишен от правоспособност" класификация 201 - Статус на лице */
	public static final int CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN = 8;

	/** Код на значение "Първоначален" класификация 205 - Видове стаж */
	public static final int CODE_ZNACHENIE_STAJ_VID_INITIAL = 1;
	/** Код на значение "Допълнителен" класификация 205 - Видове стаж */
	public static final int CODE_ZNACHENIE_STAJ_VID_ADDITIONAL = 2;

	/** Код на значение "Издържал" класификация 208 - Резултат от изпит */
	public static final int CODE_ZNACHENIE_IZPIT_RESULT_PASSED = 1;
	/** Код на значение "Неиздържал" класификация 208 - Резултат от изпит */
	public static final int CODE_ZNACHENIE_IZPIT_RESULT_FAILED = 2;
	/** Код на значение "Неявил се" класификация 208 - Резултат от изпит */
	public static final int CODE_ZNACHENIE_IZPIT_RESULT_MISSED = 3;

	/** Код на значение "Заповед за изпит" от класификация 212 - Шаблони за попълване */
	public static final int CODE_ZNACHENIE_SHABLON_ZAPOVED_IZPIT = 1;
	/** Код на значение "Удостоверение за юридическа правоспособност" от класификация 212 - Шаблони за попълване */
	public static final int CODE_ZNACHENIE_SHABLON_UDOST_PRAVOSP = 2;
	/** Код на значение "Заповед за изпит по стария ред" от класификация 212 - Шаблони за попълване */
	public static final int CODE_ZNACHENIE_SHABLON_ZAPOVED_IZPIT_STAR = 3;
	/** Код на значение "Заповед за стаж" от класификация 212 - Шаблони за попълване */
	public static final int CODE_ZNACHENIE_SHABLON_ZAPOVED_STAJ = 4;
	/** Код на значение "Заповед за допълнителен стаж" от класификация 212 - Шаблони за попълване */
	public static final int CODE_ZNACHENIE_SHABLON_ZAPOVED_DOP_STAJ = 5;
	/** Код на значение "Извлечение от заповед-допълнителен стаж" от класификация 212 - Шаблони за попълване */
	public static final int CODE_ZNACHENIE_SHABLON_IZVLECH_ZAPOVED_DOP_STAJ = 6;
	/** Код на значение "Извлечение от заповед стаж - без наставник" от класификация 212 - Шаблони за попълване */
	public static final int CODE_ZNACHENIE_SHABLON_ZAPOVED_STAJ_BEZ_NAST = 7;
	/** Код на значение "Извлечение от заповед стаж - с наставник" от класификация 212 - Шаблони за попълване */
	public static final int CODE_ZNACHENIE_SHABLON_ZAPOVED_STAJ_NAST = 8;
	/** Код на значение "Приложение към Заповед за изпит" от класификация 212 - Шаблони за попълване */
	public static final int CODE_ZNACHENIE_SHABLON_PRILOJ_ZAPOVED_IZPIT = 9;

	/** Код на значение "Уведомление" класификация 213 - Вид на съобщение */
	public static final int  CODE_ZNACHENIE_MESSAGE_VID_UVED =4;

	/** Код на значение "Активно" класификация 214 - Статус на съобщение */
	public static final int CODE_ZNACHENIE_MESSAGE_ACTIVE = 2;

	/** */
	private UriregConstants() {
		super();
	}
}
