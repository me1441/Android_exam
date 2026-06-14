package com.example.calculator.ui // Объявление пакета: класс находится в пакете ui слоя приложения

import android.os.Bundle // Импорт класса Bundle для передачи и сохранения данных при смене состояния Activity
import android.view.View // Импорт класса View для работы с видимостью элементов интерфейса
import android.widget.Toast // Импорт класса Toast для отображения всплывающих уведомлений
import androidx.appcompat.app.AppCompatActivity // Импорт базового класса для Activity с поддержкой ActionBar
import androidx.core.view.GravityCompat // Импорт утилиты для работы с гравитацией DrawerLayout (START/END для RTL)
import androidx.lifecycle.lifecycleScope // Импорт корутин-скоупа, привязанного к жизненному циклу Activity
import androidx.recyclerview.widget.LinearLayoutManager // Импорт менеджера компоновки для вертикального списка RecyclerView
import com.example.calculator.data.HistoryDatabase // Импорт класса базы данных истории вычислений
import com.example.calculator.databinding.ActivityMainBinding // Импорт сгенерированного класса ViewBinding для разметки activity_main.xml
import com.example.calculator.logic.Evaluator // Импорт класса вычислителя математических выражений
import com.example.calculator.model.HistoryItem // Импорт data-класса сущности элемента истории
import kotlinx.coroutines.flow.collectLatest // Импорт функции сбора последних значений из Flow
import kotlinx.coroutines.launch // Импорт функции запуска корутины

class MainActivity : AppCompatActivity() { // Объявление главной Activity, наследующей AppCompatActivity

    private lateinit var binding: ActivityMainBinding // Объявление поля binding типа ActivityMainBinding с отложенной инициализацией
    private val evaluator = Evaluator() // Создание экземпляра класса Evaluator для вычисления выражений
    private var currentExpression = "" // Переменная для хранения текущего математического выражения в виде строки
    private var lastResult = "" // Переменная для хранения последнего полученного результата вычисления
    private lateinit var historyAdapter: HistoryAdapter // Объявление адаптера списка истории с отложенной инициализацией

    companion object { // Объявление статического блока companion object для констант уровня класса
        private const val MAX_DISPLAY_LENGTH = 15 // Константа: максимальное количество символов на дисплее калькулятора
    }

    override fun onCreate(savedInstanceState: Bundle?) { // Переопределение метода создания Activity
        super.onCreate(savedInstanceState) // Вызов родительского метода onCreate для инициализации базовой Activity
        binding = ActivityMainBinding.inflate(layoutInflater) // Инициализация binding через инфляцию XML-разметки
        setContentView(binding.root) // Установка корневого View разметки как содержимого Activity

        setupToolbar() // Вызов метода настройки панели инструментов (Toolbar)
        setupNumberButtons() // Вызов метода настройки обработчиков цифровых кнопок
        setupOperationButtons() // Вызов метода настройки обработчиков кнопок операций (+, −, ×, ÷)
        setupControlButtons() // Вызов метода настройки обработчиков управляющих кнопок (C, %, ., скобки)
        setupNavigationDrawer() // Вызов метода настройки боковой панели навигации (Navigation Drawer)
        setupHistory() // Вызов метода настройки списка истории вычислений
    }

    private fun setupToolbar() { // Метод настройки панели инструментов
        setSupportActionBar(binding.toolbar) // Установка Toolbar в качестве ActionBar для Activity
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Включение отображения кнопки "Назад/Меню" слева в Toolbar
        supportActionBar?.setHomeAsUpIndicator(com.example.calculator.R.drawable.ic_menu) // Установка иконки "гамбургер-меню" вместо стрелки "Назад"
    }

    private fun setupNavigationDrawer() { // Метод настройки боковой панели навигации
        binding.btnMenu.setOnClickListener { // Установка слушателя клика на кнопку меню
            binding.drawerLayout.openDrawer(GravityCompat.START) // Открытие DrawerLayout с левой стороны (START учитывает RTL)
        }
    }

