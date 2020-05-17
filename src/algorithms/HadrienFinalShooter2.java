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
import java.util.Random;

public class HadrienFinalShooter2 extends Brain {


    public HadrienFinalShooter2() {
        super();
        gen = new Random();
    }

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


    private static final int ALPHA = 0x1EADDA;
    private static final int BETA = 0x5EC0;
    private static final int GAMMA = 0x333;
    private static final int TEAM = 0xBADDAD;
    private static final int UNDEFINED = 0xBADC0DE0;


    private static final int FIRE = 0xB52;
    private static final int FALLBACK = 0xFA11BAC;
    private static final int ROGER = 0x0C0C0C0C;
    private static final int OVER = 0xC00010FF;

    /*************************
     ******* STATES************ /
     *************************/

    private static final int FACESOUTH = 0;
    private static final int MOVETASK = 1;
    private static final int MOVEBACK = 2;
    private static final int ANGLEDROIT = 3;
    private static final int FACEENEMY = 4;
    private static final int SINK = 0xBADC0DE1;
    // private static final int MOVEBACK = 3;
    // private static final int HUGWALL = 4;

    private static ArrayList<String> listStates = new ArrayList<String>(
            Arrays.asList("FACESOUTH", "MOVETASK", "MOVEBACK", "ANGLEDROIT", "FACEENEMY", "SINK"));

    /*************************
     *** INSTANCE VARIABLES**** /
     *************************/

