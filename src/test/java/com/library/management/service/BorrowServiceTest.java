package com.library.management.service;

import com.library.management.dto.BorrowRequestDTO;
import com.library.management.dto.BorrowResponseDTO;
import com.library.management.exception.BookNotAvailableException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.*;
import com.library.management.repository.BorrowRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowService unit tests")
class BorrowServiceTest {

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private BookService bookService;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private BorrowService borrowService;

    private Author author;
    private Book book;
    private Member activeMember;
    private Member inactiveMember;
    private BorrowRecord borrowRecord;

    @BeforeEach
    void setUp() {
        author = Author.builder().id(1L).name("George Orwell").nationality("British").build();

        book = Book.builder()
            .id(1L)
            .title("1984")
            .isbn("978-0-452-28423-4")
            .totalCopies(5)
            .availableCopies(3)
            .author(author)
            .build();

        activeMember = Member.builder()
            .id(1L).name("Alice Johnson").email("alice@example.com")
            .active(true).membershipDate(LocalDate.now()).build();

        inactiveMember = Member.builder()
            .id(2L).name("Bob Smith").email("bob@example.com")
            .active(false).membershipDate(LocalDate.now()).build();

        borrowRecord = BorrowRecord.builder()
            .id(1L)
            .book(book)
            .member(activeMember)
            .borrowDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(14))
            .status(BorrowStatus.BORROWED)
            .fineAmount(BigDecimal.ZERO)
            .build();
    }

    // ─── borrowBook ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("borrowBook: creates record and decrements availableCopies by 1")
    void borrowBook_succeeds_andDecrementsAvailableCopies() {
        BorrowRequestDTO request = BorrowRequestDTO.builder()
            .bookId(1L).memberId(1L).borrowDays(14).build();
        when(bookService.findById(1L)).thenReturn(book);
        when(memberService.findById(1L)).thenReturn(activeMember);
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(borrowRecord);

        BorrowResponseDTO result = borrowService.borrowBook(request);

        assertThat(result.getStatus()).isEqualTo(BorrowStatus.BORROWED);
        assertThat(result.getBookTitle()).isEqualTo("1984");
        assertThat(book.getAvailableCopies()).isEqualTo(2);
        verify(borrowRecordRepository).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("borrowBook: uses 14 days as default when borrowDays is null")
    void borrowBook_usesDefaultDays_whenBorrowDaysNull() {
        BorrowRequestDTO request = BorrowRequestDTO.builder()
            .bookId(1L).memberId(1L).borrowDays(null).build();
        when(bookService.findById(1L)).thenReturn(book);
        when(memberService.findById(1L)).thenReturn(activeMember);
        when(borrowRecordRepository.save(any(BorrowRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        BorrowResponseDTO result = borrowService.borrowBook(request);

        assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(14));
    }

    @Test
    @DisplayName("borrowBook: uses custom borrowDays when provided")
    void borrowBook_usesCustomBorrowDays() {
        BorrowRequestDTO request = BorrowRequestDTO.builder()
            .bookId(1L).memberId(1L).borrowDays(7).build();
        when(bookService.findById(1L)).thenReturn(book);
        when(memberService.findById(1L)).thenReturn(activeMember);
        when(borrowRecordRepository.save(any(BorrowRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        BorrowResponseDTO result = borrowService.borrowBook(request);

        assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(7));
    }

    @Test
    @DisplayName("borrowBook: throws BookNotAvailableException when member is inactive")
    void borrowBook_throwsBookNotAvailable_whenInactiveMember() {
        BorrowRequestDTO request = BorrowRequestDTO.builder()
            .bookId(1L).memberId(2L).build();
        when(bookService.findById(1L)).thenReturn(book);
        when(memberService.findById(2L)).thenReturn(inactiveMember);

        assertThatThrownBy(() -> borrowService.borrowBook(request))
            .isInstanceOf(BookNotAvailableException.class)
            .hasMessageContaining("inactive");
        verify(borrowRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("borrowBook: throws BookNotAvailableException when availableCopies is 0")
    void borrowBook_throwsBookNotAvailable_whenNoCopiesLeft() {
        book.setAvailableCopies(0);
        BorrowRequestDTO request = BorrowRequestDTO.builder()
            .bookId(1L).memberId(1L).build();
        when(bookService.findById(1L)).thenReturn(book);
        when(memberService.findById(1L)).thenReturn(activeMember);

        assertThatThrownBy(() -> borrowService.borrowBook(request))
            .isInstanceOf(BookNotAvailableException.class)
            .hasMessageContaining("No copies available");
        verify(borrowRecordRepository, never()).save(any());
    }

    // ─── returnBook ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("returnBook: sets returnDate, marks RETURNED, increments availableCopies")
    void returnBook_setsReturnDateAndIncrementsAvailableCopies() {
        when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(borrowRecord);

        BorrowResponseDTO result = borrowService.returnBook(1L);

        assertThat(result.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(result.getReturnDate()).isEqualTo(LocalDate.now());
        assertThat(book.getAvailableCopies()).isEqualTo(4);
        assertThat(result.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("returnBook: calculates fine of $0.50/day when returned late")
    void returnBook_calculatesFine_whenOverdue() {
        borrowRecord.setDueDate(LocalDate.now().minusDays(5));
        when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(borrowRecord);

        borrowService.returnBook(1L);

        // 5 days × $0.50 = $2.50
        assertThat(borrowRecord.getFineAmount())
            .isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    @DisplayName("returnBook: no fine when returned exactly on due date")
    void returnBook_noFine_whenReturnedOnDueDate() {
        borrowRecord.setDueDate(LocalDate.now());
        when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(borrowRecord);

        borrowService.returnBook(1L);

        assertThat(borrowRecord.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("returnBook: throws BookNotAvailableException when already returned")
    void returnBook_throwsWhenAlreadyReturned() {
        borrowRecord.setStatus(BorrowStatus.RETURNED);
        when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));

        assertThatThrownBy(() -> borrowService.returnBook(1L))
            .isInstanceOf(BookNotAvailableException.class)
            .hasMessageContaining("already been returned");
        verify(borrowRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("returnBook: throws ResourceNotFoundException when record not found")
    void returnBook_throwsWhenRecordNotFound() {
        when(borrowRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowService.returnBook(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ─── query methods ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getMemberBorrowHistory: returns all records for the given member")
    void getMemberBorrowHistory_returnsRecordsForMember() {
        when(memberService.findById(1L)).thenReturn(activeMember);
        when(borrowRecordRepository.findByMemberId(1L)).thenReturn(List.of(borrowRecord));

        List<BorrowResponseDTO> results = borrowService.getMemberBorrowHistory(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMemberName()).isEqualTo("Alice Johnson");
    }

    @Test
    @DisplayName("getBookBorrowHistory: returns all records for the given book")
    void getBookBorrowHistory_returnsRecordsForBook() {
        when(bookService.findById(1L)).thenReturn(book);
        when(borrowRecordRepository.findByBookId(1L)).thenReturn(List.of(borrowRecord));

        List<BorrowResponseDTO> results = borrowService.getBookBorrowHistory(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBookTitle()).isEqualTo("1984");
    }

    @Test
    @DisplayName("getActiveBorrows: returns only records with BORROWED status")
    void getActiveBorrows_returnsOnlyBorrowedRecords() {
        when(borrowRecordRepository.findByStatus(BorrowStatus.BORROWED))
            .thenReturn(List.of(borrowRecord));

        List<BorrowResponseDTO> results = borrowService.getActiveBorrows();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(BorrowStatus.BORROWED);
    }

    @Test
    @DisplayName("getActiveBorrows: returns empty list when nothing is borrowed")
    void getActiveBorrows_returnsEmpty_whenNoBorrows() {
        when(borrowRecordRepository.findByStatus(BorrowStatus.BORROWED))
            .thenReturn(List.of());

        assertThat(borrowService.getActiveBorrows()).isEmpty();
    }
}
