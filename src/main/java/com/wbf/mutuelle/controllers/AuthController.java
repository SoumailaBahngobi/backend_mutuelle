package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.configuration.JwtUtil;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.Role;
import com.wbf.mutuelle.repositories.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mutuelle")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // Assurez-vous que c'est injecté
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          MemberRepository memberRepository,
                          PasswordEncoder passwordEncoder, // Injection correcte
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        // Vérifie si email déjà utilisé
        if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email déjà utilisé !");
        }

        // Créer utilisateur avec mot de passe encodé
        Member user = new Member();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setFirstName(request.getFirstName());
        user.setNpi(request.getNpi());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole() != null ? (Role) request.getRole() : Role.MEMBER);

        // IMPORTANT: Encoder le mot de passe avec BCrypt
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        memberRepository.save(user);

        // Générer un token
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects !");
        }

        Member user = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return ResponseEntity.ok(new AuthResponse(token));
    }
}