import java.util.Scanner

// Настройки подключения к PostgreSQL
def dbUrl = "jdbc:postgresql://db:5432/xmltest"
def dbUser = "egor"
def dbPass = "password123"
def xmlUrl = "https://expro.ru/bitrix/catalog_export/export_Sai.xml"

// Создаем экземпляр класса обновления XML
def updater = new XmlDbUpdater(xmlUrl, dbUrl, dbUser, dbPass)

Scanner scanner = new Scanner(System.in)

while (true) {
  println "\n=== XML-DB Updater Menu ==="
  println "1. Показать таблицы"
  println "2. Показать DDL таблицы"
  println "3. Обновить все таблицы"
  println "4. Обновить одну таблицу"
  println "5. Показать колонки таблицы"
  println "6. Проверить уникальность колонки"
  println "0. Выход"
  print "Выберите действие: "
  def choice = scanner.nextLine()

  switch(choice) {
    case "1":
      println "Таблицы: ${updater.getTableNames()}"
      break
    case "2":
      print "Введите имя таблицы: "
      def table = scanner.nextLine()
      println updater.getTableDDL(table)
      break
    case "3":
      println "Обновление всех таблиц..."
      updater.update()
      println "Обновление завершено!"
      break
    case "4":
      print "Введите имя таблицы для обновления: "
      def table = scanner.nextLine()
      updater.update(table)
      println "Обновление таблицы ${table} завершено!"
      break
    case "5":
      print "Введите имя таблицы: "
      def table = scanner.nextLine()
      println "Колонки: ${updater.getColumnNames(table)}"
      break
    case "6":
      print "Введите имя таблицы: "
      def table = scanner.nextLine()
      print "Введите имя колонки: "
      def column = scanner.nextLine()
      boolean unique = updater.isColumnId(table, column)
      println "Колонка ${column} уникальна: ${unique}"
      break
    case "0":
      println "Выход..."
      System.exit(0)
      break
    default:
      println "Неверный выбор. Попробуйте снова."
  }
}
