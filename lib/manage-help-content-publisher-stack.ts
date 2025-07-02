import { GuApiLambda } from "@guardian/cdk";
import { GuStack } from "@guardian/cdk/lib/constructs/core";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import type { App } from "aws-cdk-lib";
import { Duration } from "aws-cdk-lib";
import {
  ApiKeySourceType,
  LambdaIntegration,
  UsagePlan,
} from "aws-cdk-lib/aws-apigateway";
import {
  Alarm,
  ComparisonOperator,
  Metric,
  TreatMissingData,
} from "aws-cdk-lib/aws-cloudwatch";
import { SnsAction } from "aws-cdk-lib/aws-cloudwatch-actions";
import { Effect, PolicyStatement } from "aws-cdk-lib/aws-iam";
import { Runtime } from "aws-cdk-lib/aws-lambda";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import { Bucket } from "aws-cdk-lib/aws-s3";
import { Topic } from "aws-cdk-lib/aws-sns";

interface ManageHelpContentPublisherStackProps extends GuStackProps {
  stage: string;
}

export class ManageHelpContentPublisherStack extends GuStack {
  public readonly publisherApi: GuApiLambda;
  public readonly takedownApi: GuApiLambda;
  public readonly publisherLogGroup: LogGroup;
  public readonly takedownLogGroup: LogGroup;

