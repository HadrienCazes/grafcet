package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult.Types;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class HadrienFinalScouter extends Brain {

    // Direction for lazy writing
    private final static Parameters.Direction right = Parameters.Direction.RIGHT;
    private final static Parameters.Direction left = Parameters.Direction.LEFT;
    private final static double north = Parameters.NORTH;
    private final static double south = Parameters.SOUTH;
    private final static double east = Parameters.EAST;
    private final static double west = Parameters.WEST;

    private final static IFrontSensorResult.Types wall = IFrontSensorResult.Types.WALL;
    private final static IFrontSensorResult.Types nothing = IFrontSensorResult.Types.NOTHING;
    private final static IFrontSensorResult.Types teamMain = IFrontSensorResult.Types.TeamMainBot;
    private final static IFrontSensorResult.Types teamSecondary =
            IFrontSensorResult.Types.TeamSecondaryBot;
    private final static IFrontSensorResult.Types ennemyMain =
            IFrontSensorResult.Types.OpponentMainBot;
    private final static IFrontSensorResult.Types ennemySecondary =
            IFrontSensorResult.Types.OpponentSecondaryBot;
    private final static IFrontSensorResult.Types wreck = IFrontSensorResult.Types.Wreck;

    /**************************************
     *** IDENTIFICATION + COMMUNICATION**** /
     **************************************/


    private static final int ROCKY = 0x1EADDA;
    private static final int SONIC = 0x5EC0;
    private static final int TEAM = 0xBADDAD;
    private static final int UNDEFINED = 0xBADC0DE0;

    private static final int FIRE = 0xB52;
    private static final int FALLBACK = 0xFA11BAC;
    private static final int ROGER = 0x0C0C0C0C;
    private static final int OVER = 0xC00010FF;

    /*************************
     ******* STATES************ /
     *************************/
    
    private static final int FACESIDE = 0;
    private static final int MOVETASK = 1;
    private static final int MOVEBACK = 2;
    private static final int ANGLEDROIT = 3;
    private static final int FACEENEMY = 4;
    private static final int SINK = 0xBADC0DE1;
    // private static final int MOVEBACK = 3;
    // private static final int HUGWALL = 4;

    private static ArrayList<String> listStates =
            new ArrayList<String>(Arrays.asList("FACESIDE", "MOVETASK","MOVEBACK", "ANGLEDROIT","FACEENEMY", "SINK"));

    /*************************
     *** INSTANCE VARIABLES**** /
     *************************/

    private double respectiveOrientation;
    private static final double HEADINGPRECISION = 0.015; // 0.001;
    private static final double ANGLEPRECISION = 0.1; // 0.015;
    private boolean teamA, isMoving, isMovingBack;
    private double myX, myY;
    private int whoAmI;
    private int state;
    private double oldAngle, fullturnangle;
    private double faceEnemyAngle;
    private Parameters.Direction teamDirection;
    private boolean enemyDetected;
    private int distanceEnemy;
    private int cpt_closer_to_wall, cpt_moveback;


    private void println(Object msg) {
        // lazy printing
        System.out.println(timeLog() + " : " + msg);
    }

    private void myMove() {
        isMoving = true;
        move();
    }

    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(dir1 - dir2) < HEADINGPRECISION;
    }

    private boolean isRoughlySameDirection(double dir1, double dir2) {
        return Math.abs(normalizeRadian(dir1) - normalizeRadian(dir2)) < HEADINGPRECISION;
    }

    private void myMoveBack() {
        isMovingBack = true;
        moveBack();
    }

    private String timeLog() {
        Date date = new java.util.Date();
        String timeStamp = new SimpleDateFormat("HH:mm:ss").format(date);
        return timeStamp;
    }


    private void debugMsg() {
        // DEBUG MESSAGE
        if (whoAmI == ROCKY) {
            sendLogMessage("R SECONDARY HAUT (" + (int) myX + ", " + (int) myY + ") state = "
                    + printStateName());
        } else
            sendLogMessage("S SECONDARY BAS (" + (int) myX + ", " + (int) myY + ") state = "
                    + printStateName());
    }

    private String printIdentity() {
        return (whoAmI == ROCKY) ? "ROCKY " : "SONIC ";
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

    private String printStateName() {
        if (state == SINK) {
            return listStates.get(listStates.size() - 1);
        }
        return listStates.get(state);
    }

    private void updateCoordinates() {
        if (teamA) {
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
        } else {
            // TEAM B
            if (isMoving) {
                myX += Parameters.teamBSecondaryBotSpeed * Math.cos(getHeading());
                myY += Parameters.teamBSecondaryBotSpeed * Math.sin(getHeading());
                isMoving = false;
            }
            if (isMovingBack) {
                myX -= Parameters.teamBSecondaryBotSpeed * Math.cos(getHeading());
                myY -= Parameters.teamBSecondaryBotSpeed * Math.sin(getHeading());
                isMovingBack = false;
            }
        }
        if (myX <= 0){
            myX = 0;
        }
        if (myX >= 3000) {
            myX = 3000;
        }
        if (myY >= 2000){
            myY = 2000;
        }
        if (myY <= 0){
            myY = 0;
        }
    }

    private void whoAmI() {
        // in order to recognize which bot it is
        whoAmI = ROCKY;
        state = FACESIDE;
        for (IRadarResult o : detectRadar())
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH))
                whoAmI = SONIC;

        oldAngle = getHeading();
        // TEAM IDENTIFICATION
        if (oldAngle == west) {
            // TEAM B
            teamA = false;
            fullturnangle = Parameters.LEFTTURNFULLANGLE;
            teamDirection = (whoAmI == ROCKY) ? left : right;
        }
        if (oldAngle == east) {
            // TEAM A
            teamA = true;
            fullturnangle = Parameters.RIGHTTURNFULLANGLE;
            teamDirection = (whoAmI == ROCKY) ? right : left;
        }

        // TEAM COORDINATES ASSIGNEMENT
        if (teamA) {
            if (whoAmI == ROCKY) {
                myX = Parameters.teamASecondaryBot1InitX;
                myY = Parameters.teamASecondaryBot1InitY;
            } else {
                myX = Parameters.teamASecondaryBot2InitX;
                myY = Parameters.teamASecondaryBot2InitY;
                fullturnangle = -fullturnangle;
            }
        } else {
            if (whoAmI == ROCKY) {
                myX = Parameters.teamBSecondaryBot1InitX;
                myY = Parameters.teamBSecondaryBot1InitY;
            } else {
                myX = Parameters.teamBSecondaryBot2InitX;
                myY = Parameters.teamBSecondaryBot2InitY;
                fullturnangle = -fullturnangle;
            }
        }
    }

    public HadrienFinalScouter() {
        super();
    }


    @Override
    public void activate() {
        whoAmI(); // assign identity Rocky or Sonic + default state
        cpt_closer_to_wall = 0;
        cpt_moveback = 0;
        enemyDetected = false;
        isMoving = false;
        isMovingBack = false;
        respectiveOrientation = (whoAmI == ROCKY) ? north : south;
    }

    @Override
    public void step() {
        updateCoordinates(); // myX,myY updated
        debugMsg(); // sendLogMsg displaying current status of the bot

        ArrayList<IRadarResult> detected = detectRadar();
        for (IRadarResult o : detected) {
            if (o.getObjectDistance() < 250 && o.getObjectType() != IRadarResult.Types.BULLET
             && o.getObjectType() != IRadarResult.Types.TeamMainBot
             && o.getObjectType() != IRadarResult.Types.TeamSecondaryBot) {
                //println(o.getObjectType());
                double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
                double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
                println("ennemy coordinates: "+ enemyX + ":" + enemyY);
                faceEnemyAngle = Math.atan2((enemyY - myY), (enemyX - myX));
                broadcast(whoAmI+":"+TEAM+":"+FIRE+":"+enemyX+":"+enemyY+":"+OVER);
                state = FACEENEMY;
			}
        }
        
        if (state == FACESIDE) {
            if (!isRoughlySameDirection(getHeading(), respectiveOrientation)) {
                stepTurn(teamDirection);
                return;
            } else {
                state = MOVETASK;
                myMove();
                return;
            }
        }

        if (state == MOVETASK) {
            for (IRadarResult o : detected) {
                if (o.getObjectDistance() < 450 && o.getObjectType() == IRadarResult.Types.BULLET){
                    state = ANGLEDROIT;
                    oldAngle = getHeading();
                    stepTurn(teamDirection);
                    return;
                }
                else{
                    continue;
                }
            }
            IFrontSensorResult.Types o = detectFront().getObjectType();
            if (o == wall) {
                if (cpt_closer_to_wall == 100) {
                    cpt_closer_to_wall = 0;
                    state = ANGLEDROIT;
                    oldAngle = getHeading();
                    stepTurn(teamDirection);
                    return;
                } else {
                    myMove();
                    cpt_closer_to_wall++;
                    return;
                }
            }
            if (o == ennemyMain || o == ennemySecondary || o == teamMain
                || o == teamSecondary) {
                state = ANGLEDROIT;
                oldAngle = getHeading();
                stepTurn(teamDirection);
                return;
            } 
            else {
                myMove();
                return;
            }
        }

        if (state == ANGLEDROIT) {
            if (!isRoughlySameDirection(myGetHeading(), oldAngle + fullturnangle)) {
                stepTurn(teamDirection);
                return;
            } else {
                state = MOVETASK;
                myMove();
                return;
            }
        }

        if (state == FACEENEMY){
            IFrontSensorResult.Types o = detectFront().getObjectType();
            if (o == ennemyMain){
                state = MOVEBACK;
                myMoveBack();
                return;
            }
            if (!isRoughlySameDirection(myGetHeading(), faceEnemyAngle)){
                stepTurn(teamDirection);
                return;
            }
            else{
                state = MOVEBACK;
                myMoveBack();
                return;
            }

        }
        if (state == MOVEBACK){
            if (cpt_moveback == 50){
                cpt_moveback = 0;
                state = ANGLEDROIT;
                oldAngle = getHeading();
                stepTurn(teamDirection);
                return;
            }
            else{
                cpt_moveback++;
                return;
            }
        }
    }

}
