/**
 * This class is used by all generations to create URLs based on input. URLs are generated in 
 * a probabilistic fashion to allow for rapid dynamic adjustments.
 * @author Christopher Ellis (ChrisJoinEngine)
 */
package urlLibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

public class URLBank
{
	private static String BASE_URL=null;
	private static String BASE_POST=null;
	private static boolean USE_REQUEST_FILE=false;
	private static String END_URL="";
	private static String URL_SPLIT_CHARACTER="^";
	private final static String DEFAULT_ACCEPTABLE_LOWER="abcdefghijklmnopqrstuvwxyz";
	private final static String DEFAULT_ACCEPTABLE_UPPER="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private final static String DEFAULT_ACCEPTABLE_NUMBER="123456789";
	
	private static Random rand=new Random();

	//Enumeration types for draw character possibilities (i.e. character groupings, this makes it easy to add more in the future)
	public enum ProbabilisticDraw
	{
		LOWER, UPPER, NUMBER
	}
	
	/**
	 * This generates a random URL extension based on probability chances provided, it takes a chance of using lower case,
	 * a chance of using upper case character, and a chance of using numbers to create a string of random length between
	 * the minimum and maximum provided lengths. This is done in such a fashion as to allow for rapid and dynamic adjustment
	 * such as in the use of a genetic algorithm (planned). Probabilities do not have to add up to 1 as the sample is normalized.
	 * @param chanceOfLower the chance of using a lower case letter
	 * @param chanceOfUpper the chance of using an upper case letter
	 * @param chanceOfNumber the chance of using a number
	 * @param lengthMin the minimum possible length of a generated string
	 * @param lengthMax the maximum possible length of a generated string
	 * @return a randomized string generated from lower, upper and numbers, based on the input probabilities
	 */
	public static String generateURL(double chanceOfLower, double chanceOfUpper, double chanceOfNumber, int lengthMin, int lengthMax)
	{
		String generatedURLpiece="";
		List<Pair<ProbabilisticDraw, Double>> probabilityList=new ArrayList<Pair<ProbabilisticDraw, Double>>();
		int lengthDifference=lengthMax-lengthMin;
		int selectedLength=0;
		
		//Create pairing of selection type and their probability
		Pair<ProbabilisticDraw, Double> lower=new Pair<ProbabilisticDraw, Double>(ProbabilisticDraw.LOWER,chanceOfLower);
		Pair<ProbabilisticDraw, Double> upper=new Pair<ProbabilisticDraw, Double>(ProbabilisticDraw.UPPER, chanceOfUpper);
		Pair<ProbabilisticDraw, Double> number=new Pair<ProbabilisticDraw, Double>(ProbabilisticDraw.NUMBER, chanceOfNumber);
		
		//Add probabilities to list
		probabilityList.add(lower);
		probabilityList.add(upper);
		probabilityList.add(number);
		
		//Randomly select a length from the ranges of length
		if (lengthDifference==0)
			selectedLength=lengthMin;
		else
			selectedLength=rand.nextInt(lengthDifference+1)+lengthMin;
		
		EnumeratedDistribution<ProbabilisticDraw> dist=new EnumeratedDistribution<ProbabilisticDraw>(probabilityList);
	
		for (int i=0; i<selectedLength; i++)
		{
			generatedURLpiece+=convertDrawToCharacter((ProbabilisticDraw) dist.sample());
		}
		
		//Slight alteration to allow for appropriate return in the event of Post 10.29.17
		if (!USE_REQUEST_FILE)
			return BASE_URL+generatedURLpiece+END_URL;
		else //Had to add \\ because chosen deliminator is start line in regex
			return BASE_POST.replaceAll("\\"+URL_SPLIT_CHARACTER, generatedURLpiece); 
	}
	
	
	/**
	 * This takes in a probabilistic draw category (such as UPPER) and returns
	 * a character from that draw pool (such as 'A' from UPPER or 'a' from LOWER). The
	 * character returned is selected at random.
	 * @param draw the probabilistic draw pool to return from
	 * @return a randomized character pulled from the passed in draw pool
	 */
	private static char convertDrawToCharacter(ProbabilisticDraw draw)
	{
		switch (draw)
		{
			case LOWER:
				return DEFAULT_ACCEPTABLE_LOWER.charAt(rand.nextInt(DEFAULT_ACCEPTABLE_LOWER.length()));
			case UPPER:
				return DEFAULT_ACCEPTABLE_UPPER.charAt(rand.nextInt(DEFAULT_ACCEPTABLE_UPPER.length()));
			case NUMBER:
				return DEFAULT_ACCEPTABLE_NUMBER.charAt(rand.nextInt(DEFAULT_ACCEPTABLE_NUMBER.length()));
			default:
				return '0';
		}
	}
	
	/**
	 * This returns the base url that is currently set
	 * @return the base url that is currently set
	 */
	public static String getBaseURL()
	{
		return BASE_URL;
	}
	
	/**
	 * This gets the delimiter of a base URL, that is, the character used to split the url into beginning and end portions
	 * @return the delimiter for URL strings
	 */
	public static String getURLDelimiter()
	{
		return URL_SPLIT_CHARACTER;
	}
	
	/**
	 * Returns if post requests are being used instead of GET
	 * @return true if post requests are being used, false otherwise
	 */
	public static boolean getUseRequestFile()
	{
		return USE_REQUEST_FILE;
	}
	
	/**
	 * This sets the base url for generated requests
	 * @param newValue the new value to set it to
	 */
	public static void setBaseURL(String newValue)
	{
		String firstPortion="";
		String secondPortion="";
		int positionOfDelimiter=newValue.indexOf(URL_SPLIT_CHARACTER);
			
		if (positionOfDelimiter==newValue.length());
			newValue=newValue.substring(0, newValue.length()); //lop off last character because user typo
		
		if ((positionOfDelimiter!=-1))
		{
			for (int i=0; i<positionOfDelimiter; i++)
				firstPortion+=newValue.charAt(i);
	
			for (int i=positionOfDelimiter+1; i<newValue.length(); i++)
				secondPortion+=newValue.charAt(i);
			
			BASE_URL=firstPortion;
			END_URL=secondPortion;
		}
		else
		{	
			BASE_URL=newValue;
		}
	}

	/**
	 * This sets the base post request to generate requests for and make
	 * @param postRequest the post request to parse and store
	 */
	public static void setBasePOST(String postRequest) 
	{
		try 
		{
			BASE_POST=new String(Files.readAllBytes(Paths.get(postRequest)));
		} 
		catch (IOException e) 
		{
			System.out.println("Could not parse "+postRequest+" as a post request. Terminating");
			System.exit(0);
		}
	}

	/**
	 * Returns the base post String
	 * @return the base post string
	 */
	public static String getBasePOST() 
	{
		return BASE_POST;
	}
	
	/**
	 * sets if post requests should be used instead of GET requests
	 * @param newValue the new value for if post requests should be used
	 */
	public static void setUsePost(boolean newValue)
	{
		USE_REQUEST_FILE=newValue;
	}
}
