package com.example.application.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SampleBookRepository extends JpaRepository<SampleBook, Long>, JpaSpecificationExecutor<SampleBook> {
    Page<SampleBook> findByUserUsername(String username, Pageable pageable);
}
