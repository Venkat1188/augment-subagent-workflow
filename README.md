https://github.com/augmentcode/code-review-best-practices/tree/main/example-guidelines

teammates register with:

  auggie mcp add augment-shared --replace \
    --transport http \
    --url http://35.239.35.215:3000/mcp \
    --header "Authorization: Bearer HOLvw8p7Ut4MSDXLSsSt1DD3-QkBdadKrr0gEeSVnj4"

  And verify with:

  curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
    -H "Authorization: Bearer HOLvw8p7Ut4MSDXLSsSt1DD3-QkBdadKrr0gEeSVnj4" \
    http://35.239.35.215:3000/mcp


    How to use it

  Provide two things:
    1. `index_name` – which indexed repo to search
    2. `query` – natural language description of what you want to find

  Optionally: maxChars to cap response size.

  Indexes currently available

  | Index name | Source |
  |---|---|
  | Venkat1188_account-name-agent | github://Venkat1188/account-name-agent |
  | Venkat1188_account-transfer-augment | github://Venkat1188/account-transfer-augment |
  | Venkat1188_augment-subagent-workflow | github://Venkat1188/augment-subagent-workflow |
  | Venkat1188_dapr-kafka-lib | github://Venkat1188/dapr-kafka-lib |
  | Venkat1188_kafka-producer-service | github://Venkat1188/kafka-producer-service |
  | kishore1218_prolisys-web | github://kishore1218/prolisys-web |

  Example queries

    • "authentication logic" → finds login/auth code
    • "kafka producer configuration" → finds broker/topic setup
    • "error handling for HTTP requests" → finds try/catch, retry logic
    • "how sub-agents are spawned" → finds orchestration code

  Tips for good queries

    • ✅ Describe behavior or concept: "where is the retry policy defined"
    • ✅ Use domain terms: "dapr pub/sub binding", "account transfer validation"
    • ❌ Don't paste exact symbol names if you already know them — use view with regex instead
    • ❌ Don't ask for whole-file context — use view on the returned path
