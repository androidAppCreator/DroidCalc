package com.droid.droidcalc.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Stack
import javax.inject.Inject

/**
 * Use case for performing calculations on a mathematical expression string.
 * This class implements the Shunting-yard algorithm to parse infix expressions
 * into Reverse Polish Notation (RPN), and then evaluates the RPN expression.
 * It supports basic arithmetic operations (+, -, *, /) and parentheses.
 * The result is returned as a [BigDecimal] with a scale of 2 and [RoundingMode.HALF_EVEN].
 *
 * @author DroidSwap
 */
class CalculateUseCase @Inject constructor() {

    /**
     * Executes the calculation for the given mathematical expression.
     *
     * @param expression The mathematical expression in infix notation (e.g., "3 + 4 * 2 / ( 1 - 5 )").
     * @return A [Result] containing the [BigDecimal] result if the calculation is successful,
     *         or an exception if an error occurs (e.g., invalid expression, division by zero).
     */
    operator fun invoke(expression: String): Result<BigDecimal> {
        return try {
            if (expression.isBlank()) {
                return Result.failure(IllegalArgumentException("Expression cannot be blank."))
            }
            val rpnExpression = infixToRpn(expression)
            if (rpnExpression.isEmpty() && expression.any { it.isDigit() }) {
                // This can happen if only a number is provided, shunting yard might return it directly
                // or if there was an issue in tokenization/parsing that wasn't caught as an exception
                // For a single number, RPN is just the number itself.
                // We try to parse it directly if RPN is empty but input was not.
                 try {
                    val singleNumber = BigDecimal(expression.trim()).setScale(2, RoundingMode.HALF_EVEN)
                    return Result.success(singleNumber)
                } catch (e: NumberFormatException) {
                    return Result.failure(IllegalArgumentException("Invalid expression format: ${e.message}"))
                }
            }
            val result = evaluateRpn(rpnExpression)
            Result.success(result.setScale(2, RoundingMode.HALF_EVEN))
        } catch (e: Exception) {
            Timber.e(e, "Calculation failed for expression: %s", expression)
            Result.failure(e)
        }
    }

    /**
     * Converts an infix mathematical expression to Reverse Polish Notation (RPN)
     * using the Shunting-yard algorithm.
     *
     * @param infixExpression The infix expression string.
     * @return A list of strings representing the RPN expression.
     * @throws IllegalArgumentException If the expression is invalid (e.g., mismatched parentheses).
     */
    private fun infixToRpn(infixExpression: String): List<String> {
        val outputQueue = mutableListOf<String>()
        val operatorStack = Stack<String>()
        // Regex to tokenize numbers (including decimals) and operators/parentheses
        val tokens = tokenize(infixExpression)

        tokens.forEach { token ->
            when {
                isNumber(token) -> outputQueue.add(token)
                token == "(" -> operatorStack.push(token)
                token == ")" -> {
                    while (operatorStack.isNotEmpty() && operatorStack.peek() != "(") {
                        outputQueue.add(operatorStack.pop())
                    }
                    if (operatorStack.isNotEmpty() && operatorStack.peek() == "(") {
                        operatorStack.pop() // Pop "("
                    } else {
                        throw IllegalArgumentException("Mismatched parentheses in expression.")
                    }
                }
                isOperator(token) -> {
                    while (
                        operatorStack.isNotEmpty() &&
                        operatorStack.peek() != "(" &&
                        hasHigherPrecedence(operatorStack.peek(), token)
                    ) {
                        outputQueue.add(operatorStack.pop())
                    }
                    operatorStack.push(token)
                }
                else -> throw IllegalArgumentException("Invalid token: $token")
            }
        }

        while (operatorStack.isNotEmpty()) {
            val operator = operatorStack.pop()
            if (operator == "(") {
                throw IllegalArgumentException("Mismatched parentheses in expression.")
            }
            outputQueue.add(operator)
        }
        return outputQueue
    }

