/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tileworld.agent;

import java.util.ArrayList;
import java.util.List;

import sim.field.grid.ObjectGrid2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWFuelStation;
import tileworld.environment.TWHole;
import tileworld.environment.TWObject;
import tileworld.environment.TWObstacle;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.TWPath;

/**
 * TWContextBuilder
 *
 * @author michaellees
 * Created: Feb 6, 2011
 *
 * Copyright michaellees Expression year is undefined on line 16, column 24 in Templates/Classes/Class.java.
 *
 *
 * Description:
 *
 */
public class MyAgent2 extends TWAgent{
	private String name;
	private int fuelX = -1;
	private int fuelY = -1;
	private final int maxX = this.getEnvironment().getxDimension();
	private final int maxY = this.getEnvironment().getyDimension();
	private AstarPathGenerator PG = new AstarPathGenerator(this.getEnvironment(), this, maxX+maxY);
	private TWPath currentPath=null;
	private TWPath earlyPath=null;
	private int currentStep=0;
	private int earlyStep=0;
	private int state = 0;
	private int targetX = -1;
	private int targetY = -1;
	private int downStep = 0;
	private double TileLifetime = -1;
	private double HoleLifetime = -1;
	private String message;
	private Message previous_read_message = null;
	private int broadcasted_fuelstation = 0;
	private String[][] shared_memory = new String[maxX][maxY];
	private int step = 0;
	private int nearest_tileX = -1;
	private int nearest_tileY = -1;
	private int nearest_holeX = -1;
	private int nearest_holeY = -1;
	private int nearest_tile_distance = maxX+maxY;
	private int nearest_hole_distance = maxX+maxY;
	private int[][] other_targets = new int[5][2];

	//新的变量
	private static int scanType = 0;//0为横边为长边，切横边。1为竖边为长边。  
	private static int[][] originalPositions = new int[5][2];
	private static int originalPositionsUpdateTimes = 0;
	private boolean firstBroadcast = true;
	private int agentid = -1;
	private int shiftStep = 0;//平移次数，7或者到边界
	private int shiftTime = 0;//转方向次数
	private int s1_substate = 3;//优先向上走
	private boolean firstMove = true;
	private boolean changeDirection = false;
	private int maxStep = 7;
	private static List<ArrayList<int[]>> boundaryPoints = null;
	private int scan_initial_X = 0;
	private int scan_initial_Y = 0;
	private int scan_initial_corner = 0;
	private int toAreaLeft = 0;
	private int toAreaRight = 0;
	private int toAreaUp = 0;
	private int toAreaDown = 0;
	private int[][] match = new int[5][3];
	private int[][] cutxMatch = new int[5][3];
	private int[][] cutyMatch = new int[5][3];

	static{
		originalPositions[1] = new int[]{3,79};
		originalPositions[2] = new int[]{68,24};
		originalPositions[3] = new int[]{3,44};
		originalPositions[4] = new int[]{62,64};
	}

	public MyAgent2(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
		super(xpos,ypos,env,fuelLevel);
		this.name = name;
		this.agentid = Character.getNumericValue((name.charAt(5)))-1;
		//新的变量
		this.scanType = this.maxX > this.maxY?0:1;
	}

	public void print_messages() {
		ArrayList<Message> M = this.getEnvironment().getMessages();
		for (int i=0;i<M.size();i++) {
			String s = M.get(i).getMessage();
			if (s!=null) {
				String[] splited = s.split("\\s+");
				if (!splited[0].equals("blank")) {
					System.out.println(M.get(i).getMessage());
				}
			}


		}

	}

	protected void print_shared_memory() {
		System.out.println(this.name+" memory:");
		for (int i=0;i<maxX;i++) {
			for (int j=0;j<maxY;j++) {
				if (shared_memory[i][j] != null) {
					System.out.println("("+i+","+j+"): "+shared_memory[i][j]);
				}
			}
		}
	}

