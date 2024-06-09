package edu.put.holidayexplorer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import edu.put.holidayexplorer.databinding.FragmentFirstBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.io.IOException
import java.net.URL

class FirstFragment : Fragment() {

    companion object {
        var userPoint: GeoPoint? = null
        const val PERMISSION_REQUEST_LOCATION = 1001
    }

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        findLocalization()
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = MainActivity.HOLIDAY_API_URL + "/NextPublicHolidaysWorldwide"
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
                                buildTable(responseBody)
                            }
                            binding.plannerButton.setOnClickListener {
                                findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
                            }
                            val scaleUp = ScaleAnimation(
                                0.3f, 1.0f,  // od 1 do 1.5 w poziomie
                                0.3f, 1.0f,  // od 1 do 1.5 w pionie
                                Animation.RELATIVE_TO_SELF, 0.5f,
                                Animation.RELATIVE_TO_SELF, 0.5f
                            )
                            scaleUp.duration = 2000

                            val rotate = RotateAnimation(
                                0.0f, 360f, Animation.RELATIVE_TO_SELF,
                                0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                            )
                            rotate.duration = 2000

                            val animationSet = AnimationSet(true)
                            animationSet.addAnimation(scaleUp)
                            animationSet.addAnimation(rotate)

                            binding.appLogo.startAnimation(animationSet)
                        }
                    }
                }
            }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildTable(responseBody: JSONArray) {
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
            icon.setImageResource(R.drawable.baseline_chevron_right_black_24dp)
            icon.foregroundGravity = Gravity.CENTER
            tableRow.addView(date)
            tableRow.addView(name)
            tableRow.addView(country)
            tableRow.addView(icon)
            tableRow.setPadding(10,30,10,30)
            tableRow.setBackgroundResource(R.drawable.table_row_shape)
            tableRow.minimumHeight = 250
            tableRow.gravity = Gravity.CENTER

            // listener na elementach listy
            tableRow.setOnClickListener() {
                val bundle = Bundle()
                bundle.putString("item",item.toString())
                findNavController().navigate(R.id.action_FirstFragment_to_detailsFragment, bundle)
            }

            divider.minimumHeight = 30
            binding.nextHolidaysTable.addView(tableRow)
            binding.nextHolidaysTable.addView(divider)
        }
    }

    private fun findLocalization() {
        Configuration.getInstance().load(context, androidx.preference.PreferenceManager.getDefaultSharedPreferences(context))

        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val locationRequest = LocationRequest.create().apply {
            interval = 100
            fastestInterval = 50
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        userPoint = GeoPoint(latitude, longitude)
                    }
                }
                if (userPoint == null) {
                    userPoint = GeoPoint(0, 0)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_LOCATION
            )
        }
    }
}
