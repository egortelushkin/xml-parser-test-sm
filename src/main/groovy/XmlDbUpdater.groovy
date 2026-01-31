@Grab(group='org.postgresql', module='postgresql', version='42.6.0')

import groovy.xml.XmlSlurper
import javax.xml.parsers.SAXParserFactory
import java.sql.*
import org.xml.sax.InputSource

class XmlProcessor {

    String xmlUrl
    String dbUrl
    String dbUser
    String dbPassword

    def xml
    Connection conn

    void init() {
        def spf = SAXParserFactory.newInstance()
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        spf.setFeature("http://xml.org/sax/features/external-general-entities", false)
        spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)

        def parser = spf.newSAXParser()
        def slurper = new XmlSlurper(parser)
        slurper.setEntityResolver({ a, b ->
            new InputSource(new StringReader(""))
        })

        xml = slurper.parse(new URL(xmlUrl).openStream())

        println "XML parsed successfully."
        println "Currencies: ${xml.shop.currencies.currency.size()}"
        println "Categories: ${xml.shop.categories.category.size()}"
        println "Offers: ${xml.shop.offers.offer.size()}"

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
        println "Connected to DB."
    }

    //helper for normalizing table names
    String normalizeTableName(String input) {
        input
                ?.replaceAll("[^a-zA-Z]", "")
                ?.toLowerCase()
    }

    List<String> getTableNames() {
        ['currencies', 'categories', 'offers']
    }

    List<String> getColumnNames(String tableName) {
        switch (tableName) {
            case 'currencies':
                return ['id', 'rate']
            case 'categories':
                return ['id', 'parentId', 'name']
            case 'offers':
                return [
                        'id', 'available', 'url', 'price',
                        'currencyId', 'categoryId', 'picture',
                        'name', 'vendor', 'vendorCode', 'description'
                ]
            default:
                throw new IllegalArgumentException("Unknown table: $tableName")
        }
    }

    boolean isColumnId(String tableName, String columnName) {
        columnName == 'id' || (tableName == 'offers' && columnName == 'vendorCode')
    }

    String getTableDDL(String tableName) {
        tableName = normalizeTableName(tableName)

        def columns = getColumnNames(tableName)

        def ddlCols = columns.collect { col ->
            def type =
                    (col == 'price') ? 'NUMERIC' :
                            (col == 'available') ? 'BOOLEAN' :
                                    'TEXT'
            "$col $type"
        }.join(",\n")

        def uniqueCol = tableName == 'offers' ? 'vendorCode' : 'id'

        """
CREATE TABLE IF NOT EXISTS $tableName (
$ddlCols,
CONSTRAINT ${tableName}_unique UNIQUE ($uniqueCol)
);
"""
    }

    //table management
    void createTables() {
        getTableNames().each { table ->
            conn.createStatement().withCloseable { stmt ->
                stmt.execute(getTableDDL(table))
                println "Table '$table' is ready."
            }
        }
    }

    void checkTableStructure(String tableName) {
        def expected = getColumnNames(tableName)
                .collect { it.toLowerCase() }
                .sort()

        def actual = []

        def rs = conn.metaData.getColumns(null, null, tableName, null)
        while (rs.next()) {
            actual << rs.getString("COLUMN_NAME").toLowerCase()
        }

        if (actual && actual.sort() != expected) {
            throw new IllegalStateException(
                    "Table structure changed for '$tableName'\nExpected: $expected\nActual: $actual"
            )
        }
    }

    void update() {
        getTableNames().each { update(it) }
    }

    void update(String rawName) {
        def tableName = normalizeTableName(rawName)

        if (!getTableNames().contains(tableName)) {
            throw new IllegalArgumentException("Unknown table: $tableName")
        }

        checkTableStructure(tableName)

        def columns = getColumnNames(tableName)
        def rows = []

        switch (tableName) {
            case 'currencies':
                xml.shop.currencies.currency.each {
                    rows << [it.@id.toString(), it.@rate.toString()]
                }
                break

            case 'categories':
                xml.shop.categories.category.each {
                    rows << [
                            it.@id.toString(),
                            it.@parentId?.toString(),
                            it.text().trim()
                    ]
                }
                break

            case 'offers':
                xml.shop.offers.offer.each { o ->
                    rows << [
                            o.@id.toString(),
                            o.@available.toString().toBoolean(),
                            o.url.text(),
                            o.price.text() ? new BigDecimal(o.price.text()) : null,
                            o.currencyId.text(),
                            o.categoryId.text(),
                            o.picture.text(),
                            o.name.text(),
                            o.vendor.text(),
                            o.vendorCode.text(),
                            o.description.text()
                    ]
                }
                break
        }

        println "Updating table '$tableName' with ${rows.size()} rows."

        rows.each { row ->
            def placeholders = row.collect { "?" }.join(", ")
            def conflictCol = tableName == 'offers' ? 'vendorCode' : 'id'

            def updateSet = columns
                    .findAll { it != conflictCol }
                    .collect { "$it = EXCLUDED.$it" }
                    .join(", ")

            def sql = """
INSERT INTO $tableName (${columns.join(',')})
VALUES ($placeholders)
ON CONFLICT ($conflictCol)
DO UPDATE SET $updateSet
"""

            conn.prepareStatement(sql).withCloseable { ps ->
                row.eachWithIndex { val, i -> ps.setObject(i + 1, val) }
                ps.executeUpdate()
            }
        }
    }

    void close() {
        conn?.close()
    }
}


static void main(String[] args) {

    def processor = new XmlProcessor(
            xmlUrl: "https://expro.ru/bitrix/catalog_export/export_Sai.xml",
            dbUrl: "jdbc:postgresql://db:5432/xmltest",
            dbUser: "egor",
            dbPassword: "password123"
    )

    processor.init()
    processor.createTables()

    def scanner = new Scanner(System.in)

    while (true) {
        println """
=== XML Processor Menu ===
1. Показать таблицы
2. Показать DDL таблицы
3. Обновить таблицу
4. Обновить все таблицы
5. Выйти
"""
        print "Выберите действие: "
        def choice = scanner.nextLine()

        switch (choice) {
            case '1':
                println "Таблицы: ${processor.getTableNames()}"
                break

            case '2':
                print "Введите имя таблицы: "
                println processor.getTableDDL(scanner.nextLine())
                break

            case '3':
                print "Введите имя таблицы: "
                processor.update(scanner.nextLine())
                break

            case '4':
                processor.update()
                break

            case '5':
                processor.close()
                println "Выход."
                return

            default:
                println "Неверный пункт меню"
        }
    }
}
