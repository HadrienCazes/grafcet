/*
 * ****************************************************** Simovies - Eurobot 2015 Robomovies
 * Simulator. Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>. GPL version>=3
 * <http://www.gnu.org/licenses/>. $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Hadrien2Secondary extends Brain {
  // ---PARAMETERS---//
  // VARIABLES DE CLASSE+
  private static final double ANGLEPRECISION = 0.015;

  private static final int ROCKY = 0x1EADDA;
  private static final int MARIO = 0x5EC0;
  private static final int TEAM = 0xBADDAD;
  private static final int UNDEFINED = 0xBADC0DE0;

  private static final int FIRE = 0xB52;
  private static final int FALLBACK = 0xFA11BAC;
  private static final int ROGER = 0x0C0C0C0C;
  private static final int OVER = 0xC00010FF;

  // STATES
  private static final int TURNLEFTTASK = 0;
  private static final int MOVETASK = 1;
  private static final int TURNRIGHTTASK = 2;
  private static final int SINK = 0xBADC0DE1;

  // ADDED STATES
  private static final int TOWARDENEMY = 3;
  private static final int MOVEBACKTASK = 4;
  private static final int MOVETASKBIS = 5;

  // FOR DEBUGGING
  private static ArrayList<String> listStates = new ArrayList<String>(Arrays.asList("TURNLEFTTASK",
      "MOVETASK", "TURNRIGHTTASK", "TOWARDENEMY", "MOVEBACKTASK", "MOVETASKBIS", "SINK"));

  private Date date;
  private String timeStamp;
  // ---VARIABLES LIES A L'INSTANCE---//
  private int state;
  private double oldAngle;
  private double myX, myY;
  private boolean isMoving;
  private boolean isMovingBack;
  private boolean freeze;
  private int whoAmI;
  private boolean senderOfMsg;


  // TMP VARIABLES
  // private ArrayList<Double> historicalAngle = new ArrayList<Double>();
  private double angle;
  private int afkSteps;
  private double enemyX;
  private double enemyY;
  private int distanceEnemy;

  // ---CONSTRUCTORS---//
  public Hadrien2Secondary() {
    super();
  }

  // ---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    // ODOMETRY CODE
    whoAmI = ROCKY;
    for (IRadarResult o : detectRadar())
      if (isSameDirection(o.getObjectDirection(), Parameters.NORTH))
        whoAmI = UNDEFINED;
    if (whoAmI == ROCKY) {
      myX = Parameters.teamASecondaryBot1InitX;
      myY = Parameters.teamASecondaryBot1InitY;
    } else {
      myX = Parameters.teamASecondaryBot2InitX;
      myY = Parameters.teamASecondaryBot2InitY;
    }

    // INIT
    state = TURNLEFTTASK;
    isMovingBack = false;
    isMoving = false;
    senderOfMsg = false;
    afkSteps = 0;
    oldAngle = getHeading();
  }

  public void step() {
    // ODOMETRY CODE
    if (isMoving) {
      myX += Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
      myY += Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
      isMoving = false;
    }
    if (isMovingBack) {
      myX -= Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
      myY -= Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
      isMovingBack = false;
    }

    // DEBUG MESSAGE
    if (whoAmI == ROCKY)
      sendLogMessage(
          "SECONDARY HAUT (" + (int) myX + ", " + (int) myY + ") state = " + printStateName());
    // sendLogMessage("#ROCKY *thinks* he is rolling at position ("+(int)myX+", "+(int)myY+").");
    else
      sendLogMessage(
          "SECONDARY BAS (" + (int) myX + ", " + (int) myY + ") state = " + printStateName());
    // sendLogMessage("#MARIO *thinks* he is rolling at position ("+(int)myX+", "+(int)myY+").");

    // RADAR DETECTION
    freeze = false;
    for (IRadarResult o : detectRadar()) {
      if (o.getObjectType() == IRadarResult.Types.OpponentMainBot
          || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
        enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
        enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
        if (whoAmI == UNDEFINED) {
          System.out.println(timeLog() + " Me: (" + myX + "," + myY + "); ENEMY: (" + enemyX + ","
              + enemyY + ");");
          System.out.println(timeLog() + " =====> DETECTED(" + (int) enemyX + ", " + (int) enemyY
              + ") TYPE: " + o.getObjectType());
        }
        // sendLogMessage("DETECTED("+(int)enemyX+", "+(int)enemyY+") TYPE: "+ o.getObjectType());
        // broadcast(whoAmI+":"+TEAM+":"+FALLBACK+":"+enemyX+":"+enemyY+":"+OVER);
        broadcast(whoAmI + ":" + TEAM + ":" + FIRE + ":" + enemyX + ":" + enemyY + ":" + OVER);
        senderOfMsg = true;
        distanceEnemy = (int) o.getObjectDistance();
      }
      if (o.getObjectDistance() <= 100) {
        freeze = true;
      }
    }
    if (freeze)
      return;
    if (senderOfMsg) {
      // angle = Math.atan((enemyY-myY)/(double)(enemyX-myX)); bad when enemy detected goes behind
      // the scout...
      angle = Math.atan2((enemyY - myY), (enemyX - myX));
      state = TOWARDENEMY;
      senderOfMsg = false; // reset for movebacktask
      if (whoAmI == UNDEFINED) {
        System.out.println(timeLog() + " state is : " + printStateName() + "; angle is : " + angle
            + "; distance to enemy is: " + distanceEnemy + "; my heading is : " + getHeading());
        // System.out.println("==============>*<=======================");
      }
    }
    if (state == TOWARDENEMY && (!isRoughlySameDirection(getHeading(), angle))
        && distanceEnemy <= 500) {
      // System.out.println(timeLog()+" ---------> TURNING");
      // System.out.println("==============>*<=======================");
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }

    if (state == TOWARDENEMY && (!isRoughlySameDirection(getHeading(), angle))
        && distanceEnemy > 500) {
      // if (whoAmI==ROCKY)System.out.println(timeLog()+" ---------> MOVING");
      myMove();
      return;
    }

    if (state == TOWARDENEMY && (isRoughlySameDirection(getHeading(), angle))) {
      state = MOVEBACKTASK;
      myMoveBack();
      return;
    }

    if (state == MOVEBACKTASK && (distanceEnemy >= 450 && distanceEnemy <= 500)
        && (detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING)) {
      myMoveBack();
      return;
    }

    if (state == MOVEBACKTASK && !(distanceEnemy >= 450 && distanceEnemy <= 500)) {
      state = MOVETASKBIS;
      myMove();
      return;
    }

    if (state == MOVETASKBIS && (distanceEnemy >= 450 && distanceEnemy <= 500)) {
      state = MOVEBACKTASK;
      myMoveBack();
      return;
    }

    if (state == MOVETASKBIS
        && (detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING)) {
      state = TURNLEFTTASK;
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }

    if (state == MOVETASKBIS && !(distanceEnemy >= 450 && distanceEnemy <= 500)) {
      myMove();
      return;
    }
    // AUTOMATON MOVING NORTH TO EAST FOR UPPER SECONDARY
    if (state == TURNLEFTTASK && !(isSameDirection(getHeading(), Parameters.NORTH))) {
      stepTurn(Parameters.Direction.LEFT);
      // sendLogMessage("Initial TeamA Secondary Bot1 position. Heading North!");
      return;
    }
    if (state == TURNLEFTTASK && isSameDirection(getHeading(), Parameters.NORTH)) {
      state = MOVETASK;
      myMove();
      // sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state == MOVETASK && detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
      myMove(); // And what to do when blind blocked?
      // sendLogMessage("Moving a head. Waza!");
      return;
    }
    if (state == MOVETASK && detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
      state = TURNRIGHTTASK;
      oldAngle = getHeading();
      stepTurn(Parameters.Direction.RIGHT);
      // sendLogMessage("Iceberg at 12 o'clock. Heading 3!");
      return;
    }
    if (state == TURNRIGHTTASK
        && !(isSameDirection(getHeading(), oldAngle + Parameters.RIGHTTURNFULLANGLE))) {
      stepTurn(Parameters.Direction.RIGHT);
      // sendLogMessage("Iceberg at 12 o'clock. Heading 3!");
      return;
    }
    if (state == TURNRIGHTTASK
        && isSameDirection(getHeading(), oldAngle + Parameters.RIGHTTURNFULLANGLE)) {
      state = MOVETASK;
      myMove();
      // sendLogMessage("Moving a head. Waza!");
      return;
    }

    if (state == SINK) {
      myMove();
      return;
    }

    if (true) {
      return;
    }
  }

  private void myMove() {
    isMoving = true;
    move();
  }

  private boolean isSameDirection(double dir1, double dir2) {
    return Math.abs(dir1 - dir2) < ANGLEPRECISION;
  }

  private void myMoveBack() {
    isMovingBack = true;
    moveBack();
  }

  private String printStateName() {
    if (state == SINK)
      return listStates.get(listStates.size() - 1);
    return listStates.get(state);
  }

  private String timeLog() {
    date = new java.util.Date();
    timeStamp = new SimpleDateFormat("HH:mm:ss").format(date);
    return timeStamp;
  }

  private double myGetHeading() {
    return normalizeRadian(getHeading());
  }

  private double normalizeRadian(double angle) {
    double result = angle;
    while (result < 0)
      result += 2 * Math.PI;
    while (result >= 2 * Math.PI)
      result -= 2 * Math.PI;
    return result;
  }

  private boolean isRoughlySameDirection(double dir1, double dir2) {
    return Math.abs(normalizeRadian(dir1) - normalizeRadian(dir2)) < ANGLEPRECISION;
  }
}
