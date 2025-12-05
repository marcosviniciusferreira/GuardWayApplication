package com.example.guardwayapplication

interface OnMapDataFound {
    fun onAddressFound(address: String)
    fun onOccurrenceDataReceived(data: ApiService.OcorrenciaCepResponse)
    fun onError(message: String)
}