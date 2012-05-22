package com.game.fickapets;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class XMLUtils {
	
	public static List<Element> getElementsByTagName(Node e, String tagName) {
		List<Element> list = new ArrayList<Element>();
		Node child = e.getFirstChild();
		while (child != null) {
			if (child instanceof Element && child.getNodeName().equals(tagName)) {
				list.add((Element)child);
			}
			child = child.getNextSibling();
		}
		return list;
	}
	
	public static Element getElementByTagName(Node e, String tagName) {
		Node child = e.getFirstChild();
		while (child != null) {
			if (child instanceof Element && child.getNodeName().equals(tagName)) {
				return (Element) child;
			}
			child = child.getNextSibling();
		}
		return null;
	}
	
	public static String getElementText (Element e) {
		if (e != null && e.getChildNodes().getLength() == 1) {
			Text elementText = (Text) e.getFirstChild();
			return elementText.getNodeValue();
		} else {
			return "NULL";
		}
	}
	
	public static Element getDocumentElement(InputStream xmlStream) {
		/* the parseable document */
		Document doc;
		/* Initialize parser */
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance ();
			factory.setIgnoringElementContentWhitespace (true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(xmlStream);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		
		return doc.getDocumentElement();
	}
	
	public static String getChildElementTextByTagName(Element e, String tagName) {
		Element elem = getElementByTagName(e, tagName);
		return getElementText(elem);
	}
}
