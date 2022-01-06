package me.ikirby.shareagent.entity

data class AppItem(
    val label: String,
    val packageName: String,
    val name: String
) {
    fun saveString(): String {
        return "$label;$packageName;$name"
    }

    companion object {
        fun fromSaveString(str: String?): AppItem? {
            if (str.isNullOrBlank()) {
                return null
            }
            val items = str.split(";")
            if (items.size != 3) {
                return null
            }
            return AppItem(items[0], items[1], items[2])
        }
    }
}
