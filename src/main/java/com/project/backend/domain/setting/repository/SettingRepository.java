package com.project.backend.domain.setting.repository;

import com.project.backend.domain.setting.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettingRepository extends JpaRepository<Setting, Long> {

    @Modifying
    @Query("DELETE FROM Setting s WHERE s.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
