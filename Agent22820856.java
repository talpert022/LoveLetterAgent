package agents;
import loveletter.*;
import java.util.*;

public class Agent22820856 implements Agent{

	private Random rand;
	private State current;
	private int myIndex;
	State myState;
	int[] cardCounts;
	double[][] playerProbs;
	double[][] bayesProbs;
	int deckSize;
	int[] roundsEliminated;
	Card[] previouslyKnownCards = new Card[4];
	
	public AgentThree(){
		rand = new Random();
	}
	
	public String toString() {return "Jason Bourne";}
	
	public void newRound(State start){
		current = start;
		myIndex = current.getPlayerIndex();
		cardCounts = new int[] {5,2,2,2,2,1,1,1};
		roundsEliminated = new int[] {0, 0, 0, 0};
		playerProbs = new double[4][8];
		bayesProbs = new double[4][8];
		previouslyKnownCards = getKnownCards(current);
		cardCounts[((current.getCard(myIndex)).value())-1]--;
	}
	
	public void see(Action act, State results){
		
		if(!results.eliminated(myIndex))
			updateCardCounts(current, results, act); //updates known cards in the deck
			
		for (int i = 0; i<results.numPlayers(); i++) {
			if (results.eliminated(i))
				roundsEliminated[i]++;
		}
		previouslyKnownCards = getKnownCards(current);
		current = results;
	}
	
	public Action playCard(Card c){
	Action act = null;
    Card play;
    int target;
    Card guess;
    int NUM_ITERATIONS = 0;
    
    updateCardCounts(current, c); // Updates the card counts when its my turn and I draw a card
    setPlayerProbs(); // Updates the probabilities of each player having each card
    setBayesProbs(); /*Updates the probabiliteis of each player having a card using bayes theorem, given the 
    				  *the card they just played
    				  */
    
    while(!current.legalAction(act, c)){
    	
    	Card hold = current.getCard(myIndex);
    	
    	//If my player has the handmaid card, it plays it 90 percent of the time
    	if(c.value() == 4 && rand.nextDouble() < 0.8) play = c;
    	else if (hold.value() == 4 && rand.nextDouble() < 0.8) play = hold;
    	
    	/*If my player has a gaurd, and has 70 percent chance or greater of getting a guess right, it plays that
    	 *card 100 percent of the time
    	 */
    	else if(c.value() == 1 && getHighProb() > 0.7 && rand.nextDouble() < 1.0) play = c;
    	else if(hold.value() == 1 && getHighProb() > 0.7&& rand.nextDouble() < 1.0) play = hold;
    	
    	/*If my player has a baron, and has a 70 percent chance or greater of having a higher card than any player,
    	 *it plays that card 90 percent of the time
    	 */
    	else if(c.value() == 3 && pctHigherCard(hold) > 0.7 && rand.nextDouble() < 1.0) play = c;
    	else if(hold.value() == 3 && pctHigherCard(c) > 0.7 && rand.nextDouble() < 1.0) play = hold;
    	
    	/* If none of the previous conditions are true, the agent will play the higher card 100 percent of the time,
    	 * given that I don't hold a Countess and a card above a Handmaid, in which case I must play the Countess
    	 */
    	else{
    		if(hold.value() >= c.value() && rand.nextDouble() < 1.0)
    			if (!(hold.value() == 7 && c.value() > 4))
    				play = c;
    			else 
    				play = hold;
    		else 
    			if (!(c.value() == 7 && hold.value() > 4))
    				play = hold;
    			else 
    				play = c;
    	}
    	
    	if(play.value() == 1){
    		if (getHighProb() > 0.7)
    			target = gaurdTargetIndex();
    		else 
    			target = highestScorer();
    		guess = gaurdGuess(target);
    	}
    	else if (play.value() == 3){
    		if (play.value() == c.value()){
    			if(pctHigherCard(hold) > 0.7)
    				target = pctHigherCardIndex(hold);
    			else
    				target = highestScorer();
    		} else {
    			if(pctHigherCard(c) > 0.7)
    				target = pctHigherCardIndex(c);
    			else
    				target = highestScorer();
    		}
    		guess = Card.values()[rand.nextInt(7)+1];
    	}
    	else {
    		target = highestScorer();
    		guess = Card.values()[rand.nextInt(7)+1];
    	}
    	
    	//if the Agent infinitly recurses becaue of an illegal move, a random move will be picked
    	if(NUM_ITERATIONS > 100){
    		if(rand.nextDouble()<0.5) play= c;
    		else play = current.getCard(myIndex);
    		target = rand.nextInt(current.numPlayers());
    		guess = gaurdGuess(target);
    	}

		if(NUM_ITERATIONS == 0){
		}
    	try{
    		switch(play){
    		case GUARD:
    			act = Action.playGuard(myIndex, target, guess);
    			break;
    		case PRIEST:
    			act = Action.playPriest(myIndex, target);
    			break;
    		case BARON:  
    			act = Action.playBaron(myIndex, target);
    			break;
    		case HANDMAID:
    			act = Action.playHandmaid(myIndex);
    			break;
    		case PRINCE:  
    			act = Action.playPrince(myIndex, target);
    			break;
    		case KING:
    			act = Action.playKing(myIndex, target);
    			break;
    		case COUNTESS:
    			act = Action.playCountess(myIndex);
    			break;
    		default:
    			act = null;//never play princess
    		}
      	}catch(IllegalActionException e){}
      	NUM_ITERATIONS++;
    }
    return act; 
  }
	
