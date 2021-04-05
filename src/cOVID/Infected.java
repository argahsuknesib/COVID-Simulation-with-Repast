package cOVID;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.formula.functions.T;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import repast.simphony.util.collections.IndexedIterable;

public class Infected {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
//	private boolean moved;
	private int days_infected;
	
	//initialized the parameters
	Parameters params = RunEnvironment.getInstance().getParameters();
	
	private boolean symptomatic;
	public boolean hospitalized;
	
	//you retrieve parameters
	private int max_days = (Integer)params.getValue("max_days");
	private float chance_to_infect = (Float)params.getValue("chance_to_infect");
	private float prob_dying_after_days = (Float)params.getValue("prob_dying_after_days");
	private float prob_recovering = (Float)params.getValue("prob_recovering");
	private float prob_to_go_to_hospital = (Float)params.getValue("prob_to_go_to_hospital");	
	private Hospital hospital;
	
	public Infected(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
		this.days_infected = 0;
		this.symptomatic = false;
		this.hospitalized = false;
		this.hospital = null;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		this.days_infected ++;
		//make person symptomatic after max days
		if(days_infected >= max_days) {
			symptomatic = true;
		}
		
		if (!hospitalized) {
			boolean is_dead = check_if_dead();
			if(!is_dead) {
				// get the grid location of this Zombie
				GridPoint pt = grid.getLocation(this);
	
				// use the GridCellNgh class to create GridCells for
				// the surrounding neighborhood .
				GridCellNgh<Object> nghCreator = new GridCellNgh<Object>(grid, pt,
						Object.class, 1, 1);
				List<GridCell<Object>> gridCells = nghCreator.getNeighborhood(true);
				SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
	
				GridPoint point_to_move = gridCells.get(0).getPoint();
				moveTowards(point_to_move);
				
				infect();
				if (symptomatic)				go_to_hospital();
				
			}
		}
		else {
			double random = Math.random();
			
			if (random <= prob_recovering) {
				GridPoint pt = grid.getLocation(this);
				NdPoint spacePt = space.getLocation(this);
				Context<Object> context = ContextUtils.getContext(this);
				this.hospital.current_capacity++;
				context.remove(this);
				Recovered recovered= new Recovered(space, grid);
				context.add(recovered);
				space.moveTo(recovered, spacePt.getX(), spacePt.getY());
				grid.moveTo(recovered, pt.getX(), pt.getY());
				
			}
		}
	}

	private void go_to_hospital() {
		// TODO Auto-generated method stub
		
		if(Math.random() < prob_to_go_to_hospital) {
			//Write code to get nearest hospital
			//send agent there
			Hospital nearest_hospital = getNearestHospital();
			if(nearest_hospital == null) return;
			NdPoint target_location = space.getLocation(nearest_hospital);
			space.moveTo(this, (double)target_location.getX(),(double)target_location.getY());
			grid.moveTo(this, (int) target_location.getX(), (int) target_location.getY());
			this.hospital = nearest_hospital;
			hospitalized = true;
		}
	}
	public Hospital getNearestHospital() {
		double minDistSq = Double.POSITIVE_INFINITY;
		Hospital minAgent = null;
		NdPoint myLocation;
		Context context = ContextUtils.getContext(this);

		for (Object agent : context) {
			if (agent instanceof Hospital) {
				Hospital thishospital = (Hospital) agent;
				if (thishospital.current_capacity > 0 ) {
					NdPoint currloc = space.getLocation(this);
					NdPoint loc = space.getLocation(agent);
					double distSq = currloc.getX()- loc.getX() * currloc.getX()- loc.getX() + currloc.getY() - loc.getY() * currloc.getY() - loc.getY();
					if (distSq < minDistSq) {
						minDistSq = distSq;
						minAgent = (Hospital) agent;
					}
					
				}
			}
		}
		if(minAgent != null)		minAgent.current_capacity --;
		return minAgent;
	}
	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint,
					otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());

		}
	}

	
	public void infect() {
		GridPoint pt = grid.getLocation(this);
		List<Object> healthy = new ArrayList<Object>();
		//Get all healthys at the new location
		
		for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())) {
			if (obj instanceof Healthy) {
				healthy.add(obj);
			}
		}
		
		//infect any random healthy
		if (healthy.size() > 0) {
			for (Object obj : healthy) {
				double random = Math.random();
				if (random <= chance_to_infect && !((Healthy) obj).social_isolate) {
					NdPoint spacePt = space.getLocation(obj);
					Context<Object> context = ContextUtils.getContext(obj);
					context.remove(obj);
					Infected infected= new Infected(space, grid);
					context.add(infected);
					space.moveTo(infected, spacePt.getX(), spacePt.getY());
					grid.moveTo(infected, pt.getX(), pt.getY());

					Network<Object> net = (Network<Object>) context.getProjection("infection network");
					net.addEdge(this, infected);
				}
			}
			
		}
	}
	
	// ONLY A SYMPTOMATIC PERSON CAN DIE ACCORDING TO THIS LOGIC since they have to be symptomatic
	public boolean check_if_dead() {
		double random = Math.random();
		GridPoint pt = grid.getLocation(this);
		//if random number <= prob to die kill the person, and make new agent Dead
		if(random <= prob_dying_after_days && symptomatic) {
			NdPoint spacePt = space.getLocation(this);
			Context<Object>  context = ContextUtils.getContext(this);
			if (this.hospital != null) this.hospital.current_capacity++;
			context.remove(this);
			Dead dead = new Dead(space,grid);
			context.add(dead);
			space.moveTo(dead, spacePt.getX(), spacePt.getY());
			grid.moveTo(dead, pt.getX(), pt.getY());
			return true;
		
		}
		return false;
	}
	
	public int getHospitalizedCount() {
		if (hospitalized == true ) return 1;
  	
		return 0;
		
	}
}

