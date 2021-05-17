package pt.atp.bucketlist.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.atp.bucketlist.R
import pt.atp.bucketlist.databinding.FragmentChatBinding
import pt.atp.bucketlist.deviceName
import pt.atp.bucketlist.model.ChatAdapter
import pt.atp.bucketlist.model.Message
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "ChatFragment"

class ChatFragment : Fragment() { //R.layout.fragment_chat

    private lateinit var binding: FragmentChatBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        setup()
        loadMessages()
    }

    private fun setup() {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.stackFromEnd = true

        binding.rvMessages.apply {
            setHasFixedSize(true)
            layoutManager = linearLayoutManager
            adapter = ChatAdapter()
        }

        binding.btnSend.setOnClickListener {
            val message = binding.etContent.text.toString()
            sendMessage(message)

            binding.etContent.text.clear()
        }
    }

    private fun loadMessages() {
        val db = FirebaseFirestore.getInstance()
        val docs = db.collection("atp").orderBy("timestamp")
        docs.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) {
                Log.w(TAG, "Unable to retrieve data. Error=$e, snapshot=$snapshot")
                return@addSnapshotListener
            }

            Log.d(TAG, "New data retrieved:${snapshot.documents.size}")

            val messages = mutableListOf<Message>()
            for (document in snapshot.documents) {
                val message = Message(
                        document.id,
                        "${document.data?.get("username")}",
                        "${document.data?.get("content")}",
                        "${document.data?.get("timestamp")}",
                        document.data?.get("username") == deviceName()
                )

                messages += message
            }

            for (message in messages) {
                Log.d(TAG, message.toString())
            }

            val adapter = binding.rvMessages.adapter as ChatAdapter

            adapter.submitList(messages)
            adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    binding.rvMessages.scrollToPosition(positionStart)
                }
            })

        }
    }

    private fun sendMessage(content: String) {
        val message = hashMapOf(
                "username" to deviceName(), //deviceName(),
                "content" to content,
                "timestamp" to "${System.currentTimeMillis()}"
        )

        val db = FirebaseFirestore.getInstance()
        val id: String = db.collection("atp").document().id
        db.collection("atp").document(id)
                .set(message)
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error writing document. Error:$e") }

    }
}