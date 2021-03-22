package managehelpcontentpublisher

object SalesforceCleaner {
  def cleanCustomFieldName(name: String): String = name.stripSuffix("__c")
}
