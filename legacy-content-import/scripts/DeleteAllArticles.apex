/*
 * Delete all Knowledge articles
 */


// Cannot delete live articles so archive them instead

List<Knowledge__kav> live = [
  SELECT KnowledgeArticleId
  FROM Knowledge__kav
  WHERE PublishStatus = 'Online'
];

for (Knowledge__kav article: live) {
 KbManagement.PublishingService.archiveOnlineArticle(article.KnowledgeArticleId, null);
}


// Now delete archived articles

List<Knowledge__kav> archived = [
  SELECT KnowledgeArticleId
  FROM Knowledge__kav
  WHERE PublishStatus = 'Archived'
];

for (Knowledge__kav article: archived) {
  KbManagement.PublishingService.deleteArchivedArticle(article.KnowledgeArticleId);
}


// Delete draft articles

List<Knowledge__kav> draft = [
  SELECT KnowledgeArticleId
  FROM Knowledge__kav
  WHERE PublishStatus = 'Draft'
];

for (Knowledge__kav article: draft) {
  KbManagement.PublishingService.deleteDraftArticle(article.KnowledgeArticleId);
}
