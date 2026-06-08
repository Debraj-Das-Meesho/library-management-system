package com.library.management.repository;

import com.library.management.model.BorrowRecord;
import com.library.management.model.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByMemberId(Long memberId);
    List<BorrowRecord> findByBookId(Long bookId);
    List<BorrowRecord> findByStatus(BorrowStatus status);
    List<BorrowRecord> findByDueDateBeforeAndStatus(LocalDate date, BorrowStatus status);
}
