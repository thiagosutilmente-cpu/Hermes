package com.example.service

import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import com.example.coordinator.RadarCoordinator
import kotlin.random.Random

/**
 * Human Interaction Simulator (Jarvis Stealth Engine)
 * Implements clean abstraction layers to simulate natural human touch and cognitive patterns.
 * Designed to bypass robotic automation detection on modern delivery/ride platforms.
 */
object HumanInteractionSimulator {

    private const val TAG = "HumanSimulator"
    private var currentAdrenalineLevel = 1.0 // 1.0 = normal, > 1.0 = excited

    /**
     * Analyzes screen text for high-value orders to simulate an adrenaline spike.
     * High value orders cause faster reaction times but higher chances of mechanical tremors (nervousness).
     */
    fun analyzeScreenForAdrenalineSpike(screenText: String) {
        // Simple heuristic: look for currency symbols and numbers
        val regex = Regex("""R\$\s*(\d{2,3})""")
        val matches = regex.findAll(screenText)
        
        var maxVal = 0
        for (match in matches) {
            val value = match.groupValues[1].toIntOrNull() ?: 0
            if (value > maxVal) maxVal = value
        }
        
        currentAdrenalineLevel = when {
            maxVal >= 50 -> 1.4 // High adrenaline! (Faster reaction, more jitter)
            maxVal >= 20 -> 1.15
            else -> 1.0
        }
        
        if (currentAdrenalineLevel > 1.0) {
            Log.d(TAG, "Adrenaline spike detected! Level: $currentAdrenalineLevel (Max value: R$$maxVal)")
            if (currentAdrenalineLevel >= 1.4) {
                RadarCoordinator.addLog("Stealth Engine: Simulação de Adrenalina ativada (Corrida de Alto Valor). Reação muscular mais rápida, porém com mais chance de hesitação.", com.example.coordinator.LogType.ALERT)
            }
        }
    }

