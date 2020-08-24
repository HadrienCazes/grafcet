package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Hadrien3Secondary extends Brain {

    /*************************
     ******* PARAMETERS******** /
     *************************/

    private static final double ANGLEPRECISION = 0.015;

    private static final int ROCKY = 0x1EADDA;
    private static final int MARIO = 0x5EC0;
    private static final int TEAM = 0xBADDAD;
    private static final int UNDEFINED = 0xBADC0DE0;

    private static final int FIRE = 0xB52;
    private static final int FALLBACK = 0xFA11BAC;
    private static final int ROGER = 0x0C0C0C0C;
    private static final int OVER = 0xC00010FF;


    /*************************
     ******* STATES************ /
     *************************/

    // MARK : States definition

    private static final int TURNLEFTTASK = 1;
    private static final int MOVETASK = 2;
    private static final int TURNRIGHTTASK = 3;
    private static final int SINK = 0xBADC0DE1;
    /*************************
     ******* ADDED STATES****** /
     *************************/
    private static final int TOWARDENEMY = 4;
    private static final int MOVEBACKTASK = 5;

    /*************************
     ******* DEBUGGING******** /
     *************************/

    // Mark : debugging location

    private static ArrayList<String> listStates =
            new ArrayList<String>(Arrays.asList("TURNLEFTTASK", "MOVETASK", "TURNRIGHTTASK",
                    "TOWARDENEMY", "MOVEBACKTASK", "SINK"));

    
    private Date date;
    private String timeStamp;

    private String timeLog() {
        date = new java.util.Date();
        timeStamp = new SimpleDateFormat("HH:mm:ss").format(date);
        return timeStamp;
      }

    /*************************
     ****** FUNCTIONS******* /
     *************************/

    // Mark: function

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

    private void whoAmI() {
        // in order to recognize which bot it is
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
    }

    private void debugMsg() {
        // DEBUG MESSAGE
        if (whoAmI == ROCKY) {
            sendLogMessage("SECONDARY HAUT (" + (int) myX + ", " + (int) myY + ") state = "
                    + printStateName());
        } else
            sendLogMessage("SECONDARY BAS (" + (int) myX + ", " + (int) myY + ") state = "
                    + printStateName());
    }

    private void println(Object msg) {
        // lazy printing
        System.out.println(msg);
    }

    private void radarDetection() {
    
        for (IRadarResult o : detectRadar()) {
            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot
                    || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
                enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());

                broadcast(whoAmI + ":" + TEAM + ":" + FIRE + ":" + enemyX + ":" + enemyY + ":"
                        + OVER);
                distanceEnemy = (int) o.getObjectDistance();
                detectedEnemy = true;
                
                // debugging logs
                // sendLogMessage("DETECTED("+(int)enemyX+", "+(int)enemyY+") TYPE: "+
                // o.getObjectType());
                // broadcast(whoAmI+":"+TEAM+":"+FALLBACK+":"+enemyX+":"+enemyY+":"+OVER);

                
                //if (whoAmI == UNDEFINED) {
                //    System.out.println(timeLog() + " Me: (" + myX + "," + myY + "); ENEMY: ("
                //            + enemyX + "," + enemyY + ");");
                //    System.out.println(timeLog() + " =====> DETECTED(" + (int) enemyX + ", "
                //            + (int) enemyY + ") TYPE: " + o   boolean isMoving, isMovingBack;
                //   }
                //}
            if (o.getObjectDistance() <= 100) {
                freeze = true;
                }
            }
        }
    }

    /*************************
     *** INSTANCE VARIABLES**** /
     *************************/

    // Mark : variables definition

    // Direction for lazy writing
    private final static Parameters.Direction right = Parameters.Direction.RIGHT;
    private final static Parameters.Direction left = Parameters.Direction.LEFT;
    private final static double north = Parameters.NORTH;
    private final static double south = Parameters.SOUTH;
    private final static double east = Parameters.EAST;
    private final static double west = Parameters.WEST; 

    private int state;
    private double oldAngle;
    private double myX, myY;
    private boolean freeze;
    private int whoAmI;
    private boolean detectedEnemy;
    private double enemyX, enemyY; // to help track the bot
    private double angle; // angle toward enemy in radian
    private int distanceEnemy; // used to keep bot in range of the opponent detected
    private boolean in_range;
    private boolean isMoving,isMovingBack;

    // ---CONSTRUCTORS---//
    public Hadrien3Secondary() {
        super();
    }

    /*************************
     ****** GRAFCET PART******* /
     *************************/

    // ---ABSTRACT-METHODS-IMPLEMENTATION---//
    // Mark: activate secondary
    public void activate() {
        // ODOMETRY FUNCTION TO RECOGNIZE THE BOT'S IDENTITY
        whoAmI();
        // INIT
        state = TURNLEFTTASK;
        detectedEnemy = false;
        isMoving = false;
        isMovingBack = false;
        oldAngle = getHeading();
    }

    // Mark: step secondary
    public void step() {
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

        debugMsg(); // sendLogMsg displaying current status of the bot

        radarDetection(); // for loop that send msg if enemy is detected or not

        if (detectedEnemy) {
            state = TOWARDENEMY;
            angle = Math.atan2((enemyY - myY), (enemyX - myX));
            in_range = (distanceEnemy>=450 && distanceEnemy<=500);
            freeze = (in_range)?true:false;
            detectedEnemy = false; // to reset the bot
        }

        if (state == TOWARDENEMY && !(isRoughlySameDirection(myGetHeading(), angle))) {
            stepTurn(left);
            return;
        }

        if (state == TOWARDENEMY && (isRoughlySameDirection(myGetHeading(), angle))) {
            move();
            state = MOVETASK;
            return;
        }

        if (freeze && isRoughlySameDirection(myGetHeading(), angle)){
            return;
        }
        
        if (state == MOVETASK && in_range){
            freeze = true;
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
        if (state == MOVETASK
                && detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
            myMove(); // And what to do when blind blocked?
            // sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (state == MOVETASK
                && detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
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

    }
}
