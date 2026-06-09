package com.library.management.service;

import com.library.management.dto.BorrowRequestDTO;
import com.library.management.dto.BorrowResponseDTO;
import com.library.management.exception.BookNotAvailableException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.Book;
import com.library.management.model.BorrowRecord;
import com.library.management.model.BorrowStatus;
import com.library.management.model.Member;
import com.library.management.repository.BorrowRecordRepository;
import com.library.management.utils.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BorrowService {


    private final BorrowRecordRepository borrowRecordRepository;
    private final BookService bookService;
    private final MemberService memberService;

    @CacheEvict(value = AppConstants.CACHE_BOOKS, allEntries = true)
    public BorrowResponseDTO borrowBook(BorrowRequestDTO request) {
        Book book = bookService.findById(request.getBookId());
        Member member = memberService.findById(request.getMemberId());

        if (!member.getActive()) {
            throw new BookNotAvailableException("Member account is inactive");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new BookNotAvailableException("No copies available for: " + book.getTitle());
        }

        int days = request.getBorrowDays() != null ? request.getBorrowDays() : AppConstants.DEFAULT_BORROW_DAYS;

        BorrowRecord record = BorrowRecord.builder()
            .book(book)
            .member(member)
            .borrowDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(days))
            .status(BorrowStatus.BORROWED)
            .fineAmount(0.0)
            .build();

        book.setAvailableCopies(book.getAvailableCopies() - 1);

        return toDTO(borrowRecordRepository.save(record));
    }

    @CacheEvict(value = AppConstants.CACHE_BOOKS, allEntries = true)
    public BorrowResponseDTO returnBook(Long borrowRecordId) {
        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
            .orElseThrow(() -> new ResourceNotFoundException("Borrow record not found with id: " + borrowRecordId));

        if (record.getStatus() == BorrowStatus.RETURNED) {
            throw new BookNotAvailableException("This book has already been returned");
        }

        LocalDate returnDate = LocalDate.now();
        record.setReturnDate(returnDate);
        record.setStatus(BorrowStatus.RETURNED);

        if (returnDate.isAfter(record.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(record.getDueDate(), returnDate);
            record.setFineAmount(AppConstants.FINE_PER_DAY * daysLate);
        }

        record.getBook().setAvailableCopies(record.getBook().getAvailableCopies() + 1);

        return toDTO(borrowRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public List<BorrowResponseDTO> getMemberBorrowHistory(Long memberId) {
        memberService.findById(memberId);
        return borrowRecordRepository.findByMemberId(memberId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BorrowResponseDTO> getBookBorrowHistory(Long bookId) {
        bookService.findById(bookId);
        return borrowRecordRepository.findByBookId(bookId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<BorrowResponseDTO> getOverdueRecords() {
        List<BorrowRecord> overdue = borrowRecordRepository
            .findByDueDateBeforeAndStatus(LocalDate.now(), BorrowStatus.BORROWED);
        overdue.forEach(r -> r.setStatus(BorrowStatus.OVERDUE));
        borrowRecordRepository.saveAll(overdue);

        return borrowRecordRepository.findByStatus(BorrowStatus.OVERDUE).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BorrowResponseDTO> getActiveBorrows() {
        return borrowRecordRepository.findByStatus(BorrowStatus.BORROWED).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    private BorrowResponseDTO toDTO(BorrowRecord record) {
        return BorrowResponseDTO.builder()
            .id(record.getId())
            .bookId(record.getBook().getId())
            .bookTitle(record.getBook().getTitle())
            .memberId(record.getMember().getId())
            .memberName(record.getMember().getName())
            .borrowDate(record.getBorrowDate())
            .dueDate(record.getDueDate())
            .returnDate(record.getReturnDate())
            .fineAmount(record.getFineAmount())
            .status(record.getStatus())
            .build();
    }
}
