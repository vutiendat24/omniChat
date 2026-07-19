package com.omnichat.customer.service;

import com.omnichat.customer.entity.ChannelIdentity;
import com.omnichat.customer.entity.Customer;
import com.omnichat.customer.repository.ChannelIdentityRepository;
import com.omnichat.customer.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Task 5.2.1.1 - Unit Tests for CustomerMergerService
 * DoD: Pass 100% UT cho các test case gộp data.
 *
 * Test cases cover:
 * 1. Successful merge: identities transferred, contact info filled
 * 2. Non-destructive merge: existing target data not overwritten
 * 3. Self-merge prevention
 * 4. Customer not found (target / source)
 * 5. Merge by phone number: multiple duplicates
 * 6. Merge by phone number: no duplicates
 * 7. Merge by phone number: null/blank phone
 * 8. Edge cases: empty identities, null fields
 */
@ExtendWith(MockitoExtension.class)
class CustomerMergerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ChannelIdentityRepository channelIdentityRepository;

    @InjectMocks
    private CustomerMergerService mergerService;

    private Customer targetCustomer;
    private Customer sourceCustomer;

    @BeforeEach
    void setUp() {
        // Target: older customer with Facebook identity
        targetCustomer = Customer.builder()
                .id("target-uuid-001")
                .fullName("Nguyen Van A")
                .phoneNumber("0901234567")
                .address("123 Le Loi, HCMC")
                .channelIdentities(new ArrayList<>())
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        ChannelIdentity fbIdentity = ChannelIdentity.builder()
                .id(UUID.randomUUID().toString())
                .customerId(targetCustomer.getId())
                .platform(ChannelIdentity.Platform.FACEBOOK)
                .externalUserId("fb-12345")
                .build();
        targetCustomer.getChannelIdentities().add(fbIdentity);

        // Source: newer customer with Zalo identity
        sourceCustomer = Customer.builder()
                .id("source-uuid-002")
                .fullName("Nguyen A")
                .phoneNumber("0901234567")
                .address("456 Nguyen Hue, HCMC")
                .channelIdentities(new ArrayList<>())
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();

        ChannelIdentity zaloIdentity = ChannelIdentity.builder()
                .id(UUID.randomUUID().toString())
                .customerId(sourceCustomer.getId())
                .platform(ChannelIdentity.Platform.ZALO)
                .externalUserId("zalo-67890")
                .build();
        sourceCustomer.getChannelIdentities().add(zaloIdentity);
    }

    // =====================================================
    // MERGE BY IDs
    // =====================================================

    @Nested
    @DisplayName("mergeCustomers(targetId, sourceId)")
    class MergeCustomersById {

        @Test
        @DisplayName("Should transfer all identities from source to target")
        void shouldTransferIdentities() {
            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Customer result = mergerService.mergeCustomers("target-uuid-001", "source-uuid-002");

            // Target now has both FB and Zalo identities
            assertThat(result.getChannelIdentities()).hasSize(2);
            assertThat(result.getChannelIdentities())
                    .extracting(ChannelIdentity::getPlatform)
                    .containsExactlyInAnyOrder(
                            ChannelIdentity.Platform.FACEBOOK,
                            ChannelIdentity.Platform.ZALO);

            // All identities should point to target customer
            assertThat(result.getChannelIdentities())
                    .allMatch(id -> id.getCustomerId().equals("target-uuid-001"));
        }

        @Test
        @DisplayName("Should NOT overwrite existing target data (non-destructive)")
        void shouldNotOverwriteExistingTargetData() {
            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Customer result = mergerService.mergeCustomers("target-uuid-001", "source-uuid-002");

            // Target's existing data should be preserved (not overwritten by source)
            assertThat(result.getFullName()).isEqualTo("Nguyen Van A"); // target's name kept
            assertThat(result.getPhoneNumber()).isEqualTo("0901234567"); // same phone
            assertThat(result.getAddress()).isEqualTo("123 Le Loi, HCMC"); // target's address kept
        }

        @Test
        @DisplayName("Should fill missing target fields from source")
        void shouldFillMissingFieldsFromSource() {
            // Target with missing address
            targetCustomer.setAddress(null);

            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Customer result = mergerService.mergeCustomers("target-uuid-001", "source-uuid-002");

            // Missing address should be filled from source
            assertThat(result.getAddress()).isEqualTo("456 Nguyen Hue, HCMC");
        }

        @Test
        @DisplayName("Should fill missing fullName from source")
        void shouldFillMissingFullNameFromSource() {
            targetCustomer.setFullName("");

            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Customer result = mergerService.mergeCustomers("target-uuid-001", "source-uuid-002");

            assertThat(result.getFullName()).isEqualTo("Nguyen A");
        }

        @Test
        @DisplayName("Should delete source customer after merge")
        void shouldDeleteSourceCustomer() {
            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            mergerService.mergeCustomers("target-uuid-001", "source-uuid-002");

            verify(customerRepository).delete(sourceCustomer);
        }

        @Test
        @DisplayName("Should save the merged target customer")
        void shouldSaveMergedTarget() {
            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            mergerService.mergeCustomers("target-uuid-001", "source-uuid-002");

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo("target-uuid-001");
        }

        @Test
        @DisplayName("Should throw when merging customer with itself")
        void shouldThrowOnSelfMerge() {
            assertThatThrownBy(() -> mergerService.mergeCustomers("same-id", "same-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot merge a customer with itself");
        }

        @Test
        @DisplayName("Should throw when target customer not found")
        void shouldThrowWhenTargetNotFound() {
            when(customerRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mergerService.mergeCustomers("nonexistent", "source-uuid-002"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Target customer not found");
        }

        @Test
        @DisplayName("Should throw when source customer not found")
        void shouldThrowWhenSourceNotFound() {
            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mergerService.mergeCustomers("target-uuid-001", "nonexistent"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Source customer not found");
        }

        @Test
        @DisplayName("Should handle source with no identities")
        void shouldHandleSourceWithNoIdentities() {
            sourceCustomer.getChannelIdentities().clear();

            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Customer result = mergerService.mergeCustomers("target-uuid-001", "source-uuid-002");

            // Target retains its original identity only
            assertThat(result.getChannelIdentities()).hasSize(1);
            assertThat(result.getChannelIdentities().get(0).getPlatform())
                    .isEqualTo(ChannelIdentity.Platform.FACEBOOK);
        }

        @Test
        @DisplayName("Should handle source with multiple identities")
        void shouldHandleSourceWithMultipleIdentities() {
            // Add another identity to source
            ChannelIdentity shopeeIdentity = ChannelIdentity.builder()
                    .id(UUID.randomUUID().toString())
                    .customerId(sourceCustomer.getId())
                    .platform(ChannelIdentity.Platform.SHOPEE)
                    .externalUserId("shopee-99999")
                    .build();
            sourceCustomer.getChannelIdentities().add(shopeeIdentity);

            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Customer result = mergerService.mergeCustomers("target-uuid-001", "source-uuid-002");

            // Target should have 3 identities: FB (original) + Zalo + Shopee (from source)
            assertThat(result.getChannelIdentities()).hasSize(3);
            assertThat(result.getChannelIdentities())
                    .extracting(ChannelIdentity::getPlatform)
                    .containsExactlyInAnyOrder(
                            ChannelIdentity.Platform.FACEBOOK,
                            ChannelIdentity.Platform.ZALO,
                            ChannelIdentity.Platform.SHOPEE);
        }
    }

    // =====================================================
    // MERGE BY PHONE NUMBER
    // =====================================================

    @Nested
    @DisplayName("mergeByPhoneNumber(phoneNumber)")
    class MergeByPhoneNumber {

        @Test
        @DisplayName("Should merge multiple customers with same phone number")
        void shouldMergeMultipleDuplicates() {
            // Create a third duplicate
            Customer thirdCustomer = Customer.builder()
                    .id("third-uuid-003")
                    .fullName("Van A")
                    .phoneNumber("0901234567")
                    .channelIdentities(new ArrayList<>())
                    .createdAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                    .build();
            ChannelIdentity tiktokIdentity = ChannelIdentity.builder()
                    .id(UUID.randomUUID().toString())
                    .customerId(thirdCustomer.getId())
                    .platform(ChannelIdentity.Platform.TIKTOK)
                    .externalUserId("tiktok-11111")
                    .build();
            thirdCustomer.getChannelIdentities().add(tiktokIdentity);

            when(customerRepository.findByPhoneNumber("0901234567"))
                    .thenReturn(List.of(targetCustomer, sourceCustomer, thirdCustomer));
            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("third-uuid-003")).thenReturn(Optional.of(thirdCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Customer result = mergerService.mergeByPhoneNumber("0901234567");

            // Oldest customer (target, Jan 2026) should be the primary
            assertThat(result.getId()).isEqualTo("target-uuid-001");
            // Should have merged all 3 customers' identities
            assertThat(result.getChannelIdentities()).hasSize(3);
        }

        @Test
        @DisplayName("Should return single customer when no duplicates")
        void shouldReturnSingleWhenNoDuplicates() {
            when(customerRepository.findByPhoneNumber("0909999999"))
                    .thenReturn(List.of(targetCustomer));

            Customer result = mergerService.mergeByPhoneNumber("0909999999");

            assertThat(result.getId()).isEqualTo("target-uuid-001");
            verify(customerRepository, never()).save(any());
            verify(customerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should return null when no customers with phone number")
        void shouldReturnNullWhenNoMatches() {
            when(customerRepository.findByPhoneNumber("0000000000"))
                    .thenReturn(List.of());

            Customer result = mergerService.mergeByPhoneNumber("0000000000");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should throw on null phone number")
        void shouldThrowOnNullPhone() {
            assertThatThrownBy(() -> mergerService.mergeByPhoneNumber(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Phone number is required");
        }

        @Test
        @DisplayName("Should throw on blank phone number")
        void shouldThrowOnBlankPhone() {
            assertThatThrownBy(() -> mergerService.mergeByPhoneNumber("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Phone number is required");
        }

        @Test
        @DisplayName("Should select oldest customer as target")
        void shouldSelectOldestAsTarget() {
            // Source is newer (June) than target (January)
            when(customerRepository.findByPhoneNumber("0901234567"))
                    .thenReturn(List.of(sourceCustomer, targetCustomer)); // source listed first
            when(customerRepository.findById("target-uuid-001")).thenReturn(Optional.of(targetCustomer));
            when(customerRepository.findById("source-uuid-002")).thenReturn(Optional.of(sourceCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            Customer result = mergerService.mergeByPhoneNumber("0901234567");

            // Oldest (target, Jan 2026) should be the primary even though source was listed first
            assertThat(result.getId()).isEqualTo("target-uuid-001");
            verify(customerRepository).delete(sourceCustomer);
        }
    }

    // =====================================================
    // DOMAIN MODEL: Customer.mergeWith()
    // =====================================================

    @Nested
    @DisplayName("Customer.mergeWith() domain behavior")
    class CustomerMergeWithBehavior {

        @Test
        @DisplayName("Should transfer identities and update customerId reference")
        void shouldUpdateCustomerIdOnTransferredIdentities() {
            targetCustomer.mergeWith(sourceCustomer);

            // All transferred identities should reference the target
            assertThat(targetCustomer.getChannelIdentities())
                    .filteredOn(id -> id.getPlatform() == ChannelIdentity.Platform.ZALO)
                    .allMatch(id -> id.getCustomerId().equals("target-uuid-001"));
        }

        @Test
        @DisplayName("Should clear source's identities after merge")
        void shouldClearSourceIdentities() {
            targetCustomer.mergeWith(sourceCustomer);

            assertThat(sourceCustomer.getChannelIdentities()).isEmpty();
        }

        @Test
        @DisplayName("Should handle both having null fields gracefully")
        void shouldHandleBothNullFields() {
            targetCustomer.setPhoneNumber(null);
            targetCustomer.setAddress(null);
            sourceCustomer.setPhoneNumber(null);
            sourceCustomer.setAddress(null);

            targetCustomer.mergeWith(sourceCustomer);

            assertThat(targetCustomer.getPhoneNumber()).isNull();
            assertThat(targetCustomer.getAddress()).isNull();
        }
    }
}
