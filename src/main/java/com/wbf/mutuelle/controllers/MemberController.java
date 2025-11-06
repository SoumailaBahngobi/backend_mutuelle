package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.dto.ForgotPasswordRequest;
import com.wbf.mutuelle.dto.MessageResponse;
import com.wbf.mutuelle.dto.ResetPasswordRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.services.MemberService;
import com.wbf.mutuelle.services.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3000)
@RestController
@RequestMapping("mutuelle/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final String UPLOAD_DIR = "./uploads/profile-images/";
    private final PasswordResetService passwordResetService;

    @GetMapping("/profile")
    public ResponseEntity<Member> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Member member = memberService.getAllMembers().stream()
                .filter(m -> m.getEmail().equals(email))
                .findFirst()
                .orElseThrow();
        return ResponseEntity.ok(member);
    }

    @PostMapping(value = "/upload-profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utilisateur non authentifié");
            }

            String email = authentication.getName();
            Member member = memberService.getMemberByEmail(email).orElseThrow(() -> new RuntimeException("Membre non trouvé"));

            if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("Fichier vide");

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String filename = "profile_" + member.getId() + System.currentTimeMillis() + extension;
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // update member
            memberService.updateProfileImage(member.getId(), filename);

            return ResponseEntity.ok().body(Map.of("filename", filename));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur upload: " + e.getMessage());
        }
    }

    @GetMapping(value = "/profile-image/{filename}")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename);
            if (!Files.exists(filePath)) return ResponseEntity.notFound().build();

            String contentType = Files.probeContentType(filePath);
            byte[] data = Files.readAllBytes(filePath);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/profile/update")
    public ResponseEntity<Member> updateProfile(@RequestBody Member memberDetails) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String email = authentication.getName();
            Member member = memberService.getMemberByEmail(email).orElseThrow(() -> new RuntimeException("Membre non trouvé"));

            member.setName(memberDetails.getName());
            member.setFirstName(memberDetails.getFirstName());
            member.setPhone(memberDetails.getPhone());
            member.setNpi(memberDetails.getNpi());
            member.setPassword(memberDetails.getPassword());
            member.setEmail(memberDetails.getEmail());
            member.setRole(memberDetails.getRole());
            
            // add other fields as necessary

            Member updatedMember = memberService.updateMember(member.getId(), member);
            return ResponseEntity.ok(updatedMember);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public List<Member> getAllMembers() {
        return memberService.getAllMembers();
    }

    @GetMapping("/{id}")
    public Member getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id).orElseThrow();
    }

    @PostMapping
    public Member createMember(@RequestBody Member member) {
        return memberService.createMember(member);
    }

    @PostMapping("/{id}")
    public Member updateMember(@PathVariable Long id, @RequestBody Member memberDetails) {
        return memberService.updateMember(id, memberDetails);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/subscription")
    public ResponseEntity<Member> updateSubscription(
            @PathVariable Long id,
            @RequestParam Boolean isRegular,
            @RequestParam String subscriptionDate) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(subscriptionDate);
            Member updatedMember = memberService.updateSubscriptionStatus(id, isRegular, date);
            return ResponseEntity.ok(updatedMember);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/debt-status")
    public ResponseEntity<Member> updateDebtStatus(
            @PathVariable Long id,
            @RequestParam Boolean hasDebt) {
        try {
            Member updatedMember = memberService.updateDebtStatus(id, hasDebt);
            return ResponseEntity.ok(updatedMember);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/can-request-loan")
    public ResponseEntity<Boolean> canRequestLoan(@PathVariable Long id) {
        try {
            boolean canRequest = memberService.validateMemberForLoan(id);
            return ResponseEntity.ok(canRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @GetMapping("/current/can-request-loan")
    public ResponseEntity<Boolean> canCurrentUserRequestLoan(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            Member member = memberService.getMemberByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
            boolean canRequest = memberService.validateMemberForLoan(member.getId());
            return ResponseEntity.ok(canRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            String email = request.getEmail();

            // Vérifier si le membre existe
            Member member = memberService.getMemberByEmail(email)
                    .orElse(null);

            if (member == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Aucun compte trouvé avec cette adresse email"));
            }

            // Générer et envoyer le token de réinitialisation
            passwordResetService.generateAndSendResetToken(member);

            return ResponseEntity.ok(new MessageResponse("Un email de réinitialisation a été envoyé à votre adresse"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Erreur lors du traitement de votre demande"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            String token = request.getToken();
            String newPassword = request.getNewPassword();

            // Valider et traiter la réinitialisation
            boolean success = passwordResetService.resetPasswordWithToken(token, newPassword);

            if (success) {
                return ResponseEntity.ok(new MessageResponse("Mot de passe réinitialisé avec succès"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Token invalide ou expiré"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Erreur lors de la réinitialisation du mot de passe"));
        }
    }

    @PostMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        try {
            boolean isValid = passwordResetService.validateResetToken(token);

            if (isValid) {
                return ResponseEntity.ok(new MessageResponse("Token valide"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Token invalide ou expiré"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Erreur lors de la validation du token"));
        }
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        try {
            boolean isValid = passwordResetService.validateResetToken(token);

            if (isValid) {
                return ResponseEntity.ok(Map.of("valid", true, "message", "Token valide"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("valid", false, "message", "Token invalide ou expiré"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("valid", false, "message", "Erreur lors de la validation du token"));
        }
    }

     @PutMapping("/{id}")
    public ResponseEntity<Member> updateMemberById(@PathVariable Long id, @RequestBody Member memberDetails) {
        try {
            Member updatedMember = memberService.updateMember(id, memberDetails);
            return ResponseEntity.ok(updatedMember);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}