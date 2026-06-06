package com.bookexchange.repository;

import com.bookexchange.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    List<Book> findByOwnerId(Long ownerId);

    List<Book> findByCanBorrowTrueAndAvailableTrue();

    List<Book> findByCityIdAndCanBorrowTrueAndAvailableTrue(Long cityId);
}
