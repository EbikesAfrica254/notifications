package com.ebikes.notifications.dtos.responses.channels.providers;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TaifaMobileResponse(
    @JsonProperty("status_code") String statusCode,
    @JsonProperty("status_desc") String statusDesc,
    @JsonProperty("message_id") String messageId,
    @JsonProperty("mobile_number") String mobileNumber,
    @JsonProperty("network_id") String networkId,
    @JsonProperty("message_cost") String messageCost,
    @JsonProperty("credit_balance") String creditBalance) {}
