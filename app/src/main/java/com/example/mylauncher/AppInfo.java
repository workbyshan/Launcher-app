package com.example.mylauncher;

import android.graphics.drawable.Drawable;

public class AppInfo {
    // Declare fields as final if the object is intended to be immutable
    // once created. This is generally a good practice for data classes.
    public final CharSequence label;
    public final Drawable icon;
    public final String packageName;

    // Constructor
    public AppInfo(CharSequence label, Drawable icon, String packageName) {
        this.label = label;
        this.icon = icon;
        this.packageName = packageName;
    }

    // Getters
    public CharSequence getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getPackageName() {
        return packageName;
    }
}
