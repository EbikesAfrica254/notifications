package com.ebikes.notifications.database.specifications;

import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_EMAIL;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_ORGANIZATION_ID;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_PHONE_NUMBER;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.setExecutionContext;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.repositories.NotificationRepository;
import com.ebikes.notifications.dtos.requests.filters.NotificationFilter;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationStatus;
import com.ebikes.notifications.enums.UserRole;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractRepositoryTest;

@DisplayName("NotificationSpecifications")
class NotificationSpecificationsTest extends AbstractRepositoryTest {

  private static final String TEST_BRANCH_ID = "00000000-0000-0000-0000-000000000003";
  private static final String OTHER_BRANCH_ID = "00000000-0000-0000-0000-000000000098";

  @Autowired private NotificationRepository notificationRepository;

  private Notification emailPending;
  private Notification smsPending;
  private Notification emailDelivered;

  @BeforeEach
  void setUp() {
    emailPending =
        notificationRepository.save(
            NotificationFixtures.forOrganizationAndBranch(
                TEST_ORGANIZATION_ID, TEST_BRANCH_ID, TEST_EMAIL));

    smsPending =
        notificationRepository.save(
            NotificationFixtures.forOrganizationAndBranch(
                TEST_ORGANIZATION_ID, TEST_BRANCH_ID, TEST_PHONE_NUMBER, ChannelType.SMS));

    Notification n =
        NotificationFixtures.forOrganizationAndBranch(
            TEST_ORGANIZATION_ID, OTHER_BRANCH_ID, TEST_EMAIL);
    n.markProcessing();
    n.markDelivered();
    emailDelivered = notificationRepository.save(n);

    setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
  }

  @AfterEach
  void tearDown() {
    notificationRepository.deleteAll();
    ExecutionContext.clear();
  }

  @Test
  @DisplayName("No filter returns all notifications for active organisation")
  void noFilterReturnsAll() {
    NotificationFilter filter = new NotificationFilter();

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(3);
  }

  @Test
  @DisplayName("Filter by status PENDING returns only pending notifications")
  void filterByStatusPending() {
    NotificationFilter filter = new NotificationFilter();
    filter.setStatus(NotificationStatus.PENDING);

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(2).allMatch(n -> n.getStatus() == NotificationStatus.PENDING);
  }

  @Test
  @DisplayName("Filter by status DELIVERED returns only delivered notifications")
  void filterByStatusDelivered() {
    NotificationFilter filter = new NotificationFilter();
    filter.setStatus(NotificationStatus.DELIVERED);

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Notification::getId)
        .isEqualTo(emailDelivered.getId());
  }

  @Test
  @DisplayName("Filter by channel EMAIL returns only email notifications")
  void filterByChannelEmail() {
    NotificationFilter filter = new NotificationFilter();
    filter.setChannel(ChannelType.EMAIL);

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(2).allMatch(n -> n.getChannel() == ChannelType.EMAIL);
  }

  @Test
  @DisplayName("Filter by channel SMS returns only SMS notifications")
  void filterByChannelSms() {
    NotificationFilter filter = new NotificationFilter();
    filter.setChannel(ChannelType.SMS);

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Notification::getId)
        .isEqualTo(smsPending.getId());
  }

  @Test
  @DisplayName("Filter by branchId returns only notifications for that branch")
  void filterByBranchId() {
    NotificationFilter filter = new NotificationFilter();
    filter.setBranchId(TEST_BRANCH_ID);

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(2)
        .extracting(Notification::getId)
        .contains(emailPending.getId(), smsPending.getId());
  }

  @Test
  @DisplayName("Filter by branchId excludes notifications from another branch")
  void filterByBranchIdExcludesOtherBranch() {
    NotificationFilter filter = new NotificationFilter();
    filter.setBranchId(OTHER_BRANCH_ID);

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Notification::getId)
        .isEqualTo(emailDelivered.getId());
  }

  @Test
  @DisplayName("Filter by recipient returns only notifications for that recipient")
  void filterByRecipient() {
    NotificationFilter filter = new NotificationFilter();
    filter.setRecipient(TEST_EMAIL);

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(2).allMatch(n -> n.getRecipient().equals(TEST_EMAIL));
  }

  @Test
  @DisplayName("Filter by recipient phone returns only SMS notification")
  void filterByRecipientPhone() {
    NotificationFilter filter = new NotificationFilter();
    filter.setRecipient(TEST_PHONE_NUMBER);

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Notification::getId)
        .isEqualTo(smsPending.getId());
  }

  @Test
  @DisplayName("createdAtFrom in the future returns empty")
  void createdAtFromFutureReturnsEmpty() {
    NotificationFilter filter = new NotificationFilter();
    filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("createdAtTo in the past returns empty")
  void createdAtToPastReturnsEmpty() {
    NotificationFilter filter = new NotificationFilter();
    filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("createdAtFrom and createdAtTo spanning now returns all notifications")
  void createdAtRangeSpanningNowReturnsAll() {
    NotificationFilter filter = new NotificationFilter();
    filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
    filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(3);
  }

  @Test
  @DisplayName("Combined channel and status filter returns correct subset")
  void combinedChannelAndStatusFilter() {
    NotificationFilter filter = new NotificationFilter();
    filter.setChannel(ChannelType.EMAIL);
    filter.setStatus(NotificationStatus.PENDING);

    List<Notification> results =
        notificationRepository.findAll(NotificationSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Notification::getId)
        .isEqualTo(emailPending.getId());
  }
}
