package com.rustech.subscribertracker

import com.google.firebase.Timestamp

data class Subscriber(
    val name: String = "",
    val email: String = "",
    val dueDate: Timestamp? = null,
    val monthlyAmount: Double = 0.0,
    val status: String = "active"
)
