package com.craftinginterpreters.klox

class LoxFunction(
        private val declaration: Stmt.Function,
        private val closure: Environment,
        private val isInitializer: Boolean = false
) : LoxCallable {

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in declaration.params.indices) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Interpreter.Return) { // Catch custom Return exception
            if (isInitializer) return closure.getAt(0, "this")
            return returnValue.value
        }
        if (isInitializer) return closure.getAt(0, "this")
        return null // Implicit return nil if no return statement was executed
    }

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment)
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}
