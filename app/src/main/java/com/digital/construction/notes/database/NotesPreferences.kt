package com.digital.construction.notes.database

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.digital.construction.notes.fragments.NotesListFragment
import timber.log.Timber

private const val DARK_MODE_KEY = "dark_mode"
private const val SORT_MODE_KEY = "sort_mode"
private const val FONT_SIZE_KEY = "font_size"
private const val INTRODUCTION_SEEN_KEY = "introduction_seen"
private const val SWIPE_DELETE_KEY = "swipe_delete"
private const val SWIPE_OPEN_KEY = "swipe_open"

const val DARK_MODE_LIGHT = "1"
const val DARK_MODE_SYSTEM = "0"
const val DARK_MODE_DARK = "-1"

private const val TAG = "NotesPreferences"

class NotesPreferences private constructor(context: Context) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val widgetNotesPreferences = mutableListOf<Preference<*>>()
    val widgetNotes: List<Preference<*>>
        get() = widgetNotesPreferences.toList()

    private val preDefinedKeys = listOf(
        DARK_MODE_KEY,
        INTRODUCTION_SEEN_KEY,
        SORT_MODE_KEY,
        FONT_SIZE_KEY,
        SWIPE_OPEN_KEY,
        SWIPE_DELETE_KEY
    )

    init {
        Timber.tag(TAG)

        checkDarkModePreference()
        createDynamicPreferences()
        Timber.d("Created $widgetNotesPreferences")
    }

    /**
     * Updating from version 1.1.0 to 1.1.1 would always crash on startup because dark mode was
     * stored as a boolean in 1.1.0 but as a string in 1.1.1, thus crashing when trying to cast.
     *
     * This function is a 1.1.2 hotfix for that
     */
    private fun checkDarkModePreference() {
        try {
            sharedPreferences.getString(DARK_MODE_KEY, null)
        } catch (error: ClassCastException) {
            Timber.e(error, "Detected incorrect dark mode preference object type!")

            sharedPreferences.edit {
                remove(DARK_MODE_KEY)
            }
        }
    }

    inner class Preference<T : Any>(val key: String, private val defValue: T, value: T? = null) {
        private val ERROR_MSG = "Object (${defValue::class.simpleName}) is not allowed"

        var value: T = defValue
            get() {
                val result = when (defValue) {
                    is Boolean -> sharedPreferences.getBoolean(key, defValue)
                    is String -> sharedPreferences.getString(key, defValue)!!
                    is Long -> sharedPreferences.getLong(key, defValue)
                    is Int -> sharedPreferences.getInt(key, defValue)
                    is Float -> sharedPreferences.getFloat(key, defValue)
                    else -> throw IllegalStateException(ERROR_MSG)
                }

                return result as T
            }
            set(value) {
                field = value

                sharedPreferences.edit {
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is String -> putString(key, value)
                        is Long -> putLong(key, value)
                        is Int -> putInt(key, value)
                        is Float -> putFloat(key, value)
                        else -> throw IllegalStateException(ERROR_MSG)
                    }
                }
            }

        init {
            if (value != null) this.value = value
        }

        fun delete() {
            widgetNotesPreferences.remove(this)

            sharedPreferences.edit {
                remove(key)
            }
        }

        override fun toString() = "Preference($key, $defValue, $value)"
    }

    val all: Map<String, *>
        get() = sharedPreferences.all

    val darkMode = Preference(DARK_MODE_KEY, DARK_MODE_SYSTEM)
    val introductionSeen = Preference(INTRODUCTION_SEEN_KEY, false)
    var savedSortBy = Preference(SORT_MODE_KEY, NotesListFragment.SortBy.ASCENDING.name)
    val fontSizePercentage = Preference(FONT_SIZE_KEY, "100")
    val swipeToOpenOn = Preference(SWIPE_OPEN_KEY, true)
    val swipeToDeleteOn = Preference(SWIPE_DELETE_KEY, true)

    fun getPreference(key: String): Preference<*>? {
        val result: Preference<*>? = widgetNotesPreferences.find { it.key == key }

        Timber.d("Got $result for key=$key")

        return result
    }

    fun <T : Any> createPreference(key: String, defValue: T, value: T? = null): Preference<T> {
        val preference = Preference(key, defValue, value)
        widgetNotesPreferences.add(preference)

        Timber.d("Creating $preference")
        return preference
    }

    /**
     * There are SharedPreferences that don't have constant keys like the SharedPreferences
     * for the widgets that use the widget id as the key and the note for that widget as the value
     *
     * This creates a list of all those SharedPreferences
     */
    private fun createDynamicPreferences() {
        for (item in all) {
            if (item.key !in preDefinedKeys) {
                createPreference(item.key, item.value!!, item.value!!)
            }
        }
    }

    // Should be called every time the theme is changed to apply it
    fun applyDarkMode() {
        when (darkMode.value) {
            DARK_MODE_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            DARK_MODE_SYSTEM -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            DARK_MODE_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    companion object Factory {
        private var INSTANCE: NotesPreferences? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = NotesPreferences(context)
            }
        }

        fun get() = INSTANCE ?: throw IllegalStateException("NotesPreferences must be initialized")
    }
}