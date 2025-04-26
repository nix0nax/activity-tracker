package si.uni_lj.fri.pbd.classproject2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityRecognitionReceiver : BroadcastReceiver() {

    var startTime = LocalDateTime.now()

    override fun onReceive(context: Context, intent: Intent) {

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!
            for (event in result.transitionEvents) {

                broadcastActivityChange(context, event.activityType.toString(), event.transitionType.toString())

                Log.d("wa", event.activityType.toString() + " " + event.transitionType.toString())
            }
        }
    }

    private fun broadcastActivityChange(context: Context, activityType: String, transitionType: String) {
        val intent = Intent("activityChange")
        intent.putExtra("activityType", activityType)
        intent.putExtra("transitionType", transitionType)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}