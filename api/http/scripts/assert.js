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
    assertPublicUser(client, body, expected)
    client.assert(body.user === undefined, "register body must not wrap the user")
    client.assert(body.token === undefined, "token must not appear in the JSON body")
}

export function assertSessionUser(client, response, expected) {
    assertStatus(client, response, 200)
    const body = jsonBody(response)
    client.assert(body != null && typeof body === "object", "Expected a JSON body")
    assertPublicUser(client, body, expected)
    client.assert(body.user === undefined, "session/login body must not wrap the user")
    client.assert(body.token === undefined, "token must not appear in the JSON body")
}

function collectHeaderValues(headers, name) {
    const collected = []
    if (headers == null) {
        return collected
    }
    if (typeof headers.valuesOf === "function") {
        const values = headers.valuesOf(name)
        if (values != null) {
            if (Array.isArray(values)) collected.push.apply(collected, values)
            else collected.push(values)
        }
    }
    if (collected.length === 0 && typeof headers.valueOf === "function") {
        const value = headers.valueOf(name)
        if (value != null) collected.push(value)
    }
    if (collected.length === 0) {
        const fallback = headers[name] || headers[name.toLowerCase()]
        if (fallback != null) {
            if (Array.isArray(fallback)) collected.push.apply(collected, fallback)
            else collected.push(fallback)
        }
    }
    return collected
}

export function setCookieHeader(response) {
    return collectHeaderValues(response.headers, "Set-Cookie").map(String).join("\n")
}

function decodeCookieValue(value) {
    const trimmed = String(value).trim()
    if (trimmed.length === 0) {
        return trimmed
    }
    try {
        return decodeURIComponent(trimmed)
    } catch (e) {
        return trimmed
    }
}

export function authTokenFromSetCookie(response, cookieName) {
    const cookies = collectHeaderValues(response.headers, "Set-Cookie")
    for (let i = 0; i < cookies.length; i++) {
        const firstPair = String(cookies[i]).split(";")[0]
        const separator = firstPair.indexOf("=")
        if (separator < 0) {
            continue
        }
        const name = firstPair.slice(0, separator).trim()
        if (name.toLowerCase() !== cookieName.toLowerCase()) {
            continue
        }
        const value = decodeCookieValue(firstPair.slice(separator + 1))
        if (value.length > 0) {
            return value
        }
    }
    return null
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

export function assertNoContent(client, response) {
    assertStatus(client, response, 204)
    const body = jsonBody(response)
    if (body == null) {
        return
    }
    client.assert(
        typeof body === "object" && Object.keys(body).length === 0,
        "204 must not include a JSON body",
    )
    client.assert(body.token === undefined, "token must not appear in the JSON body")
    client.assert(body.user === undefined, "user must not appear in the JSON body")
    client.assert(body.password === undefined, "password must not appear in the JSON body")
}

export function assertSessionCookieCleared(client, response, cookieName) {
    const header = setCookieHeader(response)
    client.assert(header.length > 0, "Expected Set-Cookie header")
    client.assert(
        new RegExp("(?:^|[\\n,])\\s*" + cookieName + "=;").test(header),
        "Expected empty cookie " + cookieName,
    )
    client.assert(/max-age=0/i.test(header), "Cookie must expire (Max-Age=0)")
    client.assert(/httponly/i.test(header), "Cookie must be HttpOnly")
    client.assert(/samesite/i.test(header), "Cookie must set SameSite")
    client.assert(/path=\//i.test(header), "Cookie must keep Path=/")
    client.assert(!/password/i.test(header), "Cookie header must not contain password")
}

export function saveAuthToken(client, response, cookieName) {
    const token = authTokenFromSetCookie(response, cookieName)
    client.assert(token != null && token.length > 0, "Expected auth token in Set-Cookie")
    client.global.set("authToken", token)
}

export function assertEvent(client, event, expected) {
    client.assert(event != null && typeof event === "object", "Expected event object")
    client.assert(event.id !== undefined && event.id !== null, "Expected event id")
    if (expected.id !== undefined) {
        client.assert(String(event.id) === String(expected.id), 'Expected event id "' + expected.id + '" but got "' + event.id + '"')
    }
    client.assert(event.name === expected.name, 'Expected name "' + expected.name + '" but got "' + event.name + '"')
    if (Object.prototype.hasOwnProperty.call(expected, "description")) {
        client.assert(event.description === expected.description, 'Expected description "' + expected.description + '" but got "' + event.description + '"')
    }
    if (Object.prototype.hasOwnProperty.call(expected, "iconKey")) {
        client.assert(event.iconKey === expected.iconKey, 'Expected iconKey "' + expected.iconKey + '" but got "' + event.iconKey + '"')
    }
    client.assert(event.baseCurrency === expected.baseCurrency, 'Expected baseCurrency "' + expected.baseCurrency + '" but got "' + event.baseCurrency + '"')
    if (expected.memberCount !== undefined) {
        client.assert(event.memberCount === expected.memberCount, "Expected memberCount " + expected.memberCount + " but got " + event.memberCount)
    }
    client.assert(event.owner === undefined, "event must not include owner")
    client.assert(event.members === undefined, "event must not include members")
}
