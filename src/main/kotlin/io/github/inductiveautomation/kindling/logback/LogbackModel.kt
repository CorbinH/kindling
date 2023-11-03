package io.github.inductiveautomation.kindling.logback

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

/*
The very basic structure of the configuration file can be described as, <configuration> element,
containing zero or more <appender> elements, followed by zero or more <logger> elements,
followed by at most one <root> element.
 */
@JacksonXmlRootElement(localName = "configuration")
@JsonPropertyOrder("rootDir") // ensure that "rootDir" is declared before other elements reference its value
data class LogbackConfigData(

    @field:JacksonXmlProperty(isAttribute = true, localName = "debug")
    var debug: Boolean = true,

    @field:JacksonXmlProperty(isAttribute = true, localName = "scan")
    var scan: Boolean? = null,

    @field:JacksonXmlProperty(isAttribute = true, localName = "scanPeriod")
    var scanPeriod: String? = null,

    @field:JacksonXmlProperty(localName = "property")
    var rootDir: RootDirectory? = null,

    @field:JacksonXmlProperty(localName = "root")
    var root: Root = Root("INFO"),

    @JacksonXmlProperty(localName = "appender")
    @JacksonXmlElementWrapper(useWrapping = false)
    var appender: List<Appender>? = listOf(),

    @JacksonXmlProperty(localName = "logger")
    @JacksonXmlElementWrapper(useWrapping = false)
    var logger: List<Logger>? = listOf(),

)

/*
The root directory is a <property> element which stores the root log output folder as its value.
 */
@JacksonXmlRootElement
data class RootDirectory(

    @field:JacksonXmlProperty(isAttribute = true, localName = "name")
    var name: String = "ROOT",

    @field:JacksonXmlProperty(isAttribute = true, localName = "value")
    var value: String = System.getProperty("user.home"),
)

/*
The <root> element configures the root logger. It supports a single attribute, namely the level attribute.
It does not allow any other attributes because the additivity flag does not apply to the root logger.
Moreover, since the root logger is already named as "ROOT", it does not allow a name attribute either.
The value of the level attribute can be one of the case-insensitive strings TRACE, DEBUG, INFO, WARN, ERROR, ALL or OFF.
Note that the level of the root logger cannot be set to INHERITED or NULL.

Similarly to the <logger> element, the <root> element may contain zero or more <appender-ref> elements;
each appender thus referenced is added to the root logger.
 */
@JacksonXmlRootElement(localName = "root")
data class Root(

    @field:JacksonXmlProperty(isAttribute = true, localName = "level")
    var level: String? = null,

    @field:JacksonXmlProperty(localName = "appender-ref")
    @JacksonXmlElementWrapper(localName = "appender-ref", useWrapping = false)
    var appenderRef: MutableList<AppenderRef>? = mutableListOf(
        AppenderRef("SysoutAsync"),
        AppenderRef("DBAsync"),
    ),

)

/*
An appender is configured with the <appender> element, which takes two mandatory attributes name and class.
The name attribute specifies the name of the appender whereas the class attribute specifies the fully qualified name of
the appender class to instantiate. The <appender> element may contain zero or one <layout> elements, zero or more
<encoder> elements and zero or more <filter> elements.
Apart from these three common elements, <appender> elements may contain any number of elements corresponding to
JavaBean properties of the appender class.
 */
@JacksonXmlRootElement(localName = "appender")
data class Appender(

    @field:JacksonXmlProperty(isAttribute = true, localName = "name")
    var name: String,

    @field:JacksonXmlProperty(isAttribute = true, localName = "class")
    var className: String,

    @field:JacksonXmlProperty(isAttribute = true, localName = "queueSize")
    var queueSize: String? = null,

    @field:JacksonXmlProperty(isAttribute = true, localName = "discardingThreshold")
    var discardingThreshold: String? = null,

    @field:JacksonXmlProperty(localName = "rollingPolicy")
    var rollingPolicy: RollingPolicy? = null,

    @JacksonXmlProperty(localName = "encoder")
    @JacksonXmlElementWrapper(useWrapping = false)
    var encoder: MutableList<Encoder>? = mutableListOf(),

    @JacksonXmlProperty(localName = "filter")
    @JacksonXmlElementWrapper(useWrapping = false)
    var levelFilter: LevelFilter? = null,

    @field:JacksonXmlProperty(localName = "dir")
    var dir: String? = null,

    @field:JacksonXmlProperty(localName = "appender-ref")
    @JacksonXmlElementWrapper(useWrapping = false)
    var appenderRef: MutableList<AppenderRef>? = mutableListOf(),

)

