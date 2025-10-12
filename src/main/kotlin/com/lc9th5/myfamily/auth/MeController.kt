package com.lc9th5.myfamily.auth


import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MeController {
    @GetMapping("/auth/me")
    fun me(auth: JwtAuthenticationToken): Map<String, Any?> {
        val jwt = auth.token
        return mapOf(
            "sub" to jwt.subject,
            "email" to jwt.getClaimAsString("email"),
            "name" to jwt.getClaimAsString("name"),
            "picture" to jwt.getClaimAsString("picture")
        )
    }
}