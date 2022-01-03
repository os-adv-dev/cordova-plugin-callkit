package com.dmarc.cordovacall;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.collection.ArrayMap;
import androidx.core.app.ActivityCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

public class FirebaseNotificationServiceExtension extends FirebaseMessagingService {
	private boolean showForegroundPush;
	private int permissionCounter = 0;
	private String pendingAction;
	private TelecomManager tm;
	private PhoneAccountHandle handle;
	private String from;
	private static final String TAG = "FirebaseNotificSvExt";

	public FirebaseNotificationServiceExtension() {

	}

	@Override
	public void onCreate() {
		super.onCreate();

		String appName = CordovaCall.getApplicationName(getApplicationContext());
		handle = new PhoneAccountHandle(new ComponentName(getApplicationContext(), ConnectionServiceCordova.class), appName);
		tm = (TelecomManager) getApplicationContext().getSystemService(getApplicationContext().TELECOM_SERVICE);
	}

	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		super.onMessageReceived(remoteMessage);
		Map<String, String> messageData = (ArrayMap<String, String>) remoteMessage.getData();

		if (!messageData.containsKey("u")) {
			return;
		}
		try {
			JSONObject dataObj = new JSONObject(messageData.get("u"));

			if(dataObj.has("isEndCall")){
				if (dataObj.getBoolean("isEndCall")){
					endCall();
					return;
				}
			}
			boolean preferencesSaved = getSharedPreferences()
					.edit()
					.putString(Constants.MEETING_NUMBER, dataObj.getString(Constants.MEETING_NUMBER))
					.putString(Constants.MEETING_PASSWORD, dataObj.getString(Constants.MEETING_PASSWORD))
					.commit();
			if (!preferencesSaved) {
				Log.e(TAG, "Error saving preferences. Unable to start call");
			}

			Connection conn = ConnectionServiceCordova.getConnection();
			if (conn != null) {
				if (conn.getState() == Connection.STATE_ACTIVE) {
					Log.e(TAG, "You can't receive a call right now because you're already in a call");
				} else {
					Log.e(TAG, "You can't receive a call right now");
				}
			} else {
				from = dataObj.getString(Constants.DISPLAY_NAME);
				permissionCounter = 2;
				pendingAction = "receiveCall";
				this.checkCallPermission();
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void checkCallPermission() {
		if (permissionCounter >= 1) {
			PhoneAccount currentPhoneAccount = tm.getPhoneAccount(handle);
			if (currentPhoneAccount.isEnabled()) {
				if (pendingAction == "receiveCall") {
					this.receiveCall();
				}
			} else {
				if (permissionCounter == 2) {
					Intent phoneIntent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
					phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					//this.cordova.getActivity().getApplicationContext().startActivity(phoneIntent);
					startActivity(phoneIntent);
				} else {
					Log.e(TAG, "You need to accept phone account permissions in order to send and receive calls");
					//this.callbackContext.error("You need to accept phone account permissions in order to send and receive calls");
				}
			}
		}
		permissionCounter--;
	}

	private void endCall() {
		Connection conn = ConnectionServiceCordova.getConnection();
		if(conn != null) {
			DisconnectCause cause = new DisconnectCause(DisconnectCause.CANCELED);
			conn.setDisconnected(cause);
			conn.destroy();
			ConnectionServiceCordova.deinitConnection();
			ArrayList<CallbackContext> callbackContexts = CordovaCall.getCallbackContexts().get("canceled");
			if (callbackContexts != null) {
				for (final CallbackContext callbackContext : callbackContexts) {
					CordovaCall.getCordova().getThreadPool().execute(new Runnable() {
						public void run() {
							PluginResult result = new PluginResult(PluginResult.Status.OK, "canceled event called successfully");
							result.setKeepCallback(true);
							callbackContext.sendPluginResult(result);
						}
					});
				}
			}
		}
	}

	private void receiveCall() {
		Bundle callInfo = new Bundle();
		callInfo.putString("from",from);
		tm.addNewIncomingCall(handle, callInfo);
		permissionCounter = 0;
		//this.callbackContext.success("Incoming call successful");
	}

	SharedPreferences getSharedPreferences(){
		return getSharedPreferences(Constants.PREFERENCES_FILE_NAME, Activity.MODE_PRIVATE);
	}

}
