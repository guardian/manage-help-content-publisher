# Importing Salesforce Knowledge Articles

## Program
Use the [Importer](src/main/scala/legacycontentimport/importer/Main.scala) program to generate a zip file for Article Import in Salesforce.  
The program takes a list of Capi IDs that are to be imported into Salesforce.  
The generated zip file is described below, along with instructions for how to manually build the file.  

## Import process

The import of a batch of new articles uses a zip file with this structure:

- field-values (csv)
- import-config (properties)
- body
  - article-body 1 (HTML)
  - article-body 2 (HTML)
  - ...
  - article-body N (HTML)

This can be built and submitted with the following steps.

1. Create a CSV file for article import.  
    It should have a title row with fields for `Title` and custom fields.  The names of custom fields are case sensitive.  
    Each row below the title row should be for a single article.
1. Create an HTML file for content of each rich text field.  
    In custom field in CSV, put the relative path to the HTML file.  
    Any unencoded HTML entity in the input will be stored as an encoded entity in Knowledge. 
1. Create a properties file.
1. Add the CSV file, the properties file and the HTML files into a zip file.
1. Import the zip file into Salesforce.  
    Navigate: Setup > Import Articles  
    This page also shows the steps to take for an import.

NB:
* Importing an article with the same URL field value as an article that has already been imported will fail.
  This import method is only for creating new articles.  It can't be used for updates.

References

* https://help.salesforce.com/articleView?id=sf.knowledge_article_importer.htm
