package com.example.vytal.api

data class Age(val value: Int)
data class EvidenceItem(val id: String, val choice_id: String, val source: String? = null)
data class DiagnosisRequest(
    val sex: String,
    val age: Age,
    val evidence: List<EvidenceItem>,
    val extras: Map<String, Any>? = null
)

data class Condition(val id: String, val name: String, val probability: Double)
data class DiagnosisResponse(
    val conditions: List<Condition>,
    val question: Question?,
    val should_stop: Boolean?
)

data class ChoiceItem(val id: String, val label: String)
data class QuestionItem(val id: String, val name: String, val choices: List<ChoiceItem>)
data class Question(val type: String, val text: String, val items: List<QuestionItem>)
