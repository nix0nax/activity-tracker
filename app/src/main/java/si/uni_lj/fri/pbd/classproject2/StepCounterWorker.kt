package si.uni_lj.fri.pbd.classproject2

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

private const val TAG = " StepCounterWorker"

class StepCounterWorker  constructor(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "bwaa")

        // update steps here
        return Result.success()
    }
}