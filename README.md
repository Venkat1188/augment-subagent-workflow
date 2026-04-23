teammates register with:

  auggie mcp add augment-shared --replace \
    --transport http \
    --url http://35.239.35.215:3000/mcp \
    --header "Authorization: Bearer HOLvw8p7Ut4MSDXLSsSt1DD3-QkBdadKrr0gEeSVnj4"

  And verify with:

  curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
    -H "Authorization: Bearer HOLvw8p7Ut4MSDXLSsSt1DD3-QkBdadKrr0gEeSVnj4" \
    http://35.239.35.215:3000/mcp
