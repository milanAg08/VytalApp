package com.example.vytal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.vytal.R
import kotlin.math.pow

class BmiFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bmi, container, false)

        val etAge = view.findViewById<EditText>(R.id.etAge)
        val genderGroup = view.findViewById<RadioGroup>(R.id.genderGroup)
        val etHeightFeet = view.findViewById<EditText>(R.id.etHeightFeet)
        val etHeightInch = view.findViewById<EditText>(R.id.etHeightInch)
        val etWeight = view.findViewById<EditText>(R.id.etWeight)
        val btnCalculate = view.findViewById<Button>(R.id.btnCalculate)
        val tvResult = view.findViewById<TextView>(R.id.tvResult)

        btnCalculate.setOnClickListener {
            try {
                val weight = etWeight.text.toString().toFloat()
                val feet = etHeightFeet.text.toString().toFloat()
                val inches = etHeightInch.text.toString().toFloat()

                val totalInches = feet * 12 + inches
                val heightMeters = totalInches * 0.0254
                val bmi = weight / heightMeters.pow(2)

                val resultText = when {
                    bmi < 18.5 -> "Underweight"
                    bmi < 25 -> "Normal"
                    bmi < 30 -> "Overweight"
                    else -> "Obese"
                }

                tvResult.text = "Your BMI: %.1f (%s)".format(bmi, resultText)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
