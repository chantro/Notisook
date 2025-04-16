package com.example.fdea.data
//사용자 정보 저장
enum class Major{

}
data class AppUser (
    val username:String="",
    val studentNum:String="", //학번
    val phoneNum:String="",  //전화번호
    val major:String="",  //전공
    val lockerLocation:String="",
    val lockerNum:String="",
    val approved: Boolean = false,   //인증 여부
    val rejected: Boolean = false,
    val infoUpdatedAfterRejection: Boolean = false
)