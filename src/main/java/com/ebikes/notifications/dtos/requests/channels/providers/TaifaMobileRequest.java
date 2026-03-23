package com.ebikes.notifications.dtos.requests.channels.providers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaifaMobileRequest(
    String mobile,
    @JsonProperty("response_type") String responseType,
    @JsonProperty("sender_name") String senderName,
    @JsonProperty("service_id") int serviceId,
    String message,
    @JsonProperty("link_id") String linkId) {}
