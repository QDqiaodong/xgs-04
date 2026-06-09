package com.bookexchange.repository;

import com.bookexchange.entity.BookTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookTagRepository extends JpaRepository<BookTag, Long> {

    List<BookTag> findByBookId(Long bookId);

    List<BookTag> findByTagId(Long tagId);

    @Query("SELECT bt FROM BookTag bt JOIN FETCH bt.tag WHERE bt.book.id = :bookId")
    List<BookTag> findByBookIdWithTag(@Param("bookId") Long bookId);

    @Query("SELECT bt.book.id FROM BookTag bt WHERE bt.tag.id IN :tagIds")
    List<Long> findBookIdsByTagIds(@Param("tagIds") List<Long> tagIds);

    @Query("SELECT bt.book.id FROM BookTag bt WHERE bt.tag.id IN :tagIds GROUP BY bt.book.id HAVING COUNT(DISTINCT bt.tag.id) = :tagCount")
    List<Long> findBookIdsByAllTagIds(@Param("tagIds") List<Long> tagIds, @Param("tagCount") Long tagCount);

    void deleteByBookIdAndTagId(Long bookId, Long tagId);

    void deleteByBookId(Long bookId);

    @Modifying
    @Query("DELETE FROM BookTag bt WHERE bt.book.id = :bookId AND bt.tag.id IN :tagIds")
    void deleteByBookIdAndTagIds(@Param("bookId") Long bookId, @Param("tagIds") List<Long> tagIds);
}
