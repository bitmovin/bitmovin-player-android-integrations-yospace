package com.bitmovin.player.integration.yospace

internal class ListenerActionRegistry<K, A> {
    private val actionsByKey = mutableMapOf<K, MutableList<A>>()

    @Synchronized
    fun add(key: K, action: A) {
        actionsByKey.getOrPut(key) { mutableListOf() }.add(action)
    }

    @Synchronized
    fun removeLast(key: K): A? {
        val actions = actionsByKey[key] ?: return null
        val action = actions.removeAt(actions.lastIndex)
        if (actions.isEmpty()) {
            actionsByKey.remove(key)
        }
        return action
    }

    @Synchronized
    fun remove(key: K, action: A) {
        val actions = actionsByKey[key] ?: return
        actions.remove(action)
        if (actions.isEmpty()) {
            actionsByKey.remove(key)
        }
    }

    @Synchronized
    fun removeAll(matchesKey: (K) -> Boolean): List<A> {
        val keys = actionsByKey.keys.filter(matchesKey)
        return keys.flatMap { key ->
            actionsByKey.remove(key).orEmpty()
        }
    }
}
