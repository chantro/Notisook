package com.example.fdea.data

// 스크랩 해서 캘린더에 추가한 공지사항의 신청 기간 스케줄
//시작일, 마감일

data class Schedule(
    val id: String = "",
    val title: String = "",
    val startDate: String = "",
    val endDate: String = ""
)