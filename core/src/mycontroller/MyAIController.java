package mycontroller;

import java.util.HashMap;

import controller.CarController;
import sun.font.TrueTypeFont;
import tiles.GrassTrap;
import tiles.LavaTrap;
import tiles.MapTile;
import tiles.MudTrap;
import tiles.TrapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldDeadEnd;
import world.WorldSpatial;
import world.WorldSpatial.Direction;
import world.WorldSpatial.RelativeDirection;



public class MyAIController extends CarController{
	
	private WorldSpatial.RelativeDirection lastTurnDirection = null;
	HashMap<Coordinate, MapTile> currentView = null;
	private WorldSpatial.Direction previousState = null;
	
	
	private int wallSensitivity = 2;
	private int aheadSensitivity = 1;
	private int rightThreeSen = 2; //**
	
	private int rightReverseSen = 1; //**
	private int rightWallSen = 2;
	
	private int threeAheadSen = 1; //**
	private int threeLeftSen = 1;
	private int aheadRoadWallSen = 1;
	private int parraSensitivity = 1;
	
	private boolean isFollowingWall = false;
	private boolean isStickingWall = false;
	private boolean isWallRight = false;
	private boolean noWallRightTwice = false; //**
	
	//**
	private boolean normTurnLeft = false;
	private boolean normTurnRight = false;
	private boolean threePointTurn = false;
	private boolean reverse = false;
	
	
	private boolean isTurningLeft = false;
	private boolean isTurningRight = false;
	private boolean isReversing = false;
	private boolean hasReversed = false;
	
	private final float CAR_SPEED = 3;
	private int EAST_THRESHOLD = 3;
	
	//**
	float previousAngle;
	
	public MyAIController(Car car) {
		super(car);
	}

	@Override
	public void update(float delta) {
		drive(delta);
		
	}

	private void drive(float delta) {
		currentView = getView();
		checkStateChange();
		System.out.println("current Orientation: "+getOrientation());
		 
		isStickingWall = checkFollowingWall();
		if(isFollowingWall) {
			readjust(delta);
			if(normTurnRight){	
				applyRightTurn(delta); //**reactRightTurn
				System.out.println("turn right");
			}
			else if(normTurnLeft){
				// Apply the left turn if you are not currently near a wall.
				reactLeftTurn(delta);	
				System.out.println("turn left");
			}else if(threePointTurn){
				reactThreePointTurn(delta, previousAngle);
				System.out.println("3point");
			}else if(reverse){
				reactReverse(delta);
			}
			else if(isStickingWall){
				System.out.println("follow wall");
				// Maintain some velocity
				if(getVelocity() < CAR_SPEED){
					applyForwardAcceleration();
				}
				// If there is wall ahead, check dead end type!
				WorldDeadEnd deadEnd = detectDeadEnd();
				
				if(deadEnd == WorldDeadEnd.THREEPOINT){
					previousAngle = getAngle();
					if(!checkHasTurn()){
						setThreePointTurn();
					}else{
						normTurnRight = true;
					}
				}else if(deadEnd == WorldDeadEnd.REVERSEOUT){
					setReverseTurn();
					System.out.println("reverse");
				}else if(deadEnd == WorldDeadEnd.UTURN){
					setRightTurn();
					System.out.println("uturn");
				}else if(getDistanceAheadWall()!=-1&&getDistanceAheadWall()+1<=wallSensitivity&&getDisRightWall()==-1){
					System.out.println("no dead end but wall ahead");
					setRightTurn();
				}
			}
			// This indicates that I can do a left turn if I am not turning right
			else{
				setLeftTurn();
				System.out.println("no wall to follow");
			}
		}else{
			findWallToFollow(delta);//**
			
		}
		
	}
	
	private void reactReverse(float delta) {
		// as long as there are walls on the right side of car, keep reversing
		if(checkRightRoad()){
			applyReverseAcceleration();
		}else{
			//applyBrake();
			//setFrontRoadAsWall();
			reverse = false;
		}
		
	}

//	private void setFrontRoadAsWall() {
//		switch(getOrientation()){
//		case EAST:
//			setEastRoadAsWall();
//			break;
//		case NORTH:
//			setNorthRoadAsWall();
//			break;
//		case SOUTH:
//			setSouthRoadAsWall();
//			break;
//		case WEST:
//			setWestRoadAsWall();
//			break;
//		default:
//			break;
//		
//		}
//		
//	}

	private boolean checkRightRoad() {
		switch(getOrientation()){
		case EAST:
			return checkSouthRoad();
		case NORTH:
			return checkEastRoad();
		case SOUTH:
			return checkWestRoad();
		case WEST:
			return checkNorthRoad();
		default:
			return false;
		}
	}

