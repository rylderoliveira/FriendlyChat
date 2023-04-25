package com.rylderoliveira.friendlychat

import android.app.ActionBar
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.AnonymousBuilder
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.rylderoliveira.friendlychat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var messageDatabaseReference: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var appBar: ActionBar
    private var childEventListener: ChildEventListener? = null
    private var userName: String? = null
    val a = registerForActivityResult(FirebaseAuthUIActivityResultContract()) {
        if (it.resultCode == RESULT_OK) {
            Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        database = Firebase.database
        messageDatabaseReference = database.reference.child("message")
        auth = Firebase.auth
        setContentView(binding.root)
        initListeners()
        initRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_logout, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_logout -> {
                auth.signOut()
            }
        }
        return true
    }

    private fun initRecyclerView() {
        binding.recyclerViewMessage.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        messageAdapter = MessageAdapter()
        binding.recyclerViewMessage.adapter = messageAdapter
    }

    private fun initListeners() {
        binding.imageButtonSend.setOnClickListener {
            sendMessage()
        }
        authStateListener = FirebaseAuth.AuthStateListener {
            val user = it.currentUser
            if (user != null) {
                onSignedInInitialize(user.displayName)
            } else {
                onSignedOutCleanup()
                val intent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(listOf(
                            AuthUI.IdpConfig.GoogleBuilder().build(),
                            AuthUI.IdpConfig.EmailBuilder().build(),
                        )
                    ).build()
                a.launch(intent)
            }
        }
    }

    private fun onSignedInInitialize(userName: String?) {
        this.userName = userName
        attachDatabaseReadListener()
    }

    private fun onSignedOutCleanup() {
        userName = AnonymousBuilder().build().providerId
        messageAdapter.clear()
        detachDatabaseReadListener()
        childEventListener = null
    }

    private fun attachDatabaseReadListener() {
        childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue<Message>()
                message?.let { messageAdapter.addMessage(it) }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i("Rylder", "Error ${error.message}")
            }
        }
        childEventListener?.let { messageDatabaseReference.addChildEventListener(it) }
    }

    private fun detachDatabaseReadListener() {
        childEventListener?.let { messageDatabaseReference.removeEventListener(it) }
    }

    private fun sendMessage() {
        val textMessage = binding.editTextMessage.text.toString()
        val message = Message(
            text = textMessage,
            userName = auth.currentUser?.displayName,
            imageUrl = null,
        )
        messageDatabaseReference.push().setValue(message)
        binding.editTextMessage.setText("")
    }

    override fun onResume() {
        super.onResume()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onPause() {
        super.onPause()
        auth.removeAuthStateListener(authStateListener)
        detachDatabaseReadListener()
        messageAdapter.clear()
    }


}
