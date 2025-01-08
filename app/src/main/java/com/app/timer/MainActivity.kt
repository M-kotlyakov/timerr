package com.app.timer

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
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

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViewModel()
        setupClickListener()
        saveInputtedText()
        observeTimer()
        observeColorScreenState()
        observeCurrentState()
        observeLeftValue()
    }

    // Обрабатываем нажатие ни кнопку начала старта
    private fun setupClickListener() = with(binding) {
        imageBtnTimer.setOnClickListener {
            if (tvTimerValueAfterStop.visibility == View.VISIBLE && etTimerValue.text.isNotEmpty()) {
                Toast.makeText(this@MainActivity, "Сохраните время таймера", Toast.LENGTH_SHORT).show()
            } else {
                tvTimerValueAfterStop.visibility = View.GONE
                stateTimer = !stateTimer
                if (stateTimer) {
                    val timeInSeconds = etTimerValue.text.toString().toIntOrNull()
                    if (timeInSeconds != null && timeInSeconds > 0) {
                        tvCurrentTimeState.visibility = View.VISIBLE
                        etTimerValue.visibility = View.GONE
                        saveTextBtn.visibility = View.GONE
                        imageBtnTimer.setImageResource(R.drawable.ic_stop_button)
                        viewModel.startTimer(timeInSeconds)
                    } else {
                        Toast.makeText(this@MainActivity, "Введите время таймера", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    etTimerValue.visibility = View.VISIBLE
                    saveTextBtn.visibility = View.VISIBLE
                    imageBtnTimer.setImageResource(R.drawable.ic_play_button)
                    tvCurrentTimeState.visibility = View.GONE
                    viewModel.stopTimer()
                }
            }
        }
    }

    private fun observeTimer() {
        lifecycleScope.launch {
            this@MainActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timeLeft.collect { timer ->
                    with(binding) {
                        if (timer == "00:00") {
                            imageBtnTimer.setImageResource(R.drawable.ic_play_button)
                            etTimerValue.visibility = View.VISIBLE
                            saveTextBtn.visibility = View.VISIBLE
                            tvTimerValue.visibility = View.GONE
                            tvTimerValueAfterStop.visibility = View.VISIBLE
                            tvCurrentTimeState.visibility = View.GONE
                        }
                        tvTimerValue.text = timer
                    }
                }
            }
        }
    }

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

    private fun observeColorScreenState() {
        lifecycleScope.launch {
            this@MainActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.backgroundColor.collect { colorInt ->
                    val backgroundColor = when (colorInt) {
                        0 -> Color.GREEN
                        1 -> Color.YELLOW
                        2 -> Color.RED
                        else -> Color.WHITE
                    }
                    binding.main.setBackgroundColor(backgroundColor)
                }
            }
        }
    }

    private fun saveInputtedText() = with(binding) {
        saveTextBtn.setOnClickListener {
            if (etTimerValue.text.isNotEmpty()) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                // Получаем текущее фокусированное представление
                val view = currentFocus
                // Если есть текущее фокусированное представление, скрываем клавиатуру
                view?.let {
                    imm.hideSoftInputFromWindow(it.windowToken, 0)
                }
                tvTimerValueAfterStop.visibility = View.GONE
                tvTimerValue.visibility = View.VISIBLE
                tvTimerValue.text = viewModel.formatTime(etTimerValue.text.toString().toInt() * 60)
            }
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
    }
}