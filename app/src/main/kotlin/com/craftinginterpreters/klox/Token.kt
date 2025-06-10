package com.craftinginterpreters.klox

data class Token(
        val type: TokenType,
        val lexeme: String,
        val literal: Any?, // Can be String, Double, Boolean, null, etc.
        val line: Int
) {
    override fun toString(): String {
        return "$type $lexeme $literal"
    }
}
