package com.mynikatech.apnafund.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.mynikatech.apnafund.constants.ApnaBankConstants.DAILY_MESSAGE_LIMIT
import com.mynikatech.apnafund.constants.ApnaBankConstants.TYPING_DEBOUNCE_MS
import com.mynikatech.apnafund.databinding.FragmentGroupChatBinding
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.chat.model.ChatMessage
import com.mynikatech.apnafund.ui.chat.model.ChatRow
import com.mynikatech.apnafund.util.ApnaBankDate


class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var typingRunnable: Runnable? = null
    private val typingHandler = Handler(Looper.getMainLooper())
    private var isTypingSent = false
    private var replyToMessage: ChatMessage? = null
    private var isUserAtBottom = true
    private var isSearchMode = false
    private var allMessages: List<ChatMessage> = emptyList()
    private var groupId: Int = -1
    private var groupName: String = "Group"
    private var messageListener: ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null
    private var permissionErrorHandled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupId = arguments?.getInt("groupId")?.takeIf { it > 0 }
            ?: SessionManager.groupId?.takeIf { it > 0 }
                    ?: -1

        groupName = arguments?.getString("groupName")
            ?: SessionManager.groupName
                    ?: "Group"

        if (groupId == -1) {
            Log.e("GroupChat", "Invalid groupId, blocking chat")
            binding.noGroupMessageContainer.visibility = View.VISIBLE
            binding.recyclerViewMessages.visibility = View.GONE
            binding.chatInputContainer.visibility = View.GONE
            binding.textChatTitle.text = "Group Chat"
            return
        }
        Log.d(
            "CHAT_DEBUG",
            "argGroupId=$groupId, sessionGroupId=${SessionManager.groupId}"
        )

        FirebaseAuth.getInstance()
            .currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                Log.d("TOKEN_DEBUG", "claims=${result.claims}")
            }

        binding.textChatTitle.text = "$groupName Chat"
        setupRecyclerView()
        setupSendButton(groupId)
        setupTypingListener()

        // 🔑 CRITICAL FIX: refresh token BEFORE touching Firestore
        refreshFirebaseToken {
            startChat()
        }

        binding.buttonCancelReply.setOnClickListener {
            replyToMessage = null
            binding.replyContainer.visibility = View.GONE
        }

        binding.recyclerViewMessages.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val lm = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisible = lm.findLastCompletelyVisibleItemPosition()

                    isUserAtBottom =
                        lastVisible >= chatAdapter.itemCount - 2

                    if (isUserAtBottom) {
                        binding.textNewMessages.visibility = View.GONE
                    }
                }
            }
        )
        binding.textNewMessages.setOnClickListener {
            binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)
            binding.textNewMessages.visibility = View.GONE
        }
        binding.btnCloseSearch.setOnClickListener {
            closeSearch()
        }

        binding.editSearch.doOnTextChanged { text, _, _, _ ->
            val query = text.toString().trim()

            if (query.isEmpty()) {
                chatAdapter.submit(buildChatRows(allMessages))
                return@doOnTextChanged
            }

            val filtered = allMessages.filter {
                it.text.contains(query, ignoreCase = true)
            }

            chatAdapter.submit(buildChatRows(filtered))

            binding.recyclerViewMessages.post {
                if (chatAdapter.itemCount > 0) {
                    binding.recyclerViewMessages.scrollToPosition(0)
                }
            }
        }
        binding.buttonSearch.setOnClickListener {
            openSearch()
        }

    }

    private fun refreshFirebaseToken(onDone: () -> Unit) {
        FirebaseAuth.getInstance()
            .currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener {
                Log.d("TOKEN_REFRESH", "claims=${it.claims}")
                onDone()
            }
            ?.addOnFailureListener {
                Log.e("TOKEN_REFRESH", "failed", it)
                showNoAccessUI("Authentication error")
            }
    }


    private fun showNoAccessUI(msg: String) {
        if (!isAdded || _binding == null) return
        binding.noGroupMessageContainer.visibility = View.VISIBLE
        binding.recyclerViewMessages.visibility = View.GONE
        binding.chatInputContainer.visibility = View.GONE
        binding.textChatTitle.text = msg
    }

    private fun startChat() {
        if (!isAdded || _binding == null) return

        // 🔐 HARD GATE: Firebase must be ready
        if (!SessionManager.isFirebaseSynced || SessionManager.firebaseUid.isBlank()) {
            Log.w("GroupChat", "Chat blocked: Firebase not ready")
            showNoAccessUI("Chat not ready. Please wait…")
            return
        }

        binding.textChatTitle.text = "$groupName Chat"

        listenForMessages(groupId)
        listenTyping()
        loadGroupTitle(groupId)
    }

    private fun loadGroupTitle(groupId: Int) {
        FirebaseFirestore.getInstance()
            .collection("groups")
            .document(groupId.toString())
            .get()
            .addOnSuccessListener { doc ->
                _binding?.textChatTitle?.text =
                    "${doc.getString("name") ?: "Group"} Chat"
            }
            .addOnFailureListener {
                binding.textChatTitle.text = "Group Chat"
            }
    }

    private fun openSearch() {
        isSearchMode = true
        binding.searchContainer.visibility = View.VISIBLE
        binding.buttonSearch.visibility = View.GONE
        binding.editSearch.requestFocus()
    }

    private fun closeSearch() {
        isSearchMode = false
        binding.searchContainer.visibility = View.GONE
        binding.buttonSearch.visibility = View.VISIBLE
        binding.editSearch.setText("")
        chatAdapter.submit(buildChatRows(allMessages))
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(
            SessionManager.firebaseUid,
            onReply = { msg -> onReplyClicked(msg) },
            onReact = { msg, emoji -> toggleReaction(msg, emoji) },
            onReplyNavigate = { messageId ->
                scrollToMessage(messageId)
            }
        )

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun scrollToMessage(messageId: String) {
        val index = chatAdapter.findMessagePosition(messageId)
        if (index == -1) return

        binding.recyclerViewMessages.post {
            binding.recyclerViewMessages.scrollToPosition(index)
            highlightMessage(index)
        }
    }

    private fun highlightMessage(position: Int) {
        val holder =
            binding.recyclerViewMessages.findViewHolderForAdapterPosition(position)
                ?: return

        holder.itemView.setBackgroundColor(0x30FFD54F) // light yellow

        holder.itemView.postDelayed({
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }, 800)
    }

    private fun listenForMessages(groupId: Int) {
        messageListener = firestore.collection("groups")
            .document(groupId.toString())
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->

                val binding = _binding ?: return@addSnapshotListener

                if (error != null) {
                    Log.e("GroupChat", "Firestore error", error)
                    if (!permissionErrorHandled && error is FirebaseFirestoreException &&
                        error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {
                        permissionErrorHandled = true
                        handleAccessDenied()
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null || !isAdded || _binding == null || snapshot.metadata.hasPendingWrites()) return@addSnapshotListener

                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.apply {
                        id = doc.id          // 👈 IMPORTANT for updates
                    }
                }
                allMessages = messages

                if (!isSearchMode) {
                    chatAdapter.submit(buildChatRows(messages))
                }
                val lastMessage = messages.lastOrNull()
                val uid = SessionManager.firebaseUid
                binding.recyclerViewMessages.post {
                    if (isUserAtBottom || lastMessage?.senderId == uid) {
                        binding.recyclerViewMessages.scrollToPosition(chatAdapter.itemCount - 1)
                        binding.textNewMessages.visibility = View.GONE
                    } else {
                        binding.textNewMessages.visibility = View.VISIBLE
                    }
                }

                // 👇 NEW: mark delivered
                markMessagesDelivered(groupId, messages)
            }
    }

    private fun buildChatRows(messages: List<ChatMessage>): List<ChatRow> {
        val rows = mutableListOf<ChatRow>()
        var lastDate: String? = null

        messages.forEach { msg ->
            val date = ApnaBankDate.toDateLabel(msg.createdAt)

            if (date != lastDate) {
                rows.add(ChatRow.DateHeader(date))
                lastDate = date
            }
            rows.add(ChatRow.MessageRow(msg))
        }
        return rows
    }

    private fun markMessagesDelivered(groupId: Int, messages: List<ChatMessage>) {
        val myUid = SessionManager.firebaseUid
        if (myUid.isBlank()) return

        messages
            .filter { msg ->
                msg.senderId != myUid &&
                        !msg.deliveredTo.contains(myUid)
            }
            .forEach { msg ->
                firestore.collection("groups")
                    .document(groupId.toString())
                    .collection("messages")
                    .document(msg.id)
                    .update(
                        "deliveredTo",
                        FieldValue.arrayUnion(myUid)
                    )
            }
    }

    fun onReplyClicked(msg: ChatMessage) {
        replyToMessage = msg

        binding.replyContainer.visibility = View.VISIBLE
        binding.textReplyUser.text = msg.senderName
        binding.textReplyPreview.text = msg.text
    }

    private fun setupSendButton(groupId: Int) {
        binding.buttonSend.setOnClickListener {
            val text = binding.editTextMessage.text.toString().trim()

            if (text.isEmpty()) return@setOnClickListener
            checkDailyLimitAndSend(groupId, text)
        }
    }

    private fun checkDailyLimitAndSend(groupId: Int, text: String) {
        val uid = SessionManager.firebaseUid
        if (uid.isBlank()) return

        val docId = "${ApnaBankDate.todayKey()}_$uid"

        val ref = firestore.collection("groups")
            .document(groupId.toString())
            .collection("dailyStats")
            .document(docId)

        firestore.runTransaction { tx ->
            val snap = tx.get(ref)

            val current = snap.getLong("count") ?: 0L

            if (current >= DAILY_MESSAGE_LIMIT) {
                throw Exception("LIMIT_REACHED")
            }

            tx.set(
                ref,
                mapOf(
                    "userId" to uid,
                    "date" to ApnaBankDate.todayKey(),
                    "count" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }.addOnSuccessListener {
            sendMessage(groupId, text)
            binding.editTextMessage.setText("")
            sendTyping(false)
        }.addOnFailureListener { e ->
            if (e.message == "LIMIT_REACHED") {
                // DISABLE CHAT INPUT HERE
                binding.editTextMessage.isEnabled = false
                binding.buttonSend.isEnabled = false

                binding.textTyping.text = "Daily message limit reached"
                binding.textTyping.visibility = View.VISIBLE
                Toast.makeText(
                    requireContext(),
                    "Daily message limit reached",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Log.e("CHAT_SEND", "Firestore write FAILED", e)
                Toast.makeText(requireContext(), "Unable to send message", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun sendMessage(groupId: Int, text: String) {
        if (SessionManager.firebaseUid.isBlank()) {
            Log.e("CHAT_SEND", "Blocked send: firebaseUid is empty")
            Toast.makeText(requireContext(), "Chat not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val message = hashMapOf(
            "senderId" to SessionManager.firebaseUid,
            "senderName" to SessionManager.getFormattedUserName(),
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )
        Log.d(
            "CHAT_SEND",
            """
        Sending message:
        groupId=$groupId
        authUid=${SessionManager.firebaseUid}
        payload=$message
        """.trimIndent()
        )
        replyToMessage?.let {
            message["replyToMessageId"] = it.id
            message["replySenderName"] = it.senderName
            message["replyPreview"] = it.text.take(100)
        }
        firestore.collection("groups")
            .document(groupId.toString())
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                Log.d("CHAT_SEND", "Message write SUCCESS")
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun sendTyping(isTyping: Boolean) {
        val uid = SessionManager.firebaseUid
        val groupId = SessionManager.groupId ?: return
        if (uid.isBlank()) return

        // Cancel previous debounce
        typingRunnable?.let { typingHandler.removeCallbacks(it) }

        // Send "typing = true" once
        if (!isTypingSent && isTyping) {
            writeTyping(true)
            isTypingSent = true
        }

        typingRunnable = Runnable {
            writeTyping(false)
            isTypingSent = false
        }

        typingHandler.postDelayed(typingRunnable!!, TYPING_DEBOUNCE_MS)
    }

    private fun writeTyping(isTyping: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val groupId = SessionManager.groupId ?: return
        if (uid.isBlank()) return
        if (!isAdded || _binding == null) return

        FirebaseFirestore.getInstance()
            .collection("groups")
            .document(groupId.toString())
            .collection("typing")
            .document(uid)
            .set(
                mapOf(
                    "name" to SessionManager.getFormattedUserName(),
                    "isTyping" to isTyping,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener {
                Log.w("CHAT", "typing update blocked: ${it.message}")
            }
    }

    private fun listenTyping() {
        val groupId = SessionManager.groupId ?: return
        val myUid = SessionManager.firebaseUid

        typingListener = FirebaseFirestore.getInstance()
            .collection("groups")
            .document(groupId.toString())
            .collection("typing")
            .addSnapshotListener { snapshot, _ ->
                val binding = _binding ?: return@addSnapshotListener
                if (snapshot == null) return@addSnapshotListener

                // 🔥 Ignore local writes (important)
                if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener

                val now = System.currentTimeMillis()
                val typingUsers = snapshot.documents
                    .mapNotNull { doc ->
                        val uid = doc.id
                        val isTyping = doc.getBoolean("isTyping") ?: false
                        val ts = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0
                        val name = doc.getString("name") ?: return@mapNotNull null

                        if (
                            uid != myUid &&
                            isTyping &&
                            now - ts < 5000 // TTL = 5 seconds
                        ) name else null
                    }

                updateTypingUI(typingUsers)
            }
    }

    private fun setupTypingListener() {
        binding.editTextMessage.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank()) {
                sendTyping(true)
            }
        }
    }

    private fun updateTypingUI(names: List<String>) {
        if (names.isEmpty()) {
            binding.textTyping.visibility = View.GONE
        } else {
            binding.textTyping.visibility = View.VISIBLE
            binding.textTyping.text =
                if (names.size == 1)
                    "${names[0]} is typing…"
                else
                    "Several people are typing…"
        }
    }


    override fun onDestroyView() {
        messageListener?.remove()
        typingListener?.remove()
        typingRunnable?.let { typingHandler.removeCallbacks(it) }
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (_binding == null) return
        markMessagesSeen()
        // Re-enable input (new day may have started)
        binding.editTextMessage.isEnabled = true
        binding.buttonSend.isEnabled = true
    }

    private fun markMessagesSeen() {
        val uid = SessionManager.firebaseUid
        val groupId = SessionManager.groupId ?: return
        if (uid.isBlank()) return
        if (!isAdded || _binding == null) return

        val unseenMessages = allMessages.filter { msg ->
            msg.senderId != uid && !msg.seenBy.contains(uid)
        }

        if (unseenMessages.isEmpty()) return

        val batch = firestore.batch()
        val messagesRef = firestore.collection("groups")
            .document(groupId.toString())
            .collection("messages")

        unseenMessages.forEach { msg ->
            batch.update(
                messagesRef.document(msg.id),
                "seenBy",
                FieldValue.arrayUnion(uid)
            )
        }

        batch.commit()
            .addOnFailureListener {
                Log.w("CHAT", "mark seen failed: ${it.message}")
            }
    }

    fun toggleReaction(msg: ChatMessage, emoji: String) {
        val uid = SessionManager.firebaseUid
        val groupId = SessionManager.groupId ?: return
        if (uid.isBlank()) return

        val ref = firestore.collection("groups")
            .document(groupId.toString())
            .collection("messages")
            .document(msg.id)

        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val reactions =
                snap.get("reactions") as? Map<String, List<String>> ?: emptyMap()

            val users = reactions[emoji]?.toMutableList() ?: mutableListOf()

            if (users.contains(uid)) {
                users.remove(uid)
            } else {
                users.add(uid)
            }

            tx.update(ref, "reactions.$emoji", users)
        }
    }

    private fun handleAccessDenied() {
        if (!isAdded) return

        // Stop listeners immediately
        messageListener?.remove()
        typingListener?.remove()

        binding.recyclerViewMessages.visibility = View.GONE
        binding.chatInputContainer.visibility = View.GONE
        binding.noGroupMessageContainer.visibility = View.VISIBLE
        binding.textChatTitle.text = "Access denied"

        Log.w("GroupChat", "Access denied for group")
    }
}
