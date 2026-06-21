package com.omnichat.customer.service;

import com.omnichat.customer.entity.Customer;
import com.omnichat.customer.repository.ChannelIdentityRepository;
import com.omnichat.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Task 5.2.1.1 - Domain Service: CustomerMergerService
 * Per DDD_Omnichannel_Chat_Management_v1.md §4:
 *   "CustomerMergerService (Customer Context): Chứa logic so khớp (Matching logic)
 *    số điện thoại hoặc ID để hợp nhất 2 CustomerProfile thành một định danh duy nhất."
 *
 * Merge Strategy (non-destructive):
 * 1. Identify the "target" customer (the one to keep) and "source" (to be absorbed)
 * 2. Transfer all ChannelIdentities from source → target
 * 3. Fill missing contact info on target from source (don't overwrite existing)
 * 4. Delete the source customer profile
 *
 * The target is chosen as the customer created earlier (has more history),
 * or the caller can explicitly specify which is target vs source.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerMergerService {

    private final CustomerRepository customerRepository;
    private final ChannelIdentityRepository channelIdentityRepository;

    /**
     * Merge two customer profiles by their IDs.
     * The target customer absorbs the source customer's data and identities.
     *
     * @param targetCustomerId the customer to keep (absorbs data)
     * @param sourceCustomerId the customer to merge away (will be deleted)
     * @return the merged target Customer
     * @throws jakarta.persistence.EntityNotFoundException if either customer not found
     * @throws IllegalArgumentException if trying to merge a customer with itself
     */
    @Transactional
    public Customer mergeCustomers(String targetCustomerId, String sourceCustomerId) {
        // Validation: cannot merge with self
        if (targetCustomerId.equals(sourceCustomerId)) {
            throw new IllegalArgumentException("Cannot merge a customer with itself: " + targetCustomerId);
        }

        // Load both customers with their channel identities
        Customer target = customerRepository.findById(targetCustomerId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Target customer not found: " + targetCustomerId));

        Customer source = customerRepository.findById(sourceCustomerId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Source customer not found: " + sourceCustomerId));

        log.info("Merging customer {} (source) into {} (target)", sourceCustomerId, targetCustomerId);

        int sourceIdentityCount = source.getChannelIdentities().size();

        // Delegate to the domain model's mergeWith behavior
        target.mergeWith(source);

        // Persist the merged target (cascades identity updates)
        target = customerRepository.save(target);

        // Delete the now-empty source customer
        customerRepository.delete(source);

        log.info("Merge complete: customer {} absorbed {} identities from customer {} (deleted)",
                targetCustomerId, sourceIdentityCount, sourceCustomerId);

        return target;
    }

    /**
     * Find and merge all customers that share the same phone number.
     * This implements the "automatic merge suggestion" described in the DDD doc.
     *
     * Strategy: the earliest-created customer becomes the target (primary profile).
     *
     * @param phoneNumber the phone number to match on
     * @return the merged Customer (or null if no duplicates found)
     */
    @Transactional
    public Customer mergeByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required for merge matching");
        }

        List<Customer> matches = customerRepository.findByPhoneNumber(phoneNumber);

        if (matches.size() <= 1) {
            log.debug("No duplicates found for phone number: {}", phoneNumber);
            return matches.isEmpty() ? null : matches.get(0);
        }

        log.info("Found {} customers with phone number {}, starting merge", matches.size(), phoneNumber);

        // Sort by createdAt ASC → oldest becomes the target (primary profile)
        matches.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });

        Customer target = matches.get(0);

        // Merge all subsequent matches into the target
        for (int i = 1; i < matches.size(); i++) {
            target = mergeCustomers(target.getId(), matches.get(i).getId());
        }

        return target;
    }
}
