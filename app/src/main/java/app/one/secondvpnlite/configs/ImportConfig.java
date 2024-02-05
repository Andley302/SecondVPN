package app.one.secondvpnlite.configs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import app.one.secondvpnlite.MainActivity;
import app.one.secondvpnlite.R;
import app.one.secondvpnlite.SecondVPN;
import app.one.secondvpnlite.security.AppSecurityManager;
import app.one.secondvpnlite.service.TunnelManager;


public class ImportConfig extends AppCompatActivity {

    public static SharedPreferences app_prefs;
    private boolean isImportFromExternal;
    public static SharedPreferences start_msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty);

        app_prefs = getSharedPreferences(SecondVPN.PREFS_GERAL, Context.MODE_PRIVATE);

        // Get the intent that started this activity
        Intent intent = getIntent();
        String scheme = intent.getScheme();

        //if (!SecondVPN.getCurrentVpnStatus().equals("CONECTADO")){
        if (!TunnelManager.isServiceRunning){
            // Figure out what to do based on the intent type
            if (scheme != null && (scheme.equals("file") || scheme.equals("content"))) {
                isImportFromExternal = true;

                Uri data = intent.getData();

               /* File file = new File(data.getPath());
                String file_extensao = getExtension(file);
                if (file_extensao != null && file_extensao.equals("vpnlite")) {

                    try {
                        ReadConfigFile(data);
                        // importarConfigInputFile(getContentResolver()
                        //  .openInputStream(data));
                    } catch(Exception e) {
                        Toast.makeText(this, R.string.error_file_config_incompatible,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }

                }
                else {
                    Toast.makeText(this, R.string.error_file_config_incompatible,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }*/

                try {
                    ReadConfigFile(data);
                } catch(Exception e) {
                    Toast.makeText(this, R.string.error_file_config_incompatible,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }

                finish();
            }else{
                isImportFromExternal = false;
                Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/Download");
                importConfigFileFromAPI(uri);
            }
        }else{
            Toast.makeText(this, R.string.only_disconnected,
                    Toast.LENGTH_SHORT).show();
            finish();
        }

    }


    private static final int PICK_CONFIG_FILE = 2;
    private void importConfigFileFromAPI(Uri pickerInitialUri) {
        try{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
            this.startActivityForResult(intent, PICK_CONFIG_FILE);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_file_config_incompatible,
                    Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }

    }

    public String getExtension(File file) {
        String filename = file.getAbsolutePath();

        if (filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }

        return "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONFIG_FILE) {
            // Get the Uri of the selected file
            try{
                Uri uri = data.getData();
                ReadConfigFile(uri);
            } catch (Exception e) {
                finish();
                e.printStackTrace();
            }

        }

    }

    public void ReadConfigFile(Uri uri) {
        BufferedReader br;
        FileOutputStream os;
        try {
            br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            //WHAT TODO ? Is this creates new file with
            //the name NewFileName on internal app storage?
            os = openFileOutput("newFileName", Context.MODE_PRIVATE);
            String line = null;
            while ((line = br.readLine()) != null) {
                os.write(line.getBytes());
                checkAndImportConfig(line);
            }
            br.close();
            os.close();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.import_file_error), Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }
    }
    //CRITPGRAFA CONFIG AQUI E MANDA PRO EXPORTCONFIGFILE
    public static String k = AppSecurityManager.k1 + AppSecurityManager.k2 + AppSecurityManager.k3  + AppSecurityManager.k4 + AppSecurityManager.k5;
    public static String i = AppSecurityManager.iv1 + AppSecurityManager.iv2 + AppSecurityManager.ivf;
    private void checkAndImportConfig(String config){

        start_msg = getSharedPreferences(SecondVPN.FIRST_START, Context.MODE_PRIVATE);

        /*boolean showFirstTime = start_msg.getBoolean("default_config", true);
        if (showFirstTime) {
        //USER AND PASS
                start_msg.edit().putString("SSH_AUTH_DATA",jcfg.getString("userandpass")).apply();
            Toast.makeText(this, getString(R.string.open_app_firts), Toast.LENGTH_SHORT).show();
            finish();
        }else{*/
            //DECRIPTA A CONFIG AQUI

            String final_config = "";
            try{
                final_config = AppSecurityManager.decryptStrAndFromBase64(i , k,  config);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try{
                //OBTEM CONFIG DESCRIPTOGRAFADA
                JSONObject jcfg = new JSONObject(final_config);

                //CONFIG DEFAULT
                SecondVPN.setDefaultPrefs();

                Log.d("Import", String.valueOf(jcfg));

                if (jcfg.getInt("appVersion") >= 22 /*BuildConfig.VERSION_CODE -- CRIPTOGRAFAR EM BYTE ATUAL 22*/ ){
                    //DATA VALIDADE ARQUIVO
                    app_prefs.edit().putLong("VALIDADE_CONFIG",jcfg.getLong("ConfigValidityDate")).apply();

                    //MODO CONEXAO
                    app_prefs.edit().putString("CONNECTION_MODE",jcfg.getString("ConnectionMode")).apply();

                    //HTTP DIRECT
                    app_prefs.edit().putBoolean("IS_HTTP_DIRECT",jcfg.getBoolean("isHTTPDirect")).apply();

                    //PAYLOAD AFTER ATLS
                    app_prefs.edit().putBoolean("PAYLOAD_AFTER_TLS",jcfg.getBoolean("isPayloadAfterTLS")).apply();

                    //IS LOCK CONFIG
                    //app_prefs.edit().putBoolean("IS_CUSTOM_FILE_IS_LOCKED",jcfg.getBoolean("isLockedConfig").apply();
                    app_prefs.edit().putBoolean("IS_CUSTOM_FILE_LOCKED",jcfg.getBoolean("isLockConfig")).apply();

                    //IS LOCK SETTING
                    app_prefs.edit().putBoolean("IS_CUSTOM_FILE_LOCK_SETTINGS",jcfg.getBoolean("isLockAppSettings")).apply();

                    //IS ENABLE UDP
                    app_prefs.edit().putBoolean("IS_ENABLE_UDP",jcfg.getBoolean("isEnableUdp")).apply();

                    //UDP RESOLVER
                    app_prefs.edit().putString("UDP_ADDR",jcfg.getString("UDPAddr")).apply();

                    //IS ENABLE DNS
                    app_prefs.edit().putBoolean("IS_ENABLE_DNS",jcfg.getBoolean("isEnableDNS")).apply();

                    //DNS1
                    app_prefs.edit().putString("CUSTOM_DNS_1",jcfg.getString("DNS1")).apply();

                    //DNS2
                    app_prefs.edit().putString("CUSTOM_DNS_2",jcfg.getString("DNS2")).apply();

                    //CONFIG MSG
                    app_prefs.edit().putString("CONFIG_MSG",jcfg.getString("ConfigMsg")).apply();

                    //IS MOBILE DATA ONLY
                    app_prefs.edit().putBoolean("IS_CONFIG_ONLY_MOBILE_DATA",jcfg.getBoolean("onlyMobileData")).apply();

                    //TLS VERSION
                    app_prefs.edit().putString("TLS_VERSION",jcfg.getString("TLSVersion")).apply();

                    //PAYLOAD
                    app_prefs.edit().putString("PAYLOAD_KEY",jcfg.getString("CurrentPayload")).apply();

                    //SERVIDOR SSH
                    app_prefs.edit().putString("SSH_SERVER_DOMAIN",jcfg.getString("ServerSSH")).apply();

                    //SERVIDOR PROXY
                    app_prefs.edit().putString("PROXY_IP_DOMAIN",jcfg.getString("ServerProxy")).apply();

                    //SERVIDOR SNI
                    app_prefs.edit().putString("CUSTOM_SNI",jcfg.getString("ServerSNI")).apply();

                    //LOCK LOGIN EDIT
                    app_prefs.edit().putBoolean("IS_LOCK_LOGIN_EDIT",jcfg.getBoolean("LockEditLogin")).apply();

                    //USER AND PASS
                    app_prefs.edit().putString("SSH_AUTH_DATA",jcfg.getString("ConfigAuthData")).apply();

                    if (isImportFromExternal){
                        Toast.makeText(this, getString(R.string.import_sucess), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("IS_IMPORT",true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }else{
                        Toast.makeText(this, getString(R.string.import_sucess), Toast.LENGTH_SHORT).show();
                        //Thread.sleep(5000);
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("IS_IMPORT",true);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();

                    }
                }else{
                    if (isImportFromExternal){
                        Toast.makeText(this, getString(R.string.file_config_incompatible), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("IS_IMPORT",true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }else{
                        Toast.makeText(this, getString(R.string.file_config_incompatible), Toast.LENGTH_SHORT).show();
                        //Thread.sleep(5000);
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("IS_IMPORT",true);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();

                    }
                }



            } catch (Exception e) {
                e.printStackTrace();
                //Toast.makeText(this, getString(R.string.import_file_error), Toast.LENGTH_SHORT).show();
                Toast.makeText(this, getString(R.string.file_config_incompatible), Toast.LENGTH_SHORT).show();
                //MainActivity.updateUIAfterImport();
                finish();
            }




        //}

    }


}
