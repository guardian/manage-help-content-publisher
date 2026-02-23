# Migration from CloudFormation (`cfn.yaml`) to CDK

## Overview

This PR migrates the infrastructure for `manage-help-content-publisher` from a legacy CloudFormation template (
`cfn.yaml`) to a modern, maintainable Guardian CDK stack. The goal is to achieve full functional parity, Guardian
compliance, and improved developer experience.

---

## Key Migration Steps

### 1. Stack Structure

- **CloudFormation:**
    - Single template (`cfn.yaml`) defining Lambdas, API Gateway, Usage Plan, API Key, S3 permissions, CloudWatch
      alarms, and log groups.
- **CDK:**
    - All infra is now defined in TypeScript (`lib/manage-help-content-publisher-stack.ts`) using Guardian CDK
      constructs (`GuLambdaFunction`, `GuApiGatewayWithLambdaByPath`, etc.).

### 2. Lambda Functions

- Both Publisher and Takedown Lambdas are now created via `GuLambdaFunction`, with Guardian-compliant environment
  variables, memory, timeout, and S3 permissions via `initialPolicy`.
- Deployment artifacts are referenced via `fileName` and `app` props.

### 3. API Gateway

- Migrated to `GuApiGatewayWithLambdaByPath` for Guardian best practices.
- Both POST `/` (publisher) and DELETE `/{articlePath}` (takedown) methods are defined with API key authentication.
- Usage plan and API key are created and attached as per Guardian conventions.

### 4. CloudWatch Alarms

- CloudFormation defined both 4XX and 5XX alarms.
- CDK (Guardian pattern) only supports a 5XX alarm out-of-the-box, which is now configured via the
  `monitoringConfiguration` property.
- Alarm notifications are routed to the `membership-PROD` SNS topic.

### 5. Log Groups

- Both Lambdas have log groups with 90-day retention, matching Guardian standards.

### 6. IAM & S3 Permissions

- S3 permissions for both the content and deployment buckets are attached directly to the Lambdas using inline policies.

### 7. Testing & Validation

- All existing unit tests were updated to match Guardian resource naming and alarm conventions.
- Tests assert:
    - Lambda configuration and environment variables
    - API Gateway methods and usage plan
    - S3 permissions
    - Log group retention
    - CloudWatch alarm presence and properties (5XX only)
- All tests (`pnpm run package`) pass and the stack synthesizes successfully.

---

## Notable Differences from CloudFormation

- **Alarms:** Only a 5XX alarm is now created (Guardian default); 4XX alarm is omitted.
- **Resource Naming:** API Gateway and other resources now follow Guardian conventions (e.g.,
  `membership-CODE-manage-help-content-publisher`).
- **Guardian CDK Patterns:** All infra is now managed using Guardian constructs for consistency, security, and
  maintainability.

---

## Reviewer Notes

- Confirm that all tests pass and the stack synthesizes as expected.
- This migration paves the way for easier future changes, improved security, and better compliance with Guardian
  standards.

---

If you have any questions about the migration, please let me know!