package com.junkfood.seal.ui.common

// Modified by the Trawl project on 2026-08-25 (GPL-3.0 section 5(a)).
// Changes: removed the DONATE / SUPPORT_DEVELOPER / CRYPTO_DONATION / SPONSORS routes
// along with the pages behind them.

object Route {

    const val HOME = "home"
    const val DOWNLOADS = "download_history"
    const val PLAYLIST = "playlist"
    const val SETTINGS = "settings"
    const val FORMAT_SELECTION = "format"
    const val TASK_LIST = "task_list"
    const val TASK_LOG = "task_log"

    const val SETTINGS_PAGE = "settings_page"

    const val APPEARANCE = "appearance"
    const val INTERACTION = "interaction"
    const val GENERAL_DOWNLOAD_PREFERENCES = "general_download_preferences"
    const val ABOUT = "about"
    const val DOWNLOAD_DIRECTORY = "download_directory"
    const val CREDITS = "credits"
    const val LANGUAGES = "languages"
    const val TEMPLATE = "template"
    const val TEMPLATE_EDIT = "template_edit"
    const val DARK_THEME = "dark_theme"
    const val DOWNLOAD_QUEUE = "queue"
    const val DOWNLOAD_FORMAT = "download_format"
    const val NETWORK_PREFERENCES = "network_preferences"
    const val COOKIE_PROFILE = "cookie_profile"
    const val COOKIE_GENERATOR_WEBVIEW = "cookie_webview"
    const val SUBTITLE_PREFERENCES = "subtitle_preferences"
    const val AUTO_UPDATE = "auto_update"
    const val TROUBLESHOOTING = "troubleshooting"
    const val SEALPLUS_EXTRAS = "sealplus_extras"
    const val SECURITY_SETTINGS = "security_settings"
    const val ONBOARDING = "onboarding"
    const val HIDDEN_CONTENT = "hidden_content"
    const val MORE_TOOLS = "more_tools"
    const val LINKS_HISTORY = "links_history"
    const val BATCH_URL_IMPORT = "batch_url_import"
    const val VIDEO_INFO_DOWNLOAD = "video_info_download"
    const val VIDEO_INFO_DETAIL = "video_info_detail"
    const val THUMBNAIL_DOWNLOAD = "thumbnail_download"
    const val COMMENT_DOWNLOAD = "comment_download"
    const val COMMENT_DETAIL = "comment_detail"

    const val TASK_HASHCODE = "task_hashcode"
    const val TEMPLATE_ID = "template_id"
    const val VIDEO_INFO_ID = "video_info_id"
    const val COMMENT_SET_ID = "comment_set_id"
}

infix fun String.arg(arg: String) = "$this/{$arg}"

infix fun String.id(id: Int) = "$this/$id"
