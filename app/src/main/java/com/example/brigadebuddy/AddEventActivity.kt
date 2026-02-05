package com.example.brigadebuddy

import android.R
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.brigadebuddy.databinding.ActivityAddEventBinding
import com.example.brigadebuddy.db.AppDatabase
import com.example.brigadebuddy.alarm.DailyAlarmScheduler
import com.example.brigadebuddy.db.EventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


class AddEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEventBinding

    private val db by lazy { AppDatabase.getInstance(this) }
    private val eventDao by lazy { db.eventDao() }

    private var editingEventId: Long? = null

    private var selectedDay: Int = 0
    private var selectedMonth: Int = 0   // 1–12
    private var selectedYear: Int = 0

    private val eventTypes = listOf("BIRTHDAY", "ANNIVERSARY", "OTHER")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true);
        editingEventId = intent.getLongExtra("event_id", -1L).takeIf { it != -1L }


        setupEventTypeSpinner()
        setupDatePicker()
        setupSaveButton()

        if (editingEventId != null) {
            loadEventForEdit(editingEventId!!)
            title = "Edit Occasion"
        } else {
            title = "Add Occasion"
        }
    }

    private fun setupEventTypeSpinner() {
        val adapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            eventTypes
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerEventType.adapter = adapter
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val cal = Calendar.getInstance()

            val initialYear = if (selectedYear != 0) selectedYear else cal.get(Calendar.YEAR)
            val initialMonth = if (selectedMonth != 0) selectedMonth - 1 else cal.get(Calendar.MONTH)
            val initialDay = if (selectedDay != 0) selectedDay else cal.get(Calendar.DAY_OF_MONTH)

            val dialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDay = dayOfMonth
                    selectedMonth = month + 1
                    selectedYear = year
                    binding.etDate.setText(
                        String.format(
                            "%02d-%02d-%04d",
                            selectedDay,
                            selectedMonth,
                            selectedYear
                        )
                    )
                },
                initialYear,
                initialMonth,
                initialDay
            )
            dialog.show()
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveEvent()
        }
    }

    private fun saveEvent() {
        val Firstname = binding.etName.text.toString().trim()
        val Lastname = binding.etLastName.text.toString().trim()
        val rank = binding.etRank.text.toString().trim()
        val titleText = binding.etTitle.text.toString().trim()
        val detailsText = binding.etDetails.text.toString().trim()
        val eventType = eventTypes.getOrNull(binding.spinnerEventType.selectedItemPosition) ?: "OTHER"

        if (Firstname.isEmpty() || rank.isEmpty() || titleText.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedYear == 0 || selectedMonth == 0 || selectedDay == 0) {
            Toast.makeText(this, "Please select a valid date", Toast.LENGTH_SHORT).show()
            return
        }

        val event = EventEntity(
            id = editingEventId ?: 0L,
            firstname = Firstname,
            lastname=Lastname,
            rank = rank,
            eventType = eventType,
            day = selectedDay,
            month = selectedMonth,
            year = selectedYear,
            title = titleText,
            details = detailsText.ifBlank { null }
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (editingEventId == null) {
                    eventDao.insertEvent(event)
                } else {
                    eventDao.updateEvent(event)
                }
            }
            DailyAlarmScheduler.scheduleDaily8AM(this@AddEventActivity)
            finish()
        }
    }

    private fun loadEventForEdit(id: Long) {
        lifecycleScope.launch {
            val event = withContext(Dispatchers.IO) {
                eventDao.getEventById(id)
            } ?: return@launch

            binding.etName.setText(event.firstname)
            binding.etLastName.setText(event.lastname)
            binding.etRank.setText(event.rank)
            binding.etTitle.setText(event.title)
            binding.etDetails.setText(event.details ?: "")

            selectedDay = event.day
            selectedMonth = event.month
            selectedYear = event.year
            binding.etDate.setText(
                String.format("%02d-%02d-%04d", event.day, event.month, event.year)
            )

            // set spinner selection based on eventType
            val index = eventTypes.indexOf(event.eventType)
            if (index >= 0) {
                binding.spinnerEventType.setSelection(index)
            }
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.home -> {
                // Handle the back button press (navigate up)
                onBackPressed() // Or use NavUtils.navigateUpFromSameTask(this);
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }
}