    /**
     * Determines a highly natural touch coordinate inside a given bounding box (Rect).
     * Avoids the dead-center geometric point, introducing human touch landing offsets.
     */
    fun getHumanizedTouchPoint(bounds: Rect): Pair<Float, Float> {
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        
        val width = bounds.width()
        val height = bounds.height()

        if (!isAntiDetectionActive || width <= 4 || height <= 4) {
            return Pair(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }

        // Keep the touch point inside the natural 20% to 80% safe zone of the button.
        // Humans usually tap slightly lower than center or slightly to the right depending on thumb position.
        val marginPercent = 0.20
        val minX = (bounds.left + (width * marginPercent)).toInt()
        val maxX = (bounds.right - (width * marginPercent)).toInt()
        val minY = (bounds.top + (height * marginPercent)).toInt()
        val maxY = (bounds.bottom - (height * marginPercent)).toInt()

        val finalMinX = minOf(minX, maxX).coerceAtLeast(bounds.left)
        val finalMaxX = maxOf(minX, maxX).coerceAtMost(bounds.right)
        val finalMinY = minOf(minY, maxY).coerceAtLeast(bounds.top)
        val finalMaxY = maxOf(minY, maxY).coerceAtMost(bounds.bottom)

        var x = if (finalMaxX > finalMinX) (finalMinX..finalMaxX).random().toFloat() else bounds.centerX().toFloat()
        var y = if (finalMaxY > finalMinY) (finalMinY..finalMaxY).random().toFloat() else bounds.centerY().toFloat()

        // Kinetic Tremor Simulation (simulating the user is walking or in a moving vehicle)
        // Adds a high-frequency, low-amplitude noise to the final touch point
        val isMoving = Random.nextDouble() > 0.4 // 60% chance the user is in motion (bike, car, walking)
        if (isMoving) {
            val kineticJitterX = Random.nextDouble(-3.5, 3.5).toFloat()
            val kineticJitterY = Random.nextDouble(-3.5, 3.5).toFloat()
            x += kineticJitterX
            y += kineticJitterY
            Log.d(TAG, "Applied Kinetic Jitter: dx=$kineticJitterX, dy=$kineticJitterY")
        }

        Log.d(TAG, "getHumanizedTouchPoint: button rect=$bounds, selected point=($x, $y) (offset from center: dx=${x - bounds.centerX()}, dy=${y - bounds.centerY()})")
        return Pair(x, y)
    }

    /**
     * Generates a dynamic path simulating a natural human touch signature.
     * Includes initial micro-tremors, contact spread (micro-slides), and finger lift friction.
     */
    fun createHumanizedSwipePath(startX: Float, startY: Float): Path {
        val path = Path()
        path.moveTo(startX, startY)

        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        if (isAntiDetectionActive) {
            // Human skin spreads over the glass. This translates to a minor micro-movement.
            // Simulate 1 to 4 pixels of travel in a random, natural thumb-arch direction.
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val distance = Random.nextDouble(1.0, 3.5)
            val endX = startX + (Math.cos(angle) * distance).toFloat()
            val endY = startY + (Math.sin(angle) * distance).toFloat()
            
            // Add a mid-curve control point to make it a non-perfect line (bezier curve)
            val ctrlX = startX + (Math.cos(angle) * (distance / 2.0)).toFloat() + Random.nextFloat() - 0.5f
            val ctrlY = startY + (Math.sin(angle) * (distance / 2.0)).toFloat() + Random.nextFloat() - 0.5f
            
            path.quadTo(ctrlX, ctrlY, endX, endY)
        }
        return path
    }

    /**
     * Calculates a dynamic press duration mimicking the physics of human skin/tissue
     * on capacitive screens, which varies with pressure and reaction time.
     */
    fun getHumanizedPressDuration(): Long {
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        return if (isAntiDetectionActive) {
            // Natural taps range from 65ms (quick light tap) to 145ms (relaxed, deliberate tap)
            (65..145).random().toLong()
        } else {
            85L
        }
    }

    /**
     * Determines whether to simulate a "miss-click" (tapping just outside the button first, then correcting).
     * Humans, especially when fatigued, sometimes miss the button on their first try.
     */
    fun shouldSimulateMissClick(): Boolean {
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        if (!isAntiDetectionActive) return false
        
        // Higher chance if fatigued or high adrenaline. Base 5% chance.
        val baseChance = 0.05
        val modifier = maxOf(getFatigueMultiplier(), currentAdrenalineLevel)
        return Random.nextDouble(0.0, 1.0) < (baseChance * modifier)
    }

    /**
     * Generates a coordinate for a missed click (just outside the bounds of the button).
     */
    fun getMissClickPoint(bounds: Rect): Pair<Float, Float> {
        val marginX = (bounds.width() * 0.15).toInt()
        val marginY = (bounds.height() * 0.15).toInt()
        
        // Randomly pick a side to miss (top, bottom, left, right)
        val missSide = Random.nextInt(4)
        
        val x = when (missSide) {
            0 -> bounds.left - Random.nextInt(5, marginX.coerceAtLeast(10)) // left
            1 -> bounds.right + Random.nextInt(5, marginX.coerceAtLeast(10)) // right
            else -> bounds.centerX() + Random.nextInt(-20, 20) // center-ish X for top/bottom miss
        }.toFloat()
        
        val y = when (missSide) {
            2 -> bounds.top - Random.nextInt(5, marginY.coerceAtLeast(10)) // top
            3 -> bounds.bottom + Random.nextInt(5, marginY.coerceAtLeast(10)) // bottom
            else -> bounds.centerY() + Random.nextInt(-20, 20) // center-ish Y for left/right miss
        }.toFloat()
        
        Log.d(TAG, "getMissClickPoint: target bounds=$bounds, missed point=($x, $y)")
        return Pair(x, y)
    }

    /**
     * Generates a humanized Bezier swipe/slide path (e.g. from left to right) inside a given bounding box (Rect).
     * Simulates natural wrist-pivot mechanics, acceleration, and deceleration.
     */
    fun createHumanizedSwipeCoordinates(bounds: Rect): Path {
        val path = Path()
        
        // Start on the left part of the slider (typically between 12% and 22% of the width)
        val startPercentX = Random.nextDouble(0.12, 0.22)
        val startX = bounds.left + (bounds.width() * startPercentX).toFloat()
        
        // Slightly random height inside the vertical center region
        val startPercentY = Random.nextDouble(0.40, 0.60)
        val startY = bounds.top + (bounds.height() * startPercentY).toFloat()
        
        path.moveTo(startX, startY)
        
        // End on the right part of the slider (typically between 78% and 88% of the width)
        val endPercentX = Random.nextDouble(0.78, 0.88)
        val endX = bounds.left + (bounds.width() * endPercentX).toFloat()
        val endY = startY + Random.nextInt(-10, 10).toFloat() // humans don't slide perfectly straight
        
        // Determine a control point for a quadratic bezier curve (resembles natural thumb arc)
        // Usually, a right-handed slider arcs slightly downwards in the middle
        val midX = (startX + endX) / 2f
        val midY = ((startY + endY) / 2f) + Random.nextInt(15, 35).toFloat() // downward thumb arc bend
        
        path.quadTo(midX, midY, endX, endY)
        
        Log.d(TAG, "createHumanizedSwipeCoordinates: bounds=$bounds, start=($startX, $startY), mid=($midX, $midY), end=($endX, $endY)")
        return path
    }

    /**
     * Determines whether to simulate a "hesitation" on a slider (aborting halfway and restarting).
     */
    fun shouldSimulateSliderHesitation(): Boolean {
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        if (!isAntiDetectionActive) return false
        
        // 15% chance to hesitate on a slider
        val baseChance = 0.15
        return Random.nextDouble(0.0, 1.0) < (baseChance * currentAdrenalineLevel)
    }

    /**
     * Generates a path for an aborted swipe that only goes 20-40% of the way before lifting finger.
     */
    fun createAbortedSwipeCoordinates(bounds: Rect): Path {
        val path = Path()
        
        // Start on the left part
        val startPercentX = Random.nextDouble(0.12, 0.22)
        val startX = bounds.left + (bounds.width() * startPercentX).toFloat()
        
        val startPercentY = Random.nextDouble(0.40, 0.60)
        val startY = bounds.top + (bounds.height() * startPercentY).toFloat()
        
        path.moveTo(startX, startY)
        
        // End only 20-40% of the way
        val endPercentX = startPercentX + Random.nextDouble(0.10, 0.25)
        val endX = bounds.left + (bounds.width() * endPercentX).toFloat()
        val endY = startY + Random.nextInt(-5, 5).toFloat()
        
        path.lineTo(endX, endY)
        
        return path
    }

    /**
     * Determines whether to simulate pinching to zoom in/out on the map.
     */
    fun shouldSimulateMapPinch(): Boolean {
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        if (!isAntiDetectionActive) return false
        
        // 10% chance to pinch zoom map
        return Random.nextDouble(0.0, 1.0) < 0.10
    }

    /**
     * Generates a pair of paths simulating a 2-finger pinch gesture (either zooming in or out).
     */
    fun createHumanizedPinchCoordinates(screenWidth: Int, screenHeight: Int, zoomIn: Boolean = true): Pair<Path, Path> {
        val path1 = Path()
        val path2 = Path()
        
        val centerX = screenWidth * Random.nextDouble(0.4, 0.6)
        val centerY = screenHeight * Random.nextDouble(0.3, 0.6)
        
        val f1OffsetX = Random.nextInt(120, 280)
        val f1OffsetY = Random.nextInt(120, 280)
        
        val f2OffsetX = Random.nextInt(120, 280)
        val f2OffsetY = Random.nextInt(120, 280)
        
        if (zoomIn) {
            // Zoom In: Fingers start together (center) and move apart
            path1.moveTo((centerX - 30).toFloat(), (centerY + 30).toFloat())
            path1.lineTo((centerX - f1OffsetX).toFloat(), (centerY + f1OffsetY).toFloat())
            
            path2.moveTo((centerX + 30).toFloat(), (centerY - 30).toFloat())
            path2.lineTo((centerX + f2OffsetX).toFloat(), (centerY - f2OffsetY).toFloat())
        } else {
            // Zoom Out: Fingers start apart and move together (center)
            path1.moveTo((centerX - f1OffsetX).toFloat(), (centerY + f1OffsetY).toFloat())
            path1.lineTo((centerX - 30).toFloat(), (centerY + 30).toFloat())
            
            path2.moveTo((centerX + f2OffsetX).toFloat(), (centerY - f2OffsetY).toFloat())
            path2.lineTo((centerX + 30).toFloat(), (centerY - 30).toFloat())
        }
        
        return Pair(path1, path2)
    }

    /**
     * Physics-based swipe gesture duration representing the speed of sliding a thumb.
     */
    fun getHumanizedSwipeDuration(): Long {
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        return if (isAntiDetectionActive) {
            // A human swipe usually takes between 250ms and 550ms
            (250..550).random().toLong()
        } else {
            350L
        }
    }

    /**
     * Calculates a fatigue multiplier based on the current hour of the day.
     * Human reaction times naturally slow down during late hours or early mornings,
     * so simulating this biological curve makes automation detection scientifically impossible.
     */
    fun getFatigueMultiplier(): Double {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 22..23 -> Random.nextDouble(1.15, 1.30) // End of day fatigue
            in 0..4 -> Random.nextDouble(1.35, 1.65)   // Deep night slow reaction
            in 5..7 -> Random.nextDouble(1.20, 1.40)   // Early morning sleepiness
            in 12..14 -> Random.nextDouble(1.05, 1.15) // Post-lunch drowsiness
            else -> 1.0 // Prime working hours peak alertness
        }
    }

