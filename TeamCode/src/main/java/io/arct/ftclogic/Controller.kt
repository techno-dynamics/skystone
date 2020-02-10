package io.arct.ftclogic

import io.arct.ftclib.drive.MecanumDrive
import io.arct.ftclib.eventloop.OperationMode
import io.arct.ftclib.hardware.gamepad.Gamepad
import io.arct.ftclib.hardware.sensors.FtcImu
import io.arct.robotlib.hardware.motors.ContinuousServo
import io.arct.robotlib.hardware.motors.Motor
import io.arct.robotlib.hardware.motors.Servo
import io.arct.robotlib.hardware.sensors.TouchSensor
import io.arct.robotlib.robot.device
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import kotlin.concurrent.thread

@OperationMode.Bind(OperationMode.Type.Operated, name = "Standard Control", group = "Main")
class Controller : OperationMode() {
    private val drive: MecanumDrive = MecanumDrive(robot,
        robot device "motor0",
        robot device "motor3",
        robot device "motor1",
        robot device "motor2",
        rotation = ::orientation
    )

    private val imu: FtcImu = robot device "imu"

    private val intakeLeft: Motor = robot device "motor4"
    private val intakeRight: Motor = robot device "motor5"
    private val linear: ContinuousServo = robot device "servo6"

    private val buildplateLeft: Servo = robot device "servo2"
    private val buildplateRight: Servo = robot device "servo7"
    private val clamp: Servo = robot device "servo0"
    private val pivot: ContinuousServo = robot device "servo1"
    private val capstone: Servo = robot device "servo3"
    private val limit: TouchSensor = robot device "digital0"

    private var buildplate: Boolean = false
    private var orientation: Double = ImuOffset
    private var clampBrick: Boolean = false
    private var pivotBrick: Boolean = false
    private var pressed: Boolean = false
    private var capstoneMode: Boolean = true

    init {
        thread {
            imu.init()

            log.add("IMU Ready").update()

            while (true) {
                orientation = AngleUnit.DEGREES.fromUnit(imu.orientation.angleUnit, imu.orientation.firstAngle) + ImuOffset
            }
        }

        capstone.position = 1.0

        log.add("Ready").update()
    }

    override fun loop() {
        drive.gamepad(robot.gamepad[0])

        input(robot.gamepad[1])
        intake(robot.gamepad[1].rt >= 0.5, robot.gamepad[1].lt >= 0.5)
        linear(robot.gamepad[1].right.y)
        buildplate()
        clamp()
        pivot()
        capstone()
    }

    private fun intake(intake: Boolean, outtake: Boolean) {
        val power = if (intake) 0.75 else if (outtake) -0.75 else 0.0

        intakeLeft.power = power
        intakeRight.power = -power
    }

    private fun linear(power: Double) = if (limit.pressed && power < 0)
        linear.power = 0.0
    else
        linear.power = power

    private fun clamp() {
        clamp.position = if (clampBrick) 1.0 else 0.0
    }

    private fun pivot() {
        pivot.power = if (pivotBrick) -0.07 else 1.0
    }

    private fun capstone() {
        capstone.position = if (capstoneMode) 1.0 else 0.0
    }

    private fun buildplate() {
        buildplateLeft.position = if (buildplate) 1.0 else 0.0
        buildplateRight.position = if (buildplate) 0.0 else 1.0
    }

    private fun input(gamepad: Gamepad) {
        if (pressed && (gamepad.lb || gamepad.rb || gamepad.a || gamepad.b))
            return

        if (gamepad.lb)
            clampBrick = !clampBrick

        if (gamepad.rb)
            pivotBrick = !pivotBrick

        if (gamepad.a)
            buildplate = !buildplate

        if (gamepad.b)
            capstoneMode = !capstoneMode

        pressed = gamepad.lb || gamepad.rb || gamepad.a || gamepad.b
    }

    companion object {
        private const val ImuOffset = 180.0
    }
}