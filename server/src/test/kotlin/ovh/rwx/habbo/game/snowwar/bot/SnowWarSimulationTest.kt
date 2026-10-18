package ovh.rwx.habbo.game.snowwar.bot

import org.junit.Assert.assertEquals
import org.junit.Test
import ovh.rwx.habbo.game.snowwar.SnowWarUser
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTeam
import ovh.rwx.habbo.game.snowwar.enums.SnowWarTrajectory
import ovh.rwx.habbo.game.snowwar.objects.SnowWarSnowball
import ovh.rwx.habbo.game.snowwar.utils.SnowWarMath

class SnowWarSimulationTest {

    @Test
    fun testSnowball282Physics() {
        val thrower = SnowWarUser(
            session = null,
            team = SnowWarTeam.BLUE,
            botData = SnowWarBotData(-1002, "Bot", "", "M", "")
        )
        thrower.setupSpawn(3, 32 to 14) // objectId = 4
        thrower.currentLocationX = 102400
        thrower.currentLocationY = 44800

        // In Turn 260 subturn 1/3:
        // Snowball created, id=282 ttl:58 deltaX:-352 deltaY:224 deltax^2+deltay^2:174080 sqrt:416 sqrt*200:83200
        // target: 32000, 89600
        val ball = SnowWarSnowball(
            objectId = 282,
            thrower = thrower,
            startWorldX = 102400,
            startWorldY = 44800,
            targetWorldX = 32000,
            targetWorldY = 89600,
            trajectoryRequested = SnowWarTrajectory.LONG_LOB
        )

        println("Initial ball 282: dir=${ball.direction} ttl=${ball.timeToLive} paraOffs=${ball.parabolaOffset} vel=${ball.planarVelocity} traj=${ball.trajectory}")

        // 3 subturns:
        ball.calculateFrameMovement()
        ball.calculateFrameMovement()
        ball.calculateFrameMovement()

        println("After 3 subturns: locH=${ball.locH} locV=${ball.locV} z=${ball.height} ttl=${ball.timeToLive}")
        // From logger.txt Turn 260:
        // Parms:1,282,98740,47077,11250,238,2,55,4,29,1434
        assertEquals(98740, ball.locH)
        assertEquals(47077, ball.locV)
        assertEquals(11250, ball.height)
        assertEquals(55, ball.timeToLive)
    }

    @Test
    fun testSnowball283Physics() {
        val thrower = SnowWarUser(
            session = null,
            team = SnowWarTeam.RED,
            botData = SnowWarBotData(-1003, "Bot", "", "M", "")
        )
        thrower.setupSpawn(4, 10 to 28) // objectId = 5
        thrower.currentLocationX = 32000
        thrower.currentLocationY = 89600

        // In Turn 260 subturn 1/3:
        // Snowball created, id=283 ttl:50 deltaX:338 deltaY:-112 deltax^2+deltay^2:126788 sqrt:354 sqrt*200:70800
        // target: 99200, 67200
        val ball = SnowWarSnowball(
            objectId = 283,
            thrower = thrower,
            startWorldX = 32000,
            startWorldY = 89600,
            targetWorldX = 99600,
            targetWorldY = 67200,
            trajectoryRequested = SnowWarTrajectory.LONG_LOB
        )

        println("Initial ball 283: dir=${ball.direction} ttl=${ball.timeToLive} paraOffs=${ball.parabolaOffset} vel=${ball.planarVelocity} traj=${ball.trajectory}")

        // 3 subturns:
        ball.calculateFrameMovement()
        ball.calculateFrameMovement()
        ball.calculateFrameMovement()

        println("After 3 subturns: locH=${ball.locH} locV=${ball.locV} z=${ball.height} ttl=${ball.timeToLive}")
        // From logger.txt Turn 260:
        // Parms:1,283,36047,88286,10050,72,2,47,5,25,1416
        assertEquals(36047, ball.locH)
        assertEquals(88286, ball.locV)
        assertEquals(10050, ball.height)
        assertEquals(47, ball.timeToLive)
    }

    @Test
    fun testQuickRandomSeed366() {
        val seed = ovh.rwx.habbo.game.snowwar.utils.QuickRandom.iterateSeed(366)
        println("Seed 366 = $seed")
        assertEquals(93711992, seed)
    }

    @Test
    fun testSnowball336Movement() {
        val velX = SnowWarMath.getBaseVelX(158)
        val velY = SnowWarMath.getBaseVelY(158)
        val deltaH = (velX * 1425) / SnowWarMath.VELOCITY_DIVISOR
        val deltaV = (velY * 1425) / SnowWarMath.VELOCITY_DIVISOR
        println("Snowball 336: velX=$velX, velY=$velY, deltaH=$deltaH, deltaV=$deltaV")
        assertEquals(530, deltaH)
        assertEquals(1324, deltaV)
    }

    @Test
    fun testSnowball338Physics() {
        val thrower = SnowWarUser(
            session = null,
            team = SnowWarTeam.RED,
            botData = SnowWarBotData(-1009, "Bot", "", "M", "")
        )
        thrower.setupSpawn(0, 26 to 33) // objectId = 1
        thrower.currentLocationX = 83200
        thrower.currentLocationY = 105600

        // In Turn 366 subturn 2/3:
        // Snowball created, id=338 ttl:10 deltaX:79 deltaY:0 deltaX/200:0 deltaY/200:0 deltax^2+deltay^2:6241 sqrt(deltax^2+deltay^2):78 sqrt(deltax^2+deltay^2)*200:15600
        // Parms:1,338,87214,105600,3000,90,0,8,1,5,2000
        val ball = SnowWarSnowball(
            objectId = 338,
            thrower = thrower,
            startWorldX = 83200,
            startWorldY = 105600,
            targetWorldX = 83200 + 79 * 200, // 99000
            targetWorldY = 105600,
            trajectoryRequested = SnowWarTrajectory.QUICK
        )

        println("Initial ball 338: dir=${ball.direction} ttl=${ball.timeToLive} paraOffs=${ball.parabolaOffset} vel=${ball.planarVelocity} traj=${ball.trajectory}")

        // 2 subturns (subturn 2 and subturn 3):
        ball.calculateFrameMovement()
        ball.calculateFrameMovement()

        println("After 2 subturns: locH=${ball.locH} locV=${ball.locV} z=${ball.height} ttl=${ball.timeToLive}")
        assertEquals(87214, ball.locH)
        assertEquals(105600, ball.locV)
        assertEquals(3000, ball.height)
        assertEquals(90, ball.direction)
        assertEquals(0, ball.trajectory)
        assertEquals(8, ball.timeToLive)
        assertEquals(1, ball.thrower.objectId)
        assertEquals(5, ball.parabolaOffset)
        assertEquals(2000, ball.planarVelocity)
    }
}
