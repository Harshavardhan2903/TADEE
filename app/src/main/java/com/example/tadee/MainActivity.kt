package com.example.tadee

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.round
import java.lang.Math.toDegrees

class MainActivity : AppCompatActivity() {

    data class Complex(val re: Double, val im: Double) {
        operator fun plus(other: Complex) = Complex(re + other.re, im + other.im)
        operator fun minus(other: Complex) = Complex(re - other.re, im - other.im)
        operator fun times(other: Complex): Complex {
            return Complex(
                re * other.re - im * other.im,
                re * other.im + im * other.re
            )
        }
        operator fun div(other: Complex): Complex {
            val denom = other.re * other.re + other.im * other.im
            return Complex(
                (re * other.re + im * other.im) / denom,
                (im * other.re - re * other.im) / denom
            )
        }

        fun polarString(): String {
            val mag = round(hypot(re, im) * 100) / 100
            val angle = round(toDegrees(atan2(im, re)) * 100) / 100
            return "$mag < $angle°"
        }

        override fun toString(): String {
            return "$re + ${im}j"
        }

        companion object {
            fun fromString(s: String): Complex {
                val clean = s.trim().replace(" ", "").replace("j", "")

                // Handle invalid cases such as just '+' or '-'
                if (clean == "+" || clean == "-" || clean.isBlank()) {
                    throw IllegalArgumentException("Invalid input format: $s")
                }

                // Handle case where only real or imaginary part is provided
                if (clean.contains("+") || clean.contains("-")) {
                    val plusIndex = clean.indexOf('+', 1)
                    val minusIndex = clean.indexOf('-', 1)
                    val splitIndex = if (plusIndex != -1) plusIndex else minusIndex

                    if (splitIndex != -1) {
                        val real = clean.substring(0, splitIndex).toDouble()
                        val imag = clean.substring(splitIndex).toDouble()
                        return Complex(real, imag)
                    }
                }

                // If no operator (+ or -) found, treat it as real number
                return Complex(clean.toDouble(), 0.0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val inputVoltage = findViewById<EditText>(R.id.inputVoltage)
        val inputImpedance = findViewById<EditText>(R.id.inputImpedance)
        val inputNumLoads = findViewById<EditText>(R.id.inputNumLoads)
        val inputDistances = findViewById<EditText>(R.id.inputDistances)
        val inputLoads = findViewById<EditText>(R.id.inputLoads)
        val outputResult = findViewById<TextView>(R.id.outputText)
        val button = findViewById<Button>(R.id.btnCalculate)

        button.setOnClickListener {
            try {
                val V = Complex.fromString(inputVoltage.text.toString())
                val z = Complex.fromString(inputImpedance.text.toString())
                val n = inputNumLoads.text.toString().toInt()
                val l = inputDistances.text.toString().trim().split("\\s+".toRegex()).map { it.toInt() }
                val y = inputLoads.text.toString().trim().split("\\s+".toRegex()).map { Complex.fromString(it) }

                if (l.size != n || y.size != n) {
                    outputResult.text = "Error: Number of distances and loads must match the number of loads specified."
                    return@setOnClickListener
                }

                val z1 = mutableListOf<Complex>()
                z1.add(z * Complex(l[0].toDouble(), 0.0))
                for (i in 1 until n) {
                    z1.add(z * Complex((l[i] - l[i - 1]).toDouble(), 0.0))
                }

                fun ll(a: Complex, b: Complex): Complex {
                    return Complex(1.0, 0.0) / (Complex(1.0, 0.0) / a + Complex(1.0, 0.0) / b)
                }

                fun se(a: Complex, b: Complex): Complex {
                    return a + b
                }

                var zeff = se(y[n - 1], z1[n - 1])
                for (i in n - 2 downTo 0) {
                    zeff = se(ll(zeff, y[i]), z1[i])
                }

                var I = V / zeff
                val V1 = MutableList(n) { Complex(0.0, 0.0) }
                val I1 = MutableList(n) { Complex(0.0, 0.0) }

                V1[0] = V - I * z1[0]
                I1[0] = V1[0] / y[0]
                I -= I1[0]

                for (i in 1 until n) {
                    V1[i] = V1[i - 1] - I * z1[i]
                    I1[i] = V1[i] / y[i]
                    I -= I1[i]
                }

                val result = StringBuilder("Voltage at each load location (in polar form):\n")
                for (i in 0 until n) {
                    result.append("Location ${i + 1} (${l[i]} m): Voltage = ${V1[i].polarString()}\n")
                }

                outputResult.text = result.toString()

            } catch (e: Exception) {
                outputResult.text = "Error: ${e.message}"
            }
        }
    }
}