    /**
     * Evaluates an expression in Reverse Polish Notation (RPN).
     *
     * @param rpnExpression A list of strings representing the RPN expression.
     * @return The [BigDecimal] result of the evaluation.
     * @throws IllegalArgumentException If the RPN expression is invalid or division by zero occurs.
     */
    private fun evaluateRpn(rpnExpression: List<String>): BigDecimal {
        val operandStack = Stack<BigDecimal>()
        rpnExpression.forEach { token ->
            if (isNumber(token)) {
                operandStack.push(BigDecimal(token))
            } else if (isOperator(token)) {
                if (operandStack.size < 2) throw IllegalArgumentException("Invalid RPN expression: insufficient operands for operator $token")
                val operand2 = operandStack.pop()
                val operand1 = operandStack.pop()
                when (token) {
                    "+" -> operandStack.push(operand1.add(operand2))
                    "-" -> operandStack.push(operand1.subtract(operand2))
                    "*" -> operandStack.push(operand1.multiply(operand2))
                    "/" -> {
                        if (operand2.compareTo(BigDecimal.ZERO) == 0) {
                            throw ArithmeticException("Division by zero.")
                        }
                        // Perform division with sufficient scale for intermediate steps, then round at the end.
                        operandStack.push(operand1.divide(operand2, 10, RoundingMode.HALF_EVEN))
                    }
                }
            } else {
                 throw IllegalArgumentException("Invalid token in RPN expression: $token")
            }
        }
        if (operandStack.size != 1) throw IllegalArgumentException("Invalid RPN expression: stack should contain one result.")
        return operandStack.pop()
    }

    /**
     * Tokenizes the infix expression string into numbers, operators, and parentheses.
     * This version handles multi-digit numbers, decimal points, and unary minus at the beginning
     * or after an opening parenthesis.
     *
     * @param expression The infix expression string.
     * @return A list of tokens.
     */
    private fun tokenize(expression: String): List<String> {
        val tokens = mutableListOf<String>()
        val regex = """(\d+\.?\d*|\.\d+)|([+\-*/()])""".toRegex()
        var lastMatchWasOperatorOrParen = true // Start as true to allow leading unary minus

        var currentIndex = 0
        while (currentIndex < expression.length) {
            val char = expression[currentIndex]
            if (char.isWhitespace()) {
                currentIndex++
                continue
            }

            // Handle unary minus more robustly
            if (char == '-' && lastMatchWasOperatorOrParen) {
                 // It's a unary minus. Look for the number that follows.
                val numberMatch = """^(\d+\.?\d*|\.\d+)""".toRegex().find(expression.substring(currentIndex + 1))
                if (numberMatch != null) {
                    val numberToken = "-${numberMatch.value}"
                    tokens.add(numberToken)
                    currentIndex += numberToken.length // length of "-" + number
                    lastMatchWasOperatorOrParen = false
                    continue
                } else {
                    // This might be a binary minus if something weird is happening, or an error.
                    // For now, treat as binary, but Shunting-yard might fail.
                    // Or throw specific error: throw IllegalArgumentException("Invalid unary minus usage")
                }
            }

            // Try to match operators or parentheses first
            if (char in setOf('+', '-', '*', '/', '(', ')')) {
                tokens.add(char.toString())
                currentIndex++
                lastMatchWasOperatorOrParen = (char == '(') // Only ( allows next to be unary.
                                            // For other ops, next char must be operand or (
                if (char != ')') lastMatchWasOperatorOrParen = true

            } else {
                // Try to match a number (integer or decimal)
                val numberMatch = """^(\d+\.?\d*|\.\d+)""".toRegex().find(expression.substring(currentIndex))
                if (numberMatch != null) {
                    tokens.add(numberMatch.value)
                    currentIndex += numberMatch.value.length
                    lastMatchWasOperatorOrParen = false
                } else {
                    throw IllegalArgumentException("Invalid character in expression: $char at index $currentIndex")
                }
            }
        }
        return tokens
    }


    /** Checks if a token is a number. */
    private fun isNumber(token: String): Boolean {
        return try {
            BigDecimal(token)
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    /** Checks if a token is an operator. */
    private fun isOperator(token: String): Boolean = token in setOf("+", "-", "*", "/")

    /** Returns the precedence of an operator. */
    private fun precedence(operator: String): Int {
        return when (operator) {
            "+", "-" -> 1
            "*", "/" -> 2
            else -> 0 // For parentheses or other symbols
        }
    }

    /**
     * Checks if op1 has higher or equal precedence than op2.
     * Considers left-associativity for operators of same precedence.
     */
    private fun hasHigherPrecedence(op1: String, op2: String): Boolean {
        val prec1 = precedence(op1)
        val prec2 = precedence(op2)
        // All supported operators (+, -, *, /) are left-associative.
        // So, if precedences are equal, process the one on the stack (op1).
        return prec1 >= prec2
    }
}

// Basic Timber placeholder for environments where the full Timber library isn't set up for this standalone use case test.
// In a full Android app, this would use the real Timber.
private object Timber {
    fun d(message: String, vararg args: Any?) { println(String.format("DEBUG: $message", *args)) }
    fun e(t: Throwable, message: String, vararg args: Any?) { println(String.format("ERROR: $message", *args)); t.printStackTrace() }
}
