export function jsonBody(response) {
    if (response.body == null) {
        return null
    }
    if (typeof response.body === "string") {
        try {
            return JSON.parse(response.body)
        } catch (e) {
            return null
        }
    }
    return response.body
}

export function assertStatus(client, response, expected) {
    client.assert(
        response.status === expected,
        "Expected HTTP " + expected + " but got " + response.status,
    )
}

export function assertError(client, response, status, message) {
    assertStatus(client, response, status)
    const body = jsonBody(response)
    client.assert(body != null && typeof body === "object", "Expected a JSON error body")
    client.assert(body.message === message, 'Expected message "' + message + '" but got "' + body.message + '"')
    client.assert(body.user === undefined, "Error body must not include user")
    client.assert(body.password === undefined, "Error body must not include password")
}

export function assertPublicUser(client, user, expected) {
    client.assert(user != null && typeof user === "object", "Expected user object")
    client.assert(user.id !== undefined && user.id !== null, "Expected user.id")
    client.assert(user.name === expected.name, 'Expected name "' + expected.name + '" but got "' + user.name + '"')
    client.assert(typeof user.email === "string" && user.email.length > 0, "Expected user.email")
    client.assert(user.email === user.email.toLowerCase().trim(), "Email must be trimmed and lowercase")
    if (expected.email !== undefined) {
        client.assert(user.email === expected.email, 'Expected email "' + expected.email + '" but got "' + user.email + '"')
    }
    if (expected.emailPattern !== undefined) {
        client.assert(expected.emailPattern.test(user.email), 'Email "' + user.email + '" did not match ' + expected.emailPattern)
    }
    client.assert(user.password === undefined, "password must not appear in the response")
    client.assert(user.passwordHash === undefined, "passwordHash must not appear in the response")
    const keys = Object.keys(user).sort().join(",")
    client.assert(keys === "email,id,name", "user must only contain id, name, email (got: " + keys + ")")
}

export function assertCreatedUser(client, response, expected) {
    assertStatus(client, response, 201)
    const body = jsonBody(response)
    client.assert(body != null && typeof body === "object", "Expected a JSON body")
    assertPublicUser(client, body.user, expected)
}

export function assertSessionUser(client, response, expected) {
    assertStatus(client, response, 200)
    const body = jsonBody(response)
    client.assert(body != null && typeof body === "object", "Expected a JSON body")
    assertPublicUser(client, body, expected)
    client.assert(body.user === undefined, "session/login body must not wrap the user")
    client.assert(body.token === undefined, "token must not appear in the JSON body")
}

export function assertSetCookieHttpOnly(client, response, cookieName) {
    const raw = response.headers.valueOf("Set-Cookie")
    client.assert(raw != null, "Expected Set-Cookie header")
    const header = Array.isArray(raw) ? raw.join("\n") : String(raw)
    client.assert(
        header.toLowerCase().includes(cookieName.toLowerCase() + "="),
        "Expected cookie " + cookieName,
    )
    client.assert(/httponly/i.test(header), "Cookie must be HttpOnly")
    client.assert(/samesite/i.test(header), "Cookie must set SameSite")
    client.assert(!/password/i.test(header), "Cookie header must not contain password")
}
