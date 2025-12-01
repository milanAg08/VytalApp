package com.example.vytal.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.vytal.R
import com.example.vytal.LoginActivity
import com.example.vytal.model.Article
import com.example.vytal.model.ArticlesAdapter
import com.example.vytal.model.EventJoiner
import com.example.vytal.model.UserProfile
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import android.widget.Toast
import java.util.Calendar

// Fragment representing the home screen with enhanced features
class HomeFragment : Fragment() {

    // View references
    private lateinit var searchView: SearchView
    private lateinit var recyclerArticles: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var scrollView: android.widget.ScrollView? = null
    private lateinit var tvWelcome: TextView
    private lateinit var tvTotalPoints: TextView
    private lateinit var tvActiveStreak: TextView
    private lateinit var tvJoinedChallenges: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var tvFeatured: TextView
    private lateinit var tvProfileCircle: TextView
    private lateinit var tvMotivationalQuote: TextView
    private lateinit var tvHealthTip: TextView
    
    // Scroll tracking
    private var lastScrollY = 0
    
    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Data
    private val articlesList = mutableListOf<Article>()
    private val filteredArticlesList = mutableListOf<Article>()
    private var articlesAdapter: ArticlesAdapter? = null
    private var currentSearchQuery: String = ""
    
    // Stats
    private var totalPoints = 0
    private var maxStreak = 0
    private var joinedChallengesCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        initViews(view)
        setupListeners(view)
        loadArticles()
        fetchUserStats()
        setupWelcomeMessage()
        setupMotivationalContent()
        
