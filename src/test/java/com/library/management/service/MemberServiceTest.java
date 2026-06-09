package com.library.management.service;

import com.library.management.dto.MemberDTO;
import com.library.management.dto.MemberResponseDTO;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Member;
import com.library.management.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService unit tests")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
            .id(1L)
            .name("Alice Johnson")
            .email("alice@example.com")
            .phone("555-0101")
            .membershipDate(LocalDate.now())
            .active(true)
            .build();
    }

    // ─── createMember ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createMember: auto-sets membershipDate to today and active=true")
    void createMember_setsMembershipDateAndActive() {
        MemberDTO dto = MemberDTO.builder()
            .name("Alice Johnson")
            .email("alice@example.com")
            .phone("555-0101")
            .build();
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        MemberResponseDTO result = memberService.createMember(dto);

        assertThat(result.getName()).isEqualTo("Alice Johnson");
        assertThat(result.getActive()).isTrue();
        verify(memberRepository).save(argThat(m ->
            m.getMembershipDate() != null &&
            m.getMembershipDate().isEqual(LocalDate.now()) &&
            m.getActive()
        ));
    }

    // ─── getMemberById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getMemberById: returns DTO when member exists")
    void getMemberById_returnsDTO_whenFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponseDTO result = memberService.getMemberById(1L);

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getActive()).isTrue();
    }

    @Test
    @DisplayName("getMemberById: throws ResourceNotFoundException when not found")
    void getMemberById_throwsWhenNotFound() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMemberById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ─── getAllMembers ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllMembers: returns list of all members")
    void getAllMembers_returnsAllMembers() {
        Member second = Member.builder().id(2L).name("Bob Smith")
            .email("bob@example.com").active(true).build();
        when(memberRepository.findAll()).thenReturn(List.of(member, second));

        List<MemberResponseDTO> results = memberService.getAllMembers();

        assertThat(results).hasSize(2);
        assertThat(results).extracting(MemberResponseDTO::getName)
            .containsExactlyInAnyOrder("Alice Johnson", "Bob Smith");
    }

    // ─── updateMember ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateMember: updates name, email, phone")
    void updateMember_updatesFields() {
        MemberDTO dto = MemberDTO.builder()
            .name("Alice Smith")
            .email("alice.smith@example.com")
            .phone("555-9999")
            .build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        memberService.updateMember(1L, dto);

        assertThat(member.getName()).isEqualTo("Alice Smith");
        assertThat(member.getEmail()).isEqualTo("alice.smith@example.com");
        assertThat(member.getPhone()).isEqualTo("555-9999");
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("updateMember: sets active when provided in DTO")
    void updateMember_setsActiveWhenProvided() {
        MemberDTO dto = MemberDTO.builder()
            .name("Alice Johnson")
            .email("alice@example.com")
            .active(false)
            .build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        memberService.updateMember(1L, dto);

        assertThat(member.getActive()).isFalse();
    }

    // ─── deactivateMember ───────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateMember: sets active=false on the member entity")
    void deactivateMember_setsActiveFalse() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        memberService.deactivateMember(1L);

        assertThat(member.getActive()).isFalse();
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("deactivateMember: throws when member not found")
    void deactivateMember_throwsWhenNotFound() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deactivateMember(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── deleteMember ───────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteMember: calls repository.delete with found member")
    void deleteMember_callsRepositoryDelete() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.deleteMember(1L);

        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("deleteMember: throws and never deletes when not found")
    void deleteMember_throwsWhenNotFound() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteMember(99L))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(memberRepository, never()).delete(any());
    }

    // ─── searchMembers ──────────────────────────────────────────────────────

    @Test
    @DisplayName("searchMembers: returns members whose names match the query")
    void searchMembers_returnsMatchingResults() {
        when(memberRepository.findByNameContainingIgnoreCase("Alice"))
            .thenReturn(List.of(member));

        List<MemberResponseDTO> results = memberService.searchMembers("Alice");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Alice Johnson");
    }

    @Test
    @DisplayName("searchMembers: returns empty list when no match")
    void searchMembers_returnsEmptyList_whenNoMatch() {
        when(memberRepository.findByNameContainingIgnoreCase("Zara"))
            .thenReturn(List.of());

        assertThat(memberService.searchMembers("Zara")).isEmpty();
    }
}
