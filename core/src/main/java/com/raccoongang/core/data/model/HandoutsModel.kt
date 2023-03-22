package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.HandoutsModel

data class HandoutsModel(
    @SerializedName("handouts_html")
    val handoutsHtml: String
) {
    fun mapToDomain() = HandoutsModel(handoutsHtml)
}
