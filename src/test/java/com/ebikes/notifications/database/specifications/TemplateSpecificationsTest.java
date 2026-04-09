package com.ebikes.notifications.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.database.repositories.TemplateRepository;
import com.ebikes.notifications.dtos.requests.filters.TemplateFilter;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.TemplateContentType;
import com.ebikes.notifications.support.fixtures.TemplateFixtures;
import com.ebikes.notifications.support.infrastructure.AbstractRepositoryTest;

@DisplayName("TemplateSpecifications")
class TemplateSpecificationsTest extends AbstractRepositoryTest {

  @Autowired private TemplateRepository templateRepository;

  private Template activeEmailTemplate;
  private Template activeSmsTemplate;
  private Template inactiveEmailTemplate;

  @BeforeEach
  void setUp() {
    templateRepository.deleteAll();
    activeEmailTemplate = templateRepository.save(TemplateFixtures.activeEmail("EMAIL_WELCOME"));
    activeSmsTemplate = templateRepository.save(TemplateFixtures.activeSms("SMS_ALERT"));
    inactiveEmailTemplate = templateRepository.save(TemplateFixtures.inactive("EMAIL_RESET"));
  }

  @AfterEach
  void tearDown() {
    templateRepository.deleteAll();
  }

  @Test
  @DisplayName("No filter returns all templates")
  void noFilterReturnsAll() {
    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(new TemplateFilter()));

    assertThat(results).hasSize(3);
  }

  @Test
  @DisplayName("Filter by channel EMAIL returns only email templates")
  void filterByChannelEmail() {
    TemplateFilter filter = new TemplateFilter();
    filter.setChannel(ChannelType.EMAIL);

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(2).allMatch(t -> t.getChannel() == ChannelType.EMAIL);
  }

  @Test
  @DisplayName("Filter by channel SMS returns only SMS templates")
  void filterByChannelSms() {
    TemplateFilter filter = new TemplateFilter();
    filter.setChannel(ChannelType.SMS);

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Template::getId)
        .isEqualTo(activeSmsTemplate.getId());
  }

  @Test
  @DisplayName("Filter by content type HTML returns only HTML templates")
  void filterByContentTypeHtml() {
    TemplateFilter filter = new TemplateFilter();
    filter.setTemplateContentType(TemplateContentType.HTML);

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(2)
        .allMatch(t -> t.getTemplateContentType() == TemplateContentType.HTML);
  }

  @Test
  @DisplayName("Filter by content type PLAIN_TEXT returns only plain text templates")
  void filterByContentTypePlainText() {
    TemplateFilter filter = new TemplateFilter();
    filter.setTemplateContentType(TemplateContentType.PLAIN_TEXT);

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Template::getId)
        .isEqualTo(activeSmsTemplate.getId());
  }

  @Test
  @DisplayName("Filter by isActive true returns only active templates")
  void filterByIsActiveTrue() {
    TemplateFilter filter = new TemplateFilter();
    filter.setIsActive(true);

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(2).allMatch(Template::isActive);
  }

  @Test
  @DisplayName("Filter by isActive false returns only inactive templates")
  void filterByIsActiveFalse() {
    TemplateFilter filter = new TemplateFilter();
    filter.setIsActive(false);

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Template::getId)
        .isEqualTo(inactiveEmailTemplate.getId());
  }

  @Test
  @DisplayName("Null isActive filter returns all templates")
  void nullIsActiveFilterReturnsAll() {
    TemplateFilter filter = new TemplateFilter();
    filter.setIsActive(null);

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results).hasSize(3);
  }

  @Test
  @DisplayName("Filter by exact name returns matching template")
  void filterByExactName() {
    TemplateFilter filter = new TemplateFilter();
    filter.setName("EMAIL_WELCOME");

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Template::getId)
        .isEqualTo(activeEmailTemplate.getId());
  }

  @Test
  @DisplayName("Filter by partial name returns all matching templates")
  void filterByPartialName() {
    TemplateFilter filter = new TemplateFilter();
    filter.setName("EMAIL");

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(2)
        .extracting(Template::getId)
        .contains(activeEmailTemplate.getId(), inactiveEmailTemplate.getId());
  }

  @Test
  @DisplayName("Filter by name is case-insensitive")
  void filterByNameCaseInsensitive() {
    TemplateFilter filter = new TemplateFilter();
    filter.setName("email_welcome");

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Template::getId)
        .isEqualTo(activeEmailTemplate.getId());
  }

  @Test
  @DisplayName("Combined channel and isActive filter returns correct subset")
  void combinedChannelAndIsActiveFilter() {
    TemplateFilter filter = new TemplateFilter();
    filter.setChannel(ChannelType.EMAIL);
    filter.setIsActive(true);

    List<Template> results =
        templateRepository.findAll(TemplateSpecifications.buildSpecification(filter));

    assertThat(results)
        .hasSize(1)
        .first()
        .extracting(Template::getId)
        .isEqualTo(activeEmailTemplate.getId());
  }
}
