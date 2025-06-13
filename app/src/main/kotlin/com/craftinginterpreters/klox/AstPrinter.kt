package com.craftinginterpreters.klox

// Assuming Expr.kt and Token.kt (with TokenType.kt) are in the same package
// and TokenType has MINUS, STAR, etc.

class AstPrinter : Expr.Visitor<String> {

    fun print(expr: Expr?): String {
        return expr?.accept(this) ?: "null_expr"
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
