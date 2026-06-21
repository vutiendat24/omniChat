package com.omnichat.customer.repository;

import com.omnichat.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Find customers by phone number.
     * Used by CustomerMergerService to detect potential duplicates for merging.
     */
    List<Customer> findByPhoneNumber(String phoneNumber);

    /**
     * Find customers by phone number excluding a specific customer.
     * Useful for finding merge candidates: "other customers with the same phone".
     */
    List<Customer> findByPhoneNumberAndIdNot(String phoneNumber, String id);
}