        return view
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh stats when returning to home (only if views are initialized)
        if (::tvTotalPoints.isInitialized) {
            fetchUserStats()
        }
    }
    
    private fun initViews(view: View) {
        searchView = view.findViewById(R.id.search_view)
        recyclerArticles = view.findViewById(R.id.recycler_articles)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        // ScrollView will be initialized in setupScrollListener after layout
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvTotalPoints = view.findViewById(R.id.tvTotalPoints)
        tvActiveStreak = view.findViewById(R.id.tvActiveStreak)
        tvJoinedChallenges = view.findViewById(R.id.tvJoinedChallenges)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        tvFeatured = view.findViewById(R.id.tv_featured)
        tvProfileCircle = view.findViewById(R.id.tvProfileCircle)
        tvMotivationalQuote = view.findViewById(R.id.tvMotivationalQuote)
        tvHealthTip = view.findViewById(R.id.tvHealthTip)
        
        val context = requireContext()
        recyclerArticles.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        
        swipeRefreshLayout.setColorSchemeColors(
            context.getColor(R.color.colorPrimary),
            context.getColor(R.color.buttonGreenDark),
            context.getColor(R.color.darkAccent)
        )
        
        // Initially hide empty state
        tvEmptyState.visibility = View.GONE
        
        // Setup profile circle with user initials
        setupProfileCircle()
        
        // Make stats cards clickable - navigate to events
        val pointsCard = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardPoints)
        val streakCard = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardStreak)
        val challengesCard = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardChallenges)
        
        val navigateToEvents = {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EventFragment())
                .addToBackStack(null)
                .commit()
        }
        
        pointsCard?.setOnClickListener { navigateToEvents() }
        streakCard?.setOnClickListener { navigateToEvents() }
        challengesCard?.setOnClickListener { navigateToEvents() }
    }
    
    private fun setupProfileCircle() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Try to load profile name first, then fallback to display name/email
            loadUserProfile(currentUser.uid) { profile ->
                val initials = when {
                    profile.name.isNotEmpty() -> extractInitials(profile.name)
                    !currentUser.displayName.isNullOrEmpty() -> {
                        extractInitials(currentUser.displayName!!)
                    }
                    currentUser.email?.isNotEmpty() == true -> {
                        currentUser.email!!.substringBefore("@")
                            .take(2)
                            .uppercase()
                    }
                    else -> "U"
                }
                
                tvProfileCircle.text = if (initials.isEmpty()) "U" else initials
            }
            
            // Set click listener to show profile
            tvProfileCircle.setOnClickListener {
                val displayName = currentUser.displayName
                val email = currentUser.email ?: ""
                
                val initials = when {
                    !displayName.isNullOrEmpty() -> extractInitials(displayName)
                    email.isNotEmpty() -> email.substringBefore("@").take(2).uppercase()
                    else -> "U"
                }
                
                // Add scale animation
                tvProfileCircle.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction {
                        tvProfileCircle.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                        showProfileDialog(currentUser, initials)
                    }
                    .start()
            }
        } else {
            tvProfileCircle.text = "?"
            tvProfileCircle.setOnClickListener {
                // Show login prompt or handle as needed
            }
        }
    }
    
    private fun showProfileDialog(user: com.google.firebase.auth.FirebaseUser, initials: String) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_profile, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Get all views
        val tvProfileInitials = bottomSheetView.findViewById<TextView>(R.id.tvProfileInitials)
        val etProfileName = bottomSheetView.findViewById<TextInputEditText>(R.id.etProfileName)
        val tvProfileEmail = bottomSheetView.findViewById<TextView>(R.id.tvProfileEmail)
        val tvProfileUserId = bottomSheetView.findViewById<TextView>(R.id.tvProfileUserId)
        val tvProfilePoints = bottomSheetView.findViewById<TextView>(R.id.tvProfilePoints)
        val tvProfileStreak = bottomSheetView.findViewById<TextView>(R.id.tvProfileStreak)
        val tvProfileChallenges = bottomSheetView.findViewById<TextView>(R.id.tvProfileChallenges)
        val etBloodType = bottomSheetView.findViewById<TextInputEditText>(R.id.etBloodType)
        val etPhoneNumber = bottomSheetView.findViewById<TextInputEditText>(R.id.etPhoneNumber)
        val etDateOfBirth = bottomSheetView.findViewById<TextInputEditText>(R.id.etDateOfBirth)
        val etGender = bottomSheetView.findViewById<TextInputEditText>(R.id.etGender)
        val etAddress = bottomSheetView.findViewById<TextInputEditText>(R.id.etAddress)
        val etMedicalConditions = bottomSheetView.findViewById<TextInputEditText>(R.id.etMedicalConditions)
        val etAllergies = bottomSheetView.findViewById<TextInputEditText>(R.id.etAllergies)
        val etMedications = bottomSheetView.findViewById<TextInputEditText>(R.id.etMedications)
        val etEmergencyContact = bottomSheetView.findViewById<TextInputEditText>(R.id.etEmergencyContact)
        val etEmergencyPhone = bottomSheetView.findViewById<TextInputEditText>(R.id.etEmergencyPhone)
        val btnSaveProfile = bottomSheetView.findViewById<Button>(R.id.btnSaveProfile)
        val btnCloseProfile = bottomSheetView.findViewById<Button>(R.id.btnCloseProfile)
        
        // Set initial values
        tvProfileInitials.text = if (initials.isEmpty()) "U" else initials
        tvProfileEmail.text = user.email ?: "No email"
        tvProfileUserId.text = user.uid.take(8) + "..."
        
        // Set stats
        updateProfileStats(tvProfilePoints, tvProfileStreak, tvProfileChallenges)
        
        // Load user profile from Firestore
        loadUserProfile(user.uid) { profile ->
            etProfileName.setText(profile.name.ifEmpty { user.displayName ?: user.email?.substringBefore("@") ?: "" })
            etBloodType.setText(profile.bloodType)
            etPhoneNumber.setText(profile.phoneNumber)
            etDateOfBirth.setText(profile.dateOfBirth)
            etGender.setText(profile.gender)
            etAddress.setText(profile.address)
            etMedicalConditions.setText(profile.medicalConditions)
            etAllergies.setText(profile.allergies)
            etMedications.setText(profile.medications)
            etEmergencyContact.setText(profile.emergencyContact)
            etEmergencyPhone.setText(profile.emergencyPhone)
            
            // Update initials if name is available
            if (profile.name.isNotEmpty()) {
                val nameInitials = extractInitials(profile.name)
                tvProfileInitials.text = if (nameInitials.isEmpty()) "U" else nameInitials
            }
        }
        
        // Save button
        btnSaveProfile.setOnClickListener {
            val profile = UserProfile(
                uid = user.uid,
                name = etProfileName.text?.toString()?.trim() ?: "",
                email = user.email ?: "",
                bloodType = etBloodType.text?.toString()?.trim() ?: "",
                phoneNumber = etPhoneNumber.text?.toString()?.trim() ?: "",
                dateOfBirth = etDateOfBirth.text?.toString()?.trim() ?: "",
                gender = etGender.text?.toString()?.trim() ?: "",
                address = etAddress.text?.toString()?.trim() ?: "",
                emergencyContact = etEmergencyContact.text?.toString()?.trim() ?: "",
                emergencyPhone = etEmergencyPhone.text?.toString()?.trim() ?: "",
                medicalConditions = etMedicalConditions.text?.toString()?.trim() ?: "",
                allergies = etAllergies.text?.toString()?.trim() ?: "",
                medications = etMedications.text?.toString()?.trim() ?: "",
                lastUpdated = System.currentTimeMillis()
            )
            
            saveUserProfile(profile) { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Profile saved successfully! ✅", Toast.LENGTH_SHORT).show()
                    
                    // Update profile circle initials
                    val newInitials = extractInitials(profile.name)
                    if (newInitials.isNotEmpty()) {
                        tvProfileCircle.text = newInitials
                        tvProfileInitials.text = newInitials
                    }
                    
                    bottomSheetDialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Close button
        btnCloseProfile.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }
    
    private fun extractInitials(name: String): String {
        return if (name.isNotEmpty()) {
            name.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .take(2)
        } else {
            ""
        }
    }
    
    private fun loadUserProfile(uid: String, callback: (UserProfile) -> Unit) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profile = document.toObject(UserProfile::class.java) ?: UserProfile(uid = uid)
                    callback(profile)
                } else {
                    // Create default profile
                    callback(UserProfile(uid = uid))
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error loading profile", e)
                callback(UserProfile(uid = uid))
            }
    }
    
    private fun saveUserProfile(profile: UserProfile, callback: (Boolean) -> Unit) {
        db.collection("users")
            .document(profile.uid)
            .set(profile)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error saving profile", e)
                callback(false)
            }
    }
    
    private fun updateProfileStats(tvPoints: TextView, tvStreak: TextView, tvChallenges: TextView) {
        tvPoints.text = totalPoints.toString()
        tvStreak.text = maxStreak.toString()
        tvChallenges.text = joinedChallengesCount.toString()
    }
    
    private fun setupListeners(view: View) {
        swipeRefreshLayout.setOnRefreshListener {
            loadArticles()
            fetchUserStats()
        }
        
        // Setup scroll listener after views are initialized
        setupScrollListener()
        
        // Ensure bottom nav is visible when fragment is first shown
        try {
            (activity as? com.example.vytal.MainActivity)?.showBottomNavigation()
        } catch (e: Exception) {
            // Ignore if activity is not available yet
        }
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText?.lowercase() ?: ""
                filterArticles()
                return true
            }
        })
        
        val commBtn = view.findViewById<Button>(R.id.btn_community)
        val eventBtn = view.findViewById<Button>(R.id.btn_events)
        val btnBmiCalculator = view.findViewById<Button>(R.id.btnBmiCalculator)
        val btnSymptomChecker = view.findViewById<Button>(R.id.btnSymptomChecker)
        val logoutBtn = view.findViewById<Button>(R.id.btnLogout)

        commBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CommunityFragment())
                .addToBackStack(null)
                .commit()
        }

        eventBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EventFragment())
                .addToBackStack(null)
                .commit()
        }

        btnBmiCalculator.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BmiFragment())
                .addToBackStack(null)
                .commit()
        }

        btnSymptomChecker.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SymptomCheckerFragment())
                .addToBackStack(null)
                .commit()
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
    
    private fun setupScrollListener() {
        // Wait for ScrollView to be available after layout
        swipeRefreshLayout.post {
            try {
                if (swipeRefreshLayout.childCount > 0) {
                    scrollView = swipeRefreshLayout.getChildAt(0) as? android.widget.ScrollView
                    scrollView?.viewTreeObserver?.addOnScrollChangedListener {
                        try {
                            val sv = scrollView ?: return@addOnScrollChangedListener
                            val currentScrollY = sv.scrollY
                            val activity = activity as? com.example.vytal.MainActivity
                            
                            if (currentScrollY <= 0) {
                                // At the top - always show bottom nav
                                activity?.showBottomNavigation()
                            } else if (currentScrollY > lastScrollY && currentScrollY > 100) {
                                // Scrolling down - hide bottom nav
                                activity?.hideBottomNavigation()
                            } else if (currentScrollY < lastScrollY) {
                                // Scrolling up - show bottom nav
                                activity?.showBottomNavigation()
                            }
                            
                            lastScrollY = currentScrollY
                        } catch (e: Exception) {
                            // Ignore scroll listener errors
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore if ScrollView is not available
            }
        }
    }
    
    private fun setupWelcomeMessage() {
        if (::tvWelcome.isInitialized) {
            val currentUser = auth.currentUser
            val displayName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 5..11 -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                in 17..20 -> "Good Evening"
                else -> "Good Night"
            }
            tvWelcome.text = "$greeting, $displayName! 👋"
            
            // Animate welcome message appearance
            tvWelcome.alpha = 0f
            tvWelcome.animate()
                .alpha(1.0f)
                .setDuration(500)
                .start()
        }
    }
    
    private fun setupMotivationalContent() {
        // Motivational quotes
        val quotes = listOf(
            "Your health is your greatest wealth",
            "Take care of your body, it's the only place you have to live",
            "The greatest wealth is health",
            "Invest in your health, it pays the best interest",
            "A healthy outside starts from the inside",
            "Your body can do it, it's your mind you need to convince",
            "Progress, not perfection",
            "Small steps lead to big changes"
        )
        
        // Health tips
        val healthTips = listOf(
            "Stay hydrated! Drink at least 8 glasses of water daily to maintain optimal health and energy levels.",
            "Get 7-9 hours of quality sleep each night. Sleep is essential for physical and mental recovery.",
            "Take a 10-minute walk every day. Regular movement improves circulation and boosts mood.",
            "Practice deep breathing exercises for 5 minutes daily to reduce stress and improve focus.",
            "Eat a colorful variety of fruits and vegetables. Different colors provide different nutrients.",
            "Take breaks from screens every hour. Look away for 20 seconds to reduce eye strain.",
            "Stretch for 5 minutes in the morning. It improves flexibility and reduces muscle tension.",
            "Practice gratitude daily. Write down 3 things you're grateful for to boost mental wellness."
        )
        
        // Set random quote and tip
        if (::tvMotivationalQuote.isInitialized) {
            val randomQuote = quotes.random()
            tvMotivationalQuote.text = randomQuote
        }
        
        if (::tvHealthTip.isInitialized) {
            val randomTip = healthTips.random()
            tvHealthTip.text = randomTip
        }
    }
    
    private fun loadArticles() {
        if (::progressBar.isInitialized) {
            progressBar.visibility = View.VISIBLE
        }
        
        // Simulate loading delay for better UX
        recyclerArticles.postDelayed({
            // Enhanced article list with more content
            val articles = listOf(
                Article("Healthy Eating Guide", "Discover tips for a balanced diet and nutritious meal planning", "https://www.healthline.com/nutrition/healthy-eating"),
                Article("Heart Health Awareness", "Learn to recognize symptoms and prevention strategies for cardiovascular health", "https://www.heart.org/en/health-topics"),
                Article("Yoga for Beginners", "Simple poses and techniques to start your yoga journey", "https://www.yogajournal.com/poses/yoga-for-beginners"),
                Article("Mental Wellness Tips", "Strategies for managing stress and improving mental health", "https://www.mentalhealth.gov"),
                Article("Fitness Fundamentals", "Essential exercises and routines for maintaining an active lifestyle", "https://www.fitness.gov"),
                Article("Sleep Hygiene", "Improve your sleep quality with these proven techniques", "https://www.sleepfoundation.org"),
                Article("Hydration Benefits", "Understanding the importance of staying hydrated throughout the day", "https://www.healthline.com/nutrition/7-health-benefits-of-water"),
                Article("Meditation Basics", "Learn how to start a meditation practice for better mental clarity", "https://www.headspace.com/meditation/meditation-for-beginners"),
                Article("Stress Management", "Effective techniques to reduce and manage daily stress", "https://www.mayoclinic.org/healthy-lifestyle/stress-management"),
                Article("Nutrition Basics", "Understanding macronutrients and their role in health", "https://www.healthline.com/nutrition/what-are-macronutrients")
            )
            
            articlesList.clear()
            articlesList.addAll(articles)
            filterArticles()
            
            if (::progressBar.isInitialized) {
                progressBar.visibility = View.GONE
            }
        }, 500) // Small delay for smooth loading
    }
    
    private fun filterArticles() {
        filteredArticlesList.clear()
        
        if (currentSearchQuery.isEmpty()) {
            filteredArticlesList.addAll(articlesList)
        } else {
            for (article in articlesList) {
                val titleMatch = article.getTitle().lowercase().contains(currentSearchQuery)
                val subtitleMatch = article.getSubtitle().lowercase().contains(currentSearchQuery)
                if (titleMatch || subtitleMatch) {
                    filteredArticlesList.add(article)
                }
            }
        }
        
        updateArticlesAdapter()
    }
    
    private fun updateArticlesAdapter() {
        articlesAdapter = ArticlesAdapter(requireContext(), filteredArticlesList)
        recyclerArticles.adapter = articlesAdapter
        
        // Show/hide empty state
        if (filteredArticlesList.isEmpty()) {
            if (currentSearchQuery.isNotEmpty()) {
                tvEmptyState.text = "No articles found for \"$currentSearchQuery\""
            } else {
                tvEmptyState.text = "No articles available"
            }
            tvEmptyState.visibility = View.VISIBLE
            recyclerArticles.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerArticles.visibility = View.VISIBLE
        }
        
        // Update featured label
        if (currentSearchQuery.isNotEmpty()) {
            tvFeatured.text = "Search Results (${filteredArticlesList.size})"
        } else {
            tvFeatured.text = "Featured Articles"
        }
    }
    
    private fun fetchUserStats() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            updateStatsUI()
            return
        }
        
        // Fetch all events and check joiners
        db.collection("events")
            .get()
            .addOnSuccessListener { eventsSnapshot ->
                var completedCount = 0
                val totalEvents = eventsSnapshot.size()
                var totalPoints = 0
                var maxStreak = 0
                var joinedCount = 0
                
                if (totalEvents == 0) {
                    this.totalPoints = 0
                    this.maxStreak = 0
                    this.joinedChallengesCount = 0
                    updateStatsUI()
                    return@addOnSuccessListener
                }
                
                eventsSnapshot.documents.forEach { eventDoc ->
                    db.collection("events")
                        .document(eventDoc.id)
                        .collection("joiners")
                        .document(currentUserId)
                        .get(Source.SERVER)
                        .addOnSuccessListener { joinerDoc ->
                            if (joinerDoc.exists()) {
                                val joiner = joinerDoc.toObject(EventJoiner::class.java)
                                if (joiner != null) {
                                    totalPoints += joiner.totalPoints
                                    if (joiner.streak > maxStreak) {
                                        maxStreak = joiner.streak
                                    }
                                    joinedCount++
                                }
                            }
                            
                            completedCount++
                            if (completedCount == totalEvents) {
                                this.totalPoints = totalPoints
                                this.maxStreak = maxStreak
                                this.joinedChallengesCount = joinedCount
                                updateStatsUI()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("HomeFragment", "Error fetching joiner for event ${eventDoc.id}", e)
                            completedCount++
                            if (completedCount == totalEvents) {
                                this.totalPoints = totalPoints
                                this.maxStreak = maxStreak
                                this.joinedChallengesCount = joinedCount
                                updateStatsUI()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching events for stats", e)
                updateStatsUI()
            }
    }
    
    private fun updateStatsUI() {
        if (::tvTotalPoints.isInitialized && ::tvActiveStreak.isInitialized && ::tvJoinedChallenges.isInitialized) {
            // Animate number changes
            animateNumberChange(tvTotalPoints, totalPoints)
            animateNumberChange(tvActiveStreak, maxStreak)
            animateNumberChange(tvJoinedChallenges, joinedChallengesCount)
        }
        if (::swipeRefreshLayout.isInitialized) {
            swipeRefreshLayout.isRefreshing = false
        }
    }
    
    private fun animateNumberChange(textView: TextView, newValue: Int) {
        val oldValue = textView.text.toString().toIntOrNull() ?: 0
        if (oldValue != newValue) {
            // Simple fade animation
            textView.alpha = 0.5f
            textView.text = newValue.toString()
            textView.animate()
                .alpha(1.0f)
                .setDuration(300)
                .start()
        } else {
            textView.text = newValue.toString()
        }
    }
}