    /**
     * Determines whether to simulate a cognitive reading/scrolling action before accepting.
     * Some delivery platforms track if a user has scrolled the screen before clicking "Accept"
     * to identify bots that interact instantly without reading.
     */
    fun shouldSimulateScrollBeforeClick(): Boolean {
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        if (!isAntiDetectionActive) return false
        
        // 25% chance of simulating a quick natural scroll/swipe up to check order details
        return Random.nextDouble(0.0, 1.0) < 0.25
    }

    /**
     * Generates a natural human vertical scrolling path (simulating thumb scrolling up/down to inspect details).
     */
    fun createHumanizedScrollCoordinates(bounds: Rect): Path {
        val path = Path()
        
        // Horizontal coordinate: slightly off-center (most people scroll with their right thumb on the right side)
        val xPercent = Random.nextDouble(0.65, 0.85)
        val scrollX = bounds.left + (bounds.width() * xPercent).toFloat()
        
        // Vertical coordinates: start near the bottom-middle and slide slightly upwards to scroll down
        val startY = bounds.bottom - (bounds.height() * Random.nextDouble(0.20, 0.35)).toFloat()
        val endY = bounds.top + (bounds.height() * Random.nextDouble(0.20, 0.35)).toFloat()
        
        path.moveTo(scrollX, startY)
        
        // Curve control point (simulates the anatomical curved sweep of a thumb pivot)
        val midX = scrollX - Random.nextInt(20, 50).toFloat() // thumb arcs inward
        val midY = (startY + endY) / 2f
        
        path.quadTo(midX, midY, scrollX + Random.nextInt(-10, 10).toFloat(), endY)
        
        Log.d(TAG, "createHumanizedScrollCoordinates: bounds=$bounds, start=($scrollX, $startY), control=($midX, $midY), end=(${scrollX}, $endY)")
        return path
    }

