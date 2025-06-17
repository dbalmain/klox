package com.craftinginterpreters.klox

class LoxFunction(
        private val declaration: Stmt.Function,
        private val closure: Environment // The environment where the function was DECLARED
) : LoxCallable {

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment =
                Environment(
                        closure
                ) // Create new environment for execution, enclosing the declaration env
        for (i in declaration.params.indices) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Interpreter.Return) { // Catch custom Return exception
            return returnValue.value
        }
        return null // Implicit return nil if no return statement was executed
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}
