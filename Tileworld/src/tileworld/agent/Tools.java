package tileworld.agent;

import java.util.ArrayList;
import java.util.List;

import tileworld.environment.TWEnvironment;

public class Tools {
	//将给定的地图（maxX,maxY）划分为agent数量（5）个区域，比如地图大小40*60,
    protected static List<ArrayList<int[]>> divideMap(int agentNum, int maxX, int maxY){
    	int scanType = maxX>maxY?0:1;
    	List<ArrayList<int[]>> boundaryPoints = new ArrayList<ArrayList<int[]>>();
    	
    	int dividedVal = scanType==0?maxX:maxY;//60,scanType=0
    	int anotherVal = scanType==1?maxX:maxY;//40
    	int num1 = dividedVal/7;//8
    	int remainder1 = dividedVal - num1*7;//4
    	int num2 = num1/agentNum;//1
    	int remainder2 = num1-num2*agentNum;//3
    	
    	int[] points = new int[agentNum];
    	for(int i=0;i<agentNum;i++){
    		if(i<remainder2){
    			points[i] = 7 * (num2+1) * (i+1)-1;
    		}else{
    			if(i<agentNum-1){
    				points[i] = points[i-1] + 7 * num2;
    			}else{
    				points[i] = dividedVal;
    			}
    			
    		}
    	}
    	ArrayList<int[]> list1 = new ArrayList<int[]>();
    	//加入第一个区间
    	list1.add(new int[]{0,0});
    	list1.add(scanType==0?new int[]{points[0], anotherVal}:new int[]{anotherVal, points[0]});
    	boundaryPoints.add(list1);
    	for(int i=1;i<agentNum;i++){
    		ArrayList<int[]> list = new ArrayList<int[]>();
    		list.add(scanType==0?new int[]{points[i-1] + 1, 0}:new int[]{0, points[i-1] + 1});
    		list.add(scanType==0?new int[]{points[i], anotherVal}:new int[]{anotherVal, points[i]});
    		boundaryPoints.add(list);
    	}
    	return boundaryPoints;
    }
    
    
    
  //根据divideMap给的区域，给出agent初始位置的candidates（每个区域4个或者2个）
    protected static List<ArrayList<int[]>> getCandidates(List<ArrayList<int[]>> boundaryPoints){
    	List<ArrayList<int[]>> candidatePoints = new ArrayList<ArrayList<int[]>>();
    	for(int i=0;i<boundaryPoints.size();i++){
    		ArrayList<int[]> list = null;
    		for(int j=0;j<boundaryPoints.get(i).size();j++){
    			if(j%2==0){
        			list = new ArrayList<int[]>();
        			list.add(new int[]{boundaryPoints.get(i).get(j)[0] + 3, boundaryPoints.get(i).get(j)[1] + 3});
        			list.add(new int[]{boundaryPoints.get(i).get(j)[0] + 3, boundaryPoints.get(i).get(j+1)[1] - 3});
        			list.add(new int[]{boundaryPoints.get(i).get(j+1)[0] - 3, boundaryPoints.get(i).get(j+1)[1] - 3});
        			list.add(new int[]{boundaryPoints.get(i).get(j+1)[0] - 3, boundaryPoints.get(i).get(j)[1] + 3});
        		}
    		}
    		candidatePoints.add(list);
    	}
    	return candidatePoints;
    }
    private static int getManhattanDistanceBetween(int x1, int y1, int x2, int y2){
        return Math.abs(x1-x2) + Math.abs(y1-y2);
    }
    // 计算一个agent到一个区域内4个或者2个点的最短距离是哪个点（编号），并给出最短距离
    protected static int[] getDisAgentArea(ArrayList<int[]> candidates, int x, int y){
    	  
    	  int min = Integer.MAX_VALUE;
    	  int minI = -1;
    	  for(int i = 0;i<candidates.size();i++){
    		  int[] point = candidates.get(i);
    		  int dis = getManhattanDistanceBetween(x, y, point[0], point[1]);
    		  if(dis < min){
    			  min = dis;
    			  minI = i;
    		  }
    	  }
    	  int[] ans = new int[]{minI, min};
    	  return ans;
    }
    //全排列找所有可能的最优
    public static List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(result, new ArrayList<>(), nums);
        return result;
    }
    private static void backtrack(List<List<Integer>> result, List<Integer> tempList, int[] nums) {
        if (tempList.size() == nums.length) {
            result.add(new ArrayList<>(tempList));
        } else {
            for (int i = 0; i < nums.length; i++) {
                if (tempList.contains(nums[i])) continue;
                tempList.add(nums[i]);
                backtrack(result, tempList, nums);
                tempList.remove(tempList.size() - 1);
            }
        }
    }
    
    public static void main(String[] args){
    	String name = "agent0";
    	int xpos = 0;
    	int ypos = 0;
    	TWEnvironment env = new TWEnvironment();
    	double fuelLevel = 500;
//    	MyAgent agent = new MyAgent(name, xpos, ypos, env, fuelLevel);
    	int maxX = 80;
    	int maxY = 80;
    	List<ArrayList<int[]>> boundaryPoints = divideMap(5, maxX, maxY);
    	List<ArrayList<int[]>> candidates = getCandidates(boundaryPoints);
//    	getDisAgentArea(candidates.get(0), 6, 0);
    	int[][] agentLocs = new int[][]{{3,26},{77,5},{77,56},{3,36},{77,77}};
    	int[][] match = matchAgentArea(agentLocs, candidates);
    	System.out.println(match);
    }
    
    //将5个agent与划分好的5个区域内的4（2）个点进行匹配，给出效率最高的匹配方式
    protected static int[][] matchAgentArea(int[][] agentLocs, List<ArrayList<int[]>> candidatePoints){
    	int[][] match = new int[candidatePoints.size()][4];
    	int[] ids = new int[candidatePoints.size()];
    	for(int i=0;i<ids.length;i++){
    		ids[i] = i;
    	}
    	List<List<Integer>> permutation = permute(ids);
//    	int tmp = permutation.size();
    	int minTotalDis = Integer.MAX_VALUE;
    	for(int i=0;i<permutation.size();i++){
    		List<Integer> per = permutation.get(i);
    		int[][] tmpMatch = new int[candidatePoints.size()][4];
    		int totalDis = 0;
    		for(int j=0;j<per.size();j++){
    			int agid = per.get(j);
    			int[] idDis = getDisAgentArea(candidatePoints.get(j), agentLocs[agid][0], agentLocs[agid][1]);
    			tmpMatch[agid][0] = candidatePoints.get(j).get(idDis[0])[0];
    			tmpMatch[agid][1] = candidatePoints.get(j).get(idDis[0])[1];
    			tmpMatch[agid][2] = j;
    			tmpMatch[agid][3] = idDis[0];
    			totalDis += idDis[1];
    		}
    		if(totalDis<minTotalDis){
    			minTotalDis = totalDis;
    			match = tmpMatch;
    		}
    	}
    	return match;
    }
}
