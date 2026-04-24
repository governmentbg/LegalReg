package com.ib.urireg.udostDocs;

import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 * В някои УД има цели пасажи с букмаркове, които понякога да трябва да се скриват. Пример:
 * <br/><i>"..., животни [2] на брой,  фуражи [41] на брой, влп [12] на брой... "</i>
 * <br/>Ако целият израз <i>"... фуражи [41] на брой,"</i> трябва да изчезне, програмата трябва да знае да не се опитва
 * да попълни вътрешния букмарк с числото 41, защото ще бъде изтрит от документа и ще даде грешка.
 * За тази цел в документа се слага един букмарк, който загражда целия текст, който може да бъде изтрит и в
 * екрана се задава да бъде от тип container. Избират се с чекбоксове букмарковете, които са вътре в него,
 * и се избира метод от този клас, който да определи дали пасажът да се запази, дали да се изтрие или да се
 * сложи някакъв прост текст на негово място.
 * <br/><b>Методите в този клас трябва да връщат един от тези резултати от вид стринг:</b>
 * <ul>
 *     <li>null - така абзацът няма да се промени и вътрешните букмаркове ще се попълват нормално</li>
 *     <li>{@link UdostDocumentCreator#HIDE_BOOKMARK_FLAG} - цялото съдържание на букмарка ще се изтрие</li>
 *     <li>String str (различен от {@link UdostDocumentCreator#HIDE_BOOKMARK_FLAG}) - цялото съдържание на букмарка
 *     ще се изтрие и ще се подмени с върнатия стринг str</li>
 * </ul>
 *
 * @author n.kanev
 */
public class ContainerMethods {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerMethods.class);
    private static final String DB_ERROR_MSG = "Грешка при работа с базата";

    private final UserData userData;
    private final SystemData systemData;
    private final Date date;
    private final Helpers helpers;

    public ContainerMethods(UserData userData, SystemData systemData, Map<String, Object> additionalData) {
        this.userData = userData;
        this.systemData = systemData;
        this.date = new Date();
        this.helpers = new Helpers(userData, systemData);
    }

}
