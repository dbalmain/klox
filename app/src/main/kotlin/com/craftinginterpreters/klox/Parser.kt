package com.craftinginterpreters.klox

// Represents the Pratt Parser
class Parser(private val tokens: List<Token>) {
    // Custom exception for parsing errors
    private class ParseError : RuntimeException()

    private var current = 0 // Current token we're looking at

    fun parse(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()
        while (!isAtEnd()) {
            statements.add(declaration()) // Start with declaration rule
        }
        return statements
    }

    // expression     → equality ;
    private fun expression(): Expr {
        return assignment() // Start with the lowest precedence
    }

    // declaration    → varDecl | statement ;
    private fun declaration(): Stmt? {
        return try {
            if (match(TokenType.VAR)) varDeclaration() else statement()
        } catch (error: ParseError) {
            synchronize() // Attempt to recover
            null
        }
    }

    // statement      → exprStmt | printStmt | block ;
    private fun statement(): Stmt {
        if (match(TokenType.PRINT)) return printStatement()
        if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block().filterNotNull())
        // ... add ifStatement, whileStatement, forStatement later
        return expressionStatement()
    }

    // block          → "{" declaration* "}" ;
    private fun block(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration())
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    // varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) {
            initializer = expression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    // --- Expression parsing methods (from Chapter 6, with 'assignment' as new entry point) ---
    // assignment     → IDENTIFIER "=" assignment | equality ; (equality was term for Ch6)
    private fun assignment(): Expr {
        val expr = equality() // Or 'or()' when logical operators are added

        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment() // Right-associative

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }
            // else if (expr is Expr.Get) { ... for properties ... }

            error(
                    equals,
                    "Invalid assignment target."
            ) // Report error but don't throw ParseError from here
        }
        return expr
    }

    // equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    // comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private fun comparison(): Expr {
        var expr = term()

        while (match(
                TokenType.GREATER,
                TokenType.GREATER_EQUAL,
                TokenType.LESS,
                TokenType.LESS_EQUAL
        )) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    // term           → factor ( ( "-" | "+" ) factor )* ;
    private fun term(): Expr {
        var expr = factor()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    // factor         → unary ( ( "/" | "*" ) unary )* ;
    private fun factor(): Expr {
        var expr = unary()

        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    // unary          → ( "!" | "-" ) unary | primary ;
    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary() // Recursive call for unary
            return Expr.Unary(operator, right)
        }
        return primary()
    }

    // primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER
    // ;
    private fun primary(): Expr {
        if (match(TokenType.FALSE)) return Expr.Literal(false)
        if (match(TokenType.TRUE)) return Expr.Literal(true)
        if (match(TokenType.NIL)) return Expr.Literal(null)

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return Expr.Literal(previous().literal)
        }

        if (match(TokenType.IDENTIFIER)) {
            return Expr.Variable(previous())
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        // If no primary expression matches, it's an error.
        throw error(peek(), "Expect expression.")
    }

    // --- Helper Methods ---
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun error(token: Token, message: String): ParseError {
        Klox.error(token, message) // Use Klox's error reporting
        return ParseError() // Throw custom exception to unwind
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return
            when (peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.VAR,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.PRINT,
                TokenType.RETURN -> return
                else -> {
                    /* Do nothing. */
                }
            }
            advance()
        }
    }
}
