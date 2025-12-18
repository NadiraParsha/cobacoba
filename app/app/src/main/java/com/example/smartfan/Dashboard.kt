package com.example.smartfan

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.example.smartfan.api.ApiClient
import com.example.smartfan.api.FanControlRequest
import com.example.smartfan.api.FanData
import com.example.smartfan.api.SetAutoModeRequest // Import Data Class baru
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Dashboard : AppCompatActivity() {

    // --- DEKLARASI UI ---
    private lateinit var tvTemperatureValue: TextView
    private lateinit var tvHumidityValue: TextView
    private lateinit var tvFanStatus: TextView
    private lateinit var tvAiStatus: TextView
    private lateinit var tvRoomCondition: TextView
    private lateinit var tvSystemFanStatus: TextView
    private lateinit var tvSystemAiStatus: TextView
    private lateinit var tvAiRecommendation: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnTurnOn: Button
    private lateinit var btnTurnOff: Button
    private lateinit var btnAutoMode: Button
    private lateinit var lineChart: LineChart
    private lateinit var historyTableLayout: TableLayout
    private lateinit var connectionStatusIndicator: View

    // --- Variabel Logika ---
    private var isAutoModeEnabled = false
    private var dataFetchingJob: Job? = null
    private val MAX_GRAPH_POINTS = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        initializeViews()
        setupControls()
        setupChart()
        updateAiStatusUi(isAutoModeEnabled)
        fetchHistoryDataFromServer()
    }

    override fun onResume() {
        super.onResume()
        startPeriodicDataFetching()
    }

    override fun onPause() {
        super.onPause()
        stopPeriodicDataFetching()
    }

    private fun initializeViews() {
        tvTemperatureValue = findViewById(R.id.tvTemperatureValue)
        tvHumidityValue = findViewById(R.id.tvHumidityValue)
        tvFanStatus = findViewById(R.id.tvFanStatus)
        tvAiStatus = findViewById(R.id.tvAiStatus)
        tvRoomCondition = findViewById(R.id.tvRoomCondition)
        tvSystemFanStatus = findViewById(R.id.tvSystemFanStatus)
        tvSystemAiStatus = findViewById(R.id.tvSystemAiStatus)
        tvAiRecommendation = findViewById(R.id.tvAiRecommendation)
        btnMenu = findViewById(R.id.btnMenu)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnTurnOn = findViewById(R.id.btnTurnOn)
        btnTurnOff = findViewById(R.id.btnTurnOff)
        btnAutoMode = findViewById(R.id.btnAutoMode)
        lineChart = findViewById(R.id.lineChart)
        historyTableLayout = findViewById(R.id.historyTableLayout)
        connectionStatusIndicator = findViewById(R.id.connectionStatusIndicator)
    }

    private fun setupControls() {
        btnMenu.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view, Gravity.START)
            popupMenu.menuInflater.inflate(R.menu.dashboard_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem -> handleMenuClick(menuItem) }
            popupMenu.show()
        }

        btnRefresh.setOnClickListener {
            Toast.makeText(this, "Memperbarui data...", Toast.LENGTH_SHORT).show()
            fetchFanData()
            fetchHistoryDataFromServer()
        }

        btnTurnOn.setOnClickListener { sendFanCommand("on") }
        btnTurnOff.setOnClickListener { sendFanCommand("off") }

        // --- Logika Tombol Auto Mode Diubah Total ---
        btnAutoMode.setOnClickListener {
            // Balik status mode auto secara lokal terlebih dahulu
            isAutoModeEnabled = !isAutoModeEnabled
            // Update UI berdasarkan state baru
            updateAutoModeButtonState()
            updateAiStatusUi(isAutoModeEnabled)
            // Kirim state baru ke server
            sendAutoModeStatusToServer(isAutoModeEnabled)
        }
        updateAutoModeButtonState()
    }

    private fun updateAutoModeButtonState() {
        btnAutoMode.isSelected = isAutoModeEnabled
        btnTurnOn.isEnabled = !isAutoModeEnabled
        btnTurnOff.isEnabled = !isAutoModeEnabled
        btnTurnOn.alpha = if (isAutoModeEnabled) 0.5f else 1.0f
        btnTurnOff.alpha = if (isAutoModeEnabled) 0.5f else 1.0f
    }

    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.xAxis.textColor = ContextCompat.getColor(this, R.color.white)
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.axisLeft.textColor = ContextCompat.getColor(this, R.color.white)
        lineChart.axisLeft.setDrawGridLines(true)
        lineChart.axisLeft.gridColor = ContextCompat.getColor(this, R.color.text_disabled_color)
        lineChart.axisRight.isEnabled = false
        val initialSet = LineDataSet(null, "Suhu").apply {
            color = ContextCompat.getColor(this@Dashboard, R.color.white)
            setCircleColor(ContextCompat.getColor(this@Dashboard, R.color.white))
            valueTextColor = ContextCompat.getColor(this@Dashboard, R.color.white)
            setDrawValues(false)
        }
        val initialData = LineData(initialSet)
        lineChart.data = initialData
        lineChart.invalidate()
    }

    private fun startPeriodicDataFetching() {
        stopPeriodicDataFetching()
        dataFetchingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                fetchFanData()
                delay(5000)
            }
        }
    }

    private fun fetchFanData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fanData = ApiClient.instance.getFanStatus()
                withContext(Dispatchers.Main) {
                    connectionStatusIndicator.setBackgroundResource(R.drawable.indicator_connected)
                    updateStatusCardsAndPanel(fanData)
                    addLatestDataToChart(fanData)
                    // --- Logika Auto Mode Dihapus Dari Sini ---
                    // Tidak ada lagi pengecekan `if (isAutoModeEnabled)`
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectionStatusIndicator.setBackgroundResource(R.drawable.indicator_disconnected)
                    Log.e("DashboardApi", "Error fetching LATEST data: ${e.message}")
                    showError("Gagal terhubung ke perangkat.")
                }
            }
        }
    }

    private fun fetchHistoryDataFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val historyListFromServer = ApiClient.instance.getSensorHistory()
                withContext(Dispatchers.Main) {
                    connectionStatusIndicator.setBackgroundResource(R.drawable.indicator_connected)
                    updateHistoryTable(historyListFromServer)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectionStatusIndicator.setBackgroundResource(R.drawable.indicator_disconnected)
                    Log.e("DashboardApi", "Error fetching HISTORY data: ${e.message}")
                    Toast.makeText(this@Dashboard, "Gagal memuat riwayat data.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopPeriodicDataFetching() {
        dataFetchingJob?.cancel()
        dataFetchingJob = null
    }

    private fun addLatestDataToChart(latestData: FanData) {
        val data = lineChart.data
        if (data != null) {
            val set = data.getDataSetByIndex(0)
            if (set != null) {
                val newEntry = Entry(set.entryCount.toFloat(), latestData.suhu.toFloat())
                data.addEntry(newEntry, 0)
                data.notifyDataChanged()
                lineChart.notifyDataSetChanged()
                if (set.entryCount > MAX_GRAPH_POINTS) {
                    set.removeFirst()
                }
                lineChart.setVisibleXRangeMaximum(MAX_GRAPH_POINTS.toFloat())
                lineChart.moveViewToX(data.entryCount.toFloat())
            }
        }
    }

    private fun updateHistoryTable(historyList: List<FanData>) {
        historyTableLayout.removeViews(1, historyTableLayout.childCount - 1)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        for (data in historyList.reversed().take(50)) {
            val tableRow = TableRow(this)

            val timeText = TextView(this).apply {
                text = sdf.format(Date(data.timestamp))
                gravity = Gravity.CENTER; setPadding(8, 8, 8, 8)
                setTextColor(ContextCompat.getColor(context, R.color.text_disabled_color))
            }
            val suhuText = TextView(this).apply {
                text = "${data.suhu}°C"
                gravity = Gravity.CENTER; setPadding(8, 8, 8, 8)
                setTextColor(ContextCompat.getColor(context, R.color.text_disabled_color))
            }
            val lembapText = TextView(this).apply {
                text = "${data.kelembapan}%"
                gravity = Gravity.CENTER; setPadding(8, 8, 8, 8)
                setTextColor(ContextCompat.getColor(context, R.color.text_disabled_color))
            }
            val kipasText = TextView(this).apply {
                val isFanOn = data.gas_status.equals("ON", ignoreCase = true)
                text = if (isFanOn) "ON" else "OFF"
                gravity = Gravity.CENTER; setPadding(8, 8, 8, 8)
                setTextColor(ContextCompat.getColor(context, if (isFanOn) R.color.fan_on_color else R.color.fan_off_color))
                typeface = Typeface.DEFAULT_BOLD
            }
            tableRow.addView(timeText)
            tableRow.addView(suhuText)
            tableRow.addView(lembapText)
            tableRow.addView(kipasText)
            historyTableLayout.addView(tableRow)
        }
    }

    private fun updateStatusCardsAndPanel(fanData: FanData) {
        val isFanOn = fanData.gas_status.equals("ON", ignoreCase = true)
        tvTemperatureValue.text = "${fanData.suhu}°C"
        tvHumidityValue.text = "${fanData.kelembapan}%"
        tvFanStatus.text = if (isFanOn) "ON" else "OFF"
        tvFanStatus.setTextColor(ContextCompat.getColor(this, if (isFanOn) R.color.fan_on_color else R.color.fan_off_color))
        tvSystemFanStatus.text = "Status Kipas: ${if (isFanOn) "MENYALA" else "MATI"}"
        val roomCondition: String; val roomColor: Int
        when {
            fanData.suhu > 29.0 -> { roomCondition = "PANAS"; roomColor = R.color.fan_off_color }
            fanData.suhu < 26.0 -> { roomCondition = "SEJUK"; roomColor = R.color.fan_on_color }
            else -> { roomCondition = "NORMAL"; roomColor = R.color.ai_on_color }
        }
        tvRoomCondition.text = "Kondisi Ruangan: $roomCondition"
        tvRoomCondition.setTextColor(ContextCompat.getColor(this, roomColor))
        tvRoomCondition.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(this, roomColor))
    }

    private fun updateAiStatusUi(isAiActive: Boolean) {
        val statusText = if (isAiActive) "AKTIF" else "NONAKTIF"
        val statusColor = ContextCompat.getColor(this, if (isAiActive) R.color.ai_on_color else R.color.text_disabled_color)
        tvAiStatus.text = statusText
        tvAiStatus.setTextColor(statusColor)
        tvSystemAiStatus.text = "Status AI: $statusText"
        tvSystemAiStatus.setTextColor(statusColor)
        tvSystemAiStatus.compoundDrawableTintList = ColorStateList.valueOf(statusColor)
        tvAiRecommendation.text = if (isAiActive) "Sistem akan mengatur kipas secara otomatis."
        else "Klik tombol AUTO untuk kontrol otomatis."
    }

    // --- Fungsi applyAutoModeLogic Dihapus Total ---
    /*
    private fun applyAutoModeLogic(fanData: FanData) {
        // Logika ini sekarang ada di server, jadi fungsi ini tidak lagi diperlukan.
    }
    */

    private fun sendFanCommand(command: String, showToast: Boolean = true) {
        if (showToast) {
            val message = if (command == "on") "Mengirim perintah NYALAKAN..." else "Mengirim perintah MATIKAN..."
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = FanControlRequest(status = command)
                val response = ApiClient.instance.controlFan(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        connectionStatusIndicator.setBackgroundResource(R.drawable.indicator_connected)
                        if (showToast) {
                            val successMessage = if (command == "on") "Kipas BERHASIL dinyalakan" else "Kipas BERHASIL dimatikan"
                            Toast.makeText(this@Dashboard, successMessage, Toast.LENGTH_SHORT).show()
                        }
                        fetchFanData()
                        fetchHistoryDataFromServer()
                    } else {
                        connectionStatusIndicator.setBackgroundResource(R.drawable.indicator_disconnected)
                        Log.e("DashboardApi", "Command failed with code: ${response.code()} - ${response.message()}")
                        Toast.makeText(this@Dashboard, "Gagal mengirim perintah: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectionStatusIndicator.setBackgroundResource(R.drawable.indicator_disconnected)
                    Log.e("DashboardApi", "Command failed: ${e.message}")
                    Toast.makeText(this@Dashboard, "Error: Perintah gagal dikirim.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Fungsi Baru Untuk Mengirim Status Auto Mode ke Server ---
    private fun sendAutoModeStatusToServer(isEnabled: Boolean) {
        val message = if (isEnabled) "Mengaktifkan Mode Auto di Server" else "Menonaktifkan Mode Auto di Server"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = SetAutoModeRequest(auto_mode = isEnabled)
                val response = ApiClient.instance.setAutoMode(request)
                withContext(Dispatchers.Main) {
                    if (!response.isSuccessful) {
                        // Jika gagal, kembalikan state tombol ke posisi semula agar UI konsisten
                        Toast.makeText(this@Dashboard, "Gagal mengubah mode auto di server", Toast.LENGTH_LONG).show()
                        isAutoModeEnabled = !isEnabled
                        updateAutoModeButtonState()
                        updateAiStatusUi(isAutoModeEnabled)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Jika gagal, kembalikan juga state tombol ke posisi semula
                    Toast.makeText(this@Dashboard, "Error koneksi: ${e.message}", Toast.LENGTH_LONG).show()
                    isAutoModeEnabled = !isEnabled
                    updateAutoModeButtonState()
                    updateAiStatusUi(isAutoModeEnabled)
                }
            }
        }
    }


    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        val errorColor = ContextCompat.getColor(this, R.color.text_disabled_color)
        tvTemperatureValue.text = "--°C"; tvHumidityValue.text = "--%"
        tvFanStatus.text = "N/A"; tvFanStatus.setTextColor(errorColor)
        tvAiStatus.text = "N/A"; tvAiStatus.setTextColor(errorColor)
        tvRoomCondition.text = "Kondisi Ruangan: --"
        tvSystemFanStatus.text = "Status Kipas: GAGAL TERHUBUNG"
    }

    private fun handleMenuClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_logout -> { logoutUser(); true }
            else -> false
        }
    }

    private fun logoutUser() {
        stopPeriodicDataFetching()
        // Anda mungkin ingin memberi tahu server bahwa pengguna telah logout
        // CoroutineScope(Dispatchers.IO).launch { ApiClient.instance.logoutUser() } // Contoh
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        Toast.makeText(this, "Anda telah logout", Toast.LENGTH_SHORT).show()
    }
}
