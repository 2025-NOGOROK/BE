package com.example.Easeplan.api.SmartWatch.repository;

import com.example.Easeplan.api.SmartWatch.domain.SmartwatchData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SmartwatchRepository extends JpaRepository<SmartwatchData, Long> {
    @Query("SELECT d FROM SmartwatchData d WHERE d.user.email = :email")
    List<SmartwatchData> findByUserEmail(@Param("email") String email);

    Optional<SmartwatchData> findFirstByDeviceId(String deviceId); // 최초 등록된 기기 정보 조회
}