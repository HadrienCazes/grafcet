package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;


public class HadrienSecondary extends Brain{
    /*************************
     *** INSTANCE VARIABLES**** /
     *************************/

    // Mark : variables definition TEST

    // Direction for lazy writing
    private final static Parameters.Direction right = Parameters.Direction.RIGHT;
    private final static Parameters.Direction left = Parameters.Direction.LEFT;
    private final static double north = Parameters.NORTH;
    private final static double south = Parameters.SOUTH;
    private final static double east = Parameters.EAST;
    private final static double west = Parameters.WEST;
    
	private final static IFrontSensorResult.Types wall = IFrontSensorResult.Types.WALL;
    private final static IFrontSensorResult.Types nothing = IFrontSensorResult.Types.NOTHING;

    private double fullturnangle;
    private Parameters.Direction teamDirection;
    private ArrayList<IFrontSensorResult.Types> toDodge = new ArrayList<IFrontSensorResult.Types>(Arrays.asList
    (IFrontSensorResult.Types.OpponentMainBot, 
    IFrontSensorResult.Types.OpponentSecondaryBot,
    IFrontSensorResult.Types.TeamMainBot,
    IFrontSensorResult.Types.TeamSecondaryBot,
    IFrontSensorResult.Types.Wreck
   ,IFrontSensorResult.Types.WALL
    ));

    private ArrayList<Double> angleArray = new ArrayList<Double>(Arrays.asList
    (0.0,90.0,180.0,270.0,360.0));

    private int state;
    private double oldAngle;
    private double myX, myY;
    private double oldmyX,oldmyY;
    private boolean freeze;
    private int whoAmI;
    private boolean detectedEnemy;
    private double enemyX, enemyY; // to help track the bot
    private double angle; // angle toward enemy in radian
    private int distanceObject; // used to keep bot in range of the opponent detected
    private boolean objectClose;
    private boolean isMoving,isMovingBack;
    private double objectiveHugWallX;
    private double objectiveHugWallY;
    private String directionHugWall, directionFaceEnemy;
    private double sideAngle;

    private int tmp_run;
    
    /*************************
     ******* PARAMETERS******** /
     *************************/

    private static final double ANGLEPRECISION = 0.001;

    private static final int ROCKY = 0x1EADDA;
    private static final int SONIC = 0x5EC0;
    private static final int TEAM = 0xBADDAD;
    private boolean teamA; // indicates if current robot is team A or B.
    private static final int UNDEFINED = 0xBADC0DE0;

    private static final int FIRE = 0xB52;
    private static final int FALLBACK = 0xFA11BAC;
    private static final int ROGER = 0x0C0C0C0C;
    private static final int OVER = 0xC00010FF;


    /*************************
     ******* STATES************ /
     *************************/

    // MARK : States definition TEST

    private static final int FACESIDE = 0;
    private static final int MOVETASK = 1;
    private static final int ANGLEDROIT = 2;
    private static final int SINK = 0xBADC0DE1;
    private static final int MOVEBACK = 3;
    private static final int HUGWALL = 4;
    private static final int FACEENEMY = 5;

    /*************************
     ******* ADDED STATES****** /
     *************************/
    /* private static final int FACEDIRECTION = 3;
    private static final int TURNANGLEDROIT = 4; */
    
    /*************************
     ******* DEBUGGING******** /
     *************************/

    // Mark : debugging location TEST

    private static ArrayList<String> listStates =
            new ArrayList<String>(Arrays.asList("FACESIDE", "MOVETASK", "ANGLEDROIT","MOVEBACK","HUGWALL","FACEENEMY","SINK"));

    
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

    // Mark: function TEST

    private void updateCoordinates(){
        if (teamA){
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
        }
        else{
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
        if (state == SINK){
          return listStates.get(listStates.size() - 1);
        }
        return listStates.get(state);
      }

    private String printIdentity(){
        return (whoAmI==ROCKY)?"ROCKY ":"SONIC ";
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
        sideAngle = north;
        for (IRadarResult o : detectRadar()){
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)){
                whoAmI = SONIC;
                sideAngle = south;
            }
        }
        oldAngle = getHeading();
        // TEAM IDENTIFICATION
        if (oldAngle == west){
            // TEAM B
            teamA = false;
            fullturnangle = Parameters.LEFTTURNFULLANGLE;
            teamDirection = (whoAmI==ROCKY)?right:left;
        }
        if (oldAngle == east){
            // TEAM A
            teamA = true;
            fullturnangle = Parameters.RIGHTTURNFULLANGLE;
            teamDirection = (whoAmI==ROCKY)?left:right;
        }

