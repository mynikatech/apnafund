package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.ChatMessageRequest

fun buildPrompt(): String = """
Return ONLY valid JSON. No explanation.

Schema:
{
  "intent": "FETCH_DATA | SUMMARY | HELP",
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
how to add fund → {"intent":"HELP","entity":"FUND","action":"CREATE"}
Show my loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"scope":"SELF"}}
Pending loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"status":"pending"}}
Kapil loans → {"intent":"FETCH_DATA","entity":"LOAN","filters":{"borrower":"Kapil"}}
Summary → {"intent":"SUMMARY"}
""".trimIndent()

fun shortPrompt(): String = """
Return ONLY valid JSON.

Schema:
{
  "intent": "FETCH_DATA | SUMMARY | HELP",
  "entity": "LOAN | FUND | GROUP | DEPOSIT | LOAN_EMI",
  "action": "LIST | DETAILS | SUMMARY | MEMBERS | CREATE | NAVIGATE",
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
- add/create → CREATE
- where/how → NAVIGATE
- how/help → intent = HELP
- otherwise → LIST or DETAILS

Examples:

my loans → {"intent":"FETCH_DATA","entity":"LOAN","action":"LIST","filters":{"scope":"SELF"}}

pending loans → {"intent":"FETCH_DATA","entity":"LOAN","action":"LIST","filters":{"status":"pending"}}

loan summary → {"intent":"SUMMARY","entity":"LOAN","action":"SUMMARY"}

my deposits → {"intent":"FETCH_DATA","entity":"DEPOSIT","action":"LIST","filters":{"scope":"SELF"}}

deposit summary → {"intent":"SUMMARY","entity":"DEPOSIT","action":"SUMMARY"}

add deposit → {"intent":"FETCH_DATA","entity":"DEPOSIT","action":"CREATE"}

my emis → {"intent":"FETCH_DATA","entity":"LOAN_EMI","action":"LIST","filters":{"scope":"SELF"}}

pending emis → {"intent":"FETCH_DATA","entity":"LOAN_EMI","action":"LIST","filters":{"status":"pending"}}

emi summary → {"intent":"SUMMARY","entity":"LOAN_EMI","action":"SUMMARY"}

add emi → {"intent":"FETCH_DATA","entity":"LOAN_EMI","action":"CREATE"}

how to add fund → {"intent":"HELP","entity":"FUND","action":"CREATE"}

add fund → {"intent":"FETCH_DATA","entity":"FUND","action":"CREATE"}

summary → {"intent":"SUMMARY","action":"SUMMARY"}
""".trimIndent()


fun standardPrompt(): String = """
Return ONLY valid JSON.

Schema:
{
  "intent": "FETCH_DATA | SUMMARY | HELP",
  "entity": "LOAN | FUND | GROUP | DEPOSIT | LOAN_EMI",
  "action": "LIST | DETAILS | SUMMARY | MEMBERS | CREATE | NAVIGATE",
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
- add/create → CREATE
- where/how → NAVIGATE
- how/help → intent = HELP
- otherwise → LIST or DETAILS

Examples:

my loans → {"intent":"FETCH_DATA","entity":"LOAN","action":"LIST","filters":{"scope":"SELF"}}

pending loans → {"intent":"FETCH_DATA","entity":"LOAN","action":"LIST","filters":{"status":"pending"}}

loan summary → {"intent":"SUMMARY","entity":"LOAN","action":"SUMMARY"}

my deposits → {"intent":"FETCH_DATA","entity":"DEPOSIT","action":"LIST","filters":{"scope":"SELF"}}

deposit summary → {"intent":"SUMMARY","entity":"DEPOSIT","action":"SUMMARY"}

add deposit → {"intent":"FETCH_DATA","entity":"DEPOSIT","action":"CREATE"}

my emis → {"intent":"FETCH_DATA","entity":"LOAN_EMI","action":"LIST","filters":{"scope":"SELF"}}

pending emis → {"intent":"FETCH_DATA","entity":"LOAN_EMI","action":"LIST","filters":{"status":"pending"}}

emi summary → {"intent":"SUMMARY","entity":"LOAN_EMI","action":"SUMMARY"}

add emi → {"intent":"FETCH_DATA","entity":"LOAN_EMI","action":"CREATE"}

how to add fund → {"intent":"HELP","entity":"FUND","action":"CREATE"}

add fund → {"intent":"FETCH_DATA","entity":"FUND","action":"CREATE"}

summary → {"intent":"SUMMARY","action":"SUMMARY"}
""".trimIndent()

fun fullPrompt(): String = """
Return ONLY valid JSON.

Schema:
{
  "intent": "FETCH_DATA | SUMMARY | HELP",
  "entity": "LOAN | FUND | GROUP | DEPOSIT | LOAN_EMI",
  "action": "LIST | DETAILS | SUMMARY | MEMBERS | CREATE | NAVIGATE",
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
- add/create → CREATE
- where/how → NAVIGATE
- how/help → intent = HELP
- otherwise → LIST or DETAILS

Examples:

my loans → {"intent":"FETCH_DATA","entity":"LOAN","action":"LIST","filters":{"scope":"SELF"}}

pending loans → {"intent":"FETCH_DATA","entity":"LOAN","action":"LIST","filters":{"status":"pending"}}

loan summary → {"intent":"SUMMARY","entity":"LOAN","action":"SUMMARY"}

my deposits → {"intent":"FETCH_DATA","entity":"DEPOSIT","action":"LIST","filters":{"scope":"SELF"}}

deposit summary → {"intent":"SUMMARY","entity":"DEPOSIT","action":"SUMMARY"}

add deposit → {"intent":"FETCH_DATA","entity":"DEPOSIT","action":"CREATE"}

my emis → {"intent":"FETCH_DATA","entity":"LOAN_EMI","action":"LIST","filters":{"scope":"SELF"}}

pending emis → {"intent":"FETCH_DATA","entity":"LOAN_EMI","action":"LIST","filters":{"status":"pending"}}

emi summary → {"intent":"SUMMARY","entity":"LOAN_EMI","action":"SUMMARY"}

add emi → {"intent":"FETCH_DATA","entity":"LOAN_EMI","action":"CREATE"}

how to add fund → {"intent":"HELP","entity":"FUND","action":"CREATE"}

add fund → {"intent":"FETCH_DATA","entity":"FUND","action":"CREATE"}

summary → {"intent":"SUMMARY","action":"SUMMARY"}
""".trimIndent()


fun systemMessage(content: String) =
    ChatMessageRequest(role = "system", content = content)

fun userMessage(content: String) =
    ChatMessageRequest(role = "user", content = content)