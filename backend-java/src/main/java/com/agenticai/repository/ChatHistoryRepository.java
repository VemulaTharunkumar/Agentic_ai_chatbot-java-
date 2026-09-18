package com.agenticai.repository;

import com.agenticai.model.ChatHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, String> {
    List<ChatHistory> findByUserIdOrderByIsPinnedDescTimestampDesc(String userId, Pageable pageable);
    
    long countByUserIdAndIsPinnedTrue(String userId);
    
    long countByUserId(String userId);
    
    List<ChatHistory> findByUserIdAndIsPinnedFalseOrderByTimestampAsc(String userId, Pageable pageable);
    
    List<ChatHistory> findByUserIdOrderByTimestampAsc(String userId, Pageable pageable);
}
