package com.example.calculator.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calculator.data.HistoryDatabase
import com.example.calculator.databinding.ActivityMainBinding
import com.example.calculator.logic.Evaluator
import com.example.calculator.model.HistoryItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val evaluator = Evaluator()
    private var currentExpression = ""
    private var lastResult = ""
    private lateinit var historyAdapter: HistoryAdapter

    companion object {
        private const val MAX_DISPLAY_LENGTH = 15
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNumberButtons()
        setupOperationButtons()
        setupControlButtons()
        setupNavigationDrawer()
        setupHistory()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(com.example.calculator.R.drawable.ic_menu)
    }

    private fun setupNavigationDrawer() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupHistory() {
        historyAdapter = HistoryAdapter()
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }

        binding.btnClearHistory.setOnClickListener {
            lifecycleScope.launch {
                HistoryDatabase.getDatabase(this@MainActivity).historyDao().clearAll()
            }
        }

        lifecycleScope.launch {
            HistoryDatabase.getDatabase(this@MainActivity)
                .historyDao()
                .getAllHistory()
                .collectLatest { history ->
                    historyAdapter.submitList(history)
                    binding.tvEmptyHistory.visibility =
                        if (history.isEmpty()) View.VISIBLE else View.GONE
                }
        }
    }

    private fun setupNumberButtons() {
        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        numberButtons.forEach { button ->
            button.setOnClickListener {
                if (currentExpression.length < MAX_DISPLAY_LENGTH) {
                    appendToExpression(button.text.toString())
                }
            }
        }
    }

    private fun setupOperationButtons() {
        val operations = mapOf(
            binding.btnAdd to "+",
            binding.btnSubtract to "−",
            binding.btnMultiply to "×",
            binding.btnDivide to "÷"
        )

        operations.forEach { (button, operator) ->
            button.setOnClickListener {
                if (currentExpression.isNotEmpty() &&
                    currentExpression.last().isDigit()
                ) {
                    appendToExpression(operator)
                }
            }
        }

        binding.btnEquals.setOnClickListener {
            calculateResult()
        }
    }

    private fun setupControlButtons() {
        binding.btnClear.setOnClickListener {
            currentExpression = ""
            updateDisplay()
        }

        binding.btnPercent.setOnClickListener {
            if (currentExpression.isNotEmpty()) {
                appendToExpression("%")
            }
        }

        binding.btnDot.setOnClickListener {
            if (currentExpression.isNotEmpty() && 
                currentExpression.last().isDigit() &&
                !currentExpression.contains(".")) {
                appendToExpression(".")
            }
        }

        binding.btnParentheses.setOnClickListener {
            val openCount = currentExpression.count { it == '(' }
            val closeCount = currentExpression.count { it == ')' }

            val charToAdd = when {
                currentExpression.isEmpty() -> "("
                currentExpression.last() == '(' -> "("
                openCount > closeCount -> ")"
                else -> "("
            }
            appendToExpression(charToAdd)
        }
    }

    private fun appendToExpression(value: String) {
        currentExpression += value
        updateDisplay()
    }

    private fun updateDisplay() {
        binding.tvDisplay.text = if (currentExpression.isEmpty()) "0" else currentExpression
    }

    private fun calculateResult() {
        if (!evaluator.isValidExpression(currentExpression)) {
            Toast.makeText(this, "Invalid expression", Toast.LENGTH_SHORT).show()
            return
        }

        val result = evaluator.evaluate(currentExpression)

        if (result == Evaluator.ERROR_INVALID_EXPRESSION) {
            Toast.makeText(this, "Error in calculation", Toast.LENGTH_SHORT).show()
            return
        }

        lastResult = result
        binding.tvDisplay.text = result

        saveToHistory(currentExpression, result)
        currentExpression = result
    }

    private fun saveToHistory(expression: String, result: String) {
        lifecycleScope.launch {
            val historyItem = HistoryItem(
                expression = expression,
                result = result
            )
            HistoryDatabase.getDatabase(this@MainActivity)
                .historyDao()
                .insert(historyItem)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        binding.drawerLayout.openDrawer(GravityCompat.START)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
