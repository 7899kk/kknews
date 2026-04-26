package com.example.expensetracker

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.data.ExpenseEntity
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.viewmodel.DashboardViewModel
import com.example.expensetracker.viewmodel.DashboardViewModelFactory
import com.example.expensetracker.viewmodel.ExpenseViewModel
import com.example.expensetracker.viewmodel.ExpenseViewModelFactory
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExpenseTrackerTheme { ExpenseApp() } }
    }
}

@Composable
fun ExpenseApp() {
    val context = LocalContext.current
    val app = context.applicationContext as ExpenseTrackerApp
    val navController = rememberNavController()
    val expenseVm: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(app.repository))
    val dashboardVm: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(app.repository))
    val scope = rememberCoroutineScope()
    val smsState by app.settingsRepository.smsTrackingEnabled.collectAsState(initial = true)

    NavHost(navController = navController, startDestination = "onboarding") {
        composable("onboarding") {
            CenterScreen("Expense Tracker", "Track your expenses automatically using UPI transaction SMS.", "Continue") {
                navController.navigate("permission")
            }
        }
        composable("permission") {
            PermissionScreen(
                onDone = { navController.navigate("dashboard") },
                onSkip = { navController.navigate("dashboard") }
            )
        }
        composable("dashboard") {
            DashboardScreen(dashboardVm, { navController.navigate("add") }, { navController.navigate("transactions") }, { navController.navigate("settings") })
        }
        composable("add") {
            AddExpenseScreen { amount, category, note, merchant ->
                expenseVm.addManualExpense(amount, category, note, System.currentTimeMillis(), merchant)
                navController.popBackStack()
            }
        }
        composable("transactions") { TransactionsScreen(expenseVm) }
        composable("settings") {
            val expenses by expenseVm.expenses.collectAsState()
            SettingsScreen(
                smsEnabled = smsState,
                onSmsToggle = { scope.launch { app.settingsRepository.setSmsTracking(it) } },
                onExport = { exportCsv(context, expenses) },
                onClear = expenseVm::clearAll
            )
        }
    }
}

@Composable
fun CenterScreen(title: String, text: String, primaryLabel: String, onPrimary: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp)); Text(text); Spacer(Modifier.height(24.dp)); Button(onClick = onPrimary) { Text(primaryLabel) }
    }
}

@Composable
fun PermissionScreen(onDone: () -> Unit, onSkip: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onDone() }
    Box(Modifier.fillMaxSize()) {
        CenterScreen("SMS Permission", "To automatically track your UPI expenses, this app needs permission to read transaction SMS. Your data stays on your phone and is never shared.", "Allow Permission") {
            launcher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
        }
        Text("Skip (Manual Entry Only)", Modifier.align(Alignment.BottomCenter).padding(24.dp).clickable(onClick = onSkip), color = Color(0xFF2563EB))
    }
}

@Composable
fun DashboardScreen(dashboardVm: DashboardViewModel, onAdd: () -> Unit, onAllTransactions: () -> Unit, onSettings: () -> Unit) {
    val monthly by dashboardVm.monthlyTotal.collectAsState()
    val today by dashboardVm.todayTotal.collectAsState()
    val expenses by dashboardVm.expenses.collectAsState()
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, null) } },
        topBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween) {
                Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, null) }
            }
        }
    ) { p ->
        LazyColumn(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SummaryCard("Total this month", "₹%.2f".format(monthly)) }
            item { SummaryCard("Today's spending", "₹%.2f".format(today)) }
            item { MiniBarChart(expenses) }
            item { CategorySummary(expenses) }
            item { Text("Recent transactions", fontWeight = FontWeight.SemiBold) }
            items(expenses.take(10)) { ExpenseRow(it) }
            item { Text("See all", color = Color(0xFF2563EB), modifier = Modifier.clickable(onClick = onAllTransactions)) }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String) = Card { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(title); Text(value, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2563EB)) } }

@Composable
fun MiniBarChart(expenses: List<ExpenseEntity>) {
    val grouped = expenses.groupBy { Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth }
    val max = grouped.values.maxOfOrNull { it.sumOf(ExpenseEntity::amount) } ?: 1.0
    Card { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text("Monthly bar chart"); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) { (1..7).forEach { day -> val value = grouped[day]?.sumOf(ExpenseEntity::amount) ?: 0.0; Box(Modifier.weight(1f).height((value / max * 70).toInt().coerceAtLeast(4).dp).background(Color(0x662563EB), RoundedCornerShape(6.dp))) } } } }
}

