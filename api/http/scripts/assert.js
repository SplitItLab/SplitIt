function toPlainJson(value) {
    if (value == null || typeof value !== "object") {
        return value
    }
    let plain
    try {
        plain = JSON.parse(JSON.stringify(value))
    } catch (e) {
        plain = {}
        const keys = Object.keys(value)
        for (let i = 0; i < keys.length; i++) {
            const key = keys[i]
            const nested = value[key]
            if (key === "onEachLine" || key === "onEachMessage" || typeof nested === "function") {
                continue
            }
            plain[key] = nested
        }
    }
    if (plain != null && typeof plain === "object") {
        delete plain.onEachLine
        delete plain.onEachMessage
    }
    return plain
}

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
    return toPlainJson(response.body)
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

export function setCookieHeader(response) {
    const headers = response.headers
    if (headers == null) {
        return ""
    }
    const collected = []
    if (typeof headers.valuesOf === "function") {
        const values = headers.valuesOf("Set-Cookie")
        if (values != null) {
            if (Array.isArray(values)) collected.push.apply(collected, values)
            else collected.push(values)
        }
    }
    if (collected.length === 0 && typeof headers.valueOf === "function") {
        const value = headers.valueOf("Set-Cookie")
        if (value != null) collected.push(value)
    }
    if (collected.length === 0) {
        const fallback = headers["Set-Cookie"] || headers["set-cookie"]
        if (fallback != null) {
            if (Array.isArray(fallback)) collected.push.apply(collected, fallback)
            else collected.push(fallback)
        }
    }
    return collected.map(String).join("\n")
}

export function authTokenFromSetCookie(response, cookieName) {
    const header = setCookieHeader(response)
    const match = header.match(new RegExp("(?:^|[\\n,])\\s*" + cookieName + "=([^;\\n]+)"))
    return match ? match[1].trim() : null
}

export function assertSetCookieHttpOnly(client, response, cookieName) {
    const header = setCookieHeader(response)
    client.assert(header.length > 0, "Expected Set-Cookie header")
    client.assert(
        header.toLowerCase().includes(cookieName.toLowerCase() + "="),
        "Expected cookie " + cookieName,
    )
    client.assert(/httponly/i.test(header), "Cookie must be HttpOnly")
    client.assert(/samesite/i.test(header), "Cookie must set SameSite")
    client.assert(!/password/i.test(header), "Cookie header must not contain password")
}

export function saveAuthToken(client, response, cookieName) {
    const token = authTokenFromSetCookie(response, cookieName)
    client.assert(token != null && token.length > 0, "Expected auth token in Set-Cookie")
    client.global.set("authToken", token)
}
