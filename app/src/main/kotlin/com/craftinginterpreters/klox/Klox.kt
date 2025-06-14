package com.craftinginterpreters.klox

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

object Klox {
    private var hadError = false // Add this flag
    private var hadRuntimeError = false // New flag

    private val interpreter = Interpreter() // Create an interpreter instance

    @JvmStatic
    fun main(args: Array<String>) {
        println("Klox (Kotlin) Interpreter") // A friendlier greeting
        if (args.size > 1) {
            println("Usage: klox [script]")
            exitProcess(64)
        } else if (args.size == 1) {
            runFile(args[0])
            if (hadError) exitProcess(65) // Check error after running file
            if (hadRuntimeError) exitProcess(70) // Exit code for runtime errors
        } else {
            runPrompt()
        }
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
        val parser = Parser(tokens)
        val expression = parser.parse()

        // Stop if there was a syntax error.
        if (hadError) return

        if (expression != null) { // Check if parsing succeeded
            interpreter.interpret(expression) // Use the interpreter
        }
    }

    // Error reporting functions
    fun error(line: Int, message: String) {
        report(line, "", message)
    }
    fun error(token: Token, message: String) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message)
        } else {
            report(token.line, " at '${token.lexeme}'", message)
        }
    }

    // New method for runtime errors
    fun runtimeError(error: RuntimeError) {
        System.err.println("${error.message}\n[line ${error.token.line}]")
        hadRuntimeError = true
    }

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
        hadError = true
    }
}
