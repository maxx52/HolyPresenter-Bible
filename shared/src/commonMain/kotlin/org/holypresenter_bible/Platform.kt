package org.holypresenter_bible

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform