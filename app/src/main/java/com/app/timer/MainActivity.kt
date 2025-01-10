package com.app.timer

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.timer.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel

    private var stateTimer: Boolean = false

    // главный метод экрана, когда появляется и виден сам экран пользователю.
    // Основной метод для начала экрана в его жизненном цикле
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViewModel()
        setupClickListener()
        saveInputtedText()
        saveCriticalInputtedText()
        observeTimer()
        observeColorScreenState()
        observeCurrentState()
        observeLeftValue()
    }

    // Обрабатываем нажатие ни кнопку начала старта
    private fun setupClickListener() = with(binding) {
        imageBtnTimer.setOnClickListener {
            // Проверка, чтобы при повторном запуске таймера высвечивалось сообщение,
            // что сначала нужно сохранить введеное значение
            if (
                tvTimerValueAfterStop.visibility == View.VISIBLE
                    && etTimerValue.text.isNotEmpty() && etCruicialTimerValue.text.isNotEmpty()
            ) {
                Toast.makeText(this@MainActivity, "Сохраните время таймера", Toast.LENGTH_SHORT).show()
            } else {
                tvTimerValueAfterStop.visibility = View.GONE // скрываем надпись с прошедшим времени при нажатии на старт таймера
                stateTimer = !stateTimer
                if (stateTimer) {
                    // Если stateTimer == true, тогда убираем с экрана форму ввода данных и кнопку
                    // и запускаем таймер
                    if (!isValidTimeFormat(etTimerValue.text.toString()) && !isValidTimeFormat(etCruicialTimerValue.text.toString())) {
                        Toast.makeText(this@MainActivity, "Неверный формат времени. Используйте HH:mm", Toast.LENGTH_SHORT).show()
                    } else {
                        val timeInSeconds = convertTimeToSeconds(etTimerValue.text.toString()) / 60
                        val criticalTimeInSeconds = convertTimeToSeconds(etCruicialTimerValue.text.toString()) / 60

                        if (convertTimeToSeconds(etTimerValue.text.toString()) > 0) { // смотрим, если есть допустимое время для таймера
                            tvCurrentTimeState.visibility = View.VISIBLE
                            etTimerValue.visibility = View.GONE
                            etCruicialTimerValue.visibility = View.GONE
                            saveTextBtn.visibility = View.GONE
                            saveCruicialTextBtn.visibility = View.GONE
                            imageBtnTimer.setImageResource(R.drawable.ic_stop_button)
                            tvCruicialTimerValue.visibility = View.VISIBLE
                            tvTimerValue.visibility = View.VISIBLE
                            viewModel.startTimer(timeInSeconds, criticalTimeInSeconds)
                        } else {
                            // Высвечиваем сообщение, что пользователь ввел время таймера, потому что оно либо пустое, либо равно 0
                            Toast.makeText(this@MainActivity, "Введите время таймера", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    // Если stateTimer == false, тогда таймер закончился и показываем форму для ввода данных
                    // меняем иконку кнопки и останавливаем таймер
                    etTimerValue.visibility = View.VISIBLE
                    etCruicialTimerValue.visibility = View.VISIBLE
                    saveTextBtn.visibility = View.VISIBLE
                    saveCruicialTextBtn.visibility = View.VISIBLE
                    imageBtnTimer.setImageResource(R.drawable.ic_play_button)
                    tvCurrentTimeState.visibility = View.GONE
//                    tvCruicialTimerValue.visibility = View.GONE
                    viewModel.stopTimer()
                }
            }
        }
    }

    // метод, где идет подписка на события. Здесь мы следим за тем, сколько времени остается на таймере
    // то есть в методе startTimer() идет отсчет таймера и отправляется в timeLeft, а на экране это значение всегда обновляется и отображается
    // потому что связь как publisher - subscriber, где subscriber это наш экран, который подписывается через collect
    private fun observeTimer() {
        lifecycleScope.launch {
            this@MainActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timeLeft.collect { timer ->
                    with(binding) {
                        if (timer == "00:00") {
                            imageBtnTimer.setImageResource(R.drawable.ic_play_button)
                            etTimerValue.visibility = View.VISIBLE
                            etCruicialTimerValue.visibility = View.VISIBLE
                            saveTextBtn.visibility = View.VISIBLE
                            saveCruicialTextBtn.visibility = View.VISIBLE
                            tvTimerValue.visibility = View.GONE
                            tvCruicialTimerValue.visibility = View.GONE
                            tvTimerValueAfterStop.visibility = View.VISIBLE
                            tvCurrentTimeState.visibility = View.GONE
                        }
                        tvTimerValue.text = timer
                    }
                }
            }
        }
    }

    // метод, который на экране отображает таймер текущего этапа (зеленый/желтый/красный)
    private fun observeCurrentState() {
        lifecycleScope.launch {
            this@MainActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentTimer.collect { timer ->
                    with(binding) {
                        tvCurrentTimeState.text = timer
                    }
                }
            }
        }
    }

    // метод который отображает значение прошедшего времени после остановки таймера
    private fun observeLeftValue() {
        lifecycleScope.launch {
            this@MainActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timerValueAfterStop.collect { timer ->
                    with(binding) {
                        tvTimerValueAfterStop.text = timer
                    }
                }
            }
        }
    }

    // метод, отвечает за фон экрана при работающем таймере
    private fun observeColorScreenState() {
        lifecycleScope.launch {
            this@MainActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.backgroundColor.collect { colorInt ->
                    val backgroundColor = when (colorInt) {
                        0 -> Color.GREEN
                        1 -> Color.YELLOW
                        2 -> Color.RED // во вью модели должно отправляться с критического времени
                        else -> Color.WHITE
                    }

                    if (
                        backgroundColor == Color.WHITE
                            && binding.etTimerValue.text.isNotEmpty()
                            && binding.etCruicialTimerValue.text.isNotEmpty()
                    ) {
                        binding.tvTimerValueAfterStop.text = "Прошедшее время ${binding.etTimerValue.text}"
                    }

                    if (backgroundColor == Color.RED) {
                        binding.tvCruicialTimerValue.visibility = View.GONE
                    }
                    binding.main.setBackgroundColor(backgroundColor)
                }
            }
        }
    }

    // метод облрабатывает нажатие кнопки "Сохранить", когда вводим количество минут
    private fun saveInputtedText() = with(binding) {
        saveTextBtn.setOnClickListener {
            if (etTimerValue.text.isNotEmpty() && isValidTimeFormat(etTimerValue.text.toString())) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                // Получаем текущее фокусированное представление
                val view = currentFocus
                // Если есть текущее фокусированное представление, скрываем клавиатуру
                view?.let {
                    imm.hideSoftInputFromWindow(it.windowToken, 0) // скрываем клавиатуру при нажатии на кнопку "Сохранить"
                }
                tvTimerValueAfterStop.visibility = View.GONE
                tvTimerValue.visibility = View.VISIBLE
                tvTimerValue.text = etTimerValue.text.toString() /*viewModel.formatTime(etTimerValue.text.toString().toInt() * 60)*/
            } else {
                Toast.makeText(this@MainActivity, "Введите время таймера корректно", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // метод обрабатывает нажатие кнопки "Сохранить", когда вводим критическое время
    private fun saveCriticalInputtedText() = with(binding) {
        saveCruicialTextBtn.setOnClickListener {
            if (etCruicialTimerValue.text.isNotEmpty() && isValidTimeFormat(etCruicialTimerValue.text.toString())) {
                if (convertTimeToSeconds(etCruicialTimerValue.text.toString()) >= convertTimeToSeconds(etTimerValue.text.toString())) {
                    Toast.makeText(this@MainActivity, "Критическое время должно быть меньше времени таймера", Toast.LENGTH_SHORT).show()
                } else {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    // Получаем текущее фокусированное представление
                    val view = currentFocus
                    // Если есть текущее фокусированное представление, скрываем клавиатуру
                    view?.let {
                        imm.hideSoftInputFromWindow(it.windowToken, 0) // скрываем клавиатуру при нажатии на кнопку "Сохранить"
                    }
                    tvTimerValueAfterStop.visibility = View.GONE
                    tvCruicialTimerValue.visibility = View.VISIBLE
                    tvCruicialTimerValue.text = etCruicialTimerValue.text.toString() /*viewModel.formatTime(etCruicialTimerValue.text.toString().toInt() * 60)*/
                }
            } else {
                Toast.makeText(this@MainActivity, "Введите критическое время корректно", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertTimeToSeconds(time: String): Int {
        // Разделяем время на часы и минуты
        val parts = time.split(":")
        val hours = parts[0].toInt() // Часы
        val minutes = parts[1].toInt()// Минуты

        // Конвертируем в секунды
        return (hours * 3600) + (minutes * 60)
    }

    private fun isValidTimeFormat(time: String): Boolean {
        // Регулярное выражение для формата HH:mm
        val regex = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
        return regex.matches(time)
    }

    // метод для инициализации класса MainActivityViewModel
    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
    }
}