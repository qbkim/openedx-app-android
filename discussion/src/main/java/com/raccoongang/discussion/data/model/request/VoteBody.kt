package com.raccoongang.discussion.data.model.request

import com.google.gson.annotations.SerializedName

data class VoteBody(
    @SerializedName("voted")
    val voted: Boolean
)