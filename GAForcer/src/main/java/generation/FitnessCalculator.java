/**
 * This file manages fitness calculations, generation changes (such as selection, crossover, and mutation)
 * and a few other generic functions related to score. Some of the behavior is split across the Generation
 * class file for convenience purposes (maybe not a great choice, but it's a choice I made).
 *  @author Christopher Ellis (ChrisJoinEngine)
 */
package generation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class FitnessCalculator 
{
	private final static int TOTAL_ADJUSTABLE_VARIABLES=5; //This is total variables that may be mutated--upper, lower, number, minL, maxL (5)
	private static int MUTATION_CHANCE=1; //If the selected number is less than this, mutate
	private final static int MUTATION_MAX_CHANCE=1000; //the maximum chance, i.e. out of what number if the mutation chance is selected
	private Random rand=new Random();
	
	/**
	 * Returns a Generation list sorted by total score in descending order.
	 * @param generation the generation to sort
	 * @return  the sorted generation in descending order
	 */
	public ArrayList<GenerationChild> sortByFitness(ArrayList<GenerationChild> generation)
	{
		Collections.sort(generation, new Comparator<GenerationChild>()
		{
	        @Override public int compare(GenerationChild childOne, GenerationChild childTwo) 
	        {
	            return (int) (childTwo.getScore() - childOne.getScore()); 
	        }
	    });
		return generation;
	}
	
	/**
	 * This returns the average score for a generation
	 * @param generation the generation to compute the score for
	 * @return the average score as a double for a generation
	 */
	public double getGenerationAverageScore(ArrayList<GenerationChild> generation)
	{
		double score=0.0;
		for (GenerationChild element : generation)
		{
			score+=element.getScore();
		}	
		return (score/generation.size());
	}
	
	/**
	 * This applies a mutation to the members of a generation with a selected percentage. The mutation
	 * is applied randomly to one of the three fields and equates to a reroll of that field value.
	 * @param generationOld the generation to make modifications to
	 * @param generationNew the new generation where modifications will be stored
	 * @param percentChanceOfMutation the percent chance to reroll a stat
	 * @return the updated new generation after any mutations have been applied
	 */
	public ArrayList<GenerationChild> applyMutation(ArrayList<GenerationChild> generationOld)
	{		
		ArrayList<GenerationChild> generationNew=generationOld;
		for (int i=0; i<generationOld.size(); i++)
		{
			GenerationChild element=generationOld.get(i);
			Double[] asVector=element.getVectorRepresentation();
			
			int TOTAL_MIN=element.getMinLength();
			int TOTAL_MAX=element.getMaxlength();
						
			for (int j=0; j<3; j++)
			{
				if (shouldMutate())
					asVector[j]=rand.nextDouble();
			}
			//Handle length selections, these must be done separately as their range is not related to bayesian selection rate
			if (shouldMutate())
				asVector[3]=(double) (rand.nextInt((TOTAL_MAX-TOTAL_MIN)+1)+TOTAL_MIN);
			
			int minimumValue=(int) Math.round(asVector[3]);
			
			if (shouldMutate())
				asVector[4]=(double) (rand.nextInt((TOTAL_MAX-minimumValue)+1)+minimumValue);

			//Transfer the changes to the new generation
			generationNew.set(i, new GenerationChild(asVector));
		}		
		return generationNew;
	}
	
	/**
	 * This calls the validate method on all children in a generation. That method just ensures that the minimum
	 * string length is shorter than the maximum string length to avoid unforseen side-effects.
	 * @param toValidate the generation to validate
	 */
	private void validateGeneration(ArrayList<GenerationChild> toValidate)
	{
		for (GenerationChild element : toValidate)
		{
			element.validate();
		}
	}
	
	/**
	 * This calculates if a mutation should be performed with the above set percent chance (based on constants).
	 * The default was 1/1000 chance. This method returns true if the selected chance is selected, else false.
	 * @return true if the mutation chance has occurred, otherwise false
	 */
	private boolean shouldMutate()
	{
		int randNumber=rand.nextInt(MUTATION_MAX_CHANCE);
		if (randNumber<=MUTATION_CHANCE)
			return true;
		else
			return false;
	}
	
	/**
	 * This performs the crossover operation on a generation
	 * @param generationOld the old generation to perform the crossover on
	 * @param parent1 the first parent of the crossover
	 * @param parent2 the second parent of the crossover
	 * @return a tuple with the resulting two children from the crossover
	 */
	public GenerationChild[] crossover(ArrayList<GenerationChild> generationOld, int parent1, int parent2)
	{
		Double[] generationParent1=generationOld.get(parent1).getVectorRepresentation();
		Double[] generationParent2=generationOld.get(parent2).getVectorRepresentation();
		Double[] builderVectorFirstChild=new Double[TOTAL_ADJUSTABLE_VARIABLES];
		Double[] builderVectorSecondChild=new Double[TOTAL_ADJUSTABLE_VARIABLES];
				
		int selectedPositionOfCrossover=rand.nextInt(TOTAL_ADJUSTABLE_VARIABLES-1)+1; //Select the point of mutation
		
		for (int i=0; i<selectedPositionOfCrossover; i++)
		{
			builderVectorFirstChild[i]=generationParent1[i];
			builderVectorSecondChild[i]=generationParent2[i];
		}
		for (int i=selectedPositionOfCrossover; i<generationParent1.length; i++)
		{
			builderVectorFirstChild[i]=generationParent2[i];
			builderVectorSecondChild[i]=generationParent1[i];
		}
			
		GenerationChild[] newChildren=new GenerationChild[2];
		newChildren[0]=new GenerationChild(builderVectorFirstChild);
		newChildren[1]=new GenerationChild(builderVectorSecondChild);
		
		return newChildren;
	}
	

	/**
	 * This implements a roulette wheel selection, I based it on David Goldberg's algorithm in Genetic Algrithms. Good book. Page 63.
	 * @param generationOld the old generation to modify
	 * @param generationNew the generation to add roulette selected children 
	 * @return the newer generation with the number of Children designated to be preserved
	 * 
	 * Okay, maybe not intuitive, so a quick rundown. Select a random percentage of the total score (if the total score
	 * is 100, maybe you select 75 as your random sum). Iterate through the generation list and add each child's score
	 * until you are at or surpassed your number. This creates a weighted random selection. It's maybe not so intuitive, 
	 * but it does not require a sorted list to work.
	 * 
	 * Tested pretty thoroughly, it seems to work. There are other ways to do this (like in how I randomize generations), but I
	 * kind of like this algorithm, and I like building things sometimes over using prebuilt methods. It helps me understand.
	 */
	public GenerationChild rouletteSelection(ArrayList<GenerationChild> generationOld)
	{
		double randomSum=0;
		double partialSum=0;
		int totalFitness=getTotalScore(generationOld)+(generationOld.size()); //tagging in a size addition for zero score scenario
		
		randomSum=rand.nextDouble()*totalFitness;
		for (int i=0; i<generationOld.size(); i++)
		{
			partialSum+=generationOld.get(i).getScore()+1; //giving +1 to every score for zero score case..should not affect accuracy
			if (partialSum>=randomSum)
			{
				return generationOld.get(i);
			}
		}	
		return generationOld.get(0); 
	}

	/**
	 * This computes the total score for a generation
	 * @param generation the generation to compute the total score for
	 * @return the total score for the generation
	 */
	private int getTotalScore(ArrayList<GenerationChild> generation) 
	{
		int sum=0;
		for (GenerationChild element : generation)
		{
			sum+=element.getScore();
		}
		return sum;
	}
	
	/**
	 * This gets the next generation after applying reproduction (weighted selection), crossover, and mutation, in that order.
	 * @param generation the next generation to be used
	 * @return the next generation
	 */
	public ArrayList<GenerationChild> getNextGeneration(ArrayList<GenerationChild> generation)
	{
		//Beware the shallow copies
		ArrayList<GenerationChild> selectionPool=new ArrayList<GenerationChild>();
		ArrayList<GenerationChild> crossoverPool=new ArrayList<GenerationChild>();
		
		//If everything scored zero, we end up with a homogeneous pool, so we regenorate
		if (getTotalScore(generation)==0)
		{
			System.out.println("No members of the generation scored a successful hit...");
			System.out.println("A new blank generation is being used to avoid homogeneous selection pool");
			selectionPool=Generation.generateRandomizedGeneration();
			validateGeneration(selectionPool);
			Generation.recordIncrementFile(selectionPool);
			return selectionPool;
		}
		//1, handle the reproduction phase
		while (selectionPool.size()<generation.size())
			selectionPool.add(rouletteSelection(generation));
		
		//2, handle the crossover
		//Probably unnecessary after reproduction phase, but just to be safe in pairing randomness, we'll shuffle
		Collections.shuffle(selectionPool);
		
		for (int i=0; i<selectionPool.size()-1; i+=2)
		{
			GenerationChild[] crossed=crossover(selectionPool, i, i+1);
			crossoverPool.add(crossed[0]);
			crossoverPool.add(crossed[1]);
		}
		
		//If we have an odd number and one child is unmatched, then add that child back to the completed crossover pool
		if (crossoverPool.size()<selectionPool.size())
			crossoverPool.add(selectionPool.get(selectionPool.size()-1));
		
		//Get the mutated result
		ArrayList<GenerationChild> mutatedPool=applyMutation(crossoverPool);
		
		//Save the result to the start file
		validateGeneration(mutatedPool);
		Generation.recordIncrementFile(mutatedPool);
		
		//Return the mutated result
		return mutatedPool;
	}
	
	/**
	 * This returns the mutation chance as it is currently set
	 * @return the mutation chance as it is currently set
	 */
	public static int getMutationChance()
	{
		return MUTATION_CHANCE;
	}
	
	/**
	 * This updates the mutation chance to a new value
	 * @param newValue the new value to set the mutation chance
	 */
	public static void setMutationChance(String newValue)
	{
		try
		{	
			MUTATION_CHANCE=Integer.parseInt(newValue);
			if (MUTATION_CHANCE<0 || MUTATION_CHANCE>MUTATION_MAX_CHANCE)
			{
				throw new Exception();
			}
		}
		catch (Exception e)
		{
			System.out.println("Invalid mutation chance detected. Terminating.");
			System.exit(0);
		}
	}
}