@JacksonXmlRootElement(localName = "appender-ref")
data class AppenderRef(

    @field:JacksonXmlProperty(isAttribute = true, localName = "ref")
    var ref: String,

)

data class Encoder(

    @field:JacksonXmlProperty(localName = "pattern")
    var pattern: String = "%.-1p [%-30c{1}] [%d{MM:dd:YYYY HH:mm:ss, America/Los_Angeles}]: %m %X%n",

)

/*
A <logger> element takes exactly one mandatory name attribute, an optional level attribute, and an optional additivity
attribute, admitting the values true or false. The value of the level attribute admitting one of the case-insensitive
string values TRACE, DEBUG, INFO, WARN, ERROR, ALL or OFF. The special case-insensitive value INHERITED, or its synonym
NULL, will force the level of the logger to be inherited from higher up in the hierarchy. This comes in handy if you
set the level of a logger and later decide that it should inherit its level.
The <logger> element may contain zero or more <appender-ref> elements.
*/
@JacksonXmlRootElement(localName = "logger")
data class Logger(

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    var name: String,

    @JacksonXmlProperty(isAttribute = true, localName = "level")
    var level: String? = null,

    @JacksonXmlProperty(isAttribute = true, localName = "additivity")
    var additivity: Boolean? = null,

    @field:JacksonXmlProperty(localName = "appender-ref")
    @JacksonXmlElementWrapper(useWrapping = false)
    var appenderRef: MutableList<AppenderRef>? = mutableListOf(),

)

/*
The <filter> element filters events based on exact level matching.
If the event's level is equal to the configured level, the filter accepts or denies the event, depending on the
configuration of the onMatch and onMismatch properties.
 */
@JacksonXmlRootElement(localName = "filter")
data class LevelFilter(

    @field:JacksonXmlProperty(isAttribute = true, localName = "class")
    var className: String = "ch.qos.logback.classic.filter.LevelFilter",

    @field:JacksonXmlProperty(localName = "level")
    var level: String,

    @field:JacksonXmlProperty(localName = "onMatch")
    var onMatch: String? = "ACCEPT",

    @field:JacksonXmlProperty(localName = "onMismatch")
    var onMismatch: String? = "DENY",

)

@JacksonXmlRootElement(localName = "rollingPolicy")
data class RollingPolicy(

    @field:JacksonXmlProperty(isAttribute = true, localName = "class")
    var className: String = "ch.qos.logback.core.rolling.RollingFileAppender",

    @field:JacksonXmlProperty(localName = "fileNamePattern")
    var fileNamePattern: String = "\${ROOT}\\\\AdditionalLogs.%d{yyyy-MM-dd}.%i.log",

    @field:JacksonXmlProperty(localName = "maxFileSize")
    var maxFileSize: String = "10MB",

    @field:JacksonXmlProperty(localName = "totalSizeCap")
    var totalSizeCap: String = "1GB",

    @field:JacksonXmlProperty(localName = "maxHistory")
    var maxHistory: String = "5",

)