	private boolean checkNorthRoad() {
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= Car.VIEW_SQUARE; i++){
			MapTile tile1 = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
			MapTile tile2 = currentView.get(new Coordinate(currentPosition.x-1, currentPosition.y+i));
			if(tile1.getName().equals("Wall") || tile2.getName().equals("Wall")){
				return false;
			}
		}
		return true;
	}

	private boolean checkWestRoad() {
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= Car.VIEW_SQUARE; i++){
			MapTile tile1 = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
			MapTile tile2 = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y-1));
			if(tile1.getName().equals("Wall") || tile2.getName().equals("Wall")){
				return false;
			}
		}
		return true;
	}

	private boolean checkEastRoad() {
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= Car.VIEW_SQUARE; i++){
			MapTile tile1 = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
			MapTile tile2 = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y+1));
			if(tile1.getName().equals("Wall") || tile2.getName().equals("Wall")){
				return false;
			}
		}
		return true;
	}

	private boolean checkSouthRoad() {
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= Car.VIEW_SQUARE; i++){
			MapTile tile1 = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
			MapTile tile2 = currentView.get(new Coordinate(currentPosition.x+1, currentPosition.y-i));
			if(tile1.getName().equals("Wall") || tile2.getName().equals("Wall")){
				return false;
			}
		}
		return true;
	}

	private void setReverseTurn() {
		reverse = true;
		
	}

	private boolean checkHasTurn() {
		switch(getOrientation()){
		case EAST:
			System.out.println("currentangle: "+getAngle());
			return (getAngle()>30 );
		case NORTH:
			return (WorldSpatial.NORTH_DEGREE-previousAngle>30);
		case SOUTH:
			return (WorldSpatial.SOUTH_DEGREE-previousAngle>30);
		case WEST:
			return (WorldSpatial.WEST_DEGREE-previousAngle>30);
		default:
			return false;
		}
	}

	

	private void setRightTurn() {
		lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
		isTurningRight = true;
		normTurnRight = true;
		
	}

	private void findWallToFollow(float delta) {
		if(getVelocity() < CAR_SPEED){
			applyForwardAcceleration();
		}
		// Turn towards the north
		if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
			lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
			applyLeftTurn(delta);
		}
		if(checkNorth()){
			// Turn right until we go back to east!
			if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
				lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
				applyRightTurn(delta);
			}
			else{
				isFollowingWall = true;
				System.out.println("has found wall");
			}
		}
		
	}

	private void checkStateChange() {
		
		if(previousState == null){
			previousState = getOrientation();
		}
		else{
			if(previousState != getOrientation()){
				if(normTurnLeft){
					normTurnLeft = false;
					isTurningLeft = false;
				}
				if(normTurnRight){
					normTurnRight = false;
					isTurningRight = false;
				}
				if(threePointTurn){
					threePointTurn = false;
					isTurningRight = false;
					hasReversed = false;
					isReversing = false;
				}
				if(reverse){
					reverse = false;
				}
				previousState = getOrientation();
				System.out.println("state: "+previousState);
			}
		}
		
	}

	private void reactThreePointTurn(float delta, float previousAngle) {
		
		int disAheadWall = getDistanceAheadWall();
		int disRightWall = getDisRightWall();
		float currentAngle = getAngle();
		
		System.out.println("react: diff: "+Math.abs(currentAngle-previousAngle));
		System.out.println("react: ori: "+getOrientation());
		
		if(getOrientation() == WorldSpatial.Direction.EAST && currentAngle!=0){
			currentAngle = 360 - currentAngle;
		}
		
		if(Math.abs(currentAngle-previousAngle)<80){
			lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
			isTurningRight = true;
			applyRightTurn(delta);
			System.out.println("3-point-right");
		}else{
			
			if(disAheadWall<=1 && !isReversing){
				isReversing = true;
				applyBrake();
				System.out.println("3-point-start-reverse");
			}else if(disRightWall>=2 && hasReversed){
				isReversing = false;
				threePointTurn = false;
				hasReversed = false;
				
				applyBrake();
				System.out.println("stop reverse");
			}else if(isReversing){
				hasReversed = true;
				
				//TODO: get the car's acc/reverse status and judge brake or reverse
				if(checkisAccelerateInRightSide()){
					applyBrake();
					applyReverseAcceleration();
					System.out.println("3-point-reversing-brake");
				}
				else{
					applyReverseAcceleration();
					System.out.println("3-point-reversing-reverse");
				}
				

			}
			
			
		}
		
		
	}

	private boolean checkisAccelerate(){
		switch(getOrientation()){
		case EAST:
			return (getRawVelocity().x>0);
		case NORTH:
			return (getRawVelocity().y>0);
		case SOUTH:
			return (getRawVelocity().y<0);
		case WEST:
			return (getRawVelocity().x<0);
		default:
			return false;
		}
	}
	
	private boolean checkisAccelerateInRightSide(){
		switch(getOrientation()){
		case NORTH:
			return (getRawVelocity().x>0);
		case WEST:
			return (getRawVelocity().y>0);
		case EAST:
			return (getRawVelocity().y<0);
		case SOUTH:
			return (getRawVelocity().x<0);
		default:
			return false;
		}
	}
	
	

	private void setThreePointTurn() {
		threePointTurn = true;		
	}

	private void setLeftTurn() {
		lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
		normTurnLeft = true;
		isTurningLeft = true;
		
	}

	private WorldDeadEnd detectDeadEnd() {
		System.out.println("check end");
		int disAheadWall = getDistanceAheadWall();
		int disRightWall = getDisRightWall();
		
			if(disAheadWall!=-1 && disRightWall!=-1 && disAheadWall<=aheadSensitivity && disRightWall==rightThreeSen){
				return WorldDeadEnd.THREEPOINT;
			}else if(disAheadWall!=-1 && disRightWall!=-1&&disAheadWall<=aheadSensitivity && disRightWall==rightReverseSen){
				return WorldDeadEnd.REVERSEOUT;
			}else if(disAheadWall!=-1 && disRightWall!=-1&&disAheadWall<=aheadSensitivity && disRightWall==rightThreeSen+1){
				return WorldDeadEnd.UTURN;
			}
		
		
		return null;
	}
	
	private int getDistanceLeftWall() {
		switch(getOrientation()){
		case EAST:
			return getDisNorth();
		case NORTH:
			return getDisWest();
		case SOUTH:
			return getDisEast();
		case WEST:
			return getDisSouth();
		default:
			return -1;
		}
	}

	private int getDisRightWall() {
		switch(getOrientation()){
		case EAST:
			return getDisSouth();
		case NORTH:
			return getDisEast();
		case SOUTH:
			return getDisWest();
		case WEST:
			return getDisNorth();
		default:
			return -1;
		}
	}
	
	private int getDistanceAheadWall() {
		switch(getOrientation()){
		case EAST:
			return getDisEast();
		case NORTH:
			return getDisNorth();
		case SOUTH:
			return getDisSouth();
		case WEST:
			return getDisWest();
		default:
			return -1;
			}
		
	}

	private int getDisSouth() {
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= Car.VIEW_SQUARE; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
			//forwardTile can be null
			MapTile forwardTile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-(i+1)));
