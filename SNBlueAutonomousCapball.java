package org.firstinspires.ftc.teamcode.etrobot;


import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

/**
 * Created by megan on 11/8/2016.
 */
@Autonomous(name="SN Blue Autonomous: Capball", group="ET Robot")
public class SNBlueAutonomousCapball extends LinearOpMode{
    /* Declare OpMode members. */
    SNRobotHardware robot   = new SNRobotHardware();   // Use a Pushbot's hardware
    ModernRoboticsI2cGyro gyro    = null;                    // Additional Gyro device

    static final double     COUNTS_PER_MOTOR_REV    = 1440 ;    // eg: TETRIX Motor Encoder
    static final double     DRIVE_GEAR_REDUCTION    = 2.0 ;     // This is < 1.0 if geared UP
    static final double     WHEEL_DIAMETER_INCHES   = 4.0 ;     // For figuring circumference
    static final double     COUNTS_PER_INCH         = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
            (WHEEL_DIAMETER_INCHES * 3.1415);

    // These constants define the desired driving/control characteristics
    // The can/should be tweaked to suite the specific robot drive train.
    static final double     DRIVE_SPEED             = 0.7;     // Nominal speed for better accuracy.
    static final double     TURN_SPEED              = 0.5;     // Nominal half speed for better accuracy.
    static final double     CLIMB_SPEED             = 0.9;

    static final double     HEADING_THRESHOLD       = 1 ;      // As tight as we can make it with an integer gyro
    static final double     P_TURN_COEFF            = 0.1;     // Larger is more responsive, but also less stable
    static final double     P_DRIVE_COEFF           = 0.15;     // Larger is more responsive, but also less stable


    @Override
    public void runOpMode() {

        /*
         * Initialize the standard drive system variables.
         * The init() method of the hardware class does most of the work here
         */
        robot.init(hardwareMap);
        gyro = (ModernRoboticsI2cGyro)hardwareMap.gyroSensor.get("gyro");


        // Ensure the robot it stationary, then reset the encoders and calibrate the gyro.
        robot.leftFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.rightFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Send telemetry message to alert driver that we are calibrating;
        telemetry.addData(">", "Calibrating Gyro");    //
        telemetry.update();

        gyro.calibrate();

        // make sure the gyro is calibrated before continuing
        while (!isStopRequested() && gyro.isCalibrating())  {
            sleep(50);
            idle();
        }

        telemetry.addData(">", "Robot Ready.");    //
        telemetry.update();

        robot.leftFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robot.rightFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        // Wait for the game to start (Display Gyro value), and reset gyro before we move..
        while (!isStarted()) {
            telemetry.addData(">", "Robot Heading = %d", gyro.getIntegratedZValue());
            telemetry.update();
            idle();
        }
        gyro.resetZAxisIntegrator();

        // Step through each leg of the path,
        // Note: Reverse movement is obtained by setting a negative distance (not speed)
        // Put a hold after each turn
        gyroDrive(DRIVE_SPEED, 24.0, 0.0);    // Drive FWD 24 inches
        gyroDrive(DRIVE_SPEED, -12.0, 0.0);
        gyroTurn( TURN_SPEED, -90.0);         // Turn  CCW to -45 Degrees
       gyroDrive (DRIVE_SPEED, 13.0, -90.0);   //Drive FWD 48 inches
        gyroTurn(TURN_SPEED, -135.0);
        gyroDrive (CLIMB_SPEED, 10.0, -135.0);
        sleep (3000);
        gyroDrive (CLIMB_SPEED, 10.0, -135.0);
        //gyroHold( TURN_SPEED, -45.0, 30.0);    // Hold -45 Deg heading for a 1/2 second
       // gyroTurn( TURN_SPEED,  -135.0);         // Turn  CW  to  45 Degrees
        //gyroHold( TURN_SPEED,  45.0, 0.5);    // Hold  45 Deg heading for a 1/2 second
        //gyroTurn( TURN_SPEED,   0.0);         // Turn  CW  to   0 Degrees
        //gyroHold( TURN_SPEED,   0.0, 1.0);    // Hold  0 Deg heading for a 1 second
        //gyroDrive(DRIVE_SPEED,15.0, -135.0);    // Drive REV 48 inches
        //gyroHold( TURN_SPEED,   0.0, 0.5);    // Hold  0 Deg heading for a 1/2 second

        telemetry.addData("Path", "Complete");
        telemetry.update();
    }


