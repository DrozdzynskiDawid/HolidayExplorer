package edu.put.holidayexplorer

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.fragment.findNavController
import edu.put.holidayexplorer.databinding.ActivityFavlistBinding
import edu.put.holidayexplorer.dbHandler.MyDBHandler
import java.util.concurrent.Executor


class FavListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavlistBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavlistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT)
                        .show()
                    showFav()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Please authenticate")
            .setSubtitle("Use your fingerprint to get to your Favourites List")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
        binding.clearList.setOnClickListener{
            AlertDialog.Builder(this)
                .setMessage("Are you sure you want to delete all the saved items from your Favourites List?")
                .setPositiveButton("Yes") { dialog, which ->
                    deleteItem("all")
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, which ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    fun showFav() {
        val dbHandler = MyDBHandler(this.applicationContext, null, null, 1)
        val data = dbHandler.showList()
        binding.favListTable.removeAllViews()
        for (record in data) {
            val tableRow = TableRow(this)
            val divider = TableRow(this)
            val name = TextView(this)
            val deleteButton = ImageView(this)
            val added = TextView(this)

            val holidayText = record[1] + record[2] + record[3].split("\n")[0]
            name.text = holidayText
            name.gravity = Gravity.CENTER
            name.textSize = 16F
            name.width = 300
            name.setTypeface(null, Typeface.BOLD)
            val addedText = "Added:\n"  + record[4].split('T')[0] + "\n" + record[4].split('T')[1].split('.')[0]
            added.text = addedText
            added.gravity = Gravity.CENTER
            added.textSize = 15F
            added.width = 500
            deleteButton.setImageResource(R.drawable.delete_icon)
            deleteButton.setOnClickListener{
                deleteItem(record[0])
            }

            tableRow.addView(name)
            tableRow.addView(added)
            tableRow.addView(deleteButton)
            tableRow.setPadding(10,30,10,30)
            tableRow.setBackgroundResource(R.drawable.table_row_shape)
            tableRow.minimumHeight = 250
            tableRow.gravity = Gravity.CENTER

            divider.minimumHeight = 30
            binding.favListTable.addView(tableRow)
            binding.favListTable.addView(divider)
        }
    }

    private fun deleteItem(name: String) {
        val dbHandler = MyDBHandler(this.applicationContext, null, null, 1)
        val success = dbHandler.deleteFromDB(name)
        if (success) {
            Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show()
            showFav()
        }
    }
}