	public void updateCardCounts(State current, State results, Action act) {
		
		/* keeps track of whether a player discarded a card because of a prince 
		 * or other action
		 */ 
		boolean[] justDiscarded = new boolean[] {false, false, false, false};	
		
		Iterator<Card> CardIterator;
		Card justPlayed; 
		int justPlayedCard;
		
		//If the target of an act is protected by the handmaid, no action is performed and no new info is revealed
		if(!current.handmaid(act.target()))
		{
			/* *
			* Update card counts based on info recieved from any player being eliminating a player with the gaurd,
			* as long as we didn't know his discarded hand before
			*/
			if(act.card().value() == 1 && results.eliminated(act.target()) && previouslyKnownCards[act.target()] == null
				&& act.target() != myIndex){
				cardCounts[(results.getDiscards(act.target()).next().value()) - 1]--;
				justDiscarded[act.target()] = true;
				}
				/* *
				* Update card counts based on info recieved from my player playing the priest,
			   * as long as we didn't the revieled card before
			   */
			   if (act.player() == myIndex && act.card().value() == 2 && previouslyKnownCards[act.target()] == null)
			   	   cardCounts[((results.getCard(act.target())).value()) - 1]--;
			
			   /* *
			   * Update card counts based on info recieved from my player trying to eliminate a player with a baron.
			   * If we both don't get eliminated, than I know his card, given I didn't already know it before
			   */
			   if(act.player() == myIndex && act.card().value() == 3 && (!results.eliminated(act.target()) && !results.eliminated(myIndex))
			   	   && previouslyKnownCards[act.target()] == null)
			   cardCounts[((results.getCard(act.target())).value()) - 1]--;
			   /* *
			   * Update card counts based on info recieved from any player eliminating another with a baron, and the player 
			   * who played the card got eliminated, given that I didn't already know their card before hand
			   */
			   if(act.card().value() == 3 && results.eliminated(act.player()) && previouslyKnownCards[act.player()] == null){
			   	   cardCounts[(results.getDiscards(act.player()).next().value()) - 1]--;
			   	   justDiscarded[act.player()] = true;
			   }
			   /* *
			   * Update card counts based on info recieved any player eliminating another with a baron, and the targeted
			   * player gets eliminated, given that I didn't already know their card before hand
			   */
			   if(act.card().value() == 3 && results.eliminated(act.target()) && previouslyKnownCards[act.target()] == null){
			   	   cardCounts[(results.getDiscards(act.target()).next().value()) - 1]--;
			   	   justDiscarded[act.target()] = true;
			   }
			   /* *
			   * Update card counts based on info recieved any player targetting another player with the prince,
			   * and their discarded card isn't previously known
			   */
			   if(act.card().value() == 5 && previouslyKnownCards[act.target()] == null) {
			   	   cardCounts[(results.getDiscards(act.target()).next().value()) - 1]--;
			   	   justDiscarded[act.target()] = true;
			   }
		
			   if(act.card().value() == 5 && act.target() == myIndex) 
			   	   cardCounts[(results.getCard(myIndex).value()) - 1]--;
			   /* *
			   * Update card counts based on info recieved me playing a king and their card not being previously known
			   */
			   if (act.card().value() == 6 && act.player() == myIndex && previouslyKnownCards[act.target()] == null)
			   	   cardCounts[((results.getCard(myIndex)).value()) - 1]--;
		
			   if (act.card().value() == 6 && act.target() == myIndex && previouslyKnownCards[act.target()] == null)
			   	   cardCounts[((results.getCard(myIndex)).value()) - 1]--;
			   /* *
			   * Update card counts based on other players using cards that I didn't previously know
			   */
		}
		if (act.player() != myIndex && previouslyKnownCards[act.player()] != act.card()) {
			int playerIndex = (results.nextPlayer()-1);
			if (playerIndex == -1) playerIndex = 3;
			while(results.eliminated(playerIndex) && roundsEliminated[playerIndex] != 0) { 
				playerIndex--;
				if (playerIndex == -1) playerIndex = 3;
			}
			try {
				if (playerIndex != myIndex && justDiscarded[playerIndex]){
					CardIterator = results.getDiscards(playerIndex);
					justPlayed = CardIterator.next();
					cardCounts[(CardIterator.next().value()) - 1]--;
				}
				else if (playerIndex != myIndex && !justDiscarded[act.player()])
					cardCounts[(results.getDiscards(playerIndex).next().value()) - 1]--; 
		} catch (NoSuchElementException e) {}
		}
	}
	
