package cgu.ai.koldcube;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class SettingsFragment extends Fragment {

    public static final String PREFS_NAME = "koldcube_prefs";
    public static final String KEY_SOUND = "pref_sound";
    public static final String KEY_INSPECTION = "pref_inspection";
    public static final String KEY_DISPLAY_FORMAT = "pref_display_format";

    public static final boolean DEFAULT_SOUND = true;
    public static final boolean DEFAULT_INSPECTION = false;
    public static final String DEFAULT_DISPLAY_FORMAT = "seconds";

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        SwitchCompat switchSound = view.findViewById(R.id.switchSound);
        SwitchCompat switchInspection = view.findViewById(R.id.switchInspection);
        RadioGroup rgDisplayFormat = view.findViewById(R.id.rgDisplayFormat);
        MaterialButton btnBack = view.findViewById(R.id.btnSettingsBack);

        // Sound effect preference
        switchSound.setChecked(prefs.getBoolean(KEY_SOUND, DEFAULT_SOUND));
        switchSound.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean(KEY_SOUND, checked).apply());

        // WCA inspection preference
        switchInspection.setChecked(prefs.getBoolean(KEY_INSPECTION, DEFAULT_INSPECTION));
        switchInspection.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean(KEY_INSPECTION, checked).apply());

        // Display format preference
        String format = prefs.getString(KEY_DISPLAY_FORMAT, DEFAULT_DISPLAY_FORMAT);
        rgDisplayFormat.check("minsec".equals(format) ? R.id.rbMinSec : R.id.rbSeconds);
        rgDisplayFormat.setOnCheckedChangeListener((group, checkedId) -> {
            String value = checkedId == R.id.rbMinSec ? "minsec" : "seconds";
            prefs.edit().putString(KEY_DISPLAY_FORMAT, value).apply();
        });

        btnBack.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(MainActivity.PAGE_TIMER));
    }
}
