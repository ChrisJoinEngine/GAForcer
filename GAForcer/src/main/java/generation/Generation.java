/**
 * This handles the actual generation management (starts a generation in a multithreaded fashion).
 * @author Christopher Ellis (ChrisJoinEngine)
 */

package generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import runnable.GAForcer;

public class Generation 
{
	private static FitnessCalculator fitness=new FitnessCalculator();
	private static final String SCORE_OUTPUT_FILE="ScoreTracking.txt";
	private static final String INCREMENT_OUTPUT_FILE="UpcomingGeneration.txt";
	private static int NEW_GENERATION_SIZE=10;
	
	private static boolean politeMode=false;
	
	/**
	 * This takes in a list of children and starts the generation in a many threaded fashion. Multiple threads reduce execution
	 * time dramatically.
	 * @param children the list of children to pass in
	 * @param iterations the list of iterations to be used for that generation
	 * @throws InterruptedException occurs in the event of a thread interruption, this should not be thrown
	 */
	public static void startGeneration(ArrayList<GenerationChild> children, int iterations) throws InterruptedException
	{
		ArrayList<Thread> threadPool = new ArrayList<Thread>();
		
		//Starts the threads and puts them in a pool for tracking
		if (!politeMode)
		{
			for (GenerationChild element: children)
			{
				element.setIterations(iterations);
				Thread childThread = new Thread(element);
				threadPool.add(childThread);
				childThread.start();
			}
		
			//For each element in the pool, ensure it has finished. Once you loop, execution has completed
			for (Thread element : threadPool)
			{
				element.join();
			}
		}
		else //block until thread has completed. Coding this killed me a bit inside, but stealth==necessary
		{    //TODO split this for loop into a method for cleaner code
			for (GenerationChild element: children)
			{
				element.setIterations(iterations);
				Thread childThread = new Thread(element);
				threadPool.add(childThread);
				childThread.start();
				childThread.join(); 
			}
		}
		
		//Record the data for tracking purposes
		recordScore(SCORE_OUTPUT_FILE, children);
	}
	
	/**
	 * This writes a score to a file. Worth noting this is a somewhat expensive operation, and should be called 
	 * sparingly (pretty much on termination of a generation, and if you really want to go fast, maybe disable all together)
	 * @param scoreFileName the name of the file to write to 
	 * @param generation the generation to score and write to file
	 */
	public static void recordScore(String scoreFileName, ArrayList<GenerationChild> generation)
	{
		double averageScore=fitness.getGenerationAverageScore(generation);
		generation=fitness.sortByFitness(generation);
		
		try(FileWriter fw = new FileWriter(scoreFileName, true); BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw))
		{
			out.print(getGenerationRepresentation(generation, true));
			out.println("Average score: "+getScoreAsPercentage(averageScore));
			out.println();
		}
		catch (IOException e)
		{
			System.out.println("Could not write to score file!");
		}
	}
	
	/**
	 * This writes a generation to the increment file (basically a save file for the next to-run generation
	 * @param incrementFileName the name of the file to save to
	 * @param generation the generation to save to the file
	 */
	public static void recordIncrementFile(ArrayList<GenerationChild> generation)
	{
		try(FileWriter fw = new FileWriter(INCREMENT_OUTPUT_FILE, false); BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw))
		{
			out.print(getGenerationRepresentation(generation, false));
		}
		catch (IOException e)
		{
			System.out.println("Could not write to increment file!");
		}
	}
	
	/**
	 * This prints a generation out as a string representation
	 * @param generation the generation to print as a string representation
	 * @return the string representation of the generation
	 */
	public static String getGenerationRepresentation(ArrayList<GenerationChild> generation, boolean includeScore)
	{
		String representation="";
		for (GenerationChild element : generation)
		{
			if (!includeScore)
				representation+=element.getStringRepresentation()+System.getProperty("line.separator");
			else 
				representation+=getScoreAsPercentage(element.getScore())+element.getStringRepresentation()+System.getProperty("line.separator");
		}	
		return representation;	
	}
	
	/**
	 * This returns a score as a percent
	 * @param d the score to return as a percent
	 * @return the score as a percentage (String)
	 */
	public static String getScoreAsPercentage(double score)
	{
	    double iterations=GAForcer.getIterationCount();
	    double percent= score/iterations*100;
	    percent=(double) Math.round(percent*100)/100;
	    
		return "%"+String.format("%-4s" , percent+"");
	}
	
	/**
	 * Reads the generation file, and returns what is read as a new array for starting a generation
	 * @return the read in or newly generated generation which may be used for starting
	 */
	public static ArrayList<GenerationChild> loadGenerationFromFile()
	{
		if (GAForcer.reloadRequired())
		{
			GAForcer.setReloadRequired(false);
			return generateRandomizedGeneration();
		}
		
		ArrayList<GenerationChild> firstGeneration=new ArrayList<GenerationChild>();
		File f = new File(INCREMENT_OUTPUT_FILE);
		if(f.exists())
		{ 
			try
			{
				//Found the load file, load in the existing generation
				List<String> lines = Files.readAllLines(Paths.get(f.getAbsolutePath()));
				for (String element : lines)
				{
					firstGeneration.add(new GenerationChild(element));
				}
			}
			catch (Exception e)
			{
				//Some error occurred, generate a blank generation
				System.out.println("Invalid generation file detected, generating from scratch");
				firstGeneration=generateRandomizedGeneration();
			}
		}
		else
		{
			//No save file, generate a blank generation
			System.out.println("No preset generation, making a new one of size: "+NEW_GENERATION_SIZE);
			firstGeneration=generateRandomizedGeneration();
		}
		return firstGeneration;
	}
	
	/**
	 * This generates a blank generation, this is called if no previous generation is loaded, or if a generation
	 * load creates some kind of error.
	 * @return a 'blank' or fully randomized generation
	 */
	public static ArrayList<GenerationChild> generateRandomizedGeneration()
	{
		ArrayList<GenerationChild> blankGeneration=new ArrayList<GenerationChild>();
		for (int i=0; i<NEW_GENERATION_SIZE; i++)
		{
			GenerationChild someGenerationChild=new GenerationChild();
			blankGeneration.add(someGenerationChild);
		}
		return blankGeneration;
	}
	
	/**
	 * This sets the size that a new generation should be
	 * @param newValue the size of a new generation
	 */
	public static void setNewGenerationSize(String newValue)
	{
		try
		{
			NEW_GENERATION_SIZE=Integer.parseInt(newValue);
		}
		catch (Exception e)
		{
			System.out.println("Invalid generation size value detected. Terminating");
			System.exit(0);
		}
	}
	
	/**
	 * This returns the size of a new generation
	 * @return newValue the size of a new generation
	 */
	public static int getNewGenerationSize()
	{
		return NEW_GENERATION_SIZE;
	}
	
	/**
	 * This determines if generations should operate in polite mode or not (single threaded mode)
	 * @param newValue the new boolean value to set polite mode to
	 */
	public static void setPoliteMode(boolean newValue)
	{
		politeMode=newValue;
	}
}
