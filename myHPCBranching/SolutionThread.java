package myHPCBranching;

import java.util.ArrayList;
import java.util.*;
import java.io.*;

/**
 *
 * @authors Ankit, Abdullah, modified by Sergey
 */

class Pair {

    public Integer first;
    public Integer second;

    public Pair(int first, int second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "( " + first + ", " + second + " )";
    }
}


public class SolutionThread extends Thread {

    public ArrayList<Pair> bestSolution = null;
    public Float bestCost = null;
    public int[] container_classes = null;
    public int[][] communication_matrix = null;
    public int solutions_tried = 0;
    private boolean running;
    private int number_of_nodes = 0;
    private int d = -1;
    private int p = -1;
    private int c = -1;
    
    public int num_of_powered_nodes;
    public int num_of_collocated_devils;
    public int num_of_collocated_comms;

    public int best_num_of_powered_nodes = -1;
    public int best_num_of_collocated_devils = -1;
    public int best_num_of_collocated_comms = -1;
    
    
    
    
    
    public SolutionThread(int[] container_classes, int[][] communication_matrix, int number_of_nodes, int d, int p, int c) {
        this.container_classes = container_classes;
        this.communication_matrix = communication_matrix;
        this.bestSolution = new ArrayList<Pair>();
        this.bestCost = Float.MAX_VALUE;
        this.number_of_nodes = number_of_nodes;
        this.d = d;
        this.p = p;
        this.c = c;
        running = true;
    }


    
    
    
    
    
    SolutionThread() {
    }

    
    
    
    
    
    
    @Override
    public void run() {
        while (running) {
            ArrayList tempSolution = randCombination(container_classes.length, number_of_nodes);
            Float tempCost = cost(tempSolution, container_classes, communication_matrix);
            //minimization
            if (tempCost < bestCost) {
                bestSolution = tempSolution;
                bestCost = tempCost;
                best_num_of_powered_nodes = num_of_powered_nodes;
                best_num_of_collocated_devils = num_of_collocated_devils;
                best_num_of_collocated_comms = num_of_collocated_comms;
            }
            solutions_tried++;
        }
    }


    
    
    
    
    public void timeOut() {
        running = false;
    }

    
    
    
    
    
    
    
    
    
    
    
    
    public ArrayList<Pair> randCombination(int N, int M) {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        for (int i = 0; i < N; i++) {
            arr.add(i, i); // Inserts the specified element at the specified position in this list.
        }
        
        int availableCores = 2*M;
        for(int i=N; i<availableCores; i++){
            arr.add(i,-1);
        }

        Collections.shuffle(arr);
        
        ArrayList<Pair> combs = new ArrayList<Pair>();
        for(int i=0; i<arr.size(); i=i+2){
            combs.add(new Pair(arr.get(i), arr.get(i+1)));
        }
            
        return combs;
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    public float cost(ArrayList<Pair> permutation, int[] container_classes, int[][] communication_matrix) {
        //initially consider all nodes to be powered up (then reduced it on encountering [-1,-1] 
        num_of_powered_nodes = permutation.size();
        num_of_collocated_devils = 0;
        num_of_collocated_comms = 0;

        //assumption : number of processes OR length of permutation is even
        //assumption : number of cores per processor is 2
        for (int i = 0; i < permutation.size(); i++) {
            if (permutation.get(i).first == -1 || permutation.get(i).second == -1) {
                if (permutation.get(i).first == -1 && permutation.get(i).second == -1)
                    num_of_powered_nodes--;
            } else {

                if (container_classes[permutation.get(i).first] == 1 && container_classes[permutation.get(i).first] == container_classes[permutation.get(i).second]) {
                    num_of_collocated_devils++;
                }
                //the communication matrix might not be symmetric.
                //Hence, we need different operators
                if (communication_matrix[permutation.get(i).first][permutation.get(i).second] == 1 || 
                		communication_matrix[permutation.get(i).second][permutation.get(i).first] == 1) {
                    num_of_collocated_comms++;
                }
            }
        }
        
        float weighted_sum = p*num_of_powered_nodes + d*num_of_collocated_devils - c*num_of_collocated_comms;
        return weighted_sum;
    }
}
