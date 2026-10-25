package com.example.util

import com.example.model.Contact

data class T9MatchResult(
  val contact: Contact,
  val nameHighlightRanges: List<IntRange> = emptyList(),
  val numberHighlightRange: IntRange? = null,
  val score: Int = 0
)

object T9Matcher {
  private val charToDigit = mapOf(
    'a' to '2', 'b' to '2', 'c' to '2',
    'd' to '3', 'e' to '3', 'f' to '3',
    'g' to '4', 'h' to '4', 'i' to '4',
    'j' to '5', 'k' to '5', 'l' to '5',
    'm' to '6', 'n' to '6', 'o' to '6',
    'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
    't' to '8', 'u' to '8', 'v' to '8',
    'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
  )

  fun charToT9(c: Char): Char? = charToDigit[c.lowercaseChar()]

  fun match(contact: Contact, queryDigits: String): T9MatchResult? {
    if (queryDigits.isEmpty()) {
      return T9MatchResult(contact = contact, score = 0)
    }

    // 1. Match phone number directly
    val cleanNumber = contact.phoneNumber.filter { it.isDigit() || it == '+' }
    val numberIdx = cleanNumber.indexOf(queryDigits)
    val numberHighlight = if (numberIdx >= 0) {
      numberIdx until (numberIdx + queryDigits.length)
    } else {
      null
    }

    // 2. Match T9 Name:
    // Check contiguous substring match on name
    val name = contact.name
    val nameT9Builder = StringBuilder()
    for (ch in name) {
      val digit = charToT9(ch)
      nameT9Builder.append(digit ?: ' ')
    }
    val nameT9 = nameT9Builder.toString()

    // Find contiguous match in nameT9
    val nameIdx = nameT9.indexOf(queryDigits)
    var nameHighlightRanges: List<IntRange> = emptyList()

    var matchScore = 0

    if (nameIdx >= 0) {
      nameHighlightRanges = listOf(nameIdx until (nameIdx + queryDigits.length))
      matchScore = 100 - nameIdx // Earlier in name is better
    } else {
      // Check initials (e.g. "Ctrl Boss" -> 'C'=2, 'B'=2 matches "22")
      val words = name.split(" ").filter { it.isNotEmpty() }
      val initialsT9 = words.mapNotNull { it.firstOrNull()?.let { c -> charToT9(c) } }.joinToString("")
      val initialIdx = initialsT9.indexOf(queryDigits)
      if (initialIdx >= 0) {
        val ranges = mutableListOf<IntRange>()
        var curWordIdx = 0
        var charPos = 0
        for (w in words) {
          if (curWordIdx in initialIdx until (initialIdx + queryDigits.length)) {
            val wordStart = name.indexOf(w, charPos)
            if (wordStart >= 0) {
              ranges.add(wordStart..wordStart)
            }
          }
          charPos += w.length + 1
          curWordIdx++
        }
        nameHighlightRanges = ranges
        matchScore = 80
      }
    }

    if (numberHighlight != null) {
      matchScore = maxOf(matchScore, 50)
    }

    return if (nameHighlightRanges.isNotEmpty() || numberHighlight != null) {
      T9MatchResult(
        contact = contact,
        nameHighlightRanges = nameHighlightRanges,
        numberHighlightRange = numberHighlight,
        score = matchScore
      )
    } else {
      null
    }
  }
}
