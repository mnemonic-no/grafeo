#
# Script to populate the fact_by_timestamp table in Cassandra. Requires the Cassandra client driver for Python,
# see https://docs.datastax.com/en/developer/python-driver/.
#

from cassandra.cluster import Cluster
from datetime import datetime, timezone

fetched_counter = 0
inserted_counter = 0

# Change variable to point to your Cassandra instance.
cluster = Cluster(["localhost"], port=9042)
session = cluster.connect("act")

insert_statement = session.prepare("INSERT INTO fact_by_timestamp (hour_of_day, timestamp, fact_id) VALUES (?, ?, ?)")
all_facts = session.execute("SELECT id, timestamp FROM fact")
for (id, timestamp) in all_facts:
    hour_of_day = int(datetime.fromtimestamp(timestamp / 1000, timezone.utc).replace(minute=0, second=0, microsecond=0).timestamp() * 1000)
    session.execute(insert_statement, [hour_of_day, timestamp, id])

    fetched_counter = fetched_counter + 1
    inserted_counter = inserted_counter + 1

    if fetched_counter % 100000 == 0:
        print("Fetched %d, inserted %d" % (fetched_counter, inserted_counter))

print("Fetched %d, inserted %d" % (fetched_counter, inserted_counter))
print("Done populating fact_by_timestamp table")
