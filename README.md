# Manage Help Content Publisher

This repo holds code to publish Help Centre content to [MMA](https://manage.theguardian.com/help-centre).
The content is stored in [Salesforce Knowledge](https://gnmtouchpoint.lightning.force.com/lightning/o/Knowledge__kav/list?filterName=00B5I000003lI1KUAU).

It also holds [a sub-project to import articles into Salesforce from Capi](legacy-content-import).  

There's a folder for useful scripts [here](scripts).

The Repo/API has two endpoints:

1. **When publishing an article:**

- `publishContents` function is called
- JSON file for each topic containing all associated articles is produced and stored in a folder in S3 with path `manage-help-content` > `topics`  
- JSON file for the input article is produced and stored in a folder in S3 with path `manage-help-content` > `articles`
- `sitemap.txt` is produced

**2. When removing an article**
