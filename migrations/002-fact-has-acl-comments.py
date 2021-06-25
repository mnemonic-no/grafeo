#
# Script to populate the HasAcl and HasComments flags in Cassandra. Requires the Cassandra client driver for Python,
# see https://docs.datastax.com/en/developer/python-driver/.
#

from cassandra.cluster import Cluster

# Change variable to point to your Cassandra instance.
cluster = Cluster(["localhost"], port=9042)
session = cluster.connect("act")

# HasAcl = 1, HasComments = 2, see FactEntity.
has_acl_update_statement = session.prepare("UPDATE fact SET flags = flags + { 1 } WHERE id = ?")
has_comments_update_statement = session.prepare("UPDATE fact SET flags = flags + { 2 } WHERE id = ?")

facts_with_acl = {row.fact_id for row in session.execute("SELECT fact_id FROM fact_acl")}
facts_with_comments = {row.fact_id for row in session.execute("SELECT fact_id FROM fact_comment")}
print("%d facts have an ACL, %d facts have comments." % (len(facts_with_acl), len(facts_with_comments)))

print("Updating facts with HasAcl flag...")
for fact_id in facts_with_acl:
    session.execute(has_acl_update_statement, [fact_id])

print("Updating facts with HasComments flag...")
for fact_id in facts_with_comments:
    session.execute(has_comments_update_statement, [fact_id])
