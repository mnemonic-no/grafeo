#
# Script to populate the fact_refresh_log table in Cassandra. Requires the Cassandra client driver for Python,
# see https://docs.datastax.com/en/developer/python-driver/.
#

from cassandra.cluster import Cluster

ONE_DAY_MS = 24 * 60 * 60 * 1000

fetched_counter = 0
skipped_counter = 0
inserted_counter = 0

# Change variable to point to your Cassandra instance.
cluster = Cluster(["localhost"], port=9042)
session = cluster.connect("act")

insert_statement = session.prepare("INSERT INTO fact_refresh_log (fact_id, refreshed_timestamp, refreshed_by_id) VALUES (?, ?, ?)")
update_statement = session.prepare("UPDATE fact SET last_seen_by_id = ? WHERE id = ?")
all_facts = session.execute("SELECT id, timestamp, last_seen_timestamp, added_by_id, last_seen_by_id FROM fact")
for (id, timestamp, last_seen_timestamp, added_by_id, last_seen_by_id) in all_facts:
    fetched_counter = fetched_counter + 1

    # Skip facts which have last_seen_by_id set. Then the fact_refresh_log will already be populated.
    if last_seen_by_id is not None:
        skipped_counter = skipped_counter + 1
        continue

    # Update the last_seen_by_id column first.
    session.execute(update_statement, [added_by_id, id])

    # This is an approximation! For each fact add one entry every 24h between timestamp and lastSeenTimestamp
    # into the refresh log. Assume that the user who added the fact in the first place also refreshed the fact.
    current_timestamp = timestamp
    while current_timestamp <= last_seen_timestamp:
        session.execute(insert_statement, [id, current_timestamp, added_by_id])
        inserted_counter = inserted_counter + 1
        current_timestamp = current_timestamp + ONE_DAY_MS

    if fetched_counter % 100000 == 0:
        print("Fetched %d, skipped %d, inserted %d" % (fetched_counter, skipped_counter, inserted_counter))

print("Fetched %d, skipped %d, inserted %d" % (fetched_counter, skipped_counter, inserted_counter))
print("Done populating fact_refresh_log table")