  constructor(
    scope: App,
    id: string,
    props: ManageHelpContentPublisherStackProps,
  ) {
    super(scope, id, props);

    const { stage } = props;
    const app = "manage-help-content-publisher";
    const nameWithStage = `${app}-${stage}`;

    // Guardian standard environment variables
    const commonEnvironmentVariables = {
      App: app,
      Stack: this.stack,
      Stage: this.stage,
    };

    // Reference to deployment bucket - use SSM parameter like in CloudFormation
    const deploymentBucketName =
      (this.node.tryGetContext("deploymentBucket") as string) ||
      "membership-dist";
    const deploymentBucket = Bucket.fromBucketName(
      this,
      "DeploymentBucket",
      deploymentBucketName
    );

    // Content bucket name - parameterized
    const contentBucketName =
      (this.node.tryGetContext("contentBucket") as string) ||
      "manage-help-content";

    // IAM Policies - Guardian scoped approach
    const s3PolicyStatements = [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket",
        ],
        resources: [
          `arn:aws:s3:::${contentBucketName}`,
          `arn:aws:s3:::${contentBucketName}/*`,
        ],
      }),
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ["s3:GetObject"],
        resources: [deploymentBucket.arnForObjects("*")],
      }),
    ];

    // Log Groups with 90-day retention - matching original CloudFormation
    this.publisherLogGroup = new LogGroup(this, "PublisherLogGroup", {
      logGroupName: `/aws/lambda/${nameWithStage}`,
      retention: RetentionDays.THREE_MONTHS,
    });

    this.takedownLogGroup = new LogGroup(this, "TakedownLogGroup", {
      logGroupName: `/aws/lambda/${nameWithStage}-takedown`,
      retention: RetentionDays.THREE_MONTHS,
    });

    // Publisher Lambda with API Gateway
    this.publisherApi = new GuApiLambda(this, "publisher-lambda", {
      functionName: nameWithStage,
      fileName: `${app}.jar`,
      handler: "managehelpcontentpublisher.PublishingHandler::handleRequest",
      runtime: Runtime.JAVA_11,
      memorySize: 2048,
      timeout: Duration.seconds(30),
      environment: commonEnvironmentVariables,
      initialPolicy: s3PolicyStatements,
      monitoringConfiguration: { noMonitoring: true },
      app: app,
      api: {
        id: `${nameWithStage}-publisher-api`,
        restApiName: `${nameWithStage}-publisher-api`,
        description: "API Gateway for manage help content publisher",
        proxy: false,
        apiKeySourceType: ApiKeySourceType.HEADER,
        defaultMethodOptions: { apiKeyRequired: true },
        deployOptions: { stageName: stage },
      },
    });

    // Add POST method to publisher API
    this.publisherApi.api.root.addMethod("POST", new LambdaIntegration(this.publisherApi), {
      apiKeyRequired: true,
    });

    // Takedown Lambda with API Gateway
    this.takedownApi = new GuApiLambda(this, "takedown-lambda", {
      functionName: `${nameWithStage}-takedown`,
      fileName: `${app}.jar`,
      handler: "managehelpcontentpublisher.TakingDownHandler::handleRequest",
      runtime: Runtime.JAVA_11,
      memorySize: 2048,
      timeout: Duration.seconds(30),
      environment: commonEnvironmentVariables,
      initialPolicy: s3PolicyStatements,
      monitoringConfiguration: { noMonitoring: true },
      app: app,
      api: {
        id: `${nameWithStage}-takedown-api`,
        restApiName: `${nameWithStage}-takedown-api`,
        description: "API Gateway for manage help content takedown",
        proxy: false,
        apiKeySourceType: ApiKeySourceType.HEADER,
        defaultMethodOptions: { apiKeyRequired: true },
        deployOptions: { stageName: stage },
      },
    });

    // Add DELETE method with path parameter to takedown API
    const articlePathResource = this.takedownApi.api.root.addResource("{articlePath}");
    articlePathResource.addMethod("DELETE", new LambdaIntegration(this.takedownApi), {
      apiKeyRequired: true,
    });

    // Usage Plans and API Keys for both APIs
    const publisherUsagePlan = new UsagePlan(this, "PublisherUsagePlan", {
      name: `${nameWithStage}-publisher-usage-plan`,
      description: "Usage plan for manage help content publisher API",
      apiStages: [
        {
          stage: this.publisherApi.api.deploymentStage,
          api: this.publisherApi.api,
        },
      ],
    });

    const takedownUsagePlan = new UsagePlan(this, "TakedownUsagePlan", {
      name: `${nameWithStage}-takedown-usage-plan`,
      description: "Usage plan for manage help content takedown API",
      apiStages: [
        {
          stage: this.takedownApi.api.deploymentStage,
          api: this.takedownApi.api,
        },
      ],
    });

    // API Keys
    const publisherApiKey = this.publisherApi.api.addApiKey("PublisherApiKey", {
      apiKeyName: `${nameWithStage}-publisher-key`,
    });

    const takedownApiKey = this.takedownApi.api.addApiKey("TakedownApiKey", {
      apiKeyName: `${nameWithStage}-takedown-key`,
    });

    // Associate API keys to usage plans
    publisherUsagePlan.addApiKey(publisherApiKey);
    takedownUsagePlan.addApiKey(takedownApiKey);

    // CloudWatch Alarms (PROD only) - matching original CloudFormation
    if (stage === "PROD") {
      const snsTopicArn = `arn:aws:sns:${this.region}:${this.account}:membership-${stage}`;
      const snsTopic = Topic.fromTopicArn(this, "AlertsTopic", snsTopicArn);

      // Publisher API Alarms
      new Alarm(this, "Publisher4xxApiAlarm", {
        alarmName: `4XX rate from ${nameWithStage}-publisher`,
        alarmDescription: "4xx errors on manage help content publisher API",
        metric: new Metric({
          namespace: "AWS/ApiGateway",
          metricName: "4XXError",
          dimensionsMap: {
            ApiName: this.publisherApi.api.restApiName,
            Stage: this.publisherApi.api.deploymentStage.stageName,
          },
          statistic: "Sum",
          period: Duration.minutes(1),
        }),
        threshold: 5,
        comparisonOperator: ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
        evaluationPeriods: 1,
        treatMissingData: TreatMissingData.NOT_BREACHING,
      }).addAlarmAction(new SnsAction(snsTopic));

      new Alarm(this, "Publisher5xxApiAlarm", {
        alarmName: `5XX rate from ${nameWithStage}-publisher`,
        alarmDescription: "5xx errors on manage help content publisher API",
        metric: new Metric({
          namespace: "AWS/ApiGateway",
          metricName: "5XXError",
          dimensionsMap: {
            ApiName: this.publisherApi.api.restApiName,
            Stage: this.publisherApi.api.deploymentStage.stageName,
          },
          statistic: "Sum",
          period: Duration.minutes(1),
        }),
        threshold: 5,
        comparisonOperator: ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
        evaluationPeriods: 1,
        treatMissingData: TreatMissingData.NOT_BREACHING,
      }).addAlarmAction(new SnsAction(snsTopic));

      // Takedown API Alarms
      new Alarm(this, "Takedown4xxApiAlarm", {
        alarmName: `4XX rate from ${nameWithStage}-takedown`,
        alarmDescription: "4xx errors on manage help content takedown API",
        metric: new Metric({
          namespace: "AWS/ApiGateway",
          metricName: "4XXError",
          dimensionsMap: {
            ApiName: this.takedownApi.api.restApiName,
            Stage: this.takedownApi.api.deploymentStage.stageName,
          },
          statistic: "Sum",
          period: Duration.minutes(1),
        }),
        threshold: 5,
        comparisonOperator: ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
        evaluationPeriods: 1,
        treatMissingData: TreatMissingData.NOT_BREACHING,
      }).addAlarmAction(new SnsAction(snsTopic));

      new Alarm(this, "Takedown5xxApiAlarm", {
        alarmName: `5XX rate from ${nameWithStage}-takedown`,
        alarmDescription: "5xx errors on manage help content takedown API",
        metric: new Metric({
          namespace: "AWS/ApiGateway",
          metricName: "5XXError",
          dimensionsMap: {
            ApiName: this.takedownApi.api.restApiName,
            Stage: this.takedownApi.api.deploymentStage.stageName,
          },
          statistic: "Sum",
          period: Duration.minutes(1),
        }),
        threshold: 5,
        comparisonOperator: ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
        evaluationPeriods: 1,
        treatMissingData: TreatMissingData.NOT_BREACHING,
      }).addAlarmAction(new SnsAction(snsTopic));
    }
  }
}
