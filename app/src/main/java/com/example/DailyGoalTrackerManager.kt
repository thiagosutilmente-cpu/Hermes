package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Locale

/**
 * Estado reativo do rastreador de metas diárias de faturamento
 */
data class DailyGoalState(
    val targetAmount: Double = 250.00,
    val grossEarned: Double = 0.00,
    val fuelExpense: Double = 0.00,
    val netEarned: Double = 0.00,
    val completedRidesCount: Int = 0,
    val progressFraction: Float = 0f,
    val isGoalAchieved: Boolean = false,
    val remainingAmount: Double = 250.00,
    val estimatedRidesRemaining: Int = 0,
    val hourlyEarningRate: Double = 0.00,
    val shiftStartTimestamp: Long = System.currentTimeMillis()
)

/**
 * Gerenciador de Metas Diárias com Alarme Inteligente de Faturamento.
 * Permite ao piloto definir sua meta de ganhos do turno e avisa automaticamente via TTS
 * quando a meta for batida.
 */
class DailyGoalTrackerManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DailyGoalTracker"
        private const val PREFS_NAME = "radar_daily_goal_prefs"
        private const val KEY_TARGET_AMOUNT = "key_target_amount"
        private const val KEY_LAST_DAY = "key_last_day"
        private const val KEY_GROSS_EARNED = "key_gross_earned"
        private const val KEY_FUEL_EXPENSE = "key_fuel_expense"
        private const val KEY_RIDES_COUNT = "key_rides_count"
        private const val KEY_GOAL_ACHIEVED_ANNOUNCED = "key_goal_achieved_announced"
        private const val KEY_SHIFT_START = "key_shift_start"

        @Volatile
        private var INSTANCE: DailyGoalTrackerManager? = null

        fun getInstance(context: Context): DailyGoalTrackerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DailyGoalTrackerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _goalState = MutableStateFlow(DailyGoalState())
    val goalState: StateFlow<DailyGoalState> = _goalState.asStateFlow()

    private var wasCelebratedToday = false

    init {
        loadDailyState()
    }

    private fun getTodayDayOfYear(): Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    fun loadDailyState() {
        val today = getTodayDayOfYear()
        val savedDay = prefs.getInt(KEY_LAST_DAY, -1)

        val target = prefs.getFloat(KEY_TARGET_AMOUNT, 250.00f).toDouble()
        val shiftStart = prefs.getLong(KEY_SHIFT_START, System.currentTimeMillis())

        if (savedDay != today) {
            // Novo dia: reseta ganhos do turno mantendo a meta configurada
            prefs.edit()
                .putInt(KEY_LAST_DAY, today)
                .putFloat(KEY_GROSS_EARNED, 0f)
                .putFloat(KEY_FUEL_EXPENSE, 0f)
                .putInt(KEY_RIDES_COUNT, 0)
                .putBoolean(KEY_GOAL_ACHIEVED_ANNOUNCED, false)
                .putLong(KEY_SHIFT_START, System.currentTimeMillis())
                .apply()
            wasCelebratedToday = false
            updateInternalState(target, 0.0, 0.0, 0, System.currentTimeMillis())
        } else {
            val gross = prefs.getFloat(KEY_GROSS_EARNED, 0f).toDouble()
            val fuel = prefs.getFloat(KEY_FUEL_EXPENSE, 0f).toDouble()
            val rides = prefs.getInt(KEY_RIDES_COUNT, 0)
            wasCelebratedToday = prefs.getBoolean(KEY_GOAL_ACHIEVED_ANNOUNCED, false)
            updateInternalState(target, gross, fuel, rides, shiftStart)
        }
    }

    fun setTargetAmount(newTarget: Double) {
        val safeTarget = newTarget.coerceIn(50.0, 2000.0)
        prefs.edit().putFloat(KEY_TARGET_AMOUNT, safeTarget.toFloat()).apply()
        val current = _goalState.value
        updateInternalState(safeTarget, current.grossEarned, current.fuelExpense, current.completedRidesCount, current.shiftStartTimestamp)
    }

    fun recordDeliveryEarnings(grossFare: Double, fuelCost: Double = 0.0) {
        val current = _goalState.value
        val newGross = current.grossEarned + grossFare
        val newFuel = current.fuelExpense + fuelCost
        val newRides = current.completedRidesCount + 1

        prefs.edit()
            .putFloat(KEY_GROSS_EARNED, newGross.toFloat())
            .putFloat(KEY_FUEL_EXPENSE, newFuel.toFloat())
            .putInt(KEY_RIDES_COUNT, newRides)
            .apply()

        updateInternalState(current.targetAmount, newGross, newFuel, newRides, current.shiftStartTimestamp)
        checkAndCelebrateGoalAchieved()
    }

    private fun updateInternalState(
        target: Double,
        gross: Double,
        fuel: Double,
        rides: Int,
        shiftStart: Long
    ) {
        val net = (gross - fuel).coerceAtLeast(0.0)
        val progress = if (target > 0) (net / target).toFloat().coerceIn(0f, 1.5f) else 0f
        val isAchieved = net >= target
        val remaining = (target - net).coerceAtLeast(0.0)

        val avgPerRide = if (rides > 0) (net / rides) else 22.0
        val estimatedRemainingRides = if (remaining > 0 && avgPerRide > 0) {
            Math.ceil(remaining / avgPerRide).toInt().coerceAtLeast(1)
        } else 0

        val elapsedHours = ((System.currentTimeMillis() - shiftStart) / 3600000.0).coerceAtLeast(0.2)
        val hourlyRate = net / elapsedHours

        _goalState.value = DailyGoalState(
            targetAmount = target,
            grossEarned = gross,
            fuelExpense = fuel,
            netEarned = net,
            completedRidesCount = rides,
            progressFraction = progress,
            isGoalAchieved = isAchieved,
            remainingAmount = remaining,
            estimatedRidesRemaining = estimatedRemainingRides,
            hourlyEarningRate = hourlyRate,
            shiftStartTimestamp = shiftStart
        )
    }

    private fun checkAndCelebrateGoalAchieved() {
        val state = _goalState.value
        if (state.isGoalAchieved && !wasCelebratedToday) {
            wasCelebratedToday = true
            prefs.edit().putBoolean(KEY_GOAL_ACHIEVED_ANNOUNCED, true).apply()

            try {
                HapticFeedbackHelper.vibrateSuccess(context)
                CustomSoundPlayer.previewSound(context, NotificationSoundType.CHIME_MELODIC)

                val tts = OfferTextToSpeechEngine.getInstance(context)
                val targetFmt = String.format(Locale.GERMANY, "%.0f", state.targetAmount)
                val netFmt = String.format(Locale.GERMANY, "%.2f", state.netEarned).replace(".", ",")

                val speech = "Atenção piloto! Parabéns! Meta diária de $targetFmt reais batida com sucesso! Você faturou $netFmt reais líquidos em ${state.completedRidesCount} corridas. Excelente turno!"
                tts.speak(speech)
            } catch (e: Exception) {
                Log.e(TAG, "Erro na celebração da meta: ${e.message}")
            }
        }
    }

    fun resetTodayProgress() {
        prefs.edit()
            .putFloat(KEY_GROSS_EARNED, 0f)
            .putFloat(KEY_FUEL_EXPENSE, 0f)
            .putInt(KEY_RIDES_COUNT, 0)
            .putBoolean(KEY_GOAL_ACHIEVED_ANNOUNCED, false)
            .putLong(KEY_SHIFT_START, System.currentTimeMillis())
            .apply()
        wasCelebratedToday = false
        updateInternalState(_goalState.value.targetAmount, 0.0, 0.0, 0, System.currentTimeMillis())
    }
}
