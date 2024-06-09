package edu.put.holidayexplorer

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import edu.put.holidayexplorer.databinding.FragmentDetailsBinding
import edu.put.holidayexplorer.dbHandler.MyDBHandler
import kotlinx.coroutines.Dispatchers
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime


class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private var yourLocalization = false
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)

        val passedObject = arguments?.getString("item")?.let { JSONObject(it) }
        val countryCode = passedObject?.getString("countryCode")

        val url = MainActivity.COUNTRY_API_URL + "?country=" + countryCode + "&username=d_drozdzynski"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        var responseBodyObj = JSONObject()
                        try {
                            responseBodyObj = JSONObject(response.body!!.string())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        withContext(Dispatchers.Main) {
                            if (passedObject != null && responseBodyObj != null) {
                                val responseBody = responseBodyObj.getJSONArray("geonames").getJSONObject(0)
                                initMap(responseBody, passedObject)
                                showDetails(responseBody, passedObject)
                                binding.favouriteButton.setOnClickListener {
                                    addToFavourites(responseBody, passedObject)
                                }
                            }
                        }
                    }
                }
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initMap(responseBody: JSONObject, passedObject: JSONObject) {
        val lat = (responseBody.getString("south").toDouble() +
                responseBody.getString("north").toDouble()) / 2
        val lng = (responseBody.getString("east").toDouble() +
                responseBody.getString("west").toDouble()) / 2
        val point = GeoPoint(lat, lng)

        binding.osmmap.setMultiTouchControls(true)
        binding.osmmap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        binding.osmmap.controller.setZoom(5.0)
        binding.osmmap.controller.setCenter(point)

        val marker = Marker(binding.osmmap)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = passedObject.getString("localName")
        marker.snippet = "(" + passedObject.getString("name") + ")"
        val drawable = ResourcesCompat.getDrawable(requireActivity().resources,R.drawable.marker_icon,null)
        marker.icon = drawable
        binding.osmmap.overlays.add(marker)

        if (FirstFragment.userPoint == null) {
            Toast.makeText(requireContext(), "Can't find your localization!", Toast.LENGTH_SHORT).show()
        }
        else {
            val markerUser = Marker(binding.osmmap)
            markerUser.position = FirstFragment.userPoint
            markerUser.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            markerUser.title = "Your localization:"
            markerUser.snippet = FirstFragment.userPoint.toString()
            val drawableUser = ResourcesCompat.getDrawable(
                requireActivity().resources,
                R.drawable.marker_user_icon,
                null
            )
            markerUser.icon = drawableUser
            binding.osmmap.overlays.add(markerUser)

            binding.yourLocalizationButton.setOnClickListener {
                if (yourLocalization) {
                    yourLocalization = false
                    binding.osmmap.controller.animateTo(point, binding.osmmap.zoomLevelDouble, 1000L)
                }
                else {
                    yourLocalization = true
                    binding.osmmap.controller.animateTo(FirstFragment.userPoint, binding.osmmap.zoomLevelDouble, 1000L)
                }
            }
        }
    }

    private fun showDetails(responseBody: JSONObject, passedObject: JSONObject) {
        val name = passedObject.getString("name") + "\n"
        val country = responseBody.getString("countryName") +
                " (" + passedObject.getString("countryCode") + ")\n"
        val date = passedObject.getString("date") + "\n"

        val text = name + country + date
        binding.detailsDescription.text = text

        val global = passedObject.getBoolean("global")
        var globalText = ""
        if (global) {
            globalText = "Public holiday in every county (federal state)"
        } else {
            globalText = "Public holiday only in counties:\n"
            val counties = passedObject.getJSONArray("counties")
            for (i in 0..<counties.length()) {
                globalText += counties.get(i)
                globalText += " "
            }
            globalText += "\n"

        }

        binding.additionalInfo.text = globalText
    }

    fun addToFavourites(responseBody: JSONObject, passedObject: JSONObject) {
        val dbHandler = activity?.let { MyDBHandler(it.applicationContext, null, null, 1) }
        val name = passedObject.getString("name") + "\n"
        val country = responseBody.getString("countryName") +
                " (" + passedObject.getString("countryCode") + ")\n"
        val date = passedObject.getString("date") + "\n"

        dbHandler?.addRecord(name, country, date, LocalDateTime.now().toString())
        Toast.makeText(requireContext(), "Added to Your Favourite List!", Toast.LENGTH_SHORT).show()
    }

}