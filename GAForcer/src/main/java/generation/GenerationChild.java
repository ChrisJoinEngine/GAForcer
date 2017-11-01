/**
 * This is a child of a generation, it maintains its own variables, and upon completion is either preserved
 * to the next generation, mutated, or replaced.
 * @author Christopher Ellis (ChrisJoinEngine)
 */
package generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import database.DBCache;
import parser.WebParser;
import urlLibrary.URLBank;

public class GenerationChild implements Runnable 
{
	private static boolean keyfile=false;
	private static List<String> keyfileData;
	
	private static int TOTAL_MAX=8; //Must be larger than TOTAL_MIN
	private static int TOTAL_MIN=4; //Must be smaller than TOTAL_MAX 
	private static int DELAY=0;
	
	private String name="no name";
	
	private double chanceOfLower;
	private double chanceOfUpper;
	private double chanceOfNumber;
	
	private int minimumLength=0;
	private int maximumLength=0;
	private double score=0.0; 
	private int numberOfRuns=10;

	private Random rand=new Random();
	
	/**
	 * This generates a generation child with very specific parameters (non-random)
	 * @param lowerChance the chance of picking a lower-case letter
	 * @param upperChance the chance of picking an upper-case letter
	 * @param numberChance the chance of picking a number
	 * @param minLength the minimum string length to generate
	 * @param maxLength the maximum string length to generate
	 */
	public GenerationChild(double lowerChance, double upperChance, double numberChance, int minLength, int maxLength, String nameE)
	{	
		chanceOfLower=lowerChance;
		chanceOfUpper=upperChance;
		chanceOfNumber=numberChance;
		minimumLength=minLength;
		maximumLength=maxLength;
		name=nameE;
	}
	
	/**
	 * This normalizes data in this child to the 0.0-1.0 range for better processing and
	 * to avoid conversion errors.
	 */
	private void normalize() 
	{
		double maxRange=chanceOfUpper+chanceOfLower+chanceOfNumber;
		chanceOfUpper=(double) Math.round((chanceOfUpper/maxRange)*100)/100;
		chanceOfLower=(double) Math.round((chanceOfLower/maxRange)*100)/100;
		chanceOfNumber=(double) Math.round((chanceOfNumber/maxRange)*100)/100;
	}

	/**
	 * Used to create a generation child from a vector
	 * @param vector the vector to create the generation child from. Item 1 is chance of lower,
	 * item 2 is chance of upper, item 3 is chance of number, item 4 is minimum length, item 6 is
	 * maximum length. Any vector not complying to length standards will be rejected.
	 */
	public GenerationChild(Double[] vector)
	{
		setValuesFromVector(vector);
	}
	
	/**
	 * Constructor to generate a random (within reason) generation child to start. No arguments needed.
	 */
	public GenerationChild()
	{
		chanceOfLower=rand.nextDouble();
		chanceOfUpper=rand.nextDouble();
		chanceOfNumber=rand.nextDouble();
		minimumLength=rand.nextInt((TOTAL_MAX-TOTAL_MIN)+1)+TOTAL_MIN;
		maximumLength=rand.nextInt((TOTAL_MAX-minimumLength)+1)+minimumLength;
		name="I have no name";
	}
	
	/**
	 * Creates a generation child from a provided string
	 * @param vector the string to create the child from
	 */
	public GenerationChild(String vector)
	{
		//Peel any readability brackets
		String toConvert=vector.replaceAll("\\[", "");
		toConvert=toConvert.replaceAll("\\]", "");
		Double[] creationVector=new Double[5];
		
		String[] newChildValues=toConvert.split(" ");
		for (int i=0; i<newChildValues.length; i++)
		{
			creationVector[i]=Double.parseDouble(newChildValues[i]);
		}
		setValuesFromVector(creationVector);	
	}
	
