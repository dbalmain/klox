package com.craftinginterpreters.klox

import java.util.Stack // Using Java's Stack for simplicity, like the book

// Enum to track if we are inside a function, and later, a class or initializer
private enum class FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD
}

private enum class ClassType {
    NONE,
    CLASS /*, SUBCLASS */
}

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes = Stack<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE

    fun resolve(statements: List<Stmt?>) {
        for (statement in statements) {
            if (statement != null) resolve(statement)
        }
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return // Global scope, nothing to do here for resolver

        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            Klox.error(name, "Already a variable with this name in this scope.")
        }
        scope[name.lexeme] = false // false means "declared but not yet defined/initialized"
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.peek()[name.lexeme] = true // true means "fully defined and ready"
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in (scopes.size - 1) downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i) // Pass the depth
                return
            }
        }
        // Not found locally, assume global (interpreter handles this by not finding it in 'locals'
        // map)
    }

    // --- Stmt.Visitor Methods ---
    override fun visit(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visit(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS
        declare(stmt.name)
        define(stmt.name)
        beginScope()
        scopes.peek()["this"] = true
        // if (stmt.superclass != null) resolve(stmt.superclass)
        stmt.methods.forEach {
            resolveFunction(
                    it,
                    if (it.name.lexeme == "init") {
                        FunctionType.INITIALIZER
                    } else {
                        FunctionType.METHOD
                    }
            )
        }
        endScope()
        currentClass = enclosingClass
    }

    override fun visit(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visit(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name) // Define eagerly for recursion within its own scope
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }

    override fun visit(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let { resolve(it) }
    }

    override fun visit(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visit(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            Klox.error(stmt.keyword, "Can't return from top-level code.")
        } else if (stmt.value != null && currentFunction == FunctionType.INITIALIZER) {
            Klox.error(stmt.keyword, "Can't return from initializer.")
        }
        stmt.value?.let { resolve(it) }
    }

    override fun visit(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.let { resolve(it) }
        define(stmt.name)
    }

    override fun visit(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    // --- Expr.Visitor Methods ---
    override fun visit(expr: Expr.Variable) {
        if (scopes.isNotEmpty() && scopes.peek()[expr.name.lexeme] == false) {
            Klox.error(expr.name, "Can't read local variable in its own initializer.")
        }
        resolveLocal(expr, expr.name)
    }

    override fun visit(expr: Expr.Assign) {
        resolve(expr.value) // Resolve the value being assigned
        resolveLocal(expr, expr.name) // Resolve the variable being assigned to
    }

    // For other expression types, just resolve their sub-expressions
    override fun visit(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }
    override fun visit(expr: Expr.Call) {
        resolve(expr.callee)
        expr.arguments.forEach { resolve(it) }
    }
    override fun visit(expr: Expr.Get) {
        resolve(expr.objectExpr)
    }
    override fun visit(expr: Expr.Grouping) {
        resolve(expr.expression)
    }
    override fun visit(expr: Expr.Literal) {
        /* No variables to resolve */
    }
    override fun visit(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }
    override fun visit(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.objectExpr)
    }
    override fun visit(expr: Expr.Super) {
        /* For Chapter 13 */
    }
    override fun visit(expr: Expr.This) {
        if (currentClass == ClassType.NONE) {
            Klox.error(expr.keyword, "Can't use 'this' outside of a class.")
        }
        resolveLocal(expr, expr.keyword)
    }
    override fun visit(expr: Expr.Unary) {
        resolve(expr.right)
    }
}
