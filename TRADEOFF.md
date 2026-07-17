# Technical Tradeoffs & Design Decisions

## Idempotency Handling
A single idempotency key is used to deduplicate requests. In this design, if the same key is sent with different parameters, the response for the first is always returned. This approach guards against duplicate payments but can mask accidental parameter variation. Simplicity and speed in deduplication are prioritized over strict safety; without parameter hashing, there is a risk of silent failures if request parameters are mismatched. For stricter enforcement, hashing request parameters with the idempotency key could help quickly detect mismatches and prevent subtle bugs. 

## Card Data in Idempotency Table
Currently, card number and CVV are stored in plaintext in the idempotency table to simplify implementation and comparison for request deduplication. However, this approach is not secure for production or PCI compliance. Development speed and straightforward logic are provided at the expense of security. Real deployments should never store sensitive card information directly, instead relying on vaulting solutions or tokenization.

## External Bank Mutations & Synchronous Transactions
Bank API calls are made inside the same atomic transaction as local state mutation, ensuring that if the bank call fails, local state is rolled back. A short timeout is used for these synchronous operations. The gateway’s interface is synchronous, requiring clients to receive a final result on each request. This constraint prevents the adoption of a true async outbox pattern, which would otherwise decouple user API calls from external bank operations. Simplicity for API clients and atomicity for local state are gained, but this comes at the expense of ultimate resilience and failure isolation. If gateway endpoints can be made asynchronous, or if webhooks/polling callbacks are added, the outbox pattern and background mutation would enable more robust resilience and error recovery, as described in the [job-drain pattern](https://brandur.org/job-drain).

## Retries, Backoff, and Jitter
All bank and upstream API calls only retry transient network or server errors and use exponential backoff with jitter, following [AWS best practices](https://builder.aws.com/content/3EumjoZascWd1oZiEgL8ORlv3qE/timeouts-retries-and-backoff-with-jitter). This policy prevents thundering herd or retry storms if multiple requests encounter issues simultaneously. Only temporary glitches are automatically retried; permanent errors fail fast. This improves stability during brief outages but introduces complexity in client logic and timeout/error classification, and some failures may require manual intervention.

## Recovery Points with Directed Acyclic Graph (DAG)
Each externally mutative endpoint operation creates a distinct recovery point using a DAG-based pattern. This ensures that for each failed or incomplete request, system state is managed clearly and retries only redo incomplete work, improving auditability and recovery. Employing this pattern introduces complexity in state management and transaction logic, trading simplicity for more robust failure handling and preventing partial failure replay, in line with modern resilience practices.

## Merchant Registration, API Keys, and Customer Data
Gateway design requires merchants to register and receive an API key, omitting a separate API secret phase for simplicity. The API key is the sole credential for merchant authentication and request attribution. Payments include merchant-supplied customerId and customerOrderId, which are used primarily to support queries rather than core processing. If not for the need to support queries, these values would be placed under a `meta:json` field, since the gateway’s essential concern is merchant identity, not merchant-side customer references. This choice streamlines integration for most use cases but increases coupling to merchant reference formats and reduces security by not using an additional secret.

These tradeoffs and design decisions reflect both current infrastructure constraints and readiness for future migration to more robust patterns.
