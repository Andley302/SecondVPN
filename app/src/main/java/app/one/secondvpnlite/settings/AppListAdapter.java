package app.one.secondvpnlite.settings;

/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.one.secondvpnlite.R;
import app.one.secondvpnlite.util.SharedPref;

/**
 * Created by arne on 16.11.14.
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private List<ResolveInfo> resolveInfoList;
    private final PackageManager packageManager;
    private Set<String> selectedApps;

    private SharedPref sharedPref;

    public AppListAdapter(List<ResolveInfo> resolveInfoList, PackageManager packageManager, Context context) {
        this.resolveInfoList = resolveInfoList;
        this.packageManager = packageManager;

        // Initialize SharedPreferences
        sharedPref = SharedPref.getInstance(context);


        // Retrieve selected apps and their states from SharedPreferences
        selectedApps = sharedPref.getStringSet("selectedApps", new HashSet<>());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResolveInfo resolveInfo = resolveInfoList.get(position);

        new Thread(() -> {
            Drawable drawable = resolveInfo.loadIcon(packageManager);
            holder.itemView.post(() -> holder.appIcon.setImageDrawable(drawable));
        }).start();

        holder.appTitle.setText(resolveInfo.loadLabel(packageManager));
        holder.appPackageName.setText(resolveInfo.activityInfo.packageName);

        // Set the switch state based on the selected apps
        holder.mSwitch.setChecked(selectedApps.contains(resolveInfo.activityInfo.packageName));

        // Save the selected app and its state when the switch state changes
        holder.mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Add the app to the selected apps set
                selectedApps.add(resolveInfo.activityInfo.packageName);
            } else {
                // Remove the app from the selected apps set
                selectedApps.remove(resolveInfo.activityInfo.packageName);
                sharedPref.removeAppFromList(resolveInfo.activityInfo.packageName);
                sharedPref.putStringSet("selectedApps", selectedApps);
            }

            // Save the updated set to SharedPreferences
            //preferences.edit().putStringSet("selectedApps", selectedApps).apply();
            sharedPref.putStringSet("selectedApps", selectedApps);
            // Save the switch state for the app
            // preferences.edit().putBoolean(resolveInfo.activityInfo.packageName, isChecked).apply();
            sharedPref.putBoolean(resolveInfo.activityInfo.packageName, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return resolveInfoList.size();
    }

    public void selectAll() {
        for (ResolveInfo resolveInfo : resolveInfoList) {
            selectedApps.add(resolveInfo.activityInfo.packageName);
            //preferences.edit().putBoolean(resolveInfo.activityInfo.packageName, true).apply();
            sharedPref.putBoolean(resolveInfo.activityInfo.packageName, true);
        }
    }

    public void deselectAll() {
        for (ResolveInfo resolveInfo : resolveInfoList) {
            selectedApps.remove(resolveInfo.activityInfo.packageName);
            // preferences.edit().putBoolean(resolveInfo.activityInfo.packageName, false).apply();
            sharedPref.putBoolean(resolveInfo.activityInfo.packageName, false);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appTitle, appPackageName;
        SwitchCompat mSwitch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appTitle = itemView.findViewById(R.id.appName);
            mSwitch = itemView.findViewById(R.id.switchBtn);
            appPackageName = itemView.findViewById(R.id.packageName);
        }
    }
}
