package com.xomzom.androidstuff.timerapp;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;

/**
 * The timer settings dialog activity.
 * @author dedi
 */
public class SettingsActivity extends PreferenceActivity
{
    /**
     * Activity was created. Set the view according to the XML settings state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        
        attachIntValidatingListener(R.string.pref_num_intervals_key, 1, 
                Integer.MAX_VALUE); // Not likely, but still..
        attachIntValidatingListener(R.string.pref_interval_length_key, 1,
                Integer.MAX_VALUE);
        attachIntValidatingListener(R.string.pref_countdown_key, 0,
                Integer.MAX_VALUE);
    }
    
    /**
     * Helper method: attach an onPreferenceChangeListener to the given
     * preference (identified by it's key string ID), to make sure that it's
     * value is an integer within the given bounds.
     */
    private void attachIntValidatingListener(int prefKeyStringId,
            final int minAllowedValue, final int maxAllowedValue)
    {
        String preferenceKey = getString(prefKeyStringId);
        if (preferenceKey == null)
            return;

        Preference preference = (Preference)findPreference(preferenceKey);
        if (preference == null)
            return;

        preference.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue)
                    {
                        return validateIntPreference(preference,
                                newValue,
                                minAllowedValue,
                                maxAllowedValue);
                    }
                });
    }
    
    /**
     * Helper method: validate the new value of the given Preference,
     * and make sure that it is an integer between the given min and max values.
     * Also display an error message to the user if the value is invalid.
     *
     * @param preference The preference object.
     * @param newValue The new preference value (which is presumed to be 
     * a string)
     * @param minAllowedValue The minimum allowed value.
     * @param maxAllowedValue The maximum allowed value.
     * @return true if the value is valid
     */
    private boolean validateIntPreference(Preference preference,
                                          Object newValue,
                                          int minAllowedValue,
                                          int maxAllowedValue)
    {
        String newValueAsString = newValue.toString();
        int valueAsInt = 0;
        try {
            valueAsInt = Integer.parseInt(newValueAsString);
        }
        catch (NumberFormatException e)
        {
            doValueError(minAllowedValue, maxAllowedValue);
            return false;
        }
        
        if (valueAsInt < minAllowedValue || valueAsInt > maxAllowedValue)
        {
            doValueError(minAllowedValue, maxAllowedValue);
            return false;
        }
        return true;
    }

    /**
     * Helper method: Display a toast message saying that the preference value
     * was not accepted, because it is not an integer in the given bounds. 
     * @param minAllowedValue
     * @param maxAllowedValue
     */
    private void doValueError(int minAllowedValue, int maxAllowedValue)
    {
        String errMsg = getString(R.string.bad_int_pref_message, 
                minAllowedValue, maxAllowedValue);
        Toast errToast = Toast.makeText(this, errMsg, Toast.LENGTH_LONG);
        errToast.show();
    }
}
