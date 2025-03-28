import dev.zwazel.GameWorld;
import dev.zwazel.PropertyHandler;
import dev.zwazel.bot.BotInterface;
import dev.zwazel.internal.PublicGameWorld;
import dev.zwazel.internal.config.LobbyConfig;
import dev.zwazel.internal.config.LocalBotConfig;
import dev.zwazel.internal.connection.client.ConnectedClientConfig;
import dev.zwazel.internal.debug.MapVisualiser;
import dev.zwazel.internal.game.lobby.TeamConfig;
import dev.zwazel.internal.game.state.ClientState;
import dev.zwazel.internal.game.state.FlagBaseState;
import dev.zwazel.internal.game.state.FlagGameState;
import dev.zwazel.internal.game.state.flag.Carried;
import dev.zwazel.internal.game.state.flag.Dropped;
import dev.zwazel.internal.game.state.flag.FlagState;
import dev.zwazel.internal.game.tank.TankConfig;
import dev.zwazel.internal.game.tank.implemented.LightTank;
import dev.zwazel.internal.game.tank.implemented.SelfPropelledArtillery;
import dev.zwazel.internal.game.transform.Vec3;
import dev.zwazel.internal.message.data.GameConfig;
import dev.zwazel.internal.message.data.GameState;
import dev.zwazel.internal.game.utils.*;



import java.util.*;

public class Panzerspähwagen_SdKfz_234 implements BotInterface {
    private final PropertyHandler propertyHandler = PropertyHandler.getInstance();
    private final float minAttackDistance;
    private final float maxAttackDistance;

    private List<ConnectedClientConfig> teamMembers;
    private List<ConnectedClientConfig> enemyTeamMembers;

    private MapVisualiser visualiser;

    public Panzerspähwagen_SdKfz_234() {
        this.minAttackDistance = Float.parseFloat(propertyHandler.getProperty("bot.attack.minDistance"));
        this.maxAttackDistance = Float.parseFloat(propertyHandler.getProperty("bot.attack.maxDistance"));
    }


    public void start() {
        GameWorld.startGame(this);
        //GameWorld.connectToServer(this);
    }

    @Override
    public LocalBotConfig getLocalBotConfig() {
        return LocalBotConfig.builder()
                .debugMode(Optional.ofNullable(propertyHandler.getProperty("debug.mode"))
                        .map(GameWorld.DebugMode::valueOf))
                .botName(propertyHandler.getProperty("bot.name"))
                .tankType(LightTank.class)
                .serverIp(propertyHandler.getProperty("server.ip"))
                .serverPort(Integer.parseInt(propertyHandler.getProperty("server.port")))
                .lobbyConfig(LobbyConfig.builder()
                        .lobbyName(propertyHandler.getProperty("lobby.name"))
                        .teamName(propertyHandler.getProperty("lobby.name"))
                        .teamName(propertyHandler.getProperty("lobby.team.name"))
                        .mapName(propertyHandler.getProperty("lobby.map.name"))
                        .spawnPoint(Optional.ofNullable(propertyHandler.getProperty("lobby.spawnPoint"))
                                .map(Integer::parseInt))
                        .fillEmptySlots(Boolean.parseBoolean(propertyHandler.getProperty("lobby.fillEmptySlots")))
                        .build()
                )
                .build();
    }

    @Override
    public void setup(PublicGameWorld world) {
        GameConfig config = world.getGameConfig();

        TeamConfig myTeamConfig = config.getMyTeamConfig();
        TeamConfig enemyTeamConfig = config.teamConfigs().values().stream()
                .filter(teamConfig -> !teamConfig.teamName().equals(myTeamConfig.teamName()))
                .findFirst()
                .orElseThrow();

        // Get all team members, excluding myself
        teamMembers = config.getTeamMembers(myTeamConfig.teamName(), config.clientId());
        // Get all enemy team members
        enemyTeamMembers = config.getTeamMembers(enemyTeamConfig.teamName());

        // If in debug, add visualiser
        if (world.isDebug()) {
            // Add visualiser. By pressing space, you can switch between drawing modes.
            visualiser = new MapVisualiser(world);
            visualiser.setDrawingMode(MapVisualiser.DrawingMode.valueOf(propertyHandler.getProperty("debug.visualiser.mode").toUpperCase()));
            visualiser.showMap();
            world.registerVisualiser(visualiser);
        }


    }