	public void updateCardCounts(State current, Card c){
		
		cardCounts[(c.value()) - 1]--;
	}
	
	public int[] getCardCounts(){
		return cardCounts;
	}
	
	public void setPlayerProbs(){
		
		//updates cardCounts values that are less than 0 to 0, where updateCardCounts may have made an error
		for(int i = 0; i<cardCounts.length; i++){
			if(cardCounts[i] < 0)
				cardCounts[i] = 0;
		}
		
		//sets deckSize to total number of unknown cards
		int deckSize = 0;
		for(int i = 0; i<cardCounts.length; i++){
			deckSize += cardCounts[i];
		}
		for(int i = 0; i<playerProbs.length; i++){
			if (!(i == myIndex)){                      // Doesn't update playerProbs for my index because card is already known
				if(current.getCard(i) != null){		// If card for a certain player is known, it sets the probability of having that card to 1
					for(int j = 0; j<playerProbs[i].length; j++) {
						if(j != (current.getCard(i).value()) - 1)
							playerProbs[i][j] = 0.0;
						else 
							playerProbs[i][j] = 1.0;
						}
				}
				else{
					for(int j = 0; j<playerProbs[i].length; j++)
						playerProbs[i][j] = (double)cardCounts[j]/deckSize;
				}
			}
			else {
				for(int j = 0; j<playerProbs[i].length; j++) {
						if(j != (current.getCard(i).value()) - 1)
							playerProbs[i][j] = 0.0;
						else 
							playerProbs[i][j] = 1.0;
						}
					}	
				}
		
		for(int i = 0; i<playerProbs.length; i++){ //adjust possibilities based on the player recently playing a countess
			if(!(i == myIndex)){
				if(current.getDiscards(i).hasNext() && current.getDiscards(i).next().value() == 7)
					playerProbs[i] = updateForCountess(playerProbs[i]);
			}
		}
		//sets all probabilties to 0 if the player is eliminated
		for(int i = 0; i<playerProbs.length; i++){
			if(current.eliminated(i)){
				for(int j = 0; j<playerProbs[i].length; j++)
					playerProbs[i][j] = 0.0;
			}
		}
	}
	
	public void setBayesProbs(){
		
		Card recent; 
		double BgivenA, A, B, bayesVal; 
		
		for (int i = 0; i<bayesProbs.length; i++)
		{
			if(current.getDiscards(i).hasNext()){
				recent = current.getDiscards(i).next();
				B = 0.0;
				for(int x = 0; x<bayesProbs[i].length; x++) {
					if (x < recent.value()-1)
						B += (0.8*playerProbs[i][x]);
					if (x > recent.value()-1)
						B += (0.2*playerProbs[i][x]);
					if (x == recent.value()-1)
						B += (0.5*playerProbs[i][x]);
					}
					if(!(i == myIndex) && !current.eliminated(i)){
						for (int j = 0; j<bayesProbs[i].length; j++){
							if (j < recent.value()-1)
								BgivenA = 0.8;
							else if (j > recent.value()-1)
								BgivenA = 0.2;
							else
								BgivenA = 0.5;
							A = playerProbs[i][j];
							bayesVal = (BgivenA*A)/B;
							bayesProbs[i][j] = bayesVal;
						}
					}
			}
			/*
			* if the player doesn't have a discard, -1 is assigned to every probability.
			* This indicates the agent should look at playerProbs instead of bayesProbs
			*/
			else{ 
				for(int j = 0; j<playerProbs[i].length; j++) {
					bayesProbs[i][j] = -1.0;
				}
			}
		}
		//sets all probabilties to 0 if the player is eliminated
		for(int i = 0; i<playerProbs.length; i++){
			if(current.eliminated(i)){
				for(int j = 0; j<playerProbs[i].length; j++)
					bayesProbs[i][j] = 0.0;
			}
		}	
	}
						
	
	public Card[] getKnownCards(State current){
		Card[] vals = new Card[4];
		for(int i = 0; i< current.numPlayers(); i++){
			vals[i] = current.getCard(i);
		}
		return vals;
	}
	
