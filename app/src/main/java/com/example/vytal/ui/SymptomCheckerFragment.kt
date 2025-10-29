package com.example.vytal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.vytal.R

class SymptomCheckerFragment : Fragment() {

    private lateinit var checkBoxes: List<CheckBox>
    private lateinit var btnCheck: Button
    private lateinit var tvResult: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_symptom_checker, container, false)

        // List of symptoms
        checkBoxes = listOf(
            view.findViewById(R.id.cbFever),
            view.findViewById(R.id.cbCough),
            view.findViewById(R.id.cbHeadache),
            view.findViewById(R.id.cbFatigue),
            view.findViewById(R.id.cbSoreThroat),
            view.findViewById(R.id.cbNausea),
            view.findViewById(R.id.cbBodyPain)
        )

        btnCheck = view.findViewById(R.id.btnCheck)
        tvResult = view.findViewById(R.id.tvResult)

        btnCheck.setOnClickListener {
            val selected = checkBoxes.filter { it.isChecked }.map { it.text.toString() }

            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one symptom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = generateResult(selected)
            tvResult.text = result
        }

        return view
    }

    private fun generateResult(symptoms: List<String>): String {
        val lower = symptoms.joinToString(", ").lowercase()

        return when {
            listOf("fever", "cough", "sore throat").all { lower.contains(it) } ->
                "Possible: Common cold or flu. Stay hydrated and rest. If symptoms persist, consult a doctor."
            listOf("fever", "body pain", "fatigue").all { lower.contains(it) } ->
                "Possible: Viral infection such as dengue or flu. Get proper rest and monitor temperature."
            listOf("headache", "nausea").all { lower.contains(it) } ->
                "Possible: Migraine or dehydration. Try drinking water and resting in a dark room."
            lower.contains("fatigue") && lower.contains("body pain") ->
                "Possible: General tiredness or viral fever. Rest well and maintain fluids."
            else -> "Symptoms are general. Consider consulting a healthcare professional for proper diagnosis."
        }
    }
}
