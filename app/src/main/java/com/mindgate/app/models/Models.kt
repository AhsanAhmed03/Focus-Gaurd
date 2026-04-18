package com.mindgate.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScreenContent(
    val title: String = "Pause & Reflect",
    val quote: String = "Take a breath. Do you really need to check your phone right now?",
    val imageUri: String? = null,
    val durationSeconds: Int = 10,
    val triggerType: TriggerType = TriggerType.APP_LAUNCH,
    val packageName: String? = null
) : Parcelable

enum class TriggerType {
    LOCK_SCREEN, APP_LAUNCH
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?,
    var isSelected: Boolean = false
)

data class QuoteItem(
    val text: String,
    val author: String = ""
)

object DefaultQuotes {
    val quotes = listOf(
        QuoteItem("Almost everything will work again if you unplug it for a few minutes — including you.", "Anne Lamott"),
        QuoteItem("The present moment is the only moment available to us, and it is the door to all moments.", "Thich Nhat Hanh"),
        QuoteItem("Disconnect to reconnect.", ""),
        QuoteItem("Your life is happening right now, not on a screen.", ""),
        QuoteItem("Be where your feet are.", ""),
        QuoteItem("Stop scrolling. Start living.", ""),
        QuoteItem("Nature does not hurry, yet everything is accomplished.", "Lao Tzu"),
        QuoteItem("In the middle of difficulty lies opportunity.", "Albert Einstein"),
        QuoteItem("Real life is happening beyond the screen.", ""),
        QuoteItem("You don't have to be available to everyone at all times.", ""),
        QuoteItem("Put down your phone and pick up your life.", ""),
        QuoteItem("Mindfulness is a way of befriending ourselves and our experience.", "Jon Kabat-Zinn")
    )
}
