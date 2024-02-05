package app.one.secondvpnlite.settings;

/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import app.one.secondvpnlite.R;
import app.one.secondvpnlite.SecondVPN;

/**
 * Created by arne on 16.11.14.
 */
public class AllowedAppFragment extends Fragment implements AdapterView.OnItemClickListener, CompoundButton.OnCheckedChangeListener {
    private ListView mListView;
    //private VpnProfile mProfile;
    private TextView mDefaultAllowTextView;
    private PackageAdapter mListAdapter;
	public HashSet<String> mAllowedAppsVpn = new HashSet<>();
	private ProgressDialog packageLoadDialog;
	private boolean isDestroyed;
	//private SharedPreferences dsp;
    //private Settings mConfig;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppViewHolder avh = (AppViewHolder) view.getTag();
        avh.checkBox.toggle();
    }

    static class AppViewHolder {
        public ApplicationInfo mInfo;
        public View rootView;
        public TextView appName;
        public ImageView appIcon;
        //public TextView appSize;
        //public TextView disabled;
        public CompoundButton checkBox;

        static public AppViewHolder createOrRecycle(LayoutInflater inflater, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.layout_allowed_app_row, parent, false);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                AppViewHolder holder = new AppViewHolder();
                holder.rootView = convertView;
                holder.appName = (TextView) convertView.findViewById(R.id.openvpn_app_name);
                holder.appIcon = (ImageView) convertView.findViewById(R.id.openvpn_app_icon);
                //holder.appSize = (TextView) convertView.findViewById(R.id.openvpn_app_size);
                //holder.disabled = (TextView) convertView.findViewById(R.id.openvpn_app_disabled);
                holder.checkBox = (CompoundButton) convertView.findViewById(R.id.openvpn_app_selected);
                convertView.setTag(holder);


                return holder;
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                return (AppViewHolder) convertView.getTag();
            }
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String packageName = (String) buttonView.getTag();
        if (isChecked) {
            Log.d("openvpn", "adding to allowed apps " + packageName);
           mAllowedAppsVpn.add(packageName);
        } else {
            Log.d("openvpn", "removing from allowed apps" + packageName);
           mAllowedAppsVpn.remove(packageName);
        }
		SecondVPN.app_prefs.edit().putStringSet("mAllowedAppsVpn" , mAllowedAppsVpn).apply();
    }


    class PackageAdapter extends BaseAdapter {
        private final List<ApplicationInfo> mPackages;
        private final LayoutInflater mInflater;
        private final PackageManager mPm;

        PackageAdapter(Context c, List<ApplicationInfo> mPackages) {
			mInflater = LayoutInflater.from(c);
			this.mPackages = mPackages;
			mPm = c.getPackageManager();
        }

        @Override
        public int getCount() {
            return mPackages.size();
        }

        @Override
        public Object getItem(int position) {
            return mPackages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mPackages.get(position).packageName.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder viewHolder = AppViewHolder.createOrRecycle(mInflater, convertView ,parent);
            convertView = viewHolder.rootView;
            viewHolder.mInfo = mPackages.get(position);
            final ApplicationInfo mInfo = mPackages.get(position);


            CharSequence appName = mInfo.loadLabel(mPm);

            if (TextUtils.isEmpty(appName))
                appName = mInfo.packageName;
            viewHolder.appName.setText(appName);
            viewHolder.appIcon.setImageDrawable(mInfo.loadIcon(mPm));
            viewHolder.checkBox.setTag(mInfo.packageName);
            viewHolder.checkBox.setOnCheckedChangeListener(AllowedAppFragment.this);


            viewHolder.checkBox.setChecked(SecondVPN.app_prefs.getStringSet("mAllowedAppsVpn", mAllowedAppsVpn).contains(mInfo.packageName));
            return viewHolder.rootView;
        }
    }
	
	private void setupList(List<ApplicationInfo> AllPackages) {

        mListAdapter = new PackageAdapter(getActivity(), AllPackages);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(this);
	}
	
	private void showProgressDialog() {
        if (packageLoadDialog == null) {
            packageLoadDialog = new ProgressDialog(getActivity());
            packageLoadDialog.setIndeterminate(false);
            packageLoadDialog.setCancelable(false);
            packageLoadDialog.setInverseBackgroundForced(false);
            packageLoadDialog.setCanceledOnTouchOutside(false);
            packageLoadDialog.setMessage(this.getString(R.string.filter_apps_loading));
        }
        packageLoadDialog.show();
    }

    private void dismissProgressDialog() {
        if (packageLoadDialog != null && packageLoadDialog.isShowing()) {
            packageLoadDialog.dismiss();
        }
    }
	
	private List<ApplicationInfo> getInstalledApps(ApplicationLoader task) {
      
		PackageManager mPm = getActivity().getPackageManager();
		// mProfile = vp;
		List<ApplicationInfo> installedPackages = mPm.getInstalledApplications(PackageManager.GET_META_DATA);
		

		// Remove apps not using Internet

		int androidSystemUid = 0;
		ApplicationInfo system = null;
		List<ApplicationInfo> apps = new Vector<ApplicationInfo>();

		try {
			system = mPm.getApplicationInfo("android", PackageManager.GET_META_DATA);
			androidSystemUid = system.uid;
			apps.add(system);
		} catch (PackageManager.NameNotFoundException e) {
		}


		for (ApplicationInfo app : installedPackages) {

			if (mPm.checkPermission(android.Manifest.permission.INTERNET, app.packageName) == PackageManager.PERMISSION_GRANTED &&
				app.uid != androidSystemUid) {
					
				task.doProgress(this.getString(R.string.filter_apps_loading) + " (" + app.packageName + ")");
				apps.add(app);
			}
		}

		Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(mPm));
		
        return apps;
    }

    private boolean isSystemPackage(PackageInfo pkgInfo) {
        return ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

	private class ApplicationLoader extends AsyncTask<String, String, List<ApplicationInfo>> {

        @Override
        protected List<ApplicationInfo> doInBackground(String... params) {
            publishProgress("Retrieving installed application");
            return getInstalledApps(this);
        }

        @Override
        protected void onPostExecute(List<ApplicationInfo> AllPackages) {
            setupList(AllPackages);
            if (!isDestroyed) {
                dismissProgressDialog();
            }
        }

        public void doProgress(String value) {
            publishProgress(value);
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected void onProgressUpdate(String... text) {
            packageLoadDialog.setMessage(text[0]);
        }
    }
	
    @Override
    public void onResume() {
        super.onResume();
        changeDisallowText(SecondVPN.app_prefs.getBoolean("mAllowedAppsVpnAreDisallowed", true));
    }

	@Override
	public void onDestroy()
	{
		// TODO: Implement this method
		super.onDestroy();
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_allowed_app, container, false);

        mDefaultAllowTextView = (TextView) v.findViewById(R.id.openvpn_default_allow_text);

        Switch vpnOnDefaultSwitch = (Switch) v.findViewById(R.id.openvpn_default_allow);

        vpnOnDefaultSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

					changeDisallowText(isChecked);
					SecondVPN.app_prefs.edit().putBoolean("mAllowedAppsVpnAreDisallowed", isChecked).apply();
				}
			});

        vpnOnDefaultSwitch.setChecked(SecondVPN.app_prefs.getBoolean("mAllowedAppsVpnAreDisallowed", true));

        mListView = (ListView) v.findViewById(android.R.id.list);

		ApplicationLoader runner = new ApplicationLoader();
        runner.execute();

        return v;
    }

    private void changeDisallowText(boolean selectedAreDisallowed) {
        if (selectedAreDisallowed)
            mDefaultAllowTextView.setText(R.string.vpn_disallow_radio);
        else
            mDefaultAllowTextView.setText(R.string.vpn_allow_radio);
    }
}

