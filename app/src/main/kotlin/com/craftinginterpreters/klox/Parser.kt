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

    // declaration    → funDecl | varDecl | statement ;
    private fun declaration(): Stmt? {
        return try {
            if (match(TokenType.FUN)) funDeclaration("function") // Pass kind
            else if (match(TokenType.VAR)) varDeclaration() else statement()
        } catch (error: ParseError) {
            synchronize() // Attempt to recover
            null
        }
    }

    // statement      → exprStmt | printStmt | block ;
    private fun statement(): Stmt {
        if (match(TokenType.FOR)) return forStatement()
        if (match(TokenType.IF)) return ifStatement()
        if (match(TokenType.PRINT)) return printStatement()
        if (match(TokenType.WHILE)) return whileStatement()
        if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block().filterNotNull())
        if (match(TokenType.RETURN)) return returnStatement()
        return expressionStatement()
    }

    private fun funDeclaration(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        return function(kind, name) // Pass token for name
    }

    // Helper for parsing function parameters and body (reused for methods)
    // Changed to take name Token directly
    private fun function(kind: String, name: Token): Stmt.Function {
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block() // block() returns List<Stmt?>
        return Stmt.Function(
                name,
                parameters,
                body.filterNotNull()
        ) // Filter out nulls from error recovery
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        var value: Expr? = null
        if (!check(TokenType.SEMICOLON)) {
            value = expression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
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

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = statement()
        var elseBranch: Stmt? = null
        if (match(TokenType.ELSE)) {
            elseBranch = statement()
        }
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition.")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

        // Initializer
        val initializer: Stmt? =
                when {
                    match(TokenType.SEMICOLON) -> null
                    match(TokenType.VAR) -> varDeclaration()
                    else -> expressionStatement()
                }

        // Condition
        var condition: Expr? = null
        if (!check(TokenType.SEMICOLON)) {
            condition = expression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")

        // Increment
        var increment: Expr? = null
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression()
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = statement()

        // Desugaring:
        // Add increment to the end of the body
        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }

        // Create the while loop (or use true if no condition)
        val actualCondition = condition ?: Expr.Literal(true)
        body = Stmt.While(actualCondition, body)

        // Add initializer to the beginning
        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }

        return body
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
    // assignment     → IDENTIFIER "=" assignment | logic_or ; (equality was term for Ch6)
    private fun assignment(): Expr {
        val expr = or_() // Or 'or()' when logical operators are added

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

    // logic_or       → logic_and ( "or" logic_and )* ;
    private fun or_(): Expr {
        var expr = and_()
        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and_()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    // logic_and      → equality ( "and" equality )* ;
    private fun and_(): Expr {
        var expr = equality()
        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
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
        return call()
    }

    // call           → primary ( "(" arguments? ")" )* ;
    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                    // Don't consume, let error recovery handle it or it might loop infinitely on
                    // arguments
                }
                arguments.add(expression()) // Arguments are expressions
            } while (match(TokenType.COMMA))
        }
        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")
        return Expr.Call(callee, paren, arguments)
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
