package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.utils.TimeUtils
import java.util.*

data class CourseDetails(
    @SerializedName("blocks_url")
    val blocksUrl: String?,
    @SerializedName("course_id")
    val courseId: String?,
    @SerializedName("effort")
    val effort: String?,
    @SerializedName("end")
    val end: String?,
    @SerializedName("enrollment_end")
    val enrollmentEnd: String?,
    @SerializedName("enrollment_start")
    val enrollmentStart: String?,
    @SerializedName("hidden")
    val hidden: Boolean?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("invitation_only")
    val invitationOnly: Boolean?,
    @SerializedName("media")
    val media: Media?,
    @SerializedName("mobile_available")
    val mobileAvailable: Boolean?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("number")
    val number: String?,
    @SerializedName("org")
    val org: String?,
    @SerializedName("pacing")
    val pacing: String?,
    @SerializedName("short_description")
    val shortDescription: String?,
    @SerializedName("start")
    val start: String?,
    @SerializedName("start_display")
    val startDisplay: String?,
    @SerializedName("start_type")
    val startType: String?,
    @SerializedName("overview")
    val overview: String?
) {

    fun mapToDomain(): Course {
        return Course(
            id = id ?: "",
            blocksUrl = blocksUrl ?: "",
            courseId = courseId ?: "",
            effort = effort ?: "",
            enrollmentStart = TimeUtils.iso8601ToDate(enrollmentStart ?: ""),
            enrollmentEnd = TimeUtils.iso8601ToDate(enrollmentEnd ?: ""),
            hidden = hidden ?: false,
            invitationOnly = invitationOnly ?: false,
            mobileAvailable = mobileAvailable ?: false,
            name = name ?: "",
            number = number ?: "",
            org = org ?: "",
            shortDescription = shortDescription ?: "",
            start = start ?: "",
            end = end ?: "",
            startDisplay = startDisplay ?: "",
            startType = startType ?: "",
            pacing = pacing ?: "",
            overview = overview ?: "",
            media = media?.mapToDomain() ?: com.raccoongang.core.domain.model.Media()
        )
    }
}
