package com.game.fickapets;

import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import android.content.Context;
import android.content.SharedPreferences;

public class PersistenceHandler {
	
	private static final String PET_FILE = "petAttributesFile";
	
	/* key values for SharedPreferences */
	private static final String HEALTH_KEY = "health";
	private static final String HUNGER_KEY = "hunger";
	private static final String AWAKE_KEY = "awake";
	private static final String STRENGTH_KEY = "strength";
	private static final String TIREDNESS_KEY = "tirednes";
	private static final String LASTUPDATE_KEY = "lastUpdate";
	private static final String DEFAULTSET_KEY = "defaultsSet";

	private static Attributes getAttributesFromStoredState (SharedPreferences petState) {
		Attributes atts = new Attributes ();
		atts.health = petState.getFloat(HEALTH_KEY, 0);
		atts.hunger = petState.getFloat(HUNGER_KEY, 0);
		atts.isAwake = petState.getBoolean(AWAKE_KEY, true);
		atts.strength = petState.getFloat(STRENGTH_KEY, 0);
		atts.tiredness = petState.getFloat(TIREDNESS_KEY, 0);
		atts.lastUpdate = petState.getLong(LASTUPDATE_KEY, 0);
		return atts;
	}
	
	private static Element getElementByTagName(Node e, String tagName) {
		Node child = e.getFirstChild();
		while (child != null) {
			if (child instanceof Element && child.getNodeName().equals(tagName)) {
				return (Element) child;
			}
			child = child.getNextSibling();
		}
		return null;
	}
	
	private static String getElementText (Element e) {
		if (e != null && e.getChildNodes().getLength() == 1) {
			Text elementText = (Text) e.getFirstChild();
			return elementText.getNodeValue();
		} else {
			return "NULL";
		}
	}
	
	private static String getElementTextByTagName(Element e, String tagName) {
		Element elem = getElementByTagName(e, tagName);
		return getElementText(elem);
	}
	
	
	/* pull all defaults from an xml doc in res/raw */
	private static Attributes getAttributesFromDefaults (Context context) {
		Attributes atts = new Attributes ();
		/* the parseable document */
		Document doc;		
		/* Initialize parser */
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance ();
			factory.setIgnoringElementContentWhitespace (true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(context.getResources().openRawResource(R.raw.pet_defaults));
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	
		Element pet = doc.getDocumentElement();
		atts.health = Double.valueOf(getElementTextByTagName(pet, "health"));
		atts.hunger = Double.valueOf(getElementTextByTagName(pet, "hunger"));
		atts.strength = Double.valueOf(getElementTextByTagName(pet, "strength"));
		if (Integer.valueOf(getElementTextByTagName(pet, "awake")) != 0) {
			atts.isAwake = true;
		} else {
			atts.isAwake = false;
		}
		/* sleepTime tells us what time we'd like the pet to need to sleep when it's first initialized */
		atts.tiredness = Tiredness.getInitialTiredness(Double.valueOf(getElementTextByTagName(pet, "sleepTime")));
		atts.lastUpdate = Calendar.getInstance(TimeZone.getDefault ()).getTimeInMillis ();
		return atts;
	}
	/* set the pet's values.  If it's the first time running, loads values from the default xml file, 
	 * otherwise they're loaded from SharedPreferences
	 */
	public static Pet buildPet (Context context) {
		Attributes atts;
		Pet pet;
		SharedPreferences petState = context.getSharedPreferences(PET_FILE, 0);

		if (petState.getBoolean(DEFAULTSET_KEY, false)) {
			atts = getAttributesFromStoredState (petState);
			
		} else {
			atts = getAttributesFromDefaults (context);
		}
		pet = new Pet (atts);
		return pet;
	}
	/* saves everything in SharedPreferences which is android's persistent key value store */
	public static void saveState (Context context, Pet pet) {
		Attributes atts = pet.getAttributes();
		SharedPreferences petState = context.getSharedPreferences(PET_FILE, 0);
		SharedPreferences.Editor editor = petState.edit();
		editor.putFloat(HEALTH_KEY, (float) atts.health);
		editor.putFloat(HUNGER_KEY, (float) atts.hunger);
		editor.putBoolean(AWAKE_KEY, atts.isAwake);
		editor.putFloat(STRENGTH_KEY, (float) atts.strength);
		editor.putFloat(TIREDNESS_KEY, (float) atts.tiredness);
		editor.putLong(LASTUPDATE_KEY, atts.lastUpdate);
		editor.putBoolean(DEFAULTSET_KEY, true);
		editor.commit();
	}
	
	public static Pet reset (Context context) {
		SharedPreferences petState = context.getSharedPreferences (PET_FILE, 0);
		SharedPreferences.Editor editor = petState.edit ();
		editor.putBoolean (DEFAULTSET_KEY, false);
		editor.commit ();

		return buildPet (context);
		
	}
}