@Composable
fun CategorySummary(expenses: List<ExpenseEntity>) {
    val cat = expenses.groupBy { it.category }.mapValues { it.value.sumOf(ExpenseEntity::amount) }
    Card { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("Category-wise chart"); cat.forEach { (k, v) -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(k); Text("₹%.0f".format(v)) } } } }
}

@Composable
fun ExpenseRow(expense: ExpenseEntity) {
    Card { Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween) { Column { Text(expense.merchant); Text(expense.category, color = Color.Gray) }; Text("₹%.2f".format(expense.amount), color = Color(0xFF2563EB), fontWeight = FontWeight.Bold) } }
}

@Composable
fun AddExpenseScreen(onSave: (Double, String, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var notes by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    val categories = listOf("Food", "Travel", "Shopping", "Bills", "Other")
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Add Expense", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(amount, { amount = it }, label = { Text("Amount") })
        OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { categories.forEach { Text(it, Modifier.background(if (it == category) Color(0x332563EB) else Color.Transparent, RoundedCornerShape(12.dp)).clickable { category = it }.padding(8.dp)) } }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
        Button(onClick = { amount.toDoubleOrNull()?.let { onSave(it, category, notes, merchant) } }) { Text("Save") }
    }
}

@Composable
fun TransactionsScreen(vm: ExpenseViewModel) {
    val expenses by vm.expenses.collectAsState()
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Transactions", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(query, { query = it }, label = { Text("Search by merchant") })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(expenses.filter { it.merchant.contains(query, true) }) { ExpenseRow(it) } }
    }
}

@Composable
fun SettingsScreen(smsEnabled: Boolean, onSmsToggle: (Boolean) -> Unit, onExport: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Enable / Disable SMS tracking"); Switch(smsEnabled, onCheckedChange = onSmsToggle) }
        Button(onClick = onExport) { Text("Export data (CSV)") }
        Button(onClick = onClear) { Text("Clear all data") }
        Text("App info & privacy note: SMS is processed locally and never uploaded.")
    }
}

private fun exportCsv(context: android.content.Context, expenses: List<ExpenseEntity>) {
    val file = File(context.cacheDir, "expenses.csv")
    file.writeText(buildString {
        appendLine("id,amount,merchant,category,notes,date,source")
        expenses.forEach {
            val d = LocalDate.ofInstant(Instant.ofEpochMilli(it.dateMillis), ZoneId.systemDefault())
            appendLine("${it.id},${it.amount},${it.merchant},${it.category},${it.notes},$d,${it.source}")
        }
    })
}


@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    ExpenseTrackerTheme {
        CenterScreen(
            title = "Expense Tracker",
            text = "Track your expenses automatically using UPI transaction SMS.",
            primaryLabel = "Continue",
            onPrimary = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionPreview() {
    ExpenseTrackerTheme {
        CenterScreen(
            title = "SMS Permission",
            text = "To automatically track your UPI expenses, this app needs permission to read transaction SMS.",
            primaryLabel = "Allow Permission",
            onPrimary = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    ExpenseTrackerTheme {
        val sample = listOf(
            ExpenseEntity(amount = 250.0, merchant = "Swiggy", category = "Food", dateMillis = System.currentTimeMillis()),
            ExpenseEntity(amount = 399.0, merchant = "Amazon", category = "Shopping", dateMillis = System.currentTimeMillis()),
            ExpenseEntity(amount = 180.0, merchant = "Uber", category = "Travel", dateMillis = System.currentTimeMillis())
        )
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Total this month", "₹829.00")
            SummaryCard("Today's spending", "₹829.00")
            MiniBarChart(sample)
            CategorySummary(sample)
            sample.forEach { ExpenseRow(it) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddExpensePreview() {
    ExpenseTrackerTheme {
        AddExpenseScreen(onSave = { _, _, _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    ExpenseTrackerTheme {
        SettingsScreen(
            smsEnabled = true,
            onSmsToggle = {},
            onExport = {},
            onClear = {}
        )
    }
}
