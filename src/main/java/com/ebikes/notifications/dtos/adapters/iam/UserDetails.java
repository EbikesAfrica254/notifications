package com.ebikes.notifications.dtos.adapters.iam;

public record UserDetails(
    String id, String keycloakUserId, String firstName, String lastName, String username) {

  public String displayName() {
    if (firstName != null && lastName != null) {
      return (firstName + " " + lastName).trim();
    }
    return username;
  }
}