	//redistributes probabilities based on a player playing a countess most recently
	public double[] updateForCountess(double[] countessPlayer){
		double reducedProbs = 0; //variable for accumulating the probabilities of cards below a prince
		int princeOrHigherCards = 0; //gets the number of cards, prince or higher, that have greater than 0 possibilites of being unknown
		for(int i = 0; i < countessPlayer.length; i++) {
			if (i<4){
				reducedProbs += countessPlayer[i];
				countessPlayer[i] = 0.0;
			}
			else if (countessPlayer[i] != 0)
				princeOrHigherCards++;
		}
		
		double addedProbs = (double)reducedProbs/princeOrHigherCards;
		for(int i = 0; i < countessPlayer.length; i++){
			if (i >= 4 && countessPlayer[i] != 0)
				countessPlayer[i] += addedProbs;
		}
		return countessPlayer;
	}
	
	public double getHighProb(){
		
		double highest = 0.0;
		
		for (int i = 0; i<bayesProbs.length; i++){
			if (i != myIndex){
				for(int j = 1; j<bayesProbs[i].length; j++){
					if(bayesProbs[i][j] == -1.0){
						if(playerProbs[i][j] >= highest)
							highest = playerProbs[i][j];
					} else {
						if(bayesProbs[i][j] >= highest)
							highest = bayesProbs[i][j];
					}
				}
			}
		}
		return highest;
	}
	
	public int gaurdTargetIndex(){
		
		double highest = 0.0;
		int index = 0;
		
		for (int i = 0; i<bayesProbs.length; i++){
			if (i != myIndex){
				for(int j = 0; j<bayesProbs[i].length; j++){
					if(bayesProbs[i][j] == -1.0){
						if(playerProbs[i][j] >= highest){
							highest = playerProbs[i][j];
							index = i;
						}
					} else {
						if(bayesProbs[i][j] >= highest){
							highest = bayesProbs[i][j];
							index = i;
						}
					}
				}
			}
		}
		return index;
	}
	
	private Card gaurdGuess(int index){
		
		double highPCT = 0.0;
		int highGuess = 1;
		
		for(int j = 1; j<bayesProbs[index].length; j++){
			if (bayesProbs[index][j] == -1.0){
				if(playerProbs[index][j] > highPCT){
					highPCT = playerProbs[index][j];
					highGuess = j;
				}
			}
			else{
				if(bayesProbs[index][j] > highPCT){
					highPCT = bayesProbs[index][j];
					highGuess = j;
					}
			}
		}
		return Card.values()[highGuess];
	}
	
	public double pctHigherCard(Card c){
		
		double highestPCT = 0.0;
		for (int i = 0; i<bayesProbs.length; i++){
			double highestCurrentPCT = 0.0;
			if (i != myIndex){
				for(int j = 0; j<bayesProbs[i].length; j++){
					if (j < c.value()-1){
						if(bayesProbs[i][j] == -1.0)
							highestCurrentPCT += playerProbs[i][j];
						else
							highestCurrentPCT += bayesProbs[i][j];
					}
				}
				if (highestCurrentPCT >= highestPCT)
					highestPCT = highestCurrentPCT;
			}
		}
		return highestPCT;
	}
	
	public int pctHigherCardIndex(Card c){
		
		double highestPCT = 0.0;
		int highestPCTIndex = 0;
		for (int i = 0; i<bayesProbs.length; i++){
			double highestCurrentPCT = 0.0;
			if (i != myIndex && !current.handmaid(i) && !current.eliminated(i)){
				for(int j = 0; j<bayesProbs[i].length; j++)
					if (j < c.value()-1){
						if(bayesProbs[i][j] == -1.0)
							highestCurrentPCT += playerProbs[i][j];
						else
							highestCurrentPCT += bayesProbs[i][j];
					}
				}
				if (highestCurrentPCT >= highestPCT){
					highestPCT = highestCurrentPCT;
					highestPCTIndex = i;
				}
			}
		return highestPCTIndex;
	}
	
		
	private int highestScorer(){
  		int player = myIndex;
  		int top = -1;
  		int score;
  		for(int i = 0; i<current.numPlayers(); i++){
  			if (i == myIndex)
				continue;
			if (!(allowedAttack(current, i)))
				continue;
			score = current.score(i);
			if (score >= top){
				player = i;
				top = score;
			}
		}
		return player;
	}
	
	
	
	private boolean allowedAttack(State current, int player){
		if (current.eliminated(player))
			return false;
		if (current.handmaid(player))
			return false;
		return true;
	}
				
}
	
		
		
	
			
		
