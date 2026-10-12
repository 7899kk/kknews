package com.karthikkilari.expiryradar.backup

object Csv {
    fun encode(rows: List<List<String>>): String =
        rows.joinToString("\r\n") { row ->
            row.joinToString(",") { "\"${it.replace("\"","\"\"")}\"" }
        }

    fun decode(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var quote = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' ->
                    if (quote && i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        quote = !quote
                    }
                !quote && c == ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                }
                !quote && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(field.toString())
                    field.setLength(0)
                    rows.add(row.toList())
                    row.clear()
                }
                else -> field.append(c)
            }
            i++
        }
        require(!quote) { "Unclosed quoted field" }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row.toList())
        }
        return rows
    }
}
