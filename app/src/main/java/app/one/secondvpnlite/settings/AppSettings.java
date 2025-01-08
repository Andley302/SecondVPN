package app.one.secondvpnlite.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import app.one.secondvpnlite.MainActivity;
import app.one.secondvpnlite.R;
import app.one.secondvpnlite.SecondVPN;

public class AppSettings extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
    public static SharedPreferences app_prefs;

    private String TAG = "AppSettings";
    
    private Toolbar settings_toolbar;
    private SwitchCompat enable_udp;
    private SwitchCompat enable_dns;
    private SwitchCompat enable_tethering;
    //private SwitchCompat enable_tethering_root;
    private SwitchCompat enable_wakelock;
    private SwitchCompat show_logs;
    //private SwitchCompat enable_notifications;
    private SwitchCompat enable_ssh_compress;
    private SwitchCompat disable_tcp_delay;

    private EditText type_udp;
    private EditText type_dns_1;
    private EditText type_dns_2;
    private TextView tls_version;
    private TextView filter_apps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_appsettings);
        settings_toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(settings_toolbar);

        app_prefs = getSharedPreferences(SecondVPN.PREFS_GERAL, Context.MODE_PRIVATE);

        enable_udp = (SwitchCompat) findViewById(R.id.enable_udp);
        enable_dns = (SwitchCompat) findViewById(R.id.enable_dns);
        enable_tethering = (SwitchCompat) findViewById(R.id.enable_thetering);
        //enable_tethering_root = (SwitchCompat) findViewById(R.id.enable_thetering_root);
        enable_wakelock = (SwitchCompat) findViewById(R.id.enable_wakelock);
        show_logs = (SwitchCompat) findViewById(R.id.show_logs) ;
        //enable_notifications = (SwitchCompat) findViewById(R.id.enable_notification);
        enable_ssh_compress = (SwitchCompat) findViewById(R.id.enable_sshcompress);
        disable_tcp_delay = (SwitchCompat) findViewById(R.id.disable_tcp_delay);

        tls_version = (TextView) findViewById(R.id.set_tls_version);
        filter_apps = (TextView) findViewById(R.id.filter_apps_configure);
        type_udp = (EditText) findViewById(R.id.input_udp);
        type_dns_1 = (EditText) findViewById(R.id.input_dns_1);
        type_dns_2 = (EditText) findViewById(R.id.input_dns_2);


        if (SecondVPN.isEnableCustomUDP()){
            enable_udp.setChecked(true);
            type_udp.setVisibility(View.VISIBLE);
            type_udp.setText(SecondVPN.getUDPResolver());
        }else{
            enable_udp.setChecked(false);
            type_udp.setVisibility(View.GONE);
        }

        if (SecondVPN.isEnableCustomDNS()){
            enable_dns.setChecked(true);
            type_dns_1.setVisibility(View.VISIBLE);
            type_dns_2.setVisibility(View.VISIBLE);
            type_dns_1.setText(SecondVPN.customDNS1());
            type_dns_2.setText(SecondVPN.customDNS2());
        }else{
            enable_dns.setChecked(false);
            type_dns_1.setVisibility(View.GONE);
            type_dns_2.setVisibility(View.GONE);
        }

        if (SecondVPN.isEnableTethering()){
            enable_tethering.setChecked(true);
        }else{
            enable_tethering.setChecked(false);
        }

        if (SecondVPN.isEnableTetheringRoot()){
           // enable_tethering_root.setChecked(true);
        }else{
           // enable_tethering_root.setChecked(false);
        }

        if (SecondVPN.isEnableWakeLock()){
            enable_wakelock.setChecked(true);
        }else{
            enable_wakelock.setChecked(false);
        }

        if (SecondVPN.isShowLogs()){
            show_logs.setChecked(true);
        }else{
            show_logs.setChecked(false);
        }

        if (SecondVPN.isEnableNotification()){
           // enable_notifications.setChecked(true);
        }else{
           // enable_notifications.setChecked(false);
        }

        if (SecondVPN.isEnableSSHCompress()){
            enable_ssh_compress.setChecked(true);
        }else{
            enable_ssh_compress.setChecked(false);
        }

        if (SecondVPN.isEnableNoTCPDelay()){
            disable_tcp_delay.setChecked(true);
        }else{
            disable_tcp_delay.setChecked(false);
        }

        enable_udp.setOnCheckedChangeListener(this);
        enable_dns.setOnCheckedChangeListener(this);
        enable_tethering.setOnCheckedChangeListener(this);
        //enable_tethering_root.setOnCheckedChangeListener(this);
        enable_wakelock.setOnCheckedChangeListener(this);
        show_logs.setOnCheckedChangeListener(this);
       // enable_notifications.setOnCheckedChangeListener(this);
        enable_ssh_compress.setOnCheckedChangeListener(this);
        disable_tcp_delay.setOnCheckedChangeListener(this);


    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.set_tls_version:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.tls_version_title);

                // add a list
                String[] TH = {"Auto","TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
                builder.setItems(TH, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Auto
                                //Toasty.normal(getContext(), "Auto", Toast.LENGTH_SHORT).show();
                                app_prefs.edit().putString("TLS_VERSION","auto").apply();
                                break;
                            case 1: // 1
                                app_prefs.edit().putString("TLS_VERSION","TLSv1").apply();
                                break;
                            case 2: // 1.1
                                app_prefs.edit().putString("TLS_VERSION","TLSv1.1").apply();
                                break;
                            case 3: // 1.2
                                app_prefs.edit().putString("TLS_VERSION","TLSv1.2").apply();
                                break;
                            case 4: // 1.3
                                app_prefs.edit().putString("TLS_VERSION","TLSv1.3").apply();
                                break;
							/*case 5: // v3
								editor.putString(Settings.TLS_VERSION_KEY, "SSLv3").apply();
								editor.apply();
								break;*/
                        }
                    }
                });
                // create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
                break;

            case R.id.filter_apps_configure:
                try{
                    Intent intent = new Intent(this, AllowedAppsActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.d("Settings","Falhou ao iniciar allowed apps " + e);
                    e.printStackTrace();
                }

                break;
        }

    }

    @Override
    public void onBackPressed()
    {
        savePreferences(true);
        MainActivity.updateUIAfterImport();
        Intent main_activity = new Intent(this, MainActivity.class);
        main_activity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        this.startActivity(main_activity);
        super.onBackPressed();
    }

    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param buttonView The compound button view whose state has changed.
     * @param isChecked  The new checked state of buttonView.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!enable_udp.isChecked()){
            enable_dns.setChecked(true);

        }else if (enable_udp.isChecked() && type_udp.getText().toString().isEmpty()){
            type_udp.setText("127.0.0.1:7300");
        }

        if (enable_dns.isChecked() && type_dns_1.getText().toString().isEmpty()){
            type_dns_1.setText("1.1.1.1");
        }
        if (enable_dns.isChecked() && type_dns_2.getText().toString().isEmpty()){
            type_dns_2.setText("1.0.0.1");
        }
       savePreferences(false);
    }
    private void savePreferences(boolean isBackToHome){
        if (enable_udp.isChecked()){
            String udp_text = type_udp.getText().toString();
            enable_udp.setChecked(true);
            type_udp.setVisibility(View.VISIBLE);
            if (!isBackToHome){
                if (!udp_text.isEmpty()){
                    app_prefs.edit().putString("UDP_ADDR",type_udp.getText().toString()).apply();
                    type_udp.setText(SecondVPN.getUDPResolver());
                }

            }else{
                if (udp_text.isEmpty()){
                    app_prefs.edit().putBoolean("IS_ENABLE_UDP",false).apply();
                }else{
                    app_prefs.edit().putBoolean("IS_ENABLE_UDP",true).apply();
                    app_prefs.edit().putString("UDP_ADDR",udp_text).apply();
                }

            }


        }else{
            if (!isBackToHome){
                enable_udp.setChecked(false);
                type_udp.setVisibility(View.GONE);
            }else{
                app_prefs.edit().putBoolean("IS_ENABLE_UDP",false).apply();
            }

        }

        if (enable_dns.isChecked()){
            String dns_1_text = type_dns_1.getText().toString();
            String dns_2_text = type_dns_2.getText().toString();
            enable_dns.setChecked(true);
            type_dns_1.setVisibility(View.VISIBLE);
            type_dns_2.setVisibility(View.VISIBLE);

            if (!isBackToHome) {
                if (!dns_1_text.isEmpty()){
                    app_prefs.edit().putString("CUSTOM_DNS_1",dns_1_text).apply();
                    type_dns_1.setText(SecondVPN.customDNS1());

                }else if (!dns_2_text.isEmpty()){
                   app_prefs.edit().putString("CUSTOM_DNS_2",dns_2_text).apply();
                    type_dns_2.setText(SecondVPN.customDNS2());
                }

            }else{
                if (dns_1_text.isEmpty() && dns_2_text.isEmpty()){
                    app_prefs.edit().putBoolean("IS_ENABLE_DNS",false).apply();
                }else if (dns_2_text.isEmpty()){
                    app_prefs.edit().putBoolean("IS_ENABLE_DNS",false).apply();
                }else{
                    app_prefs.edit().putBoolean("IS_ENABLE_DNS",true).apply();
                    app_prefs.edit().putString("CUSTOM_DNS_1",dns_1_text).apply();
                    app_prefs.edit().putString("CUSTOM_DNS_2",dns_2_text).apply();
                }

            }


            //type_dns_1.setText(SecondVPN.customDNS1());
            //type_dns_2.setText(SecondVPN.customDNS1());
        }else{
            if (!isBackToHome){
                enable_dns.setChecked(false);
                type_dns_1.setVisibility(View.GONE);
                type_dns_2.setVisibility(View.GONE);

            }else{
                app_prefs.edit().putBoolean("IS_ENABLE_DNS",false).apply();
            }

        }

        if (enable_tethering.isChecked()){
            enable_tethering.setChecked(true);
            app_prefs.edit().putBoolean("IS_ENABLE_TETHERING",true).apply();
        }else{
            enable_tethering.setChecked(false);
            app_prefs.edit().putBoolean("IS_ENABLE_TETHERING",false).apply();
        }

       /* if (enable_tethering_root.isChecked()){
           // app_prefs.edit().putBoolean("IS_ENABLE_TETHERING_ROOT",true).apply();
           // enable_tethering_root.setChecked(true);
        }else{
           // enable_tethering_root.setChecked(false);
           // app_prefs.edit().putBoolean("IS_ENABLE_TETHERING_ROOT",false).apply();
        }*/

        if (enable_wakelock.isChecked()){
            enable_wakelock.setChecked(true);
            app_prefs.edit().putBoolean("IS_ENABLE_WAKELOCK",true).apply();
        }else{
            enable_wakelock.setChecked(false);
            app_prefs.edit().putBoolean("IS_ENABLE_WAKELOCK",false).apply();
        }

        if (show_logs.isChecked()){
            show_logs.setChecked(true);
            app_prefs.edit().putBoolean("IS_SHOW_LOGS",true).apply();
        }else{
            show_logs.setChecked(false);
            app_prefs.edit().putBoolean("IS_SHOW_LOGS",false).apply();
        }

       /* if (enable_notifications.isChecked()){
            app_prefs.edit().putBoolean("IS_ENABLE_NOTIFICATION",true).apply();
            enable_notifications.setChecked(true);
        }else{
            app_prefs.edit().putBoolean("IS_ENABLE_NOTIFICATION",false).apply();
            enable_notifications.setChecked(false);
        }*/

        if (enable_ssh_compress.isChecked()){
            enable_ssh_compress.setChecked(true);
            app_prefs.edit().putBoolean("IS_ENABLE_SSH_COMPRESS",true).apply();
        }else{
            enable_ssh_compress.setChecked(false);
            app_prefs.edit().putBoolean("IS_ENABLE_SSH_COMPRESS",false).apply();
        }

        if (disable_tcp_delay.isChecked()){
            disable_tcp_delay.setChecked(true);
            app_prefs.edit().putBoolean("IS_ENABLE_NO_TCP_DELAY",true).apply();
        }else{
            disable_tcp_delay.setChecked(false);
            app_prefs.edit().putBoolean("IS_ENABLE_NO_TCP_DELAY",false).apply();
        }
    }

    @Override
    protected void onPause()
    {
        savePreferences(true);
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        savePreferences(true);
        super.onDestroy();
    }

}
