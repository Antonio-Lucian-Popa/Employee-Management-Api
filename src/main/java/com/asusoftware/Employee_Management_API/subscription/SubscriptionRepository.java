package com.asusoftware.Employee_Management_API.subscription;

import com.asusoftware.Employee_Management_API.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByCompany_Id(UUID companyId);
}