    LinkedList<Node> path = new LinkedList<>();

    @Override
    public void processTick(PublicGameWorld world) {

        boolean allowDiagonal = false;
        GameConfig config1 = world.getGameConfig();
        float[][] heightMap = config1.mapDefinition().tiles();

        Graph graph = new Graph(config1.mapDefinition(), allowDiagonal);


        ClientState myClientState = world.getMyState();

        Vec3 myClosestTile = world.getGameConfig().mapDefinition().getClosestTileFromWorld(myClientState.transformBody().getTranslation());

        Node root = graph.getNode(myClosestTile.getX(), myClosestTile.getZ());


        GameState gameState = world.getGameState();

        HashMap<Long, FlagGameState> flagStates = gameState.flagStates();

        Node flag = null;
        FlagGameState enemyFlagState = null;
        if (!flagStates.isEmpty()) {
            enemyFlagState = flagStates
                    .values()
                    .stream()
                    .filter(state ->
                            !state.team().equals(config1.getMyConfig().clientTeam()))
                    .findFirst()
                    .orElse(null);


            Vec3 enemyFlagClosestTile = config1.mapDefinition().getClosestTileFromWorld(enemyFlagState.transform().getTranslation());

            flag = graph.getNode((int) enemyFlagClosestTile.getX(), (int) enemyFlagClosestTile.getZ());

        }


        if (myClientState.state() == ClientState.PlayerState.DEAD) {
            System.out.println("I'm dead!");
            return;
        }

        TankConfig myTankConfig = world.getTank().getConfig(world);
        if (path.isEmpty()) {
            if (enemyFlagState.state() instanceof Carried) {
                Carried carriedState = (Carried) enemyFlagState.state();
                if (carriedState.entityId() == config1.getMyConfig().clientId()) {
                    HashMap<Long, FlagBaseState> flagBaseStates = gameState.flagBaseStates();
                    FlagBaseState flagBaseState = null;
                    if (!flagBaseStates.isEmpty()) {
                        flagBaseState = flagBaseStates
                                .values()
                                .stream()
                                .filter(state ->
                                        state.team().equals(config1.getMyConfig().clientTeam()))
                                .findFirst()
                                .orElse(null);

                        Vec3 teamflagbase = config1.mapDefinition().getClosestTileFromWorld(flagBaseState.transform().getTranslation());

                        Node base = graph.getNode((int) teamflagbase.getX(), (int) teamflagbase.getZ());


                        path = new FindPath(root, base, graph, myTankConfig).findPath();
                    }

                }
            } else {

                path = new FindPath(root, flag, graph, myTankConfig).findPath();
            }
        }

        if (!path.isEmpty()) {
            Node nextTargetPos = path.peekFirst();

            Vec3 worldPosOfTile = world.getGameConfig()
                    .mapDefinition()
                    .getWorldTileCenter(
                            nextTargetPos.getX(),
                            nextTargetPos.getY()
                    );

            double distanceToNext = myClientState.transformBody().getTranslation().distance(worldPosOfTile);
            double closeEnough = 0.3;
            if (distanceToNext < closeEnough) {
                path.pollFirst();
                nextTargetPos = path.peekFirst();

                if (nextTargetPos == null) {
                    System.out.println("Finished Path");
                    return;
                }

            }




            world.getTank().moveTowards(world, worldPosOfTile, false);

            if (visualiser != null) {
                visualiser.setPath(path);
                visualiser.setGraph(graph);
            }


        }
    }
}


