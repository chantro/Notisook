package com.example.fdea.data

// 장소 데이터를 위한 데이터 클래스
data class BenefitLocation(
    var name: String = "",
    var major: String = "",
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var benefit: String = "",
    val id: String = ""
)
