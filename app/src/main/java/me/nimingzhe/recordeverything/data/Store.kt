package me.nimingzhe.recordeverything.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Snapshot(
    val todos: List<Todo> = emptyList()
)

/**
 * 单文件 JSON 持久层。原 App 用 Room + 22 个版本迁移，这里刻意简化：
 * 整个库就是一个 JSON 文件，写盘用「临时文件 + rename」，导出即分享这份结构。
 */
class Store(private val context: Context) {

    private val file: File
        get() = File(context.filesDir, "recordeverything.json")

    fun load(): Snapshot {
        val f = file
        if (!f.exists()) return Snapshot()
        return try {
            val root = JSONObject(f.readText())
            Snapshot(todos = root.optJSONArray("todos").toTodos())
        } catch (t: Throwable) {
            Snapshot()
        }
    }

    fun save(snapshot: Snapshot) {
        val root = JSONObject()
        root.put("schema", 2)
        root.put("savedAt", System.currentTimeMillis())
        root.put("todos", JSONArray().also { arr -> snapshot.todos.forEach { arr.put(it.toJson()) } })
        val tmp = File(context.filesDir, "recordeverything.json.tmp")
        tmp.writeText(root.toString())
        val target = file
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }

    fun clearAll() {
        file.delete()
    }
}

// ================= JSON 映射 =================

fun Todo.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("note", note)
    put("due", due ?: JSONObject.NULL)
    put("time", time ?: JSONObject.NULL)
    put("done", done)
    put("doneAt", doneAt ?: JSONObject.NULL)
    put("important", important)
    put("group", group)
    put("createdAt", createdAt)
}

fun JSONObject.toTodo(): Todo = Todo(
    id = optString("id").ifBlank { newId() },
    title = optString("title"),
    note = optString("note"),
    due = if (isNull("due")) null else optLong("due"),
    time = if (isNull("time")) null else optInt("time"),
    done = optBoolean("done"),
    doneAt = if (isNull("doneAt")) null else optLong("doneAt"),
    important = optBoolean("important"),
    group = optString("group").ifBlank { Todo.DEFAULT_GROUP },
    createdAt = optLong("createdAt", System.currentTimeMillis())
)

fun JSONArray?.toTodos(): List<Todo> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.toTodo() }
}
