package com.craftinginterpreters.klox

// Represents the Pratt Parser
class Parser(private val tokens: List<Token>) {
    // Custom exception for parsing errors
    private class ParseError : RuntimeException()

    private var current = 0 // Current token we're looking at

    fun parse(): Expr? {
        return try {
            expression()
        } catch (error: ParseError) {
            null // Return null if there was a syntax error
        }
    }

    // expression     → equality ;
    private fun expression(): Expr {
        return equality() // Start with the lowest precedence
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

        // For Chapter 6, we don't have variables yet, so IDENTIFIER would be an error.
        // We'll add variable handling later. For now, an unexpected IDENTIFIER would fall through
        // to the error at the end of this function if not handled by grouping.

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

    // For Chapter 6, synchronize is not yet implemented, but it's good to have the placeholder
    // private fun synchronize() {
    //     advance()
    //     while (!isAtEnd()) {
    //         if (previous().type == TokenType.SEMICOLON) return
    //         when (peek().type) {
    //             TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR,
    //             TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
    //             else -> { /* Do nothing. */ }
    //         }
    //         advance()
    //     }
    // }
}
