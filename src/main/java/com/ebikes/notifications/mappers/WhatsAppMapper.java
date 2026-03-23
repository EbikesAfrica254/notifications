package com.ebikes.notifications.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.ebikes.notifications.constants.ApplicationConstants;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppNotificationContext;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppOutboundRequest;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.WhatsAppRequest;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppAction;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppButton;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppDocument;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppInteractive;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppRow;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppSection;
import com.ebikes.notifications.dtos.requests.channels.whatsapp.components.WhatsAppText;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface WhatsAppMapper {

  default WhatsAppInteractive.Body toBody(String text) {
    return text != null ? new WhatsAppInteractive.Body(text) : null;
  }

  WhatsAppButton toButton(WhatsAppNotificationContext.ButtonContext context);

  default WhatsAppAction.Button toButton(WhatsAppButton dto) {
    return new WhatsAppAction.Button(
        ApplicationConstants.WhatsApp.BUTTON_REPLY_TYPE, dto.id(), dto.title());
  }

  default WhatsAppInteractive toButtonsInteractive(WhatsAppRequest request) {
    List<WhatsAppAction.Button> buttons = request.buttons().stream().map(this::toButton).toList();
    WhatsAppAction action = new WhatsAppAction(buttons, null, null);
    return new WhatsAppInteractive(
        ApplicationConstants.WhatsApp.INTERACTIVE_TYPE_BUTTON,
        null,
        toBody(request.body()),
        toFooter(request.footer()),
        action);
  }

  @Mapping(target = "messagingProduct", constant = ApplicationConstants.WhatsApp.MESSAGING_PRODUCT)
  @Mapping(target = "type", constant = ApplicationConstants.WhatsApp.MESSAGE_TYPE_INTERACTIVE)
  @Mapping(target = "interactive", expression = "java(toButtonsInteractive(request))")
  @Mapping(target = "text", ignore = true)
  @Mapping(target = "document", ignore = true)
  WhatsAppOutboundRequest toButtonsRequest(WhatsAppRequest request);

  default WhatsAppDocument toDocumentPayload(WhatsAppRequest request) {
    return new WhatsAppDocument(request.documentUrl(), request.filename(), request.caption());
  }

  @Mapping(target = "messagingProduct", constant = ApplicationConstants.WhatsApp.MESSAGING_PRODUCT)
  @Mapping(target = "type", constant = ApplicationConstants.WhatsApp.MESSAGE_TYPE_DOCUMENT)
  @Mapping(target = "document", expression = "java(toDocumentPayload(request))")
  @Mapping(target = "text", ignore = true)
  @Mapping(target = "interactive", ignore = true)
  WhatsAppOutboundRequest toDocumentRequest(WhatsAppRequest request);

  default WhatsAppInteractive.Footer toFooter(String text) {
    return text != null ? new WhatsAppInteractive.Footer(text) : null;
  }

  default WhatsAppInteractive toListInteractive(WhatsAppRequest request) {
    List<WhatsAppAction.Section> sections =
        request.sections().stream().map(this::toSection).toList();
    WhatsAppAction action = new WhatsAppAction(null, request.buttonText(), sections);
    return new WhatsAppInteractive(
        ApplicationConstants.WhatsApp.INTERACTIVE_TYPE_LIST,
        null,
        toBody(request.body()),
        toFooter(request.footer()),
        action);
  }

  @Mapping(target = "messagingProduct", constant = ApplicationConstants.WhatsApp.MESSAGING_PRODUCT)
  @Mapping(target = "type", constant = ApplicationConstants.WhatsApp.MESSAGE_TYPE_INTERACTIVE)
  @Mapping(target = "interactive", expression = "java(toListInteractive(request))")
  @Mapping(target = "text", ignore = true)
  @Mapping(target = "document", ignore = true)
  WhatsAppOutboundRequest toListRequest(WhatsAppRequest request);

  default WhatsAppOutboundRequest toOutboundRequest(WhatsAppRequest request) {
    return switch (request.messageType()) {
      case TEXT -> toTextRequest(request);
      case BUTTONS -> toButtonsRequest(request);
      case LIST -> toListRequest(request);
      case DOCUMENT -> toDocumentRequest(request);
    };
  }

  @Mapping(target = "to", source = "recipient")
  @Mapping(
      target = "messageType",
      expression = "java(WhatsAppMessageType.fromString(context.messageType()))")
  @Mapping(target = "body", source = "context.body")
  @Mapping(target = "buttonText", source = "context.buttonText")
  @Mapping(target = "buttons", source = "context.buttons")
  @Mapping(target = "caption", source = "context.caption")
  @Mapping(target = "documentUrl", source = "context.documentUrl")
  @Mapping(target = "filename", source = "context.filename")
  @Mapping(target = "footer", source = "context.footer")
  @Mapping(target = "previewUrl", source = "context.previewUrl")
  @Mapping(target = "sections", source = "context.sections")
  WhatsAppRequest toRequest(WhatsAppNotificationContext context, String recipient);

  WhatsAppRow toRow(WhatsAppNotificationContext.RowContext context);

  default WhatsAppAction.Row toRow(WhatsAppRow dto) {
    return new WhatsAppAction.Row(dto.id(), dto.title(), dto.description());
  }

  WhatsAppSection toSection(WhatsAppNotificationContext.SectionContext context);

  default WhatsAppAction.Section toSection(WhatsAppSection dto) {
    List<WhatsAppAction.Row> rows = dto.rows().stream().map(this::toRow).toList();
    return new WhatsAppAction.Section(dto.title(), rows);
  }

  default WhatsAppText toText(WhatsAppRequest request) {
    return new WhatsAppText(request.body(), request.previewUrl());
  }

  @Mapping(target = "messagingProduct", constant = ApplicationConstants.WhatsApp.MESSAGING_PRODUCT)
  @Mapping(target = "type", constant = ApplicationConstants.WhatsApp.MESSAGE_TYPE_TEXT)
  @Mapping(target = "text", expression = "java(toText(request))")
  @Mapping(target = "interactive", ignore = true)
  @Mapping(target = "document", ignore = true)
  WhatsAppOutboundRequest toTextRequest(WhatsAppRequest request);
}
