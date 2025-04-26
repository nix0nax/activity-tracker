package si.uni_lj.fri.pbd.classproject2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import si.uni_lj.fri.pbd.classproject2.databinding.FragmentDashboardBinding
import si.uni_lj.fri.pbd.classproject2.databinding.FragmentHistoryBinding
import java.time.LocalDate
import java.time.temporal.TemporalUnit
import kotlin.math.round
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class HistoryFragment : Fragment() {

    private lateinit var binding : FragmentHistoryBinding;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentHistoryBinding.inflate(layoutInflater)
        updateDisplay();

        // Inflate the layout for this fragment
        return binding.root
    }

    fun updateDisplay() {
        var date = LocalDate.now();
        date = date.minusDays(1)
        val preferenceManager = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)

        val stepsYesterday = preferenceManager.getInt("${date}_steps", 0)
        binding.historyStepsYesterdayValue.text = stepsYesterday.toString();

        binding.historySedentaryYesterdayValue.text = yesterdayActivityString("sedentary");
        binding.historyWalkingYesterdayValue.text = yesterdayActivityString("walk");
        binding.historyRunningYesterdayValue.text = yesterdayActivityString("run");
        binding.historyCyclingYesterdayValue.text = yesterdayActivityString("cycling");

        val stepsAvg = calculateAverageSteps()
        binding.historyStepsWeekValue.text = stepsAvg.toString();

        binding.historySedentaryWeekValue.text = averageActivityString("sedentary");
        binding.historyRunningWeekValue.text = averageActivityString("run");
        binding.historyWalkingWeekValue.text = averageActivityString("walk");
        binding.historyCyclingWeekValue.text = averageActivityString("cycling");
    }

    fun calculateAverageSteps(): Int{
        val preferenceManager = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
        var totalSteps = 0;
        var date = LocalDate.now();

        for (i in 1..7)
        {
            date = date.minusDays(1)
            totalSteps += preferenceManager.getInt("${date}_steps", 0)
        }

        return totalSteps/7;
    }

    fun yesterdayActivityString(activity: String): String {
        val preferenceManager = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
        var date = LocalDate.now();

        date = date.minusDays(1)
        var totalTime = preferenceManager.getInt("${date}_${activity}", 0)

        val secondsInDay = 1.days.inWholeSeconds.toInt()

        val percent = totalTime / secondsInDay * 100

        val str = totalTime.seconds.toComponents { hours, minutes, _, _ -> "${hours}h:${minutes}m (${percent}%)" }

        return str
    }

    fun averageActivityString(activity: String): String {
        val preferenceManager = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
        var totalTime = 0;
        var date = LocalDate.now();

        for (i in 1..7)
        {
            date = date.minusDays(1)
            totalTime += preferenceManager.getInt("${date}_${activity}", 0)
        }

        val secondsInDay = 1.days.inWholeSeconds.toInt()
        val averageTime = totalTime / 7

        val percent = averageTime / secondsInDay * 100

        val str = averageTime.seconds.toComponents { hours, minutes, _, _ -> "${hours}h:${minutes}m (${percent}%)" }

        return str
    }
}