package com.library.management.controller;

import com.library.management.dto.BorrowRequestDTO;
import com.library.management.dto.BorrowResponseDTO;
import com.library.management.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @PostMapping
    public ResponseEntity<BorrowResponseDTO> borrowBook(@Valid @RequestBody BorrowRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(borrowService.borrowBook(request));
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<BorrowResponseDTO> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(borrowService.returnBook(id));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<BorrowResponseDTO>> getMemberBorrowHistory(@PathVariable Long memberId) {
        return ResponseEntity.ok(borrowService.getMemberBorrowHistory(memberId));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<BorrowResponseDTO>> getBookBorrowHistory(@PathVariable Long bookId) {
        return ResponseEntity.ok(borrowService.getBookBorrowHistory(bookId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<BorrowResponseDTO>> getOverdueRecords() {
        return ResponseEntity.ok(borrowService.getOverdueRecords());
    }

    @GetMapping("/active")
    public ResponseEntity<List<BorrowResponseDTO>> getActiveBorrows() {
        return ResponseEntity.ok(borrowService.getActiveBorrows());
    }
}
