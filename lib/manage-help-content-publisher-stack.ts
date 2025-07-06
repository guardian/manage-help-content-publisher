import { GuStack } from '@guardian/cdk/lib/constructs/core';
import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import type { App } from 'aws-cdk-lib';
import { Duration } from 'aws-cdk-lib';
import {
	ApiKey,
	LambdaIntegration,
	MethodLoggingLevel,
	RestApi,
	UsagePlan,
} from 'aws-cdk-lib/aws-apigateway';
import {
	Alarm,
	ComparisonOperator,
	Metric,
	TreatMissingData,
} from 'aws-cdk-lib/aws-cloudwatch';
import { SnsAction } from 'aws-cdk-lib/aws-cloudwatch-actions';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import {
	Code,
	Function as LambdaFunction,
	Runtime,
} from 'aws-cdk-lib/aws-lambda';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { Bucket } from 'aws-cdk-lib/aws-s3';
import { Topic } from 'aws-cdk-lib/aws-sns';

interface ManageHelpContentPublisherStackProps extends GuStackProps {
	stage: string;
}

export class ManageHelpContentPublisherStack extends GuStack {
	constructor(
		scope: App,
		id: string,
		props: ManageHelpContentPublisherStackProps,
	) {
		super(scope, id, props);

		const { stage } = props;
		const app = 'manage-help-content-publisher';
		const publisherFunctionName = `${app}-${stage}`;
		const takedownFunctionName = `${app}-takedown-${stage}`;
		const publisherLogGroupName = `/aws/lambda/${app}-${stage}`;
		const takedownLogGroupName = `/aws/lambda/${app}-takedown-${stage}`;
		const apiGatewayName = `${app}-${stage}-api-gateway`;
		const usagePlanName = `${app}-${stage}-usage-plan`;

		// Log groups
		new LogGroup(this, 'PublisherLogGroup', {
			logGroupName: publisherLogGroupName,
			retention: RetentionDays.THREE_MONTHS,
		});
		new LogGroup(this, 'TakedownLogGroup', {
			logGroupName: takedownLogGroupName,
			retention: RetentionDays.THREE_MONTHS,
		});

		// S3 policies
		const s3PolicyStatements = [
			new PolicyStatement({
				effect: Effect.ALLOW,
				actions: ['s3:ListBucket'],
				resources: ['arn:aws:s3:::manage-help-content*'],
			}),
			new PolicyStatement({
				effect: Effect.ALLOW,
				actions: ['s3:GetObject', 's3:PutObject'],
				resources: [`arn:aws:s3:::manage-help-content/${stage}/*`],
			}),
			new PolicyStatement({
				effect: Effect.ALLOW,
				actions: ['s3:GetObject'],
				resources: ['arn:aws:s3::*:membership-dist/*'],
			}),
		];

		// Buckets (for Lambda code)
		const deploymentBucket = Bucket.fromBucketName(
			this,
			'DeploymentBucket',
			'membership-dist',
		);

		// Guardian standard environment variables
		const guardianEnvVars = {
			App: app,
			Stack: this.stack,
			Stage: this.stage,
			stage,
		};

		// Publisher Lambda
		const publisherLambda = new LambdaFunction(this, 'PublisherLambda', {
			functionName: publisherFunctionName,
			runtime: Runtime.JAVA_11,
			handler: 'managehelpcontentpublisher.PublishingHandler::handleRequest',
			code: Code.fromBucket(
				deploymentBucket,
				`membership/${stage}/${app}/${app}.jar`,
			),
			memorySize: 2048,
			timeout: Duration.seconds(30),
			environment: guardianEnvVars,
			initialPolicy: s3PolicyStatements,
			description:
				'Codebase: https://github.com/guardian/manage-help-content-publisher.',
		});

		// Takedown Lambda
		const takedownLambda = new LambdaFunction(this, 'TakedownLambda', {
			functionName: takedownFunctionName,
			runtime: Runtime.JAVA_11,
			handler: 'managehelpcontentpublisher.TakingDownHandler::handleRequest',
			code: Code.fromBucket(
				deploymentBucket,
				`membership/${stage}/${app}/${app}.jar`,
			),
			memorySize: 2048,
			timeout: Duration.seconds(30),
			environment: guardianEnvVars,
			initialPolicy: s3PolicyStatements,
			description:
				'Codebase: https://github.com/guardian/manage-help-content-publisher.',
		});

		// API Gateway unique
		const api = new RestApi(this, 'ApiGateway', {
			restApiName: apiGatewayName,
			deployOptions: {
				stageName: stage,
				loggingLevel: MethodLoggingLevel.INFO,
			},
			description: 'API Gateway for manage-help-content-publisher',
		});

		// POST / (publisher)
		api.root.addMethod('POST', new LambdaIntegration(publisherLambda), {
			apiKeyRequired: true,
		});
		// DELETE /{articlePath} (takedown)
		api.root
			.addResource('{articlePath}')
			.addMethod('DELETE', new LambdaIntegration(takedownLambda), {
				apiKeyRequired: true,
			});

		// API Key & Usage Plan
		const apiKey = new ApiKey(this, 'ApiKey', {
			apiKeyName: `${app}-${stage}-api-key`,
			enabled: true,
		});
		const usagePlan = new UsagePlan(this, 'UsagePlan', {
			name: usagePlanName,
			apiStages: [{ api, stage: api.deploymentStage }],
		});
		usagePlan.addApiKey(apiKey);

		// Alarms (PROD uniquement)
		if (stage === 'PROD') {
			const snsTopic = Topic.fromTopicArn(
				this,
				'MembershipProdTopic',
				'arn:aws:sns:eu-west-1:123456789012:membership-PROD',
			);
			// 4xx
			new Alarm(this, '4xxApiAlarm', {
				alarmName: `4XX rate from ${apiGatewayName}`,
				alarmDescription:
					'See https://github.com/guardian/manage-help-content-publisher/blob/main/README.md#Troubleshooting',
				metric: new Metric({
					namespace: 'AWS/ApiGateway',
					metricName: '4XXError',
					dimensionsMap: { ApiName: apiGatewayName, Stage: stage },
					statistic: 'Sum',
					period: Duration.hours(1),
				}),
				evaluationPeriods: 1,
				threshold: 1,
				comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
				treatMissingData: TreatMissingData.IGNORE,
			}).addAlarmAction(new SnsAction(snsTopic));
			// 5xx
			new Alarm(this, '5xxApiAlarm', {
				alarmName: `5XX rate from ${apiGatewayName}`,
				alarmDescription:
					'See https://github.com/guardian/manage-help-content-publisher/blob/main/README.md#Troubleshooting',
				metric: new Metric({
					namespace: 'AWS/ApiGateway',
					metricName: '5XXError',
					dimensionsMap: { ApiName: apiGatewayName, Stage: stage },
					statistic: 'Sum',
					period: Duration.hours(1),
				}),
				evaluationPeriods: 1,
				threshold: 1,
				comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
				treatMissingData: TreatMissingData.IGNORE,
			}).addAlarmAction(new SnsAction(snsTopic));
		}
	}
}
