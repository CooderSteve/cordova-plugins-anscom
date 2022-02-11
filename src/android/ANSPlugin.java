package com.ans.cordova.anscom;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;

import org.apache.cordova.Config;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import android.util.Log;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import static android.content.Context.MODE_PRIVATE;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.MediaPlayer;
import android.net.Uri;

public class ANSPlugin extends CordovaPlugin {

	public static final String ACTION_SET_AUDIO_MODE = "setAudioMode";
	protected static ANSPlugin instance = null;
	protected static final String LOG_TAG = "ANSCOM";
	private static MediaPlayer mplayer;
	private static Context context;
	private static Uri notification;
	private static AudioManager audioManager;
	private static int orignalVolumeLevel;
	private static int maxVolumeLevel;
	private final static String SERVER_KEY = "8c3e16df6725bcafd34d8a844a478ef3f5d5badc65f9175669fa862f1cabbd5d";

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		context = cordova.getActivity();
		notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

	// @Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Log.d(LOG_TAG, "ANS Comm Plugin");

		if (action.equals("init")) {
			init();
			callbackContext.success(SERVER_KEY);
			return true;
		} else if (action.equals("getPreference")) {
			Log.d(LOG_TAG, "Get SharedPreferences");
			return getPublicPreference(args, callbackContext);
		} else if (action.equals("setPreference")) {
			return setPublicPreference(args, callbackContext);
		} else if (action.equals("removePreference")) {
			return removePreference(args, callbackContext);
		} else if (action.equals(ACTION_SET_AUDIO_MODE)
				&& (args.getString(0).equals("get_volume") || args.getString(0).equals("ringstop"))) {
			Log.d(LOG_TAG, "Setting Audio Volume");
			return setAudioMode(args.getString(0), callbackContext);
		} else if (action.equals(ACTION_SET_AUDIO_MODE)) {
			Log.d(LOG_TAG, "Setting Audio Ring Tone");
			if (!setAudioMode(args.getString(0))) {
				callbackContext.error("Invalid audio mode:" + args.getString(0));
				return false;
			}
			return true;
		} else if (action.equals("set_volume")) {
			Log.d(LOG_TAG, "Set Volume");
			return setAudioVolume(args.getString(0), callbackContext);
		}
		callbackContext.error("Incorrect method call");
		return false;
	}

	private void init() {
		cordova.getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				cordova.getActivity().getWindow()
						.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
								| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
								| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
								| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

			}
		});
	}

	/**
	 * This method stores key/value passed by Cordova
	 * @param JSONArray arg
	 * @throws JSONException
	 */
	private boolean setPublicPreference(JSONArray args, CallbackContext callbackContext) throws JSONException {
		Context context = cordova.getActivity();

		SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(args.getString(0), args.getString(1));
		editor.commit();
		checkPreference(context, args.getString(0));
		return true;
	}

	/**
	 * This method fetches the stored key/value and return it to Cordova
	 * @param (Json) args passed by Cordova
	 * @throws JSONException
	 */
	private boolean getPublicPreference(JSONArray args, CallbackContext callbackContext) throws JSONException {
		SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);

		for (String key : settings.getAll().keySet()) {
			String p = settings.getString(key, "");
			if (p != null) {
				Log.d(LOG_TAG, "SharedPreferences in database: " + key + ":" + p);
			}
		}

		if (settings.contains(args.getString(0))) {
			String StoredValue = settings.getString(args.getString(0), "");
			Log.d(LOG_TAG, "SharedPreferences found key: " + args.getString(0) + ":" + StoredValue);
			callbackContext.success(StoredValue);
			return true;
		} else {
			Log.d(LOG_TAG, "Key doesn't exists: " + args.getString(0));
			callbackContext.error("No data " + args.getString(0));
			return false;
		}
	}

	/**
	 * This method check of a key value has been set
	 * @param JSONArray arg
	 * @param callbackContext
	 * @return
	 * @throws JSONException
	 */
	private boolean hasPreference(JSONArray args, CallbackContext callbackContext) throws JSONException {
		SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);

		if (settings.contains(args.getString(0))) {
			callbackContext.success("Key " + args.getString(0) + " exists");
			return true;
		} else {
			Log.d(LOG_TAG, "Key doesn't exists: " + args.getString(0));
			callbackContext.success("Key " + args.getString(0) + " does not exists");
			return false;
		}
	}

	/**
	 * This method removes a key/value set
	 * @param args
	 * @param callbackContext
	 * @return
	 * @throws JSONException
	 */
	private boolean removePreference(JSONArray args, CallbackContext callbackContext) throws JSONException {
		SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);

		if (settings.contains(args.getString(0))) {
			SharedPreferences.Editor editor = settings.edit();
			editor.remove(args.getString(0));
			editor.apply();
			callbackContext.success(args.getString(0) + " successfully removed");
			return true;
		} else {
			Log.d(LOG_TAG, "Key doesn't exists ");
			callbackContext.error("No data " + args.getString(0));
			return false;
		}
	}

	/**
	 * This method is used for debugging.  It will return the stored key/value and can be viewed in debugging tools
	 * @param context
	 * @param key
	 * @return
	 */
	private static boolean checkPreference(Context context, String key) {
		SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
		String StoredValue = settings.getString(key, "");
		Log.d(LOG_TAG, "Preference '" + key + "' Retreived Value:" + StoredValue);
		return true;
	}

	/**
	 * This method is used to manage the audio setting on a Android Device
	 * @param volume
	 * @param callbackContext
	 * @return
	 */
	private boolean setAudioVolume(String volume, CallbackContext callbackContext) {
		int maxVolumeLevel = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		boolean speaker = audioManager.isSpeakerphoneOn();
		Log.d(LOG_TAG, "Set Volume To:" + volume);
		int intVolume = Integer.parseInt(volume);
		Log.d(LOG_TAG, "Int Volume:" + intVolume);
		if (intVolume < 0) {
			intVolume = 0;
			Log.d(LOG_TAG, "Int less than 0");
		} else if (intVolume > maxVolumeLevel) {
			intVolume = maxVolumeLevel;
			Log.d(LOG_TAG, "Int more than " + maxVolumeLevel);
		}
		if (speaker) {
			Log.d(LOG_TAG, "turn speaker off");
			setAudioMode("normal");
		}
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, intVolume, 0);
		if (speaker) {
			Log.d(LOG_TAG, "Turn speaker back on");
			setAudioMode("speaker");
		}

		Log.d(LOG_TAG, "Set Volume To:" + intVolume);
		callbackContext.success("Volume set to " + intVolume);
		return true;
	}

	/**
	 * This method is used to select the audio mode for adjustsment used by Cordova
	 * @param mode
	 * @param callbackContext
	 * @return
	 */
	public boolean setAudioMode(String mode, CallbackContext callbackContext) {
		if (mode.equals("get_volume")) {
			Log.d(LOG_TAG, "Get Volume");
			int volume_level = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			// callbackContext.success(volume_level);
			Log.d(LOG_TAG, "Current stream volume level:" + volume_level);

			int max_volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			Log.d(LOG_TAG, "Max volume level:" + max_volume);

			volume_level = audioManager.getStreamVolume(AudioManager.MODE_RINGTONE);
			callbackContext.success(volume_level);
			Log.d(LOG_TAG, "Current ring volume level:" + volume_level);
			return true;
		} else if (mode.equals("ringstop")) {
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, orignalVolumeLevel, 0);
			audioManager.setMode(AudioManager.MODE_NORMAL);
			audioManager.setSpeakerphoneOn(false);
			Log.d(LOG_TAG, "Audio Mode: Stop Ring");
			mplayer.stop();
			Log.d(LOG_TAG, "Ring stopped");

			JSONObject currentVolume = new JSONObject();
			try {
				currentVolume.put("currentVolume", orignalVolumeLevel);
				currentVolume.put("maxVolumeLevel", maxVolumeLevel);
				Log.d(LOG_TAG, "Volume Object:" + currentVolume);
				callbackContext.success(currentVolume);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return true;
		} else {
			callbackContext.error("Error setting volume");
			return false;
		}
	}

	/**
	 * This method is used to select the audio mode for adjustsment
	 * @param mode
	 * @param callbackContext
	 * @return
	 */
	public boolean setAudioMode(String mode) {

		if (mode.equals("earpiece")) {
			Log.d(LOG_TAG, "Audio Mode: EarPiece");
			audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
			audioManager.setSpeakerphoneOn(false);
			return true;
		} else if (mode.equals("speaker")) {
			Log.d(LOG_TAG, "Audio Mode: Speaker");
			audioManager.setMode(AudioManager.STREAM_MUSIC);
			audioManager.setSpeakerphoneOn(true);
			return true;
		} else if (mode.equals("ringtone")) {
			Log.d(LOG_TAG, "Audio Mode: RingTone");
			audioManager.setMode(AudioManager.MODE_RINGTONE);
			audioManager.setSpeakerphoneOn(false);
			return true;
		} else if (mode.equals("ringstart")) {
			Log.d(LOG_TAG, "Audio Mode: Ring");
			orignalVolumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			audioManager.setSpeakerphoneOn(true);
			audioManager.setMode(AudioManager.STREAM_MUSIC);
			int maxVolumeLevel = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			float percent = 0.7f;
			int seventyVolume = (int) (maxVolumeLevel * percent);
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);

			mplayer = MediaPlayer.create(context, notification);
			mplayer.start();
			Log.d(LOG_TAG, "Ring started");
			return true;
		} else if (mode.equals("normal")) {
			Log.d(LOG_TAG, "Audio Mode: Normal");
			audioManager.setMode(AudioManager.MODE_NORMAL);
			audioManager.setSpeakerphoneOn(false);
			return true;
		} else if (mode.equals("volume_up")) {
			Log.d(LOG_TAG, "Audio volume up");
			audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.STREAM_MUSIC);
			return true;
		} else if (mode.equals("volume_down")) {
			Log.d(LOG_TAG, "Audio volume down");
			audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.STREAM_MUSIC);
			return true;
		}

		return false;
	}
}
