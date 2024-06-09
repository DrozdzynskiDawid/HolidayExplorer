package edu.put.holidayexplorer

import android.R
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import edu.put.holidayexplorer.databinding.FragmentSecondBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.time.LocalDate


class SecondFragment : Fragment() {

private var _binding: FragmentSecondBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

      _binding = FragmentSecondBinding.inflate(inflater, container, false)
      return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = MainActivity.HOLIDAY_API_URL + "/AvailableCountries"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        var responseBody = JSONArray()
                        try {
                            responseBody = JSONArray(response.body!!.string())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        withContext(Dispatchers.Main) {
                            if (responseBody != null) {
                                showSpinner(responseBody)
                            }

                        }
                    }
                }
            }
        })
    }

    private fun showSpinner(responseBody: JSONArray) {
        val data = arrayListOf("Select country")
        for (i in 0..<responseBody.length()) {
            val item = responseBody.getJSONObject(i)
            val itemText = item.getString("name") + " (" + item.getString("countryCode") + ")"
            data.add(itemText)
        }
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_dropdown_item, data)
        binding.spinnerCountries.adapter = adapter
        binding.spinnerCountries.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val value = parent?.getItemAtPosition(position).toString()
                    val countryCode = value.split(" ")[1].substring(1,3)
                    val currentYear = LocalDate.now().year

                    val url = MainActivity.HOLIDAY_API_URL + "/LongWeekend/" + currentYear + "/" + countryCode
                    val request = Request.Builder().url(url).build()
                    val client = OkHttpClient()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            e.printStackTrace()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    var responseBodyWeekend = JSONArray()
                                    try {
                                        responseBodyWeekend = JSONArray(response.body!!.string())
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (responseBodyWeekend != null) {
                                            showLongWeekends(responseBodyWeekend)
                                        }

                                    }
                                }
                            }
                        }
                    })

                    val urlHolidays = MainActivity.HOLIDAY_API_URL + "/PublicHolidays/" + currentYear + "/" + countryCode
                    val requestHolidays = Request.Builder().url(urlHolidays).build()
                    val clientHolidays = OkHttpClient()

                    clientHolidays.newCall(requestHolidays).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            e.printStackTrace()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    var responseBodyHolidays = JSONArray()
                                    try {
                                        responseBodyHolidays = JSONArray(response.body!!.string())
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (responseBodyHolidays != null) {
                                            showHolidays(responseBodyHolidays)
                                        }

                                    }
                                }
                            }
                        }
                    })
                }
            }

        }
    }

    private fun showLongWeekends(responseBody: JSONArray) {
        binding.longWeekendTable.removeAllViews()
        for (i in 0..<responseBody.length()) {
            val item = responseBody.getJSONObject(i)
            val tableRow = TableRow(activity)
            val divider = TableRow(activity)
            val dates = TextView(activity)
            val daysNumber = TextView(activity)
            val bridgeDay = TextView(activity)

            val datesText = "From: " + item.getString("startDate") + "\nTo: " + item.getString("endDate")
            dates.text = datesText
            dates.gravity = Gravity.CENTER
            dates.textSize = 18F
            dates.width = 400

            val daysNumberText = "Days: " + item.getInt("dayCount").toString()
            daysNumber.text = daysNumberText
            daysNumber.gravity = Gravity.CENTER
            daysNumber.textSize = 18F
            daysNumber.width = 200

            val bridgeDayExists = item.getBoolean("needBridgeDay")
            bridgeDay.text = "Bridge day:\nNo"
            if (bridgeDayExists) {
                bridgeDay.text = "Bridge day:\nYes"
            }
            bridgeDay.gravity = Gravity.CENTER
            bridgeDay.textSize = 18F
            bridgeDay.width = 300

            tableRow.addView(dates)
            tableRow.addView(daysNumber)
            tableRow.addView(bridgeDay)
            tableRow.setPadding(10,30,10,30)
            tableRow.setBackgroundResource(edu.put.holidayexplorer.R.drawable.table_row_shape)
            tableRow.minimumHeight = 250
            tableRow.gravity = Gravity.CENTER

            divider.minimumHeight = 30
            binding.longWeekendTable.addView(tableRow)
            binding.longWeekendTable.addView(divider)
        }
    }

    private fun showHolidays(responseBody: JSONArray) {
        binding.holidayTable.removeAllViews()
        for (i in 0..<responseBody.length()) {
            val item = responseBody.getJSONObject(i)
            val tableRow = TableRow(activity)
            val divider = TableRow(activity)
            val date = TextView(activity)
            val name = TextView(activity)
            val country = TextView(activity)
            val icon = ImageView(activity)

            date.text = item.getString("date")
            date.gravity = Gravity.CENTER
            date.textSize = 17F
            date.width = 200
            name.text = item.getString("localName")
            name.gravity = Gravity.CENTER
            name.textSize = 17F
            name.width = 500
            name.setTypeface(null, Typeface.BOLD)
            country.text = item.getString("countryCode")
            country.gravity = Gravity.CENTER
            country.textSize = 17F
            country.width = 100
            icon.setImageResource(edu.put.holidayexplorer.R.drawable.baseline_chevron_right_black_24dp)
            icon.foregroundGravity = Gravity.CENTER

            tableRow.addView(date)
            tableRow.addView(name)
            tableRow.addView(country)
            tableRow.addView(icon)
            tableRow.setPadding(10,30,10,30)
            tableRow.setBackgroundResource(edu.put.holidayexplorer.R.drawable.table_row_shape)
            tableRow.minimumHeight = 250
            tableRow.gravity = Gravity.CENTER

            // listener na elementach listy
            tableRow.setOnClickListener() {
                val bundle = Bundle()
                bundle.putString("item",item.toString())
                findNavController().navigate(edu.put.holidayexplorer.R.id.action_SecondFragment_to_detailsFragment, bundle)
            }

            divider.minimumHeight = 30
            binding.holidayTable.addView(tableRow)
            binding.holidayTable.addView(divider)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}