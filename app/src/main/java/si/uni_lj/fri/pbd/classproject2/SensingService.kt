package si.uni_lj.fri.pbd.classproject2

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import java.time.LocalDate


class SensingService : Service(), SensorEventListener {

    val TRANSITIONS_RECEIVER_ACTION = "action.TRANSITIONS_DATA"
    private lateinit var sensorManager: SensorManager;
    private val mTransitionsReceiver: ActivityRecognitionReceiver = ActivityRecognitionReceiver();
    private var totalSteps = 0;
    companion object {
        val TAG = SensingService::class.java.simpleName
    }

    var killrun = false;

    var currentActivity = "sedentary"
    var actSed = 0;
    var actWalk = 0;
    var actRun = 0;
    var actCycle = 0;

    private lateinit var handler: Handler;

    var runnableUpdateSteps = object : Runnable {
        override fun run() {
            if (killrun) {
                return
            }
            //val currentSteps = totalSteps - previousTotalSteps;

            // add current steps to today's log (or add log if it doesn't exist)
            val date = LocalDate.now();
            val sharedPref = PreferenceManager
                .getDefaultSharedPreferences(applicationContext)

            val updateVal = sharedPref.getInt("${date}_steps", 0) + totalSteps;
            with (sharedPref.edit()) {
                putInt("${date}_steps", updateVal)
                commit()
            }
            broadcastStepUpdate(updateVal);

            updateNotification()
            // set previous steps to total steps
            totalSteps = 0;
            handler.postDelayed(this, 5000)
        }

    }

    var runnableSeconds = object : Runnable {
        override fun run() {
            if (killrun) {
                return
            }
            handler.postDelayed(this, 1000)
            when (currentActivity) {
                "sedentary" -> actSed++
                "walking" -> actWalk++
                "running" -> actRun++
                "bicycle" -> actCycle++
            }

            broadcastActivityUpdate();
        }
    }

    // an implementation of Binder interface
    internal class LocalBinder(val service: SensingService) : Binder()
    // a reference to LocalBinder
    private val binder = LocalBinder(this)

    override fun onBind(intent: Intent?): IBinder {
        return binder;
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()

        killrun = false;

        // Get data from prefs, store locally
        val date = LocalDate.now();
        val sharedPref = PreferenceManager
            .getDefaultSharedPreferences(baseContext)

        actSed = sharedPref.getInt("${date}_sedentary", 0);
        actWalk = sharedPref.getInt("${date}_walk", 0);
        actRun = sharedPref.getInt("${date}_run", 0);
        actCycle = sharedPref.getInt("${date}_cycling", 0);

        // sensor manager for tracking steps
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager;
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // step update
        handler = Handler(Looper.getMainLooper());

        handler.postDelayed(runnableUpdateSteps, 5000)

        // start activity handler every second
        handler.postDelayed(runnableSeconds, 1000)

        // transitions
        var mIntent = Intent(TRANSITIONS_RECEIVER_ACTION)
        var mPendingIntent = PendingIntent.getBroadcast(this@SensingService, 0, mIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT);

        val transitions = buildTransitionList()


        val request = ActivityTransitionRequest(transitions)
        val task = ActivityRecognition.getClient(this)
            .requestActivityTransitionUpdates(request, mPendingIntent)

        task.addOnSuccessListener {
            Log.d(TAG, "yipi")
            // Handle success

        }

        task.addOnFailureListener { e: Exception ->
            // Handle error
            Log.d(TAG, e.toString())
        }

        registerReceiver(
            mTransitionsReceiver,
            IntentFilter(TRANSITIONS_RECEIVER_ACTION),
            RECEIVER_EXPORTED
        )

        // Get today's data

        // Handle activity changes
        LocalBroadcastManager.getInstance(baseContext).registerReceiver(activityReciever,
            IntentFilter("activityChange")
        )

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        var sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(this)

        val handler = Handler(Looper.getMainLooper());
        killrun = true;
        handler.removeCallbacks(runnableUpdateSteps, null)
        handler.removeCallbacks(runnableSeconds, null)
        handler.removeCallbacksAndMessages(null)
        runnableUpdateSteps.run();
        totalSteps = 0;

        unregisterReceiver(mTransitionsReceiver)
        LocalBroadcastManager.getInstance(baseContext).unregisterReceiver(activityReciever)

        var date = LocalDate.now();
        val sharedPref = PreferenceManager
            .getDefaultSharedPreferences(baseContext)

        with (sharedPref.edit()) {
            putInt("${date}_sedentary", actSed);
            putInt("${date}_walk", actWalk);
            putInt("${date}_cycling", actCycle);
            putInt("${date}_run", actRun);
            commit()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "sensing_channel",
            "Activity Sensing",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = buildNotification();
        startForeground(1, notification)
    }

    private fun buildNotification(): Notification{
        val date = LocalDate.now();
        val steps = PreferenceManager
            .getDefaultSharedPreferences(baseContext)
            .getInt("${date}_steps", 0)
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "sensing_channel")
            .setContentTitle("Activity Sensing")
            .setContentText("${currentActivity} | Steps today: ${steps}")
            .setSmallIcon(android.R.drawable.star_big_on)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        return  notification;
    }

    private fun updateNotification() {
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification()
        mNotificationManager.notify(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        totalSteps++;
        return
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    private fun broadcastStepUpdate(steps: Int) {
        val intent = Intent("stepUI")
        intent.putExtra("steps", steps.toString())
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastActivityUpdate() {
        val intent = Intent("activityUI")
        intent.putExtra("sedentary", actSed.toString())
        intent.putExtra("walk", actWalk.toString())
        intent.putExtra("run", actRun.toString())
        intent.putExtra("cycling", actCycle.toString())
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun buildTransitionList() : List<ActivityTransition> {
        val transitions = mutableListOf<ActivityTransition>()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        return transitions;
    }

    private val activityReciever: BroadcastReceiver = object :
        BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val activity = intent?.getStringExtra("activityType")
            val transitionType = intent?.getStringExtra("transitionType")

            // Activity ended
            if (transitionType == "1") {
                return
            }

            currentActivity = when (activity) {
                "0", "3" -> "sedentary"
                "7" -> "walking"
                "8" -> "running"
                "1" -> "bicycle"
                else -> "null"
            }

            updateNotification()
        }
    }
}
