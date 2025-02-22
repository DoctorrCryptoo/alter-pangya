package work.fking.pangya.game

import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.apache.logging.log4j.LogManager
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import work.fking.pangya.discovery.DiscoveryClient
import work.fking.pangya.discovery.HeartbeatPublisher
import work.fking.pangya.discovery.ServerType.GAME
import work.fking.pangya.game.persistence.JooqAchievementsRepository
import work.fking.pangya.game.persistence.JooqCaddieRepository
import work.fking.pangya.game.persistence.JooqCardInventory
import work.fking.pangya.game.persistence.JooqCharacterRepository
import work.fking.pangya.game.persistence.JooqEquipmentRepository
import work.fking.pangya.game.persistence.JooqInventoryRepository
import work.fking.pangya.game.persistence.JooqPlayerRepository
import work.fking.pangya.game.persistence.JooqStatisticsRepository
import work.fking.pangya.game.persistence.PersistenceContext
import work.fking.pangya.game.session.SessionClient
import java.nio.file.Files
import java.nio.file.Path


object Bootstrap {
    private val LOGGER = LogManager.getLogger(Bootstrap::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        LOGGER.info("Bootstrapping the game server server...")
        val objectMapper = TomlMapper().registerKotlinModule()
        val configPath = Path.of("config.toml")

        if (!Files.exists(configPath)) {
            LOGGER.error("Missing 'config.toml' file, double check if it exists.")
            return
        }
        val serverConfig = objectMapper.readValue<GameServerConfig>(Files.newInputStream(configPath))

        val redisClient = RedisClient.create(RedisURI.create(serverConfig.redis.url))
        val discoveryClient = DiscoveryClient(redisClient)

        val jooq = setupJooq(serverConfig.database)
        val persistenceContext = setupPersistenceContext(jooq)

        val server = GameServer(
            redisClient = redisClient,
            serverConfig = serverConfig,
            persistenceCtx = persistenceContext,
        )
        HeartbeatPublisher(discoveryClient, GAME, serverConfig) { server.players.count() }.start()
        server.start()

    }

    private fun setupPersistenceContext(jooq: DSLContext): PersistenceContext {
        return PersistenceContext(
            jooq = jooq,
            playerRepository = JooqPlayerRepository(),
            characterRepository = JooqCharacterRepository(),
            caddieRepository = JooqCaddieRepository(),
            inventoryRepository = JooqInventoryRepository(),
            cardRepository = JooqCardInventory(),
            equipmentRepository = JooqEquipmentRepository(),
            statisticsRepository = JooqStatisticsRepository(),
            achievementsRepository = JooqAchievementsRepository()
        )
    }

    private fun setupJooq(databaseConfig: DatabaseConfig): DSLContext {
        val hikariConfig = HikariConfig()

        with(hikariConfig) {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = databaseConfig.url
            username = databaseConfig.username
            password = databaseConfig.password
        }
        val dataSource = HikariDataSource(hikariConfig)

        val jooqConfig = DefaultConfiguration()
            .set(DataSourceConnectionProvider(dataSource))
            .set(SQLDialect.POSTGRES)

        return DSL.using(jooqConfig)
    }
}
