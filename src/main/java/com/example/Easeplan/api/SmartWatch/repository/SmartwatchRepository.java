package com.example.Easeplan.api.SmartWatch.repository;

import com.example.Easeplan.api.SmartWatch.domain.SmartwatchData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SmartwatchRepository extends JpaRepository<SmartwatchData, Long> {

    Optional<SmartwatchData> findByUserEmailOrderByMeasuredAtDesc(String email);

}
