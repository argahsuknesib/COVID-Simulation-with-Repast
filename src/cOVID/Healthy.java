package cOVID;

import java.util.List;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class Healthy {

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private boolean infected;
	public boolean social_isolate;
	private int days_infected;
	
	//initialized the parameters
	Parameters params = RunEnvironment.getInstance().getParameters();
	
	private float prob_to_social_isolate = (Float)params.getValue("prob_to_social_isolate");	
	

	public Healthy(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
		this.infected = false;
		this.days_infected = 0;
		
		if(Math.random() <= prob_to_social_isolate) {
			this.social_isolate = true;
		}
		
	}
	

	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
//		if(Math.random() <= prob_social_isolation)
		if(this.social_isolate)
			return;
		// get the grid location of this Human
		GridPoint pt = grid.getLocation(this);
		// use the GridCellNgh class to create GridCells for
		// the surrounding neighborhood .
		GridCellNgh<Object> nghCreator = new GridCellNgh<Object>(grid, pt,
				Object.class, 1, 1);
		List<GridCell<Object>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

//		GridPoint pointWithLeastZombies = null;
		GridCell<Object> cell = gridCells.get(0);
		
		GridPoint point_to_move = cell.getPoint();
		moveTowards(point_to_move);
		
	}

	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint,
					otherPoint);
			space.moveByVector(this, 2, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());

		}
	}
}
