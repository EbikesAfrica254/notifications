package com.ebikes.notifications.database.specifications;

import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_EMAIL;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_ORGANIZATION_ID;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_PHONE_NUMBER;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_USER_ID;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.setExecutionContext;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.setExecutionContextWithRecipients;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.notifications.database.entities.Delivery;
import com.ebikes.notifications.database.entities.Notification;
import com.ebikes.notifications.database.entities.OrganizationChannelPreference;
import com.ebikes.notifications.database.entities.UserChannelPreference;
import com.ebikes.notifications.database.repositories.DeliveryRepository;
import com.ebikes.notifications.database.repositories.NotificationRepository;
import com.ebikes.notifications.database.repositories.OrganizationPreferenceRepository;
import com.ebikes.notifications.database.repositories.UserPreferenceRepository;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.enums.UserRole;
import com.ebikes.notifications.exceptions.AuthorizationException;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.fixtures.DeliveryFixtures;
import com.ebikes.notifications.support.fixtures.NotificationFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractRepositoryTest;

@DisplayName("AuthorizationSpecifications")
class AuthorizationSpecificationsTest extends AbstractRepositoryTest {

  @Autowired private DeliveryRepository deliveryRepository;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private OrganizationPreferenceRepository organizationPreferenceRepository;
  @Autowired private UserPreferenceRepository userPreferenceRepository;

  private static final String TEST_BRANCH_ID = "00000000-0000-0000-0000-000000000003";
  private static final String OTHER_BRANCH_ID = "00000000-0000-0000-0000-000000000098";
  public static final String OTHER_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000099";
  public static final String OTHER_USER_ID = "00000000-0000-0000-0000-000000000097";
  public static final String OTHER_EMAIL = "other@other.test";

  @Nested
  @DisplayName("forDeliveries()")
  class ForDeliveries {

    private Delivery ownDelivery;
    private Delivery otherDelivery;

    @BeforeEach
    void setUp() {
      Notification ownNotification =
          notificationRepository.save(
              NotificationFixtures.forOrganizationAndBranch(
                  TEST_ORGANIZATION_ID, TEST_BRANCH_ID, TEST_EMAIL));

      Notification otherNotification =
          notificationRepository.save(
              NotificationFixtures.forOrganizationAndBranch(
                  OTHER_ORGANIZATION_ID, OTHER_BRANCH_ID, OTHER_EMAIL));

      ownDelivery =
          deliveryRepository.save(DeliveryFixtures.pending(ownNotification, TEST_ORGANIZATION_ID));
      otherDelivery =
          deliveryRepository.save(
              DeliveryFixtures.pending(otherNotification, OTHER_ORGANIZATION_ID));
    }

    @AfterEach
    void tearDown() {
      deliveryRepository.deleteAll();
      notificationRepository.deleteAll();
      ExecutionContext.clear();
    }

    @Test
    @DisplayName("SYSTEM_ADMIN returns all deliveries across organisations")
    void systemAdminReturnsAll() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);

      List<Delivery> results =
          deliveryRepository.findAll(AuthorizationSpecifications.forDeliveries());

      assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN returns only deliveries for active organisation")
    void organizationAdminReturnsOwnOrgOnly() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      List<Delivery> results =
          deliveryRepository.findAll(AuthorizationSpecifications.forDeliveries());

      assertThat(results)
          .hasSize(1)
          .first()
          .extracting(Delivery::getId)
          .isEqualTo(ownDelivery.getId());
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN does not return deliveries from another organisation")
    void organizationAdminExcludesOtherOrg() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      List<Delivery> results =
          deliveryRepository.findAll(AuthorizationSpecifications.forDeliveries());

