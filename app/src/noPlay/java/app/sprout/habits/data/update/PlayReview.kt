// Same signature as the play build's object, whose parameter this stand-in doesn't need.
@file:Suppress("UNUSED_PARAMETER")

package app.sprout.habits.data.update

import android.app.Activity

/** The foss and github builds have no Google Play code: this stands in for the play build's object. */
object PlayReview {
    fun launch(activity: Activity): Boolean = false
}
