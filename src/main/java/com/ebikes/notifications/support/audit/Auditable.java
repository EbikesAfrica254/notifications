package com.ebikes.notifications.support.audit;

import java.util.Map;

public interface Auditable {
  Map<String, String> toAuditMetadata();
}
