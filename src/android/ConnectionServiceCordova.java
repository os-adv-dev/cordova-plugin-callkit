package com.dmarc.cordovacall;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.StatusHints;
import android.telecom.TelecomManager;
import android.os.Handler;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

public class ConnectionServiceCordova extends ConnectionService {

    private static String TAG = "ConnectionServiceCordova";
    private static Connection conn;

    public static Connection getConnection() {
        return conn;
    }

    public static void deinitConnection() {
        conn = null;
    }

    @Override
    public Connection onCreateIncomingConnection(final PhoneAccountHandle connectionManagerPhoneAccount, final ConnectionRequest request) {
        final Connection connection = new Connection() {
            @Override
            public void onAnswer() {
                this.setActive();

                KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (!km.isKeyguardLocked()) {
                    boolean preferencesSaved = getSharedPreferences()
                            .edit()
                            .putBoolean(Constants.CALL_ANSWERED_BY_USER, true)
                            .commit();
                    if (!preferencesSaved){
                        Log.e(TAG, "Error saving preferences. OutSystems won't start the call");
                    }
                }
                ///Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                Intent intent = new Intent(getApplicationContext(), getMainActivity());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onReject() {
                DisconnectCause cause = new DisconnectCause(DisconnectCause.REJECTED);
                this.setDisconnected(cause);
                this.destroy();
                conn = null;
                ArrayList<CallbackContext> callbackContexts = CordovaCall.getCallbackContexts().get("reject");
                if (callbackContexts != null) {
                    for (final CallbackContext callbackContext : callbackContexts) {
                        CordovaCall.getCordova().getThreadPool().execute(new Runnable() {
                            public void run() {
                                PluginResult result = new PluginResult(PluginResult.Status.OK, "reject event called successfully");
                                result.setKeepCallback(true);
                                callbackContext.sendPluginResult(result);
                            }
                        });
                    }
                }
            }


            @Override
            public void onDisconnect() {
                DisconnectCause cause = new DisconnectCause(DisconnectCause.LOCAL);
                this.setDisconnected(cause);
                this.destroy();
                conn = null;
                ArrayList<CallbackContext> callbackContexts = CordovaCall.getCallbackContexts().get("hangup");
                if (callbackContexts != null) {
                    for (final CallbackContext callbackContext : callbackContexts) {
                        CordovaCall.getCordova().getThreadPool().execute(new Runnable() {
                            public void run() {
                                PluginResult result = new PluginResult(PluginResult.Status.OK, "hangup event called successfully");
                                result.setKeepCallback(true);
                                callbackContext.sendPluginResult(result);
                            }
                        });
                    }
                }
            }
        };
        connection.setAddress(Uri.parse(request.getExtras().getString("from")), TelecomManager.PRESENTATION_ALLOWED);
        Icon icon = CordovaCall.getIcon();
        if(icon != null) {
            StatusHints statusHints = new StatusHints((CharSequence)"", icon, new Bundle());
            connection.setStatusHints(statusHints);
        }
        conn = connection;
        ArrayList<CallbackContext> callbackContexts = CordovaCall.getCallbackContexts().get("receiveCall");
        if (callbackContexts != null) {
            for (final CallbackContext callbackContext : callbackContexts) {
                CordovaCall.getCordova().getThreadPool().execute(new Runnable() {
                    public void run() {
                        PluginResult result = new PluginResult(PluginResult.Status.OK, "receiveCall event called successfully");
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                });
            }
        }
        return connection;
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        final Connection connection = new Connection() {
            @Override
            public void onAnswer() {
                super.onAnswer();
            }

            @Override
            public void onReject() {
                super.onReject();
            }

            @Override
            public void onAbort() {
                super.onAbort();
            }

            @Override
            public void onDisconnect() {
                DisconnectCause cause = new DisconnectCause(DisconnectCause.LOCAL);
                this.setDisconnected(cause);
                this.destroy();
                conn = null;
                ArrayList<CallbackContext> callbackContexts = CordovaCall.getCallbackContexts().get("hangup");
                if (callbackContexts != null) {
                    for (final CallbackContext callbackContext : callbackContexts) {
                        CordovaCall.getCordova().getThreadPool().execute(new Runnable() {
                            public void run() {
                                PluginResult result = new PluginResult(PluginResult.Status.OK, "hangup event called successfully");
                                result.setKeepCallback(true);
                                callbackContext.sendPluginResult(result);
                            }
                        });
                    }
                }
            }

            @Override
            public void onStateChanged(int state) {
              if(state == Connection.STATE_DIALING) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(CordovaCall.getCordova().getActivity().getApplicationContext(), CordovaCall.getCordova().getActivity().getClass());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        CordovaCall.getCordova().getActivity().getApplicationContext().startActivity(intent);
                    }
                }, 500);
              }
            }
        };
        connection.setAddress(Uri.parse(request.getExtras().getString("to")), TelecomManager.PRESENTATION_ALLOWED);
        Icon icon = CordovaCall.getIcon();
        if(icon != null) {
            StatusHints statusHints = new StatusHints((CharSequence)"", icon, new Bundle());
            connection.setStatusHints(statusHints);
        }
        connection.setDialing();
        conn = connection;
        ArrayList<CallbackContext> callbackContexts = CordovaCall.getCallbackContexts().get("sendCall");
        if(callbackContexts != null) {
            for (final CallbackContext callbackContext : callbackContexts) {
                CordovaCall.getCordova().getThreadPool().execute(new Runnable() {
                    public void run() {
                        PluginResult result = new PluginResult(PluginResult.Status.OK, "sendCall event called successfully");
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                });
            }
        }
        return connection;
    }

    SharedPreferences getSharedPreferences(){
        return getSharedPreferences(Constants.PREFERENCES_FILE_NAME, Activity.MODE_PRIVATE);
    }

    /** Gets the "Main class" for this app so we can start the activity
     * without knowing the package e.g. com.outsystems.experts.project.MainActivity
     * **/
    Class getMainActivity(){
        Class mainActivity;
        Context context = getApplicationContext();
        String  packageName = context.getPackageName();
        Intent  launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String  className = launchIntent.getComponent().getClassName();

        try {
            //loading the Main Activity to not import it in the plugin
            mainActivity = Class.forName(className);
            return mainActivity;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
