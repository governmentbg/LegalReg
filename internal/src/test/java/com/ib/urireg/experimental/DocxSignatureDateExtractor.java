package com.ib.urireg.experimental;

import com.ib.system.utils.FileUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.security.auth.x500.X500Principal;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public class DocxSignatureDateExtractor {
    public static void main(String[] args) {

    	 try {


//	    	byte[] bytes = FileUtils.getBytesFromFile(new File("D:\\_tests\\SignVassil.docx"));
    		 byte[] bytes = FileUtils.getBytesFromFile(new File("D:\\_tests\\SignValery.docx"));

	    	InputStream bis = new ByteArrayInputStream(bytes);
	    	OPCPackage opcPackage = OPCPackage.open(bis);

            // Find the digital signature parts
            List<PackagePart> signatureParts = opcPackage.getPartsByContentType("application/vnd.openxmlformats-package.digital-signature-xmlsignature+xml");

            for (PackagePart part : signatureParts) {
                System.out.println("Signature part: " + part.getPartName());

                try (InputStream is = part.getInputStream()) {
                    // Parse the signature.xml content
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true); // Enable namespace awareness
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(is);

                    printNode((Node)doc, "");


                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void printNode(Node node, String offset) throws CertificateException {


    	if (node.getNodeName().equals("#text")) {
    		return;
    	}

    	 String value = getSimpleNodeValue(node);
    	 System.out.println(offset + node.getNodeName()  + ":= "+ value);

    	 if (node.getNodeName() != null && node.getNodeName().contains("X509Certificate")) {
	    	 value = "-----BEGIN CERTIFICATE-----\n"+value+"\n-----END CERTIFICATE-----";
    		 CertificateFactory cf = CertificateFactory.getInstance("X.509");
    		 X509Certificate  cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(value.getBytes()));
	    	 X500Principal principal = cert.getSubjectX500Principal();

	    	String[] rows = principal.getName().split(",");
	    	for (String row : rows) {
	    		if (row.startsWith("CN=")) {
	    			System.out.println("************************   " + row.substring(3));
	    		}
	    	}


    	 }


    	if (node != null ) {
    		NodeList childNodes = node.getChildNodes();
    		if (childNodes != null) {
	            for (int i = 0; i < childNodes.getLength(); i++) {
	            	printNode(childNodes.item(i), offset + "\t");
	            }
    		}
    	}

    }


    private static String getSimpleNodeValue(Node node) {
		if (node.getNodeValue() != null) {
			return node.getNodeValue();
		}else {
			if (node.getChildNodes() != null) {
				for (int i = 0; i < node.getChildNodes().getLength(); i++) {
					Node child = node.getChildNodes().item(i);
					if ( child.getNodeName() != null && child.getNodeName().equalsIgnoreCase( "#text")) {
						if (child.getNodeValue() != null && ! child.getNodeValue().trim().isEmpty())
						return child.getNodeValue();
					}
				}

				for (int i = 0; i < node.getChildNodes().getLength(); i++) {
					Node child = node.getChildNodes().item(i);
					if ( child.getNodeName() != null && child.getNodeName().equalsIgnoreCase( "value")) {
						return getSimpleNodeValue(child);
					}
				}
			}
		}

		return null;
	}
}