class LogbackConfigManager(
    var configs: LogbackConfigData?,
    var configString: String? = null,
) {

    // Build XmlMapper with the parameters for serialization
    private val xmlMapperBuilder = XmlMapper.builder()
        .defaultUseWrapper(false)
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .build()

    private val xmlMapper: XmlMapper = xmlMapperBuilder.apply {
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    // Convert LogbackConfigData data class to XML string (for UI and clipboard)
    fun generateXmlString(): String {
        return XML_HEADER + xmlMapper.writeValueAsString(configs)
    }

    // Convert LogbackConfigData data class to XML file (serialization)
    fun writeXmlFile(filePathString: String) {
        xmlMapper.writeValue(File(filePathString), configs)
    }

    /*
    Each selected logger will either output to a separate appender or use the default Sysout appender.
    In either case, we need a <logger> element.
    For those using a separate appender, we need to generate that <appender> element.
    */
    fun updateLoggerConfigs(selectedLoggers: MutableList<SelectedLogger>) {
        if (selectedLoggers.isNotEmpty()) {
            val separateOutputLoggers = selectedLoggers.filter { selectedLogger: SelectedLogger ->
                selectedLogger.separateOutput
            }

            println("Loggers with separate output:")
            println(separateOutputLoggers)

            val loggerElements = selectedLoggers.map {
                Logger(
                    name = it.name,
                    level = it.level,
                    additivity = false,
                    appenderRef = if (it.separateOutput) {
                        mutableListOf(AppenderRef(it.name))
                    } else {
                        mutableListOf(AppenderRef("SysoutAsync"))
                    },
                )
            }

            val appenderElements = separateOutputLoggers.map {
                Appender(
                    name = it.name,
                    className = "ch.qos.logback.core.rolling.RollingFileAppender",
                    rollingPolicy = RollingPolicy(
                        className = "ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy",
                        fileNamePattern = it.outputFolder + it.filenamePattern,
                        maxFileSize = it.maxFileSize.toString() + "MB",
                        totalSizeCap = it.totalSizeCap.toString() + "MB",
                        maxHistory = it.maxDaysHistory.toString(),
                    ),
                    encoder = mutableListOf(
                        Encoder(
                            pattern = "%.-1p [%-30c{1}] [%d{MM:dd:YYYY HH:mm:ss, America/Los_Angeles}]: %m %X%n",
                        ),
                    ),
                )
            }

            configs?.logger = loggerElements
            configs?.appender = appenderElements.plus(DEFAULT_APPENDERS)

            println(configs)
        }
    }
    companion object {
        const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        val DEFAULT_APPENDERS = listOf(
            Appender(
                name = "SysoutAppender",
                className = "ch.qos.logback.core.ConsoleAppender",
                encoder = mutableListOf(
                    Encoder(
                        pattern = "%.-1p [%-30c{1}] [%d{HH:mm:ss,SSS}]: %m %X%n",
                    ),
                ),
            ),
            Appender(
                name = "DB",
                className = "com.inductiveautomation.logging.SQLiteAppender",
                dir = "logs",
            ),
            Appender(
                name = "SysoutAsync",
                className = "ch.qos.logback.classic.AsyncAppender",
                queueSize = "1000",
                discardingThreshold = "0",
                appenderRef = mutableListOf(AppenderRef(ref = "SysoutAppender")),
            ),
            Appender(
                name = "DBAsync",
                className = "ch.qos.logback.classic.AsyncAppender",
                queueSize = "100000",
                discardingThreshold = "0",
                appenderRef = mutableListOf(AppenderRef(ref = "DB")),
            ),
        )
    }

    init {
        configString = generateXmlString()
    }
}

class LogbackConfigDeserializer {

    private val xmlModule = JacksonXmlModule().apply {
        setDefaultUseWrapper(false)
    }

    private val kotlinXmlMapper = XmlMapper(xmlModule).registerKotlinModule()

    // Read in XML file as LogbackConfigData data class (deserialization)
    fun getObjectFromXML(filePath: String): LogbackConfigData? {
        kotlinXmlMapper.apply {
            jacksonMapperBuilder().apply {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                enable(MapperFeature.USE_ANNOTATIONS)
            }
        }

        return try {
            val file = File(filePath)
            if (file.exists()) {
                val xmlContent = file.readText()
                println("XML Content: $xmlContent")
                kotlinXmlMapper.readValue(file, LogbackConfigData::class.java)
            } else {
                println("File not found: $filePath")
                null
            }
        } catch (e: Exception) {
            println("An error occurred: ${e.message}")
            null
        }
    }
}
