package com.library.management.controller;

import com.library.management.dto.MemberDTO;
import com.library.management.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<List<MemberDTO>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberDTO> getMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MemberDTO>> searchMembers(@RequestParam String name) {
        return ResponseEntity.ok(memberService.searchMembers(name));
    }

    @PostMapping
    public ResponseEntity<MemberDTO> createMember(@Valid @RequestBody MemberDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.createMember(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberDTO> updateMember(@PathVariable Long id, @Valid @RequestBody MemberDTO dto) {
        return ResponseEntity.ok(memberService.updateMember(id, dto));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<MemberDTO> deactivateMember(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.deactivateMember(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }
}
