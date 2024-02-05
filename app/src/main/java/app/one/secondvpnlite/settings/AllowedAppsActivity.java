package app.one.secondvpnlite.settings;

import android.annotation.SuppressLint;
import android.app.*;
import android.graphics.*;
import android.os.*;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.AppBarLayout;
import app.one.secondvpnlite.R;


public class AllowedAppsActivity extends AppCompatActivity {
	//private CenteredToolBar toolbar;

	private ToggleButton toggle;
	private Toolbar tb;

	@SuppressLint("ResourceType")
	@Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
		//setTheme(R.style.AppTheme_Dark);

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(1);
		ll.setId(1);
		//ll.setPadding(40,0,40,0);
		ll.setLayoutParams(layoutParams);
		AppBarLayout abl = new AppBarLayout(this);
		tb = new Toolbar(this);
		tb.setTitle(R.string.title_filter_apps);
		tb.setTitleTextColor(Color.WHITE);
		tb.setBackgroundColor(Color.parseColor(getString(R.color.primary_color_variant)));
		tb.setPopupTheme(R.style.Theme_SecondVPNLite);
		abl.addView(tb);
		ll.addView(abl);
		FrameLayout fl = new FrameLayout(this);
		ll.addView(fl);
		setContentView(ll);
		setSupportActionBar(tb);
		//getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		FragmentTransaction beginTransaction = getFragmentManager().beginTransaction();
		beginTransaction.replace(ll.getId(), new AllowedAppFragment());
		beginTransaction.commit();
    }
	
}
