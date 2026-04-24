package com.ib.urireg.udostDocs;

import com.aspose.words.ConvertUtil;
import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.Font;
import com.aspose.words.HtmlSaveOptions;
import com.aspose.words.License;
import com.aspose.words.ParagraphAlignment;
import com.aspose.words.ParagraphFormat;
import com.aspose.words.SaveFormat;
import com.aspose.words.Shape;
import com.aspose.words.SignatureLineOptions;
import com.ib.urireg.db.dto.Referent;
import com.ib.urireg.db.dto.ShablonLogic;
import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UriregConstants;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ib.system.utils.ValidationUtils.isNotBlank;

/**
 * @author n.kanev
 */
public class AsposeUtils {

    public static List<String> getBookmarkLabelsFromWordFile(Document wordFile) {
        final List<String> labels = new ArrayList<>();
        wordFile.getRange()
                .getBookmarks()
                .forEach(b -> {
                    if(!b.getName().startsWith("_") && !b.getName().equals(UdostDocumentCreator.DUBLIKAT_BM_LABEL)) {
                        labels.add(b.getName());
                    }
                });
        return labels;
    }

    public static Document getWordFileFromBytes(byte[] bytes) throws Exception {
        // TODO може би трябва да се сетва на друго място само веднъж
        License license = new License();
        String nameLicense="Aspose.Words.lic";
        InputStream inp = AsposeUtils.class.getClassLoader().getResourceAsStream(nameLicense);
        license.setLicense(inp);

        InputStream input = new ByteArrayInputStream(bytes);
        return new Document(input);
    }

    public static String getDocumentAsHtml(Document document) throws Exception {
        String html = "";

        try(ByteArrayOutputStream htmlStream = new ByteArrayOutputStream()) {
            HtmlSaveOptions saveOptions = new HtmlSaveOptions(SaveFormat.HTML);
            saveOptions.setPrettyFormat(true);
            saveOptions.setExportOriginalUrlForLinkedImages(true);
            saveOptions.setExportRoundtripInformation(false);
            saveOptions.setExportFontResources(false);
            saveOptions.setExportImagesAsBase64(true);

            document.save(htmlStream, saveOptions);
            html = htmlStream.toString();
        }

        return html;
    }

    public static void resetParagraphFontFormatting(DocumentBuilder builder) {
        Font font = builder.getFont();
        ParagraphFormat format = builder.getParagraphFormat();
        format.setAlignment(ParagraphAlignment.JUSTIFY);
        format.setSpaceBefore(0);
        format.setSpaceAfter(0);
        font.setColor(Color.BLACK);
        font.setHighlightColor(new  Color(0, 0, 0, 0));
    }

    public static void buildSignField(DocumentBuilder builder, SystemData systemData, Referent referent, double width, int lang) throws Exception {
        String position = systemData.decodeItem(UriregConstants.CODE_CLASSIF_POSITION, referent.getEmplPosition(), lang, new Date());

        SignatureLineOptions signatureLineOptions = new SignatureLineOptions();
        signatureLineOptions.setSigner(isNotBlank(referent.getRefName()) ?  referent.getRefName().toUpperCase() : "");
        signatureLineOptions.setSignerTitle(isNotBlank(position) ? position.toUpperCase() : "");
        Shape signatureLine = builder.insertSignatureLine(signatureLineOptions);
        signatureLine.setWidth(ConvertUtil.millimeterToPoint(width));
        signatureLine.setAspectRatioLocked(true);
    }

    public static void buildSignField(DocumentBuilder builder, double width, String name, String position) throws Exception {
        SignatureLineOptions signatureLineOptions = new SignatureLineOptions();
        signatureLineOptions.setSigner(isNotBlank(name) ? name.toUpperCase() : "");
        signatureLineOptions.setSignerTitle(isNotBlank(position) ? position.toUpperCase() : "");
        Shape signatureLine = builder.insertSignatureLine(signatureLineOptions);
        signatureLine.setWidth(ConvertUtil.millimeterToPoint(width));
        signatureLine.setAspectRatioLocked(true);
    }
}
