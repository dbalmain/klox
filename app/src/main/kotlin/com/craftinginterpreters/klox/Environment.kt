package com.craftinginterpreters.klox

class Environment(private val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    @Deprecated("This function is deprecated and may be removed in future versions. Use getAt")
    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }
        @Suppress("DEPRECATION") if (enclosing != null) return enclosing.get(name)

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    private fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 1..distance) {
            val nextEnvironment = environment.enclosing
            if (nextEnvironment == null) {
                // This should ideally not happen if 'distance' is correct
                // and refers to a depth within the known chain.
                throw IllegalStateException(
                        "Requested ancestor distance $distance is too deep or structure is invalid."
                )
            }
            environment = nextEnvironment
        }
        return environment
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    fun assignAt(distance: Int, name: String, value: Any?) {
        ancestor(distance).values[name] = value
    }

    override fun toString(): String {
        var builder = StringBuilder()
        builder.append("(")
        for ((k, v) in values) {
            builder.append(" ").append(k).append(": ").append(v)
        }
        if (this.enclosing != null) {
            builder.append(this.enclosing.toString())
        }
        builder.append(" )")
        return builder.toString()
    }
}
