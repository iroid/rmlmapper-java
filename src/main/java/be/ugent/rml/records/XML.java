package be.ugent.rml.records;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XML extends IteratorFormat {

    @Override
    List<Record> _get(String path, String iterator) throws IOException {
        List<Record> records = new ArrayList<>();

        try {
            FileInputStream fileIS = new FileInputStream(new File(path));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(fileIS);

            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList result = (NodeList) xPath.compile(iterator).evaluate(xmlDocument, XPathConstants.NODESET);

            for (int i = 0; i < result.getLength(); i ++) {
                records.add(new XMLRecord(result.item(i)));
            }
        } catch (XPathExpressionException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        }

        return records;
    }
}