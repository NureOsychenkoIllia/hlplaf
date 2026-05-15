package com.example.pract3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pract3.ui.theme.Cloud
import com.example.pract3.ui.theme.Fog
import com.example.pract3.ui.theme.Ink
import com.example.pract3.ui.theme.Moss
import com.example.pract3.ui.theme.Pract3Theme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.sqrt
import kotlin.random.Random

private data class PracticeTab(
    val title: String,
    val level: String,
    val task: String
)

private val tabs = listOf(
    PracticeTab("Вік", "Рівень 1", "Дата народження -> повних років"),
    PracticeTab("Секунди", "Рівень 2", "Від 02.05.366 до н.е. 11:30 до 12:00 заданого дня"),
    PracticeTab("КНП", "Рівень 3", "Камінь, ножиці, папір з комп'ютером"),
    PracticeTab("Prime", "Рівень 4", "Просте число за номером у послідовності")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pract3Theme(dynamicColor = false) {
                PracticeApp()
            }
        }
    }
}

@Composable
private fun PracticeApp() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Fog, Color(0xFFE8EEE7))
                    )
                )
                .padding(innerPadding)
        ) {
            Header()
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Ink,
                contentColor = Color(0xFFEDE4CC)
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TaskIntro(tabs[selectedTab])
                when (selectedTab) {
                    0 -> AgeTask()
                    1 -> SecondsTask()
                    2 -> RockPaperScissorsTask()
                    3 -> PrimeTask()
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = "Практична 3",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Ink
        )
    }
}

@Composable
private fun TaskIntro(tab: PracticeTab) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFBF6EC),
            contentColor = Ink
        ),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(tab.level, color = Color(0xFF8A6A3A), fontWeight = FontWeight.Bold)
            Text(
                tab.task,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF43514C)
            )
        }
    }
}

@Composable
private fun AgeTask() {
    var birthDate by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("Введіть дату у форматі дд.мм.рррр") }

    TaskCard {
        DateInput("Дата народження", birthDate) { birthDate = it }
        Button(onClick = { result = calculateAgeMessage(birthDate) }) {
            Text("Обчислити вік")
        }
        ResultText(result)
    }
}

@Composable
private fun SecondsTask() {
    var targetDate by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("Формат дати: дд.мм.рррр. Час призначення: 12:00") }

    TaskCard {
        DateInput("Заданий день", targetDate) { targetDate = it }
        Button(onClick = { result = calculateSecondsMessage(targetDate) }) {
            Text("Порахувати секунди")
        }
        ResultText(result)
    }
}

@Composable
private fun RockPaperScissorsTask() {
    var result by rememberSaveable { mutableStateOf("Оберіть хід") }
    val choices = listOf("Камінь", "Ножиці", "Папір")

    TaskCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choices.forEach { choice ->
                OutlinedButton(
                    onClick = { result = playRound(choice) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(choice)
                }
            }
        }
        ResultText(result)
    }
}

@Composable
private fun PrimeTask() {
    var number by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("Наприклад: 1 -> 2, 5 -> 11") }

    TaskCard {
        OutlinedTextField(
            value = number,
            onValueChange = { number = it.filter(Char::isDigit) },
            label = { Text("Номер простого числа") },
            placeholder = { Text("5") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = appTextFieldColors()
        )
        Button(onClick = { result = nthPrimeMessage(number) }) {
            Text("Знайти")
        }
        ResultText(result)
    }
}

@Composable
private fun TaskCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Cloud.copy(alpha = 0.96f)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun DateInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '.' }.take(10)) },
        label = { Text(label) },
        placeholder = { Text("02.05.2026") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        colors = appTextFieldColors()
    )
}

@Composable
private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    cursorColor = Ink,
    focusedBorderColor = Color(0xFF7D8F85),
    unfocusedBorderColor = Color(0xFF9BA79F),
    focusedLabelColor = Color(0xFF607068),
    unfocusedLabelColor = Color(0xFF74837B),
    focusedPlaceholderColor = Color(0xFF97A39C),
    unfocusedPlaceholderColor = Color(0xFFA8B2AC)
)

@Composable
private fun ResultText(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Moss,
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink
        )
    }
}

private fun calculateAgeMessage(rawDate: String): String {
    val birth = parseDate(rawDate) ?: return "Некоректна дата. Приклад: 15.05.2004"
    val today = Calendar.getInstance()
    if (birth.after(today)) return "Дата народження не може бути в майбутньому."

    var years = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    val hadBirthdayThisYear = today.get(Calendar.DAY_OF_YEAR) >= birth.get(Calendar.DAY_OF_YEAR)
    if (!hadBirthdayThisYear) years--

    return "Повних років: $years"
}

private fun calculateSecondsMessage(rawDate: String): String {
    val target = parseDate(rawDate, TimeZone.getTimeZone("UTC")) ?: return "Некоректна дата. Приклад: 15.05.2026"
    target.set(Calendar.HOUR_OF_DAY, 12)
    target.set(Calendar.MINUTE, 0)
    target.set(Calendar.SECOND, 0)
    target.set(Calendar.MILLISECOND, 0)

    val start = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        gregorianChange = Date(Long.MIN_VALUE)
        clear()
        set(Calendar.ERA, GregorianCalendar.BC)
        set(Calendar.YEAR, 366)
        set(Calendar.MONTH, Calendar.MAY)
        set(Calendar.DAY_OF_MONTH, 2)
        set(Calendar.HOUR_OF_DAY, 11)
        set(Calendar.MINUTE, 30)
    }

    val seconds = (target.timeInMillis - start.timeInMillis) / 1000
    return "Минуло секунд: ${"%,d".format(Locale.US, seconds)}"
}

private fun playRound(userChoice: String): String {
    val computerChoice = listOf("Камінь", "Ножиці", "Папір").random(Random.Default)
    val outcome = when {
        userChoice == computerChoice -> "Нічия"
        userChoice == "Камінь" && computerChoice == "Ножиці" -> "Ви перемогли"
        userChoice == "Ножиці" && computerChoice == "Папір" -> "Ви перемогли"
        userChoice == "Папір" && computerChoice == "Камінь" -> "Ви перемогли"
        else -> "Переміг комп'ютер"
    }
    return "Ваш хід: $userChoice\nКомп'ютер: $computerChoice\n$outcome"
}

private fun nthPrimeMessage(rawNumber: String): String {
    val n = rawNumber.toIntOrNull() ?: return "Введіть додатне ціле число."
    if (n <= 0) return "Номер має бути більшим за нуль."
    if (n > 20_000) return "Для показового застосунку обмежимося n <= 20000."

    var count = 0
    var candidate = 1
    while (count < n) {
        candidate++
        if (candidate.isPrime()) count++
    }
    return "$n-е просте число: $candidate"
}

private fun Int.isPrime(): Boolean {
    if (this < 2) return false
    if (this == 2) return true
    if (this % 2 == 0) return false
    val limit = sqrt(toDouble()).toInt()
    var divisor = 3
    while (divisor <= limit) {
        if (this % divisor == 0) return false
        divisor += 2
    }
    return true
}

private fun parseDate(rawDate: String, timeZone: TimeZone = TimeZone.getDefault()): GregorianCalendar? {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.US).apply {
        isLenient = false
        this.timeZone = timeZone
    }
    val date = runCatching { formatter.parse(rawDate) }.getOrNull() ?: return null
    return GregorianCalendar(timeZone).apply { time = date }
}

@Preview(showBackground = true)
@Composable
private fun PracticeAppPreview() {
    Pract3Theme(dynamicColor = false) {
        PracticeApp()
    }
}
