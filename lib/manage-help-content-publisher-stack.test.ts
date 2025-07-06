import { App } from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { ManageHelpContentPublisherStack } from './manage-help-content-publisher-stack';

describe('ManageHelpContentPublisherStack', () => {
	let app: App;
	let template: Template;

	beforeEach(() => {
		app = new App();
	});

	describe('CODE environment', () => {
		beforeEach(() => {
			const stack = new ManageHelpContentPublisherStack(app, 'TestStack', {
				env: { account: '123456789012', region: 'eu-west-1' },
				stage: 'CODE',
				stack: 'membership',
				app: 'manage-help-content-publisher',
			});
			template = Template.fromStack(stack);
		});

		test('creates Lambda functions with correct configuration', () => {
			// Publisher Lambda
			template.hasResourceProperties('AWS::Lambda::Function', {
				FunctionName: 'manage-help-content-publisher-CODE',
				Handler: 'managehelpcontentpublisher.PublishingHandler::handleRequest',
				Runtime: 'java11',
				MemorySize: 2048,
				Timeout: 30,
				Environment: {
					Variables: {
						App: 'manage-help-content-publisher',
						Stack: 'membership',
						Stage: 'CODE',
						stage: 'CODE',
					},
				},
			});

			// Takedown Lambda
			template.hasResourceProperties('AWS::Lambda::Function', {
				FunctionName: 'manage-help-content-publisher-takedown-CODE',
				Handler: 'managehelpcontentpublisher.TakingDownHandler::handleRequest',
				Runtime: 'java11',
				MemorySize: 2048,
				Timeout: 30,
				Environment: {
					Variables: {
						App: 'manage-help-content-publisher',
						Stack: 'membership',
						Stage: 'CODE',
						stage: 'CODE',
					},
				},
			});
		});

		test('creates a single API Gateway with correct name and description', () => {
			template.hasResourceProperties('AWS::ApiGateway::RestApi', {
				Name: 'manage-help-content-publisher-CODE-api-gateway',
				Description: 'API Gateway for manage-help-content-publisher',
			});
		});

		test('creates API Gateway methods correctly', () => {
			// POST method for publisher
			template.hasResourceProperties('AWS::ApiGateway::Method', {
				HttpMethod: 'POST',
				ApiKeyRequired: true,
			});

			// DELETE method for takedown
			template.hasResourceProperties('AWS::ApiGateway::Method', {
				HttpMethod: 'DELETE',
				ApiKeyRequired: true,
			});
		});

		test('creates usage plan and API key', () => {
			template.hasResourceProperties('AWS::ApiGateway::UsagePlan', {
				UsagePlanName: 'manage-help-content-publisher-CODE-usage-plan',
			});
			template.hasResourceProperties('AWS::ApiGateway::ApiKey', {
				Enabled: true,
			});
		});

		test('creates log groups with correct retention', () => {
			template.hasResourceProperties('AWS::Logs::LogGroup', {
				LogGroupName: '/aws/lambda/manage-help-content-publisher-CODE',
				RetentionInDays: 90,
			});
			template.hasResourceProperties('AWS::Logs::LogGroup', {
				LogGroupName: '/aws/lambda/manage-help-content-publisher-takedown-CODE',
				RetentionInDays: 90,
			});
		});

		test('creates IAM policies for S3 access', () => {
			const resources = template.findResources('AWS::IAM::Policy') as Record<
				string,
				{ Properties: { PolicyDocument: { Statement: unknown[] } } }
			>;
			const found = Object.values(resources).some((policy) => {
				const doc = policy.Properties.PolicyDocument;
				return (doc.Statement as Array<Record<string, unknown>>).some(
					(stmt) => {
						const resource = stmt.Resource;
						const resourceArr = Array.isArray(resource) ? resource : [resource];
						return (
							Array.isArray(stmt.Action) &&
							stmt.Action.includes('s3:GetObject') &&
							resourceArr.some(
								(res) =>
									typeof res === 'string' &&
									res.includes('manage-help-content'),
							)
						);
					},
				);
			});
			expect(found).toBe(true);
		});

		test('does not create CloudWatch alarms in CODE environment', () => {
			const alarms = template.findResources('AWS::CloudWatch::Alarm');
			expect(Object.keys(alarms).length).toBe(0);
		});

		test('has correct resource counts for CODE environment', () => {
			template.resourceCountIs('AWS::Lambda::Function', 2);
			template.resourceCountIs('AWS::ApiGateway::RestApi', 1);
			template.resourceCountIs('AWS::ApiGateway::UsagePlan', 1);
			template.resourceCountIs('AWS::ApiGateway::ApiKey', 1);
			template.resourceCountIs('AWS::Logs::LogGroup', 2);
		});
	});

	describe('PROD environment', () => {
		beforeEach(() => {
			const stack = new ManageHelpContentPublisherStack(app, 'TestStack', {
				env: { account: '123456789012', region: 'eu-west-1' },
				stage: 'PROD',
				stack: 'membership',
				app: 'manage-help-content-publisher',
			});
			template = Template.fromStack(stack);
		});

		test('creates CloudWatch alarms for API Gateway in PROD', () => {
			template.hasResourceProperties('AWS::CloudWatch::Alarm', {
				AlarmName:
					'4XX rate from manage-help-content-publisher-PROD-api-gateway',
				AlarmDescription:
					'See https://github.com/guardian/manage-help-content-publisher/blob/main/README.md#Troubleshooting',
			});
			template.hasResourceProperties('AWS::CloudWatch::Alarm', {
				AlarmName:
					'5XX rate from manage-help-content-publisher-PROD-api-gateway',
				AlarmDescription:
					'See https://github.com/guardian/manage-help-content-publisher/blob/main/README.md#Troubleshooting',
			});
		});

		test('has correct resource counts for PROD environment', () => {
			template.resourceCountIs('AWS::Lambda::Function', 2);
			template.resourceCountIs('AWS::ApiGateway::RestApi', 1);
			template.resourceCountIs('AWS::ApiGateway::UsagePlan', 1);
			template.resourceCountIs('AWS::ApiGateway::ApiKey', 1);
			template.resourceCountIs('AWS::Logs::LogGroup', 2);
			template.resourceCountIs('AWS::CloudWatch::Alarm', 2);
		});
	});

	describe('Guardian CDK compliance', () => {
		beforeEach(() => {
			const stack = new ManageHelpContentPublisherStack(app, 'TestStack', {
				env: { account: '123456789012', region: 'eu-west-1' },
				stage: 'CODE',
				stack: 'membership',
				app: 'manage-help-content-publisher',
			});
			template = Template.fromStack(stack);
		});

		test('Lambda functions have Guardian metadata', () => {
			// Check that Lambda functions have the App tag
			const lambdas = template.findResources('AWS::Lambda::Function');
			expect(Object.keys(lambdas)).toHaveLength(2);

			Object.values(lambdas).forEach((lambda: Record<string, unknown>) => {
				const lambdaObj = lambda as {
					Properties: { Tags?: Array<{ Key: string; Value: string }> };
				};
				const appTag = lambdaObj.Properties.Tags?.find(
					(tag: { Key: string; Value: string }) => tag.Key === 'App',
				);
				expect(appTag).toBeDefined();
				expect(appTag?.Value).toBe('manage-help-content-publisher');
			});
		});

		test('uses Guardian environment variables pattern', () => {
			template.hasResourceProperties('AWS::Lambda::Function', {
				Environment: {
					Variables: {
						App: 'manage-help-content-publisher',
						Stack: 'membership',
						Stage: 'CODE',
					},
				},
			});
		});
	});
});
