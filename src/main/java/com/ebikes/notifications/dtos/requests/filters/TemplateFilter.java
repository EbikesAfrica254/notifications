package com.ebikes.notifications.dtos.requests.filters;

import com.ebikes.notifications.enums.ChannelType;
import com.ebikes.notifications.enums.ContentType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TemplateFilter extends BaseFilter {
  private ChannelType channel;
  private ContentType contentType;
  private Boolean isActive;
  private String name;
}
