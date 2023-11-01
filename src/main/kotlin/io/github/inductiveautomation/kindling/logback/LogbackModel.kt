package io.github.inductiveautomation.kindling.logback

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
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
    var appender: MutableList<Appender>? = mutableListOf(),

    @JacksonXmlProperty(localName = "logger")
    @JacksonXmlElementWrapper(useWrapping = false)
    var logger: MutableList<Logger>? = mutableListOf(),

)

@JacksonXmlRootElement
data class RootDirectory(

    @field:JacksonXmlProperty(isAttribute = true, localName = "name")
    val name: String = "ROOT",

    @field:JacksonXmlProperty(isAttribute = true, localName = "value")
    val value: String = System.getProperty("user.home"),
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

    @field:JacksonXmlProperty(isAttribute = true, localName = "name")
    val name: String,

    @field:JacksonXmlProperty(isAttribute = true, localName = "level")
    var level: String? = null,

    @field:JacksonXmlProperty(isAttribute = true, localName = "additivity")
    var additivity: Boolean? = null,

    @field:JacksonXmlProperty(isAttribute = true, localName = "appender-ref")
    var appenderRef: String? = null,

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

    @JacksonXmlProperty(localName = "appender-ref")
    var appenderRef: MutableList<AppenderRef>? = mutableListOf(),

)

/*
An appender is configured with the <appender> element, which takes two mandatory attributes name and class.
The name attribute specifies the name of the appender whereas the class attribute specifies the fully qualified name of
the appender class to instantiate. The <appender> element may contain zero or one <layout> elements, zero or more
<encoder> elements and zero or more <filter> elements.
Apart from these three common elements, <appender> elements may contain any number of elements corresponding to
JavaBean properties of the appender class.
 */
data class Appender(

    @field:JacksonXmlProperty(isAttribute = true, localName = "name")
    val name: String,

    @field:JacksonXmlProperty(isAttribute = true, localName = "class")
    val className: String,

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
    var appenderRef: AppenderRef? = null,

)

data class AppenderRef(

    @field:JacksonXmlProperty(isAttribute = true, localName = "ref")
    var ref: String,

)

data class Encoder(

    @field:JacksonXmlProperty(localName = "pattern")
    var pattern: String = "%.-1p [%-30c{1}] [%d{MM:dd:YYYY HH:mm:ss, America/Los_Angeles}]: %m %X%n",

)

data class LevelFilter(

    @field:JacksonXmlProperty(localName = "class")
    val className: String = "ch.qos.logback.classic.filter.LevelFilter",

    @field:JacksonXmlProperty(localName = "level")
    var level: String,

    @field:JacksonXmlProperty(localName = "onMatch")
    val onMatch: String? = "ACCEPT",

    @field:JacksonXmlProperty(localName = "onMismatch")
    val onMismatch: String? = "DENY",

)

@JacksonXmlRootElement
data class RollingPolicy(

    @field:JacksonXmlProperty(isAttribute = true, localName = "class")
    var className: String,

    @field:JacksonXmlProperty(localName = "fileNamePattern")
    var fileNamePattern: String = "\${ROOT}\\\\AlarmJournal.%d{yyyy-MM-dd}.%i.log",

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
//    val selectedLoggers: MutableList<SelectedLogger>? = null,
) {

    // Build XmlMapper with the parameters for serialization
    private val xmlMapper: ObjectMapper = XmlMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        @Suppress("DEPRECATION")
        enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    // Convert UI selections to new properties/objects

    // Modify (read/write to) LogbackConfigData data class properties

    // Generate XML-mapped data classes from the selected loggers

    // For each SelectedLogger, we need:
    // a <logger> element

    // if same output, use <appender-ref ref="SysoutAppender" />

    // if separate output, use <appender-ref ref="FILE"/>
    // also need a new appender, use <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    // filter? <filter class="ch.qos.logback.classic.filter.LevelFilter">
    // rollingPolicy
    // encoder.filenamePattern
    //
    fun updateLoggerConfigs(selectedLoggers: MutableList<SelectedLogger>?) {
        selectedLoggers?.forEach {
            println("Checking logger: $it")
            if (it.separateOutput) {
                println(it)
                println("${it.name} is set to a separate output destination.")
                println("Output folder: ${it.outputFolder}")
            } else {
                println("${it.name} is set to output to regular logs file.")
            }
        }
    }

//    val name: String = "SelectedLogger.name",
//    val description: String = "SelectedLogger.description",
//    val level: String = "INFO",
//    val separateOutput: Boolean = false,
//    val outputFolder: String = "\${ROOT}\\AdditionalLogs",
//    val filenamePattern: String = "${name.replace(".", "")}.%d{yyyy-MM-dd}.%i.log",
//    val maxFileSize: Long = 10,
//    val totalSizeCap: Long = 1000,
//    val maxDaysHistory: Long = 5

    // Convert LogbackConfigData data class to XML string (for UI and clipboard)
    private fun generateXmlString(): String {
        return XML_HEADER + xmlMapper.writeValueAsString(configs)
    }

    // Convert LogbackConfigData data class to XML file (serialization)
    fun writeXmlFile(filePathString: String) {
        xmlMapper.writeValue(File(filePathString), configs)
    }

    companion object {
        const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
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
