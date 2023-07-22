package work.fking.pangya.game

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import org.slf4j.LoggerFactory
import work.fking.pangya.game.net.ServerChannelInitializer
import work.fking.pangya.game.persistence.PersistenceContext
import work.fking.pangya.game.player.CaddieRoster
import work.fking.pangya.game.player.CharacterRoster
import work.fking.pangya.game.player.Equipment
import work.fking.pangya.game.player.Inventory
import work.fking.pangya.game.player.Player
import work.fking.pangya.game.player.PlayerAchievements
import work.fking.pangya.game.player.PlayerGroup
import work.fking.pangya.game.player.PlayerStatistics
import work.fking.pangya.game.player.PlayerWallet
import work.fking.pangya.networking.selectBestEventLoopAvailable
import work.fking.pangya.networking.selectBestServerChannelAvailable
import java.net.InetAddress
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

private val LOGGER = LoggerFactory.getLogger(GameServer::class.java)

class GameServer(
    private val serverConfig: GameServerConfig,
    val persistenceContext: PersistenceContext,
    val sessionClient: SessionClient,
    val serverChannels: List<ServerChannel> = serverConfig.serverChannels
) {
    private val executorService = Executors.newVirtualThreadPerTaskExecutor()
    private val connectionIdSequence = AtomicInteger()
    val players = PlayerGroup()

    fun serverChannelById(id: Int): ServerChannel? = serverChannels.firstOrNull { it.id == id }

    fun <R> submitTask(callable: Callable<R>): Future<R> = executorService.submit(callable)

    fun runTask(runnable: Runnable): Future<*> = executorService.submit(runnable)

    fun registerPlayer(
        channel: Channel,
        sessionInfo: SessionClient.SessionInfo,
        playerWallet: PlayerWallet,
        characterRoster: CharacterRoster,
        caddieRoster: CaddieRoster,
        inventory: Inventory,
        equipment: Equipment,
        statistics: PlayerStatistics,
        achievements: PlayerAchievements
    ): Player {
        val player = Player(
            channel = channel,
            uid = sessionInfo.uid,
            connectionId = connectionIdSequence.getAndIncrement(),
            username = sessionInfo.username,
            nickname = sessionInfo.nickname,
            wallet = playerWallet,
            characterRoster = characterRoster,
            caddieRoster = caddieRoster,
            inventory = inventory,
            equipment = equipment,
            statistics = statistics,
            achievements = achievements,
        )

        players.add(player)
        channel.closeFuture().addListener { onPlayerDisconnect(player) }
        return player
    }

    private fun onPlayerDisconnect(player: Player) {
        player.currentRoom?.removePlayer(player)
        player.currentChannel?.removePlayer(player)
        players.remove(player)
        LOGGER.debug("{} disconnected", player.nickname)
    }

    fun start() {
        val bootstrap = ServerBootstrap()
        val bossGroup = selectBestEventLoopAvailable()
        val workerGroup = selectBestEventLoopAvailable(2)

        bootstrap.group(bossGroup, workerGroup)
            .channel(selectBestServerChannelAvailable())
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(ServerChannelInitializer(this))

        LOGGER.info("Binding to port {}...", serverConfig.port)
        try {
            val inetAddress = InetAddress.getByName(serverConfig.bindAddress)
            val channel = bootstrap.bind(inetAddress, serverConfig.port)
                .sync()
                .channel()
            LOGGER.info("Successfully bound to {}, server bootstrap completed!", channel.localAddress())
            channel.closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}
