package com.game.fickapets;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;


public class NetworkErrorDialog {
	public static final int CREATE_GAME_FAIL = 400;
	public static final int SERVER_FAIL = 401;
	public static final int FACEBOOK_SERVER_FAIL = 402;
	public static final int FACEBOOK_NETWORK_ERROR = 403;
	
	public static Dialog createDialog(final Activity activity, int id) {
		switch(id) {
		case CREATE_GAME_FAIL:
    		AlertDialog createGameFail = new AlertDialog.Builder(activity)
    			.setMessage("Failed to create new game. If network is connected, our server may be down")
    			.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    					activity.finish();
    				}
    			}).create();
    		return createGameFail;
    	case SERVER_FAIL:
    		AlertDialog serverFail = new AlertDialog.Builder(activity)
    			.setMessage("Cannot connect to server.  If you're connected to the Internet, our server may be down\nPlease try again")
    			.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    					activity.finish();
    				}
    			}).create();
    		return serverFail;
    	case FACEBOOK_SERVER_FAIL:
    		AlertDialog facebookServerFail = new AlertDialog.Builder(activity)
    			.setMessage("There was an error on Facebook's servers.\nPlease try again")
    			.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    					activity.finish();
    				}
    			}).create();
    		return facebookServerFail;
    	case FACEBOOK_NETWORK_ERROR:
    		AlertDialog facebookNetworkError = new AlertDialog.Builder(activity)
    			.setMessage("Could not connect to Facebook. Check your network connection and try again")
    			.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.finish();
					}
				}).create();
    		return facebookNetworkError;
    	default:
			return null;
		}
		
	}
}
