# MySQL 2 SQLite transformieren #


In diesem Artikel wird in Kürze erklärt wie man ein MySQL Dump in
eine SQLite Datenbank transformiert. Dies ist ein häufiger Fall
beim Programmieren von Android Anwendungen.


Die Datenbank von OpenThesaurus liegt im MySQL Format vor, diese muss
in die SQLite Datenbank portiert werden. Hierfür verwenden wir das
unten stehende Skript. Das original Skript ist auf folgender Seite zu finden http://www.sqlite.org/cvstrac/wiki?p=ConverterTools


```
   #!/bin/sh
  if [ "x$1" = "x" ]; then
     echo "Usage: $0 <dbname>"
     exit
  fi
  if [ -e "$1.db" ]; then
     echo "$1.db already exists.  I will overwrite it in 15 seconds if you do not press CTRL-C."
     COUNT=5
     while [ $COUNT -gt 0 ]; do
        echo "$COUNT"
        sleep 1
        COUNT=$((COUNT - 1))
     done
     rm $1.db
  fi
  mysqldump -u root -p --skip-opt --compatible=ansi $1 |
  grep -v ' KEY "' |
  grep -v ' UNIQUE KEY "' |
  grep -v ' PRIMARY KEY ' |
  sed 's/ unsigned / /g' |
  sed 's/ auto_increment/ primary key autoincrement/gi' |
  sed 's/ smallint([0-9]*) / integer /gi' |
  sed 's/ tinyint([0-9]*) / integer /gi' |
  sed 's/ int([0-9]*) / integer /gi' |
  sed 's/ character set [^ ]* / /gi' |
  sed 's/ enum([^)]*) / varchar(255) /gi' |
  sed 's/ on update [^,]*//gi' |
  sed 's/\\r\\n/\\n/g'|
  sed 's/\\"/"/g'|
  perl -e 'local $/;$_=<>;s/,\n\)/\n\)/gs;print "begin;\n";print;print "commit;\n"' |
  perl -pe '
  if (/^(INSERT.+?)\(/) {
     $a=$1;
     s/\\'\''/'\'\''/g;
     s/\\n/\n/g;
     s/\),\(/\);\n$a\(/g;
  }
  ' > $1.sql
  cat $1.sql | sqlite3 $1.db > $1.err
  ERRORS=`cat $1.err | wc -l`
  if [ "$ERRORS" == "0" ]; then
     echo "Conversion completed without error. Output file: $1.db"
  else
     echo "There were errors during conversion.  Please review $1.err and $1.sql for details."
  fi
```

Die Zeile `mysqldump -u root -p --skip-opt --compatible=ansi $1 |` passt man an die eigene Datenbank an. Die Ausführung sieht anschließend
folgendermaßen aus : "./mysql2sqlite database\_name".