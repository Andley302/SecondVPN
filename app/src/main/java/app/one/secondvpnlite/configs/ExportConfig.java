package app.one.secondvpnlite.configs;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Calendar;

import app.one.secondvpnlite.R;
import app.one.secondvpnlite.SecondVPN;
import app.one.secondvpnlite.security.AppSecurityManager;

public class ExportConfig extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
    public static SharedPreferences app_prefs;
    private String TAG = "Export Config";


    private Toolbar export_toolbar;
    private Button export_config_button;
    private EditText config_name;
    private EditText config_msg;
    private SwitchCompat lock_config_from_edit;
    private SwitchCompat lock_settings_app;
    private SwitchCompat only_mobile_data;
    private SwitchCompat validate_date;
    private SwitchCompat lock_login;
    private long mValidade = 0;
    private String configToExportOnPermisson;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.export_config);
        export_toolbar = findViewById(R.id.export_toolbar);
        setSupportActionBar(export_toolbar);

        app_prefs = getSharedPreferences(SecondVPN.PREFS_GERAL, Context.MODE_PRIVATE);

        config_name = (EditText) findViewById(R.id.config_file_name);
        config_msg = (EditText) findViewById(R.id.config_msg);

        lock_config_from_edit = (SwitchCompat) findViewById(R.id.lock_config);
        lock_settings_app = (SwitchCompat) findViewById(R.id.lock_config_settings);
        only_mobile_data = (SwitchCompat) findViewById(R.id.lock_only_mobile_data);
        validate_date = (SwitchCompat) findViewById(R.id.lock_config_validate);
        lock_login = (SwitchCompat) findViewById(R.id.lock_login_edit);
        export_config_button = (Button) findViewById(R.id.export_config_button);

        lock_config_from_edit.setOnCheckedChangeListener(this);
        lock_settings_app.setOnCheckedChangeListener(this);
        only_mobile_data.setOnCheckedChangeListener(this);
        validate_date.setOnCheckedChangeListener(this);
        export_config_button.setOnClickListener(this);

        }
    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.export_config_button:
                if (config_name.getText().toString().isEmpty()){
                    Toast.makeText(this, getString(R.string.filename_empty), Toast.LENGTH_SHORT).show();
                }else if(lock_login.isChecked() && SecondVPN.getUsuarioAndPass().isEmpty()){
                    Toast.makeText(this, getString(R.string.empty_login), Toast.LENGTH_SHORT).show();
                }else{
                    if (validate_date.isChecked()){
                        validate_date = null;
                    }else{
                        mValidade = 0;
                    }


                    JSONObject config_str = new JSONObject();
                    String k = AppSecurityManager.k1 + AppSecurityManager.k2 + AppSecurityManager.k3  + AppSecurityManager.k4 + AppSecurityManager.k5;
                    String i = AppSecurityManager.iv1 + AppSecurityManager.iv2 + AppSecurityManager.ivf;
                    try{
                        config_str.put("appVersion", 22); /*Integer.parseInt(String.valueOf(BuildConfig.VERSION_CODE))*/
                        config_str.put("ConfigValidityDate", mValidade);
                        config_str.put("ConnectionMode", SecondVPN.getConnectionMode());
                        config_str.put("isHTTPDirect", SecondVPN.isHTTPDirect());
                        config_str.put("isPayloadAfterTLS", SecondVPN.isPayloadAfterTLS());
                        config_str.put("isLockConfig", lock_config_from_edit.isChecked());
                        config_str.put("isLockAppSettings", lock_settings_app.isChecked());
                        config_str.put("isEnableUdp", SecondVPN.isEnableCustomUDP());
                        config_str.put("UDPAddr", SecondVPN.getUDPResolver());
                        config_str.put("isEnableDNS", SecondVPN.isEnableCustomDNS());
                        config_str.put("DNS1", SecondVPN.customDNS1());
                        config_str.put("DNS2", SecondVPN.customDNS2());
                        config_str.put("ConfigMsg", config_msg.getText().toString());
                        config_str.put("onlyMobileData", only_mobile_data.isChecked());
                        config_str.put("TLSVersion", SecondVPN.getTLSVersion());
                        config_str.put("CurrentPayload", SecondVPN.getPayloadKey());
                        config_str.put("ServerSSH", SecondVPN.getServidorSSHDomain());
                        config_str.put("ServerProxy", SecondVPN.getProxyIPDomain());
                        config_str.put("ServerSNI",SecondVPN.getSNI());
                        config_str.put("LockEditLogin", lock_login.isChecked());
                        config_str.put("ConfigAuthData", SecondVPN.getUsuarioAndPass());

                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                    String enc_config = "";
                    try{
                        enc_config = AppSecurityManager.encryptStrAndToBase64(i , k, config_str.toString());
                    } catch (Exception e) {
                        enc_config = null;
                        e.printStackTrace();
                    }

                    configToExportOnPermisson = enc_config.replace("\n","");
                    //createFile(toEnc);
                    Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/Download");
                    createFile(uri);
                    //Log.d(TAG,"Config is: " + config_result);

                }
        }
    }

    // Request code for creating a PDF document.
    private static final int CREATE_FILE = 1;

    private void createFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, config_name.getText().toString() + ".vpnlite");

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, CREATE_FILE);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                try {
                    Uri uri = data.getData();

                    OutputStream outputStream = getContentResolver().openOutputStream(uri);

                    outputStream.write(configToExportOnPermisson.getBytes());

                    outputStream.close(); // very important

                    //SUCESSO
                    Toast.makeText(this, getString(R.string.export_sucess), Toast.LENGTH_SHORT).show();
                    finish();
                } catch (IOException e) {
                    Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show();

                    e.printStackTrace();
                }
            }
        }else{
            Toast.makeText(this,getString(R.string.erro_save_file) , Toast.LENGTH_SHORT).show();
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

        if (validate_date.isChecked()){
          setValidadeDate();
        }else{
            mValidade = 0;
        }
    }



    private void setValidadeDate() {

        // Get Current Date
        Calendar c = Calendar.getInstance();
        final long time_hoje = c.getTimeInMillis();

        c.setTimeInMillis(time_hoje+(1000*60*60*24));

        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);

        mValidade = c.getTimeInMillis();

        final DatePickerDialog dialog = new DatePickerDialog(this,R.style.DATE_DIALOG_THEME,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker p1, int year, int monthOfYear, int dayOfMonth) {
                        Calendar c = Calendar.getInstance();
                        c.set(year, monthOfYear, dayOfMonth);

                        mValidade = c.getTimeInMillis();
                    }
                },
                mYear, mMonth, mDay);

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog2, int which) {
                        DateFormat df = DateFormat.getDateInstance();
                        DatePicker date = dialog.getDatePicker();

                        Calendar c = Calendar.getInstance();
                        c.set(date.getYear(), date.getMonth(), date.getDayOfMonth());

                        mValidade = c.getTimeInMillis();

                        if (mValidade < time_hoje) {
                            mValidade = 0;

                            Toast.makeText(getApplicationContext(), R.string.error_date_selected_invalid,
                                    Toast.LENGTH_SHORT).show();

                            if (validate_date != null)
                                validate_date .setChecked(false);
                        }
                        else {
                            long dias = ((mValidade-time_hoje)/1000/60/60/24);

                            if (validate_date  != null) {
                                validate_date .setVisibility(View.VISIBLE);
                                validate_date .setText(String.format("%s (%s)", dias, df.format(mValidade)));
                            }
                        }
                    }
                }
        );

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mValidade = 0;

                        if (validate_date  != null) {
                            validate_date .setChecked(false);
                        }
                    }
                }
        );

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface v1) {
                mValidade = 0;
                if (validate_date  != null) {
                    validate_date .setChecked(false);
                }
            }
        });

        dialog.show();
    }
}
