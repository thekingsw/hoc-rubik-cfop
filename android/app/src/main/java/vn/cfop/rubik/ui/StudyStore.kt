package vn.cfop.rubik.ui

import android.content.Context
import android.content.SharedPreferences

/** Lưu/đọc StudyData trong SharedPreferences của máy. */
class StudyStore(private val prefs: SharedPreferences) {
    fun load(): StudyData = StudyData.decode(prefs.getString(KEY, null))

    fun save(data: StudyData) {
        prefs.edit().putString(KEY, data.encode()).apply()
    }

    companion object {
        private const val KEY = "f2l_study"

        fun from(context: Context): StudyStore =
            StudyStore(context.getSharedPreferences("cfop_study", Context.MODE_PRIVATE))
    }
}
