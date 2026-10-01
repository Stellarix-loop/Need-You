package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.FriendStatus
import com.example.model.PingRecord
import com.example.model.PingType
import com.example.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = PreferencesManager(context)

    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var isFirebaseAvailable = false

    private val _userProfile = MutableStateFlow(
        UserProfile(
            userId = prefs.getUserId(),
            displayName = prefs.getSavedDisplayName(),
            pairCode = prefs.getPairCode(),
            pairedWithUserId = prefs.getCachedPairedFriendId(),
            pairedWithName = prefs.getCachedPairedFriendName(),
            isOnline = true
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _friendStatus = MutableStateFlow(FriendStatus.OFFLINE)
    val friendStatus: StateFlow<FriendStatus> = _friendStatus.asStateFlow()

    private val _friendLastSeenTimestamp = MutableStateFlow<Long>(System.currentTimeMillis())
    val friendLastSeenTimestamp: StateFlow<Long> = _friendLastSeenTimestamp.asStateFlow()

    private val _incomingPings = MutableSharedFlow<PingRecord>(extraBufferCapacity = 10)
    val incomingPings: SharedFlow<PingRecord> = _incomingPings.asSharedFlow()

    private val _recentPings = MutableStateFlow<List<PingRecord>>(emptyList())
    val recentPings: StateFlow<List<PingRecord>> = _recentPings.asStateFlow()

    private val _isLiveFirebaseMode = MutableStateFlow(false)
    val isLiveFirebaseMode: StateFlow<Boolean> = _isLiveFirebaseMode.asStateFlow()

    private var userDocListener: ListenerRegistration? = null
    private var friendDocListener: ListenerRegistration? = null
    private var pingsListener: ListenerRegistration? = null

    private val sessionStartTime = System.currentTimeMillis()

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }

            if (app != null) {
                firestore = FirebaseFirestore.getInstance()
                auth = FirebaseAuth.getInstance()
                isFirebaseAvailable = true
                _isLiveFirebaseMode.value = true
                Log.i(TAG, "Firebase initialized successfully in live mode.")
                setupLiveSession()
            } else {
                enableLocalFallback("FirebaseApp initialization returned null")
            }
        } catch (e: Exception) {
            enableLocalFallback("Firebase not available: ${e.message}")
        }
    }

    private fun enableLocalFallback(reason: String) {
        isFirebaseAvailable = false
        _isLiveFirebaseMode.value = false
        Log.w(TAG, "Running in local simulation mode: $reason")
        // Initialize friend status based on cached state
        if (_userProfile.value.isPaired) {
            _friendStatus.value = FriendStatus.CONNECTED
        }
    }

    private fun setupLiveSession() {
        scope.launch {
            try {
                val currentAuth = auth ?: return@launch
                if (currentAuth.currentUser == null) {
                    currentAuth.signInAnonymously().await()
                }

                val currentUserId = currentAuth.currentUser?.uid ?: prefs.getUserId()
                val currentPairCode = prefs.getPairCode()
                val currentDisplayName = prefs.getSavedDisplayName()

                // Fetch or register FCM token
                fetchAndUpdateFcmToken(currentUserId)

                // Sync current user document in Firestore
                val db = firestore ?: return@launch
                val userRef = db.collection("users").document(currentUserId)
                val snapshot = userRef.get().await()

                if (!snapshot.exists()) {
                    val initialData = hashMapOf(
                        "userId" to currentUserId,
                        "displayName" to currentDisplayName,
                        "pairCode" to currentPairCode,
                        "pairedWithUserId" to null,
                        "pairedWithName" to null,
                        "isOnline" to true,
                        "lastSeenTimestamp" to FieldValue.serverTimestamp()
                    )
                    userRef.set(initialData).await()

                    // Also write to pairing_codes lookup table
                    db.collection("pairing_codes").document(currentPairCode).set(
                        hashMapOf("userId" to currentUserId, "createdAt" to FieldValue.serverTimestamp())
                    ).await()
                } else {
                    userRef.update(
                        mapOf(
                            "isOnline" to true,
                            "lastSeenTimestamp" to FieldValue.serverTimestamp(),
                            "displayName" to currentDisplayName
                        )
                    )
                }

                observeUserDocument(currentUserId)
                observeIncomingPings(currentUserId)
            } catch (e: Exception) {
                Log.e(TAG, "Error in setupLiveSession: ${e.message}", e)
            }
        }
    }

    private fun fetchAndUpdateFcmToken(userId: String) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    updateFcmToken(token, userId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FCM token retrieval skipped: ${e.message}")
        }
    }

    fun updateFcmToken(token: String, targetUserId: String? = null) {
        val uid = targetUserId ?: auth?.currentUser?.uid ?: prefs.getUserId()
        firestore?.collection("users")?.document(uid)?.update("fcmToken", token)
    }

    private fun observeUserDocument(userId: String) {
        val db = firestore ?: return
        userDocListener?.remove()
        userDocListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val displayName = snapshot.getString("displayName") ?: prefs.getSavedDisplayName()
                val pairCode = snapshot.getString("pairCode") ?: prefs.getPairCode()
                val pairedWith = snapshot.getString("pairedWithUserId")
                val pairedName = snapshot.getString("pairedWithName")

                prefs.setCachedPairing(pairedWith, pairedName)

                _userProfile.value = UserProfile(
                    userId = userId,
                    displayName = displayName,
                    pairCode = pairCode,
                    pairedWithUserId = pairedWith,
                    pairedWithName = pairedName,
                    isOnline = true
                )

                if (!pairedWith.isNullOrBlank()) {
                    observeFriendDocument(pairedWith)
                } else {
                    friendDocListener?.remove()
                    _friendStatus.value = FriendStatus.OFFLINE
                }
            }
    }

    private fun observeFriendDocument(friendUserId: String) {
        val db = firestore ?: return
        friendDocListener?.remove()
        friendDocListener = db.collection("users").document(friendUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    _friendStatus.value = FriendStatus.OFFLINE
                    return@addSnapshotListener
                }

                val isOnline = snapshot.getBoolean("isOnline") ?: false
                val serverTimestamp = snapshot.getTimestamp("lastSeenTimestamp")?.toDate()?.time
                    ?: snapshot.getLong("lastSeenTimestamp")
                    ?: System.currentTimeMillis()

                _friendLastSeenTimestamp.value = serverTimestamp
                _friendStatus.value = if (isOnline) FriendStatus.CONNECTED else FriendStatus.OFFLINE
            }
    }

    fun toggleFriendStatusForDemo() {
        val current = _friendStatus.value
        _friendStatus.value = if (current == FriendStatus.CONNECTED) FriendStatus.OFFLINE else FriendStatus.CONNECTED
        _friendLastSeenTimestamp.value = System.currentTimeMillis()
    }

    private fun observeIncomingPings(currentUserId: String) {
        val db = firestore ?: return
        pingsListener?.remove()
        pingsListener = db.collection("pings")
            .whereEqualTo("toUserId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val pings = snapshot.documents.mapNotNull { doc ->
                    val typeStr = doc.getString("type")
                    val fromId = doc.getString("fromUserId") ?: ""
                    val fromName = doc.getString("fromName") ?: "Friend"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    PingRecord(
                        id = doc.id,
                        fromUserId = fromId,
                        fromUserName = fromName,
                        toUserId = currentUserId,
                        type = PingType.fromString(typeStr),
                        timestamp = timestamp
                    )
                }

                _recentPings.value = pings

                // Emit newly received ping if arrived after session start
                val latest = pings.firstOrNull()
                if (latest != null && latest.timestamp > sessionStartTime) {
                    _incomingPings.tryEmit(latest)
                }
            }
    }

    suspend fun pairWithCode(inputCode: String): Result<String> {
        val cleanCode = inputCode.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Please enter a valid 6-digit code."))
        }

        if (cleanCode == _userProfile.value.pairCode) {
            return Result.failure(IllegalArgumentException("You cannot pair with your own code!"))
        }

        val db = firestore
        if (db == null || !isFirebaseAvailable) {
            // Local simulation fallback
            val friendName = if (cleanCode == "123456") "Sam (Demo)" else "Friend #$cleanCode"
            val friendId = "sim_user_$cleanCode"
            prefs.setCachedPairing(friendId, friendName)
            _userProfile.value = _userProfile.value.copy(
                pairedWithUserId = friendId,
                pairedWithName = friendName
            )
            _friendStatus.value = FriendStatus.CONNECTED
            return Result.success(friendName)
        }

        return try {
            val codeDoc = db.collection("pairing_codes").document(cleanCode).get().await()
            if (!codeDoc.exists()) {
                return Result.failure(IllegalArgumentException("No user found with code $cleanCode. Double check the code."))
            }

            val targetUserId = codeDoc.getString("userId")
                ?: return Result.failure(IllegalStateException("Invalid pairing record."))

            val targetUserDoc = db.collection("users").document(targetUserId).get().await()
            if (!targetUserDoc.exists()) {
                return Result.failure(IllegalArgumentException("User account not found."))
            }

            val targetName = targetUserDoc.getString("displayName") ?: "Friend"
            val currentUserId = _userProfile.value.userId
            val myName = _userProfile.value.displayName

            // Mutual pairing update
            db.collection("users").document(currentUserId).update(
                mapOf(
                    "pairedWithUserId" to targetUserId,
                    "pairedWithName" to targetName
                )
            ).await()

            db.collection("users").document(targetUserId).update(
                mapOf(
                    "pairedWithUserId" to currentUserId,
                    "pairedWithName" to myName
                )
            ).await()

            prefs.setCachedPairing(targetUserId, targetName)
            _userProfile.value = _userProfile.value.copy(
                pairedWithUserId = targetUserId,
                pairedWithName = targetName
            )
            observeFriendDocument(targetUserId)

            Result.success(targetName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unpairFriend(): Result<Unit> {
        val currentUserId = _userProfile.value.userId
        val friendId = _userProfile.value.pairedWithUserId

        prefs.setCachedPairing(null, null)
        _userProfile.value = _userProfile.value.copy(
            pairedWithUserId = null,
            pairedWithName = null
        )
        _friendStatus.value = FriendStatus.OFFLINE

        val db = firestore
        if (db != null && isFirebaseAvailable && !friendId.isNullOrBlank()) {
            try {
                db.collection("users").document(currentUserId).update(
                    mapOf("pairedWithUserId" to null, "pairedWithName" to null)
                ).await()

                db.collection("users").document(friendId).update(
                    mapOf("pairedWithUserId" to null, "pairedWithName" to null)
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Unpair remote update warning: ${e.message}")
            }
        }
        return Result.success(Unit)
    }

    suspend fun sendPing(type: PingType): Result<PingRecord> {
        val currentUser = _userProfile.value
        val friendId = currentUser.pairedWithUserId
        if (friendId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("You are not paired with a friend yet."))
        }

        val record = PingRecord(
            id = UUID.randomUUID().toString(),
            fromUserId = currentUser.userId,
            fromUserName = currentUser.displayName,
            toUserId = friendId,
            type = type,
            timestamp = System.currentTimeMillis()
        )

        // Prepend to local recent list immediately for snappy UX
        _recentPings.value = listOf(record) + _recentPings.value.take(19)

        val db = firestore
        if (db != null && isFirebaseAvailable) {
            try {
                val pingData = hashMapOf(
                    "fromUserId" to record.fromUserId,
                    "fromName" to record.fromUserName,
                    "toUserId" to record.toUserId,
                    "type" to record.type.name,
                    "timestamp" to record.timestamp,
                    "status" to "pending"
                )
                db.collection("pings").document(record.id).set(pingData).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload ping: ${e.message}")
                return Result.failure(e)
            }
        }

        return Result.success(record)
    }

    fun updateDisplayName(newName: String) {
        val trimmed = newName.trim().take(30)
        prefs.setDisplayName(trimmed)
        _userProfile.value = _userProfile.value.copy(displayName = trimmed)

        val db = firestore
        val uid = _userProfile.value.userId
        if (db != null && isFirebaseAvailable && uid.isNotBlank()) {
            db.collection("users").document(uid).update("displayName", trimmed)
        }
    }

    fun setAppForegroundStatus(isForeground: Boolean) {
        val db = firestore
        val uid = _userProfile.value.userId
        if (db != null && isFirebaseAvailable && uid.isNotBlank()) {
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isForeground,
                    "lastSeenTimestamp" to FieldValue.serverTimestamp()
                )
            )
        }
    }

    companion object {
        private const val TAG = "NeedYouFirebase"

        @Volatile
        private var instance: FirebaseManager? = null

        fun getInstance(context: Context): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
