const titleKeywordWeighting = 5;

function translateBranchAndChildrenToText(branch) {
	if (branch.element === "text") {
		return "" + branch.content;
	} else {
		return branch.content
			.map((subElement) => translateBranchAndChildrenToText(subElement))
			.join(" ");
	}
}

function getWeightedTitlePrefix(title) {
	return (title + " ").repeat(titleKeywordWeighting);
}

function generateConcatenatedPlainText(sourceJson) {
	return (
		getWeightedTitlePrefix(sourceJson.title) +
		sourceJson.body
			.map((subElement) => translateBranchAndChildrenToText(subElement))
			.join(" ")
	);
}

function computeKeywordsWithOccurence(sourceJson, stopwords) {
	const concatenatedText = generateConcatenatedPlainText(sourceJson);
	const index = {};
	const words = getListOfWordsExcludingStopWords(concatenatedText, stopwords);

	for (const word of words) {
		if (index[word]) {
			index[word]++;
		} else {
			index[word] = 1;
		}
	}

	return index;
}

function getListOfWordsExcludingStopWords(articleText, stopwords) {
	return (articleText || "")
		.replace(/[\/\n+()+?@*"'“”’,.:;\d]+/g, " ")
		.toLowerCase()
		.trim()
		.split(/\s+/)
		.filter((w) => !stopwords.includes(w));
}

function indexAllDocuments(documents, stopwords) {
	const documentIndex = {};
	for (const [documentName, document] of Object.entries(documents)) {
		documentIndex[documentName] = computeKeywordsWithOccurence(
			document,
			stopwords
		);
	}

	return documentIndex;
}

function indexKeywordsFromDocuments(documentIndex) {
	const keywordIndex = {};
	for (const [documentName, keywords] of Object.entries(documentIndex)) {
		for (const [keyword, keywordCount] of Object.entries(keywords)) {
			if (!keywordIndex[keyword]) {
				keywordIndex[keyword] = {};
			}
			keywordIndex[keyword][documentName] = keywordCount;
		}
	}

	return keywordIndex;
}

function breakDownSearchText(searchText, stopWords) {
	return getListOfWordsExcludingStopWords(searchText, stopWords);
}

function getKeywordIndexesForSearchKeywords(searchKeywords, keywordIndex) {
	const subsetOfKeywordIndexes = {};
	for (const word of searchKeywords) {
		if (keywordIndex[word]) {
			subsetOfKeywordIndexes[word] = keywordIndex[word];
		}
	}
	return subsetOfKeywordIndexes;
}

function calculateCumulativeRelevanceScores(keywordIndexForSearchTerms) {
	const cumulativeRelevanceScorePerDocument = {};
	for (const [, relevanceScoreForOneKeyword] of Object.entries(
		keywordIndexForSearchTerms
	)) {
		for (const [documentName, score] of Object.entries(
			relevanceScoreForOneKeyword
		)) {
			if (!cumulativeRelevanceScorePerDocument[documentName]) {
				cumulativeRelevanceScorePerDocument[documentName] = 0;
			}
			cumulativeRelevanceScorePerDocument[documentName] += score;
		}
	}

	return cumulativeRelevanceScorePerDocument;
}

function sortDocumentsByRelevance(cumulativeScoresPerDocument) {
	const compareDocsByScore = (a, b) => b[1] - a[1];
	return Object.entries(cumulativeScoresPerDocument)
		.sort(compareDocsByScore)
		.map((x) => x[0]);
}

module.exports = {
	generateConcatenatedPlainText,
	computeKeywordsWithOccurence,
	indexAllDocuments,
	indexKeywordsFromDocuments,
	breakDownSearchText,
	getKeywordIndexesForSearchKeywords,
	calculateCumulativeRelevanceScores,
	sortDocumentsByRelevance,
	getWeightedTitlePrefix,
};
