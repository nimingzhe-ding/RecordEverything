package me.nimingzhe.recordeverything.data

/** 16 位短 id，够用且不依赖数据库自增。 */
fun newId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