    /**
     *  Method to drive on a fixed compass bearing (angle), based on encoder counts.
     *  Move will stop if either of these conditions occur:
     *  1) Move gets to the desired position
     *  2) Driver stops the opmode running.
     *
     * @param speed      Target speed for forward motion.  Should allow for _/- variance for adjusting heading
     * @param distance   Distance (in inches) to move from current position.  Negative distance means move backwards.
     * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
     *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                   If a relative angle is required, add/subtract from current heading.
     */

    public void gyroDrive ( double speed,
                            double distance,
                            double angle) {

        int     newLeftFrontTarget;
        int     newRightFrontTarget;
        int     moveCounts;
        double  max;
        double  error;
        double  steer;
        double  leftFrontSpeed;
        double  rightFrontSpeed;


        // Ensure that the opmode is still active
        if (opModeIsActive()) {

            // Determine new target position, and pass to motor controller
            moveCounts = (int)(distance * COUNTS_PER_INCH);
            newLeftFrontTarget = robot.leftFrontMotor.getCurrentPosition() + moveCounts;
            newRightFrontTarget = robot.rightFrontMotor.getCurrentPosition() - moveCounts;

            // Set Target and Turn On RUN_TO_POSITION
            robot.leftFrontMotor.setTargetPosition(newLeftFrontTarget);
            robot.rightFrontMotor.setTargetPosition(newRightFrontTarget);

            robot.leftFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.rightFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // start motion.
            speed = Range.clip(Math.abs(speed), 0.0, 1.0);
            robot.leftFrontMotor.setPower(speed);
            robot.rightFrontMotor.setPower(-speed);


            // keep looping while we are still active, and ALL FOUR motors are running.
            while (opModeIsActive() &&
                    (robot.leftFrontMotor.isBusy() && robot.rightFrontMotor.isBusy() )) {

                // adjust relative speed based on heading error.
                error = getError(angle);
                steer = getSteer(error, P_DRIVE_COEFF);

                // if driving in reverse, the motor correction also needs to be reversed
                if (distance < 0)
                    steer *= -1.0;

                leftFrontSpeed = speed - steer;
                rightFrontSpeed = speed + steer;

                // Normalize speeds if any one exceeds +/- 1.0;
                max = Math.max(Math.abs(leftFrontSpeed), Math.abs(rightFrontSpeed));
                if (max > 1.0)
                {
                    leftFrontSpeed /= max;
                    rightFrontSpeed /= max;
                }

                robot.leftFrontMotor.setPower(leftFrontSpeed);
                robot.rightFrontMotor.setPower(-rightFrontSpeed);

                // Display drive status for the driver.
                telemetry.addData("Err/St",  "%5.1f/%5.1f",  error, steer);
                telemetry.addData("Target",  "%7d:%7d",      newLeftFrontTarget,  newRightFrontTarget);
                telemetry.addData("Actual",  "%7d:%7d",      robot.leftFrontMotor.getCurrentPosition(),
                        robot.rightFrontMotor.getCurrentPosition());
                telemetry.addData("Speed",   "%5.2f:%5.2f",  leftFrontSpeed, rightFrontSpeed);
                telemetry.update();
            }

            // Stop all motion;
            robot.leftFrontMotor.setPower(0);
            robot.rightFrontMotor.setPower(0);
            telemetry.addData("Strop Robot: ", "GyroDrive");

            // Turn off RUN_TO_POSITION
            robot.leftFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.rightFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
    }

    /**
     *  Method to spin on central axis to point in a new direction.
     *  Move will stop if either of these conditions occur:
     *  1) Move gets to the heading (angle)
     *  2) Driver stops the opmode running.
     *
     * @param speed Desired speed of turn.
     * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
     *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                   If a relative angle is required, add/subtract from current heading.
     */
    public void gyroTurn (  double speed, double angle) {

        // keep looping while we are still active, and not on heading.
        while (opModeIsActive() && !onHeading(speed, angle, P_TURN_COEFF)) {
            // Update telemetry & Allow time for other processes to run.
            telemetry.update();
        }
    }

    /**
     *  Method to obtain & hold a heading for a finite amount of time
     *  Move will stop once the requested time has elapsed
     *
     * @param speed      Desired speed of turn.
     * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
     *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                   If a relative angle is required, add/subtract from current heading.
     * @param holdTime   Length of time (in seconds) to hold the specified heading.
     */
    public void gyroHold( double speed, double angle, double holdTime) {

        ElapsedTime holdTimer = new ElapsedTime();

        // keep looping while we have time remaining.
        holdTimer.reset();
        while (opModeIsActive() && (holdTimer.time() < holdTime)) {
            // Update telemetry & Allow time for other processes to run.
            onHeading(speed, angle, P_TURN_COEFF);
            telemetry.update();
        }

        // Stop all motion;
        robot.leftFrontMotor.setPower(0);
        robot.rightFrontMotor.setPower(0);
    }

    /**
     * Perform one cycle of closed loop heading control.
     *
     * @param speed     Desired speed of turn.
     * @param angle     Absolute Angle (in Degrees) relative to last gyro reset.
     *                  0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                  If a relative angle is required, add/subtract from current heading.
     * @param PCoeff    Proportional Gain coefficient
     * @return
     */
    boolean onHeading(double speed, double angle, double PCoeff) {
        double   error ;
        double   steer ;
        boolean  onTarget = false ;
        double leftSpeed;
        double rightSpeed;

        // determine turn power based on +/- error
        error = getError(angle);

        if (Math.abs(error) <= HEADING_THRESHOLD) {
            steer = 0.0;
            leftSpeed  = 0.0;
            rightSpeed = 0.0;
            onTarget = true;
        }
        else {
            steer = getSteer(error, PCoeff);
            rightSpeed  = speed * steer;
            leftSpeed   = -rightSpeed;
        }

        // Send desired speeds to motors.
        robot.leftFrontMotor.setPower(leftSpeed);
        robot.rightFrontMotor.setPower(-rightSpeed);

        // Display it for the driver.
        telemetry.addData("Target", "%5.2f", angle);
        telemetry.addData("Err/St", "%5.2f/%5.2f", error, steer);
        telemetry.addData("Speed.", "%5.2f:%5.2f", leftSpeed, rightSpeed);

        return onTarget;
    }

    /**
     * getError determines the error between the target angle and the robot's current heading
     * @param   targetAngle  Desired angle (relative to global reference established at last Gyro Reset).
     * @return  error angle: Degrees in the range +/- 180. Centered on the robot's frame of reference
     *          +ve error means the robot should turn LEFT (CCW) to reduce error.
     */
    public double getError(double targetAngle) {

        double robotError;

        // calculate error in -179 to +180 range  (
        robotError = targetAngle - gyro.getIntegratedZValue();
        while (robotError > 180)  robotError -= 360;
        while (robotError <= -180) robotError += 360;
        return robotError;
    }

    /**
     * returns desired steering force.  +/- 1 range.  +ve = steer left
     * @param error   Error angle in robot relative degrees
     * @param PCoeff  Proportional Gain Coefficient
     * @return
     */
    public double getSteer(double error, double PCoeff) {
        return Range.clip(error * PCoeff, -1, 1);
    }

}




