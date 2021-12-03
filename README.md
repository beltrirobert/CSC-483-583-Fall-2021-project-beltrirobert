# CSC-483-583-Fall-2021-project-beltrirobert
Building (a part of) Watson by Robert Beltri

IBM’s Watson is a Question Answering (QA) system that "can compete at the human champion level in real time on the TV quiz show, Jeopardy."

This project uses the following data:
	• 100 questions from previous Jeopardy games, whose answers appear as Wikipediapages. The questions are listed in a single file, with 4 lines per question, in thefollowing format: CATEGORY CLUE ANSWER NEWLINE. For example:
		
		NEWSPAPERS
		The dominant paper in our nation’s capital, it’s among the top 10 U.S. papers in circulation
		The Washington Post
	
	• A collection of approximately 280,000 Wikipedia pages, which include the correct answers for the above 100 questions. The pages are stored in 80 files (thus each file contains several thousand pages). Each page starts with its title, encased in double square brackets. For example, BBC’s page starts with "[[BBC]]".

Addressed points:

	First:

		Index the Wikipedia collection (wiki-data) with a state of the art Information Retrieval system (Lucene: http://lucene.apache.org/).
		Each Wikipedia page appears as a separate document in the index.
		
	Second:

		Measure the performance of the Jeopardy system, using mean reciprocal rank (MRR).
		
	Third:

		Replace the scoring function in the system with another.
		
			Set similarity to BM25, doesn't change overall MRR
			Set similarity to Boolean Similarity, lowers overall MRR to .1883

	Fourth:

		Perform an error analysis of the best system.
		
Download the code:
	
	git clone https://github.com/beltrirobert/CSC-483-583-Fall-2021-project-beltrirobert.git
	
Compile the code (from /CSC-483-583-Fall-2021-project-beltrirobert directory):

	mvn compile -e
	
Execute the code (from /CSC-483-583-Fall-2021-project-beltrirobert directory):

	mvn exec:java -P main -e
	
Best score obtained (changing :

	MRR: 0.19373411
	Overall Score: 19.373411%