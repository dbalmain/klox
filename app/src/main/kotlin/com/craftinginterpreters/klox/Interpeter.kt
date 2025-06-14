// klox/app/src/main/kotlin/com/craftinginterpreters/klox/Interpreter.kt
package com.craftinginterpreters.klox

// Custom exception for runtime errors
class RuntimeError(val token: Token, override val message: String) : RuntimeException(message)

class Interpreter : Expr.Visitor<Any?> {

    fun interpret(expression: Expr?) {
        try {
            val value = evaluate(expression)
            println(stringify(value))
        } catch (error: RuntimeError) {
            Klox.runtimeError(error) // Delegate to Klox for reporting
        }
    }

    private fun evaluate(expr: Expr?): Any? {
        return expr?.accept(this)
    }

    override fun visit(expr: Expr.Assign): Any? {
        // We'll implement this in a later chapter (Chapter 8)
        Klox.runtimeError(RuntimeError(expr.name, "Assignment not yet implemented."))
        return null
    }

    override fun visit(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) > (right as Double)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) >= (right as Double)
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < (right as Double)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) <= (right as Double)
            }
            TokenType.BANG_EQUAL -> !isEqual(left, right)
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) - (right as Double)
            }
            TokenType.PLUS -> {
                if (left is Double && right is Double) {
                    return left + right
                }
                if (left is String && right is String) {
                    return left + right
                }
                throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                if (right as Double == 0.0) { // Check for division by zero
                    throw RuntimeError(expr.operator, "Division by zero.")
                }
                left as Double / right
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }
            else -> null // Unreachable.
        }
    }

    override fun visit(expr: Expr.Call): Any? {
        // We'll implement this in a later chapter (Chapter 10)
        Klox.runtimeError(RuntimeError(expr.paren, "Function calls not yet implemented."))
        return null
    }

    override fun visit(expr: Expr.Get): Any? {
        // We'll implement this in a later chapter (Chapter 12)
        Klox.runtimeError(RuntimeError(expr.name, "Properties not yet implemented."))
        return null
    }

    override fun visit(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visit(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visit(expr: Expr.Logical): Any? {
        // We'll implement this in a later chapter (Chapter 9)
        Klox.runtimeError(RuntimeError(expr.operator, "Logical operators not yet implemented."))
        return null
    }

    override fun visit(expr: Expr.Set): Any? {
        // We'll implement this in a later chapter (Chapter 12)
        Klox.runtimeError(RuntimeError(expr.name, "Set properties not yet implemented."))
        return null
    }

    override fun visit(expr: Expr.Super): Any? {
        Klox.runtimeError(RuntimeError(expr.keyword, "Super expressions not yet implemented."))
        return null
    }

    override fun visit(expr: Expr.This): Any? {
        Klox.runtimeError(RuntimeError(expr.keyword, "This expressions not yet implemented."))
        return null
    }

    override fun visit(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.BANG -> !isTruthy(right)
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }
            else -> null // Unreachable.
        }
    }

    override fun visit(expr: Expr.Variable): Any? {
        // We'll implement this in a later chapter (Chapter 8)
        Klox.runtimeError(RuntimeError(expr.name, "Variables not yet implemented."))
        return null
    }

    // --- Helper methods for type checking and truthiness ---
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun isTruthy(obj: Any?): Boolean {
        if (obj == null) return false // nil is falsey
        if (obj is Boolean) return obj // Booleans are themselves
        return true // Everything else is truthy
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        // Standard Kotlin equality (structural for data classes, referential for others)
        return a == b
    }

    private fun stringify(obj: Any?): String {
        if (obj == null) return "nil"

        // Handle Doubles to print integers nicely (e.g., 5.0 as "5")
        if (obj is Double) {
            var text = obj.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
            }
            return text
        }
        return obj.toString()
    }
}
