package com.ebikes.notifications.support.fixtures;

import java.util.List;

import com.ebikes.notifications.database.models.TemplateVariable;
import com.ebikes.notifications.dtos.requests.templates.CreateTemplateRequest;
import com.ebikes.notifications.dtos.requests.templates.UpdateTemplateRequest;
import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.TemplateContentType;
import com.ebikes.notifications.enums.VariableType;

public final class TemplateRequestFixtures {

  private TemplateRequestFixtures() {}

  public static CreateTemplateRequest createEmailRequest(String name) {
    return new CreateTemplateRequest(
        "Hello [[${recipientName}]]",
        ChannelType.EMAIL,
        TemplateContentType.HTML,
        name,
        "Welcome to Ebikes",
        List.of(
            new TemplateVariable(
                "recipientName", VariableType.STRING, "Recipient display name", true, false)));
  }

  public static UpdateTemplateRequest updateRequest() {
    return new UpdateTemplateRequest(
        "Updated body [[${recipientName}]]",
        "Updated subject",
        List.of(
            new TemplateVariable(
                "recipientName", VariableType.STRING, "Recipient display name", true, false)));
  }
}
