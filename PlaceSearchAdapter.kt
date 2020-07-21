package com.ansitindia.citysupport.adapter

import android.app.Activity
import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ansitindia.citysupport.R
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class PlaceSearchAdapter(val activity : Activity) :
    RecyclerView.Adapter<PlaceSearchAdapter.ViewHolder>(),Filterable {

    private var placesClient: PlacesClient
    private var clickListener: ClickListener? = null
    private var mResultList: ArrayList<PlaceAutocomplete> = ArrayList()
    private var STYLE_BOLD: CharacterStyle? = null
    private var STYLE_NORMAL: CharacterStyle? = null

    interface ClickListener {
        fun click(place: Place?)
    }


    fun setClickListener(clickListener: ClickListener) {
        this.clickListener = clickListener
    }

    init {
        if(!Places.isInitialized()){
            Places.initialize(activity!!,activity.getString(R.string.google_maps_key))
        }
        STYLE_BOLD = StyleSpan(Typeface.BOLD);
        STYLE_NORMAL = StyleSpan(Typeface.NORMAL);
        placesClient = com.google.android.libraries.places.api.Places.createClient(activity);
    }

    class ViewHolder(view : View): RecyclerView.ViewHolder(view) {
        val tv_address : TextView= view.findViewById(R.id.tv_address)
        val tv_area : TextView= view.findViewById(R.id.tv_area)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(R.layout.rv_place,parent,false))
    }

    override fun getItemCount(): Int {
        return mResultList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      holder.tv_address.text = mResultList.get(position).address
      holder.tv_area.text = mResultList.get(position).area
    }

    override fun getFilter(): Filter? {
        return object : Filter() {
             override fun performFiltering(constraint: CharSequence?): FilterResults? {
                val results = FilterResults()
                // Skip the autocomplete query if no constraints are given.
                if (constraint != null) {
                    // Query the autocomplete API for the (constraint) search string.
                    mResultList = getPredictions(constraint)
                    if (mResultList != null) {
                        // The API successfully returned results.
                        results.values = mResultList
                        results.count = mResultList.size
                    }
                }
                return results
            }

            protected override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {
                if (results != null && results.count > 0) {
                    // The API returned at least one result, update the data.
                    notifyDataSetChanged()
                } else {
                    // The API did not return any results, invalidate the data set.
                    //notifyDataSetInvalidated();
                }
            }
        }
    }

    private fun getPredictions(constraint: CharSequence): ArrayList<PlaceAutocomplete> {
        val resultList: ArrayList<PlaceAutocomplete> = ArrayList()

        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling fetchPlace()).
        val token = AutocompleteSessionToken.newInstance()

        //https://gist.github.com/graydon/11198540
        // Use the builder to create a FindAutocompletePredictionsRequest.
        val request =
            FindAutocompletePredictionsRequest.builder() // Call either setLocationBias() OR setLocationRestriction().
                //.setLocationBias(bounds)
                //.setCountry("BD")
                //.setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .setQuery(constraint.toString())
                .build()
        val autocompletePredictions =
            placesClient.findAutocompletePredictions(request)

        // This method should have been called off the main UI thread. Block and wait for at most
        // 60s for a result from the API.
        try {
            Tasks.await(
                autocompletePredictions,
                60,
                TimeUnit.SECONDS
            )
        } catch (e: Exception) {
            Toast.makeText(activity, ""+e.message, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        return if (autocompletePredictions.isSuccessful) {
            val findAutocompletePredictionsResponse =
                autocompletePredictions.result
            if (findAutocompletePredictionsResponse != null) for (prediction in findAutocompletePredictionsResponse.autocompletePredictions) {
                Log.i("TAG", prediction.placeId)
                resultList.add(
                    PlaceAutocomplete(
                        prediction.placeId,
                        prediction.getPrimaryText(STYLE_NORMAL).toString(),
                        prediction.getFullText(STYLE_BOLD).toString()
                    )
                )
            }
            resultList
        } else {
            resultList
        }
    }

   data class PlaceAutocomplete (
        var placeId: CharSequence,
        var area: CharSequence,
        var address: CharSequence
    )
}