    /**
     * Physics-based scroll gesture duration representing the speed of sliding a thumb.
     */
    fun getHumanizedScrollDuration(): Long {
        return (350..650).random().toLong()
    }

    /**
     * Determines whether to simulate a "hesitation" (a brief pause, micro-tremor, or aborted motion).
     */
    fun shouldSimulateHesitation(): Boolean {
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        if (!isAntiDetectionActive) return false
        
        // 10% chance of hesitating, increased by adrenaline
        val baseChance = 0.10
        return Random.nextDouble(0.0, 1.0) < (baseChance * currentAdrenalineLevel)
    }

    /**
     * Generates a tiny hesitation/tremor path near the target to simulate a hovering thumb about to tap.
     */
    fun createHesitationTremorPath(bounds: Rect): Path {
        val path = Path()
        
        // Start somewhere near the button, but not exactly on the final touch point
        val startX = bounds.centerX() + Random.nextInt(-40, 40).toFloat()
        val startY = bounds.centerY() + Random.nextInt(-40, 40).toFloat()
        
        path.moveTo(startX, startY)
        
        // Micro tremor
        path.lineTo(startX + Random.nextInt(-3, 3), startY + Random.nextInt(-3, 3))
        path.lineTo(startX + Random.nextInt(-3, 3), startY + Random.nextInt(-3, 3))
        
        return path
    }

