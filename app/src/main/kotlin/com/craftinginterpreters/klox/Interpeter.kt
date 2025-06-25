package com.craftinginterpreters.klox

import java.util.IdentityHashMap

// Custom exception for runtime errors
class RuntimeError(val token: Token, override val message: String) : RuntimeException(message)

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Any?> {
    val globals = Environment()
    private var environment = globals
    private val locals = IdentityHashMap<Expr, Int>()
    class Return(val value: Any?) : RuntimeException(null, null, false, false)

    init {
        // Define native clock() function
        globals.define(
                "clock",
                object : LoxCallable {
                    override fun arity(): Int = 0
                    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                        return System.currentTimeMillis() / 1000.0
                    }
                    override fun toString(): String = "<native fn>"
                }
        )
    }

    fun interpret(statements: List<Stmt?>) {
        try {
            for (statement in statements) {
                if (statement != null) execute(statement)
            }
        } catch (error: RuntimeError) {
            Klox.runtimeError(error) // Delegate to Klox for reporting
        }
    }

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    // For executing blocks in a new scope
    fun executeBlock(statements: List<Stmt?>, blockEnvironment: Environment) {
        val previous = this.environment
        try {
            this.environment = blockEnvironment
            for (statement in statements) {
                if (statement != null) execute(statement)
            }
        } finally {
            this.environment = previous // Restore previous environment
        }
    }

    private fun evaluate(expr: Expr?): Any? {
        return expr?.accept(this)
    }
    // --- Stmt.Visitor methods ---
    override fun visit(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment)) // New environment for the block
    }

    override fun visit(stmt: Stmt.Class) {
        environment.define(stmt.name.lexeme, null)
        val superclass =
                if (stmt.superclass != null) {
                    val superclass = evaluate(stmt.superclass)
                    if (superclass is LoxClass) {
                        environment = Environment(environment)
                        environment.define("super", superclass)
                        superclass
                    } else {
                        throw RuntimeError(stmt.superclass.name, "Superclass must be a class.")
                    }
                } else {
                    null
                }
        val methods = mutableMapOf<String, LoxFunction>()
        stmt.methods.forEach {
            methods.put(it.name.lexeme, LoxFunction(it, environment, it.name.lexeme == "init"))
        }
        val klass = LoxClass(stmt.name.lexeme, superclass, methods)
        // reset environment
        if (superclass != null) {
            environment = environment.enclosing!!
        }
        environment.assign(stmt.name, klass)
    }

    override fun visit(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment) // Capture current environment as closure
        environment.define(stmt.name.lexeme, function)
    }

    override fun visit(stmt: Stmt.Return) {
        val value = if (stmt.value != null) evaluate(stmt.value) else null
        throw Return(value) // Throw custom exception to unwind call stack
    }

    override fun visit(stmt: Stmt.Expression) {
        evaluate(stmt.expression) // Evaluate and discard
    }

    override fun visit(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visit(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visit(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visit(stmt: Stmt.Var) {
        var value: Any? = null
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer)
        }
        environment.define(stmt.name.lexeme, value)
    }

    // --- Expr.Visitor methods ---
    override fun visit(expr: Expr.Assign): Any? {
        val distance = locals.get(expr)
        val value = evaluate(expr.value)
        if (distance == null) return globals.assignAt(0, expr.name.lexeme, value)
        else environment.assignAt(distance, expr.name.lexeme, value)
        return value
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
        val callee = evaluate(expr.callee)
        val arguments = mutableListOf<Any?>()
        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }

        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes. ${expr.callee}")
        }

        val function = callee // Already checked it's LoxCallable
        if (arguments.size != function.arity()) {
            throw RuntimeError(
                    expr.paren,
                    "Expected ${function.arity()} arguments but got ${arguments.size}."
            )
        }

        return function.call(this, arguments)
    }

    override fun visit(expr: Expr.Get): Any? {
        val obj = evaluate(expr.objectExpr)
        if (obj is LoxInstance) {
            return obj.get(expr.name)
        }
        throw RuntimeError(expr.name, "Only instances have properties.")
    }

    override fun visit(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visit(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visit(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left // Short-circuit
        } else { // AND
            if (!isTruthy(left)) return left // Short-circuit
        }
        return evaluate(expr.right)
    }

    override fun visit(expr: Expr.Set): Any? {
        val obj = evaluate(expr.objectExpr)
        if (obj is LoxInstance) {
            val value = evaluate(expr.value)
            obj.set(expr.name, value)
            return value
        }

        throw RuntimeError(expr.name, "Only instances have fields.")
    }

    override fun visit(expr: Expr.Super): Any? {
        val distance = locals.get(expr)!!
        val superclass = environment.getAt(distance, "super") as LoxClass
        val thisObj = environment.getAt(distance - 1, "this") as LoxInstance
        val method = superclass.findMethod(expr.method.lexeme)
        if (method == null) {
            throw RuntimeError(expr.method, "Undefined property '${expr.method.lexeme}'.")
        }
        return method.bind(thisObj)
    }

    override fun visit(expr: Expr.This): Any? {
        return lookupVariable(expr.keyword, expr)
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
        return lookupVariable(expr.name, expr)
    }

    // --- Helper methods for type checking and truthiness ---
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands [${left}] and [${right}] must be numbers.")
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

    fun resolve(expr: Expr, depth: Int) {
        // println(":::${expr} - ${depth}")
        locals.put(expr, depth)
    }

    private fun lookupVariable(name: Token, expr: Expr): Any? {
        val distance = locals.get(expr)
        // println("Getting ${name.lexeme}:${distance}")
        // println(environment)
        // println(locals)
        if (distance == null) return globals.getAt(0, name.lexeme)
        else return environment.getAt(distance, name.lexeme)
    }
}
