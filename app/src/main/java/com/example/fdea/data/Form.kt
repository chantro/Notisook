package com.example.fdea.data

//사물함 form

data class Form(
    val id: String = "",
    val major: String = "",
    val type: String = "",
    val formName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val persons: String="",
    val enrolled: Boolean = false,
    val paidFee: Boolean = false,
    val content: String = "",

    var applicantInfos: List<ApplicantInfo?> = emptyList(), // 신청자 정보 리스트
    var lockerInfo: List<LockerDetail> =emptyList()  //사물함에 대한 정보
)
data class ApplicantInfo(
    val user: AppUser = AppUser(),  // 현재 로그인한 사용자 정보
    val appliedTime:Long=0L, // 사용자의 신청 시간 타임스탬프 리스트
    val lockerNum: String? = null,  // 신청한 사물함 번호, 기본값은 null
    val lockerLocation:String=""  // 사물함 위치, 기본값은 null
)
data class LockerDetail(  //한 구역의 사물함
    var location:String="",  //사물함의 위치 ex> 순헌관 지하1층A구역
    var width:String="",     // 가로 개수 - 나중에 int 로 바꿔
    var height: String=""  //세로 개수 - 나중에 int로 바꾸기

)


