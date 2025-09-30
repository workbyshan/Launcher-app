package com.example.mylauncher;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo; // Keep for reference, not directly used in loop filter
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements AppAdapter.OnItemClickListener, AppAdapter.OnItemLongClickListener {

    private static final String PREFS_NAME = "MyLauncherPrefs";
    private static final String KEY_LAYOUT_MANAGER = "LayoutManager";
    private static final String KEY_THEME = "Theme"; // SharedPreferences key for theme

    private static final int LAYOUT_MANAGER_GRID = 0;
    private static final int LAYOUT_MANAGER_LIST = 1;
    private static final int GRID_SPAN_COUNT = 4;

    private AppAdapter adapter;
    private List<AppInfo> appsList = new ArrayList<>();
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private ImageButton layoutToggleButton;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences sharedPreferences;
    private boolean isGridLayoutManager = true; // Default to grid

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        applySavedTheme(); // Apply theme before setContentView

        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.app_list_recycler_view);
        searchEditText = findViewById(R.id.search_edit_text);
        layoutToggleButton = findViewById(R.id.layout_toggle_button);

        adapter = new AppAdapter(this, new ArrayList<>(), this, this);
        recyclerView.setAdapter(adapter);

        loadLayoutPreference();
        updateLayoutManager();

        loadAppsInBackground();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }
        });

        layoutToggleButton.setOnClickListener(v -> toggleLayoutManager());
    }

    private void applySavedTheme() {
        int currentTheme = sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(currentTheme);
    }

    private void saveThemePreference(int themeMode) {
        sharedPreferences.edit().putInt(KEY_THEME, themeMode).apply();
    }

    private void cycleTheme() {
        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            saveThemePreference(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (currentNightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            saveThemePreference(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            saveThemePreference(AppCompatDelegate.MODE_NIGHT_YES);
        }
        recreate(); // Recreate activity to apply theme immediately
    }

    private void loadLayoutPreference() {
        int layoutManagerType = sharedPreferences.getInt(KEY_LAYOUT_MANAGER, LAYOUT_MANAGER_GRID);
        isGridLayoutManager = (layoutManagerType == LAYOUT_MANAGER_GRID);
    }

    private void saveLayoutPreference() {
        sharedPreferences.edit()
                .putInt(KEY_LAYOUT_MANAGER, isGridLayoutManager ? LAYOUT_MANAGER_GRID : LAYOUT_MANAGER_LIST)
                .apply();
    }

    private void updateLayoutManager() {
        if (isGridLayoutManager) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, GRID_SPAN_COUNT));
            layoutToggleButton.setImageResource(R.drawable.ic_view_list);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            layoutToggleButton.setImageResource(R.drawable.ic_view_module);
        }
    }

    private void toggleLayoutManager() {
        isGridLayoutManager = !isGridLayoutManager;
        updateLayoutManager();
        saveLayoutPreference();
    }

    private void loadAppsInBackground() {
        executorService.execute(() -> {
            final List<AppInfo> loadedApps = getInstalledApps();
            handler.post(() -> {
                appsList.clear();
                appsList.addAll(loadedApps);
                adapter = new AppAdapter(MainActivity.this, appsList, MainActivity.this, MainActivity.this);
                recyclerView.setAdapter(adapter);
                // Temporarily removed initial filtering to ensure all apps are shown first
                // String currentFilter = searchEditText.getText().toString();
                // if (!currentFilter.isEmpty()) {
                //    adapter.filter(currentFilter);
                // }
            });
        });
    }

    private List<AppInfo> getInstalledApps() {
        List<AppInfo> installedApps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allAppsResolveInfo = pm.queryIntentActivities(i, 0); // Query for launchable apps

        for (ResolveInfo ri : allAppsResolveInfo) {
            // No additional filtering based on ApplicationInfo.flags
            // Add all apps that have ACTION_MAIN and CATEGORY_LAUNCHER
            installedApps.add(new AppInfo(
                    ri.loadLabel(pm),
                    ri.activityInfo.loadIcon(pm),
                    ri.activityInfo.packageName
            ));
        }
        Collections.sort(installedApps, (o1, o2) -> o1.getLabel().toString().compareToIgnoreCase(o2.getLabel().toString()));
        return installedApps;
    }

    @Override
    public void onItemClick(AppInfo app) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(app.getPackageName());
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Cannot open this app", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemLongClick(AppInfo app) {
        final CharSequence[] items = {"App Info", "Uninstall", "Change Theme"};
        new AlertDialog.Builder(this)
                .setTitle(app.getLabel())
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent appInfoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            appInfoIntent.setData(Uri.parse("package:" + app.getPackageName()));
                            startActivity(appInfoIntent);
                            break;
                        case 1:
                            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
                            uninstallIntent.setData(Uri.parse("package:" + app.getPackageName()));
                            uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                            startActivityForResult(uninstallIntent, 1001);
                            break;
                        case 2: // Change Theme
                            cycleTheme();
                            break;
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            loadAppsInBackground();
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
