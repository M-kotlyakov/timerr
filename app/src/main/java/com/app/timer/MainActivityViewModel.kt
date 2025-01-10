package com.app.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {

    private var stopFlag: Boolean = false
    private var leftValue = ""
    private var totalTime = 0

    private val _currentTimer = MutableStateFlow("")
    val currentTimer: StateFlow<String> = _currentTimer

    private val _timerValueAfterStop = MutableStateFlow("")
    val timerValueAfterStop: StateFlow<String> = _timerValueAfterStop

    private val _timeLeft = MutableStateFlow("")
    val timeLeft: StateFlow<String> = _timeLeft

    private val _backgroundColor = MutableStateFlow(android.graphics.Color.WHITE)
    val backgroundColor: StateFlow<Int> = _backgroundColor

    // метод для начала старта таймера, куда отправляются количество минут
    fun startTimer(totalMinutes: Int, criticalTimeInSeconds: Int) {
        val totalSeconds = totalMinutes * 60
        totalTime = totalSeconds
        val firstTime = totalSeconds
        val secondTime = totalSeconds / 2
        val thirdTime = criticalTimeInSeconds * 60
        stopFlag = false

        // запускаем параллельно два таймера
        // отсчет общего таймера
        viewModelScope.launch {
            for (remainingTime in totalSeconds downTo  0) {
                if (stopFlag) return@launch
                when {
                    remainingTime in (secondTime + 1)..firstTime -> _backgroundColor.value = 0
                    remainingTime in (thirdTime + 1) ..< secondTime -> _backgroundColor.value = 1
                    remainingTime in 0..thirdTime -> _backgroundColor.value = 2
                    else -> 3
                }
                _timeLeft.value = formatTime(remainingTime)
                leftValue = formatTime(totalSeconds - remainingTime)
                delay(1000) // задержка в 1 секунду
            }
            _backgroundColor.value = 10
        }

        // отсчет таймера определенного этапа
        viewModelScope.launch {
            startStageTimer(firstTime, secondTime, thirdTime)
        }
    }

    // метод, который запускает таймер для определенного этапа
    private suspend fun startStageTimer(firstTime: Int, secondTime: Int, thirdTime: Int) {
        val stages = listOf(firstTime, secondTime, thirdTime)
        for (stage in stages) {
            val stageCorrect = if (stage != firstTime) convertTimeToSeconds(timeLeft.value) else stage
            for (second in stageCorrect downTo 0) {
                if (stopFlag) return
                _currentTimer.value = "Текущее время этапа ${formatTime(second)}"
                delay(1000) // Задержка в 1 секунду
            }
        }
    }

    // форматируем таймер в удобный вид 10:00
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    // форматируем данные из вида 10:00 в обычное число
    fun convertTimeToSeconds(timeString: String): Int {
        // Разделение строки по символу ":"
        val parts = timeString.split(":")

        // Если строка не соответствует формату, вернем 0
        if (parts.size != 2) {
            return 0
        }

        // Преобразуем минуты и секунды в Int
        val minutes = parts[0].toIntOrNull() ?: return 0
        val seconds = parts[1].toIntOrNull() ?: return 0

        // Конверсия в общее количество секунд
        return minutes * 60 + seconds
    }

    // метод для остановки таймера
    fun stopTimer() {
        viewModelScope.launch {
            stopFlag = true
            _timeLeft.value = formatTime(0)
            _backgroundColor.value = 3
            _timerValueAfterStop.value = "Прошедшее время $leftValue"
        }
    }
}