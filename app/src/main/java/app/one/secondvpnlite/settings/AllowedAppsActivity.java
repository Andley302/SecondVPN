package app.one.secondvpnlite.settings;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.os.*;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import java.util.List;

import app.one.secondvpnlite.R;


public class AllowedAppsActivity extends AppCompatActivity {
	private ImageView btnBack;
	private RecyclerView splitTunnelRV;
	private AppListAdapter appAdapter;
	private Button selectAllBtn, deselectAllBtn;
	private List<ResolveInfo> resolveInfoList;
	private PackageManager packageManager;
	private TextView toolbar_title;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_allowed_app);
		initializeViews();
		setAdapterPackageManager();
		setStatusBar(this,R.color.black);

	}

	private void setAdapterPackageManager() {
		appAdapter = new AppListAdapter(resolveInfoList, packageManager, AllowedAppsActivity.this);
		splitTunnelRV.setAdapter(appAdapter);

	}


	private void initializeViews() {
		splitTunnelRV = findViewById(R.id.splitRecyclerView);
		selectAllBtn = findViewById(R.id.btnSelectAll);
		deselectAllBtn = findViewById(R.id.btnDeselectAll);
		toolbar_title = findViewById(R.id.toolbar_title);
		btnBack = findViewById(R.id.btnBack);
		packageManager = getPackageManager();
		resolveInfoList = getInstalledApps(packageManager);
		toolbar_title.setVisibility(View.VISIBLE);
		toolbar_title.setText("Split Tunneling");
		btnBack.setVisibility(View.VISIBLE);

		btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

		selectAllBtn.setOnClickListener(v -> {
			appAdapter.selectAll();
			ShowToast("Blocked All Apps");
			appAdapter.notifyDataSetChanged();
		});

		deselectAllBtn.setOnClickListener(v -> {
			appAdapter.deselectAll();
			ShowToast("Unblocked All Apps");
			appAdapter.notifyDataSetChanged();
		});



	}

	private void ShowToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}


	private List<ResolveInfo> getInstalledApps(PackageManager packageManager) {
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		return packageManager.queryIntentActivities(intent, 0);
	}

	public static void setStatusBar(Activity activity, int color) {
		activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, color));
	}
}
