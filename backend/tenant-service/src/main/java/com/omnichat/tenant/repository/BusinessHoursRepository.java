package com.omnichat.tenant.repository;

import com.omnichat.tenant.domain.entity.BusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessHoursRepository extends JpaRepository<BusinessHours, String> {
}
