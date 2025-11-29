package com.example.vytal.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.vytal.R
import com.example.vytal.model.Event
import com.example.vytal.model.EventJoiner
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import java.util.Calendar

class EventFragment : Fragment() {

    // ============================================================
    // COMPANION OBJECT - Static data that persists across fragment recreations
    // This is KEY to preventing data reset on tab switches!
    // ============================================================
    companion object {
        private val eventsList = mutableListOf<Event>()
        private val filteredEventsList = mutableListOf<Event>()
        private val joinersMap = mutableMapOf<String, EventJoiner>()
        
        // Cached stats - persist across tab switches
        private var cachedJoinedCount = 0
        private var cachedTotalPoints = 0
        private var cachedMaxStreak = 0
        private var isDataLoaded = false
    }

    // View references
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var tvEmptyStateTitle: TextView
    private lateinit var layoutEmptyState: View
    private lateinit var btnCreateSampleEvents: Button
    private lateinit var btnCreateFirstEvent: Button
    private lateinit var btnRefresh: ImageButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchView: SearchView
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var tvEventCount: TextView
    private lateinit var tvJoinedCount: TextView
    private lateinit var tvTotalPoints: TextView
    private lateinit var tvActiveStreak: TextView
    private lateinit var tvMyEventsCount: TextView
    private lateinit var fabAddEvent: ExtendedFloatingActionButton
    private var btnAddEventTop: com.google.android.material.button.MaterialButton? = null

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Filter state
    private var currentFilterCategory: String? = null
    private var currentSearchQuery: String = ""
    private var showOnlyMyEvents: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_event, container, false)
        
        initViews(view)
        setupListeners()
        
        // IMPORTANT: Restore cached stats immediately to prevent 0 flash
        restoreCachedStats()
        
        // Only fetch from server if we don't have data yet
        if (!isDataLoaded) {
            fetchEvents()
        } else {
            // We have cached data - just refresh the adapter
            filterEvents()
        }
        
        return view
    }

    // ============================================================
    // ON RESUME - Called when returning from EventDetailsActivity
    // This is KEY to refreshing data after logging progress!
    // ============================================================
    override fun onResume() {
        super.onResume()
        // Always refresh joiners when returning (gets latest progress/streak)
        if (isDataLoaded && eventsList.isNotEmpty()) {
            fetchJoiners() // Faster - just refresh join/progress data
        } else {
            fetchEvents() // Full refresh if no data yet
        }
    }

    private fun restoreCachedStats() {
        // Show cached values immediately (prevents 0 flash on tab switch)
        tvJoinedCount.text = cachedJoinedCount.toString()
        tvTotalPoints.text = cachedTotalPoints.toString()
        tvActiveStreak.text = cachedMaxStreak.toString()
        tvMyEventsCount.text = joinersMap.size.toString()
        tvEventCount.text = filteredEventsList.size.toString()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewEvents)
        progressBar = view.findViewById(R.id.progressBarEvents)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        tvEmptyStateTitle = view.findViewById(R.id.tvEmptyStateTitle)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        btnCreateSampleEvents = view.findViewById(R.id.btnCreateSampleEvents)
        btnCreateFirstEvent = view.findViewById(R.id.btnCreateFirstEvent)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        searchView = view.findViewById(R.id.searchViewEvents)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
        tvEventCount = view.findViewById(R.id.tvEventCount)
        tvJoinedCount = view.findViewById(R.id.tvJoinedCount)
        tvTotalPoints = view.findViewById(R.id.tvTotalPoints)
        tvActiveStreak = view.findViewById(R.id.tvActiveStreak)
        tvMyEventsCount = view.findViewById(R.id.tvMyEventsCount)
        fabAddEvent = view.findViewById(R.id.fabAddEvent)
        btnAddEventTop = view.findViewById(R.id.btnAddEventTop)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        swipeRefreshLayout.setColorSchemeColors(
            requireContext().getColor(R.color.colorPrimary),
            requireContext().getColor(R.color.colorSecondary)
        )
    }

    private fun setupListeners() {
        swipeRefreshLayout.setOnRefreshListener { fetchEvents() }
        
        btnRefresh.setOnClickListener { 
            it.animate().rotation(it.rotation + 360f).setDuration(500).start()
            fetchEvents() 
        }
        
        btnCreateSampleEvents.setOnClickListener { createSampleEvents() }
        btnCreateFirstEvent.setOnClickListener { showCreateEventBottomSheet() }
        btnAddEventTop?.setOnClickListener { showCreateEventBottomSheet() }
        fabAddEvent.setOnClickListener { showCreateEventBottomSheet() }

        // Search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText?.lowercase() ?: ""
                filterEvents()
                return true
            }
        })

        // Category filter chips
        chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedChipId = checkedIds.firstOrNull() ?: R.id.chipAll
            showOnlyMyEvents = selectedChipId == R.id.chipMyEvents
            
            currentFilterCategory = when (selectedChipId) {
                R.id.chipAll -> null
                R.id.chipYoga -> "Yoga"
                R.id.chipFitness -> "Fitness"
                R.id.chipDiet -> "Diet"
                R.id.chipWellness -> "Wellness"
                R.id.chipHealth -> "Health"
                R.id.chipMyEvents -> null
                else -> null
            }
            filterEvents()
        }
        
        // Hide/show FAB and bottom nav on scroll
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                try {
                    val activity = activity as? com.example.vytal.MainActivity
                    
                    if (!recyclerView.canScrollVertically(-1)) {
                        // At the top - always show bottom nav
                        activity?.showBottomNavigation()
                    } else if (dy > 0 && recyclerView.canScrollVertically(1)) {
                        // Scrolling down
                        fabAddEvent.shrink()
                        activity?.hideBottomNavigation()
                    } else if (dy < 0) {
                        // Scrolling up
                        fabAddEvent.extend()
                        activity?.showBottomNavigation()
                    }
                } catch (e: Exception) {
                    // Ignore scroll listener errors
                }
            }
        })
        
        // Ensure bottom nav is visible when fragment is first shown
        try {
            (activity as? com.example.vytal.MainActivity)?.showBottomNavigation()
        } catch (e: Exception) {
            // Ignore if activity is not available yet
        }
    }

    private fun showCreateEventBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_create_event, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val etTitle = bottomSheetView.findViewById<TextInputEditText>(R.id.etEventTitle)
        val etDescription = bottomSheetView.findViewById<TextInputEditText>(R.id.etEventDescription)
        val etDuration = bottomSheetView.findViewById<TextInputEditText>(R.id.etDuration)
        val etReward = bottomSheetView.findViewById<TextInputEditText>(R.id.etReward)
        val chipGroupCategory = bottomSheetView.findViewById<ChipGroup>(R.id.chipGroupCategory)
        val btnCreate = bottomSheetView.findViewById<MaterialButton>(R.id.btnCreateEvent)

        // Safety check
        if (etTitle == null || etDuration == null || etReward == null || chipGroupCategory == null || btnCreate == null) {
            Toast.makeText(requireContext(), "Error loading form. Please try again.", Toast.LENGTH_SHORT).show()
            Log.e("EventFragment", "One or more views are null in bottom sheet")
            return
        }

        btnCreate.setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            val description = etDescription?.text?.toString()?.trim() ?: ""
            val durationStr = etDuration.text?.toString()?.trim() ?: "7"
            val rewardStr = etReward.text?.toString()?.trim() ?: "100"

            if (title.isEmpty()) {
                etTitle.error = "Title is required"
                return@setOnClickListener
            }

            val duration = durationStr.toIntOrNull() ?: 7
            val rewardPoints = rewardStr.toIntOrNull() ?: 100

            val selectedCategoryId = chipGroupCategory.checkedChipId
            val category = when (selectedCategoryId) {
                R.id.chipCatYoga -> "Yoga"
                R.id.chipCatFitness -> "Fitness"
                R.id.chipCatDiet -> "Diet"
                R.id.chipCatWellness -> "Wellness"
                R.id.chipCatHealth -> "Health"
                R.id.chipCatCustom -> "Custom"
                else -> "Custom"
            }

            createCustomEvent(title, description, duration, rewardPoints, category, bottomSheetDialog)
        }

        bottomSheetDialog.show()
    }

    private fun createCustomEvent(
        title: String,
        description: String,
        duration: Int,
        rewardPoints: Int,
        category: String,
        dialog: BottomSheetDialog
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login to create events", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        val currentTime = System.currentTimeMillis()

        val event = Event(
            title = title,
            description = description.ifEmpty { "My personal $category challenge" },
            duration = duration,
            reward = "$rewardPoints Points + $category Badge",
            startDate = currentTime,
            endDate = currentTime + (duration.toLong() * 24 * 60 * 60 * 1000),
            category = category,
            imageUrl = "",
            isCustom = true,
            creatorId = currentUser.uid
        )

        db.collection("events")
            .add(event)
            .addOnSuccessListener { documentRef ->
                val joiner = EventJoiner(
                    uid = currentUser.uid,
                    joinDate = currentTime,
                    streak = 0,
                    completedDays = emptyList(),
                    totalPoints = 0,
                    isCompleted = false
                )

                db.collection("events")
                    .document(documentRef.id)
                    .collection("joiners")
                    .document(currentUser.uid)
                    .set(joiner)
                    .addOnSuccessListener {
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "🎉 Challenge created!", Toast.LENGTH_SHORT).show()
                        fetchEvents()
                    }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("EventFragment", "Error creating event", e)
                Log.e("EventFragment", "Error message: ${e.message}")
                Log.e("EventFragment", "Error cause: ${e.cause}")
                Toast.makeText(requireContext(), "Failed to create event: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun filterEvents() {
        filteredEventsList.clear()

        for (event in eventsList) {
            if (showOnlyMyEvents && !joinersMap.containsKey(event.id)) {
                continue
            }

            val matchesSearch = currentSearchQuery.isEmpty() ||
                    event.title.lowercase().contains(currentSearchQuery) ||
                    event.description.lowercase().contains(currentSearchQuery) ||
                    event.category.lowercase().contains(currentSearchQuery)

            val matchesCategory = currentFilterCategory == null ||
                    event.category.equals(currentFilterCategory, ignoreCase = true)

            if (matchesSearch && matchesCategory) {
                filteredEventsList.add(event)
            }
        }

        updateEventCount()
        setupAdapter()
    }

    private fun updateEventCount() {
        tvEventCount.text = filteredEventsList.size.toString()
    }

    private fun fetchEvents() {
        if (!isDataLoaded) {
            progressBar.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
        swipeRefreshLayout.isRefreshing = true

        db.collection("events")
            .orderBy("startDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                eventsList.clear()
                for (document in documents) {
                    val event = document.toObject(Event::class.java).copy(id = document.id)
                    eventsList.add(event)
                }
                fetchJoiners()
            }
            .addOnFailureListener { e ->
                Log.e("EventFragment", "Error fetching events", e)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                if (isDataLoaded && eventsList.isNotEmpty()) {
                    filterEvents()
                } else {
                    showEmptyState("Failed to load events. Please try again.")
                }
            }
    }

    // ============================================================
    // FETCH JOINERS - Uses Source.SERVER to get FRESH data!
    // This is KEY to getting updated progress after logging!
    // Refactored to avoid collection group query (no index needed)
    // ============================================================
    private fun fetchJoiners() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            filterEvents()
            updateMyEventsCount()
            return
        }

        // Instead of collection group query, check each event's joiners subcollection
        // Since joiner document ID is the user's uid, we can read directly (no query needed)
        val newJoinersMap = mutableMapOf<String, EventJoiner>()
        var completedCount = 0
        val totalEvents = eventsList.size

        if (totalEvents == 0) {
            // No events to check
            joinersMap.clear()
            cachedJoinedCount = 0
            cachedTotalPoints = 0
            cachedMaxStreak = 0
            isDataLoaded = true
            filterEvents()
            updateMyEventsCount()
            return
        }

        // Use parallel reads to check all events at once
        eventsList.forEach { event ->
            db.collection("events")
                .document(event.id)
                .collection("joiners")
                .document(currentUserId)
                .get(Source.SERVER)
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val joiner = document.toObject(EventJoiner::class.java)
                        if (joiner != null) {
                            newJoinersMap[event.id] = joiner
                        }
                    }
                    
                    completedCount++
                    // When all reads complete, update the UI
                    if (completedCount == totalEvents) {
                        // Calculate stats
                        var totalPoints = 0
                        var maxStreak = 0
                        var joinedCount = 0

                        for (joiner in newJoinersMap.values) {
                            totalPoints += joiner.totalPoints
                            if (joiner.streak > maxStreak) {
                                maxStreak = joiner.streak
                            }
                            joinedCount++
                        }

                        // Swap data atomically
                        joinersMap.clear()
                        joinersMap.putAll(newJoinersMap)
                        
                        // Cache stats (persists in companion object)
                        cachedJoinedCount = joinedCount
                        cachedTotalPoints = totalPoints
                        cachedMaxStreak = maxStreak
                        isDataLoaded = true
                        
                        // Update UI
                        tvJoinedCount.text = joinedCount.toString()
                        tvTotalPoints.text = totalPoints.toString()
                        tvActiveStreak.text = maxStreak.toString()

                        filterEvents()
                        updateMyEventsCount()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("EventFragment", "Error fetching joiner for event ${event.id}", e)
                    completedCount++
                    // Still update when all reads complete (even if some failed)
                    if (completedCount == totalEvents) {
                        // Calculate stats with whatever we got
                        var totalPoints = 0
                        var maxStreak = 0
                        var joinedCount = 0

                        for (joiner in newJoinersMap.values) {
                            totalPoints += joiner.totalPoints
                            if (joiner.streak > maxStreak) {
                                maxStreak = joiner.streak
                            }
                            joinedCount++
                        }

                        joinersMap.clear()
                        joinersMap.putAll(newJoinersMap)
                        
                        cachedJoinedCount = joinedCount
                        cachedTotalPoints = totalPoints
                        cachedMaxStreak = maxStreak
                        isDataLoaded = true
                        
                        tvJoinedCount.text = joinedCount.toString()
                        tvTotalPoints.text = totalPoints.toString()
                        tvActiveStreak.text = maxStreak.toString()

                        filterEvents()
                        updateMyEventsCount()
                    }
                }
        }
    }

    private fun updateMyEventsCount() {
        tvMyEventsCount.text = joinersMap.size.toString()
    }

    private fun setupAdapter() {
        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false

        if (filteredEventsList.isEmpty()) {
            if (eventsList.isEmpty()) {
                showEmptyState("Create your first challenge!")
            } else {
                showEmptyState("No events match your search or filter.")
            }
        } else {
            layoutEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = EventAdapter(
                filteredEventsList,
                joinersMap,
                auth.currentUser?.uid,
                onJoinClick = { event -> joinChallenge(event) },
                onViewDetailsClick = { event, joiner -> openEventDetails(event, joiner) }
            )
        }
    }

    private fun showEmptyState(message: String) {
        layoutEmptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvEmptyState.text = message
    }

    private fun createSampleEvents() {
        btnCreateSampleEvents.isEnabled = false
        btnCreateSampleEvents.text = "Creating..."
        progressBar.visibility = View.VISIBLE

        val currentTime = System.currentTimeMillis()

        val sampleEvents = listOf(
            Event(
                title = "30-Day Yoga Challenge",
                description = "Transform your body and mind with daily yoga practice!",
                duration = 30,
                reward = "500 Points + Yoga Master Badge",
                startDate = currentTime,
                endDate = currentTime + (30L * 24 * 60 * 60 * 1000),
                category = "Yoga"
            ),
            Event(
                title = "Healthy Eating Challenge",
                description = "21 days of nutritious meals. Build healthy habits!",
                duration = 21,
                reward = "300 Points + Nutrition Expert Badge",
                startDate = currentTime,
                endDate = currentTime + (21L * 24 * 60 * 60 * 1000),
                category = "Diet"
            ),
            Event(
                title = "10K Steps Daily",
                description = "Walk 10,000 steps every day for 14 days!",
                duration = 14,
                reward = "200 Points + Fitness Warrior Badge",
                startDate = currentTime,
                endDate = currentTime + (14L * 24 * 60 * 60 * 1000),
                category = "Fitness"
            ),
            Event(
                title = "Meditation Journey",
                description = "15 minutes of daily meditation for 28 days!",
                duration = 28,
                reward = "400 Points + Zen Master Badge",
                startDate = currentTime,
                endDate = currentTime + (28L * 24 * 60 * 60 * 1000),
                category = "Wellness"
            ),
            Event(
                title = "Hydration Hero",
                description = "Drink 8 glasses of water daily for a week!",
                duration = 7,
                reward = "100 Points + Hydration Badge",
                startDate = currentTime,
                endDate = currentTime + (7L * 24 * 60 * 60 * 1000),
                category = "Health"
            )
        )

        var successCount = 0
        var failureCount = 0

        sampleEvents.forEach { event ->
            db.collection("events")
                .add(event)
                .addOnSuccessListener { docRef ->
                    Log.d("EventFragment", "Sample event created: ${event.title} with ID: ${docRef.id}")
                    successCount++
                    if (successCount + failureCount == sampleEvents.size) {
                        onSampleEventsCreated(successCount)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("EventFragment", "Error creating sample event: ${event.title}", e)
                    Log.e("EventFragment", "Error details: ${e.message}")
                    failureCount++
                    if (successCount + failureCount == sampleEvents.size) {
                        onSampleEventsCreated(successCount)
                    }
                }
        }
    }

    private fun onSampleEventsCreated(successCount: Int) {
        progressBar.visibility = View.GONE
        btnCreateSampleEvents.isEnabled = true
        btnCreateSampleEvents.text = "Load Samples"
        
        if (successCount > 0) {
            Toast.makeText(requireContext(), "🎉 $successCount challenges created!", Toast.LENGTH_SHORT).show()
            fetchEvents()
        } else {
            Toast.makeText(requireContext(), "Failed to create events.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun joinChallenge(event: Event) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login to join", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if already joined
        if (joinersMap.containsKey(event.id)) {
            Toast.makeText(requireContext(), "Already joined!", Toast.LENGTH_SHORT).show()
            return
        }

        val joiner = EventJoiner(
            uid = currentUser.uid,
            joinDate = System.currentTimeMillis(),
            streak = 0,
            completedDays = emptyList(),
            totalPoints = 0,
            isCompleted = false
        )

        db.collection("events")
            .document(event.id)
            .collection("joiners")
            .document(currentUser.uid)
            .set(joiner)
            .addOnSuccessListener {
                // Update local cache immediately
                joinersMap[event.id] = joiner
                cachedJoinedCount = joinersMap.size
                
                // Update UI immediately
                tvJoinedCount.text = cachedJoinedCount.toString()
                tvMyEventsCount.text = cachedJoinedCount.toString()
                
                // Refresh adapter
                filterEvents()
                
                Toast.makeText(requireContext(), "🎯 Joined ${event.title}!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("EventFragment", "Error joining", e)
                Toast.makeText(requireContext(), "Failed to join", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openEventDetails(event: Event, joiner: EventJoiner?) {
        val intent = android.content.Intent(requireContext(), EventDetailsActivity::class.java).apply {
            putExtra("event", event)
            putExtra("joiner", joiner)
        }
        startActivity(intent)
    }
}