      assertThat(results).isNotEmpty();
      assertThat(results).extracting(Delivery::getId).doesNotContain(otherDelivery.getId());
    }

    @Test
    @DisplayName("BRANCH_ADMIN returns only deliveries where notification.branchId matches")
    void branchAdminReturnsBranchOnly() {
      setExecutionContext(TEST_ORGANIZATION_ID, TEST_BRANCH_ID, UserRole.BRANCH_ADMIN);

      List<Delivery> results =
          deliveryRepository.findAll(AuthorizationSpecifications.forDeliveries());

      assertThat(results)
          .hasSize(1)
          .first()
          .extracting(Delivery::getId)
          .isEqualTo(ownDelivery.getId());
    }

    @Test
    @DisplayName("BRANCH_ADMIN excludes deliveries from another branch")
    void branchAdminExcludesOtherBranch() {
      setExecutionContext(TEST_ORGANIZATION_ID, TEST_BRANCH_ID, UserRole.BRANCH_ADMIN);

      List<Delivery> results =
          deliveryRepository.findAll(AuthorizationSpecifications.forDeliveries());

      assertThat(results).isNotEmpty();
      assertThat(results).extracting(Delivery::getId).doesNotContain(otherDelivery.getId());
    }

    @Test
    @DisplayName("USER returns only deliveries where notification.recipient matches email")
    void userReturnsOwnByEmail() {
      setExecutionContextWithRecipients(TEST_ORGANIZATION_ID, TEST_EMAIL, null, UserRole.CUSTOMER);

      List<Delivery> results =
          deliveryRepository.findAll(AuthorizationSpecifications.forDeliveries());

      assertThat(results)
          .hasSize(1)
          .first()
          .extracting(Delivery::getId)
          .isEqualTo(ownDelivery.getId());
    }

    @Test
    @DisplayName("USER with both email and phone matches either recipient via OR")
    void userWithEmailAndPhoneUsesOrCondition() {
      Notification phoneNotification =
          notificationRepository.save(
              NotificationFixtures.forOrganizationAndBranch(
                  TEST_ORGANIZATION_ID, TEST_BRANCH_ID, TEST_PHONE_NUMBER));
      Delivery phoneDelivery =
          deliveryRepository.save(
              DeliveryFixtures.pending(phoneNotification, TEST_ORGANIZATION_ID));

      setExecutionContextWithRecipients(
          TEST_ORGANIZATION_ID, TEST_EMAIL, TEST_PHONE_NUMBER, UserRole.CUSTOMER);

      List<Delivery> results =
          deliveryRepository.findAll(AuthorizationSpecifications.forDeliveries());

      assertThat(results)
          .extracting(Delivery::getId)
          .contains(ownDelivery.getId(), phoneDelivery.getId());
    }

    @Test
    @DisplayName("Missing activeOrganization for non-SYSTEM_ADMIN throws AuthorizationException")
    void missingActiveOrganizationThrows() {
      setExecutionContext(null, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(
              () -> deliveryRepository.findAll(AuthorizationSpecifications.forDeliveries()))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("Missing activeBranch for branch role throws AuthorizationException")
    void missingActiveBranchThrows() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.BRANCH_ADMIN);

      assertThatThrownBy(
              () -> deliveryRepository.findAll(AuthorizationSpecifications.forDeliveries()))
          .isInstanceOf(AuthorizationException.class);
    }
  }

  @Nested
  @DisplayName("forNotifications()")
  class ForNotifications {

    private Notification ownNotification;
    private Notification otherNotification;

    @BeforeEach
    void setUp() {
      ownNotification =
          notificationRepository.save(
              NotificationFixtures.forOrganizationAndBranch(
                  TEST_ORGANIZATION_ID, TEST_BRANCH_ID, TEST_EMAIL));

      otherNotification =
          notificationRepository.save(
              NotificationFixtures.forOrganizationAndBranch(
                  OTHER_ORGANIZATION_ID, OTHER_BRANCH_ID, OTHER_EMAIL));
    }

    @AfterEach
    void tearDown() {
      notificationRepository.deleteAll();
      ExecutionContext.clear();
    }

    @Test
    @DisplayName("SYSTEM_ADMIN returns all notifications")
    void systemAdminReturnsAll() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);

      List<Notification> results =
          notificationRepository.findAll(AuthorizationSpecifications.forNotifications());

      assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN returns only notifications for active organisation")
    void organizationAdminReturnsOwnOrgOnly() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      List<Notification> results =
          notificationRepository.findAll(AuthorizationSpecifications.forNotifications());

      assertThat(results)
          .hasSize(1)
          .first()
          .extracting(Notification::getId)
          .isEqualTo(ownNotification.getId());
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN does not return notifications from another organisation")
    void organizationAdminExcludesOtherOrg() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      List<Notification> results =
          notificationRepository.findAll(AuthorizationSpecifications.forNotifications());

      assertThat(results).isNotEmpty();
      assertThat(results).extracting(Notification::getId).doesNotContain(otherNotification.getId());
    }

    @Test
    @DisplayName("BRANCH_ADMIN returns only notifications where branchId matches")
    void branchAdminReturnsBranchOnly() {
      setExecutionContext(TEST_ORGANIZATION_ID, TEST_BRANCH_ID, UserRole.BRANCH_ADMIN);

      List<Notification> results =
          notificationRepository.findAll(AuthorizationSpecifications.forNotifications());

      assertThat(results)
          .hasSize(1)
          .first()
          .extracting(Notification::getId)
          .isEqualTo(ownNotification.getId());
    }

    @Test
    @DisplayName("BRANCH_ADMIN excludes notifications from another branch")
    void branchAdminExcludesOtherBranch() {
      setExecutionContext(TEST_ORGANIZATION_ID, TEST_BRANCH_ID, UserRole.BRANCH_ADMIN);

      List<Notification> results =
          notificationRepository.findAll(AuthorizationSpecifications.forNotifications());

      assertThat(results).isNotEmpty();
      assertThat(results).extracting(Notification::getId).doesNotContain(otherNotification.getId());
    }

    @Test
    @DisplayName("USER returns only notifications where recipient matches email")
    void userReturnsOwnByRecipient() {
      setExecutionContextWithRecipients(TEST_ORGANIZATION_ID, TEST_EMAIL, null, UserRole.CUSTOMER);

      List<Notification> results =
          notificationRepository.findAll(AuthorizationSpecifications.forNotifications());

      assertThat(results)
          .hasSize(1)
          .first()
          .extracting(Notification::getId)
          .isEqualTo(ownNotification.getId());
    }

    @Test
    @DisplayName("USER with both email and phone matches either recipient via OR")
    void userWithEmailAndPhoneUsesOrCondition() {
      Notification phoneNotification =
          notificationRepository.save(
              NotificationFixtures.forOrganizationAndBranch(
                  TEST_ORGANIZATION_ID, TEST_BRANCH_ID, TEST_PHONE_NUMBER));

      setExecutionContextWithRecipients(
          TEST_ORGANIZATION_ID, TEST_EMAIL, TEST_PHONE_NUMBER, UserRole.CUSTOMER);

      List<Notification> results =
          notificationRepository.findAll(AuthorizationSpecifications.forNotifications());

      assertThat(results)
          .extracting(Notification::getId)
          .contains(ownNotification.getId(), phoneNotification.getId());
    }

    @Test
    @DisplayName("Missing activeOrganization throws AuthorizationException")
    void missingActiveOrganizationThrows() {
      setExecutionContext(null, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(
              () -> notificationRepository.findAll(AuthorizationSpecifications.forNotifications()))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("Missing activeBranch for branch role throws AuthorizationException")
    void missingActiveBranchThrows() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.BRANCH_ADMIN);

      assertThatThrownBy(
              () -> notificationRepository.findAll(AuthorizationSpecifications.forNotifications()))
          .isInstanceOf(AuthorizationException.class);
    }
  }

  @Nested
  @DisplayName("forOrganizationPreferences()")
  class ForOrganizationPreferences {

    private OrganizationChannelPreference ownPref;
    private OrganizationChannelPreference otherPref;

    @BeforeEach
    void setUp() {
      ownPref =
          organizationPreferenceRepository.save(
              OrganizationChannelPreference.builder()
                  .organizationId(TEST_ORGANIZATION_ID)
                  .channel(ChannelType.EMAIL)
                  .category(NotificationCategory.TRANSACTIONAL)
                  .enabled(true)
                  .build());

      otherPref =
          organizationPreferenceRepository.save(
              OrganizationChannelPreference.builder()
                  .organizationId(OTHER_ORGANIZATION_ID)
                  .channel(ChannelType.EMAIL)
                  .category(NotificationCategory.TRANSACTIONAL)
                  .enabled(true)
                  .build());
    }

    @AfterEach
    void tearDown() {
      organizationPreferenceRepository.deleteAll();
      ExecutionContext.clear();
    }

    @Test
    @DisplayName("SYSTEM_ADMIN returns all organisation preferences")
    void systemAdminReturnsAll() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);

      List<OrganizationChannelPreference> results =
          organizationPreferenceRepository.findAll(
              AuthorizationSpecifications.forOrganizationPreferences());

      assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN returns only preferences for active organisation")
    void organizationAdminReturnsOwnOnly() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      List<OrganizationChannelPreference> results =
          organizationPreferenceRepository.findAll(
              AuthorizationSpecifications.forOrganizationPreferences());

      assertThat(results)
          .hasSize(1)
          .first()
          .extracting(OrganizationChannelPreference::getId)
          .isEqualTo(ownPref.getId());
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN does not return preferences from another organisation")
    void organizationAdminExcludesOtherOrg() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      List<OrganizationChannelPreference> results =
          organizationPreferenceRepository.findAll(
              AuthorizationSpecifications.forOrganizationPreferences());

      assertThat(results).isNotEmpty();
      assertThat(results)
          .extracting(OrganizationChannelPreference::getId)
          .doesNotContain(otherPref.getId());
    }

    @Test
    @DisplayName("Missing activeOrganization throws AuthorizationException")
    void missingActiveOrganizationThrows() {
      setExecutionContext(null, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(
              () ->
                  organizationPreferenceRepository.findAll(
                      AuthorizationSpecifications.forOrganizationPreferences()))
          .isInstanceOf(AuthorizationException.class);
    }
  }

  @Nested
  @DisplayName("forUserPreferences()")
  class ForUserPreferences {

    private UserChannelPreference ownEmailPref;
    private UserChannelPreference ownSmsPref;
    private UserChannelPreference otherPref;

    @BeforeEach
    void setUp() {
      ownEmailPref =
          userPreferenceRepository.save(
              UserChannelPreference.builder()
                  .userId(TEST_USER_ID)
                  .organizationId(TEST_ORGANIZATION_ID)
                  .channel(ChannelType.EMAIL)
                  .category(NotificationCategory.TRANSACTIONAL)
                  .enabled(true)
                  .build());

      ownSmsPref =
          userPreferenceRepository.save(
              UserChannelPreference.builder()
                  .userId(TEST_USER_ID)
                  .organizationId(TEST_ORGANIZATION_ID)
                  .channel(ChannelType.SMS)
                  .category(NotificationCategory.TRANSACTIONAL)
                  .enabled(true)
                  .build());

      otherPref =
          userPreferenceRepository.save(
              UserChannelPreference.builder()
                  .userId(OTHER_USER_ID)
                  .organizationId(OTHER_ORGANIZATION_ID)
                  .channel(ChannelType.EMAIL)
                  .category(NotificationCategory.TRANSACTIONAL)
                  .enabled(true)
                  .build());
    }

    @AfterEach
    void tearDown() {
      userPreferenceRepository.deleteAll();
      ExecutionContext.clear();
    }

    @Test
    @DisplayName("SYSTEM_ADMIN returns all user preferences")
    void systemAdminReturnsAll() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);

      List<UserChannelPreference> results =
          userPreferenceRepository.findAll(AuthorizationSpecifications.forUserPreferences());

      assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN returns only preferences for active organisation")
    void organizationAdminReturnsOwnOrgOnly() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      List<UserChannelPreference> results =
          userPreferenceRepository.findAll(AuthorizationSpecifications.forUserPreferences());

      assertThat(results)
          .hasSize(2)
          .extracting(UserChannelPreference::getId)
          .contains(ownEmailPref.getId(), ownSmsPref.getId());
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN does not return preferences from another organisation")
    void organizationAdminExcludesOtherOrg() {
      setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      List<UserChannelPreference> results =
          userPreferenceRepository.findAll(AuthorizationSpecifications.forUserPreferences());

      assertThat(results).isNotEmpty();
      assertThat(results)
          .extracting(UserChannelPreference::getId)
          .doesNotContain(otherPref.getId());
    }

    @Test
    @DisplayName("CUSTOMER returns only preferences for own userId")
    void customerReturnsOwnByUserId() {
      setExecutionContextWithRecipients(
          TEST_ORGANIZATION_ID, TEST_EMAIL, TEST_PHONE_NUMBER, UserRole.CUSTOMER);

      List<UserChannelPreference> results =
          userPreferenceRepository.findAll(AuthorizationSpecifications.forUserPreferences());

      assertThat(results)
          .hasSize(2)
          .extracting(UserChannelPreference::getId)
          .contains(ownEmailPref.getId(), ownSmsPref.getId());
    }

    @Test
    @DisplayName("Missing activeOrganization throws AuthorizationException")
    void missingActiveOrganizationThrows() {
      setExecutionContext(null, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(
              () ->
                  userPreferenceRepository.findAll(
                      AuthorizationSpecifications.forUserPreferences()))
          .isInstanceOf(AuthorizationException.class);
    }
  }
}
