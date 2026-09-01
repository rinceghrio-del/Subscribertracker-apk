package com.rustech.subscribertracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rustech.subscribertracker.databinding.ActivityDashboardBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        binding.tvName.text = user.email ?: "Subscriber"
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        requestNotificationPermissionIfNeeded()
        loadSubscriberRecord(user.email!!.lowercase())
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    private fun loadSubscriberRecord(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("subscribers").document(email).get()
            .addOnSuccessListener { doc ->
                binding.progressBar.visibility = View.GONE
                if (doc.exists()) {
                    val subscriber = doc.toObject(Subscriber::class.java)
                    if (subscriber != null) {
                        displaySubscriber(subscriber)
                        scheduleReminders(subscriber)
                    }
                } else {
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load record: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun displaySubscriber(subscriber: Subscriber) {
        val name = subscriber.name.ifEmpty { auth.currentUser?.email ?: "Subscriber" }
        binding.tvName.text = name
        binding.tvStatus.text = "Status: ${subscriber.status.replaceFirstChar { it.uppercase() }}"

        subscriber.dueDate?.let {
            val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            binding.tvDueDate.text = sdf.format(it.toDate())
        } ?: run {
            binding.tvDueDate.text = "Not set"
        }

        if (subscriber.monthlyAmount > 0) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
            binding.tvAmount.text = formatter.format(subscriber.monthlyAmount)
        } else {
            binding.tvAmount.text = "--"
        }
    }

    /**
     * Schedules local reminder notifications: 3 days before, 1 day before, and on the due date.
     * Uses WorkManager one-time work with an initial delay, so it survives app restarts
     * (as long as the device isn't rebooted — for reboot survival we'd also need a
     * BOOT_COMPLETED receiver to reschedule, which can be added later).
     */
    private fun scheduleReminders(subscriber: Subscriber) {
        val dueDate = subscriber.dueDate?.toDate() ?: return
        val now = Date()

        val offsets = listOf(
            Triple(3L, "Payment Due in 3 Days", "Your subscription payment is due in 3 days."),
            Triple(1L, "Payment Due Tomorrow", "Your subscription payment is due tomorrow."),
            Triple(0L, "Payment Due Today", "Your subscription payment is due today.")
        )

        val workManager = WorkManager.getInstance(applicationContext)

        for ((daysBefore, title, message) in offsets) {
            val triggerTime = dueDate.time - TimeUnit.DAYS.toMillis(daysBefore)
            val delay = triggerTime - now.time
            if (delay <= 0) continue // don't schedule reminders in the past

            val data = Data.Builder()
                .putString(ReminderWorker.KEY_TITLE, title)
                .putString(ReminderWorker.KEY_MESSAGE, message)
                .putInt(ReminderWorker.KEY_NOTIF_ID, 2000 + daysBefore.toInt())
                .build()

            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            workManager.enqueueUniqueWork(
                "reminder_${daysBefore}d",
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
