#!/bin/sh

# Set the backup directory
BACKUPDIR="/home/jbilling/backup"
if [ ! -d "$BACKUPDIR" ]; then
  /usr/bin/logger "$0: Destination directory $BACKUPDIR doesn't exist."
  /bin/mkdir -p $BACKUPDIR
fi

# Avoid taking all the CPU
/usr/bin/renice +19 -p $$ > /dev/null

# Dump the DB and compress it while dumping
DB_NAME="jbilling"
DB_USER="jbilling"
DUMP_FILE="$BACKUPDIR/jbilling-database.$(date +%Y%m%d-%H%M).sql.bz2"
/usr/bin/pg_dump $DB_NAME -U $DB_USER | /usr/bin/bzip2 -9 > $DUMP_FILE

# Remove backups older than KEEY_DAYS
KEEP_DAYS=30
#/bin/find $BACKUPDIR -maxdepth 1 -type f -mtime +$KEEP_DAYS -delete
