package io.github.astarProxy;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by maryam on 5/20/2018.
 */

public class Preferences extends AppCompatActivity {
    private SharedPreferences prefPort, prefServer, prefUser, prefPass;
    private static String PORT = "port";
    private static String SERVER = "server";
    private static String USERNAME = "username";
    private static String PASSWORD = "password";
    public static final String PREF_PORT = "PrefPort";
    public static final String PREF_SERVER = "PrefServer";
    public static final String PREF_USERNAME = "PrefUsername";
    public static final String PREF_PASSWORD = "PrefPass";

    public void setPrefPort(int port, Context context) {
        prefPort = context.getSharedPreferences(PREF_PORT, 0);
        SharedPreferences.Editor editor = prefPort.edit();
        editor.putInt(PORT, port);
        editor.commit();
    }

    public Integer getPrefPort(Context context) {
        prefPort = context.getSharedPreferences(PREF_PORT, 0);
         Integer str = prefPort.getInt(PORT, 0);
        return str;
    }

    public void setPrefServer(String server, Context context) {
        prefServer = context.getSharedPreferences(PREF_SERVER, 0);
        SharedPreferences.Editor editor = prefServer.edit();
        editor.putString(SERVER, server);
        editor.commit();
    }

    public String getPrefServer(Context context) {
        prefServer = context.getSharedPreferences(PREF_SERVER, 0);
        String str = prefServer.getString(SERVER, "");
        return str;
    }

    public void setPrefUser(String user, Context context) {
        prefUser = context.getSharedPreferences(PREF_USERNAME, 0);
        SharedPreferences.Editor editor = prefUser.edit();
        editor.putString(USERNAME, user);
        editor.commit();
    }

    public String getPrefUser(Context context) {
        prefUser = context.getSharedPreferences(PREF_USERNAME, 0);
        String str = prefUser.getString(USERNAME, "");
        return str;
    }

    public void setPrefPass(String pass, Context context) {
        prefPass = context.getSharedPreferences(PREF_PASSWORD, 0);
        SharedPreferences.Editor editor = prefPass.edit();
        editor.putString(PASSWORD, pass);
        editor.commit();
    }

    public String getPrefPass(Context context) {
        prefPass = context.getSharedPreferences(PREF_PASSWORD, 0);
        String str = prefPass.getString(PASSWORD, "");
        return str;
    }


}
