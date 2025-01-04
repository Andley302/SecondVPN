package app.one.secondvpnlite;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import app.one.secondvpnlite.util.SharedPref;


public class SecondVPN extends Application {
    private static final String TAG = SecondVPN.class.getSimpleName();
    public static final String PREFS_GERAL = "SECONDVPN_PREFS";
    public static final String FIRST_START = "FIRST_START_PREFS";

    public static SharedPref app_prefs;



    public static AppOpenManager appOpenManager;

    public static boolean isEnableCustomDNS () {
        return  app_prefs.getBoolean("IS_ENABLE_DNS", true);
    }

    public static long data_validade_file() {
        return  app_prefs.getLong("VALIDADE_CONFIG", 0);
    }
    public static boolean isEnableTethering () {
        return  app_prefs.getBoolean("IS_ENABLE_TETHERING", false);
    }
    public static boolean isEnableTetheringRoot () {
        return  app_prefs.getBoolean("IS_ENABLE_TETHERING_ROOT", false);
    }
    public static boolean isEnableCustomUDP () {
        return  app_prefs.getBoolean("IS_ENABLE_UDP", true);
    }
    public static boolean isEnableNotification () {
        return  app_prefs.getBoolean("IS_ENABLE_NOTIFICATION", true);
    }
    public static boolean isEnableNoTCPDelay () {
        return  app_prefs.getBoolean("IS_ENABLE_NO_TCP_DELAY", true);
    }
    public static boolean isEnableSSHCompress () {
        return  app_prefs.getBoolean("IS_ENABLE_SSH_COMPRESS", true);
    }
    public static boolean getIsCustomFileIsLocked () {
        return  app_prefs.getBoolean("IS_CUSTOM_FILE_LOCKED", false);
    }
    public static boolean getIsCustomFileLockSettings () {
        return  app_prefs.getBoolean("IS_CUSTOM_FILE_LOCK_SETTINGS", false);
    }
    public static final boolean isPayloadAfterTLS () {
        return  app_prefs.getBoolean("PAYLOAD_AFTER_TLS", false);
    }

    public static boolean isHTTPDirect () {
        return  app_prefs.getBoolean("IS_HTTP_DIRECT", false);
    }

    public static boolean isLockLoginEdit () {
        return  app_prefs.getBoolean("IS_LOCK_LOGIN_EDIT", false);
    }

    public static boolean isEnableWakeLock () {
        return  app_prefs.getBoolean("IS_ENABLE_WAKELOCK", true);
    }
    public static boolean isShowLogs(){
        return  app_prefs.getBoolean("IS_SHOW_LOGS",true);
    }

    public static boolean isIsCustomConfigOnlyMobileData () {
        return  app_prefs.getBoolean("IS_CONFIG_ONLY_MOBILE_DATA", false);
    }

    public static String getUDPResolver() {
        return app_prefs.getString("UDP_ADDR","127.0.0.1:7300");
    }
    public static String customDNS1(){
        return app_prefs.getString("CUSTOM_DNS_1","1.1.1.1");
    }

    public static String customDNS2(){
        return app_prefs.getString("CUSTOM_DNS_2","1.0.0.1");
    }
    public static String getServidorSSHDomain(){
        return app_prefs.getString("SSH_SERVER_DOMAIN","");
    }
    public static String getSNI(){
        return app_prefs.getString("CUSTOM_SNI","");
    }
    public static String getProxyIPDomain(){
        return app_prefs.getString("PROXY_IP_DOMAIN","");
    }
    public static String getTLSVersion(){
        return app_prefs.getString("TLS_VERSION","auto");
    }
    public static String getConnectionMode(){
        return app_prefs.getString("CONNECTION_MODE","");
    }
    public static String getPayloadKey(){
        return app_prefs.getString("PAYLOAD_KEY","");
    }
    public static String getUsuarioAndPass (){
        return app_prefs.getString("SSH_AUTH_DATA","");
    }

    public static String getConfigMsgText (){
        return app_prefs.getString("CONFIG_MSG","");
    }

    public static String getCurrentVpnStatus (){
        return app_prefs.getString("LAST_VPN_STATUS","DESCONECTADO");
    }

    public static void setCurrentVpnStatus (String status){
        app_prefs.putString("LAST_VPN_STATUS",status);
    }

    public static boolean getLastA(){
        return app_prefs.getBoolean("LAST_A",false);
    }

    public static void setDefaultPrefs(){
        app_prefs.clear();
        app_prefs.putString("CONNECTION_MODE","MODO_HTTP");
        app_prefs.putBoolean("IS_HTTP_DIRECT",true);
        app_prefs.putBoolean("LAST_A",false);
        app_prefs.putBoolean("PAYLOAD_AFTER_TLS",false);
        app_prefs.putBoolean("IS_CUSTOM_FILE_LOCKED",false);
        app_prefs.putString("LAST_VPN_STATUS","DESCONECTADO");

    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        app_prefs  = SharedPref.getInstance(this);

        MobileAds.initialize(
                this,
                new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(InitializationStatus initializationStatus) {}
                });
        appOpenManager = new AppOpenManager(this);

     }


}
