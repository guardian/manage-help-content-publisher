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
        app: 'manage-help-content-publisher'
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
            Stage: 'CODE'
          }
        }
      });

      // Takedown Lambda
      template.hasResourceProperties('AWS::Lambda::Function', {
        FunctionName: 'manage-help-content-publisher-CODE-takedown',
        Handler: 'managehelpcontentpublisher.TakingDownHandler::handleRequest',
        Runtime: 'java11',
        MemorySize: 2048,
        Timeout: 30,
        Environment: {
          Variables: {
            App: 'manage-help-content-publisher',
            Stack: 'membership',
            Stage: 'CODE'
          }
        }
      });
    });

    test('creates two separate API Gateways', () => {
      // Publisher API Gateway
      template.hasResourceProperties('AWS::ApiGateway::RestApi', {
        Name: 'manage-help-content-publisher-CODE-publisher-api',
        Description: 'API Gateway for manage help content publisher'
      });

      // Takedown API Gateway
      template.hasResourceProperties('AWS::ApiGateway::RestApi', {
        Name: 'manage-help-content-publisher-CODE-takedown-api',
        Description: 'API Gateway for manage help content takedown'
      });
    });

    test('creates API Gateway methods correctly', () => {
      // POST method for publisher
      template.hasResourceProperties('AWS::ApiGateway::Method', {
        HttpMethod: 'POST',
        ApiKeyRequired: true
      });

      // DELETE method for takedown
      template.hasResourceProperties('AWS::ApiGateway::Method', {
        HttpMethod: 'DELETE',
        ApiKeyRequired: true
      });
    });

    test('creates usage plans and API keys for both APIs', () => {
      // Publisher usage plan
      template.hasResourceProperties('AWS::ApiGateway::UsagePlan', {
        UsagePlanName: 'manage-help-content-publisher-CODE-publisher-usage-plan',
        Description: 'Usage plan for manage help content publisher API'
      });

      // Takedown usage plan
      template.hasResourceProperties('AWS::ApiGateway::UsagePlan', {
        UsagePlanName: 'manage-help-content-publisher-CODE-takedown-usage-plan',
        Description: 'Usage plan for manage help content takedown API'
      });

      // Publisher API key
      template.hasResourceProperties('AWS::ApiGateway::ApiKey', {
        Name: 'manage-help-content-publisher-CODE-publisher-key'
      });

      // Takedown API key
      template.hasResourceProperties('AWS::ApiGateway::ApiKey', {
        Name: 'manage-help-content-publisher-CODE-takedown-key'
      });
    });

    test('creates log groups with correct retention', () => {
      template.hasResourceProperties('AWS::Logs::LogGroup', {
        LogGroupName: '/aws/lambda/manage-help-content-publisher-CODE',
        RetentionInDays: 90
      });

      template.hasResourceProperties('AWS::Logs::LogGroup', {
        LogGroupName: '/aws/lambda/manage-help-content-publisher-CODE-takedown',
        RetentionInDays: 90
      });
    });

    test('creates IAM policies for S3 access', () => {
      // Check that IAM policies exist with S3 permissions
      const policies = template.findResources('AWS::IAM::Policy');
      expect(Object.keys(policies)).toHaveLength(2);
      
      // Check that at least one policy contains S3 permissions for manage-help-content bucket
      const policyStatements = Object.values(policies).flatMap((policy: Record<string, unknown>) => {
        const policyObj = policy as { Properties: { PolicyDocument: { Statement: unknown[] } } };
        return policyObj.Properties.PolicyDocument.Statement;
      });
      
      const s3Statement = policyStatements.find((statement: unknown) => {
        const stmt = statement as { Action?: string[]; Resource?: string[]; Effect?: string };
        return stmt.Action?.includes('s3:GetObject') && 
               stmt.Resource?.some((resource: string) => resource.includes('manage-help-content'));
      }) as { Action?: string[]; Resource?: string[]; Effect?: string } | undefined;
      
      expect(s3Statement).toBeDefined();
      expect(s3Statement?.Effect).toBe('Allow');
    });

    test('does not create CloudWatch alarms in CODE environment', () => {
      template.resourceCountIs('AWS::CloudWatch::Alarm', 0);
    });

    test('has correct resource counts for CODE environment', () => {
      template.resourceCountIs('AWS::Lambda::Function', 2);
      template.resourceCountIs('AWS::ApiGateway::RestApi', 2);
      template.resourceCountIs('AWS::ApiGateway::UsagePlan', 2);
      template.resourceCountIs('AWS::ApiGateway::ApiKey', 2);
      template.resourceCountIs('AWS::Logs::LogGroup', 2);
      template.resourceCountIs('AWS::CloudWatch::Alarm', 0);
    });
  });

  describe('PROD environment', () => {
    beforeEach(() => {
      const stack = new ManageHelpContentPublisherStack(app, 'TestStack', {
        env: { account: '123456789012', region: 'eu-west-1' },
        stage: 'PROD',
        stack: 'membership',
        app: 'manage-help-content-publisher'
      });
      template = Template.fromStack(stack);
    });

    test('creates CloudWatch alarms for both APIs in PROD', () => {
      // Publisher API alarms
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: '4XX rate from manage-help-content-publisher-PROD-publisher',
        AlarmDescription: '4xx errors on manage help content publisher API'
      });

      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: '5XX rate from manage-help-content-publisher-PROD-publisher',
        AlarmDescription: '5xx errors on manage help content publisher API'
      });

      // Takedown API alarms
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: '4XX rate from manage-help-content-publisher-PROD-takedown',
        AlarmDescription: '4xx errors on manage help content takedown API'
      });

      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: '5XX rate from manage-help-content-publisher-PROD-takedown',
        AlarmDescription: '5xx errors on manage help content takedown API'
      });
    });

    test('has correct resource counts for PROD environment', () => {
      template.resourceCountIs('AWS::Lambda::Function', 2);
      template.resourceCountIs('AWS::ApiGateway::RestApi', 2);
      template.resourceCountIs('AWS::ApiGateway::UsagePlan', 2);
      template.resourceCountIs('AWS::ApiGateway::ApiKey', 2);
      template.resourceCountIs('AWS::Logs::LogGroup', 2);
      template.resourceCountIs('AWS::CloudWatch::Alarm', 4); // 4 alarms for 2 APIs
    });
  });

  describe('Guardian CDK compliance', () => {
    beforeEach(() => {
      const stack = new ManageHelpContentPublisherStack(app, 'TestStack', {
        env: { account: '123456789012', region: 'eu-west-1' },
        stage: 'CODE',
        stack: 'membership',
        app: 'manage-help-content-publisher'
      });
      template = Template.fromStack(stack);
    });

    test('Lambda functions have Guardian metadata', () => {
      // Check that Lambda functions have the App tag
      const lambdas = template.findResources('AWS::Lambda::Function');
      expect(Object.keys(lambdas)).toHaveLength(2);
      
      Object.values(lambdas).forEach((lambda: Record<string, unknown>) => {
        const lambdaObj = lambda as { Properties: { Tags?: Array<{ Key: string; Value: string }> } };
        const appTag = lambdaObj.Properties.Tags?.find((tag: { Key: string; Value: string }) => tag.Key === 'App');
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
            Stage: 'CODE'
          }
        }
      });
    });
  });
});
