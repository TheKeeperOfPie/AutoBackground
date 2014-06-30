package cw.kop.autobackground;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import cw.kop.autobackground.R;

public class AboutFragment extends PreferenceFragment{

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure default values are applied.  In a real app, you would
        // want this in a shared function that is used to retrieve the
        // SharedPreferences wherever they are needed.
        PreferenceManager.setDefaultValues(getActivity(),
                R.xml.preferences_about, false);
        
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_about);
    }
	
}

