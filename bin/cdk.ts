#!/usr/bin/env node
import 'source-map-support/register';
import { App } from 'aws-cdk-lib';
import { ManageHelpContentPublisherStack } from '../lib/manage-help-content-publisher-stack';
import * as console from 'node:console';

const app = new App();

// Deploy to different stages
const stages = ['CODE', 'PROD'];

stages.forEach((stage) => {
	new ManageHelpContentPublisherStack(
		app,
		`ManageHelpContentPublisher-${stage}`,
		{
			env: {
				account: process.env.CDK_DEFAULT_ACCOUNT,
				region: 'eu-west-1',
			},
			stage: stage,
			stack: 'membership',
			app: 'manage-help-content-publisher',
		},
	);
});

console.log(process.env);

app.synth();