	protected boolean isGood(int p, int q) {
		int myidx = Character.getNumericValue((name.charAt(5)))-1;
		int distance = -1;
		for (int i=0;i<5;i++) {
			if (i == myidx || other_targets[i][0] == -1 || other_targets[i][1] == -1) {
				continue;
			}
			distance = Math.abs(p-other_targets[i][0])+Math.abs(q-other_targets[i][1]);
			if (distance <= 3) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void communicate() {
		Message s = new Message(this.name, "all", message);
		this.getEnvironment().receiveMessage(s); 
		// this will send the message to the broadcast channel of the environment
	}

	protected int[] find_nearest_non_blocked(TWDirection dir, int x, int y) {
		int[] result = {x,y};
		switch (dir) {
		case E:
			x++;
			while (x<maxX && this.getMemory().getMemoryGrid().get(x,y) instanceof TWObstacle) {
				x++;
			}
			result[0] = Math.min(x, maxX-1);
			result[1] = y;
			return result;

		case W:
			x--;
			while (x>=0 && this.getMemory().getMemoryGrid().get(x,y) instanceof TWObstacle ) {
				x--;
			}
			result[0] = Math.max(x, 0);
			result[1] = y;
			return result;

		case N:
			y--;
			while (y>=0 && this.getMemory().getMemoryGrid().get(x,y) instanceof TWObstacle) {
				y--;
			}
			result[0] = x;
			result[1] = Math.max(y, 0);
			return result;

		case S:
			y++;
			while (y<maxY && this.getMemory().getMemoryGrid().get(x,y) instanceof TWObstacle) {
				y++;
			}
			result[0] = x;
			result[1] = Math.min(y, maxY-1);
			return result;


		}
		return null;
	}

	/**
	 * zs
	 */
	protected void broadcast_position(){
		message = "position "+this.x+" "+this.y;
		communicate();
	}

	protected void broadcast_sensor() {
		message = "target "+targetX+" "+targetY;
		communicate();

		ObjectGrid2D current_objects = this.getMemory().getMemoryGrid();
		//TWAgentPercept[][] current_objects = this.getMemory().getObjects();
		for (int i=Math.max(this.getX()-3,0); i<Math.min(this.getX()+4,maxX-1);i++) {
			for (int j=Math.max(this.getY()-3,0);j<Math.min(this.getY()+4,maxY-1);j++) {
				if (current_objects.get(i,j) == null) {
					message = "blank "+i+" "+j;
					communicate();
				} else if (current_objects.get(i,j) instanceof TWTile) {
					message = "Tile "+i+" "+j;
					communicate();
				} else if (current_objects.get(i,j) instanceof TWHole){
					message = "Hole "+i+" "+j;
					communicate();
				} 
			}
		}
	}

	protected void process_messages() {
		ArrayList<Message> AllMessages = this.getEnvironment().getMessages();
		for (int i=0;i<AllMessages.size();i++) {
			String from = AllMessages.get(i).getFrom();
			String s = AllMessages.get(i).getMessage();
			if (s != null) {
				String[] splited = s.split("\\s+");

				// 有人找到了加油站并广播了信息
				if (splited[0].equals("fuelstaion")) {
					this.fuelX = Integer.parseInt(splited[1]);
					this.fuelY = Integer.parseInt(splited[2]);
					this.shared_memory[this.fuelX][this.fuelY] = splited[0];
					broadcasted_fuelstation = 1;
					state = 3;
				} else if (splited[0].equals("Tile") || splited[0].equals("Hole")) {
					int a = Integer.parseInt(splited[1]);
					int b = Integer.parseInt(splited[2]);
					this.shared_memory[a][b] = splited[0];
				} else if (splited[0].equals("blank")) {
					int a = Integer.parseInt(splited[1]);
					int b = Integer.parseInt(splited[2]);
					this.shared_memory[a][b] = null;
				} else if (splited[0].equals("target")) {
					int a = Integer.parseInt(splited[1]);
					int b = Integer.parseInt(splited[2]);
					int idx = Character.getNumericValue((from.charAt(5)))-1;
					int[] result = new int[2];
					result[0] = a;
					result[1] = b;
					other_targets[idx] = result;
				} else if (splited[0].equals("position")) {
					int a = Integer.parseInt(splited[1]);
					int b = Integer.parseInt(splited[2]);
					int idx = Character.getNumericValue((from.charAt(5)))-1;
					int[] result = new int[2];
					result[0] = a;
					result[1] = b;
					//					if(this.originalPositionsUpdateTimes<5){
					originalPositions[idx] = result;
					this.originalPositionsUpdateTimes++;
					//					}


				}
			}

		}
	}

	public void process_shared_memory() {
		this.nearest_tile_distance = maxX+maxY;
		this.nearest_hole_distance = maxX+maxY;
		int distance = -1;
		int tempX = -1;
		int tempY = -1;
		int myidx = Character.getNumericValue((name.charAt(5)))-1;
		switch (myidx) {

		case 0:
			tempX = maxX / 4;
			tempY = maxY / 4;
			break;
		case 1:
			tempX = maxX / 4 * 3;
			tempY = maxY / 4 * 3;
			break;
		case 2:
			tempX = maxX / 4;
			tempY = maxY / 4 * 3;
			break;
		case 3:
			tempX = maxX / 4 * 3;
			tempY = maxY / 4;
			break;
		case 4:
			tempX = maxX / 2;
			tempY = maxY / 2;
			break;
		}

		tempX = this.getX();
		tempY = this.getY();

		for (int i=0;i<maxX;i++) {
			for (int j=0;j<maxY;j++) {
				if (shared_memory[i][j] != null) {
					if (shared_memory[i][j].equals("Tile")) {

						distance = Math.abs(i-tempX)+Math.abs(j-tempY);
						if (distance <= nearest_tile_distance && isGood(i,j)) {
							this.nearest_tile_distance = distance;
							this.nearest_tileX = i;
							this.nearest_tileY = j;
						}
					}
					else if (shared_memory[i][j].equals("Hole")) {
						distance = Math.abs(i-tempX)+Math.abs(j-tempY);
						if (distance <= nearest_hole_distance && isGood(i,j)) {
							this.nearest_hole_distance = distance;
							this.nearest_holeX = i;
							this.nearest_holeY = j;
						}
					}
				}


			}
		}
		if (this.nearest_tile_distance == maxX+maxY) {
			this.nearest_tileX = -1;
			this.nearest_tileY = -1;
		}
		if (this.nearest_hole_distance == maxX+maxY) {
			this.nearest_holeX = -1;
			this.nearest_holeY = -1;
		}

	}


	protected void print_targets() {
		System.out.print(name+": ");
		for (int i=0;i<5;i++) {
			System.out.print("("+other_targets[i][0]+","+other_targets[i][1]+"); ");  		
		}
		System.out.println();
	}


	protected void traverse() {

		switch (s1_substate) {

		case 0:
			if (this.getX()==targetX && this.getY()==targetY) {
				s1_substate = 1;
				targetX = maxX - 4;
				targetY = this.getY() + 7;
				return;
			}				
			earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			earlyStep = 0;
			while (earlyPath == null) {
				targetX++;
				if (targetX >= maxX) {
					targetX = maxX - 4;
					targetY--; 
				}
				earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			}
			return;

		case 1:
			if (this.getX()==targetX && this.getY()==targetY) {
				s1_substate = 2;
				targetX = 3;
				return;
			}		
			earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			earlyStep = 0;
			while (earlyPath == null) {
				targetX++;
				if (targetX >= maxX) {
					targetX = maxX - 4;
					targetY--; 
				}
				earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			}
			return;

		case 2:
			if (this.getX()==targetX && this.getY()==targetY) {
				s1_substate = 3;
				targetX = 3;
				targetY = this.getY() + 7;
				return;
			}		
			earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			earlyStep = 0;
			while (earlyPath == null) {
				targetX--;
				if (targetX >= maxX) {
					targetX = 3;
					targetY--; 
				}
				earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			}
			return;

		case 3:
			if (this.getX()==targetX && this.getY()==targetY) {
				s1_substate = 0;
				targetX = maxX - 4;
				return;
			}		
			earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			earlyStep = 0;
			while (earlyPath == null) {
				targetX--;
				if (targetX >= maxX) {
					targetX = 3;
					targetY--; 
				}
				earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			}
			return;
		}


	}

	protected void state2() {
		switch (s1_substate) {

		case 0:
			if (this.getX()==targetX && this.getY()==targetY) {
				state = 1;
				targetX = -1;
				targetY = -1;
				return;
			}

			earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			earlyStep = 0;
			while (earlyPath == null) {
				targetX++;
				if (targetX >= maxX) {
					targetX = this.getX()+1;
					targetY--; 
				}
				earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			}
			return;



		case 1:
			if (this.getX()==targetX && this.getY()==targetY) {
				state = 1;
				targetX = -1;
				targetY = -1;
				return;
			}

			earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			earlyStep = 0;
			while (earlyPath == null) {
				targetX--;
				if (targetX <= 0) {
					targetX = this.getX()-1;
					targetY--; 
				}
				earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			}
			return;

		case 2:
			if (this.getX()==targetX && this.getY()==targetY) {
				state = 1;
				targetX = -1;
				targetY = -1;
				return;
			}

			earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			earlyStep = 0;
			while (earlyPath == null) {
				targetY++;
				if (targetY >= maxY) {
					targetX++;
					targetY = this.getY()+1; 
				}
				earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			}
			return;

		case 3:
			if (this.getX()==targetX && this.getY()==targetY) {
				state = 1;
				targetX = -1;
				targetY = -1;
				return;
			}

			earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			earlyStep = 0;
			while (earlyPath == null) {
				targetY--;
				if (targetY <= 0) {
					targetX++;
					targetY = this.getY()+1; 
				}
				earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			}
			return;
		}
	}

	protected void state3() {
		TWTile closeTile = (TWTile) this.getMemory().getClosestObjectInSensorRange(TWTile.class);
		TWHole closeHole = (TWHole) this.getMemory().getClosestObjectInSensorRange(TWHole.class);

		///----------------------------
		if (closeTile == null) {
			closeTile = (TWTile) this.getMemory().getNearbyTile(this.getX(),this.getY(),this.TileLifetime);
		}
		if (closeHole == null) {
			closeHole = (TWHole) this.getMemory().getNearbyHole(this.getX(),this.getY(),this.HoleLifetime);
		}
		///--------------------------- 


		if (closeTile != null && this.carriedTiles.size() < 3 && 
				closeHole != null && this.carriedTiles.size() > 0) {
			state = 3;
			double dh = this.getDistanceTo(closeHole);
			double dt = this.getDistanceTo(closeTile);
			if (dh <= dt) {
				targetX = closeHole.getX();
				targetY = closeHole.getY();
				if (!isGood(targetX, targetY)) {
					targetX = nearest_holeX;
					targetY = nearest_holeY;
				}
				//System.out.println("closeHole=("+closeHole.getX() +","+ closeHole.getY()+")");
			} else {
				targetX = closeTile.getX();
				targetY = closeTile.getY();
				if (!isGood(targetX, targetY)) {
					targetX = nearest_tileX;
					targetY = nearest_tileY;
				}
				//System.out.println("closeTile=("+closeTile.getX() +","+ closeTile.getY()+")");
			}
		} else if (closeTile != null && this.carriedTiles.size() == 0){
			state = 3;
			targetX = closeTile.getX();
			targetY = closeTile.getY();
			if (!isGood(targetX, targetY)) {
				targetX = nearest_tileX;
				targetY = nearest_tileY;
			}
			//System.out.println("closeTile=("+closeTile.getX() +","+ closeTile.getY()+")");

		} else if (closeHole != null && this.carriedTiles.size() == 3){
			state = 3;
			targetX = closeHole.getX();
			targetY = closeHole.getY();
			if (!isGood(targetX, targetY)) {
				targetX = nearest_holeX;
				targetY = nearest_holeY;
			}
			//System.out.println("closeHole=("+closeHole.getX() +","+ closeHole.getY()+")");

		} else {
			if (this.carriedTiles.size() < 3 && this.carriedTiles.size() > 0) {
				if (nearest_hole_distance <= nearest_tile_distance) {
					targetX = nearest_holeX;
					targetY = nearest_holeY;
				} else {
					targetX = nearest_tileX;
					targetY = nearest_tileY;
				}
			} else if (this.carriedTiles.size() == 0) {
				targetX = nearest_tileX;
				targetY = nearest_tileY;
			} else if (this.carriedTiles.size() == 3){
				targetX = nearest_holeX;
				targetY = nearest_holeY;
			}

		}

		
	}

	protected void toCorner() {

		switch (agentid) {

		case 0:
			targetX = maxX / 4;
			targetY = maxY / 4;
			break;
		case 1:
			targetX = maxX / 4 * 3;
			targetY = maxY / 4 * 3;
			break;
		case 2:
			targetX = maxX / 4;
			targetY = maxY / 4 * 3;
			break;
		case 3:
			targetX = maxX / 4 * 3;
			targetY = maxY / 4;
			break;
		case 4:
			targetX = maxX / 2;
			targetY = maxY / 2;
			break;
		}

	}

	protected void state4() {
		toCorner();
		currentPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
		currentStep = 0;
		while (currentPath == null) {
			targetX++;
			currentPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);		
		}
	}


	protected TWThought think() {
		step++;
		if(this.firstBroadcast){
			broadcast_position();
		}
		broadcast_sensor();
		//print_messages();
		process_messages();
		//print_shared_memory();
		process_shared_memory();
		//System.out.println("Simple Score: " + this.score + "; Have tile:" + this.carriedTiles.size());
		//print_targets();
		/*
    	System.out.println(this.name+": current place:("+this.getX()+","+this.getY()+"); target place:("+this.targetX+","+this.targetY+"); "
    			+ "\nHave tile:" + this.carriedTiles.size() + "; nearest tile = ("+this.nearest_tileX+","+this.nearest_tileY+")"+
    			"; nearest hole = ("+this.nearest_holeX+","+this.nearest_holeY+")");
		 */
		//System.out.println(this.name + this.fuelX+this.fuelY + "; target place:("+this.targetX+","+this.targetY+");");


		if(step == 1){
			return new TWThought(TWAction.MOVE, TWDirection.Z);
		}else if(step == 2){

			int[][] agentLocs = MyAgent.originalPositions;
			boundaryPoints = Tools.divideMap(5, maxX, maxY);
			List<ArrayList<int[]>> candidates = Tools.getCandidates(boundaryPoints);
			//	    	getDisAgentArea(candidates.get(0), 6, 0);

			match = Tools.matchAgentArea(agentLocs, candidates);
			scan_initial_X = match[this.agentid][0];
			scan_initial_Y = match[this.agentid][1];
			scan_initial_corner = match[this.agentid][3];
			this.firstBroadcast = false;
		}


		if (this.carriedTiles.size() < 3 && this.getMemory().getMemoryGrid().get(this.getX(), this.getY()) instanceof TWTile){
			return new TWThought(TWAction.PICKUP, TWDirection.Z);
		}
		if (this.carriedTiles.size() > 0 && this.getMemory().getMemoryGrid().get(this.getX(), this.getY()) instanceof TWHole){
			return new TWThought(TWAction.PUTDOWN, TWDirection.Z);
		}
		if (this.getX()==fuelX && this.getY()==fuelY){
			return new TWThought(TWAction.REFUEL, TWDirection.Z);
		}
		//getMemory().getClosestObjectInSensorRange(TWTile.class);

		toAreaLeft = this.getX() - boundaryPoints.get(match[this.agentid][2]).get(0)[0] - 3;
		toAreaRight = boundaryPoints.get(match[this.agentid][2]).get(1)[0] - 3 - this.getX();
		toAreaUp = this.getY() - boundaryPoints.get(match[this.agentid][2]).get(0)[1] - 3;
		toAreaDown = boundaryPoints.get(match[this.agentid][2]).get(1)[1] - 3 - this.getY();
		//		System.out.println("matchArea[this.agentid]:"+match[this.agentid][2]+";agentid:"+this.agentid+";Position:"+this.getX()+","+this.getY()+"\t"
		//				+"boundaryPoints:"+boundaryPoints.get(match[this.agentid][2]).get(0)[0]+","+boundaryPoints.get(match[this.agentid][2]).get(0)[1]+";"
		//				+boundaryPoints.get(match[this.agentid][2]).get(1)[0]+","+boundaryPoints.get(match[this.agentid][2]).get(1)[1]+"\t"
		//				+"toAreaLeft:"+toAreaLeft+";toAreaRight:"+toAreaRight+";toAreaUp:"+toAreaUp+";toAreaDown:"+toAreaDown);

		if (fuelX == -1 || fuelY == -1) {
			this.fuelX = this.getMemory().getFuelX();
			this.fuelY = this.getMemory().getFuelY();
		}


		if (TileLifetime == -1 || HoleLifetime == -1) {
			TWTile oneTile = (TWTile) this.getMemory().getClosestObjectInSensorRange(TWTile.class);
			TWHole oneHole = (TWHole) this.getMemory().getClosestObjectInSensorRange(TWHole.class);
			if (oneTile!=null) {
				this.TileLifetime = oneTile.getDeathTime();
				//System.out.println("TileLifetime="+this.TileLifetime);
			}
			if (oneHole!=null) {
				this.HoleLifetime = oneHole.getDeathTime();
				//System.out.println("HoleLifetime="+this.HoleLifetime);
			}      	
		}


		if (fuelX == -1 && fuelY == -1) {

			if (state==0) {
				//				System.out.println("scan_initial_X:"+scan_initial_X+"scan_initial_Y"+scan_initial_Y);
				if (!(this.getX()==scan_initial_X && this.getY()==scan_initial_Y)) {

					earlyPath = PG.findPath(this.getX(), this.getY(), scan_initial_X, scan_initial_Y);
					earlyStep = 0;
					while (earlyPath == null) {
						
							scan_initial_Y--;
							if (scan_initial_Y < 0) {
								scan_initial_Y = match[this.agentid][1];
								scan_initial_X--; 
							}
							earlyPath = PG.findPath(this.getX(), this.getY(), scan_initial_X, scan_initial_Y);
							break;

					} 
					//System.out.println(name+": target=("+scan_initial_X+","+scan_initial_Y+")");
					target_on_the_way(scan_initial_X,scan_initial_Y);
					//					System.out.println(name+": corner=("+scan_initial_X+","+scan_initial_Y+"); target=("+targetX+" "+targetY+")");
					earlyPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
					if (earlyPath != null) {
						return new TWThought(TWAction.MOVE, earlyPath.getStep(earlyStep).getDirection());
					} else {
						earlyPath = PG.findPath(this.getX(), this.getY(), scan_initial_X, scan_initial_Y);
						return new TWThought(TWAction.MOVE, earlyPath.getStep(earlyStep).getDirection());
					}

				}else{
					if (scan_initial_corner == 0 || scan_initial_corner == 1) {
						s1_substate = 0;
					} else if (scan_initial_corner == 2 || scan_initial_corner == 3) {
						s1_substate = 1;
					}
					state = 1;
				}


			}  
			if (state == 1) {
				//System.out.println(name+": state=1; corner=("+scan_initial_X+","+scan_initial_Y+");");
				/*
				traverse();
				System.out.println(name+": substate="+s1_substate+"; current=("+this.getX()+","+this.getY()+"); target=("+this.targetX+","+this.targetY+")");
				return new TWThought(TWAction.MOVE, earlyPath.getStep(earlyStep).getDirection());
				 */

				/*
				//// 战's method 0right, 1left, 2down, 3up.
				if (s1_substate == 3 && (toAreaUp <= 0 || changeDirection)){
					if(0>=toAreaLeft){
						s1_substate = 0;
					}else{
						s1_substate = 1;
					}
					shiftStep = 0;
					shiftTime++;
					changeDirection = false;
				}
				else if (s1_substate == 0 && (toAreaRight <= 0 || changeDirection)) {
					if(0>=toAreaDown){
						s1_substate = 3;
					}else{
						s1_substate = 2;
					}
					shiftStep = 0;
					shiftTime++;
					changeDirection = false;
				}

				else if (s1_substate == 2 && (toAreaDown <= 0 || changeDirection)) {
					if(0>=toAreaLeft){
						s1_substate = 0;
					}else{
						s1_substate = 1;
					}
					shiftStep = 0;
					shiftTime++;
					changeDirection = false;
				}

				else if (s1_substate == 1 && (toAreaLeft <= 0 || changeDirection)) {
					if(0>=toAreaDown){
						s1_substate = 3;
					}else{
						s1_substate = 2;
					}
					shiftStep = 0;
					shiftTime++;
					changeDirection = false;
				}else{
					shiftStep++;
				}
//				
//				System.out.println("s1_substate:"+s1_substate+";changeDirection:"+changeDirection
//						+";toAreaRight:"+toAreaRight+";toAreaLeft:"+toAreaLeft+";toAreaDown:"+toAreaDown+";toAreaUp:"+toAreaUp);
//				
//				System.out.println("shiftTime:"+shiftTime+";shiftStep:"+shiftStep);
//				
				if(shiftTime%2 == 1 && shiftStep == 6){
					shiftStep = 0;
					changeDirection = true;
				}

				if(firstMove){
					shiftTime = 0;
					firstMove = false;
				}
				 */

				int toLeft = this.getX() - 3;
				int toRight = maxX - 4 - this.getX();
				int toUp = this.getY() - 3;
				int toDown = maxY -4- this.getY();

				if (scan_initial_corner == 0 || scan_initial_corner == 3) {

					if (s1_substate == 0 && toRight <= 0 && toDown>=1) {
						s1_substate = 2;
						downStep = 0;
					}

					else if (s1_substate == 1 && toLeft <= 0 && toDown>=1) {
						s1_substate = 2;
						downStep = 0;
					}

					else if (s1_substate == 2) {
						if (toDown<0 && toLeft <= toRight) {
							s1_substate = 0;
							downStep = 0;
						} else if(toDown<0 && toLeft > toRight) {
							s1_substate = 1;
							downStep = 0;
						} else {
							downStep += 1;
						}

					}

					if (downStep >= 7) {       			
						if (toLeft <= toRight) {
							s1_substate = 0;
						} else {
							s1_substate = 1;
						}
						downStep = 0;
					}
				} else if (scan_initial_corner == 1 || scan_initial_corner == 2){

					if (s1_substate == 0 && toRight <= 0 && toUp>=1) {
						s1_substate = 3;
						downStep = 0;
					}

					else if (s1_substate == 1 && toLeft <= 0 && toUp>=1) {
						s1_substate = 3;
						downStep = 0;
					}

					else if (s1_substate == 3) {
						if (toUp<0 && toLeft <= toRight) {
							s1_substate = 0;
							downStep = 0;
						} else if(toUp<0 && toLeft > toRight) {
							s1_substate = 1;
							downStep = 0;
						} else {
							downStep += 1;
						}

					}

					if (downStep >= 7) {       			
						if (toLeft <= toRight) {
							s1_substate = 0;
						} else {
							s1_substate = 1;
						}
						downStep = 0;
					}

				}



				//System.out.println(name+": substate="+s1_substate+"; current=("+this.getX()+","+this.getY()+"); target=("+this.targetX+","+this.targetY+")");

				if (s1_substate == 0){
					return new TWThought(TWAction.MOVE, TWDirection.E);
				} else if (s1_substate == 1){
					return new TWThought(TWAction.MOVE, TWDirection.W);
				} else if (s1_substate == 2){
					return new TWThought(TWAction.MOVE, TWDirection.S);
				} else if (s1_substate == 3){
					return new TWThought(TWAction.MOVE, TWDirection.N);
				}


				////----------------------------------------------------

			} else if (state == 2) {
				state2();
				//				System.out.println(name+": substate="+s1_substate+"; current=("+this.getX()+","+this.getY()+"); target=("+this.targetX+","+this.targetY+")");
				if (targetX == -1 && targetY == -1) {
					state = 1;
					System.out.println(name+" block over! substate="+s1_substate);
					return think();
				}			
				return new TWThought(TWAction.MOVE, earlyPath.getStep(earlyStep).getDirection());
			}




		} 


		else if (this.getFuelLevel() > (this.getDistanceTo(fuelX, fuelY)+step/100+1)){
			if (broadcasted_fuelstation == 0) {
				message = "fuelstaion "+fuelX+" "+fuelY;
				communicate();
				broadcasted_fuelstation = 1;
			}


			// 先视野后记忆
			// ---------------------------------------------------------------
			state3();
			//// ---------------------------------------------------------------

			if (targetX>=0 && targetY>=0){
				currentPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
				currentStep = 0;
				if (currentPath != null) {
					return new TWThought(TWAction.MOVE, currentPath.getStep(currentStep).getDirection());
				}
			} else {
				if (this.getFuelLevel() / this.getDistanceTo(fuelX, fuelY)<2) {
					targetX = fuelX;
					targetY = fuelY;
				} else {
					return new TWThought(TWAction.MOVE,getRandomDirection());
				}
			}



		} else {
			System.out.println(name+": FUELSTATION!!!!!");
			targetX = fuelX;
			targetY = fuelY;
			
			target_on_the_way(fuelX, fuelY);
			
			currentPath = PG.findPath(this.getX(), this.getY(), targetX, targetY);
			currentStep = 0;
			if (currentPath != null) {
				return new TWThought(TWAction.MOVE, currentPath.getStep(currentStep).getDirection());
			}

		}


		//System.out.println("Simple Score: " + this.score + "; Have tile:" + this.carriedTiles.size());
		//System.out.println("current place:("+this.getX()+","+this.getY()+"); target place:("+this.targetX+","+this.targetY+");");
		return new TWThought(TWAction.MOVE,getRandomDirection());
	}

	@Override
	protected void act(TWThought thought) {
		Object tempObject = this.getMemory().getMemoryGrid().get(this.getX(), this.getY());  	
		switch(thought.getAction()){

		case PICKUP:
			pickUpTile((TWTile) tempObject);
			this.getMemory().removeObject(this.getX(), this.getY());

			return;

		case PUTDOWN:
			putTileInHole((TWHole) tempObject);
			this.getMemory().removeObject(this.getX(), this.getY());

			return;

		case REFUEL:
			this.refuel();
			this.currentPath = null;
			this.currentStep = 0;
			act(new TWThought(TWAction.MOVE,getRandomDirection()));

			return;


		}
		//You can do:
		//move(thought.getDirection())
		//pickUpTile(Tile)
		//putTileInHole(Hole)
		//refuel()

		try {
			/*
        	if (this.getX() == targetX && this.getY() == targetY 
        			&& thought.getAction().equals(TWAction.MOVE)
        			&& previousAction == TWAction.MOVE) {
        		targetX = fuelX;
        		targetY = fuelY;
        		return;
        	}
			 */

			this.move(thought.getDirection());
		} catch (CellBlockedException | java.lang.ArrayIndexOutOfBoundsException | java.lang.StackOverflowError ex) {
			TWDirection dir = thought.getDirection();
			int toLeft = this.getX() - 3;
			int toRight = maxX - 4 - this.getX();
			// Cell is blocked, replan?


			if (state == 1) {
				state = 2;


				/*/ 战---------------------------------------------
        		switch(s1_substate){
        		case 0:
        			if ((shiftTime%2 == 1 && shiftStep < 7) || (shiftTime%2 == 0)) {
        				targetX = this.getX()+2;
            			targetY = this.getY();
            			break;
        			}else{
        				if (toAreaUp <= toAreaDown) {
        					targetX = this.getX()+1;
                			targetY = this.getY()+1;
                			s1_substate = 2;
        				} else {
        					targetX = this.getX()+1;
                			targetY = this.getY()-1;
                			s1_substate = 3;
        				}
        			}

        		case 1:
        			if ((shiftTime%2 == 1 && shiftStep < 7)||(shiftTime%2 == 0)) {
        				targetX = this.getX()-2;
            			targetY = this.getY();
            			break;
        			}else{
        				if (toAreaUp <= toAreaDown) {
        					targetX = this.getX()-1;
                			targetY = this.getY()+1;
                			s1_substate = 2;
        				} else {
        					targetX = this.getX()-1;
                			targetY = this.getY()-1;
                			s1_substate = 3;
        				}
        			}

        		case 2:
        			if ((shiftTime%2 == 1 && shiftStep < 7) || (shiftTime%2 == 0)) {
        				targetX = this.getX();
            			targetY = this.getY()+2;
        			} else {
        				if (toAreaLeft <= toAreaRight) {
        					targetX = this.getX()+1;
                			targetY = this.getY()+1;
                			s1_substate = 0;
        				} else {
        					targetX = this.getX()-1;
                			targetY = this.getY()+1;
                			s1_substate = 1;
        				}

        			}

        			break;
        		case 3:
        			if ((shiftTime%2 == 1 && shiftStep < 7) || (shiftTime%2 == 0)) {
        				targetX = this.getX();
            			targetY = this.getY()-2;
            			break;
        			}else{
        				if (toAreaLeft <= toAreaRight) {
        					targetX = this.getX()+1;
                			targetY = this.getY()-1;
                			s1_substate = 0;
        				} else {
        					targetX = this.getX()-1;
                			targetY = this.getY()-1;
                			s1_substate = 1;
        				}
        			}

        		}
        		//----------------------------------------------/*/

				switch(s1_substate){
				case 0:
					targetX = this.getX()+2;
					targetY = this.getY();
					break;
				case 1:
					targetX = this.getX()-2;
					targetY = this.getY();
					break;
				case 2:
					if (downStep < 7) {
						targetX = this.getX();
						targetY = this.getY()+2;
					} else {
						if (toLeft <= toRight) {
							targetX = this.getX()+1;
							targetY = this.getY()+1;
							s1_substate = 0;
						} else {
							targetX = this.getX()-1;
							targetY = this.getY()+1;
							s1_substate = 1;
						}

					}

					break;
				case 3:
					if (downStep < 7) {
						targetX = this.getX();
						targetY = this.getY()-2;
					} else {
						if (toLeft <= toRight) {
							targetX = this.getX()+1;
							targetY = this.getY()-1;
							s1_substate = 0;
						} else {
							targetX = this.getX()-1;
							targetY = this.getY()-1;
							s1_substate = 1;
						}

					}

					break;

				}
				targetX = Math.max(0, targetX);
				targetX = Math.min(maxX-1, targetX);
				targetY = Math.max(0, targetY);
				targetY = Math.min(maxY-1, targetY);
				//System.out.println(name+": blocked! substate="+s1_substate+"; current=("+this.getX()+","+this.getY()+"); "+"target=("+targetX+","+targetY+"), shifttime="+shiftTime+"; shiftstep="+shiftStep);
				think();

			} else {
				think();
			}


			//act(new TWThought(TWAction.MOVE,getRandomDirection()));
		}
	}


	private TWDirection getRandomDirection(){

		TWDirection randomDir = TWDirection.values()[this.getEnvironment().random.nextInt(5)];

		if(this.getX()>=this.getEnvironment().getxDimension() ){
			randomDir = TWDirection.W;
		}else if(this.getX()<=1 ){
			randomDir = TWDirection.E;
		}else if(this.getY()<=1 ){
			randomDir = TWDirection.S;
		}else if(this.getY()>=this.getEnvironment().getxDimension() ){
			randomDir = TWDirection.N;
		}

		return randomDir;

	}


	@Override
	public String getName() {
		return name;
	}

	protected int[] find_nearest_in_range(int startX, int startY, int endX, int endY) {
		int tile_distance = maxX+maxY;
		int hole_distance = maxX+maxY;
		int path_distance = -1;
		int direct_distance = -1;
		int[] result = new int[6];
		result[2] = -1;
		result[5] = -1;

		for (int i=startX;i<=endX;i++) {
			for (int j=startY;j<=endY;j++) {
				String s = shared_memory[i][j];
				if (s==null) {
					continue;
				}


				if (s.equals("Tile")) {
					TWPath path = PG.findPath(this.getX(), this.getY(), i, j);
					if (path == null) {
						continue;
					}
					path_distance = path.getpath().size();
					direct_distance = Math.abs(i-this.getX())+Math.abs(j-this.getY());
					System.out.println("Tile: "+name+" "+this.getX()+","+this.getY()+" "+i+","+j+"; path_distance="+path_distance+"; direct_distance="+direct_distance);
					if (direct_distance <= tile_distance && path_distance == direct_distance) {
						tile_distance = direct_distance;
						result[0] = i;
						result[1] = j;
						result[2] = direct_distance;

					}
				} else if (s.equals("Hole")) {
					TWPath path = PG.findPath(this.getX(), this.getY(), i, j);
					if (path == null) {
						continue;
					}
					path_distance = path.getpath().size();
					direct_distance = Math.abs(i-this.getX())+Math.abs(j-this.getY());
					System.out.println("Hole: "+name+" "+this.getX()+","+this.getY()+" "+i+","+j+"; path_distance="+path_distance+"; direct_distance="+direct_distance);
					if (direct_distance <= hole_distance && path_distance == direct_distance) {
						hole_distance = direct_distance;
						result[3] = i;
						result[4] = j;
						result[5] = direct_distance;

					}
				}
			}
		}


		return result;

	}


	protected void target_on_the_way(int tx, int ty) {
		int startX = Math.min(this.getX(), tx);
		int startY = Math.min(this.getY(), ty);
		int endX = Math.max(this.getX(), tx);
		int endY = Math.max(this.getY(), ty);

		int[] result = find_nearest_in_range(startX, startY, endX, endY);
		int dt = result[2];
		int dh = result[5];

		targetX = tx;
		targetY = ty;
		if (dt == -1 && dh == -1) {	
			return;
		}

		if (this.carriedTiles.size() < 3 && this.carriedTiles.size() > 0) {
			if (dh <= dt && dh != -1) {
				targetX = result[3];
				targetY = result[4];
			} else if (dh > dt && dt != -1){
				targetX = result[0];
				targetY = result[1];
			}
		} else if (this.carriedTiles.size() == 0 && dt != -1) {
			targetX = result[0];
			targetY = result[1];
		} else if (this.carriedTiles.size() == 3 && dh != -1){
			targetX = result[3];
			targetY = result[4];
		}
	}
}
