package app.one.secondvpnlite;


import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import app.one.secondvpnlite.configs.ExportConfig;
import app.one.secondvpnlite.configs.ImportConfig;
import app.one.secondvpnlite.drawer.DrawerLog;
import app.one.secondvpnlite.logs.AppLogManager;
import app.one.secondvpnlite.service.TunnelManager;
import app.one.secondvpnlite.settings.AppSettings;
import app.one.secondvpnlite.tunnel.TunnelUtils;

public class MainActivity extends AppCompatActivity implements DrawerLayout.DrawerListener,View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    public static SharedPreferences app_prefs;
    public static SharedPreferences start_msg;
    public static final int START_VPN_PROFILE = 70;
    private final static String TAG = "MainActivity";

    private Thread mTunnelThread;
    private TunnelManager mTunnelManager;


    public static Handler UIHandler;

    static
    {
        UIHandler = new Handler(Looper.getMainLooper());
    }



    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }
    public static void setButtonStatus(String status){
        switch (status){
            case "INICIANDO":
                MainActivity.runOnUI(() -> start_button.setText(R.string.stop));
                try{
                    start_button.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "CONECTANDO":
                MainActivity.runOnUI(() -> start_button.setText(R.string.stop));
                try{
                    start_button.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "CONECTADO":
                MainActivity.runOnUI(() -> start_button.setText(R.string.stop));
                try{
                    start_button.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "DESCONECTADO":
                MainActivity.runOnUI(() -> {
                    enableInterfaceOnConnect();
                    start_button.setText(R.string.start);
                    try{
                        start_button.setEnabled(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case "PARANDO":
                //disableInterfaceOnConnect();
                MainActivity.runOnUI(() -> {
                    start_button.setText(R.string.stopping);
                    try{
                        start_button.setEnabled(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
        }

    }

    public static void updateUIAfterImport(){
        if (!TunnelManager.isServiceRunning){
        //if (!SecondVPN.getCurrentVpnStatus().equals("CONECTADO")){
            MainActivity.runOnUI(() -> {
               updateLayoutWithConfig1();
               updateLayoutWithConfig2();
               updateLayoutWithConfig3();
            });
        }

    }
    @SuppressLint("StaticFieldLeak")
    public static Button start_button ;
    @SuppressLint("StaticFieldLeak")
    public static TextView servidor_ssh;
    @SuppressLint("StaticFieldLeak")
    public static TextView servidor_proxy;
    @SuppressLint("StaticFieldLeak")
    public static TextView custom_sni;
    @SuppressLint("StaticFieldLeak")
    public static TextView custom_payload;
    @SuppressLint("StaticFieldLeak")
    public static TextView user_and_pass;
    public static AppCompatRadioButton modo_http;
    public static AppCompatRadioButton modo_https;
    public static SwitchCompat payload_after_tls;
    public static SwitchCompat direct_mode;
    @SuppressLint("StaticFieldLeak")
    public static TextView config_msg;
    @SuppressLint("StaticFieldLeak")
    private static LinearLayout servidor_ssh_layout;
    @SuppressLint("StaticFieldLeak")
    private static LinearLayout servidor_proxy_layout;
    @SuppressLint("StaticFieldLeak")
    private static LinearLayout custom_sni_layout;
    @SuppressLint("StaticFieldLeak")
    private static LinearLayout payload_layout;
    @SuppressLint("StaticFieldLeak")
    private static LinearLayout user_and_pass_layout;

    private boolean configIsOk;
    private boolean exportWithoutLogin = false;
    private boolean intentToImport = false;

    private DrawerLog mDrawer;
    private Toolbar app_toolbar;

    private static AdView mAdView;
    private static InterstitialAd mInterstitialAd;
    public static Context sContext;
    public static Activity sActivity;
    public static PowerManager.WakeLock wakeLock;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startApp();


    }

    private void startApp(){
        mDrawer = new DrawerLog(this);
        setContentView(R.layout.activity_main_drawer);
        app_toolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar(app_toolbar);
        //CORRIGE ABRIR TECLADO MSM COM INPUTTEXT DESABILITADO EM ALGUNS TELEFONES
        app_toolbar.requestFocus();

        sActivity = this;
        sContext = this;

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        //List<String> testDeviceIds = Arrays.asList("6F24778EBA50C09BF8ABE74236319D29");
        List<String> testDeviceIds = Arrays.asList("74961B0CF01927ADD5D88020D3CC1F9C", "6F24778EBA50C09BF8ABE74236319D29");
        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
        MobileAds.setRequestConfiguration(configuration);

        //DECLARE ADVIEW
        mAdView = findViewById(R.id.adView);

        mDrawer.setDrawer(this);

        app_prefs = getSharedPreferences(SecondVPN.PREFS_GERAL, Context.MODE_PRIVATE);
        start_msg = getSharedPreferences(SecondVPN.FIRST_START, Context.MODE_PRIVATE);

        boolean showFirstTime = start_msg.getBoolean("default_config", true);

       /*PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        boolean isIgnoringBatteryOptimizations;*/
        Intent intent = getIntent();
        if (showFirstTime  && intent != null && !intent.getBooleanExtra("IS_IMPORT", false)) {
            SecondVPN.setDefaultPrefs();
            start_msg.edit().putBoolean("default_config", false).apply();
            showStartMsg();
        }else if (showFirstTime  && intent != null && intent.getBooleanExtra("IS_IMPORT", false)){
            start_msg.edit().putBoolean("default_config", false).apply();
            showStartMsg();
        }


        servidor_ssh_layout = (LinearLayout) findViewById(R.id.activity_mainInputServerSSHIPORTLayout);
        servidor_proxy_layout = (LinearLayout) findViewById(R.id.activity_mainInputServerPROXYIPORTLayout);
        custom_sni_layout = (LinearLayout) findViewById(R.id.activity_mainInputSNILayout);
        payload_layout = (LinearLayout) findViewById(R.id.activity_mainInputPayloadLinearLayout);
        user_and_pass_layout = (LinearLayout) findViewById(R.id.activity_mainInputUserandPassLayout);
        start_button = (Button) findViewById(R.id.activity_StartConnection);


        FloatingActionButton logs = findViewById(R.id.logs);
        logs.setOnClickListener(this);
        // if (SecondVPN.getCurrentVpnStatus().equals("CONECTADO")){
        if (TunnelManager.isServiceRunning){
            disableInterfaceOnConnect();
            start_button.setText(this.getString(R.string.stop));
        }else{
            enableInterfaceOnConnect();
            start_button.setText(this.getString(R.string.start));
        }

        user_and_pass= (TextView) findViewById(R.id.activity_mainInputUserandPass);
        servidor_ssh = (TextView) findViewById(R.id.activity_mainInputServerSSHIPORT);
        servidor_proxy = (TextView) findViewById(R.id.activity_mainInputServerPROXYIPORT);
        custom_sni = (TextView) findViewById(R.id.activity_mainInputSNI);
        custom_payload = (TextView) findViewById(R.id.activity_mainInputPayloadEditText);
        modo_http = (AppCompatRadioButton) findViewById(R.id.modo_http);
        modo_https = (AppCompatRadioButton) findViewById(R.id.modo_https);
        payload_after_tls = (SwitchCompat) findViewById(R.id.activity_mainPayloadAfterTLS);
        direct_mode = (SwitchCompat) findViewById(R.id.activity_mainDirectMode);
        config_msg = (TextView) findViewById(R.id.config_msg_textview);

        //SETUP AD
        setup_ad();

        updateLayoutWithConfig1();
        updateLayoutWithConfig2();
        start_button.setOnClickListener(this);
        modo_https.setOnCheckedChangeListener(this);
        modo_http.setOnCheckedChangeListener(this);
        payload_after_tls.setOnCheckedChangeListener(this);
        direct_mode.setOnCheckedChangeListener(this);

        //Toast.makeText(this,"This is toast",Toast.LENGTH_SHORT).show();

        updateLayoutWithConfig3();
        //if (SecondVPN.getCurrentVpnStatus().equals("CONECTADO")){
        if (TunnelManager.isServiceRunning){
            disableInterfaceOnConnect();
            start_button.setText(this.getString(R.string.stop));
        }else{
            enableInterfaceOnConnect();
            start_button.setText(this.getString(R.string.start));
        }


        //LOAD AD
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            if (intent != null && !intent.getBooleanExtra("IS_IMPORT", false)){
                ladi();
            }
        }else if (TunnelManager.isServiceRunning)/*(SecondVPN.getCurrentVpnStatus().equals("CONECTADO"))*/ {
            if (intent != null && !intent.getBooleanExtra("IS_IMPORT", true)){
                ladi();
            }
        }

        //ccb();

    }


    /*private void ccb() {
        if (SecondVPN.getIsCustomFileIsLocked() && !servidor_ssh.getText().toString().contains(getAst())){
             f(c());
        }
    }*/

    @Override
    public void onBackPressed()
    {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);

        super.onBackPressed();
    }

    private boolean duringisLoad = false;
    private static void setup_ad(){
        //ADMOB KEYS
        AdView adView = new AdView(sContext);

        adView.setAdSize(AdSize.BANNER);
        String ab = "ca-app";
        if (BuildConfig.DEBUG){
            ab = "ca-app-pub-3940256099942544/6300978111";
        }else{
            ab = "ca-app-pub-2955918284006466/5236078879";
        }
        adView.setAdUnitId(ab);
        // TODO: Add adView to your view hierarchy.

        //ADREQUEST E CARREGA BANNER
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        String ai = "ca-app";
        if (BuildConfig.DEBUG){
            ai = "ca-app-pub-3940256099942544/1033173712";
        }else{
            ai = "ca-app-pub-2955918284006466/2356432963";
        }

        //CARREGA INTERSTITIAL AD
        InterstitialAd.load(sContext,ai, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i(TAG, loadAdError.getMessage());
                        mInterstitialAd = null;

                    }
                });
    }

    static int delay = 4000;
    static boolean isShowing = false;
    public static void ladi(){
        if (!SecondVPN.getLastA()){
            if (!isShowing){
                sActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        if (mInterstitialAd == null){
                            delay = 6000; //ver o tempo bom pra esse delay
                            setup_ad();
                        }
                        try{
                           // ProgressDialog pd = new ProgressDialog(sActivity);
                           // pd.setMessage(sContext.getString(R.string.please_wait_trying));
                           // pd.setCancelable(false);
                           // pd.show();
                            isShowing = true;

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (mInterstitialAd != null) {
                                        mInterstitialAd.show(sActivity);
                                        app_prefs.edit().putBoolean("LAST_A",true).apply();
                                        isShowing = false;
                                        /*try{
                                            if (pd.isShowing()){
                                                pd.dismiss();
                                                isShowing = false;
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }*/


                                    } else {
                                        isShowing = false;
                                        /*try{
                                            if (pd.isShowing()){
                                                pd.dismiss();
                                                isShowing = false;
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }*/
                                        Log.d("TAG", "The interstitial ad wasn't ready yet.");
                                    };
                                }
                            }, delay);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }




                    }
                });
            }
        }

    }

    private void clear_textviews(){
        try{
                servidor_ssh.setText("");
                custom_payload.setText("");
                user_and_pass.setText("");
                servidor_proxy.setText("");
                direct_mode.setText("");
                custom_sni.setText("");
                user_and_pass.setText("");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateLayoutWithConfig1(){
        if (!SecondVPN.getIsCustomFileIsLocked() && SecondVPN.getConnectionMode().equals("MODO_HTTP")){
            payload_after_tls.setEnabled(false);
            payload_after_tls.setVisibility(View.GONE);
            direct_mode.setEnabled(true);
            direct_mode.setVisibility(View.VISIBLE);
            modo_http.setChecked(true);
            modo_https.setChecked(false);
            custom_sni_layout.setVisibility(View.GONE);

            if (SecondVPN.isHTTPDirect()){
                direct_mode.setChecked(true);
                servidor_proxy_layout.setVisibility(View.GONE);
            }else{
                direct_mode.setChecked(false);
                servidor_proxy_layout.setVisibility(View.VISIBLE);
            }

        }
        if (!SecondVPN.getIsCustomFileIsLocked() && SecondVPN.getConnectionMode().equals("MODO_HTTPS")){
            payload_after_tls.setEnabled(true);
            payload_after_tls.setVisibility(View.VISIBLE);
            direct_mode.setEnabled(false);
            modo_http.setChecked(false);
            direct_mode.setVisibility(View.GONE);
            modo_https.setChecked(true);
            custom_sni_layout.setVisibility(View.VISIBLE);
            servidor_proxy_layout.setVisibility(View.GONE);
            if (SecondVPN.isPayloadAfterTLS()){
                payload_layout.setVisibility(View.VISIBLE);
                payload_after_tls.setChecked(true);
            }else{
                  payload_after_tls.setChecked(false);
                payload_layout.setVisibility(View.GONE);

            }
        }
    }

    public static void updateLayoutWithConfig2(){
        if (SecondVPN.getConnectionMode().equals("MODO_HTTPS")){
            modo_https.setChecked(true);
            modo_http.setChecked(false);
            direct_mode.setVisibility(View.GONE);
            payload_after_tls.setVisibility(View.VISIBLE);
            custom_sni_layout.setVisibility(View.VISIBLE);

            if (SecondVPN.isPayloadAfterTLS()){
                payload_after_tls.setChecked(true);
            }else{
                payload_after_tls.setChecked(false);
            }
        }

        if (SecondVPN.getConnectionMode().equals("MODO_HTTP")){
            modo_https.setChecked(false);
            modo_http.setChecked(true);

            direct_mode.setVisibility(View.VISIBLE);
            payload_after_tls.setVisibility(View.GONE);
            custom_sni_layout.setVisibility(View.GONE);

            if (SecondVPN.isHTTPDirect()){
                direct_mode.setChecked(true);

            }else{
                direct_mode.setChecked(false);
            }
        }
    }

    private static void updateLayoutWithConfig3(){
        if (SecondVPN.getIsCustomFileIsLocked()){
            if (SecondVPN.isLockLoginEdit()){
                user_and_pass.setEnabled(false);
                user_and_pass.setText(SecondVPN.getUsuarioAndPass().replaceAll(".", "*"));
            }else{
                user_and_pass.setText(SecondVPN.getUsuarioAndPass());
                user_and_pass.setEnabled(true);
            }


            servidor_ssh.setEnabled(false);
            servidor_ssh.setText(SecondVPN.getServidorSSHDomain().replaceAll(".","*"));

            servidor_proxy.setEnabled(false);
            servidor_proxy.setText(SecondVPN.getProxyIPDomain().replaceAll(".","*"));

            custom_sni.setEnabled(false);
            custom_sni.setText(SecondVPN.getSNI().replaceAll(".", "*"));

            custom_payload.setEnabled(false);
            custom_payload.setText(SecondVPN.getPayloadKey().replaceAll(".","*"));


            if (modo_http.isChecked()){
                if (direct_mode.isChecked()){
                    servidor_proxy_layout.setVisibility(View.GONE);
                    direct_mode.setChecked(true);
                    direct_mode.setEnabled(false);
                    payload_layout.setVisibility(View.VISIBLE);
                    payload_after_tls.setVisibility(View.GONE);
                    direct_mode.setVisibility(View.VISIBLE);

                }else{
                    servidor_proxy_layout.setVisibility(View.VISIBLE);
                    payload_layout.setVisibility(View.VISIBLE);
                    payload_after_tls.setVisibility(View.GONE);
                    direct_mode.setVisibility(View.VISIBLE);
                    direct_mode.setChecked(false);
                    direct_mode.setEnabled(false);
                }
            }
            if (modo_https.isChecked()) {
                if (payload_after_tls.isChecked()){
                    servidor_proxy_layout.setVisibility(View.GONE);
                    payload_after_tls.setChecked(true);
                    payload_after_tls.setEnabled(false);
                    payload_layout.setVisibility(View.VISIBLE);
                    payload_after_tls.setVisibility(View.VISIBLE);

                }else{
                    servidor_proxy_layout.setVisibility(View.GONE);
                    payload_after_tls.setChecked(false);
                    payload_after_tls.setEnabled(false);
                    payload_layout.setVisibility(View.GONE);
                    payload_after_tls.setVisibility(View.VISIBLE);
                }
            }

            modo_http.setEnabled(false);
            modo_https.setEnabled(false);


            config_msg.setEnabled(true);
            config_msg.setVisibility(View.VISIBLE);
            config_msg.setText(Html.fromHtml(SecondVPN.getConfigMsgText()));

        }else if (!SecondVPN.getIsCustomFileIsLocked()){
            servidor_ssh.setEnabled(true);
            servidor_ssh.setText(SecondVPN.getServidorSSHDomain());

            servidor_proxy.setEnabled(true);
            servidor_proxy.setText(SecondVPN.getProxyIPDomain());

            custom_sni.setEnabled(true);
            custom_sni.setText(SecondVPN.getSNI());

            custom_payload.setEnabled(true);
            custom_payload.setText(SecondVPN.getPayloadKey());

            modo_http.setEnabled(true);
            modo_https.setEnabled(true);

            payload_after_tls.setEnabled(true);
            direct_mode.setEnabled(true);

            user_and_pass.setEnabled(true);
            user_and_pass.setText(SecondVPN.getUsuarioAndPass());

            config_msg.setEnabled(true);
            config_msg.setVisibility(View.VISIBLE);
            config_msg.setText(Html.fromHtml(SecondVPN.getConfigMsgText()));

           /* if (SecondVPN.getConnectionMode().equals("MODO_HTTP")){
                if (SecondVPN.isHTTPDirect()){
                    servidor_proxy_layout.setVisibility(View.GONE);
                    direct_mode.setChecked(true);
                    direct_mode.setEnabled(false);
                    payload_layout.setVisibility(View.VISIBLE);
                    payload_after_tls.setVisibility(View.GONE);
                    direct_mode.setVisibility(View.VISIBLE);

                }else{
                    servidor_proxy_layout.setVisibility(View.VISIBLE);
                    payload_layout.setVisibility(View.VISIBLE);
                    payload_after_tls.setVisibility(View.GONE);
                    direct_mode.setVisibility(View.VISIBLE);
                    direct_mode.setChecked(false);
                    direct_mode.setEnabled(false);
                }
            }
            if (SecondVPN.getConnectionMode().equals("MODO_HTTPS")){
                if (SecondVPN.isPayloadAfterTLS()){
                    servidor_proxy_layout.setVisibility(View.GONE);
                    payload_after_tls.setChecked(true);
                    payload_after_tls.setEnabled(false);
                    payload_layout.setVisibility(View.VISIBLE);
                    payload_after_tls.setVisibility(View.VISIBLE);

                }else{
                    servidor_proxy_layout.setVisibility(View.GONE);
                    payload_after_tls.setChecked(false);
                    payload_after_tls.setEnabled(false);
                    payload_layout.setVisibility(View.GONE);
                    payload_after_tls.setVisibility(View.VISIBLE);
                }
            }*/

        }



    }

    private void showStartMsg() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this,R.style.AlertDialogTheme)
        //AlertDialog.Builder dialog = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(this.getString(R.string.start_msg_title))
                .setCancelable(true)
                .setMessage(this.getString(R.string.start_msg_text))
                .setPositiveButton("OK", (dialog1, which) -> {

                    // TODO: Add positive button action code here
                    app_prefs.edit().putBoolean("default_config",false).apply();
                    dialog1.dismiss();
                    getPermissionAndroid13();


                });
        dialog.show();
    }

    public void getPermissionAndroid13() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void stopVPNService(){
        try{
            TunnelManager.stopForwarderSocks();
            TunnelManager.isToStopService = true;
            mTunnelManager = new TunnelManager();
            mTunnelThread = new Thread(mTunnelManager);
            mTunnelThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVPNService() {
        Intent intent = VpnService.prepare(this);

        if (intent != null) {
            try {
                startActivityForResult(intent, START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                Toast.makeText(this, this.getString(R.string.error_request_permission), Toast.LENGTH_LONG).show();
            }
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }
    }



    private boolean checkConfig(boolean istoSave){
        configIsOk = true;
        //VERIFICA A REDE
        if (!TunnelUtils.isNetworkOnline(this)) {
            if (!istoSave){
                Toast.makeText(this, this.getString(R.string.no_network), Toast.LENGTH_SHORT).show();
                configIsOk = false;
            }

        }
        //USUARIO E SENHA VALIDAÇÃO
        if (!istoSave){
            exportWithoutLogin = true;
            try{
                String[] ipSplit = SecondVPN.getUsuarioAndPass().split("@");
                String user=  ipSplit[0];
                String passwd =  ipSplit[1];

                if (user.isEmpty()){
                    configIsOk = false;
                }
                if (passwd.isEmpty()){
                    configIsOk = false;
                }

            } catch (Exception e) {
                configIsOk = false;
                e.printStackTrace();
            }
        }else{
            exportWithoutLogin = false;
        }

        //SERVIDOR SSH VALIDAÇÃO
        try{
            String[] ipSplit = SecondVPN.getServidorSSHDomain().split(":");
            String servidor =  ipSplit[0];
            int porta = Integer.parseInt(ipSplit[1]);

            if (servidor.isEmpty()){
                configIsOk = false;
            }
            if (porta == 0){
                configIsOk = false;
            }
        } catch (Exception e) {
            configIsOk = false;
            e.printStackTrace();
        }

        if (SecondVPN.getConnectionMode().equals("MODO_HTTP")) {
            if (direct_mode.isChecked()) {
                if (SecondVPN.getServidorSSHDomain().isEmpty()){
                    configIsOk = false;
                }
                if (SecondVPN.getPayloadKey().isEmpty()){
                    configIsOk = false;
                }

            }else{
                try{
                    String[] ipSplit = SecondVPN.getProxyIPDomain().split(":");
                    String servidor =  ipSplit[0];
                    int porta = Integer.parseInt(ipSplit[1]);

                    if (servidor.isEmpty()){
                        configIsOk = false;
                    }
                    if (porta == 0){
                        configIsOk = false;
                    }
                } catch (Exception e) {
                    configIsOk = false;
                    e.printStackTrace();
                }

                if (SecondVPN.getServidorSSHDomain().isEmpty()){
                    configIsOk = false;
                }
                if (SecondVPN.getPayloadKey().isEmpty()){
                    configIsOk = false;
                }
                if (SecondVPN.getProxyIPDomain().isEmpty()){
                    configIsOk = false;
                }
            }
        }
        if (SecondVPN.getConnectionMode().equals("MODO_HTTPS")) {
            if (payload_after_tls.isChecked()){
                if (SecondVPN.getServidorSSHDomain().isEmpty()){
                    configIsOk = false;
                }
                if (SecondVPN.getSNI().isEmpty()){
                    configIsOk = false;
                }
                if (SecondVPN.getPayloadKey().isEmpty()){
                    configIsOk = false;
                }
            }else{
                if (SecondVPN.getServidorSSHDomain().isEmpty()){
                    configIsOk = false;
                }
                if (SecondVPN.getSNI().isEmpty()){
                    configIsOk = false;
                }
            }
        }

        if (SecondVPN.getIsCustomFileIsLocked()){
            if (SecondVPN.getIsCustomFileIsLocked() && SecondVPN.isIsCustomConfigOnlyMobileData()){
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager.isWifiEnabled()) {
                    configIsOk = false;
                    if(!istoSave){
                        Toast.makeText(this, getString(R.string.only_mobile_data_toast), Toast.LENGTH_SHORT).show();

                    }
                }
            }
            if (checkValidade(SecondVPN.data_validade_file())){
                configIsOk = false;
                if (!istoSave){
                    Toast.makeText(this, getString(R.string.file_validate_end), Toast.LENGTH_SHORT).show();

                }
            }
        }


        return configIsOk;
    }

    public static boolean checkValidade(long validadeDateMillis) {
        if (validadeDateMillis == 0) {
            return false;
        }

        // Get Current Date
        long date_atual = Calendar.getInstance()
                .getTime().getTime();

        //COVERTER PRA STRING
        //AppLogManager.addLog("Data atual: " + date_atual + " Vencimento: " + validadeDateMillis);

        if (date_atual >= validadeDateMillis) {
            return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == Activity.RESULT_OK) {
                //CHECA CONFIG
                if (checkConfig(false)){
                    //SETA ADS
                    app_prefs.edit().putBoolean("LAST_A",false).apply();
                    //DESATIVA BOTOES
                    disableInterfaceOnConnect();
                    //LIMPA LOGS
                    AppLogManager.clearLog();
                    //MOSTRA LOGS
                    if (SecondVPN.isShowLogs()){
                        showLog();
                    }

                    //SETA WAKELOCK
                    if (SecondVPN.isEnableWakeLock()){
                        try{
                            wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(1, "SecondVPNLite::WakeLock");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    //INICIA VPN
                    try{
                        TunnelManager.isToStopService = false;
                        TunnelManager.stoppedLog = false;
                        TunnelManager.stoppingLog = false;
                        TunnelManager.vpnDestroyedLog = false;

                        mTunnelManager = new TunnelManager();
                        mTunnelThread = new Thread(mTunnelManager);
                        mTunnelThread.start();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }else{
                    Toast.makeText(this, this.getString(R.string.empty_configs), Toast.LENGTH_LONG).show();
                }
                }
            }
    }


    private void showLog() {
        if (mDrawer != null && !isFinishing()) {
            DrawerLayout drawerLayout = mDrawer.getDrawerLayout();

            if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        }
    }

    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param buttonView The compound button view whose state has changed.
     * @param isChecked  The new checked state of buttonView.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            // savePreferences();
            if (modo_http.isChecked()) {
                app_prefs.edit().putString("CONNECTION_MODE", "MODO_HTTP").apply();

                payload_after_tls.setEnabled(false);
                direct_mode.setEnabled(true);
                payload_after_tls.setVisibility(View.GONE);
                custom_sni_layout.setVisibility(View.GONE);
                payload_layout.setVisibility(View.VISIBLE);
                direct_mode.setVisibility(View.VISIBLE);

                if (direct_mode.isChecked()) {
                    if (!SecondVPN.getIsCustomFileIsLocked()){
                        //app_prefs.edit().putBoolean("PAYLOAD_AFTER_TLS", false).apply();
                        app_prefs.edit().putBoolean("IS_HTTP_DIRECT", true).apply();
                    }
                    servidor_proxy_layout.setVisibility(View.GONE);
                } else {
                    if (!SecondVPN.getIsCustomFileIsLocked()){
                       //app_prefs.edit().putBoolean("PAYLOAD_AFTER_TLS", true).apply();
                       app_prefs.edit().putBoolean("IS_HTTP_DIRECT", false).apply();
                    }
                    servidor_proxy_layout.setVisibility(View.VISIBLE);
                }

            }
            if (modo_https.isChecked()){
                app_prefs.edit().putString("CONNECTION_MODE", "MODO_HTTPS").apply();
                payload_after_tls.setEnabled(true);
                direct_mode.setEnabled(false);
                payload_after_tls.setVisibility(View.VISIBLE);
                custom_sni_layout.setVisibility(View.VISIBLE);
                direct_mode.setVisibility(View.GONE);

                if (payload_after_tls.isChecked()) {
                    if (!SecondVPN.getIsCustomFileIsLocked()){
                       //app_prefs.edit().putBoolean("IS_HTTP_DIRECT", false).apply();
                        app_prefs.edit().putBoolean("PAYLOAD_AFTER_TLS", true).apply();
                    }
                    payload_layout.setVisibility(View.VISIBLE);
                    servidor_proxy_layout.setVisibility(View.GONE);
                } else {
                    if (!SecondVPN.getIsCustomFileIsLocked()){
                       // app_prefs.edit().putBoolean("IS_HTTP_DIRECT", false).apply();
                        app_prefs.edit().putBoolean("PAYLOAD_AFTER_TLS", false).apply();
                    }
                    payload_layout.setVisibility(View.GONE);
                    servidor_proxy_layout.setVisibility(View.GONE);
                }
            }

        if (!intentToImport){
           savePreferences();
        }


    }
    @Override
    protected void onPause()
    {
        savePreferences();
        //Appodeal.destroy(Appodeal.BANNER_VIEW | Appodeal.INTERSTITIAL);
        super.onPause();
    }


    @Override
    protected void onStop() {
        if (!duringisLoad){

        }
        super.onStop();
    }


    @Override
    protected void onResume()
    {

        //ccb();
        if (!intentToImport){
            savePreferences();
           // updateUIAfterImport();

        }else{
            intentToImport = false;
        }

        //if (SecondVPN.getCurrentVpnStatus().equals("CONECTADO")){
        if (TunnelManager.isServiceRunning){
            /*if (mInterstitialAd == null){
                setup_ad();
            }*/

            if (mInterstitialAd == null){
                setup_ad();
            }

        }

        super.onResume();
    }



    @Override
    protected void onDestroy()
    {
        savePreferences();
        mAdView.destroy();
        super.onDestroy();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Menu Itens
        switch (item.getItemId()) {
            case R.id.import_file:
                intentToImport = true;
                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else{
                    Intent import_file = new Intent(this, ImportConfig.class);
                    this.startActivity(import_file);
                }

            break;

            case R.id.clearlogs:
                AppLogManager.clearLog();
                //AppLogManager.addLog(getString(R.string.cleared_logs));
                break;
            case R.id.clearconfig:
                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else{
                   // AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialogTheme);
                    builder.setTitle(getString(R.string.reset_app_config_title));
                    builder.setCancelable(true)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    SecondVPN.setDefaultPrefs();
                                    clear_textviews();
                                    updateUIAfterImport();
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();

                }
                break;
            case R.id.forcestop:
                android.os.Process.killProcess(android.os.Process.myPid());

                break;
            case R.id.cleardata:

                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else{
                   // AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialogTheme);
                    builder.setTitle(getString(R.string.reset_app_title));
                    builder.setMessage(getString(R.string.reset_app_txt))
                            .setCancelable(true)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    try{
                                        String packageName = getApplicationContext().getPackageName();
                                        Runtime runtime = Runtime.getRuntime();
                                        runtime.exec("pm clear "+packageName);
                                    } catch (Exception e) {
                                        Toast.makeText(MainActivity.this, getString(R.string.operation_failed), Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }



                break;

            case R.id.export_file:
                savePreferences();
                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else if (!checkConfig(true)){
                    if(exportWithoutLogin){
                        if (!SecondVPN.getIsCustomFileIsLocked()){
                            //ATIVIDADE EXPORTAR
                            Intent export_file = new Intent(this, ExportConfig.class);
                            this.startActivity(export_file);
                        }else{
                            Toast.makeText(this,this.getString(R.string.block_by_config_author),Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(this,this.getString(R.string.empty_configs_export),Toast.LENGTH_LONG).show();
                    }
                }else if (SecondVPN.getIsCustomFileLockSettings()){
                    Toast.makeText(this,this.getString(R.string.block_by_config_author),Toast.LENGTH_LONG).show();
                }else{
                    if (!SecondVPN.getIsCustomFileIsLocked()){
                        //ATIVIDADE EXPORTAR
                        Intent export_file = new Intent(this, ExportConfig.class);
                        this.startActivity(export_file);
                    }else{
                        Toast.makeText(this,this.getString(R.string.block_by_config_author),Toast.LENGTH_LONG).show();
                    }
                }
            break;

            case R.id.settings:
                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else if (SecondVPN.getIsCustomFileLockSettings()){
                    Toast.makeText(this,this.getString(R.string.block_by_config_author),Toast.LENGTH_LONG).show();

                }else{
                    Intent settings = new Intent(this, AppSettings.class);
                    this.startActivity(settings);
                }
            break;

            case R.id.minimize:
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
             break;

            case R.id.exit_app:
                finishAffinity();
            break;

        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        {
            switch (v.getId()) {
                case R.id.activity_StartConnection:
                    setStartOrStopVPN(this);
                    break;

                case R.id.logs:
                    showLog();
                    break;

            }

        }
    }

    private void setStartOrStopVPN(MainActivity mainActivity) {
        if (!start_button.getText().toString().equals(this.getString(R.string.stop))) {
            //SET PREFERENCES
            savePreferences();
            startVPNService();
            Log.d("MainActivity","Start VPN Tunnel");
        }else{
            stopVPNService();
            Log.d("MainActivity","Stop VPN Tunnel");
        }
    }

    private void savePreferences(){
        if (SecondVPN.getIsCustomFileIsLocked()){
            if (!SecondVPN.isLockLoginEdit()){
                app_prefs.edit().putString("SSH_AUTH_DATA",user_and_pass.getText().toString()).apply();
            }
        }
        if (!SecondVPN.getIsCustomFileIsLocked()){
            //SALVA USUARIO E SENHA
            app_prefs.edit().putString("SSH_AUTH_DATA",user_and_pass.getText().toString()).apply();
            //SALVA POR MODO DE CONEXAO
            if (modo_http.isChecked()){
                if (direct_mode.isChecked()){
                    app_prefs.edit().putString("SSH_SERVER_DOMAIN",servidor_ssh.getText().toString()).apply();
                    app_prefs.edit().putString("PAYLOAD_KEY",custom_payload.getText().toString()).apply();

                }else{
                    app_prefs.edit().putString("SSH_SERVER_DOMAIN",servidor_ssh.getText().toString()).apply();
                    app_prefs.edit().putString("PAYLOAD_KEY",custom_payload.getText().toString()).apply();
                    app_prefs.edit().putString("PROXY_IP_DOMAIN",servidor_proxy.getText().toString()).apply();
                }

                //}else if (SecondVPN.getConnectionMode().equals("MODO_HTTPS")){
            }
            if (modo_https.isChecked()){
                if (payload_after_tls.isChecked()){
                    app_prefs.edit().putString("SSH_SERVER_DOMAIN",servidor_ssh.getText().toString()).apply();
                    app_prefs.edit().putString("PAYLOAD_KEY",custom_payload.getText().toString()).apply();
                    app_prefs.edit().putString("CUSTOM_SNI",custom_sni.getText().toString()).apply();

                }else{
                    app_prefs.edit().putString("SSH_SERVER_DOMAIN",servidor_ssh.getText().toString()).apply();
                    app_prefs.edit().putString("CUSTOM_SNI",custom_sni.getText().toString()).apply();

                }
            }
        }
    }

    public static void disableInterfaceOnConnect(){
        try{
            if (servidor_ssh_layout.getVisibility() == View.VISIBLE) {
                servidor_ssh.setEnabled(false);
            }
            if (servidor_proxy_layout.getVisibility() == View.VISIBLE) {
                servidor_proxy.setEnabled(false);
            }
            if (user_and_pass_layout.getVisibility() == View.VISIBLE) {
                user_and_pass.setEnabled(false);
            }
            if (payload_layout.getVisibility() == View.VISIBLE) {
                custom_payload.setEnabled(false);
            }
            if (direct_mode.getVisibility() == View.VISIBLE) {
                direct_mode.setEnabled(false);
            }
            if (payload_after_tls.getVisibility() == View.VISIBLE) {
                payload_after_tls.setEnabled(false);
            }
            if (custom_sni.getVisibility() == View.VISIBLE) {
                custom_sni.setEnabled(false);
            }
            if (modo_http.getVisibility() == View.VISIBLE) {
                modo_http.setEnabled(false);
            }
            if (modo_https.getVisibility() == View.VISIBLE) {
                modo_https.setEnabled(false);
            }
            if (modo_https.getVisibility() == View.VISIBLE) {
                modo_https.setEnabled(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void enableInterfaceOnConnect(){
        try{
            if (SecondVPN.isLockLoginEdit()){
                user_and_pass.setEnabled(false);
            }else{
                user_and_pass.setEnabled(true);
            }
            if (!SecondVPN.getIsCustomFileIsLocked()){
                if (servidor_ssh_layout.getVisibility() == View.VISIBLE) {
                    servidor_ssh.setEnabled(true);
                }
                if (servidor_proxy_layout.getVisibility() == View.VISIBLE) {
                    servidor_proxy.setEnabled(true);
                }
                if (user_and_pass_layout.getVisibility() == View.VISIBLE) {
                    user_and_pass.setEnabled(true);
                }
                if (payload_layout.getVisibility() == View.VISIBLE) {
                    custom_payload.setEnabled(true);
                }
                if (direct_mode.getVisibility() == View.VISIBLE) {
                    direct_mode.setEnabled(true);
                }
                if (payload_after_tls.getVisibility() == View.VISIBLE) {
                    payload_after_tls.setEnabled(true);
                }
                if (custom_sni.getVisibility() == View.VISIBLE) {
                    custom_sni.setEnabled(true);
                }
                if (modo_http.getVisibility() == View.VISIBLE) {
                    modo_http.setEnabled(true);
                }
                if (modo_https.getVisibility() == View.VISIBLE) {
                    modo_https.setEnabled(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Called when a drawer's position changes.
     *
     * @param drawerView  The child view that was moved
     * @param slideOffset The new offset of this drawer within its range, from 0-1
     */
    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    /**
     * Called when a drawer has settled in a completely open state.
     * The drawer is interactive at this point.
     *
     * @param drawerView Drawer view that is now open
     */
    @Override
    public void onDrawerOpened(@NonNull View drawerView) {

    }

    /**
     * Called when a drawer has settled in a completely closed state.
     *
     * @param drawerView Drawer view that is now closed
     */
    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        if (drawerView.getId() == R.id.activity_mainLogsDrawerLinear) {
            app_toolbar.getMenu().clear();
            getMenuInflater().inflate(R.menu.main_menu, app_toolbar.getMenu());
        }
    }

    /**
     * Called when the drawer motion state changes. The new state will
     * be one of ,  or .
     *
     * @param newState The new drawer motion state
     */
    @Override
    public void onDrawerStateChanged(int newState) {

    }
}
