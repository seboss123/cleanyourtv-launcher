package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AppDrawerActivity: a full-screen app drawer showing all installed launchable apps,
 * separate from the Home screen. Opens from Home (long-press home button or library).
 * Apps are sorted alphabetically and shown in a grid with focus scaling.
 */
public class AppDrawerActivity extends Activity {

    private static final int COLUMNS = 5;

    private RecyclerView drawerGrid;
    private AppDrawerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_drawer);

        drawerGrid = findViewById(R.id.drawerGrid);
        drawerGrid.setLayoutManager(new GridLayoutManager(this, COLUMNS));

        List<AppEntry> apps = loadLaunchableApps();
        adapter = new AppDrawerAdapter(this, apps);
        drawerGrid.setAdapter(adapter);

        // Prevent back from closing the drawer with no animation — just finish normally.
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Back or Home button returns to Home.
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static List<AppEntry> loadLaunchableApps() {
        List<AppEntry> apps = new ArrayList<>();
        // Loaded lazily in the adapter via PackageManager; here we just collect component names.
        return apps;
    }

    static final class AppEntry {
        final String packageName;
        final String className;
        final String label;
        Drawable icon;

        AppEntry(String packageName, String className, String label) {
            this.packageName = packageName;
            this.className = className;
            this.label = label;
        }
    }

    static final class AppDrawerAdapter extends RecyclerView.Adapter<AppDrawerAdapter.ViewHolder> {

        private final Activity activity;
        private final PackageManager pm;
        private final List<AppEntry> entries = new ArrayList<>();

        AppDrawerAdapter(Activity activity, List<AppEntry> initial) {
            this.activity = activity;
            this.pm = activity.getPackageManager();
            loadApps();
        }

        private void loadApps() {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> ris = pm.queryIntentActivities(intent, 0);

            for (ResolveInfo ri : ris) {
                String pkg = ri.activityInfo.packageName;
                // Skip the launcher itself and obvious system stubs.
                if (pkg.equals("com.gathin.tvhome")) continue;
                if (pkg.startsWith("android")) continue;

                String cls = ri.activityInfo.name;
                String label = ri.activityInfo.loadLabel(pm).toString();
                entries.add(new AppEntry(pkg, cls, label));
            }

            Collections.sort(entries, (a, b) -> {
                java.text.Collator c = java.text.Collator.getInstance();
                return c.compare(a.label, b.label);
            });
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_drawer, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppEntry entry = entries.get(position);
            holder.bind(entry);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnFocusChangeListener, View.OnClickListener {
            final ImageView icon;
            final TextView label;
            AppEntry entry;

            ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.drawerIcon);
                label = itemView.findViewById(R.id.drawerLabel);
                itemView.setOnFocusChangeListener(this);
                itemView.setOnClickListener(this);
            }

            void bind(AppEntry entry) {
                this.entry = entry;
                if (entry.icon == null) {
                    try {
                        entry.icon = pm.getApplicationIcon(entry.packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        entry.icon = pm.getDefaultActivityIcon();
                    }
                }
                icon.setImageDrawable(entry.icon);
                label.setText(entry.label);
            }

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                float scale = hasFocus ? 1.15f : 1.0f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(150).start();
                label.setTextColor(hasFocus ? 0xFFFFFFFF : 0xE6FFFFFF);
            }

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(entry.packageName, entry.className);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
        }
    }
}
