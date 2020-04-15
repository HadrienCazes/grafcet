/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;

public class Stage31Robot extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;
  private static final double ANGLEPRECISION = 0.1;

  private static final int CHOSEN_ONE = 0x1EADDA;
  private static final int NOT_CHOSEN_ONE = 0x5EC0;
  private static final int UNDEFINED = 0xBADC0DE0;

  private int turnCpt = 0;
  private static final int TURNLEFTTASK = 1;
  private static final int MOVETASK = 2;
  private static final int TURNRIGHTTASK = 3;
  private static final int MOVETASKTURNAROUND = 0;
  private static final int TURNAROUND = 4;
  private static final int TURNAROUNDAGAIN = 5;
  private static final int SINK = 0xBADC0DE1;

  //---VARIABLES---//
  private int state;
  private double oldAngle;
  private double myX,myY;
  private boolean isMoving;
  private int whoAmI;

  //---CONSTRUCTORS---//
  public Stage31Robot() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    //ODOMETRY CODE
    whoAmI = CHOSEN_ONE;
    for (IRadarResult o: detectRadar())
      if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=NOT_CHOSEN_ONE;
    if (whoAmI == CHOSEN_ONE){
      myX=Parameters.teamASecondaryBot1InitX;
      myY=Parameters.teamASecondaryBot1InitY;
    } else {
      myX=0;
      myY=0;
    }
    //INIT
    state=(whoAmI==CHOSEN_ONE)?TURNLEFTTASK:SINK;
    isMoving=false;
    oldAngle=getHeading();
  }
  public void step() {
    //ODOMETRY CODE
    if (isMoving && whoAmI == CHOSEN_ONE){
      myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
      myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
      isMoving=false;
    }
    //DEBUG MESSAGE
    if (whoAmI == CHOSEN_ONE) {
      sendLogMessage("#ROCKY *thinks* he is rolling at position ("+(int)myX+", "+(int)myY+").");
    }

    //AUTOMATON
    if (state==TURNLEFTTASK && !(isSameDirection(getHeading(),Parameters.NORTH))) {
      stepTurn(Parameters.Direction.LEFT); //change orientation
      //sendLogMessage("Initial TeamA Secondary Bot1 position. Heading North!");
      return;
    }
    if (state==TURNLEFTTASK && isSameDirection(getHeading(),Parameters.NORTH)) {
      state=MOVETASK; // move toward new orientation
      myMove();
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVETASK && detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
      myMove(); // move toward wall 
      //And what to do when blind blocked?
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVETASK && detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      state=TURNRIGHTTASK;
      oldAngle=getHeading();
      System.out.println(oldAngle);
      stepTurn(Parameters.Direction.RIGHT);
      //sendLogMessage("Iceberg at 12 o'clock. Heading 3!");
      return;
    }
    if (state==TURNRIGHTTASK && !(isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE))) {
      stepTurn(Parameters.Direction.RIGHT);
      //sendLogMessage("Iceberg at 12 o'clock. Heading 3!");
      return;
    }
    if (state==TURNRIGHTTASK && isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE)) {
      state=MOVETASKTURNAROUND;
      myMove();
      //sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state==MOVETASKTURNAROUND && isSameDirection(getHeading(), Parameters.EAST)) {
      if ((turnCpt == 0 && myX >= 750)||(turnCpt == 1 && myX >= 1500)||
         (turnCpt == 2 && myX >= 2250)){
          state = TURNAROUND;
          stepTurn(Parameters.Direction.RIGHT);
          turnCpt++;
          return;
      }
      else{
        myMove();
      }
    }

    if (state==TURNAROUND && !isSameDirection(getHeading(), Parameters.WEST)){
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }

    if (state==TURNAROUND && isSameDirection(getHeading(), Parameters.WEST)){
      state = TURNAROUNDAGAIN;
      myMove();
      return;
    }
    if (state==TURNAROUNDAGAIN && !isSameDirection(getHeading(), Parameters.EAST)){
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }

    if (state==TURNAROUNDAGAIN && isSameDirection(getHeading(), Parameters.EAST)){
      state = MOVETASKTURNAROUND;
      myMove();
      return;
    }

    if (state==SINK) {
      myMove();
      return;
    }
    if (true) {
      return;
    }
  }
  private void myMove(){
    isMoving=true;
    move();
  }
  
  private boolean isSameDirection(double dir1, double dir2){
    return Math.abs(normalize(dir1)-normalize(dir2))<ANGLEPRECISION;
  }
  private double normalize(double dir){
    double res=dir;
    while (res<0) res+=2*Math.PI;
    while (res>=2*Math.PI) res-=2*Math.PI;
    return res;
  }
}
