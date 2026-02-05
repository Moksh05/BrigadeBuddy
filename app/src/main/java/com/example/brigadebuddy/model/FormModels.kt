package com.example.brigadebuddy.model

data class OfficerForm(
    var firstName: String = "",
    var lastName: String = "",
    var rank: String = "",
    var gender: String = Gender.MALE,

    var birthDay: Int = 0,
    var birthMonth: Int = 0,
    var birthYear: Int = 0,

    // optional sections
    var hasWorkAnniversary: Boolean = false,
    var workDay: Int = 0,
    var workMonth: Int = 0,
    var workYear: Int = 0,

    var isMarried: Boolean = false,
    var spouse: SpouseForm? = null,

    var children: MutableList<ChildForm> = mutableListOf()
)

// 🔹 SPOUSE
data class SpouseForm(
    var name: String = "",
    var gender: String = Gender.FEMALE,

    var birthDay: Int = 0,
    var birthMonth: Int = 0,
    var birthYear: Int = 0,

    var anniversaryDay: Int = 0,
    var anniversaryMonth: Int = 0,
    var anniversaryYear: Int = 0
)

// 🔹 CHILD
data class ChildForm(
    var name: String = "",
    var gender: String = Gender.MALE,

    var birthDay: Int = 0,
    var birthMonth: Int = 0,
    var birthYear: Int = 0
)