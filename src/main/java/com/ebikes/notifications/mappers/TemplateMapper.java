package com.ebikes.notifications.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ebikes.notifications.database.entities.Template;
import com.ebikes.notifications.dtos.responses.templates.TemplateResponse;
import com.ebikes.notifications.dtos.responses.templates.TemplateSummaryResponse;

@Mapper(componentModel = "spring")
public interface TemplateMapper {

  @Mapping(source = "active", target = "isActive")
  TemplateResponse toResponse(Template template);

  @Mapping(source = "active", target = "isActive")
  TemplateSummaryResponse toSummaryResponse(Template template);
}
