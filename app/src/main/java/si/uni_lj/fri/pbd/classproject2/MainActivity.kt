package si.uni_lj.fri.pbd.classproject2

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import si.uni_lj.fri.pbd.classproject2.databinding.ActivityMainBinding
import android.Manifest

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        const val NOTIF_REQUEST_CODE = 42
    }

    // Service setup
    private var service: SensingService? = null
    private lateinit var binding: ActivityMainBinding

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.i(TAG, "onServiceConnected()")
            this@MainActivity.service = (service as SensingService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "onServiceDisconnected()")
            service = null
        }
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Navigation setup
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard, R.id.nav_history, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()

        // Ask for permissions
        val permNotif = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
        if (permNotif != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permBody = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS);
        if (permBody != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }

        val permAct = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION);
        if (permAct != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // start service if preference is set
        val servicePreference = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getBoolean("enable_service", false)

        // start/stop service on change
        if (servicePreference) {
            startSensingService();
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        service = null
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
//            if (!isGranted) {
//                moveTaskToBack(true);
//                exitProcess(-1)
//            }
        }

    fun startSensingService() {
        val intent = Intent(this@MainActivity, SensingService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    fun stopSensingService() {
        service?.let {
            unbindService(connection)
            service = null
            stopService(Intent(this@MainActivity, SensingService::class.java))
        }
    }

}