//			boolean adsa=tile instanceof TrapTile;
			if(tile.getName().equals("Wall")){
				return (i-1);
			}else if(checkIfGrassCannotPass(tile, forwardTile)){
				return (i-1);
			}else if(checkIfMudCannotPass(tile, forwardTile)){
				return (i-1);
			}else if(checkIfLavaCannotPass(tile,forwardTile)){
				return (i-1);
			}
		}
		return -1;
	}

	private int getDisEast() {
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= Car.VIEW_SQUARE; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
			//forwardTile can be null
			MapTile forwardTile = currentView.get(new Coordinate(currentPosition.x+(i+1), currentPosition.y));
			if(tile.getName().equals("Wall")){
				return (i-1);
			}else if(checkIfGrassCannotPass(tile, forwardTile)){
				return (i-1);
			}else if(checkIfMudCannotPass(tile, forwardTile)){
				return (i-1);
			}else if(checkIfLavaCannotPass(tile,forwardTile)){
				return (i-1);
			}
		}
		return -1;
	}

	private int getDisWest() {
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= Car.VIEW_SQUARE; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
			//forwardTile can be null
			MapTile forwardTile = currentView.get(new Coordinate(currentPosition.x-(i+1), currentPosition.y));
			if(tile.getName().equals("Wall")){
				return (i-1);
			}else if(checkIfGrassCannotPass(tile, forwardTile)){
				return (i-1);
			}else if(checkIfMudCannotPass(tile, forwardTile)){
				return (i-1);
			}else if(checkIfLavaCannotPass(tile,forwardTile)){
				return (i-1);
			}
		}
		return -1;
	}

	private int getDisNorth() {
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= Car.VIEW_SQUARE; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
			//forwardTile can be null
			MapTile forwardTile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+(i+1)));
			if(tile.getName().equals("Wall")){
				return (i-1);
			}else if(checkIfGrassCannotPass(tile, forwardTile)){
				return (i-1);
			}else if(checkIfMudCannotPass(tile, forwardTile)){
				return (i-1);
			}else if(checkIfLavaCannotPass(tile,forwardTile)){
				return (i-1);
			}
		}
		return -1;
	}
	/**
	 * Check if the currentTile can regard as wall if it is a grass tile.
	 * Rules are: if the tile is grass trap, and the forward tile is wall,
	 * the grass trap will be regard as a wall since this trap cannot be simply passed
	 * @param currentTile 
	 * @param forwardTile
	 * @return true if the currentTile cannot pass the grass test and should be regarded as wall
	 * false if the currentTile can pass the test and it can be regard as road(if it is a grass) 
	 * or currentTile is not a grass trap
	 */ 
	private boolean checkIfGrassCannotPass(MapTile currentTile, MapTile forwardTile){
		if(currentTile instanceof GrassTrap){
			if(null!= forwardTile&&forwardTile.getName().equals("Wall")){
				return true;
			}
		}
		return false;
	}
	/**
	 * Check if the currentTile can regard as wall if it is a Mud tile.
	 * Rules are: if the tile is a mud trap, and the forward tile is a mud trap,
	 * the mud trap will be regard as a wall since this trap will cause game lose
	 * @param currentTile 
	 * @param forwardTile
	 * @return true if the currentTile cannot pass the mud test and should be regarded as wall
	 * false if the currentTile can pass the test and it can be regard as road(if it is a mud) 
	 * or currentTile is not a mud trap
	 */ 
	private boolean checkIfMudCannotPass(MapTile currentTile, MapTile forwardTile){
		if(currentTile instanceof MudTrap){
			if(null!= forwardTile&&forwardTile instanceof MudTrap){
				return true;
			}
		}
		return false;
	}
	/**
	 * Check if the currentTile can regard as wall if it is a lava tile.
	 * Rules are: if the tile is lava trap, and the forward tile is a lava trap,
	 * the grass trap will be regard as a wall since this trap will cause at least 40point of health eventually
	 * We regard this kind of lava as bad path and keep it as wall
	 * @param currentTile 
	 * @param forwardTile
	 * @return true if the currentTile cannot pass the lava test and should be regarded as wall
	 * false if the current can pass the test and it can be regard as road(if it is a lava) 
	 * or currentTile is not a lava trap
	 */ 
	private boolean checkIfLavaCannotPass(MapTile currentTile, MapTile forwardTile){
		if(currentTile instanceof LavaTrap){
			if(null!= forwardTile&&forwardTile instanceof LavaTrap){
				return true;
			}
		}
		return false;
	}
	

	private void reactLeftTurn(float delta) {
		if(!checkFollowingWall()){
			
			applyLeftTurn(delta);
		}
		else{
			
			normTurnLeft = false;
			isTurningLeft = false;
		}
		
	}

	
	
	
	
	private boolean checkFollowingWall() {
		switch(getOrientation()){
		case EAST:
			return checkNorth();
		case NORTH:
			return checkWest();
		case SOUTH:
			return checkEast();
		case WEST:
			return checkSouth();
		default:
			return false;
		}
	}

	
	

	private boolean checkWest() {
		// Check tiles to my left
				Coordinate currentPosition = new Coordinate(getPosition());
				for(int i = 0; i <= wallSensitivity; i++){
					MapTile tile = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
					if(tile.getName().equals("Wall")){
						return true;
					}
				}
				return false;
	}

	private boolean checkSouth() {
		// Check tiles to towards the top
				Coordinate currentPosition = new Coordinate(getPosition());
				for(int i = 0; i <= wallSensitivity; i++){
					MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
					if(tile.getName().equals("Wall")){
						return true;
					}
				}
				return false;
	}

	private boolean checkNorth() {
		// Check tiles to towards the top
				Coordinate currentPosition = new Coordinate(getPosition());
				for(int i = 0; i <= wallSensitivity; i++){
					MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
					if(tile.getName().equals("Wall")){
						return true;
					}
				}
				return false;
	}

	private boolean checkEast() {
		// Check tiles to my right
				Coordinate currentPosition = new Coordinate(getPosition());
				for(int i = 0; i <= wallSensitivity; i++){
					MapTile tile = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
					if(tile.getName().equals("Wall")){
						return true;
					}
				}
				return false;
	}

	private void applyLeftTurn(float delta) {
		switch(getOrientation()){
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

	

	private void applyRightTurn(float delta) {
		switch(getOrientation()){
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
				System.out.println("inner: turn right");
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

	private void readjust(float delta) {
		if(lastTurnDirection != null){
			if(!isTurningRight && lastTurnDirection.equals(WorldSpatial.RelativeDirection.RIGHT)){
				adjustRight(delta);
			}
			else if(!isTurningLeft && lastTurnDirection.equals(WorldSpatial.RelativeDirection.LEFT)){
				adjustLeft(delta);
				System.out.println("adjust left");
			}
		}
		
	}

	//**
	private void adjustLeft(float delta) {
		switch(getOrientation()){
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

	//**
	private void adjustRight(float delta) {
		switch(getOrientation()){
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

	
	

}