    private Random gen;
    private double respectiveOrientation;
    private static final double HEADINGPRECISION = 0.015; // 0.001;
    private static final double ANGLEPRECISION = 0.1; // 0.015;
    private boolean teamA, isMoving, isMovingBack, randomFire, fireTarget;
    private double myX, myY;
    private int whoAmI;
    private int state;
    private double oldAngle, fullturnangle;
    private double faceEnemyAngle;
    private Parameters.Direction teamDirection;
    private boolean enemyDetected;
    private int distanceEnemy;
    private int cpt_wait_RF, fireSteps, cpt_moveback;
    private double targetX, targetY;


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
        if (whoAmI == ALPHA && state != SINK) {
            sendLogMessage("A MAIN BAS (" + (int) myX + ", " + (int) myY + ") state = "
                    + printStateName());
        }
        if (whoAmI == BETA && state != SINK) {
            sendLogMessage("B MAIN MILIEU (" + (int) myX + ", " + (int) myY + ") state = "
                    + printStateName());
        }
        if (whoAmI == GAMMA && state != SINK) {
            sendLogMessage("G MAIN HAUT (" + (int) myX + ", " + (int) myY + ") state = "
                    + printStateName());
        }
    }

    private String printIdentity() {
        if (whoAmI == ALPHA) {
            return "ALPHA";
        }
        if (whoAmI == BETA) {
            return "BETA";
        }
        if (whoAmI == GAMMA) {
            return "GAMMA";
        }
        return "UNDEFINED";
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

    private void firePosition(double x, double y) {
        fire(Math.atan2((targetY - myY), (targetX - myX)));
        return;
    }

    private void process(String message) {
        if (Integer.parseInt(message.split(":")[2]) == FIRE) {
            fireTarget = true;
            randomFire = false;
            targetX = Double.parseDouble(message.split(":")[3]);
            targetY = Double.parseDouble(message.split(":")[4]);
        }
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
    }

    private void whoAmI() {
        // in order to recognize which bot it is
        whoAmI = GAMMA;
        state = FACESOUTH;
        for (IRadarResult o : detectRadar())
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH))
                whoAmI = ALPHA;
        for (IRadarResult o : detectRadar())
            if (isSameDirection(o.getObjectDirection(), Parameters.SOUTH) && whoAmI != GAMMA)
                whoAmI = BETA;
        if (whoAmI == GAMMA) {
            myX = Parameters.teamAMainBot1InitX;
            myY = Parameters.teamAMainBot1InitY;
        }
        if (whoAmI == BETA) {
            myX = Parameters.teamAMainBot2InitX;
            myY = Parameters.teamAMainBot2InitY;
        }
        if (whoAmI == ALPHA) {
            myX = Parameters.teamAMainBot3InitX;
            myY = Parameters.teamAMainBot3InitY;
        }
        // TEAM IDENTIFICATION
        if (oldAngle == west) {
            // TEAM B
            teamA = false;
            fullturnangle = Parameters.RIGHTTURNFULLANGLE;
            teamDirection = right;
        }
        if (oldAngle == east) {
            // TEAM A
            teamA = true;
            fullturnangle = Parameters.LEFTTURNFULLANGLE;
            teamDirection = left;
        }

        // TEAM COORDINATES ASSIGNEMENT
        if (teamA) {
            if (whoAmI == GAMMA) {
                myX = Parameters.teamAMainBot1InitX;
                myY = Parameters.teamAMainBot1InitY;
            }
            if (whoAmI == BETA) {
                myX = Parameters.teamAMainBot2InitX;
                myY = Parameters.teamAMainBot2InitY;
            }
            if (whoAmI == ALPHA) {
                myX = Parameters.teamAMainBot3InitX;
                myY = Parameters.teamAMainBot3InitY;
            }
        } else {
            if (whoAmI == GAMMA) {
                myX = Parameters.teamBMainBot1InitX;
                myY = Parameters.teamBMainBot1InitY;
            }
            if (whoAmI == BETA) {
                myX = Parameters.teamBMainBot2InitX;
                myY = Parameters.teamBMainBot2InitY;
            }
            if (whoAmI == ALPHA) {
                myX = Parameters.teamBMainBot3InitX;
                myY = Parameters.teamBMainBot3InitY;
            }
        }
    }

    @Override
    public void activate() {
        whoAmI();
        isMoving = false;
        isMovingBack = false;
        randomFire = false;
        fireSteps = 0;
        cpt_wait_RF = 0;
        fireTarget = false;
    }

    @Override
    public void step() {
        updateCoordinates();
        debugMsg();

        ArrayList<IRadarResult> radarResults = detectRadar();
        for (IRadarResult r : radarResults) {
            if (r.getObjectType() == IRadarResult.Types.OpponentMainBot
                    || r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                double enemyX = myX + r.getObjectDistance() * Math.cos(r.getObjectDirection());
                double enemyY = myY + r.getObjectDistance() * Math.sin(r.getObjectDirection());
                broadcast(whoAmI + ":" + TEAM + ":" + FIRE + ":" + enemyX + ":" + enemyY + ":"
                        + OVER);
                fire(r.getObjectDirection());
                return;
            }
        }
        if (state == FACESOUTH) {
            if (!isRoughlySameDirection(myGetHeading(), south)) {
                stepTurn(teamDirection);
                return;
            } else {
                state = MOVETASK;
                myMove();
                return;
            }
        }
        if (state == MOVETASK) {
            IFrontSensorResult.Types o = detectFront().getObjectType();
            if (o == wall || o == wreck) {
                state = ANGLEDROIT;
                oldAngle = myGetHeading();
                stepTurn(teamDirection);
                return;
            }

            for (IRadarResult r : radarResults) {
                // if (r.getObjectType() == IRadarResult.Types.TeamMainBot
                // || r.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
                if (r.getObjectType() == IRadarResult.Types.TeamMainBot
                        || r.getObjectType() == IRadarResult.Types.TeamSecondaryBot){
                        //|| r.getObjectType() == IRadarResult.Types.Wreck) {

                    if (r.getObjectDistance() < 150) {
                        state = MOVEBACK;
                        cpt_moveback = 0;
                        myMoveBack();
                        return;
                    }
                }
            }
            myMove();
            return;
        }

        if (state == MOVEBACK) {
            //IFrontSensorResult.Types o = detectFront().getObjectType();
            for (IRadarResult r : radarResults) {
                // if (r.getObjectType() == IRadarResult.Types.TeamMainBot
                // || r.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
                if (r.getObjectType() == IRadarResult.Types.TeamMainBot
                        || r.getObjectType() == IRadarResult.Types.TeamSecondaryBot
                        || r.getObjectType() == IRadarResult.Types.Wreck) {

                    if (r.getObjectDistance() < 150) {
                        state = ANGLEDROIT;
                        oldAngle = myGetHeading();
                        stepTurn(teamDirection);
                        return;
                    }
                }
            }
            IFrontSensorResult.Types o = detectFront().getObjectType();
            if (o == wreck){
                cpt_moveback=0;
                state = ANGLEDROIT;
                oldAngle = myGetHeading();
                stepTurn(teamDirection);
                return;
            }
            if (cpt_moveback < 100) {
                cpt_moveback++;
                myMoveBack();
                return;
            } else {
                if (o == nothing || o == teamMain || o == teamSecondary){
                    cpt_moveback=0;
                    state = MOVETASK;
                    myMove();
                    return;
                }
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
    }
}
