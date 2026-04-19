package com.floracare.app.domain.model

import kotlinx.datetime.Instant

data class Plant(
    val id: String,
    val nickname: String,
    val speciesId: String?,
    val locationTag: LocationTag,
    val acquiredAt: Instant,
    val coverPhotoUri: String?,
    val notes: String,
    val archived: Boolean = false,
)
