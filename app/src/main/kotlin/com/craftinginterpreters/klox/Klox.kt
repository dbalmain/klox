// src/main/kotlin/com/craftinginterpreters/loxkt/Lox.kt
package com.craftinginterpreters.klox

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

// We'll make Lox an object (singleton) for now, similar to the static methods in Java
object Klox {
    @JvmStatic // Important for Gradle's application plugin to find the main method
    fun main(args: Array<String>) {
        println("Hello from Lox (Kotlin)!")
        if (args.size > 1) {
            println("Usage: loxkt [script]")
            exitProcess(64)
        } else if (args.size == 1) {
            runFile(args[0])
        } else {
            runPrompt()
        }
    }

    private fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        run(String(bytes, Charset.defaultCharset()))
        // Indicate an error in the exit code.
        // if (hadError) exitProcess(65) // We'll add error handling later
    }

    private fun runPrompt() {
        val reader = System.`in`.bufferedReader()
        while (true) {
            print("> ")
            val line = reader.readLine() ?: break // Exit on Ctrl+D (EOF)
            run(line)
            // hadError = false // Reset error for interactive mode
        }
    }

    private fun run(source: String) {
        println("Executing: $source")
        // Placeholder for scanner, parser, interpreter
        // val scanner = Scanner(source)
        // val tokens = scanner.scanTokens()
        // for (token in tokens) {
        //     println(token)
        // }
    }

    // fun error(line: Int, message: String) {
    //     report(line, "", message)
    // }

    // private fun report(line: Int, where: String, message: String) {
    //     System.err.println("[line $line] Error$where: $message")
    //     // hadError = true
    // }
}