	/**
	 * This runs the child and computes a final score, score is how many active links were identified in a given
	 * number of iterations.
	 */
	@Override
	public void run() 
	{
		normalize();
		for (int i=0; i<numberOfRuns; i++)
		{
			//If delay flag is set to a non-zero value. Delay is up here for the case where only one request is made.
			if (DELAY!=0)
			{
				try 
				{
					Thread.sleep(DELAY);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
			boolean cacheHit=false;
			String actualURL=null;
			
			String generatedURL=URLBank.generateURL(chanceOfLower, chanceOfUpper, chanceOfNumber, minimumLength, maximumLength);
			
			//Check the cache first
			actualURL=DBCache.checkForRecord(generatedURL);	
			if (actualURL==null)
				actualURL=WebParser.getPage(generatedURL);
			else
				cacheHit=true;
			
			//If we have a hit, add it to the cache and display as needed based on options
			if (actualURL!=null)
			{	
				if (!cacheHit) //If not in the cache already, add it. Brute mode, no recordings
				{
					DBCache.addRecord(generatedURL, actualURL);
				}
				else
				{
					System.out.println("cache hit "+ cacheHit);
				}
				
				if (!keyfile)
				{	
					if (URLBank.getUseRequestFile())
						System.out.println(actualURL+"\ngenerated request: \n"+generatedURL+"\n");
					else
						System.out.println(actualURL+" ("+generatedURL+")");
					score++;
				}
				else 
				{
					for(String element : keyfileData)
					{   
						//If a keyfile element is matched in the URL, then print it
						if (actualURL.contains(element))
						{
							if (URLBank.getUseRequestFile())
								System.out.println(actualURL+"\ngenerated request: \n"+generatedURL+"\n");
							else
								System.out.println(actualURL+" ("+generatedURL+")"+"("+element+")");	
							break;
						}
					}
					score++;
				}
			}
		}
	}
	
	/**
	 * This is a standard getter for this generation child's score, or the value of correctly caluclated links
	 * @return the generation's score
	 */
	public double getScore()
	{
		return score;
	}

	/**
	 * This sets the number of iterations that should be performed. Iterations is the number of URLs to be attempted 
	 * to be fetched.
	 * @param iterations the number of iterations to use for this child.
	 */
	public void setIterations(int iterations) 
	{
		numberOfRuns=iterations;
	}

	/**
	 * This returns the name of this generation child, names are not necessary, but may be helpful for debugging purposes.
	 * @return the name of this generation child
	 */
	public String getName() 
	{
		return name;
	}
	
	/**
	 * This returns the generation child as a vector representation. This
	 * is used for easy mutation and generation propagation. 
	 * @return the vector representation of this generation child
	 */
	public Double[] getVectorRepresentation()
	{
		Double[] vectorRepresentation=new Double[5];
		vectorRepresentation[0]=chanceOfLower;
		vectorRepresentation[1]=chanceOfUpper;
		vectorRepresentation[2]=chanceOfNumber;
		vectorRepresentation[3]=(double) minimumLength;
		vectorRepresentation[4]=(double) maximumLength;
		
		return vectorRepresentation;
	}
	
	/**
	 * This sets a generation child's score, used mainly for testing purposes
	 * @param newScore the new score to set
	 */
	public void setScore(int newScore)
	{
		score=newScore;
	}
	
	/**
	 * This sets the classes values from a vector. It should never directly be called by an external entity
	 * @param vector the vector to set the values to
	 */
	private void setValuesFromVector(Double[] vector)
	{
		if (vector.length!=5)
		{
			System.out.println("Invalid vector recieved! Cannot initialize. Vector length "+vector.length);
			System.exit(1);
		}
		else
		{
			chanceOfLower=vector[0];
			chanceOfUpper=vector[1];
			chanceOfNumber=vector[2];
			minimumLength=(int) Math.round(vector[3]);
			maximumLength=(int) Math.round(vector[4]);
		}
	}
	
	/**
	 * This returns the minimum length possible for this child's string
	 * @return the minimum length possible
	 */
	public int getMinLength()
	{
		return TOTAL_MIN;
	}
	
	/**
	 * In the event of unforeseen side-effect setting minimum length too high,
	 * this method validates and resets it back to the lower value between the lengths
	 */
	public void validate()
	{
		if (minimumLength>maximumLength)
			minimumLength=maximumLength;
	}
	
	/**
	 * This gets a clean string representation of the generation child. It's output as a vector format
	 * for readability.
	 * @return A string representation of this Generation Child
	 */
	public String getStringRepresentation()
	{
		String stringRepresentation="[";
		Double[] asVector=getVectorRepresentation();
		DecimalFormat df=new DecimalFormat("#.00");
		
		for (Double element : asVector)
		{
			stringRepresentation+=df.format(element)+" ";
		}
		stringRepresentation+="]";
		
		return stringRepresentation;
	}
	
	/**
	 * This returns the maximum length possible for this child's string
	 * @return the maximum length possible
	 */
	public int getMaxlength()
	{
		return TOTAL_MAX;
	}
	
	/**
	 * This sets the key file for alerts
	 * @param newValue the value to alert on
	 */
	public static void setKeyFile(String newValue)
	{
		try
		{
			keyfileData = Files.readAllLines(Paths.get(newValue));
			keyfile=true;
		} 
		catch (IOException e)//IOException e)
		{
			System.out.println("Invalid key file. Terminating");
			System.exit(0);
		}
	}
	
	/**
	 * This sets the minimum value for the generated string
	 * @param newValue the new value for the minimum length
	 */
	public static void setStringMin(String newValue)
	{
		try
		{
			TOTAL_MIN=Integer.parseInt(newValue);
		}
		catch (Exception e) 
		{
			System.out.println("Invalid value for total min length! Terminating");
			System.exit(0);
		}
	}
	
	/**
	 * This sets the maximum value for the generated string
	 * @param newValue the new value for the maximum length
	 */
	public static void setStringMax(String newValue)
	{
		try
		{
			TOTAL_MAX=Integer.parseInt(newValue);
		}
		catch (Exception e) 
		{
			System.out.println("Invalid value for total max length! Terminating");
			System.exit(0);
		}
	}
	
	/**
	 * Returns the minimum length allowed for a generation child's generated URL
	 * @return the minimum length for generated URLs allowed
	 */
	public static int getStringMin()
	{
		return TOTAL_MIN;
	}
	
	/**
	 * Returns the maximum length allowed for a generation child's generated URL
	 * @return the maximum length for generated URLs allowed
	 */
	public static int getStringMax()
	{
		return TOTAL_MAX;
	}

	/**
	 * The delay to use between requests made by a child. By default, it's zero.
	 * @param newDelay the delay to set between a child thread's request attempts
	 */
	public static void setDelayFlag(String newDelay) 
	{
		try
		{
			DELAY=Integer.parseInt(newDelay);
		}
		catch (Exception e)
		{
			System.out.println("Invalid delay flag setting. Terminating.");
			System.exit(0);
		}
	}
}
