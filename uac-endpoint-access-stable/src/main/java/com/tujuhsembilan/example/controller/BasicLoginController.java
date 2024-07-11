package com.tujuhsembilan.example.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import com.tujuhsembilan.example.configuration.property.AuthProp;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BasicLoginController {

  private final ObjectMapper objMap;

  private final JwtEncoder jwtEncoder;
  private final AuthProp authProp;

  private final ECKey ecJwk;

  @GetMapping("/jwks.json")
  public ResponseEntity<?> jwk() throws JsonProcessingException {
    return ResponseEntity.ok(Map.of("keys", Set.of(objMap.readTree(ecJwk.toPublicJWK().toJSONString()))));
  }

  // You MUST login using BASIC AUTH, NOT POST BODY
  @PostMapping("/login")
  public ResponseEntity<?> login(@NotNull Authentication auth) {
    var now = Instant.now();
    var expiry = now.plus(Duration.ofHours(1));

    var claims = JwtClaimsSet.builder()
        .issuer(authProp.getUuid())
        .audience(List.of(authProp.getUuid()))
        .subject(((User) auth.getPrincipal()).getUsername())
        .expiresAt(expiry)
        .claim("role", ((User) auth.getPrincipal()).getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()))
        .build();

    var headers = JwsHeader.with(SignatureAlgorithm.ES512).build();

    var jwt = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims));

    return ResponseEntity.ok(jwt.getTokenValue());
  }
}
