package com.library.management.service;

import com.library.management.dto.MemberDTO;
import com.library.management.dto.MemberResponseDTO;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Member;
import com.library.management.repository.MemberRepository;
import com.library.management.utils.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    @Cacheable(value = AppConstants.CACHE_MEMBERS, key = AppConstants.CACHE_KEY_ALL)
    @Transactional(readOnly = true)
    public List<MemberResponseDTO> getAllMembers() {
        return memberRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Cacheable(value = AppConstants.CACHE_MEMBERS, key = "#id")
    @Transactional(readOnly = true)
    public MemberResponseDTO getMemberById(Long id) {
        return toDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public List<MemberResponseDTO> searchMembers(String name) {
        return memberRepository.findByNameContainingIgnoreCase(name).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @CacheEvict(value = AppConstants.CACHE_MEMBERS, allEntries = true)
    public MemberResponseDTO createMember(MemberDTO dto) {
        Member member = Member.builder()
            .name(dto.getName())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .membershipDate(LocalDate.now())
            .active(true)
            .build();
        return toDTO(memberRepository.save(member));
    }

    @CacheEvict(value = AppConstants.CACHE_MEMBERS, allEntries = true)
    public MemberResponseDTO updateMember(Long id, MemberDTO dto) {
        Member member = findById(id);
        member.setName(dto.getName());
        member.setEmail(dto.getEmail());
        member.setPhone(dto.getPhone());
        if (dto.getActive() != null) {
            member.setActive(dto.getActive());
        }
        return toDTO(memberRepository.save(member));
    }

    @CacheEvict(value = AppConstants.CACHE_MEMBERS, allEntries = true)
    public MemberResponseDTO deactivateMember(Long id) {
        Member member = findById(id);
        member.setActive(false);
        return toDTO(memberRepository.save(member));
    }

    @CacheEvict(value = AppConstants.CACHE_MEMBERS, allEntries = true)
    public void deleteMember(Long id) {
        memberRepository.delete(findById(id));
    }

    Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
    }

    private MemberResponseDTO toDTO(Member member) {
        return MemberResponseDTO.builder()
            .id(member.getId())
            .name(member.getName())
            .email(member.getEmail())
            .phone(member.getPhone())
            .membershipDate(member.getMembershipDate())
            .active(member.getActive())
            .build();
    }
}
