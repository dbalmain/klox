package com.craftinginterpreters.klox

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

object Klox {
    private var hadError = false // Add this flag

    @JvmStatic
    fun main(args: Array<String>) {
        testAstPrinter()
        // println("Klox (Kotlin) Interpreter") // A friendlier greeting
        // if (args.size > 1) {
        //     println("Usage: kloxkt [script]")
        //     exitProcess(64)
        // } else if (args.size == 1) {
        //     runFile(args[0])
        //     if (hadError) exitProcess(65) // Check error after running file
        // } else {
        //     runPrompt()
        // }
    }

    private fun testAstPrinter() {
        // Example from the book: -123 * (45.67)
        // In Lisp-like: (* (- 123) (group 45.67))
        val expression: Expr =
                Expr.Binary(
                        Expr.Unary(
                                Token(TokenType.MINUS, "-", null, 1),
                                Expr.Literal(123.0) // Use Double for numbers
                        ),
                        Token(TokenType.STAR, "*", null, 1),
                        Expr.Grouping(Expr.Literal(45.67))
                )
        println("AST Printer Test:")
        println(AstPrinter().print(expression))
        println("----")

        // A more extensive test
        // ((1 + 2) * (3 / -4)) == nil
        val moreComplexExpression: Expr =
                Expr.Binary(
                        Expr.Binary(
                                Expr.Grouping(
                                        Expr.Binary(
                                                Expr.Literal(1.0),
                                                Token(TokenType.PLUS, "+", null, 2),
                                                Expr.Literal(2.0)
                                        )
                                ),
                                Token(TokenType.STAR, "*", null, 2),
                                Expr.Grouping(
                                        Expr.Binary(
                                                Expr.Literal(3.0),
                                                Token(TokenType.SLASH, "/", null, 2),
                                                Expr.Unary(
                                                        Token(TokenType.MINUS, "-", null, 2),
                                                        Expr.Literal(4.0)
                                                )
                                        )
                                )
                        ),
                        Token(TokenType.EQUAL_EQUAL, "==", null, 2),
                        Expr.Literal(null) // Represents 'nil'
                )
        println("More Complex AST Printer Test:")
        println(AstPrinter().print(moreComplexExpression))
        println("----")

        // Test with an Assign expression (if you've generated it)
        val assignExpression: Expr =
                Expr.Assign(
                        Token(TokenType.IDENTIFIER, "a", null, 3),
                        Expr.Literal("some string value")
                )
        println("Assign Expression Test:")
        println(AstPrinter().print(assignExpression))
        println("----")

        // Test Call expression (if generated)
        val callExpression: Expr =
                Expr.Call(
                        Expr.Variable(Token(TokenType.IDENTIFIER, "myFunction", null, 4)),
                        Token(TokenType.LEFT_PAREN, "(", null, 4),
                        listOf(
                                Expr.Literal(10.0),
                                Expr.Binary(
                                        Expr.Literal(20.0),
                                        Token(TokenType.PLUS, "+", null, 4),
                                        Expr.Literal(30.0)
                                )
                        )
                )
        println("Call Expression Test:")
        println(AstPrinter().print(callExpression))
    }

    private fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        run(String(bytes, Charset.defaultCharset()))
    }

    private fun runPrompt() {
        val reader = System.`in`.bufferedReader()
        while (true) {
            print("> ")
            val line = reader.readLine()
            if (line == null || line.equals("exit", ignoreCase = true)
            ) { // Check for null (Ctrl+D) or "exit" command
                println("Exiting Klox REPL.")
                break
            }

            if (line.isNotBlank()) { // Process only if the line is not blank
                run(line)
                hadError = false // Reset error in interactive mode
            }
        }
    }

    private fun run(source: String) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()

        // For now, just print the tokens.
        for (token in tokens) {
            println(token)
        }
    }

    // Error reporting functions
    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
        hadError = true
    }
}
