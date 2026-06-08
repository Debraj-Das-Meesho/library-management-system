package com.library.management.repository;

import com.library.management.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    List<Member> findByActiveTrue();
    List<Member> findByNameContainingIgnoreCase(String name);
}