/*

        dev.zwazel.internal.game.tank.implemented.LightTank tank = (dev.zwazel.internal.game.tank.implemented.LightTank) world.getTank();
        TankConfig myTankConfig = tank.getConfig(world);
        GameConfig config = world.getGameConfig();

        Optional<ClientState> closestEnemy = enemyTeamMembers.stream()
                .map(connectedClientConfig -> world.getClientState(connectedClientConfig.clientId()))
                .filter(clientState -> clientState != null && clientState.transformBody().getTranslation() != null &&
                        clientState.state() != ClientState.PlayerState.DEAD)
                .min((o1, o2) -> {
                    double distance1 = myClientState.transformBody().getTranslation().distance(o1.transformBody().getTranslation());
                    double distance2 = myClientState.transformBody().getTranslation().distance(o2.transformBody().getTranslation());
                    return Double.compare(distance1, distance2);
                });

        closestEnemy.ifPresentOrElse(
                enemy -> {
                    double distanceToEnemy = myClientState.transformBody().getTranslation().distance(enemy.transformBody().getTranslation());

                    if (distanceToEnemy < this.minAttackDistance) {
                        tank.moveTowards(world, Tank.MoveDirection.BACKWARD, enemy.transformBody().getTranslation(), true);
                    } else if (distanceToEnemy > this.maxAttackDistance) {
                        tank.moveTowards(world, Tank.MoveDirection.FORWARD, enemy.transformBody().getTranslation(), true);
                    }
                    tank.rotateTurretTowards(world, enemy.transformBody().getTranslation());

                    if (distanceToEnemy <= this.maxAttackDistance) {
                        if (tank.canShoot(world)) {
                            if (tank.shoot(world) && world.isDebug()) {
                                System.out.println("Shot at enemy!");
                            }
                        }
                    }
                },
                () -> {
                    tank.rotateBody(world, -myTankConfig.bodyRotationSpeed());
                    tank.move(world, Tank.MoveDirection.FORWARD);
                }
        );

        tank.rotateBody(world, -myTankConfig.bodyRotationSpeed());
        tank.rotateTurretYaw(world, myTankConfig.turretYawRotationSpeed());
        tank.move(world, Tank.MoveDirection.FORWARD);

        List<MessageContainer> hitMessages = world.getIncomingMessages(Hit.class);
        for (MessageContainer message : hitMessages) {
            Hit gotHitMessageData = (Hit) message.getMessage();
            handleHittingTank(world, gotHitMessageData);
        }

        List<MessageContainer> messages = world.getIncomingMessages();
        for (MessageContainer message : messages) {
            MessageData data = message.getMessage();

            switch (data) {
                case SimpleTextMessage textMessage ->
                        System.out.println("Received text message:\n\t" + textMessage.message());
                case GotHit gotHitMessageData -> handleGettingHit(world, gotHitMessageData);
                case Hit _ -> {
                }
                default -> System.err.println("Received unknown message type: " + data.getClass().getSimpleName());
            }
        }

        teamMembers
                .forEach(target -> world.send(new MessageContainer(
                        CLIENT.get(target.clientId()),
                        new SimpleTextMessage(
                                "Hello " + target.clientName() + " from " + config.getMyConfig().clientName() + "!"
                        )
                )));

        enemyTeamMembers
                .forEach(target -> world.send(new MessageContainer(
                        CLIENT.get(target.clientId()),
                        new SimpleTextMessage(
                                "You're going down, " + target.clientName() + "!"
                        )
                )));
    }

    private void handleHittingTank(PublicGameWorld world, Hit hitMessageData) {
        ConnectedClientConfig targetConfig = world.getConnectedClientConfig(hitMessageData.hitEntity()).orElseThrow();
        TankConfig targetTankConfig = targetConfig.getTankConfig(world);
        TankConfig myTankConfig = world.getTank().getConfig(world);
        float armorOnHitSide = targetTankConfig.armor().get(hitMessageData.hitSide());
        float myExpectedDamage = myTankConfig.projectileDamage();
        float dealtDamage = hitMessageData.damageDealt();
        ClientState targetState = targetConfig.getClientState(world);
        System.out.println("Hit " + targetConfig.clientName() + " on " + hitMessageData.hitSide() + " side!");
        System.out.println("Dealt damage: " + dealtDamage + " = " + myExpectedDamage + " * (1 - " + armorOnHitSide + ")");
        System.out.println(targetConfig.clientName() + " health: " + targetState.currentHealth());
    }

    private void handleGettingHit(PublicGameWorld world, GotHit gotHitMessageData) {
        ConnectedClientConfig shooterConfig = world.getConnectedClientConfig(gotHitMessageData.shooterEntity()).orElseThrow();
        System.out.println("Got hit by " + shooterConfig.clientName() + " on " + gotHitMessageData.hitSide());
        System.out.println("Received " + gotHitMessageData.damageReceived() + " damage!");
        System.out.println("Current health: " + world.getMyState().currentHealth());

        if (world.getMyState().state() == ClientState.PlayerState.DEAD) {
            System.out.println("I died! killed by " + shooterConfig.clientName());
        }
    }

 */