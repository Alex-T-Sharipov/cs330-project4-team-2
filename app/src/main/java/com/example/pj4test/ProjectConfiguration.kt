package com.example.pj4test

import android.graphics.Color

class ProjectConfiguration {
    companion object {
        // colors
        var activeBackgroundColor: Int = Color.parseColor("#FF9800")
        const val activeTextColor: Int = Color.WHITE // Color.parseColor("#FFFFFF")
        const val idleBackgroundColor: Int = Color.WHITE // Color.parseColor("#FFFFFF")
        const val idleTextColor: Int = Color.BLACK // Color.parseColor("#000000")
    }
}