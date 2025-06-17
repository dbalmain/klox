package com.craftinginterpreters.klox

interface LoxCallable {
    fun arity(): Int // Number of arguments it expects
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}
