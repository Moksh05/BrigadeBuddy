package com.example.brigadebuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.brigadebuddy.databinding.ActivityMainBinding
import com.example.brigadebuddy.db.EventDao
import com.example.brigadebuddy.db.EventEntity
import com.example.brigadebuddy.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.brigadebuddy.alarm.DailyAlarmScheduler
import com.example.brigadebuddy.worker.CheckEventsWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter : RecyclerViewAdapter

    private val db by lazy { AppDatabase.getInstance(this) }
    private val eventDao by lazy { db.eventDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(false);
        maybeRequestNotificationPermission()
        setupRecyclerView()
        setupSwipeToDelete()
        setupFab()
        //fortesting
        binding.btnRunTestCheck.setOnClickListener {
            runTestWorkerOnce()
        }


    }
    //for testing
    private fun runTestWorkerOnce() {
        DailyAlarmScheduler.scheduleTestAlarm(this)
    }


    override fun onResume() {
        super.onResume()
        loadEvents()
    }



    private fun setupRecyclerView() {
        adapter = RecyclerViewAdapter(
            onItemClick = { event ->
                // Open AddEditActivity in edit mode
                val intent = Intent(this, AddEventActivity::class.java)
                intent.putExtra("event_id", event.id)
                startActivity(intent)
            },
            onItemLongClick = { event ->

            }
        )

        binding.rvOccasions.layoutManager = LinearLayoutManager(this)
        binding.rvOccasions.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddOccasion.setOnClickListener {
            // Open AddEditActivity in add mode
            val intent = Intent(this, AddEventActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadEvents() {
        lifecycleScope.launch {
            val events: List<EventEntity> = withContext(Dispatchers.IO) {
                eventDao.getAllActiveEvents()
            }
            adapter.submitList(events)
        }
    }
    private fun deleteEventFromDb(event: EventEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                eventDao.deleteEventsByGroupId(event.personGroupId)
            }
        }
    }
    private fun deleteEvent(event: EventEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                eventDao.deleteEvent(event)
            }
            Toast.makeText(this@MainActivity, "Event deleted", Toast.LENGTH_SHORT).show()
            loadEvents()
        }
    }
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(
                    this,
                    "Notifications are disabled. You won't get occasion reminders., Please Enable from settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }



    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            private val background = ColorDrawable(Color.RED)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // we are not supporting move
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                // Get the event to delete
                val deletedEvent = adapter.currentList[position]

                // Create new list without that item (optimistic UI update)
                val newList = adapter.currentList.toMutableList()
                newList.removeAt(position)
                adapter.submitList(newList)

                // Show Snackbar with UNDO
                val snackbar = Snackbar.make(
                    binding.rootCoordinator,  // make sure your root CoordinatorLayout has this id
                    "Profile deleted",
                    Snackbar.LENGTH_LONG
                )

                snackbar.setAction("UNDO") {
                    // User clicked UNDO → restore in adapter, don't delete from DB
                    val restoredList = adapter.currentList.toMutableList()
                    restoredList.add(position.coerceAtMost(restoredList.size), deletedEvent)
                    adapter.submitList(restoredList)
                }

                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)

                        if (event != DISMISS_EVENT_ACTION) {
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    eventDao.deleteEventsByGroupId(deletedEvent.personGroupId)
                                }
                                loadEvents()   // 🔥 refresh RecyclerView
                            }
                        }
                    }
                })

                snackbar.show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView

                    if (dX > 0) {
                        // Swiping to the right
                        background.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt(),
                            itemView.bottom
                        )
                    } else if (dX < 0) {
                        // Swiping to the left
                        background.setBounds(
                            itemView.right + dX.toInt(),
                            itemView.top,
                            itemView.right,
                            itemView.bottom
                        )
                    } else {
                        background.setBounds(0, 0, 0, 0)
                    }

                    background.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvOccasions)
    }

}