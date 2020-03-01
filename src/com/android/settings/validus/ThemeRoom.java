package com.android.settings.validus;

import com.android.internal.logging.nano.MetricsProto;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.ServiceManager;
import android.os.UserHandle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.R;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.android.settings.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class ThemeRoom extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String ACCENT_PRESET = "accent_preset";
    private static final String ACCENT_COLOR = "accent_color";
    private static final String ACCENT_COLOR_PROP = "persist.sys.theme.accentcolor";

    private IOverlayManager mOverlayService;
    private ListPreference mAccentPreset;
    private ColorPickerPreference mThemeColor;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.validus_theme_room);
        mOverlayService = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
        mThemeColor = (ColorPickerPreference) findPreference(ACCENT_COLOR);
        String colorVal = SystemProperties.get(ACCENT_COLOR_PROP, "-1");
        try {
            int color = "-1".equals(colorVal)
                    ? Color.WHITE
                    : Color.parseColor("#" + colorVal);
            mThemeColor.setNewPreviewColor(color);
        }
        catch (Exception e) {
            mThemeColor.setNewPreviewColor(Color.WHITE);
        }
        mThemeColor.setOnPreferenceChangeListener(this);

        mAccentPreset = (ListPreference) findPreference(ACCENT_PRESET);
        mAccentPreset.setOnPreferenceChangeListener(this);
        checkColorPreset(colorVal);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mThemeColor) {
            int color = (Integer) newValue;
            String hexColor = String.format("%08X", (0xFFFFFFFF & color));
            SystemProperties.set(ACCENT_COLOR_PROP, hexColor);
            checkColorPreset(hexColor);
            try {
                 mOverlayService.reloadAndroidAssets(UserHandle.USER_CURRENT);
                 mOverlayService.reloadAssets("com.android.settings", UserHandle.USER_CURRENT);
                 mOverlayService.reloadAssets("com.android.systemui", UserHandle.USER_CURRENT);
            } catch (RemoteException ignored) {
            }
        } else if (preference == mAccentPreset) {
            String value = (String) newValue;
            int index = mAccentPreset.findIndexOfValue(value);
            mAccentPreset.setSummary(mAccentPreset.getEntries()[index]);
            SystemProperties.set(ACCENT_COLOR_PROP, value);
            try {
                 mOverlayService.reloadAndroidAssets(UserHandle.USER_CURRENT);
                 mOverlayService.reloadAssets("com.android.settings", UserHandle.USER_CURRENT);
                 mOverlayService.reloadAssets("com.android.systemui", UserHandle.USER_CURRENT);
            } catch (RemoteException ignored) {
            }
        }
        return true;
    }

    private void checkColorPreset(String colorValue) {
        List<String> colorPresets = Arrays.asList(
                getResources().getStringArray(R.array.accent_presets_values));
        if (colorPresets.contains(colorValue)) {
            mAccentPreset.setValue(colorValue);
            int index = mAccentPreset.findIndexOfValue(colorValue);
            mAccentPreset.setSummary(mAccentPreset.getEntries()[index]);
        }
        else {
            mAccentPreset.setSummary(
                    getResources().getString(R.string.custom_string));
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VALIDUS;
    }
}