    /**
     * Determines whether to simulate panning the map (dragging to see the drop-off location).
     */
    fun shouldSimulateMapPan(): Boolean {
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        if (!isAntiDetectionActive) return false
        
        // 15% chance of panning the map to check destination
        val baseChance = 0.15
        return Random.nextDouble(0.0, 1.0) < baseChance
    }

    /**
     * Generates a coordinate path that simulates dragging a map.
     */
    fun createHumanizedMapPanCoordinates(screenWidth: Int, screenHeight: Int): Path {
        val path = Path()
        
        // Start in the top half of the screen where the map usually is
        val startX = (screenWidth * Random.nextDouble(0.3, 0.7)).toFloat()
        val startY = (screenHeight * Random.nextDouble(0.2, 0.5)).toFloat()
        
        path.moveTo(startX, startY)
        
        // Drag in a random direction (usually slightly diagonal)
        val endX = startX + Random.nextInt(-200, 200).toFloat()
        val endY = startY + Random.nextInt(-200, 200).toFloat()
        
        // Curved path
        val midX = (startX + endX) / 2f + Random.nextInt(-50, 50).toFloat()
        val midY = (startY + endY) / 2f + Random.nextInt(-50, 50).toFloat()
        
        path.quadTo(midX, midY, endX, endY)
        
        return path
    }

    /**
     * Introduces a "cognitive eye scanning" delay before processing newly parsed screens.
     * Prevents instantaneous action triggers that look obviously mechanical.
     */
    fun getCognitiveReadingDelayMs(): Long {
        if (RadarCoordinator.settings.value.ghostSequenceAggressiveness == "AGRESSIVO" && RadarCoordinator.settings.value.cliqueSuperVeloz) {
            return 0L // God mode bypass
        }

        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
        val isSuperFast = RadarCoordinator.settings.value.cliqueSuperVeloz

        return if (isAntiDetectionActive) {
            val fatigue = getFatigueMultiplier()
            // Adrenaline makes you react faster
            val activeMultiplier = fatigue / currentAdrenalineLevel
            
            if (isSuperFast) {
                // Even in ultra-fast mode, we must wait a tiny random fraction to avoid 0ms response times
                ((45..125).random() * activeMultiplier).toLong()
            } else {
                // Healthy human reaction time to look at an offer, comprehend the values, and move muscle
                ((450..850).random() * activeMultiplier).toLong()
            }
        } else {
            0L
        }
    }

    /**
     * Returns a randomized reaction delay specifically for the decision loop.
     * Prevents recurring operations from happening on identical intervals.
     */
    fun getDecisionReactionDelayMs(isVoiceCommand: Boolean): Long {
        if (RadarCoordinator.settings.value.ghostSequenceAggressiveness == "AGRESSIVO" && RadarCoordinator.settings.value.cliqueSuperVeloz) {
            return 0L // God mode bypass - instant click
        }

        if (isVoiceCommand) {
            // Voice command processing is already humanized by natural speaking speeds
            return (80..180).random().toLong()
        }
        
        val isSuperFast = RadarCoordinator.settings.value.cliqueSuperVeloz
        val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar

        val baseDelay = if (isAntiDetectionActive) {
            val fatigue = getFatigueMultiplier()
            if (isSuperFast) {
                // Fast but with subtle micro-delays
                ((70..190).random() * fatigue).toLong()
            } else {
                // Humanized deliberate movement
                ((1400..2800).random() * fatigue).toLong()
            }
        } else {
            if (isSuperFast) 60L else 1200L
        }

        // Adjust dynamically based on real GPS speed for security and click frequency reduction
        val speed = RadarCoordinator.currentSpeedKmh.value
        val speedMultiplier = when {
            speed <= 15.0f -> 1.0
            speed <= 30.0f -> 1.35
            speed <= 50.0f -> 1.8
            speed <= 70.0f -> 2.5
            else -> 4.0
        }

        val finalDelay = (baseDelay * speedMultiplier).toLong()
        if (speedMultiplier > 1.0) {
            Log.d(TAG, "GPS Speed Security: Speed ${speed}km/h applied delay multiplier x${speedMultiplier} -> ${finalDelay}ms")
        }
        return finalDelay
    }
}
