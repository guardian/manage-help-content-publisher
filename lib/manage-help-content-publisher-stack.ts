import { GuApiGatewayWithLambdaByPath } from '@guardian/cdk';
import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import { GuStack } from '@guardian/cdk/lib/constructs/core';
import { GuLambdaFunction } from '@guardian/cdk/lib/constructs/lambda';
import type { App } from 'aws-cdk-lib';
import { Duration } from 'aws-cdk-lib';
import { UsagePlan } from 'aws-cdk-lib/aws-apigateway';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Runtime } from 'aws-cdk-lib/aws-lambda';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';

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
		const usagePlanName = `${app}-${stage}-usage-plan`;

		new LogGroup(this, 'PublisherLogGroup', {
			logGroupName: publisherLogGroupName,
			retention: RetentionDays.THREE_MONTHS,
		});

		new LogGroup(this, 'TakedownLogGroup', {
			logGroupName: takedownLogGroupName,
			retention: RetentionDays.THREE_MONTHS,
		});

		const basePolices: PolicyStatement[] = [
			new PolicyStatement({
				effect: Effect.ALLOW,
				actions: ['s3:ListBucket'],
				resources: ['arn:aws:s3:::manage-help-content*'],
			}),
			new PolicyStatement({
				effect: Effect.ALLOW,
				actions: ['s3:GetObject'],
				resources: ['arn:aws:s3::*:membership-dist/*'],
			}),
		];

		const guardianEnvVars = {
			App: app,
			Stack: this.stack,
			Stage: this.stage,
			stage,
		};

		const publisherLambda = new GuLambdaFunction(this, 'PublisherLambda', {
			functionName: publisherFunctionName,
			runtime: Runtime.JAVA_11,
			handler: 'managehelpcontentpublisher.PublishingHandler::handleRequest',
			fileName: 'manage-help-content-publisher.jar',
			app: app,
			memorySize: 2048,
			timeout: Duration.seconds(30),
			environment: guardianEnvVars,
			initialPolicy: [
				...basePolices,
				new PolicyStatement({
					effect: Effect.ALLOW,
					actions: ['s3:GetObject', 's3:PutObject'],
					resources: [`arn:aws:s3:::manage-help-content/${stage}/*`],
				}),
			],
			description:
				'Codebase: https://github.com/guardian/manage-help-content-publisher.',
		});

		const takedownLambda = new GuLambdaFunction(this, 'TakedownLambda', {
			functionName: takedownFunctionName,
			runtime: Runtime.JAVA_11,
			handler: 'managehelpcontentpublisher.TakingDownHandler::handleRequest',
			fileName: 'manage-help-content-publisher.jar',
			app: app,
			memorySize: 2048,
			timeout: Duration.seconds(30),
			environment: guardianEnvVars,
			initialPolicy: [
				...basePolices,
				new PolicyStatement({
					effect: Effect.ALLOW,
					actions: ['s3:GetObject', 's3:PutObject', 's3:DeleteObject'],
					resources: [`arn:aws:s3:::manage-help-content/${stage}/*`],
				}),
			],
			description:
				'Codebase: https://github.com/guardian/manage-help-content-publisher.',
		});

		const apiGateway = new GuApiGatewayWithLambdaByPath(this, {
			app: app,
			deployOptions: {
				stageName: stage,
			},
			targets: [
				{
					path: '/',
					httpMethod: 'POST',
					lambda: publisherLambda,
					apiKeyRequired: true,
				},
				{
					path: '/{articlePath}',
					httpMethod: 'DELETE',
					lambda: takedownLambda,
					apiKeyRequired: true,
				},
			],
			monitoringConfiguration:
				stage === 'PROD'
					? {
							http5xxAlarm: { tolerated5xxPercentage: 5 },
							snsTopicName: `membership-PROD`,
						}
					: { noMonitoring: true },
		});

		const apiKey = apiGateway.api.addApiKey(`${app}-${stage}-api-key`, {
			apiKeyName: `${app}-${stage}-api-key`,
		});

		const usagePlan: UsagePlan = new UsagePlan(this, 'UsagePlan', {
			name: usagePlanName,
			apiStages: [
				{ api: apiGateway.api, stage: apiGateway.api.deploymentStage },
			],
		});
		usagePlan.addApiKey(apiKey);
	}
}