        // TEAM COORDINATES ASSIGNEMENT
        if (teamA){
            if (whoAmI == ROCKY) {
                myX = Parameters.teamASecondaryBot1InitX;
                myY = Parameters.teamASecondaryBot1InitY;
            } else {
                myX = Parameters.teamASecondaryBot2InitX;
                myY = Parameters.teamASecondaryBot2InitY;
            }
        }
        else{
            if (whoAmI == ROCKY) {
                myX = Parameters.teamBSecondaryBot1InitX;
                myY = Parameters.teamBSecondaryBot1InitY;
            } else {
                myX = Parameters.teamBSecondaryBot2InitX;
                myY = Parameters.teamBSecondaryBot2InitY;
            }
        }
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

    private void println(Object msg) {
        // lazy printing
        System.out.println(timeLog() + " : " + msg);
    }

    private void radarDetection() {
        for (IRadarResult o : detectRadar()) {
            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot
                    || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
                enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());

                broadcast(whoAmI + ":" + TEAM + ":" + FIRE + ":" + enemyX + ":" + enemyY + ":"
                        + OVER);
                distanceObject = (int) o.getObjectDistance();
                detectedEnemy = true;
                
            if (o.getObjectDistance() <= 300) {
                objectClose = true;
                }
            }
            if (o.getObjectType() == IRadarResult.Types.TeamMainBot
                    || o.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
                if (o.getObjectDistance() <= 50) {
                    objectClose = true;
                    distanceObject = (int) o.getObjectDistance();
                }
            }
        }
    }

    private boolean inGround(){
        // Play ground limits
        return myX>100 && myX<2900 && myY>100 && myY<2900;
    }
    // ---CONSTRUCTORS---//
    public HadrienSecondary() {
        super();
    }

    // ---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
		// ODOMETRY FUNCTION TO RECOGNIZE THE BOT'S IDENTITY
        whoAmI();
        // INIT
        state = FACESIDE;
        detectedEnemy = false;
        isMoving = false;
        isMovingBack = false;
        tmp_run = 0;
        if (whoAmI==SONIC){
            fullturnangle = -fullturnangle;
        }
    }

    public void step() {
        
        updateCoordinates(); // myX,myY updated

        debugMsg(); // sendLogMsg displaying current status of the bot

        radarDetection(); // for loop that send msg if enemy is detected or not

        if (state == FACESIDE){
            if (!isRoughlySameDirection(getHeading(), sideAngle)) {
                stepTurn(teamDirection);
            }
            else{
                state = MOVETASK;
                myMove();
            }
            return;
        }

        if (state == MOVETASK){
            //println(tmp_run);
            //println(detectFront().getObjectType());
            if (detectFront().getObjectType() == nothing){
                myMove();
                return;
            }

            if (detectFront().getObjectType() == IFrontSensorResult.Types.OpponentMainBot || 
                detectFront().getObjectType() == IFrontSensorResult.Types.OpponentSecondaryBot){
                tmp_run = 100;
            }

            if (detectFront().getObjectType() == wall && inGround()) {
                tmp_run ++;
                myMove();
                return;
            }
            if (detectFront().getObjectType() == wall && !inGround()) {
                tmp_run = 100;
            }

            if (tmp_run >= 100){
                state = ANGLEDROIT;
                oldAngle = myGetHeading();
                stepTurn(teamDirection);
                return;
            }
        }
        if (state == ANGLEDROIT){
            tmp_run = 0;
            if (!isRoughlySameDirection(myGetHeading(), oldAngle +fullturnangle)){
                stepTurn(teamDirection);
                return;
            }
            else{
                state = MOVETASK;
                myMove();
                return;
                }
            }
        }
    }
