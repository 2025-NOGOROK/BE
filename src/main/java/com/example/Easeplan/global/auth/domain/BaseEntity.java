package com.example.Easeplan.global.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)  // 자동으로 날짜 업데이트
public abstract class BaseEntity {

    @CreatedDate  // 생성될 때 자동으로 시간 설정
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate  // 수정될 때마다 자동 업데이트
    private LocalDateTime updatedAt;
}
