# Manage Help Content Publisher

This repo holds code to publish Help Centre content to [MMA](https://manage.theguardian.com/help-centre).
The content is stored in [Salesforce Knowledge](https://gnmtouchpoint.lightning.force.com/lightning/o/Knowledge__kav/list?filterName=00B5I000003lI1KUAU).

It also holds [a sub-project to import articles into Salesforce from Capi](legacy-content-import).  

There's a folder for useful scripts [here](scripts).

algorithm

When publishing an article:

1. publisharticle function is called
2. the 'article' key is read from the json body as the new article
3. the 'dataCategories' are read in from the json body as the 'Topics', each item in the array is converted to Topic type, encoded into JSON and stored in S3 with the filename being the `path` property
4. 
