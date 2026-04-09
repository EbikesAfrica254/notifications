package com.ebikes.notifications.support.fixtures;

import java.util.List;

import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.database.models.TemplateVariable;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.TemplateContentType;
import com.ebikes.notifications.enums.VariableType;

public final class TemplateFixtures {

  private TemplateFixtures() {}

  public static Template activeEmail(String name) {
    return base(name, ChannelType.EMAIL, TemplateContentType.HTML, "Test subject", true).build();
  }

  public static Template activeSms(String name) {
    return base(name, ChannelType.SMS, TemplateContentType.PLAIN_TEXT, null, true).build();
  }

  public static Template inactive(String name) {
    return base(name, ChannelType.EMAIL, TemplateContentType.HTML, "Test subject", false).build();
  }

  private static Template.TemplateBuilder<?, ?> base(
      String name,
      ChannelType channel,
      TemplateContentType contentType,
      String subject,
      boolean isActive) {
    return Template.builder()
        .name(name)
        .channel(channel)
        .templateContentType(contentType)
        .bodyTemplate("Test body template")
        .subject(subject)
        .isActive(isActive)
        .variableDefinitions(
            List.of(
                new TemplateVariable(
                    "recipientName", VariableType.STRING, "Recipient name", true, false)));
  }
}
