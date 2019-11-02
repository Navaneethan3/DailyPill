package com.example.thyroidhelper

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import android.view.*
import android.widget.TextView
import java.text.DateFormat

class MainActivity : AppCompatActivity(), SharedPreferencesListener {

    private var resetMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (DataModel.isFirstDay()) {
            // This could be the first time we have opened the app since it was installed.
            Notifications.setAlarm()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        resetMenuItem = menu.findItem(R.id.action_reset)
        resetMenuItem!!.isEnabled = DataModel.hasTakenDrugToday()
        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals(DataModel.DRUG_TAKEN_TIMESTAMP)) {
            updateUI(true)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI(false)
        DataModel.addListener(this)
    }

    override fun onStop() {
        super.onStop()
        DataModel.removeListener(this)
    }

    private fun updateUI(useFade: Boolean) {
        val drugTaken = DataModel.hasTakenDrugToday()
        val fragment =
            if (drugTaken) { MedicineTakenFragment()    }
            else           { MedicineNotTakenFragment() }

        resetMenuItem?.isEnabled = drugTaken
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            if (useFade) {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
            commit()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun performUpdateTime(btn: View) {
        DataModel.takeDrugNow()
    }

    private fun doReset() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.reset_confirmation_title)
            .setMessage(R.string.reset_confirmation_message)
            .setPositiveButton(R.string.reset_confirmation_ok) { _, _ ->
                DataModel.unsetDrugTakenTimestamp()
            }
            .setNegativeButton(R.string.reset_confirmation_cancel,null)
            .create()
        dialog.show()
    }

    private fun doAddNotification() {
        Notifications.sendMorningReminderNotification()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reset -> {
                doReset()
                return true
            }
            R.id.action_register_notification -> {
                doAddNotification()
                return true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        // Fallback
        return super.onOptionsItemSelected(item)
    }

    class MedicineNotTakenFragment : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_medicine_not_taken, container, false)
        }
    }

    class MedicineTakenFragment : Fragment() {

        private lateinit var drugTakenMessage: String
        private lateinit var drugTakenMessageView: TextView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            drugTakenMessage =  getString(R.string.drug_taken_message)
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val root = inflater.inflate(R.layout.fragment_medicine_taken, container, false)
            drugTakenMessageView = root.findViewById(R.id.drug_taken_message)
            return root
        }

        override fun onResume() {
            val timestamp = DataModel.getDrugTakenTimestamp()
            val timeStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(timestamp)
            // Use non-breaking space to avoid a line-break between 6:00 and AM
            val nbspTimeStr = timeStr.replace(" ", "\u00A0" )
            drugTakenMessageView.text = String.format(drugTakenMessage, nbspTimeStr)

            super.onResume()
        }

    }
}
