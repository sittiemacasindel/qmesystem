package com.qme.mobile.data.repository
 
import com.qme.mobile.data.api.ApiService
import com.qme.mobile.data.model.response.ApiResponse
import com.qme.mobile.data.model.response.HistoryEntryResponse
import com.qme.mobile.data.model.response.JoinQueueResponse
import com.qme.mobile.data.model.response.OrganizationResponse
import com.qme.mobile.data.model.response.QueueDetailsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
 
/**
* Handles all customer-facing queue operations:
* lookup by code, join, view current, cancel, and history.
*/
class QueueRepository(private val api: ApiService) {
 
    fun lookupByCode(code: String, onResult: (OrganizationResponse?, String?) -> Unit) {
        api.getOrganizationByCode(code).enqueue(object : Callback<ApiResponse<OrganizationResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<OrganizationResponse>>,
                response: Response<ApiResponse<OrganizationResponse>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    onResult(response.body()!!.data, null)
                } else {
                    val msg = when (response.code()) {
                        404  -> "No queue found with that code."
                        401  -> "Please log in to look up a queue."
                        500  -> "Server error. Please try again later."
                        else -> "Could not find queue (${response.code()})."
                    }
                    onResult(null, msg)
                }
            }
 
            override fun onFailure(call: Call<ApiResponse<OrganizationResponse>>, t: Throwable) {
                onResult(null, "Network error. Please check your connection.")
            }
        })
    }
 
    fun joinQueue(queueCode: String, onResult: (JoinQueueResponse?, String?) -> Unit) {
        api.joinQueue(queueCode).enqueue(object : Callback<ApiResponse<JoinQueueResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<JoinQueueResponse>>,
                response: Response<ApiResponse<JoinQueueResponse>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    onResult(response.body()!!.data, null)
                } else {
                    val msg = when (response.code()) {
                        400  -> "You are already in this queue or it is not accepting customers."
                        401  -> "Please log in to join a queue."
                        404  -> "Queue not found."
                        500  -> "Server error. Please try again later."
                        else -> "Failed to join queue (${response.code()})."
                    }
                    onResult(null, msg)
                }
            }
 
            override fun onFailure(call: Call<ApiResponse<JoinQueueResponse>>, t: Throwable) {
                onResult(null, "Network error. Please check your connection.")
            }
        })
    }
 
    fun getMyQueue(onResult: (QueueDetailsResponse?, String?) -> Unit) {
        api.getMyQueue().enqueue(object : Callback<ApiResponse<QueueDetailsResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<QueueDetailsResponse>>,
                response: Response<ApiResponse<QueueDetailsResponse>>
            ) {
                if (response.isSuccessful) {
                    onResult(response.body()?.data, null)
                } else {
                    when (response.code()) {
                        // 404 means the customer has no active queue (served/cancelled externally)
                        404  -> onResult(null, null)
                        401  -> onResult(null, "Session expired. Please log in again.")
                        500  -> onResult(null, "Server error. Please try again later.")
                        else -> onResult(null, "Failed to load queue (${response.code()}).")
                    }
                }
            }
 
            override fun onFailure(call: Call<ApiResponse<QueueDetailsResponse>>, t: Throwable) {
                onResult(null, "Network error. Please check your connection.")
            }
        })
    }
 
    fun cancelQueue(entryId: String, onResult: (Boolean, String?) -> Unit) {
        api.cancelQueue(entryId).enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(
                call: Call<ApiResponse<Void>>,
                response: Response<ApiResponse<Void>>
            ) {
                if (response.isSuccessful) {
                    onResult(true, null)
                } else {
                    val msg = when (response.code()) {
                        400  -> "Queue entry could not be cancelled."
                        401  -> "Session expired. Please log in again."
                        404  -> "Queue entry not found."
                        500  -> "Server error. Please try again later."
                        else -> "Failed to cancel queue (${response.code()})."
                    }
                    onResult(false, msg)
                }
            }
 
            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                onResult(false, "Network error. Please check your connection.")
            }
        })
    }
 
    fun getHistory(onResult: (List<HistoryEntryResponse>?, String?) -> Unit) {
        api.getHistory().enqueue(object : Callback<ApiResponse<List<HistoryEntryResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<HistoryEntryResponse>>>,
                response: Response<ApiResponse<List<HistoryEntryResponse>>>
            ) {
                if (response.isSuccessful) {
                    onResult(response.body()?.data ?: emptyList(), null)
                } else {
                    val msg = when (response.code()) {
                        401  -> "Session expired. Please log in again."
                        500  -> "Server error. Please try again later."
                        else -> "Failed to load history (${response.code()})."
                    }
                    onResult(null, msg)
                }
            }
 
            override fun onFailure(call: Call<ApiResponse<List<HistoryEntryResponse>>>, t: Throwable) {
                onResult(null, "Network error. Please check your connection.")
            }
        })
    }
}