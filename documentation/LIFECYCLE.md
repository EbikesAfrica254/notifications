# Notification Lifecycle

Notifications move through a defined state machine. Transitions are driven by the `MessageProcessor` pipeline and API calls.

```
PENDING       // notification created — awaiting dispatch
PROCESSING    // dispatch in progress
DELIVERED     // channel confirmed successful delivery — terminal
FAILED        // dispatch failed — eligible for retry
CANCELLED     // cancelled before delivery — terminal
```

Notifications are created via inbound `NotificationRequest` events consumed from RabbitMQ. All notifications enter the lifecycle at `PENDING`.

---

## State Transitions

| From         | To           | Trigger                                                              |
|--------------|--------------|----------------------------------------------------------------------|
| `PENDING`    | `PROCESSING` | `MessageProcessor` begins dispatch                                   |
| `PENDING`    | `CANCELLED`  | `DELETE /notifications/{id}` called                                  |
| `PROCESSING` | `DELIVERED`  | Channel confirms successful delivery                                 |
| `PROCESSING` | `FAILED`     | Channel returns an error (external, invalid recipient, or disabled)  |
| `PROCESSING` | `CANCELLED`  | `DELETE /notifications/{id}` called                                  |
| `FAILED`     | `PENDING`    | Retry scheduled — transient failures (timeout, rate limit) only      |

---

## Messaging

### Consumed Events

| Routing Key               | Effect                                          |
|---------------------------|-------------------------------------------------|
| `notifications.*`         | Creates and dispatches a notification           |

### Notes

- `DELIVERED` and `CANCELLED` are terminal — no further transitions are permitted.
- `FAILED` is only reset to `PENDING` on transient failures (timeout, rate limit). Hard failures (invalid recipient, a channel disabled) leave the notification in `FAILED`.
- Each dispatch attempt — including retries — is recorded as a separate `Delivery` row. See the delivery attempt statuses in the database schema.