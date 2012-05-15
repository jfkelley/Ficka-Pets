package com.game.fickapets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Context;

public class ItemManager {
	private static Map<String, Item> items = new LinkedHashMap<String, Item>();
	
	private static void readItems(Context context) {
		Document doc;		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance ();
			factory.setIgnoringElementContentWhitespace (true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(context.getResources().openRawResource(R.raw.items));
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		
		Element itemsElement = doc.getDocumentElement();
		for (Element itemElement : XMLUtils.getElementsByTagName(itemsElement, "Item")) {
			String id = XMLUtils.getElementTextByTagName(itemElement, "id");
			String name = XMLUtils.getElementTextByTagName(itemElement, "name");
			String imageLocation = XMLUtils.getElementTextByTagName(itemElement, "image");
			int price = Integer.valueOf(XMLUtils.getElementTextByTagName(itemElement, "price"));
			Item item;
			
			String type = itemElement.getAttributes().getNamedItem("type").getNodeValue();
			if (type.equals("food")) {
				int health = Integer.valueOf(XMLUtils.getElementTextByTagName(itemElement, "health"));
				item = new Food(id, name, imageLocation, price, health);
			} else {
				System.err.println("unknown item type in items.xml");
				item = null;
			}
			
			if (item != null) items.put(id, item);
		}
		
	}
	
	public static List<Item> allItems(Context context) {
		if (items.isEmpty()) readItems(context);
		List<Item> list = new ArrayList<Item>();
		for (String id : items.keySet()) {
			list.add(items.get(id));
		}
		return list;
	}
	
	public static Item getItem(Context context, String id) {
		if (items.isEmpty()) readItems(context);
		return items.get(id);
	}
}
