package com.example.brigadebuddy

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brigadebuddy.databinding.ActivityAddEventBinding
import com.example.brigadebuddy.db.AppDatabase
import com.example.brigadebuddy.db.EventEntity
import com.example.brigadebuddy.model.*
import com.example.brigadebuddy.alarm.DailyAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AddEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEventBinding
    private lateinit var officerForm: OfficerForm
    private lateinit var childAdapter: ChildAdapter

    private val db by lazy { AppDatabase.getInstance(this) }
    private val eventDao by lazy { db.eventDao() }

    private var editingGroupId: Long? = null

    // 🔥 prevents overriding spouse gender repeatedly
    private var spouseGenderAutoSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        officerForm = OfficerForm()

        setupGenderSpinners()
        setupToggles()
        setupDatePickers()
        setupChildrenRecycler()
        setupSave()

        val editId = intent.getLongExtra("event_id", -1)
        if (editId != -1L) loadForEdit(editId)
    }

    // ----------------------------------------------------
    // GENDER SPINNERS + AUTO OPPOSITE LOGIC
    // ----------------------------------------------------

    private fun setupGenderSpinners() {
        val genders = listOf("MALE", "FEMALE")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerGender.adapter = adapter
        binding.spinnerSpouseGender.adapter = adapter

        binding.spinnerGender.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val officerGender =
                        parent?.getItemAtPosition(position).toString()

                    val opposite =
                        if (officerGender == Gender.MALE) Gender.FEMALE else Gender.MALE

                    if (!spouseGenderAutoSet) {
                        val spousePos = if (opposite == Gender.FEMALE) 1 else 0
                        binding.spinnerSpouseGender.setSelection(spousePos)
                        spouseGenderAutoSet = true
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }

    // ----------------------------------------------------

    private fun setupToggles() {
        binding.cbWork.setOnCheckedChangeListener { _, checked ->
            binding.layoutWork.visibility = if (checked) View.VISIBLE else View.GONE
        }

        binding.rgMarital.setOnCheckedChangeListener { _, id ->
            if (id == R.id.rbMarried) {
                binding.layoutSpouse.visibility = View.VISIBLE
                officerForm.isMarried = true
                if (officerForm.spouse == null) officerForm.spouse = SpouseForm()
            } else {
                binding.layoutSpouse.visibility = View.GONE
                officerForm.isMarried = false
            }
        }
    }

    // ----------------------------------------------------

    private fun setupDatePickers() {

        binding.etBirthday.setOnClickListener {
            pickDate { d, m, y ->
                officerForm.birthDay = d
                officerForm.birthMonth = m
                officerForm.birthYear = y
                binding.etBirthday.setText("$d-$m-$y")
            }
        }

        binding.etWorkDate.setOnClickListener {
            pickDate { d, m, y ->
                officerForm.workDay = d
                officerForm.workMonth = m
                officerForm.workYear = y
                binding.etWorkDate.setText("$d-$m-$y")
            }
        }

        binding.etSpouseBirthday.setOnClickListener {
            pickDate { d, m, y ->
                if (officerForm.spouse == null) officerForm.spouse = SpouseForm()
                officerForm.spouse!!.birthDay = d
                officerForm.spouse!!.birthMonth = m
                officerForm.spouse!!.birthYear = y
                binding.etSpouseBirthday.setText("$d-$m-$y")
            }
        }

        binding.etAnniversary.setOnClickListener {
            pickDate { d, m, y ->
                if (officerForm.spouse == null) officerForm.spouse = SpouseForm()
                officerForm.spouse!!.anniversaryDay = d
                officerForm.spouse!!.anniversaryMonth = m
                officerForm.spouse!!.anniversaryYear = y
                binding.etAnniversary.setText("$d-$m-$y")
            }
        }
    }

    private fun pickDate(callback: (Int, Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d -> callback(d, m + 1, y) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ----------------------------------------------------

    private fun setupChildrenRecycler() {
        childAdapter = ChildAdapter(officerForm.children)
        binding.rvChildren.layoutManager = LinearLayoutManager(this)
        binding.rvChildren.adapter = childAdapter

        binding.btnAddChild.setOnClickListener {
            officerForm.children.add(ChildForm())
            childAdapter.notifyItemInserted(officerForm.children.size - 1)
        }
    }

    private fun setupSave() {
        binding.btnSave.setOnClickListener { saveProfile() }
    }

    // ----------------------------------------------------
    // SAVE
    // ----------------------------------------------------

    private fun saveProfile() {

        officerForm.firstName = binding.etName.text.toString().trim()
        officerForm.lastName = binding.etLastName.text.toString().trim()
        officerForm.rank = binding.etRank.text.toString().trim()
        officerForm.gender = binding.spinnerGender.selectedItem.toString()

        if (officerForm.isMarried) {
            if (officerForm.spouse == null) officerForm.spouse = SpouseForm()

            officerForm.spouse!!.name =
                binding.etSpouseName.text.toString().trim()

            officerForm.spouse!!.gender =
                binding.spinnerSpouseGender.selectedItem.toString()
        }

        if (officerForm.firstName.isEmpty() || officerForm.rank.isEmpty()) {
            toast("Fill required fields")
            return
        }

        val groupId = editingGroupId ?: System.currentTimeMillis()
        val events = generateEvents(groupId)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (editingGroupId != null) {
                    eventDao.deleteEventsByGroupId(groupId)
                }
                events.forEach { eventDao.insertEvent(it) }
            }

            DailyAlarmScheduler.scheduleDaily8AM(this@AddEventActivity)
            finish()
        }
    }

    // ----------------------------------------------------
    // EVENT GENERATOR (unchanged)
    // ----------------------------------------------------

    private fun generateEvents(groupId: Long): List<EventEntity> {
        val list = mutableListOf<EventEntity>()
        val officerName = officerForm.firstName
        val rank = officerForm.rank

        list.add(
            EventEntity(
                personGroupId = groupId,
                firstname = officerName,
                lastname = officerForm.lastName,
                rank = rank,
                relationType = RelationType.SELF,
                relatedName = officerName,
                gender = officerForm.gender,
                eventType = "BIRTHDAY",
                day = officerForm.birthDay,
                month = officerForm.birthMonth,
                year = officerForm.birthYear,
                title = "Birthday"
            )
        )

        if (binding.cbWork.isChecked) {
            list.add(
                EventEntity(
                    personGroupId = groupId,
                    firstname = officerName,
                    lastname = officerForm.lastName,
                    rank = rank,
                    relationType = RelationType.WORK,
                    eventType = "ANNIVERSARY",
                    day = officerForm.workDay,
                    month = officerForm.workMonth,
                    year = officerForm.workYear,
                    title = "Work Anniversary"
                )
            )
        }

        officerForm.spouse?.let { spouse ->
            list.add(
                EventEntity(
                    personGroupId = groupId,
                    firstname = officerName,
                    lastname = officerForm.lastName,
                    rank = rank,
                    relationType = RelationType.SPOUSE,
                    relatedName = spouse.name,
                    gender = spouse.gender,
                    eventType = "BIRTHDAY",
                    day = spouse.birthDay,
                    month = spouse.birthMonth,
                    year = spouse.birthYear,
                    title = "Spouse Birthday"
                )
            )

            list.add(
                EventEntity(
                    personGroupId = groupId,
                    firstname = officerName,
                    lastname = officerForm.lastName,
                    rank = rank,
                    relationType = RelationType.ANNIVERSARY,
                    relatedName = spouse.name,
                    eventType = "ANNIVERSARY",
                    day = spouse.anniversaryDay,
                    month = spouse.anniversaryMonth,
                    year = spouse.anniversaryYear,
                    title = "Anniversary"
                )
            )
        }

        officerForm.children.forEach { child ->
            list.add(
                EventEntity(
                    personGroupId = groupId,
                    firstname = officerName,
                    lastname = officerForm.lastName,
                    rank = rank,
                    relationType = RelationType.CHILD,
                    relatedName = child.name,
                    gender = child.gender,
                    eventType = "BIRTHDAY",
                    day = child.birthDay,
                    month = child.birthMonth,
                    year = child.birthYear,
                    title = "Child Birthday"
                )
            )
        }

        return list
    }

    // ----------------------------------------------------
    // EDIT MODE (unchanged)
    // ----------------------------------------------------

    private fun loadForEdit(eventId: Long) {
        lifecycleScope.launch {
            val events = withContext(Dispatchers.IO) {
                val event = eventDao.getEventById(eventId)
                event?.personGroupId?.let { eventDao.getEventsByGroupId(it) }
            } ?: return@launch

            editingGroupId = events.first().personGroupId
            title = "Edit Profile"
            populateFormFromEvents(events)
        }
    }

    private fun populateFormFromEvents(events: List<EventEntity>) {
        val self = events.first { it.relationType == RelationType.SELF }

        officerForm.firstName = self.firstname
        officerForm.lastName = self.lastname
        officerForm.rank = self.rank
        officerForm.gender = self.gender ?: Gender.MALE
        officerForm.birthDay = self.day
        officerForm.birthMonth = self.month
        officerForm.birthYear = self.year

        binding.etName.setText(self.firstname)
        binding.etLastName.setText(self.lastname)
        binding.etRank.setText(self.rank)
        binding.spinnerGender.setSelection(
            if (officerForm.gender == Gender.FEMALE) 1 else 0
        )
        binding.etBirthday.setText("${self.day}-${self.month}-${self.year}")

        events.find { it.relationType == RelationType.WORK }?.let {
            binding.cbWork.isChecked = true
            officerForm.workDay = it.day
            officerForm.workMonth = it.month
            officerForm.workYear = it.year
            binding.etWorkDate.setText("${it.day}-${it.month}-${it.year}")
        }

        val spouseBirthday = events.find { it.relationType == RelationType.SPOUSE }
        val anniversary = events.find { it.relationType == RelationType.ANNIVERSARY }

        if (spouseBirthday != null) {
            binding.rbMarried.isChecked = true
            binding.layoutSpouse.visibility = View.VISIBLE

            officerForm.spouse = SpouseForm(
                name = spouseBirthday.relatedName ?: "",
                gender = spouseBirthday.gender ?: Gender.FEMALE,
                birthDay = spouseBirthday.day,
                birthMonth = spouseBirthday.month,
                birthYear = spouseBirthday.year,
                anniversaryDay = anniversary?.day ?: 0,
                anniversaryMonth = anniversary?.month ?: 0,
                anniversaryYear = anniversary?.year ?: 0
            )

            binding.etSpouseName.setText(officerForm.spouse!!.name)
            binding.spinnerSpouseGender.setSelection(
                if (officerForm.spouse!!.gender == Gender.FEMALE) 1 else 0
            )
            binding.etSpouseBirthday.setText(
                "${spouseBirthday.day}-${spouseBirthday.month}-${spouseBirthday.year}"
            )

            anniversary?.let {
                binding.etAnniversary.setText("${it.day}-${it.month}-${it.year}")
            }
        }

        officerForm.children.clear()
        val children = events.filter { it.relationType == RelationType.CHILD }
        officerForm.children.addAll(
            children.map {
                ChildForm(
                    name = it.relatedName ?: "",
                    gender = it.gender ?: Gender.MALE,
                    birthDay = it.day,
                    birthMonth = it.month,
                    birthYear = it.year
                )
            }
        )
        childAdapter.notifyDataSetChanged()
    }

    // ----------------------------------------------------

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
