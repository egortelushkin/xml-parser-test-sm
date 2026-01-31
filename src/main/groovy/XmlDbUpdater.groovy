@GrabConfig(systemClassLoader=true)
@Grab('org.postgresql:postgresql:42.6.0')
import groovy.xml.XmlSlurper
import java.sql.*

class XmlDbUpdater {

    String xmlUrl
    Connection conn
    def xml

    XmlDbUpdater(String xmlUrl, String dbUrl, String dbUser, String dbPass) {
        this.xmlUrl = xmlUrl
        this.conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)
        loadXml()
    }

    void loadXml() {
        def parser = new XmlSlurper(false, false) // validation=false, namespaceAware=false
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        xml = parser.parse(xmlUrl)
    }

    ArrayList getTableNames() {
        ['currency', 'categories', 'offers']
    }

    String getTableDDL(String tableName) {
        def ddl = new StringBuilder()
        switch (tableName) {
            case 'currency':
                ddl << "CREATE TABLE IF NOT EXISTS currency (id SERIAL PRIMARY KEY,"
                xml.currency.each { c -> c.children().each { k,v -> ddl << "${k} TEXT," } }
                ddl.deleteCharAt(ddl.length()-1)
                ddl << ");"
                break
            case 'categories':
                ddl << "CREATE TABLE IF NOT EXISTS categories (id SERIAL PRIMARY KEY,"
                xml.categories.category.each { cat -> cat.children().each { k,v -> ddl << "${k} TEXT," } }
                ddl.deleteCharAt(ddl.length()-1)
                ddl << ");"
                break
            case 'offers':
                ddl << "CREATE TABLE IF NOT EXISTS offers (id SERIAL PRIMARY KEY,"
                xml.offers.offer.each { offer -> offer.children().each { k,v -> ddl << "${k} TEXT," } }
                ddl.deleteCharAt(ddl.length()-1)
                ddl << ");"
                break
            default:
                throw new RuntimeException("Unknown table: $tableName")
        }
        return ddl.toString()
    }

    ArrayList getColumnNames(String tableName) {
        switch(tableName) {
            case 'currency': return xml.currency.children().collect { it.name() }
            case 'categories': return xml.categories.category.children().collect { it.name() }
            case 'offers': return xml.offers.offer.children().collect { it.name() }
            default: return []
        }
    }

    boolean isColumnId(String tableName, String columnName) {
        def values = []
        switch(tableName) {
            case 'currency': xml.currency.each { values << it."$columnName".text() }; break
            case 'categories': xml.categories.category.each { values << it."$columnName".text() }; break
            case 'offers': xml.offers.offer.each { values << it."$columnName".text() }; break
        }
        return values.size() == values.toSet().size()
    }

    void update() {
        getTableNames().each { update(it) }
    }

    void update(String tableName) {
        def columns = getColumnNames(tableName)
        def stmt
        switch(tableName) {
            case 'currency':
                xml.currency.each { row ->
                    def values = columns.collect { "'${row."$it".text().replace("'", "''")}'" }.join(',')
                    def sql = "INSERT INTO currency (${columns.join(',')}) VALUES (${values}) ON CONFLICT DO NOTHING;"
                    stmt = conn.prepareStatement(sql)
                    stmt.executeUpdate()
                }
                break
            case 'categories':
                xml.categories.category.each { row ->
                    def values = columns.collect { "'${row."$it".text().replace("'", "''")}'" }.join(',')
                    def sql = "INSERT INTO categories (${columns.join(',')}) VALUES (${values}) ON CONFLICT DO NOTHING;"
                    stmt = conn.prepareStatement(sql)
                    stmt.executeUpdate()
                }
                break
            case 'offers':
                xml.offers.offer.each { row ->
                    def values = columns.collect { "'${row."$it".text().replace("'", "''")}'" }.join(',')
                    def vendorCode = row.vendorCode.text()
                    def sql = "INSERT INTO offers (${columns.join(',')}) VALUES (${values}) " +
                            "ON CONFLICT (vendorCode) DO UPDATE SET ${columns.collect { "$it = EXCLUDED.$it" }.join(',')};"
                    stmt = conn.prepareStatement(sql)
                    stmt.executeUpdate()
                }
                break
        }
    }
}
