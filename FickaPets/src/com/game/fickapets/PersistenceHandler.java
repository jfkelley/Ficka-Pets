package com.game.fickapets;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
	private static final String ACCESS_TOKEN_KEY = "facebookAccessToken";
	private static final String ACCESS_EXPIRATION_KEY = "facebookAccessExpires";
	private static final String USER_FILE = "userAttributesFile";
	
	private static final String COINS_KEY = "coins";
	private static final String INVENTORY_KEY = "inventory";

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
		atts.health = Double.valueOf(XMLUtils.getElementTextByTagName(pet, "health"));
		atts.hunger = Double.valueOf(XMLUtils.getElementTextByTagName(pet, "hunger"));
		atts.strength = Double.valueOf(XMLUtils.getElementTextByTagName(pet, "strength"));
		if (Integer.valueOf(XMLUtils.getElementTextByTagName(pet, "awake")) != 0) {
			atts.isAwake = true;
		} else {
			atts.isAwake = false;
		}
		/* sleepTime tells us what time we'd like the pet to need to sleep when it's first initialized */
		atts.tiredness = Tiredness.getInitialTiredness(Double.valueOf(XMLUtils.getElementTextByTagName(pet, "sleepTime")));
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
	
	public static Pet reset (Context context) {
		SharedPreferences petState = context.getSharedPreferences (PET_FILE, 0);
		SharedPreferences.Editor editor = petState.edit ();
		editor.putBoolean (DEFAULTSET_KEY, false);
		editor.commit ();

		return buildPet (context);
		
	}
	
	public static void saveState(Context context, Pet pet) {
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
	
	public static void saveState(Context context, User user) {
		SharedPreferences userState = context.getSharedPreferences(USER_FILE, 0);
		SharedPreferences.Editor editor = userState.edit();
		editor.putInt(COINS_KEY, user.getCoins());
		editor.putString(INVENTORY_KEY, encodeInventory(user.getInventory()));
		editor.commit();
	}
	
	/* saves everything in SharedPreferences which is android's persistent key value store */
	public static void saveState (Context context, Pet pet, User user) {
		saveState(context, pet);
		saveState(context, user);
	}
	
	private static String encodeInventory(List<Item> inventory) {
		System.out.println(inventory);
		StringBuilder sb = new StringBuilder();
		for (Item item : inventory) {
			sb.append(item.getId());
			sb.append(",");
		}
		if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
		System.out.println(sb.toString());
		return sb.toString();
	}
	
	private static List<Item> decodeInventory(Context context, String text) {
		String[] ids = text.split(",");
		List<Item> items = new ArrayList<Item>();
		for (String id : ids) {
			/* don't want to add anything for empty strings */
			if (id.compareTo("") != 0) {
				items.add(ItemManager.getItem(context, id));
			}
		}
		return items;
	}
	
	public static User buildUser(Context context) {
		SharedPreferences userState = context.getSharedPreferences(USER_FILE, 0);

		if (userState.getInt(COINS_KEY, -1) == -1) {
			return new User();
		} else {
			int coins = userState.getInt(COINS_KEY, 0);
			List<Item> inventory = decodeInventory(context, userState.getString(INVENTORY_KEY, ""));
			return new User(coins, inventory);
		}
	}
	
	public static String facebookAccessToken (Context context) {
		SharedPreferences facebookPrefs = context.getSharedPreferences(USER_FILE, 0);
		return facebookPrefs.getString(ACCESS_TOKEN_KEY, null);
	}
	public static long facebookTokenExpiration (Context context) {
		SharedPreferences facebookPrefs = context.getSharedPreferences(USER_FILE, 0);
		return facebookPrefs.getLong(ACCESS_EXPIRATION_KEY, 0);
	}
	
	public static void saveFacebookAccess(Context context, String accessToken, long accessExpires) {
		SharedPreferences facebookPrefs = context.getSharedPreferences(USER_FILE, 0);
		SharedPreferences.Editor editor = facebookPrefs.edit();
		editor.putString(ACCESS_TOKEN_KEY, accessToken);
		editor.putLong(ACCESS_EXPIRATION_KEY, accessExpires);
		editor.commit();
	}
	
}
