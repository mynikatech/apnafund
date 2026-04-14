package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.ChatMessageRequest

fun buildPrompt(): String = """
Return ONLY valid JSON. No explanation.

Schema:
{
  "intent": "FETCH_DATA | SUMMARY",
  "entity": "LOAN | FUND | GROUP | DEPOSIT",
  "filters": {
    "status": "string",
    "borrower": "string",
    "scope": "SELF | ALL"
  }
}

Rules:
- filters values must be strings
- omit unused fields
- no arrays, no nested objects

Examples:
Show my loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"scope":"SELF"}}
Pending loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"status":"pending"}}
Kapil loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"borrower":"Kapil"}}
Summary → {"intent":"SUMMARY"}
""".trimIndent()

fun shortPrompt(): String = """
Return ONLY JSON. No explanation. No markdown.

intent: FETCH_DATA | SUMMARY
entity: LOAN | FUND | GROUP | DEPOSIT
filters: string values only

Examples:
my loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"scope":"SELF"}}
summary → {"intent":"SUMMARY"}
""".trimIndent()

fun standardPrompt(): String = """
Return ONLY valid JSON. No explanation. No markdown.

Schema:
{
  "intent": "FETCH_DATA | SUMMARY",
  "entity": "LOAN | FUND | GROUP | DEPOSIT",
  "filters": {
    "status": "string",
    "borrower": "string",
    "scope": "SELF | ALL"
  }
}

Rules:
- filters values must be strings
- omit unused fields
- no arrays, no nested objects
- do not wrap output in markdown

Examples:
my loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"scope":"SELF"}}
pending loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"status":"pending"}}
kapil loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"borrower":"Kapil"}}
summary → {"intent":"SUMMARY"}
""".trimIndent()

fun fullPrompt(): String = """
You are an assistant for a finance app.

Return ONLY valid JSON. No explanation. No markdown.

Schema:
{
  "intent": "FETCH_DATA | SUMMARY | UNKNOWN",
  "entity": "LOAN | FUND | GROUP | DEPOSIT",
  "filters": {
    "status": "string",
    "borrower": "string",
    "scope": "SELF | ALL"
  }
}

Rules:
- filters values must be strings
- omit unused fields
- no arrays, no nested objects
- do not wrap output in markdown
- if unsure, return {"intent":"UNKNOWN"}

Examples:
show my loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"scope":"SELF"}}
show closed loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"status":"closed"}}
show kapil loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"borrower":"Kapil"}}
show summary → {"intent":"SUMMARY"}
""".trimIndent()



fun systemMessage(content: String) =
    ChatMessageRequest(role = "system", content = content)

fun userMessage(content: String) =
    ChatMessageRequest(role = "user", content = content)