package com.rylderoliveira.friendlychat

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.AnonymousBuilder
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
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
    private var childEventListener: ChildEventListener? = null
    private var userName: String? = null
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { result ->
        onSignInResult(result)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_logout -> auth.signOut()
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
                val providers = arrayListOf(
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.EmailBuilder().build(),
                )
                val intent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setIsSmartLockEnabled(false)
                    .setAvailableProviders(providers)
                    .build()
                signInLauncher.launch(intent)
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
        childEventListener = null
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult?) {
        val response = result?.idpResponse
        if (result?.resultCode == RESULT_OK) {
            userName = auth.currentUser.toString()
            onSignedInInitialize(userName)
        } else {
            val errorCode = response?.error?.errorCode
            handleSignInError(errorCode)
        }
    }

    private fun handleSignInError(errorCode: Int?) {
        when (errorCode) {
            ErrorCodes.UNKNOWN_ERROR -> finish()
            null -> finish()
        }
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
}
