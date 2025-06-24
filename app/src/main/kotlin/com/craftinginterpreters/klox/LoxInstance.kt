package com.craftinginterpreters.klox

class LoxInstance(val klass: LoxClass) {
    val fields = mutableMapOf<String, Any?>()

    override fun toString(): String {
        return klass.name + " instance"
    }

    fun get(token: Token): Any? {
        val name = token.lexeme
        if (fields.containsKey(name)) {
            return fields[name]
        }
        val method = klass.findMethod(name)
        if (method != null) {
            return method.bind(this)
        }
        throw RuntimeException("Undefined property '" + name + "'.")
    }

    fun set(name: Token, value: Any?) {
        fields.set(name.lexeme, value)
    }
}
