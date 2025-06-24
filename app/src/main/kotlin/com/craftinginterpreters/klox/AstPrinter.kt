package com.craftinginterpreters.klox

class AstPrinter : Expr.Visitor<String>, Stmt.Visitor<String> {

    override fun visit(stmt: Stmt.Block): String {
        val builder = StringBuilder()
        builder.append("(block")
        for (statement in stmt.statements) {
            builder.append(" ").append(statement.accept(this))
        }
        builder.append(")")
        return builder.toString()
    }

    override fun visit(stmt: Stmt.Class): String {
        val builder = StringBuilder()
        builder.append("(class ${stmt.name}")
        for (method in stmt.methods) {
            builder.append(" ").append(method.accept(this))
        }
        builder.append(")")
        return builder.toString()
    }

    override fun visit(stmt: Stmt.Expression): String {
        return parenthesize("expression", stmt.expression)
    }

    override fun visit(stmt: Stmt.Print): String {
        return parenthesize("print", stmt.expression)
    }

    override fun visit(stmt: Stmt.Var): String {
        if (stmt.initializer != null) {
            return parenthesize("var ${stmt.name.lexeme}", stmt.initializer)
        }
        return "(var ${stmt.name.lexeme})"
    }

    override fun visit(stmt: Stmt.If): String {
        val builder = StringBuilder()
        builder.append("(if ").append(stmt.condition.accept(this))
        builder.append(" ").append(stmt.thenBranch.accept(this))
        if (stmt.elseBranch != null) {
            builder.append(" ").append(stmt.elseBranch.accept(this))
        }
        builder.append(")")

        return builder.toString()
    }

    override fun visit(stmt: Stmt.While): String {
        val builder = StringBuilder()
        builder.append("(while ")
                .append(stmt.condition.accept(this))
                .append(stmt.body.accept(this))
                .append(")")

        return builder.toString()
    }

    override fun visit(stmt: Stmt.Function): String {
        val builder = StringBuilder()
        builder.append("(fun ${stmt.name.lexeme} (")
        for (param in stmt.params) {
            builder.append(param.lexeme).append(" ")
        }
        if (stmt.params.isNotEmpty()) {
            builder.setLength(builder.length - 1) // Remove trailing space
        }
        builder.append(") ")
        for (bodyStmt in stmt.body) {
            builder.append(bodyStmt.accept(this)).append(" ")
        }
        if (stmt.body.isNotEmpty()) {
            builder.setLength(builder.length - 1) // Remove trailing space
        }
        builder.append(")")
        return builder.toString()
    }

    override fun visit(stmt: Stmt.Return): String {
        if (stmt.value != null) {
            return parenthesize("return", stmt.value)
        }
        return "(return)"
    }

    fun print(expr: Expr?): String {
        return expr?.accept(this) ?: "null_expr"
    }

    fun print(stmts: List<Stmt?>): String {
        val builder = StringBuilder()
        for (stmt in stmts) {
            if (stmt != null) {
                builder.append(stmt.accept(this)).append("\n")
            }
        }
        return builder.toString()
    }

    override fun visit(expr: Expr.Assign): String {
        return parenthesize("assign ${expr.name.lexeme}", expr.value)
    }

    override fun visit(expr: Expr.Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visit(expr: Expr.Call): String {
        return parenthesize("call ${print(expr.callee)}", *expr.arguments.toTypedArray())
    }

    override fun visit(expr: Expr.Get): String {
        return parenthesize(
                ".${expr.name.lexeme}",
                expr.objectExpr
        ) // Assuming 'objectExpr' field name
    }

    override fun visit(expr: Expr.Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visit(expr: Expr.Literal): String {
        if (expr.value == null) return "nil"
        return expr.value.toString()
    }

    override fun visit(expr: Expr.Logical): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visit(expr: Expr.Set): String {
        return parenthesize(
                "set .${expr.name.lexeme}",
                expr.objectExpr,
                expr.value
        ) // Assuming 'objectExpr'
    }

    override fun visit(expr: Expr.Super): String {
        return "(super.${expr.method.lexeme})"
    }

    override fun visit(expr: Expr.This): String {
        return "this"
    }

    override fun visit(expr: Expr.Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    override fun visit(expr: Expr.Variable): String {
        return expr.name.lexeme
    }

    private fun parenthesize(name: String, vararg exprs: Expr?): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr?.accept(this) ?: "null_arg")
        }
        builder.append(")")
        return builder.toString()
    }
}
