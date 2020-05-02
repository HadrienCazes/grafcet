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
    private static final int TURNLEFTTASK = 1;
    private static final int MOVETASK = 2;
    private static final int TURNRIGHTTASK = 3;
    private static final int SINK = 0xBADC0DE1;
    /*************************
     ******* ADDED STATES****** /
     *************************/
    private static final int TOWARDENEMY = 3;
    private static final int MOVEBACKTASK = 4;

    /*************************
     ******* DEBUGGING******** /
     *************************/
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
     *** INSTANCE VARIABLES**** /
     *************************/
    private int state;
    private double oldAngle;
    private double myX,myY;
    private boolean isMoving;
    private boolean freeze;
    private int whoAmI;

    // ---CONSTRUCTORS---//
    public Hadrien3Secondary() {
        super();
    }

    /*************************
     ****** GRAFCET PART******* /
     *************************/

    // ---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {

    }

    public void step(){

    }
}