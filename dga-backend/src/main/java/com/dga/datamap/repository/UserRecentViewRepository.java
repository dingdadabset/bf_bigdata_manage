package com.dga.datamap.repository;

import com.dga.datamap.entity.UserRecentView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRecentViewRepository extends JpaRepository<UserRecentView, Long> {
    
    @Query("SELECT v FROM UserRecentView v WHERE v.username = ?1 ORDER BY v.viewedAt DESC")
    List<UserRecentView> findTopByUsername(String username, Pageable pageable);
}