    private fun setupHistory() { // Метод настройки отображения и работы со списком истории
        historyAdapter = HistoryAdapter() // Создание экземпляра адаптера для RecyclerView истории
        binding.recyclerViewHistory.apply { // Применение настроек к RecyclerView через блок apply
            layoutManager = LinearLayoutManager(this@MainActivity) // Установка вертикального линейного менеджера компоновки
            adapter = historyAdapter // Привязка адаптера к RecyclerView
        }

        binding.btnClearHistory.setOnClickListener { // Установка слушателя клика на кнопку очистки истории
            lifecycleScope.launch { // Запуск корутины в скоупе жизненного цикла Activity
                HistoryDatabase.getDatabase(this@MainActivity).historyDao().clearAll() // Вызов DAO для удаления всех записей из таблицы истории
            }
        }

        lifecycleScope.launch { // Запуск корутины для наблюдения за изменениями в базе данных
            HistoryDatabase.getDatabase(this@MainActivity) // Получение экземпляра базы данных
                .historyDao() // Получение объекта DAO для работы с историей
                .getAllHistory() // Получение Flow со всеми записями истории
                .collectLatest { history -> // Сбор последних значений из Flow при каждом изменении данных
                    historyAdapter.submitList(history) // Передача нового списка в адаптер с дифф-утилитой
                    binding.tvEmptyHistory.visibility = // Установка видимости текста "История пуста"
                        if (history.isEmpty()) View.VISIBLE else View.GONE // Показать если список пуст, иначе скрыть
                }
        }
    }

    private fun setupNumberButtons() { // Метод настройки цифровых кнопок калькулятора
        val numberButtons = listOf( // Создание списка ссылок на кнопки цифр 0-9
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, // Кнопки 0, 1, 2, 3
            binding.btn4, binding.btn5, binding.btn6, binding.btn7, // Кнопки 4, 5, 6, 7
            binding.btn8, binding.btn9 // Кнопки 8, 9
        )

        numberButtons.forEach { button -> // Перебор каждой кнопки в списке
            button.setOnClickListener { // Установка слушателя клика для текущей кнопки
                if (currentExpression.length < MAX_DISPLAY_LENGTH) { // Проверка: не превышена ли максимальная длина выражения
                    appendToExpression(button.text.toString()) // Добавление цифры с кнопки к текущему выражению
                }
            }
        }
    }

    private fun setupOperationButtons() { // Метод настройки кнопок математических операций
        val operations = mapOf( // Создание карты (Map) сопоставления кнопок и символов операций
            binding.btnAdd to "+", // Кнопка сложения соответствует символу "+"
            binding.btnSubtract to "−", // Кнопка вычитания соответствует символу "−" (минус)
            binding.btnMultiply to "×", // Кнопка умножения соответствует символу "×"
            binding.btnDivide to "÷" // Кнопка деления соответствует символу "÷"
        )

        operations.forEach { (button, operator) -> // Перебор пар "кнопка-оператор" в карте
            button.setOnClickListener { // Установка слушателя клика для кнопки операции
                if (currentExpression.isNotEmpty() && // Проверка: выражение не пустое
                    currentExpression.last().isDigit() // И последний символ является цифрой (запрет двух операторов подряд)
                ) {
                    appendToExpression(operator) // Добавление символа операции к выражению
                }
            }
        }

        binding.btnEquals.setOnClickListener { // Установка слушателя клика на кнопку "равно"
            calculateResult() // Вызов метода вычисления результата
        }
    }

