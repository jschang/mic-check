package com.jonschang.speech;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.WordResult;

public class Sphinx4Test {
	public static void main(String[] argv) {
		try {
			Configuration configuration = new Configuration();
			 
	        // Load model from the jar
			System.out.println("Loading acoustic model...");
	        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/acoustic/wsj");
	        // You can also load model from folder
	        // configuration.setAcousticModelPath("file:en-us");
	        System.out.println("Loading dictionary...");
	        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/acoustic/wsj/dict/cmudict.0.6d");
	        System.out.println("Loading language model...");
	        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/language/en-us.lm.dmp");
			
			LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);
			
			// Start recognition process pruning previously cached data.
			System.out.println("Starting recognition...");
			recognizer.startRecognition(true);
			for(int i=0;i<(5*4);i++) {
				System.out.printf(".");
				Thread.currentThread().sleep(250);
			}
			System.out.printf("\n");
			
			System.out.println("Getting result...");
			SpeechResult result = recognizer.getResult();
			System.out.println("Hypothesis: "+result.getHypothesis());
			for(String best : result.getNbest(40)) {
				System.out.print("best: "+best);
			}
			System.out.printf("words: ");
			for(WordResult word : result.getWords()) {
				System.out.printf("%s(%f)", word, word.getConfidence());
			}
			System.out.printf("\n");
			
			System.out.println("Stopping recognition...");
			recognizer.stopRecognition();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
