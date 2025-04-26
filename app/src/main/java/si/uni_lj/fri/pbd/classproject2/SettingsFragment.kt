package si.uni_lj.fri.pbd.classproject2

//import android.R
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


class SettingsFragment : SharedPreferences.OnSharedPreferenceChangeListener,  PreferenceFragmentCompat() {
    private var service: SensingService? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button: Preference? = findPreference("delete_data")
        button!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(requireActivity().baseContext)

                with (sharedPref.edit()) {
                    clear()
                    commit()
                }
            }

    }
    override fun onResume() {
        super.onResume()
        // Set up a listener whenever a key changes
        preferenceScreen.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // extra logic is only required when starting service. Also make sure activity is correct.
        if (key?.equals("enable_service") != true || activity !is MainActivity || sharedPreferences == null) {
            return
        }

        // cast activity and find preference
        val servicePref = sharedPreferences.getBoolean(key, false)
        val activityMain = activity as MainActivity;

        // start/stop service on change
        if (servicePref) {
            activityMain.startSensingService();
        } else {
            activityMain.stopSensingService();
        }
    }

}