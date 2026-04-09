package com.ebikes.notifications.database.specifications;

import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_ORGANIZATION_ID;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.TEST_USER_ID;
import static com.ebikes.notifications.support.fixtures.SecurityFixtures.setExecutionContext;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.notifications.database.entities.UserChannelPreference;
import com.ebikes.notifications.database.repositories.UserPreferenceRepository;
import com.ebikes.notifications.dtos.requests.filters.ChannelPreferenceFilter;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.NotificationCategory;
import com.ebikes.notifications.enums.UserRole;
import com.ebikes.notifications.support.context.ExecutionContext;
import com.ebikes.notifications.support.fixtures.UserPreferenceFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractRepositoryTest;

@DisplayName("UserPreferenceSpecifications")
class UserPreferenceSpecificationsTest extends AbstractRepositoryTest {

  @Autowired private UserPreferenceRepository userPreferenceRepository;

  private UserChannelPreference emailTransactionalEnabled;
  private UserChannelPreference smsTransactionalEnabled;
  private UserChannelPreference emailMarketingDisabled;

  @BeforeEach
  void setUp() {
    userPreferenceRepository.deleteAll();
    emailTransactionalEnabled =
        userPreferenceRepository.save(
            UserPreferenceFixtures.enabled(
                TEST_USER_ID,
                TEST_ORGANIZATION_ID,
                ChannelType.EMAIL,
                NotificationCategory.TRANSACTIONAL));
    smsTransactionalEnabled =
        userPreferenceRepository.save(
            UserPreferenceFixtures.enabled(
                TEST_USER_ID,
                TEST_ORGANIZATION_ID,
                ChannelType.SMS,
                NotificationCategory.TRANSACTIONAL));
    emailMarketingDisabled =
        userPreferenceRepository.save(
            UserPreferenceFixtures.disabled(
                TEST_USER_ID,
                TEST_ORGANIZATION_ID,
                ChannelType.EMAIL,
                NotificationCategory.MARKETING));

    setExecutionContext(TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
  }

  @AfterEach
  void tearDown() {
    userPreferenceRepository.deleteAll();
    ExecutionContext.clear();
  }

  @Test
  @DisplayName("No filter returns all preferences for active organisation")
  void noFilterReturnsAll() {
    List<UserChannelPreference> results =
        userPreferenceRepository.findAll(
            UserPreferenceSpecifications.buildSpecification(new ChannelPreferenceFilter()));

    assertThat(results).hasSize(3);
  }

  @Test
  @DisplayName("Filter by channel EMAIL returns only email preferences")
  void filterByChannelEmail() {
    ChannelPreferenceFilter filter = new ChannelPreferenceFilter();
    filter.setChannel(ChannelType.EMAIL);

    List<UserChannelPreference> results =
        userPreferenceRepository.findAll(UserPreferenceSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(2).allMatch(p -> p.getChannel() == ChannelType.EMAIL);
  }

  @Test
  @DisplayName("Filter by channel SMS returns only SMS preferences")
  void filterByChannelSms() {
    ChannelPreferenceFilter filter = new ChannelPreferenceFilter();
    filter.setChannel(ChannelType.SMS);

    List<UserChannelPreference> results =
        userPreferenceRepository.findAll(UserPreferenceSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(UserChannelPreference::getId)
        .isEqualTo(smsTransactionalEnabled.getId());
  }

  @Test
  @DisplayName("Filter by category TRANSACTIONAL returns only transactional preferences")
  void filterByCategoryTransactional() {
    ChannelPreferenceFilter filter = new ChannelPreferenceFilter();
    filter.setCategory(NotificationCategory.TRANSACTIONAL);

    List<UserChannelPreference> results =
        userPreferenceRepository.findAll(UserPreferenceSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(2)
        .allMatch(p -> p.getCategory() == NotificationCategory.TRANSACTIONAL);
  }

  @Test
  @DisplayName("Filter by category MARKETING returns only marketing preferences")
  void filterByCategoryMarketing() {
    ChannelPreferenceFilter filter = new ChannelPreferenceFilter();
    filter.setCategory(NotificationCategory.MARKETING);

    List<UserChannelPreference> results =
        userPreferenceRepository.findAll(UserPreferenceSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(UserChannelPreference::getId)
        .isEqualTo(emailMarketingDisabled.getId());
  }

  @Test
  @DisplayName("Filter by enabled true returns only enabled preferences")
  void filterByEnabledTrue() {
    ChannelPreferenceFilter filter = new ChannelPreferenceFilter();
    filter.setEnabled(true);

    List<UserChannelPreference> results =
        userPreferenceRepository.findAll(UserPreferenceSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(2).allMatch(UserChannelPreference::getEnabled);
  }

  @Test
  @DisplayName("Filter by enabled false returns only disabled preferences")
  void filterByEnabledFalse() {
    ChannelPreferenceFilter filter = new ChannelPreferenceFilter();
    filter.setEnabled(false);

    List<UserChannelPreference> results =
        userPreferenceRepository.findAll(UserPreferenceSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(UserChannelPreference::getId)
        .isEqualTo(emailMarketingDisabled.getId());
  }

  @Test
  @DisplayName("Null enabled filter returns all preferences")
  void nullEnabledReturnsAll() {
    ChannelPreferenceFilter filter = new ChannelPreferenceFilter();
    filter.setEnabled(null);

    List<UserChannelPreference> results =
        userPreferenceRepository.findAll(UserPreferenceSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(3);
  }

  @Test
  @DisplayName("Combined channel EMAIL and enabled true returns correct preference")
  void combinedChannelAndEnabledTrue() {
    ChannelPreferenceFilter filter = new ChannelPreferenceFilter();
    filter.setChannel(ChannelType.EMAIL);
    filter.setEnabled(true);

    List<UserChannelPreference> results =
        userPreferenceRepository.findAll(UserPreferenceSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(UserChannelPreference::getId)
        .isEqualTo(emailTransactionalEnabled.getId());
  }
}
