package com.library.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.BorrowRequestDTO;
import com.library.management.dto.BorrowResponseDTO;
import com.library.management.exception.BookNotAvailableException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.model.BorrowStatus;
import com.library.management.service.BorrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BorrowController.class)
@DisplayName("BorrowController web-layer tests")
class BorrowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BorrowService borrowService;

    @Autowired
    private ObjectMapper objectMapper;

    private BorrowResponseDTO activeBorrowResponse;
    private BorrowResponseDTO returnedBorrowResponse;

    @BeforeEach
    void setUp() {
        activeBorrowResponse = BorrowResponseDTO.builder()
            .id(1L)
            .bookId(1L)
            .bookTitle("1984")
            .memberId(1L)
            .memberName("Alice Johnson")
            .borrowDate(LocalDate.of(2026, 6, 8))
            .dueDate(LocalDate.of(2026, 6, 22))
            .fineAmount(0.0)
            .status(BorrowStatus.BORROWED)
            .build();

        returnedBorrowResponse = BorrowResponseDTO.builder()
            .id(1L)
            .bookId(1L)
            .bookTitle("1984")
            .memberId(1L)
            .memberName("Alice Johnson")
            .borrowDate(LocalDate.of(2026, 6, 8))
            .dueDate(LocalDate.of(2026, 6, 22))
            .returnDate(LocalDate.of(2026, 6, 10))
            .fineAmount(0.0)
            .status(BorrowStatus.RETURNED)
            .build();
    }

    // ─── POST /api/borrows ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/borrows → 201 with borrow record on success")
    void borrowBook_returns201OnSuccess() throws Exception {
        BorrowRequestDTO request = BorrowRequestDTO.builder()
            .bookId(1L).memberId(1L).borrowDays(14).build();
        when(borrowService.borrowBook(any(BorrowRequestDTO.class)))
            .thenReturn(activeBorrowResponse);

        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.bookTitle").value("1984"))
            .andExpect(jsonPath("$.memberName").value("Alice Johnson"))
            .andExpect(jsonPath("$.status").value("BORROWED"))
            .andExpect(jsonPath("$.fineAmount").value(0));
    }

    @Test
    @DisplayName("POST /api/borrows → 400 when bookId and memberId are missing")
    void borrowBook_returns400OnMissingRequiredFields() throws Exception {
        BorrowRequestDTO invalid = new BorrowRequestDTO();

        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/borrows → 409 when no copies available")
    void borrowBook_returns409WhenNoCopiesAvailable() throws Exception {
        BorrowRequestDTO request = BorrowRequestDTO.builder()
            .bookId(1L).memberId(1L).build();
        when(borrowService.borrowBook(any(BorrowRequestDTO.class)))
            .thenThrow(new BookNotAvailableException("No copies available for: 1984"));

        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("No copies available for: 1984"));
    }

    @Test
    @DisplayName("POST /api/borrows → 409 when member is inactive")
    void borrowBook_returns409WhenMemberInactive() throws Exception {
        BorrowRequestDTO request = BorrowRequestDTO.builder()
            .bookId(1L).memberId(2L).build();
        when(borrowService.borrowBook(any(BorrowRequestDTO.class)))
            .thenThrow(new BookNotAvailableException("Member account is inactive"));

        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Member account is inactive"));
    }

    @Test
    @DisplayName("POST /api/borrows → 404 when book or member not found")
    void borrowBook_returns404WhenResourceNotFound() throws Exception {
        BorrowRequestDTO request = BorrowRequestDTO.builder()
            .bookId(99L).memberId(1L).build();
        when(borrowService.borrowBook(any(BorrowRequestDTO.class)))
            .thenThrow(new ResourceNotFoundException("Book not found with id: 99"));

        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Book not found with id: 99"));
    }

    // ─── PUT /api/borrows/{id}/return ────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/borrows/{id}/return → 200 with RETURNED status and no fine")
    void returnBook_returns200OnSuccess() throws Exception {
        when(borrowService.returnBook(1L)).thenReturn(returnedBorrowResponse);

        mockMvc.perform(put("/api/borrows/1/return"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RETURNED"))
            .andExpect(jsonPath("$.fineAmount").value(0))
            .andExpect(jsonPath("$.returnDate").value("2026-06-10"));
    }

    @Test
    @DisplayName("PUT /api/borrows/{id}/return → 200 with fine when late")
    void returnBook_returns200WithFineWhenLate() throws Exception {
        BorrowResponseDTO lateReturn = BorrowResponseDTO.builder()
            .id(1L).bookId(1L).bookTitle("1984").memberId(1L).memberName("Alice")
            .borrowDate(LocalDate.of(2026, 5, 1))
            .dueDate(LocalDate.of(2026, 5, 15))
            .returnDate(LocalDate.of(2026, 5, 20))
            .fineAmount(2.50)
            .status(BorrowStatus.RETURNED)
            .build();
        when(borrowService.returnBook(1L)).thenReturn(lateReturn);

        mockMvc.perform(put("/api/borrows/1/return"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RETURNED"))
            .andExpect(jsonPath("$.fineAmount").value(2.50));
    }

    @Test
    @DisplayName("PUT /api/borrows/{id}/return → 409 when already returned")
    void returnBook_returns409WhenAlreadyReturned() throws Exception {
        when(borrowService.returnBook(1L))
            .thenThrow(new BookNotAvailableException("This book has already been returned"));

        mockMvc.perform(put("/api/borrows/1/return"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("This book has already been returned"));
    }

    @Test
    @DisplayName("PUT /api/borrows/{id}/return → 404 when record not found")
    void returnBook_returns404WhenNotFound() throws Exception {
        when(borrowService.returnBook(99L))
            .thenThrow(new ResourceNotFoundException("Borrow record not found with id: 99"));

        mockMvc.perform(put("/api/borrows/99/return"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Borrow record not found with id: 99"));
    }

    // ─── GET /api/borrows/active ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/borrows/active → 200 with active borrow list")
    void getActiveBorrows_returns200WithList() throws Exception {
        when(borrowService.getActiveBorrows()).thenReturn(List.of(activeBorrowResponse));

        mockMvc.perform(get("/api/borrows/active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].status").value("BORROWED"));
    }

    @Test
    @DisplayName("GET /api/borrows/active → 200 with empty list when nothing borrowed")
    void getActiveBorrows_returns200WithEmptyList() throws Exception {
        when(borrowService.getActiveBorrows()).thenReturn(List.of());

        mockMvc.perform(get("/api/borrows/active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/borrows/overdue ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/borrows/overdue → 200 (empty when nothing overdue)")
    void getOverdueRecords_returns200() throws Exception {
        when(borrowService.getOverdueRecords()).thenReturn(List.of());

        mockMvc.perform(get("/api/borrows/overdue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ─── GET /api/borrows/member/{memberId} ─────────────────────────────────

    @Test
    @DisplayName("GET /api/borrows/member/{memberId} → 200 with member history")
    void getMemberBorrowHistory_returns200() throws Exception {
        when(borrowService.getMemberBorrowHistory(1L))
            .thenReturn(List.of(activeBorrowResponse));

        mockMvc.perform(get("/api/borrows/member/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].memberName").value("Alice Johnson"));
    }

    @Test
    @DisplayName("GET /api/borrows/member/{memberId} → 404 when member not found")
    void getMemberBorrowHistory_returns404WhenMemberNotFound() throws Exception {
        when(borrowService.getMemberBorrowHistory(99L))
            .thenThrow(new ResourceNotFoundException("Member not found with id: 99"));

        mockMvc.perform(get("/api/borrows/member/99"))
            .andExpect(status().isNotFound());
    }

    // ─── GET /api/borrows/book/{bookId} ─────────────────────────────────────

    @Test
    @DisplayName("GET /api/borrows/book/{bookId} → 200 with book borrow history")
    void getBookBorrowHistory_returns200() throws Exception {
        when(borrowService.getBookBorrowHistory(1L))
            .thenReturn(List.of(activeBorrowResponse, returnedBorrowResponse));

        mockMvc.perform(get("/api/borrows/book/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
