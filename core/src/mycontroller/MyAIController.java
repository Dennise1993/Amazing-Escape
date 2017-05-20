package mycontroller;

import java.util.HashMap;

import controller.CarController;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;

public class MyAIController extends CarController{

	// How many minimum units the wall is away from the player.
	private int wallSensitivity = 2;
	private int reverseSensitivity = 3;
	private int aheadSensitivity = 1;
	private boolean hasReversed = false;
	private boolean isFollowingWall = false; // This is initialized when the car sticks to a wall.
	private WorldSpatial.RelativeDirection lastTurnDirection = null; // Shows the last turn direction the car takes.
	private boolean isTurningLeft = false;
	private boolean isTurningRight = false; 
	private WorldSpatial.Direction previousState = null; // Keeps track of the previous state
	
	// Car Speed to move at
	private final float CAR_SPEED = 3;
	
	// Offset used to differentiate between 0 and 360 degrees
	private int EAST_THRESHOLD = 3;
	
	
	//================reverse=====================
	private boolean isReversing = false;
	private float previousAngle = 0;
	
	public MyAIController(Car car) {
		super(car);
	}

	@Override
	public void update(float delta) {
		
		// Gets what the car can see
		HashMap<Coordinate, MapTile> currentView = getView();
		
		/* Check if the car's (state) orientation has changed or not
		 * if it has changed, stop turning and assign current orientation 
		 * to previous state (attributes in this AIController class)
		 */		
		checkStateChange();


		// If you are not following a wall initially, find a wall to stick to!
		if(!isFollowingWall){
			if(getVelocity() < CAR_SPEED){// Initial Speed is 0
				applyForwardAcceleration(); //Change the car's attribute "accelerating" to "true"
			}
			// Get the car current orientation, and if it is not North,
			// then turn towards the north in wise clock way
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
				applyLeftTurn(getOrientation(),delta);// call method in Car (car.getOrientation)
			}
			//Check if there is a wall on the north within 2 units of tiles(WallSensitivity)
			
			if(checkNorth(currentView, wallSensitivity)){
				// Turn right until we go back to east!
				if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					applyRightTurn(getOrientation(),delta);
				}
				else{
					isFollowingWall = true;
				}
			}
		}
		// Once the car is already stuck to a wall, apply the following logic
		else{
			
			// Readjust the car if it is misaligned.
			readjust(lastTurnDirection,delta);
			
			if(isTurningRight &&!isReversing ){
				applyRightTurn(getOrientation(),delta);
				//System.out.println("turnRight!!!======");
			}
			else if(isTurningLeft){
				// Apply the left turn if you are not currently near a wall.
				if(!checkFollowingWall(getOrientation(),currentView, wallSensitivity)){
					applyLeftTurn(getOrientation(),delta);
					//System.out.println("turnLeft!!!======");
				}
				else{
					isTurningLeft = false;
					//System.out.println("has wall on the left, so stop turnLeft!!!======");
				}
				
			}
			//3-point turn
			else if(isTurningRight&&isReversing) {
				//System.out.println("general situation!!!======previousangle: "+previousAngle+", currentangle: "+ getAngle());
				if(getAngle()!=previousAngle && Math.abs(getAngle() - previousAngle)<315 && checkWallAhead(getOrientation(),currentView, aheadSensitivity)) {
					applyReverseAcceleration();
					//System.out.println("currentOrientation: "+getOrientation());
					//System.out.println("back off, reverse!!!======previousangle: "+previousAngle+", currentangle: "+ getAngle());
					hasReversed = true;
					//System.out.println("1meter has wall or not: "+checkWallAhead(getOrientation(),currentView, aheadSensitivity));
				}
				else if(!checkWallAhead(getOrientation(),currentView, 1)&&hasReversed){
					isReversing = false;
					hasReversed = false;
					//System.out.println("=========wall behind, stop reverse");
				}
				else{
					applyRightTurn(getOrientation(),delta);
					//System.out.println("back off, turn right!!!======");
				}
				
			}
			// Try to determine whether or not the car is next to a wall.
			else if(checkFollowingWall(getOrientation(),currentView, wallSensitivity)){
				// Maintain some velocity
				if(getVelocity() < CAR_SPEED){
					//System.out.println("speed up!!!======");
					applyForwardAcceleration();
				}
				// If there is wall right within 2 units tiles, and 
				// there is wall ahead within 2 units tiles, back off!
				if(checkWallAhead(getOrientation(),currentView, wallSensitivity)
						&& !checkWallRight(getOrientation(),currentView, reverseSensitivity)){
					//System.out.println("U turn!!!======");
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					isTurningRight = true;				
					
				}
				else if(checkWallAhead(getOrientation(),currentView, reverseSensitivity)
						&& checkWallRight(getOrientation(),currentView, reverseSensitivity)){
					//System.out.println("3-point turn!!!======");
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					isTurningRight = true;	
					isReversing = true;
					previousAngle = getAngle();
					//System.out.println("previousangle: "+ getAngle());
				}

			}
			// This indicates that I can do a left turn if I am not turning right
			else{
				//System.out.println("no wall on the left side!!!======");
				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
				isTurningLeft = true;
			}
		}
	}
	
	/**
	 * Readjust the car to the orientation we are in.
	 * @param lastTurnDirection
	 * @param delta
	 */
	private void readjust(WorldSpatial.RelativeDirection lastTurnDirection, float delta) {
		if(lastTurnDirection != null){
			if(!isTurningRight && lastTurnDirection.equals(WorldSpatial.RelativeDirection.RIGHT)){
				adjustRight(getOrientation(),delta);
			}
			else if(!isTurningLeft && lastTurnDirection.equals(WorldSpatial.RelativeDirection.LEFT)){
				adjustLeft(getOrientation(),delta);
			}
		}
		
	}
	
	/**
	 * Try to orient myself to a degree that I was supposed to be at if I am
	 * misaligned.
	 */
	private void adjustLeft(WorldSpatial.Direction orientation, float delta) {
		
		switch(orientation){
		case EAST:
			if(getAngle() > WorldSpatial.EAST_DEGREE_MIN+EAST_THRESHOLD){
				turnRight(delta);
			}
			break;
		case NORTH:
			if(getAngle() > WorldSpatial.NORTH_DEGREE){
				turnRight(delta);
			}
			break;
		case SOUTH:
			if(getAngle() > WorldSpatial.SOUTH_DEGREE){
				turnRight(delta);
			}
			break;
		case WEST:
			if(getAngle() > WorldSpatial.WEST_DEGREE){
				turnRight(delta);
			}
			break;
			
		default:
			break;
		}
		
	}

	private void adjustRight(WorldSpatial.Direction orientation, float delta) {
		switch(orientation){
		case EAST:
			if(getAngle() > WorldSpatial.SOUTH_DEGREE && getAngle() < WorldSpatial.EAST_DEGREE_MAX){
				turnLeft(delta);
			}
			break;
		case NORTH:
			if(getAngle() < WorldSpatial.NORTH_DEGREE){
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if(getAngle() < WorldSpatial.SOUTH_DEGREE){
				turnLeft(delta);
			}
			break;
		case WEST:
			if(getAngle() < WorldSpatial.WEST_DEGREE){
				turnLeft(delta);
			}
			break;
			
		default:
			break;
		}
		
	}
	
	/**
	 * Checks whether the car's state has changed or not, stops turning if it
	 *  already has.
	 */
	private void checkStateChange() {

		if(previousState == null){
			previousState = getOrientation();
		}
		else{
			//System.out.println("checkStateChange pre: "+previousState);
			//System.out.println("checkStateChange currentorientation: "+getOrientation());
			if(previousState != getOrientation()){
				if(isTurningLeft){
					isTurningLeft = false;
				}
				if(isTurningRight){
					isTurningRight = false;
				}
				previousState = getOrientation();
			}
		}
	}
	
	/**
	 * Turn the car counter clock wise (think of a compass going counter clock-wise)
	 */
	private void applyLeftTurn(WorldSpatial.Direction orientation, float delta) {
		switch(orientation){
		case EAST:
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				turnLeft(delta);
			}
			break;
		case NORTH:
			if(!getOrientation().equals(WorldSpatial.Direction.WEST)){
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
				turnLeft(delta);
			}
			break;
		case WEST:
			if(!getOrientation().equals(WorldSpatial.Direction.SOUTH)){
				turnLeft(delta);
			}
			break;
		default:
			break;
		
		}
		
	}
	
	/**
	 * Turn the car clock wise (think of a compass going clock-wise)
	 */
	private void applyRightTurn(WorldSpatial.Direction orientation, float delta) {
		switch(orientation){
		case EAST:
			if(!getOrientation().equals(WorldSpatial.Direction.SOUTH)){
				turnRight(delta);
			}
			break;
		case NORTH:
			if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
				turnRight(delta);
			}
			break;
		case SOUTH:
			if(!getOrientation().equals(WorldSpatial.Direction.WEST)){
				turnRight(delta);
			}
			break;
		case WEST:
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				turnRight(delta);
			}
			break;
		default:
			break;
		
		}
		
	}

	/**
	 * Check if you have a wall in front of you!
	 * @param orientation the orientation we are in based on WorldSpatial
	 * @param currentView what the car can currently see
	 * @return
	 */
	private boolean checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView, int sensitivity){
		switch(orientation){
		case EAST:
			return checkEast(currentView, sensitivity);
		case NORTH:
			return checkNorth(currentView, sensitivity);
		case SOUTH:
			return checkSouth(currentView, sensitivity);
		case WEST:
			return checkWest(currentView, sensitivity);
		default:
			return false;
		
		}
	}
	
	/**
	 * Check if the wall is on your left hand side given your orientation
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private boolean checkFollowingWall(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView, int sensitivity) {
		
		switch(orientation){
		case EAST:
			return checkNorth(currentView, sensitivity);
		case NORTH:
			return checkWest(currentView, sensitivity);
		case SOUTH:
			return checkEast(currentView, sensitivity);
		case WEST:
			return checkSouth(currentView, sensitivity);
		default:
			return false;
		}
		
	}
	
	private boolean checkWallRight(WorldSpatial.Direction orientation,HashMap<Coordinate, MapTile> currentView, int sensitivity) {
		
		switch(orientation){
		case EAST:
			return checkSouth(currentView, sensitivity);
		case NORTH:
			return checkEast(currentView, sensitivity);
		case SOUTH:
			return checkWest(currentView, sensitivity);
		case WEST:
			return checkNorth(currentView, sensitivity);
		default:
			return false;
		}
	
	}
	
	private boolean checkWallBehind(WorldSpatial.Direction orientation,HashMap<Coordinate, MapTile> currentView, int sensitivity) {
		
		switch(orientation){
		case EAST:
			return checkWest(currentView, sensitivity);
		case NORTH:
			return checkSouth(currentView, sensitivity);
		case SOUTH:
			return checkNorth(currentView, sensitivity);
		case WEST:
			return checkEast(currentView, sensitivity);
		default:
			return false;
		}
	
	}

	/**
	 * Method below just iterates through the list and check in the correct coordinates.
	 * i.e. Given your current position is 10,10
	 * checkEast will check up to wallSensitivity amount of tiles to the right.
	 * checkWest will check up to wallSensitivity amount of tiles to the left.
	 * checkNorth will check up to wallSensitivity amount of tiles to the top.
	 * checkSouth will check up to wallSensitivity amount of tiles below.
	 */
	public boolean checkEast(HashMap<Coordinate, MapTile> currentView, int sensitivity){
		// Check tiles to my right
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= sensitivity; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
			if(tile.getName().equals("Wall")){
				return true;
			}
		}
		return false;
	}
	
	public boolean checkWest(HashMap<Coordinate,MapTile> currentView, int sensitivity){
		// Check tiles to my left
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= sensitivity; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
			if(tile.getName().equals("Wall")){
				return true;
			}
		}
		return false;
	}
	

	/**
	 * Check if there is a wall within 2 units tiles on the north
	 * @param currentView: HashMap<Coordinate,MapTile>
	 * @return true if there is a wall
	 */
	public boolean checkNorth(HashMap<Coordinate,MapTile> currentView, int sensitivity){
		// Check tiles to towards the top
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= sensitivity; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
			if(tile.getName().equals("Wall")){
				return true;
			}
		}
		return false;
	}
	
	public boolean checkSouth(HashMap<Coordinate,MapTile> currentView, int sensitivity){
		// Check tiles towards the bottom
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= sensitivity; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
			if(tile.getName().equals("Wall")){
				return true;
			}
		}
		return false;
	}
	
	
	

}
