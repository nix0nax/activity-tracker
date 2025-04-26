package si.uni_lj.fri.pbd.classproject2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import si.uni_lj.fri.pbd.classproject2.databinding.FragmentDashboardBinding
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DashboardFragment : Fragment() {

    private lateinit var binding : FragmentDashboardBinding;
    private lateinit var stepCountText : TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentDashboardBinding.inflate(layoutInflater)
        stepCountText = binding.dashTextStepsDoneValue

        val broadcastManager = LocalBroadcastManager.getInstance(requireActivity().baseContext)
        broadcastManager.registerReceiver(stepReciever,
            IntentFilter("stepUI")
        )
        broadcastManager.registerReceiver(activityReceiver,
            IntentFilter("activityUI")
        )

        // populate for test
        populateForTest();

        // update numbers from preferences
        updateDisplay(binding)

        // Inflate the layout for this fragment
        return binding.root;
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireActivity().baseContext).unregisterReceiver(stepReciever)
            super.onStop()
    }

    fun populateForTest() {
        var date = LocalDate.now();
        val sharedPref = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
        with (sharedPref.edit()) {
            putInt("${date}_steps", 1506)
            putInt("${date}_sedentary", 9564)
            putInt("${date}_run", 2467)
            putInt("${date}_walk", 3574)
            putInt("${date}_cycling", 9875)
            date = date.minusDays(1)
            putInt("${date}_steps", 205)
            putInt("${date}_sedentary", 1234)
            putInt("${date}_run", 4567)
            putInt("${date}_walk", 755)
            putInt("${date}_cycling", 1806)
            date = date.minusDays(1)
            putInt("${date}_steps", 111)
            putInt("${date}_sedentary", 6547)
            putInt("${date}_run", 2356)
            putInt("${date}_walk", 3333)
            putInt("${date}_cycling", 5553)
            date = date.minusDays(1)
            putInt("${date}_steps", 1563)
            putInt("${date}_sedentary", 6542)
            putInt("${date}_run", 4265)
            putInt("${date}_walk", 467)
            putInt("${date}_cycling", 745)
            date = date.minusDays(1)
            putInt("${date}_steps", 789)
            putInt("${date}_sedentary", 456)
            putInt("${date}_run", 3556)
            putInt("${date}_walk", 4545)
            putInt("${date}_cycling", 5656)
            date = date.minusDays(1)
            putInt("${date}_steps", 1975)
            putInt("${date}_sedentary", 6567)
            putInt("${date}_run", 8766)
            putInt("${date}_walk", 6655)
            putInt("${date}_cycling", 3466)
            date = date.minusDays(1)
            putInt("${date}_steps", 8567)
            putInt("${date}_sedentary", 3464)
            putInt("${date}_run", 346)
            putInt("${date}_walk", 6343)
            putInt("${date}_cycling", 6346)
            date = date.minusDays(1)
            putInt("${date}_steps", 12345)
            putInt("${date}_sedentary", 54246)
            putInt("${date}_run", 2642)
            putInt("${date}_walk", 24246)
            putInt("${date}_cycling", 2466)

            commit()
        }
    }

    fun updateDisplay(binding: FragmentDashboardBinding) {
        val stepGoal = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
            .getString("step_goal", "10000")

        binding.dashTextStepGoalValue.text = stepGoal

        val date = LocalDate.now();
        val steps = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
            .getInt("${date}_steps", 0)
        binding.dashTextStepsDone.text = steps.toString();

        val sedentaryTime = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
            .getInt("${date}_sedentary", 0)
        binding.dashTextSedentaryTime.text = createStringFromSeconds(sedentaryTime);

        val walkingTime = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
            .getInt("${date}_walk", 0)
        binding.dashTextWalkingTime.text = createStringFromSeconds(walkingTime);

        val runningTime = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
            .getInt("${date}_run", 0)
        binding.dashTextRunningTime.text = createStringFromSeconds(runningTime);

        val runningIntensity = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
            .getInt("${date}_run_intensity", 0)
        binding.dashTextRunningIntensityValue.text = runningIntensity.toString();

        val cyclingTime = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
            .getInt("${date}_cycling", 0)
        binding.dashTextCyclingTime.text = createStringFromSeconds(cyclingTime);

        val cyclingIntensity = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
            .getInt("${date}_cycling_intensity", 0)
        binding.dashTextCyclingIntensityValue.text = cyclingIntensity.toString();
    }

    private val stepReciever: BroadcastReceiver = object :
        BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.dashTextStepsDone.text = intent?.getStringExtra("steps")
        }
    }

    private val activityReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val sedentary = intent?.getStringExtra("sedentary")
            val walk = intent?.getStringExtra("walk")
            val run = intent?.getStringExtra("run")
            val cycle = intent?.getStringExtra("cycling")

            binding.dashTextSedentaryTime.text = createStringFromSeconds(sedentary?.toInt() ?: 0)
            binding.dashTextRunningTime.text = createStringFromSeconds(run?.toInt() ?: 0)
            binding.dashTextCyclingTime.text = createStringFromSeconds(cycle?.toInt() ?: 0)
            binding.dashTextWalkingTime.text = createStringFromSeconds(walk?.toInt() ?: 0)
        }
    }

    private fun createStringFromSeconds(seconds: Int): String {
        val asDuration = seconds.seconds
        val currentSeconds = LocalTime.now().toSecondOfDay();
        val percent = seconds / currentSeconds * 100

        val str = asDuration.toComponents { hours, minutes, _, _ -> "${hours}h:${minutes}m (${percent}%)" }
        return str;
    }
}