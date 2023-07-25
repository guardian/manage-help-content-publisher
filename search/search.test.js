/*
 * INDEXING (Pre compiled)
 * --------
 * Load in JSON and produce concatenated text to analyse
 * Analyse each text to generate key words with frequency of occurence weighting
 * Build an index per file of each keyword with weighting
 * Build an index of keywords with weighting in each file
 *
 * SEARCHING (on fly)
 * ---------
 * Take search text and break down into key words (remove stops)
 * Calculate relevance score of each document per key word
 * Calculate cumulative relevance score of each document and rank
 *
 */

const {
	generateConcatenatedPlainText,
	computeKeywordsWithOccurence,
	indexAllDocuments,
	indexKeywordsFromDocuments,
	breakDownSearchText,
	getKeywordIndexesForSearchKeywords,
	calculateCumulativeRelevanceScores,
	sortDocumentsByRelevance,
	getWeightedTitlePrefix,
} = require("./search");

const titleKeywordWeighting = 5;

const exampleDocuments = {
	doc1: {
		body: [
			{
				element: "h2",
				content: [
					{
						element: "text",
						content: "keep calm,,, and Carry on, MY old lamb",
					},
				],
			},
		],
	},
	doc2: {
		body: [
			{
				element: "h2",
				content: [{ element: "text", content: "Mary" }],
			},
			{
				element: "h2",
				content: [{ element: "text", content: `haD\n	\n\n a little\n lamb` }],
			},
			{
				element: "h2",
				content: [
					{
						element: "text",
						content: "had a liTTle		lamb     that Ate all the apples",
					},
				],
			},
		],
	},
	doc3: {
		body: [
			{
				element: "h2",
				content: [{ element: "text", content: "apples" }],
			},
			{
				element: "h2",
				content: [{ element: "text", content: "pears" }],
			},
			{
				element: "h2",
				content: [{ element: "text", content: "bananas and apples" }],
			},
			{
				element: "h2",
				content: [
					{ element: "text", content: "but apples are best, pears next" },
				],
			},
		],
	},
};

describe("I can build an index", () => {
	it("Can generate a concatenated plain text based on JSON input", () => {
		const concatenatedText =
			"Dev Like Dev Like Dev Like Dev Like Dev Like I like to write code. I eat sandwhiches";
		const sourceJson = {
			title: "Dev Like",
			body: [
				{
					element: "h2",
					content: [{ element: "text", content: "I like to write code." }],
				},
				{
					element: "p",
					content: [
						{
							element: "text",
							content: "I eat sandwhiches",
						},
					],
				},
			],
		};

		expect(generateConcatenatedPlainText(sourceJson)).toEqual(concatenatedText);
	});

	it("Can calculate key words with score in a document, exluding stop words", () => {
		const sourceJson = {
			body: [
				{
					element: "h2",
					content: [{ element: "text", content: "apples" }],
				},
				{
					element: "h2",
					content: [{ element: "text", content: "pEArs himself" }],
				},
				{
					element: "h2",
					content: [
						{ element: "text", content: "bananas and apples are the same" },
					],
				},
				{
					element: "h2",
					content: [
						{
							element: "text",
							content: "but just apples are best, pears very next",
						},
					],
				},
			],
		};

		// const keywordIndex = {
		//     "apples": 3,
		//     "pears": 2,
		//     "bananas": 1,
		//     "best": 1,
		//     "next": 1
		// }

		const stopwords = ["just", "very", "and"];
		const computedIndex = computeKeywordsWithOccurence(sourceJson, stopwords);

		expect(computedIndex.apples).toEqual(3);
		expect(computedIndex.pears).toEqual(2);
		expect(computedIndex.bananas).toEqual(1);
		expect(computedIndex.best).toEqual(1);
		expect(computedIndex.next).toEqual(1);
		expect(computedIndex.just === undefined).toBe(true);
		expect(computedIndex.very === undefined).toBe(true);
		expect(computedIndex.and === undefined).toBe(true);
		expect(computedIndex.elephants === undefined).toBe(true);
	});

	it("generates a collated index of words per file", () => {
		const stopWords = [];
		const documentIndex = indexAllDocuments(exampleDocuments, stopWords);
		expect(!!documentIndex.doc1).toBe(true);
		expect(documentIndex.doc2.lamb).toBe(2);
	});

	it("generates a keyword based lookup index of documents", () => {
		const stopWords = [];
		const documentIndex = indexAllDocuments(exampleDocuments, stopWords);
		const keywordIndex = indexKeywordsFromDocuments(documentIndex);
		expect(keywordIndex.apples.doc3).toBe(3);
	});

	it("generates weight title prefix", () => {
		const title = "Hi";
		const weightedTitle = getWeightedTitlePrefix(title);
		const expectedWeightTitle = "Hi Hi Hi Hi Hi ";

		expect(weightedTitle).toBe(expectedWeightTitle);
	});
});

describe("I can search", () => {
	it("Can take search text and break down into key words (without stop words)", () => {
		const searchText = "how, now Brown cow";
		const stopWords = ["how"];
		const searchKeywords = breakDownSearchText(searchText, stopWords);
		expect(searchKeywords).toEqual(["now", "brown", "cow"]);
	});

	it("Can calculate relevance score of each document per key word", () => {
		const stopWords = [];
		const documentIndex = indexAllDocuments(exampleDocuments, stopWords);
		const keywordIndex = indexKeywordsFromDocuments(documentIndex);

		const searchText = "lamb apples";
		const searchKeywords = breakDownSearchText(searchText, stopWords);

		const justIndexForSearchKeywords = getKeywordIndexesForSearchKeywords(
			searchKeywords,
			keywordIndex
		);

		expect(justIndexForSearchKeywords).toEqual({
			apples: {
				doc2: 1,
				doc3: 3,
			},
			lamb: {
				doc1: 1,
				doc2: 2,
			},
		});
	});

	it("Calculate cumulative relevance score of each document", () => {
		const stopWords = [];
		const documentIndex = indexAllDocuments(exampleDocuments, stopWords);
		const keywordIndex = indexKeywordsFromDocuments(documentIndex);

		const searchText = "lamb aPPles";
		const searchKeywords = breakDownSearchText(searchText, stopWords);

		const justIndexForSearchKeywords = getKeywordIndexesForSearchKeywords(
			searchKeywords,
			keywordIndex
		);

		/*
		 * scores = {
		 *	doc1: 1,
		 *	doc3: 7,
		 * }
		 */

		const cumulativeScores = calculateCumulativeRelevanceScores(
			justIndexForSearchKeywords
		);

		expect(cumulativeScores.doc1).toBe(1);
		expect(cumulativeScores.doc2).toBe(3);
		expect(cumulativeScores.doc3).toBe(3);
	});

	it("Ranks documents by relevance score", () => {
		const stopWords = [];
		const documentIndex = indexAllDocuments(exampleDocuments, stopWords);
		const keywordIndex = indexKeywordsFromDocuments(documentIndex);

		const searchText = "lamb aPPles";
		const searchKeywords = breakDownSearchText(searchText, stopWords);

		const justIndexForSearchKeywords = getKeywordIndexesForSearchKeywords(
			searchKeywords,
			keywordIndex
		);

		const cumulativeScoresPerDocument = calculateCumulativeRelevanceScores(
			justIndexForSearchKeywords
		);

		const rankedDocuments = sortDocumentsByRelevance(
			cumulativeScoresPerDocument
		);

		expect(rankedDocuments[0]).toEqual("doc2");
		expect(rankedDocuments[2]).toEqual("doc1");
	});
});
