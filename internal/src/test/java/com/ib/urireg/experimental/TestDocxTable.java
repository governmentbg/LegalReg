package com.ib.urireg.experimental;

import com.aspose.words.Bookmark;
import com.aspose.words.Cell;
import com.aspose.words.CompositeNode;
import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.License;
import com.aspose.words.Paragraph;
import com.aspose.words.Row;
import com.aspose.words.Table;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author n.kanev
 */
public class TestDocxTable {

    private static final String INPUT_FILE = "Приложение към Заповед.docx";
    private static final String OUTPUT_PATH = "N:\\temp\\попълнена таблица.docx";
    private static List<List<String>> testData;

    @BeforeClass
    public static void setLicense() throws Exception {
        String nameLicense="Aspose.Words.lic";
        InputStream inp = TestDocxTable.class.getClassLoader().getResourceAsStream(nameLicense);
        License license = new License();
        license.setLicense(inp);

        testData = new ArrayList<>();
        testData.add(List.of("Айя", "Градинарова", "Заявление1"));
        testData.add(List.of("Кая", "Величкова", "Заявление2"));
        testData.add(List.of("Иваела", "Лещарска", "Заявление3"));
    }

    @Test
    public void writeTable() throws Exception {
        // отваряне на файла
        Document doc;
        try(InputStream fis = Thread.currentThread().getContextClassLoader().getResourceAsStream(INPUT_FILE);) {
            assertNotNull(fis);
            doc = new Document(fis);
        }

        Bookmark bookmark = doc.getRange().getBookmarks().get("table");

        // намиране на елемента таблица
        CompositeNode node = bookmark.getBookmarkStart().getParentNode();
        Table targetTable = null;
        while(node != null) {
            if(node instanceof Table) {
                targetTable = (Table) node;
                break;
            }
            node = node.getParentNode();
        }

        assertNotNull(targetTable);

        // изтриват се всички празни редове след антетката
        int initialTableRows = targetTable.getRows().getCount();
        for(int i = 1; i < initialTableRows; i++) {
            Row row = targetTable.getRows().get(1);
            targetTable.getRows().remove(row);
        }

        // попълване на редовете
        DocumentBuilder builder = new DocumentBuilder(doc);
        for(int i = 0; i < testData.size(); i++) {
            List<String> dataRow = testData.get(i);

            Row row = new Row(doc);
            targetTable.appendChild(row);

            // номер
            Cell cell = new Cell(doc);
            row.appendChild(cell);
            cell.appendChild(new Paragraph(doc));
            builder.moveTo(cell.getFirstParagraph());
            builder.write(String.valueOf(i + 1));

            // останали данни
            for(String dataCell : dataRow) {
                cell = new Cell(doc);
                row.appendChild(cell);
                cell.appendChild(new Paragraph(doc));
                builder.moveTo(cell.getFirstParagraph());
                builder.write(dataCell);
            }
        }

        // запис на файла
        doc.save(OUTPUT_PATH);
    }
}