    private fun setupControlButtons() { // Метод настройки управляющих кнопок (C, %, точка, скобки)
        binding.btnClear.setOnClickListener { // Установка слушателя на кнопку очистки (C)
            currentExpression = "" // Очистка текущего выражения
            updateDisplay() // Обновление отображения на дисплее
        }

        binding.btnPercent.setOnClickListener { // Установка слушателя на кнопку процента (%)
            if (currentExpression.isNotEmpty()) { // Проверка: выражение не пустое
                appendToExpression("%") // Добавление символа процента к выражению
            }
        }

        binding.btnDot.setOnClickListener { // Установка слушателя на кнопку десятичной точки
            if (currentExpression.isNotEmpty() && // Проверка: выражение не пустое
                currentExpression.last().isDigit() && // И последний символ — цифра
                !currentExpression.contains(".")) { // И в выражении ещё нет точки (запрет двух точек)
                appendToExpression(".") // Добавление десятичной точки к выражению
            }
        }

        binding.btnParentheses.setOnClickListener { // Установка слушателя на кнопку скобок
            val openCount = currentExpression.count { it == '(' } // Подсчёт открывающих скобок в выражении
            val closeCount = currentExpression.count { it == ')' } // Подсчёт закрывающих скобок в выражении

            val charToAdd = when { // Определение какую скобку добавить через when
                currentExpression.isEmpty() -> "(" // Если выражение пустое — добавить открывающую скобку
                currentExpression.last() == '(' -> "(" // Если последний символ "(" — добавить ещё одну "("
                openCount > closeCount -> ")" // Если открывающих больше закрывающих — добавить ")"
                else -> "(" // В остальных случаях добавить открывающую скобку
            }
            appendToExpression(charToAdd) // Добавление выбранной скобки к выражению
        }
    }

    private fun appendToExpression(value: String) { // Метод добавления символа к текущему выражению
        currentExpression += value // Конкатенация строки: добавление нового символа в конец выражения
        updateDisplay() // Вызов метода обновления отображения на дисплее
    }

    private fun updateDisplay() { // Метод обновления текста на дисплее калькулятора
        binding.tvDisplay.text = if (currentExpression.isEmpty()) "0" else currentExpression // Установка "0" если пусто, иначе выражение
    }

    private fun calculateResult() { // Метод вычисления результата математического выражения
        if (!evaluator.isValidExpression(currentExpression)) { // Проверка валидности выражения через Evaluator
            Toast.makeText(this, "Invalid expression", Toast.LENGTH_SHORT).show() // Показ уведомления о невалидном выражении
            return // Прерывание выполнения метода
        }

        val result = evaluator.evaluate(currentExpression) // Вычисление результата через класс Evaluator

        if (result == Evaluator.ERROR_INVALID_EXPRESSION) { // Проверка: вернул ли Evaluator код ошибки
            Toast.makeText(this, "Error in calculation", Toast.LENGTH_SHORT).show() // Показ уведомления об ошибке вычисления
            return // Прерывание выполнения метода
        }

        lastResult = result // Сохранение результата в переменную последнего результата
        binding.tvDisplay.text = result // Отображение результата на дисплее

        saveToHistory(currentExpression, result) // Сохранение выражения и результата в историю
        currentExpression = result // Установка результата как нового текущего выражения (для дальнейших операций)
    }

    private fun saveToHistory(expression: String, result: String) { // Метод сохранения записи в историю
        lifecycleScope.launch { // Запуск корутины в скоупе жизненного цикла
            val historyItem = HistoryItem( // Создание объекта HistoryItem
                expression = expression, // Установка математического выражения
                result = result // Установка результата вычисления
            )
            HistoryDatabase.getDatabase(this@MainActivity) // Получение экземпляра базы данных
                .historyDao() // Получение DAO
                .insert(historyItem) // Вставка записи в таблицу истории
        }
    }

    override fun onSupportNavigateUp(): Boolean { // Переопределение метода обработки нажатия на кнопку "вверх" в ActionBar
        binding.drawerLayout.openDrawer(GravityCompat.START) // Открытие Navigation Drawer при нажатии на иконку меню
        return true // Возврат true — событие обработано
    }

    @Deprecated("Deprecated in Java") // Аннотация указывающая что метод устарел в Java API
    override fun onBackPressed() { // Переопределение метода нажатия системной кнопки "Назад"
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) { // Проверка: открыт ли Navigation Drawer
            binding.drawerLayout.closeDrawer(GravityCompat.START) // Закрытие Drawer если он открыт
        } else {
            super.onBackPressed() // Вызов родительского метода (стандартное поведение "Назад") если Drawer закрыт
        }
    }
} // Закрывающая фигурная скобка класса MainActivity
